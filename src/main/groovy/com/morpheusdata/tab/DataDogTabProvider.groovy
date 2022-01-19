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

		// Instantiate an object for storing data
		// passed to the html template
		ViewModel<Instance> model = new ViewModel<>()
		
		// Retrieve additional details about the instance
        // https://developer.morpheusdata.com/api/com/morpheusdata/model/TaskConfig.InstanceConfig.html
		TaskConfig instanceDetails = morpheus.buildInstanceConfig(instance, [:], null, [], [:]).blockingGet()

		// Define an object for storing the data retrieved
		// from the DataDog REST API
		def HashMap<String, String> dataDogPayload = new HashMap<String, String>();

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
		JsonSlurper slurper = new JsonSlurper()
		def settingsJson = slurper.parseText(settingsOutput)

		// Query the DataDog hosts REST API endpoint with a 
		// host filter using the name of the Morpheus instance.
		// The DataDog API endpoint expects headers of DD-APPLICATION-KEY
		// and DD-API-KEY for authentication purposes. 
		// The payload using the API and Application key from the plugin settings.
		// https://docs.datadoghq.com/api/latest/hosts/#get-all-hosts-for-your-organization
		def results = dataDogAPI.callApi("https://api.datadoghq.com", "api/v1/hosts?filter=host:${instance.name}", "", "", new RestApiUtil.RestOptions(headers:['Content-Type':'application/json','DD-API-KEY':settingsJson.ddApiKey,'DD-APPLICATION-KEY':settingsJson.ddAppKey], ignoreSSL: false), 'GET')

		// Parse the JSON response payload returned
		// from the REST API call.
		def json = slurper.parseText(results.content)

		// Evaluate whether the query returned a host.
		// If the host was not found then render the HTML template
		// for when a host isn't found
		if (json.host_list.size == 0){
			getRenderer().renderTemplate("hbs/instanceNotFoundTab", model)
		} else {
			// Store objects from the response payload
			def baseHost = json.host_list[0]
			def status = baseHost.up
			def agentVersion = baseHost.meta.agent_version
			def checks = baseHost.meta.agent_checks.size
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
			dataDogPayload.put("name", instance.name)
			dataDogPayload.put("apps", apps)
			dataDogPayload.put("muteStatus", muteStatus)
			dataDogPayload.put("agentVersion", agentVersion)
			dataDogPayload.put("checks", checks)
			dataDogPayload.put("agentChecks", agentChecks)
			dataDogPayload.put("status", status)
			dataDogPayload.put("cpuCores", cpuCores)
			dataDogPayload.put("logicalProcessors", logicalProcessors)
			dataDogPayload.put("operatingSystem", operatingSystem)
			dataDogPayload.put("memory", memory)
			dataDogPayload.put("platformDetails", platformDetails)
			dataDogPayload.put("cpuDetails", cpuDetails)
			dataDogPayload.put("memoryDetails", memoryDetails)

			// Set the value of the model object to the HashMap object
			model.object = dataDogPayload

			// Render the HTML template located in 
			// resources/renderer/hbs/instanceTab.hbs
			getRenderer().renderTemplate("hbs/instanceTab", model)
		}
	}


	// This method contains the logic for when the tab
	// should be displayed in the UI
	@Override
	Boolean show(Instance instance, User user, Account account) {
		// Set the tab to not be shown be default
		def show = false

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
		JsonSlurper slurper = new JsonSlurper()
		def settingsJson = slurper.parseText(settingsOutput)

		// Retrieve additional details about the instance
		TaskConfig config = morpheus.buildInstanceConfig(instance, [:], null, [], [:]).blockingGet()
		//println "tenant ${config.tenant}"
		//println "instance details ${config.instance.createdByUsername}"
		//println "customOptions ${config.customOptions}"
		//println "instance cloud ID ${instance.provisionZoneId}"

		// Only display the tab if the user
		// accessing the instance has the correct permission
		def permissionStatus = false
		if(user.permissions["datadog-instance-tab"] == "full"){
			permissionStatus = true
		}

		// Only display the tab if the instance
		// is part of the defined environment or any is specified.
		// The environment names should be provided in a comma seperated list
		// and we iterate through the list to remove any leading and trailing whitespaces
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

		// Only display the tab if the instance
		// is part of the defined groups or any is specified.
		// The group names should be provided in a comma seperated list
		// and we iterate through the list to remove any leading and trailing whitespaces
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

		// Only display the tab if the all
		// of the previous evaluations are true
		if(permissionStatus == true && 
		   environmentStatus == true && 
		   groupStatus == true
		  ){
			show = true
		}
		return show
	}
	/**
	 * Allows various sources used in the template to be loaded
	 * @return
	 */
	@Override
	ContentSecurityPolicy getContentSecurityPolicy() {
		def csp = new ContentSecurityPolicy()
		csp
	}
}