package com.morpheusdata.tab

import com.morpheusdata.core.AbstractInstanceTabProvider
import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.core.Plugin
import com.morpheusdata.core.cypher.CypherAccess
import com.morpheusdata.model.Account
import com.morpheusdata.model.Instance
import com.morpheusdata.model.TaskConfig
import com.morpheusdata.model.ContentSecurityPolicy
import com.morpheusdata.model.User
import com.morpheusdata.views.HTMLResponse
import com.morpheusdata.views.ViewModel
import com.morpheusdata.core.util.RestApiUtil
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.reactivex.Single
import groovy.util.logging.Slf4j
import org.yaml.snakeyaml.*

@Slf4j
class DataDogTabProvider extends AbstractInstanceTabProvider {
	Plugin plugin
	MorpheusContext morpheus
	RestApiUtil dataDogAPI

	String code = 'datadog-tab'
	String name = 'DataDog'

	DataDogTabProvider(Plugin plugin, MorpheusContext context) {
		this.plugin = plugin
		this.morpheus = context
		this.dataDogAPI = new RestApiUtil()
	}

	DataDogTabProvider(Plugin plugin, MorpheusContext morpheusContext, RestApiUtil api, Account account) {
		this.morpheusContext = morpheusContext
		this.plugin = plugin
		this.dataDogAPI = api
	}

	@Override
	HTMLResponse renderTemplate(Instance instance) {

		// Instantiate an object for storing data passed to the html template
		ViewModel<Instance> model = new ViewModel<>()
		
		// Retrieve additional details about the instance
        // https://developer.morpheusdata.com/api/com/morpheusdata/model/TaskConfig.InstanceConfig.html
		TaskConfig instanceDetails = morpheus.buildInstanceConfig(instance, [:], null, [], [:]).blockingGet()

		// Define an object for storing the data retrieved from the DataDog REST API
		def HashMap<String, String> dataDogPayload = new HashMap<String, String>();

		try {
			// Retrieve plugin settings
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

			// Parse the plugin settings payload. The settings will be available as
			// settingsJson.$optionTypeFieldName i.e. - settingsJson.ddApiKey to retrieve the DataDog API key setting
			log.info("DataDog Plugin: Parsing Settings JSON")
			JsonSlurper slurper = new JsonSlurper()
			def settingsJson = slurper.parseText(settingsOutput)

			// Check if the API and Application keys have been set for the plugin
			if (settingsJson.ddApiKey == "" || settingsJson.ddAppKey == ""){
				log.info("DataDog Plugin: DataDog API credentials are not set. Ensure that the plugin settings have been configured.")
				return getRenderer().renderTemplate("hbs/instanceNotFoundTab", model)
			}

			// Check if the API endpoint provided is valid
			def apiEndpoints = ["datadoghq.com", "us3.datadoghq.com", "us5.datadoghq.com", "datadoghq.eu","ddog-gov.com","ap1.datadoghq.com"]
			if (!apiEndpoints.contains(settingsJson.ddApiEndpoint)){
				log.info("DataDog Plugin: The configured endpoint of ${settingsJson.ddApiEndpoint} is not one of the valid endpoints - ${apiEndpoints}.")
				return getRenderer().renderTemplate("hbs/instanceNotFoundTab", model)
			}

			// Parse the custom settings
			Yaml yaml = new Yaml();
			Map<String, Object> obj = yaml.load(settingsJson.ddPluginCustomization);

			// Primary to a tag associated with the instance
			// Secondary to an evironment variable associated with the instance
			// Default to the Morpheus instance name
			def normalizedInstanceName = ""

			if (obj != null){
				if (obj.instanceName != null){
					def nameSelector = obj.instanceName.property

					// Set the hostname to query the DataDog API
					// using a defined tag
					def tags = instanceDetails.instance.metadata
					if(nameSelector == "tag"){
						if(tags.containsKey(obj.instanceName.value)){
							normalizedInstanceName = tags[obj.instanceName.value]
							log.info("DataDog Plugin: Using ${normalizedInstanceName} as the DataDog hostname due to the tag ${obj.instanceName.value}")
						}
					}

					// Set the hostname to query the DataDog API
					// using a defined evironment variable
					def evars = instanceDetails.instance.evars
					if (nameSelector == "evar"){
						normalizedInstanceName = evars[obj.instanceName.value]
						log.info("DataDog Plugin: Using ${normalizedInstanceName} as the DataDog hostname due to the evar ${obj.instanceName.value}")
					}
				}
			}

			if (normalizedInstanceName == "") {
		        normalizedInstanceName = instance.name.toLowerCase()
			    log.info("DataDog Plugin: Using ${normalizedInstanceName} as the DataDog hostname due to no additional settings being defined")
			}

			def appSubdomain = "app"

			if (obj != null){
				if (obj.tenantMetadata != null){
					def tenantMetadata = obj.tenantMetadata
					for(tenant in tenantMetadata){
						if (tenant["name"].toLowerCase() == instance.account.name.toLowerCase()){
							appSubdomain = tenant["dataDogSubdomain"]
						}
					}
				}
			}

			// Query the DataDog hosts REST API endpoint with a 
			// host filter using the name of the Morpheus instance.
			// The DataDog API endpoint expects headers of DD-APPLICATION-KEY and DD-API-KEY for authentication purposes. 
			// The payload using the API and Application key from the plugin settings.
			// https://docs.datadoghq.com/api/latest/hosts/#get-all-hosts-for-your-organization
			def apiURL = "https://api.${settingsJson.ddApiEndpoint}"
			def appURL = "https://${appSubdomain}.${settingsJson.ddApiEndpoint}"

			log.info("DataDog Plugin: Configured API endpoint ${apiURL}")
			log.info("DataDog Plugin: Configured App endpoint ${appURL}")
			def results = dataDogAPI.callApi(apiURL, "api/v1/hosts", "", "", new RestApiUtil.RestOptions(queryParams:['filter':normalizedInstanceName],headers:['Content-Type':'application/json','DD-API-KEY':settingsJson.ddApiKey,'DD-APPLICATION-KEY':settingsJson.ddAppKey], ignoreSSL: false), 'GET')

			if (results.success == false){
				log.info("DataDog Plugin: Error making the REST API call ${results.errorCode}")
				return getRenderer().renderTemplate("hbs/instanceNotFoundTab", model)
			}

			// Parse the JSON response payload returned from the REST API call.
			def json = slurper.parseText(results.content)

			// Evaluate whether the query returned a host.
			// If the host was not found then render the HTML template for when a host isn't found.
			if (json.total_returned == 0){
				getRenderer().renderTemplate("hbs/instanceNotFoundTab", model)
			} else {
				// Store objects from the response payload
				def baseHost = json.host_list[0]
				def hostId = baseHost.id
				def status = baseHost.up
				def agentVersion = baseHost.meta.agent_version
				def checks = 0
				if (baseHost.meta.agent_checks == null){
				  checks = 0
				} else {
				  checks = baseHost.meta.agent_checks.size
				}
				def agentChecks = baseHost.meta.agent_checks
				def apps = baseHost.apps

				// Parse the gohai data JSON payload.
				// This step is necessary because there is
				// a JSON payload embedded in the original response
				// as a string which needs to be parsed.
				def gohaidata = slurper.parseText(baseHost.meta.gohai)
				def cpuCores = gohaidata.cpu.cpu_cores
				def logicalProcessors = gohaidata.cpu.cpu_logical_processors
				def memory = gohaidata.memory.total
				try {
					Integer.parseInt(memory)
				} catch(NumberFormatException ex) {
				    if(memory.isNumber()) {                                                     
				        memory = memory.toLong()
				    } else {                                                                   	 
				        memory = gohaidata.memory.total.replaceAll("[A-Za-z]", "").toLong() 		      // assume value has a unit symbol appended e.g. "34359738368234kB" and convert to Long
				    }
				}
				def formattedMemory = (memory/1000000000).round(2)  
				def operatingSystem = gohaidata.platform.os
				def cpuDetails = gohaidata.cpu
				def platformDetails = gohaidata.platform
				def memoryDetails = gohaidata.memory

				// Evaluate if the DataDog host mute status is true or false
				// and pass muted or unmuted instead of true or false to the html template
				def muteStatus = ""
				if (baseHost.is_muted == true){
					muteStatus = "muted"
				} else {
					muteStatus = "unmuted"
				}

				// Set the values of the HashMap object (defined on line #51)
				// that will be used to populate the HTML template
				dataDogPayload.put("id", instance.id)
				dataDogPayload.put("hostId", hostId)
				dataDogPayload.put("name", normalizedInstanceName)
				dataDogPayload.put("apps", apps)
				dataDogPayload.put("muteStatus", muteStatus)
				dataDogPayload.put("agentVersion", agentVersion)
				dataDogPayload.put("checks", checks)
				dataDogPayload.put("agentChecks", agentChecks)
				dataDogPayload.put("status", status)
				dataDogPayload.put("cpuCores", cpuCores)
				dataDogPayload.put("logicalProcessors", logicalProcessors)
				dataDogPayload.put("operatingSystem", operatingSystem)
				dataDogPayload.put("memory", formattedMemory.round(2))
				dataDogPayload.put("platformDetails", platformDetails)
				dataDogPayload.put("cpuDetails", cpuDetails)
				dataDogPayload.put("memoryDetails", memoryDetails)
				dataDogPayload.put("appUrl", appURL)

				// Set the value of the model object to the HashMap object
				model.object = dataDogPayload

				// Render the HTML template located in 
				// resources/renderer/hbs/instanceTab.hbs
				getRenderer().renderTemplate("hbs/instanceTab", model)
			}
		}
		catch(Exception ex) {
          	log.error("DataDog Plugin: Error in fetching data from DataDog ${ex}")
		  	getRenderer().renderTemplate("hbs/instanceNotFoundTab", model)
        }
	}


	// This method contains the logic for when the tab
	// should be displayed in the UI
	@Override
	Boolean show(Instance instance, User user, Account account) {
		// Set the tab to not be shown be default
		def show = false

        // Retrieve plugin settings
		try {
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

			// Retrieve additional details about the instance
			TaskConfig config = morpheus.buildInstanceConfig(instance, [:], null, [], [:]).blockingGet()

			// Only display the tab if the user
			// accessing the instance has the correct permission
			def permissionStatus = false
			if(user.permissions["datadog-instance-tab"] == "full"){
			    permissionStatus = true
			}

			// Only display the tab if the instance is part of the defined environment or any is specified.
			// The environment names should be provided in a comma seperated list
			// and we iterate through the list to remove any leading and trailing whitespaces
			def tabEnvironments = settingsJson.environmentVisibilityField.split(",");
			def visibleEnvironments = []
			for(environment in tabEnvironments){
				visibleEnvironments.add(environment.trim())
			}
			def environmentStatus = false
			if (visibleEnvironments.contains("any")){
				environmentStatus = true
			}
			if(visibleEnvironments.contains(config.instance.instanceContext)){
				environmentStatus = true
			}

			// Only display the tab if the instance is part of the defined groups or any is specified.
			// The group names should be provided in a comma seperated list
			// and we iterate through the list to remove any leading and trailing whitespaces
			def tabGroups = settingsJson.groupVisibilityField.split(",");
			def visibleGroups = []
			for(group in tabGroups){
				visibleGroups.add(group.trim())
			}
			def groupStatus = false
			if(visibleGroups.contains("any")){
				groupStatus = true
			}
			if(visibleGroups.contains(instance.site.name)){
				groupStatus = true
			}

			// Only display the tab if the instance
			// has a tag in the defined tags or any is specified.
			// The tag names should be provided in a comma seperated list
			// and we iterate through the list to remove any leading and trailing whitespaces
			def tabTags = settingsJson.tagVisibilityField.split(",");
			def visibleTags = []
			for(tag in tabTags){
				visibleTags.add(tag.trim())
			}
			def tagStatus = false
			if(visibleTags.contains("any")){
				tagStatus = true
			}

			def tags = config.instance.metadata
			for(tag in visibleTags){
				if(tags.containsKey(tag)){
					tagStatus = true
				}
			}
	
			// Only display the tab if the instance
			// has a label in the defined labels or any is specified.
			// The label names should be provided in a comma seperated list
			// and we iterate through the list to remove any leading and trailing whitespaces
			def tabLabels = settingsJson.labelVisibilityField.split(",");
			def visibleLabels = []
			for(label in tabLabels){
				visibleLabels.add(label.trim())
			}
			def labelStatus = false
			if(visibleLabels.contains("any")){
				labelStatus = true
			}

			def labels = instance.tags
			if (labels != null){
				ArrayList<String> labelArray = new  ArrayList<String>(Arrays.asList(labels.split(",")));
				if (labelArray.size > 0) {
					for(label in visibleLabels){
						if(labelArray.contains(label)){
							labelStatus = true
						}
					}
				}
			}

			// Only display the tab if the all of the previous evaluations are true
			if(permissionStatus == true && environmentStatus == true &&
			   tagStatus == true && groupStatus == true && labelStatus == true
			){
				show = true
			}
		}
		catch(Exception ex) {
          log.info("DataDog Plugin: Error parsing the DataDog plugin settings. Ensure that the plugin settings have been configured. ${ex}")
        }
		return show
	}

	@Override
	ContentSecurityPolicy getContentSecurityPolicy() {
		def csp = new ContentSecurityPolicy()
		csp
	}
}
