package com.morpheusdata.tab

import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.core.Plugin
import com.morpheusdata.model.*
import groovy.util.logging.Slf4j
import com.morpheusdata.core.OptionSourceProvider

@Slf4j
class DataDogAPIEndpointOptionSourceProvider implements OptionSourceProvider {

	Plugin plugin
	MorpheusContext morpheusContext

	DataDogAPIEndpointOptionSourceProvider(Plugin plugin, MorpheusContext context) {
		this.plugin = plugin
		this.morpheusContext = context
	}

	@Override
	MorpheusContext getMorpheus() {
		return this.morpheusContext
	}

	@Override
	Plugin getPlugin() {
		return this.plugin
	}

	@Override
	String getCode() {
		return 'datadog-tab-plugin'
	}

	@Override
	String getName() {
		return 'DataDog API Endpoints'
	}

	@Override
	List<String> getMethodNames() {
		return new ArrayList<String>(['datadogApiEndpoints'])
	}

	List datadogApiEndpoints(args) {
		[
				[name: 'Public (datadoghq.com)', value: 'datadoghq.com'],
				[name: 'GovCloud (ddog-gov.com)', value: 'ddog-gov.com']
		]
	}
}