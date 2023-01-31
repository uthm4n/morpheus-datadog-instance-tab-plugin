## 0.0.8 (Janurary 31, 2023)

CHANGES:

* Fix an issue where an instance with no labels created an issue with the visibility logic.

## 0.0.7 (Janurary 31, 2023)

CHANGES:

* Added support for determining the tab's visibility based on instance labels.
* Added support for setting the hostname used to query the DataDog API based upon a tag or label.

## 0.0.6 (Janurary 27, 2023)

CHANGES:

* Added support for additional DataDog API endpoints outside of US-1 and Government.
* Added additional logging to better troubleshoot host not found issues.

## 0.0.5 (December 1, 2022)

CHANGES:

* Fixed a bug with the API endpoints option list causing a 500 error to be thrown when the plugin is marked as disabled. The select list for the API endpoint has been migrated to an optional checkbox for the GovCloud API endpoint.

## 0.0.4 (November 30, 2022)

CHANGES:

* Added a plugin setting for toggling between public (datadoghq.com) and govcloud (ddog-gov.com) API endpoints.

## 0.0.3 (May 18, 2022)

CHANGES:

* Added additional error handling to log when the API and Application key settings are not set
* Added additional error handling to log when invalid credentials are used
* Updated the default tag visibility setting from any to datadog

## 0.0.2 (April 28, 2022)

CHANGES:

* Added support for the new plugin settings to enable the runtime configuration of the plugin without the need to rebuild
* Updated the Morpheus plugin API version from 0.11.x to 0.12.x
* Migrated the configuration of the API credentials from Cypher to the plugin settings
* Updated the visibility code to strip leading and trailing whitespaces from the group and environment visibility settings

## 0.0.1 (December 14, 2021)

CHANGES:

* Initial code release
