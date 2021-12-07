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

## Storing sensitive data

Now that we know the credentials we'll need for interacting with the API, we need somewhere to store them. Morpheus Cypher provides a native method for securely storing sensitive data, such as API keys.

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

## Store the credentials in Morpheus Cypher

The credentials for interacting with the DataDog REST API will be stored in the cypher secure storage. The [cypher service](https://developer.morpheusdata.com/api/com/morpheusdata/core/cypher/MorpheusCypherService.html) enables us to easily interact with cypher to retrieve the API keys.

The first thing we need to do is initialize our access to cypher which requires the account or tenant ID where the credentials are stored. This example assumes that the credentials are stored in the Morpheus master tenant.

```
def account = new Account(id: 1)
def cypherAccess = new CypherAccess(account)
```

**Retrieve the API key**

The API Key used to authenticate to the DataDog API is stored in Morpheus Cypher so the plugin needs to retrieve the API key before making an API call to fetch the instance data. The plugin expects the cypher secret to be named `dd-api-key` in order to retrieve the value from cypher.

```
def intApiKey = morpheus.getCypher().read(cypherAccess, "secret/dd-api-key")
def apiKey = ""
intApiKey.subscribe(
    { secretData -> 
            apiKey = secretData
    },
    { error ->
            println error.printStackTrace()
    }
)
```

**Retrieve the Application key**

The Application key used to authenticate to the DataDog API is stored in Morpheus Cypher so the plugin needs to retrieve the Application key before making an API call to fetch the instance data. The plugin expects the cypher secret to be named `dd-application-key` in order to retrieve the value from cypher.

```
def intAppKey = morpheus.getCypher().read(cypherAccess, "secret/dd-application-key")
def appKey = ""
intAppKey.subscribe(
    { secretData -> 
            appKey = secretData
    },
    { error ->
            println error.printStackTrace()
    }
)
```

## Query the DataDog API

With credentials we're now ready to define our API call to the DataDog API endpoint. We'll use the REST API helper included in the plugin library to simplify the API call.

```
def results = dataDogAPI.callApi("https://api.datadoghq.com", "api/v1/hosts?filter=host:${instance.name}", "", "", new RestApiUtil.RestOptions(headers:['Content-Type':'application/json','DD-API-KEY':apiKey,'DD-APPLICATION-KEY':appKey], ignoreSSL: false), 'GET')
```

## Parse the JSON Response

Now that we've got the response payload we'll use the JsonSlurper library to parse the JSON.

```
JsonSlurper slurper = new JsonSlurper()
def json = slurper.parseText(results.content)
```

## Managing tab visibility

Now that we've got the core functionality of the plugin developed we want to restrict which instances the tab is displayed for (i.e. - only production, AWS cloud instance , etc.).

### Manage visibility by instance environment

The Morpheus environment that an instance or virtual machine belongs to can be used to determine the visibility of the DataDog plugin tab.

```
def tabEnvironments = ["all"]
def environmentStatus = false
if (tabEnvironments.contains("all")){
        environmentStatus = true
} 
if(tabEnvironments.contains(config.instance.instanceContext)){
        environmentStatus = true
}
```

### Manage visibility by Morpheus group

The Morpheus group that an instance or virtual machine belongs to can be used to determine the visibility of the DataDog plugin tab.

```
def tabGroups = ["all"]
def groupStatus = false
if(tabGroups.contains("all")){
        groupStatus = true
}
if(tabGroups.contains(instance.site.name)){
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

# Install and configure the UI plugin
Custom plugins are added to Morpheus through the Plugins tab in the Integrations section.

1. Navigate to Administration > Integrations > Plugins and click CHOOSE FILE. 
2. Browse for your JAR file and upload it to Morpheus. 
3. The new plugin will be added next to any other custom plugins that may have been developed for your appliance.

