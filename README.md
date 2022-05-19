# DataDog Integration

The DataDog integration plugin integrates with the DataDog monitoring solution to bring contextual workload data from DataDog into the Morpheus UI. The plugin adds a new DataDog tab to Morpheus instances that includes monitoring details pulled from DataDog.

## Requirements

The following requirements must be met prior to using the DataDog plugin:

* A DataDog account (https://www.datadoghq.com/).
* An API key and Application key that will be used by the plugin to perform API calls to the DataDog API (https://docs.datadoghq.com/account_management/api-app-keys/).
* A compatible Morpheus platform version. The Morpheus platform version must be equal to or higher than the version specified on the plugin.

## Installation

Once the plugin file is downloaded, browse to the Administration -> Integrations -> Plugins section in the Morpheus UI. Click the Upload File button to select your plugin and upload it. The plugin should now be loaded into the environment for use.

## Configuration

With the plugin installed, there are a few configuration steps required before you are able to use the plugin. 

### Plugin Settings

The plugin supports dynamic configuration via the plugin settings which are accessed by clicking on the pencil next to the plugin.

The following settings are available for the DataDog plugin:

|Setting|Description|Required|Default|
|---|---|---|---|
|API Key |The generated DataDog API key used to authenticate to the DataDog REST API |  Yes |n/a |
| Application Key | The generated DataDog Application key used to authenticate to the DataDog REST API | Yes |n/a |
| Environments | This toggles the visibility of the tab based upon the Morpheus environment the instance is in. Multiple environments are supported by adding multiple comma separated environments in the text box.| No| any|
| Groups | This toggles the visibility of the tab based upon the Morpheus group the instance is in. Multiple groups are supported by adding multiple comma separated groups in the text box.| No|any |
| Tags | This toggles the visibility of the tab based upon the tag(s) assigned to the instance. The tag key is what is used for the evaluation. Multiple tags are supported by adding multiple comma separated tags in the text box.|No| datadog |

### Permissions

The DataDog plugin adds a new RBAC permission to the Morpheus platform. 

**Permission Name**
```
DataDog Instance Tab	
```
The permissions supports FULL and NONE access types for which aligns to whether the user sees the instance tab or not.

## Usage

Once the plugin has been installed and configured, any instance in the appropriate group, environment and with the required tags will display the DataDog tab on the instance page.