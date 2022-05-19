# Morpheus DataDog UI Plugin

The Morpheus plugin architecture is a library which allows the functionality of the Morpheus platform to be extended. This functionality includes support for new Cloud providers, Task types, UI views, custom reports, and more.

Morpheus plugins are written in Java or Groovy, our example here will be written in Groovy. Complete developer documentation including the full API documentation and links to Github repositories containing complete code examples are available in the [Developer Portal](https:/developer.morpheusdata.com).

In this guide, we'll take a look at developing a custom instance UI tab. Custom plugin development requires some programming experience but this guide is designed to break down the required steps into a digestible process that users can quickly run with.

**Requirements**

Before we begin, we need to ensure that we've met the following requirements to develop the plugin:

* Gradle 6.5 or later installed
* Java 8 or 11 installed
* A DataDog account (https://www.datadoghq.com/)

# Table of Contents

[Planning the UI plugin](#planning-the-ui-plugin)

* [Understanding the REST API](#understanding-the-rest-api)

* [Storing sensitive data](#storing-sensitive-data)

* [Making API calls](#making-api-calls)

[Developing the UI plugin](#developing-the-ui-plugin)

[Build the UI plugin](#build-the-ui-plugin)

[Install and configure the UI plugin](#install-and-configure-the-ui-plugin)

# Planning the UI plugin

In this example, we'll create a custom instance UI tab that fetches instance related data from the DataDog monitoring solution. Before writing any code we'll plan out the functionality of our plugin.

**Overview Section**

The first section we want is an overview page that provides basic information about the instance that DataDog collects.

![DataDog Plugin Overview](/_images/DataDog-Plugin-Overview.png)

**System Information Section**

The next section we want is system information or details that DataDog collects. 

![DataDog Plugin Details](/_images/DataDog-Plugin-Details.png)

**Host Not Found Section**

The final section we want to develop is a placeholder for when there isn't a DataDog agent installed on the system.

![DataDog Plugin Not Found](/_images/DataDog-Plugin-Not-Found.png)

## Understanding the REST API

Now that we know what we want our plugin to do, we need the API calls to gather the necessary data. The [DataDog REST API documentation](https://docs.datadoghq.com/api/latest/) details the various API calls that the platform supports along with the response payload.

### Authentication

The DataDog REST API requires an API key as well as an Application key since we'll be fetching or getting data from the REST API. The process for generating the two keys are detailed in the DataDog [documentation](https://docs.datadoghq.com/account_management/api-app-keys/#api-keys).

## Making API calls

The Morpheus plugin library includes functions for simplifying the process of making a REST API call to an external system.

# Developing the UI plugin

Now that we've planned out the plugin, we're ready to begin developing the plugin. The first thing we'll do is create a new directory to house the project. You’ll ultimately end up with a file structure typical of Java or Groovy projects, looking something like this:

```
./
.gitignore
build.gradle
src/main/groovy/
src/main/resources/renderer/hbs/
src/test/groovy/
src/assets/images/
src/assets/javascript/
src/assets/stylesheets/
```

Configure the .gitignore file to ignore the build/ directory which will appear after performing the first build. Project packages live within src/main/groovy and contain source files ending in .groovy. View resources are stored in the src/main/resources subfolder and vary depending on the view renderer of choice. Static assets, like icons or custom javascript, live within the src/assets folder. Consult the table below for key files, their purpose, and their locations. Example code and further discussion of relevant files is included in the following sections.

## Updating the build.gradle file

The `build.gradle` file contains information used during the build of the plugin. The example included is sufficient for most use cases with a few updates detailed in the table below.

```
plugins {
    id "com.bertramlabs.asset-pipeline" version "3.3.2"
    id "com.github.johnrengelman.plugin-shadow" version "2.0.3"
}

apply plugin: 'java'
apply plugin: 'groovy'
apply plugin: 'maven-publish'

group = 'com.morpheusdata'
version = '0.0.2'

sourceCompatibility = '1.8'
targetCompatibility = '1.8'

ext.isReleaseVersion = !version.endsWith("SNAPSHOT")

repositories {
    mavenCentral()
}

dependencies {
    compileOnly 'com.morpheusdata:morpheus-plugin-api:0.12.0'
    compileOnly 'org.codehaus.groovy:groovy-all:2.5.6'
    compileOnly 'io.reactivex.rxjava2:rxjava:2.2.0'
    compileOnly "org.slf4j:slf4j-api:1.7.26"
    compileOnly "org.slf4j:slf4j-parent:1.7.26"
}

jar {
    manifest {
        attributes(
	    'Plugin-Class': 'com.morpheusdata.tab.DataDogTabPlugin',
            'Plugin-Version': archiveVersion.get() // Get version defined in gradle
        )
    }
}

tasks.assemble.dependsOn tasks.shadowJar
```

|Name|Description|Example|
|----|-----------|-------|
|group|The group of the project or an identifier of who owns the plugins. This is typically a domain that is associated with the individual developer or organization.|com.morpheusdata|
|version|The version of the plugin which will appear in the Morpheus UI|0.0.2|
|dependencies|The plugin utilizes a number of dependencies for functionality and the `morpheus-plugin-api` dependency is the one of particular interest to us. The available release versions can be found on [Maven Central](https://search.maven.org/artifact/com.morpheusdata/morpheus-plugin-api) where the releases are hosted. **Versions of the dependency align with specified versions of the Morpheus platform** |com.morpheusdata:morpheus-plugin-api:0.12.0|
|plugin-class|The name of the plugin class (group.packageName.className). In the example this also aligns with the folder structure of com/morpheusdata/tab/DataDogTabPlugin.groovy |com.morpheusdata.tab.DataDogTabPlugin|

## Plugin Settings
The plugin should support runtime configuration to allow administrators to update the configuration of the plugin without the need to rebuild it. The 5.4.1 release of the Morpheus platform added supported for plugin settings which enable the plugin to support runtime configuration.

In our example we want to expose a few runtime configuration settings such as the API credentials and conditions for when the tab should be visible. The table below details the settings that the plugin supports.

|Name|Description|
|----|-----------|
|DataDog API Key|The API key used for authenticating to the DataDog REST API|
|DataDog Application Key|The application key used for authenticating to the DatDog REST API|
|Environment Visibility|The setting to define which Morpheus environments that a workload belongs to for the tab to be visible|
|Group Visibility|The setting to define which Morpheus groups that a workload belongs to for the tab to be visible|


### Defining Plugin Settings
The plugin settings are defined in the plugin [definition file](/src/main/groovy/com/morpheusdata/tab/DataDogTabPlugin.groovy) as option types similar to the option types or inputs used in other areas of the Morpheus platform. One item of note is the `fieldName` which is used as the reference for when retrieving the value specified from within the code.

```
this.settings << new OptionType(
        name: 'API Key',
        code: 'datadog-plugin-api-key',
        fieldName: 'ddApiKey',
        displayOrder: 0,
        fieldLabel: 'API Key',
        helpText: 'The DataDog API key',
        required: true,
        inputType: OptionType.InputType.PASSWORD
)
```

### Retrieving Plugin Settings

The plugin settings enable administrators to update the runtime configuration of the plugin and so we need to reference them in our code. The settings are accessible by retrieving them and parsing the JSON payload.

```
def settings = morpheus.getSettings(plugin)
def settingsOutput = ""
settings.subscribe(
        { outData -> 
        settingsOutput = outData
},
{ error ->
        println error.printStackTrace()
}
)
JsonSlurper slurper = new JsonSlurper()
def settingsJson = slurper.parseText(settingsOutput)
```

In our example the value of the settings will be available using dot notation (i.e. - settingsJson.settingFieldName). This is where fieldName value of the plugin settings is referenced.

## Query the DataDog API

With credentials we're now ready to define our API call to the DataDog API endpoint. We'll use the REST API helper included in the plugin library to simplify the API call.

```
def results = dataDogAPI.callApi("https://api.datadoghq.com", "api/v1/hosts?filter=host:${instance.name}", "", "", new RestApiUtil.RestOptions(headers:['Content-Type':'application/json','DD-API-KEY':settingsJson.ddApiKey,'DD-APPLICATION-KEY':settingsJson.ddAppKey], ignoreSSL: false), 'GET')
```

## Parse the JSON Response

Now that we've got the response payload we'll use the JsonSlurper library to parse the JSON.

```
JsonSlurper slurper = new JsonSlurper()
def json = slurper.parseText(results.content)
```

The parsed payload supports accessing the payload values using dot notation. The example below which is been truncated for brevity shows how the payload is accessed and ultimately passed to the html template file.

```
if (json.host_list.size == 0){
        getRenderer().renderTemplate("hbs/instanceNotFoundTab", model)
} else {
        // Store objects from the response payload
        def baseHost = json.host_list[0]
        def apps = baseHost.apps
        def agentVersion = baseHost.meta.agent_version
        def checks = baseHost.meta.agent_checks.size
        def agentChecks = baseHost.meta.agent_checks

        dataDogPayload.put("id", instance.id)
        dataDogPayload.put("apps", apps)
        dataDogPayload.put("agentVersion", agentVersion)
        dataDogPayload.put("checks", checks)
        dataDogPayload.put("agentChecks", agentChecks)
        // Set the value of the model object to the HashMap object
        model.object = dataDogPayload

        // Render the HTML template located in 
        // resources/renderer/hbs/instanceTab.hbs
        getRenderer().renderTemplate("hbs/instanceTab", model)
}
```

## Managing tab visibility

Now that we've got the core functionality of the plugin developed we want to restrict which instances the tab is displayed for (i.e. - only production, AWS cloud instance , etc.).

### Manage visibility with Morpheus RBAC

The RBAC permissions associated with a user can be used to determine
the visibility of the DataDog plugin tab.

```
def permissionStatus = false
if(user.permissions["datadog-instance-tab"] == "full"){
    permissionStatus = true
}
```

### Manage visibility by instance environment

The Morpheus environment that an instance or virtual machine belongs to can be used to determine the visibility of the DataDog plugin tab.

```
def tabEnvironments = settingsJson.environmentVisibilityField.split(",");
def visibleEnvironments = []
for(environment in tabEnvironments){
    visibleEnvironments.add(environment.trim())
}
println visibleEnvironments
def environmentStatus = false
if (visibleEnvironments.contains("any")){
    environmentStatus = true
} 
if(visibleEnvironments.contains(config.instance.instanceContext)){
    environmentStatus = true
}
```

### Manage visibility by Morpheus group

The Morpheus group that an instance or virtual machine belongs to can be used to determine the visibility of the DataDog plugin tab.

```
def tabGroups = settingsJson.groupVisibilityField.split(",");
def visibleGroups = []
for(group in tabGroups){
    visibleGroups.add(group.trim())
}
println visibleGroups
def groupStatus = false
if(visibleGroups.contains("any")){
    groupStatus = true
}
if(visibleGroups.contains(instance.site.name)){
    groupStatus = true
}
```

# Build the UI plugin
With the code written, we'll use gradle to build the JAR which we'll upload to Morpheus to install the plugin. To do so, change directory into the location of the directory created earlier to hold your custom plugin code.

```
cd path/to/your/directory
```

Run gradle to build a new version of the plugin.

```
gradle shadowJar
```

Once the build process has completed, locate the JAR in the build/libs directory.

# Install and Configure the UI plugin
Custom plugins are added to Morpheus through the Plugins tab in the Integrations section of the Morpheus UI.

**Install the plugin**

![DataDog Plugin Installation](/_images/DataDog-Plugin-Installation.png)

1. Navigate to Administration > Integrations > Plugins and click CHOOSE FILE. 
2. Browse for your JAR file and upload it to Morpheus. 
3. The new plugin will be added next to any other custom plugins that may have been developed for your appliance.

![DataDog Plugin Successful Installation](/_images/DataDog-Plugin-Installed.png)

**Configure the plugin**

1. Click on the pencil to the right of the plugin to open the configuration modal.

![DataDog Plugin Settings](/_images/DataDog-Plugin-Settings.png)

2. Enter the API and Application keys used to authenticate to the DataDog REST API.
3. Click the `SAVE` button to save the changes.