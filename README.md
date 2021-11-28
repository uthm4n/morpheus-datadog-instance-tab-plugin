The Morpheus plugin architecture is a library which allows users to extend functionality in several categories, including new Cloud providers, Task types, UI views, custom reports, and more. In this guide, we will take a look at developing a custom instance UI tab. Complete developer documentation including the full API documentation and links to Github repositories containing complete code examples are available in the Developer Portal.

Custom plugin development requires programming experience but this guide is designed to break down the required steps into a digestible process that users can quickly run with. Morpheus plugins are written in Java or Groovy, our example here will be written in Groovy. Support for additional languages is planned but not yet available at the time of this writing. If you’re not an experienced Java or Groovy developer, it may help to clone an example code repository which we link to in our developer portal. An additional example, which this guide is based on, is here. You can read the example code and tweak it to suit your needs using the guidance in this document.

Before you begin, ensure you have the following installed in your development environment:

* Gradle 6.5 or later
* Java 8 or 11
* A DataDog account (https://www.datadoghq.com/)

In this example, you'll create a custom instance UI tab that fetches instance related data from the DataDog monitoring solution. Below that will be a table which shows each Cypher item individually and sorted alphabetically along with their create date and the date they were last accessed.

# Planning the Custom Instance UI Tab Plugin

## Understanding the REST API

https://docs.datadoghq.com/api/latest/

### Authentication

The DataDog REST API requires an API key as well as an Application key since we'll be fetching or getting data from the REST API.

https://docs.datadoghq.com/account_management/api-app-keys/#api-keys

## Storing sensitive data
Morpheus Cypher provides a native secure storage feature
In this case it makes sense to store the DataDog API and Application keys in Cypher.


### API Calls

With the authentication 

# Developing the plugin

Now that we've planned out plugin, we're ready to begin developing the plugin. The first thing we'll do is create a new directory to house the project. You’ll ultimately end up with a file structure typical of Java or Groovy projects, looking something like this:

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



## Parse the JSON Response


```
def results = dataDogAPI.callApi("https://api.datadoghq.com", "api/v1/hosts?filter=host:${instance.name}", "", "", new RestApiUtil.RestOptions(headers:['Content-Type':'application/json','DD-API-KEY':apiKey,'DD-APPLICATION-KEY':appKey], ignoreSSL: false), 'GET')

JsonSlurper slurper = new JsonSlurper()
def json = slurper.parseText(results.content)
```


# Build the JAR
With the code written, use gradle to build the JAR which we can upload to Morpheus so the report can be viewed. To do so, change directory into the location of the directory created earlier to hold your custom plugin code.

```
cd path/to/your/directory
```

Build your new plugin.

```
gradle shadowJar
```

Once the build process has completed, locate the JAR in the build/libs directory

# Upload the Custom Instance Tab Plugin to Morpheus UI
Custom plugins are added to Morpheus through the Plugins tab in the Integrations section (Administration > Integrations > Plugins). Navigate to this section and click CHOOSE FILE. Browse for your JAR file and upload it to Morpheus. The new plugin will be added next to any other custom plugins that may have been developed for your appliance.

## Configuring the 
