package com.morpheusdata.tab

import com.morpheusdata.core.Plugin
import com.morpheusdata.model.Permission
import com.morpheusdata.views.HandlebarsRenderer
import com.morpheusdata.views.ViewModel
import com.morpheusdata.web.Dispatcher
import com.morpheusdata.model.OptionType

class DataDogTabPlugin extends Plugin {

	@Override
	String getCode() {
		return 'datadog-tab-plugin'
	}

	@Override
	void initialize() {
		DataDogTabProvider dataDogTabProvider = new DataDogTabProvider(this, morpheus)
		this.pluginProviders.put(dataDogTabProvider.code, dataDogTabProvider)
		this.setName("DataDog Tab Plugin")
		this.setDescription("Instance tab plugin displaying DataDog data")
		this.setAuthor("Martez Reed")
		this.setSourceCodeLocationUrl("https://github.com/martezr/morpheus-datadog-instance-tab-plugin")
		this.setIssueTrackerUrl("https://github.com/martezr/morpheus-datadog-instance-tab-plugin/issues")
		this.setPermissions([Permission.build('DataDog Instance Tab','datadog-instance-tab', [Permission.AccessType.none, Permission.AccessType.full])])
		// Plugin settings the are used to configure the behavior of the plugin
		// https://developer.morpheusdata.com/api/com/morpheusdata/model/OptionType.html
		this.settings << new OptionType(
			name: 'API Key',
			code: 'datadog-plugin-api-key',
			fieldName: 'ddApiKey',
			displayOrder: 0,
			fieldLabel: 'API Key',
			helpText: 'The DataDog API key',
			required: true,
			inputType: OptionType.InputType.PASSWORD
		)

		this.settings << new OptionType(
			name: 'Application Key',
			code: 'datadog-plugin-application-key',
			fieldName: 'ddAppKey',
			displayOrder: 1,
			fieldLabel: 'Application Key',
			helpText: 'The DataDog Application key',
			required: true,
			inputType: OptionType.InputType.PASSWORD
		)

		this.settings << new OptionType(
			name: 'Visible Environments',
			code: 'datadog-plugin-environments-field',
			fieldName: 'environmentVisibilityField',
			defaultValue: 'any',
			displayOrder: 2,
			fieldLabel: 'Environments',
			fieldGroup: 'Visibility Settings',
			helpText: 'List of the names of the Morpheus environments the tab is visible (i.e. - production,qa,dev)',
			required: false,
			inputType: OptionType.InputType.TEXT
		)

		this.settings << new OptionType(
			name: 'Visible Groups',
			code: 'datadog-plugin-groups-field',
			fieldName: 'groupVisibilityField',
			defaultValue: 'any',
			displayOrder: 3,
			fieldLabel: 'Groups',
			fieldGroup: 'Visibility Settings',
			helpText: 'List of the names of the Morpheus groups the tab is visible (i.e. - devs,admins,security)',
			required: false,
			inputType: OptionType.InputType.TEXT
		)

		this.settings << new OptionType(
			name: 'Visible Tags',
			code: 'datadog-plugin-tags-field',
			fieldName: 'tagVisibilityField',
			defaultValue: 'datadog',
			displayOrder: 3,
			fieldLabel: 'Tags',
			fieldGroup: 'Visibility Settings',
			helpText: 'List of the names of the Morpheus tags the tab is visible (i.e. - datadog,monitoring,observability)',
			required: false,
			inputType: OptionType.InputType.TEXT
		)
	}

	@Override
	void onDestroy() {}
}