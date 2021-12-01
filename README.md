The Morpheus plugin architecture is a library which allows users to extend functionality in several categories, including new Cloud providers, Task types, UI views, custom reports, and more. In this guide, we will take a look at developing a custom instance UI tab. Complete developer documentation including the full API documentation and links to Github repositories containing complete code examples are available in the [Developer Portal](https:/developer.morpheusdata.com).

Custom plugin development requires some programming experience but this guide is designed to break down the required steps into a digestible process that users can quickly run with. Morpheus plugins are written in Java or Groovy, our example here will be written in Groovy. 


**Requirements**

Before we begin, we need to ensure that we've met the following requirements to develop the plugin:

* Gradle 6.5 or later installed
* Java 8 or 11 installed
* A DataDog account (https://www.datadoghq.com/)

# Planning the Custom Instance UI Tab Plugin

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

Now that we know what we want our plugin to functionally do we know need to find out the API calls we need to make in order to fetch the desired information.

[DataDog REST API documentation](https://docs.datadoghq.com/api/latest/)

### Authentication

The DataDog REST API requires an API key as well as an Application key since we'll be fetching or getting data from the REST API.

https://docs.datadoghq.com/account_management/api-app-keys/#api-keys

## Storing sensitive data

Morpheus Cypher provides a native secure storage feature

In this case it makes sense to store the DataDog API and Application keys in Cypher.


### API Calls


# Developing the plugin

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

```
def results = dataDogAPI.callApi("https://api.datadoghq.com", "api/v1/hosts?filter=host:${instance.name}", "", "", new RestApiUtil.RestOptions(headers:['Content-Type':'application/json','DD-API-KEY':apiKey,'DD-APPLICATION-KEY':appKey], ignoreSSL: false), 'GET')
```

## Parse the JSON Response

```
JsonSlurper slurper = new JsonSlurper()
def json = slurper.parseText(results.content)
```


## Managing tab visibility

Now that we've got the core functionality of the plugin developed we want to restrict which instances the tab is displayed for (i.e. - only production, AWS cloud instance , etc.).

### Manage visibility by instance environment

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



# Build the JAR
With the code written, use gradle to build the JAR which we can upload to Morpheus so the report can be viewed. To do so, change directory into the location of the directory created earlier to hold your custom plugin code.

```
cd path/to/your/directory
```

Run gradle to build a new version of the plugin.

```
gradle shadowJar
```

Once the build process has completed, locate the JAR in the build/libs directory

# Install and Configure  the Custom Plugin
Custom plugins are added to Morpheus through the Plugins tab in the Integrations section.

1. Navigate to Administration > Integrations > Plugins and click CHOOSE FILE. 
2. Browse for your JAR file and upload it to Morpheus. 
3. The new plugin will be added next to any other custom plugins that may have been developed for your appliance.

