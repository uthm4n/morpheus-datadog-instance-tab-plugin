package com.morpheusdata.tab

import com.morpheusdata.core.Plugin
import com.morpheusdata.model.Permission
import com.morpheusdata.views.HandlebarsRenderer
import com.morpheusdata.views.ViewModel

class DataDogTabPlugin extends Plugin {

	@Override
	void initialize() {
		DataDogTabProvider dataDogTabProvider = new DataDogTabProvider(this, morpheus)
		this.pluginProviders.put(dataDogTabProvider.code, dataDogTabProvider)
		this.setName("DataDog Tab Plugin")
		this.setDescription("Instance tab plugin displaying DataDog data")
		this.setPermissions([Permission.build('DataDog Instance Tab','datadog-instance-tab', [Permission.AccessType.none, Permission.AccessType.full])])
	}

	@Override
	void onDestroy() {
	}
}