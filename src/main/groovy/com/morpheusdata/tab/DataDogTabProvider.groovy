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
		ViewModel<Instance> model = new ViewModel<>()
		
		// Retrieve additional details about the instance
		TaskConfig instanceDetails = morpheus.buildInstanceConfig(instance, [:], null, [], [:]).blockingGet()

		// Define an object for storing the data retrieved
		// from the DataDog REST API
		def HashMap<String, String> dataDogPayload = new HashMap<String, String>();

		// Define the Morpheus account/tenant in which
		// to search for the DataDog credentials
		def account = new Account(id: 1)
		def cypherAccess = new CypherAccess(account)

		// Retrieve the DataDog API key from Cypher
		// The plugin expects the name of the secret to be dd-api-key
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

		// Retrieve the DataDog Application key from Cypher
		// The plugin expects the name of the secret to be dd-application-key
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

		// Query the DataDog hosts REST API endpoint with a 
		// host filter using the name of the Morpheus instance.
		// The DataDog API endpoint expects headers of DD-APPLICATION-KEY
		// and DD-API-KEY for authentication purposes.
		def results = dataDogAPI.callApi("https://api.datadoghq.com", "api/v1/hosts?filter=host:${instance.name}", "", "", new RestApiUtil.RestOptions(headers:['Content-Type':'application/json','DD-API-KEY':apiKey,'DD-APPLICATION-KEY':appKey], ignoreSSL: false), 'GET')
		JsonSlurper slurper = new JsonSlurper()
		def json = slurper.parseText(results.content)
		println "host data: $json"

		// Evaluate whether the query returned a host.
		// If the host was not found then render the HTML template
		// for when a host isn't found
		if (json.host_list.size == 0){
			println "host data not found"
			getRenderer().renderTemplate("hbs/instanceNotFoundTab", model)
		} else {
			def baseHost = json.host_list[0]
			def status = baseHost.up
			def agentVersion = baseHost.meta.agent_version
			def checks = baseHost.meta.agent_checks.size
			def agentChecks = baseHost.meta.agent_checks
			def apps = baseHost.apps

			// Parse gohai data JSON payload
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

			// Set the values of the HashMap object
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

	@Override
	Boolean show(Instance instance, User user, Account account) {
		def show = false
		// Retrieve additional details about the instance
		TaskConfig config = morpheus.buildInstanceConfig(instance, [:], null, [], [:]).blockingGet()
		//println "tenant ${config.tenant}"
		//println "instance details ${config.instance.createdByUsername}"
		//println "customOptions ${config.customOptions}"
		//println "instance context ${config.instance.instanceContext}"
		//println "instance cloud ID ${instance.provisionZoneId}"

		def permissionStatus = false
		if(user.permissions["datadog-instance-tab"] == "full"){
			permissionStatus = true
		}
		println "Permission status ${permissionStatus}"

		// Only display the tab if the instance
		// is part of the defined environment or all is specified
		def tabEnvironments = ["all"]
		def environmentStatus = false
		if (tabEnvironments.contains("all")){
			environmentStatus = true
		} 
		if(tabEnvironments.contains(config.instance.instanceContext)){
			environmentStatus = true
		}

		// Only display the tab if the instance
		// is part of the defined groups or ALL is specified
		def tabGroups = ["all"]
	    def groupStatus = false
		if(tabGroups.contains("all")){
			groupStatus = true
		}
		if(tabGroups.contains(instance.site.name)){
			groupStatus = true
		}
		println "Group status ${groupStatus}"

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