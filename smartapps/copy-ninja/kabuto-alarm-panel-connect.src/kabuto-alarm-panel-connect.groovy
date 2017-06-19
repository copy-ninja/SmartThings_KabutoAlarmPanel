/**
 *  Kabuto Alarm Panel (Connect)
 *
 *  Copyright 2017 Jason Mok
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: 			"Kabuto Alarm Panel (Connect)",
    namespace: 		"copy-ninja",
    author: 		"Jason Mok",
    description: 	"Kabuto Alarm Panel",
    category: 		"Safety & Security",
	iconUrl: 		"http://www.copyninja.net/smartthings/icons/KabutoAlarmPanel.png",
	iconX2Url: 		"http://www.copyninja.net/smartthings/icons/KabutoAlarmPanel@2x.png",
	iconX3Url: 		"http://www.copyninja.net/smartthings/icons/KabutoAlarmPanel@3x.png",
    singleInstance: false
)
mappings {
	path("/sensors/:id/:state")	{ action: [ PUT: "updateSensor"] }
	path("/system/:state") 		{ action: [ PUT: "updateAlarmSystemStatus"] }
}
def getDeviceType() { return "urn:schemas-copyninja-net:device:AlarmPanel:1" }
preferences {
	page(name: "pageDiscovery",     install: false, uninstall: true, content: "pageDiscovery", 		nextPage: "pageConfiguration" )
	page(name: "pageConfiguration", install: true, 	uninstall: true, content: "pageConfiguration")
}
def pageDiscovery() {
	if(!state.accessToken) { createAccessToken() }	
	dynamicPage(name: "pageDiscovery", nextPage: "pageConfiguration", refreshInterval: 3) {
		alarmPanelDiscoverySubscribe()
		alarmPanelDiscover()
		alarmPanelVerify()
		def alarmPanels = pageDiscoveryGetAlarmPanels()
		section("Please wait while we discover your Kabuto Alarm Panel") {
			input(name: "selectedAlarmPanels", type: "enum", title: "Select Alarm Panel (${alarmPanels.size() ?: 0} found)", required: true, multiple: false, options: alarmPanels)
		}
	}	
}
def pageConfiguration() {
	getSelectedAlarmPanel()
	dynamicPage(name: "pageConfiguration", nextPage: "pageDiscovery" ) {
		section("Sensor Pin 1") {
			input(name: "sensorType_1", type: "enum", title:"Sensor Type", required: false, multiple: false, options: pageConfigurationGetSensorType(), submitOnChange: true)
			if (sensorType_1) { 
				input(name: "sensorName_1", type: "text", title:"Sensor Name", required: false)
			}
		}		
		section("Sensor Pin 2") {
			input(name: "sensorType_2", type: "enum", title:"Sensor Type", required: false, multiple: false, options: pageConfigurationGetSensorType(), submitOnChange: true)
			if (sensorType_2) { 
				input(name: "sensorName_2", type: "text", title:"Sensor Name", required: false)
			}
		}	
		section("Sensor Pin 5") {
			input(name: "sensorType_5", type: "enum", title:"Sensor Type", required: false, multiple: false, options: pageConfigurationGetSensorType(), submitOnChange: true)
			if (sensorType_5) { 
				input(name: "sensorName_5", type: "text", title:"Sensor Name", required: false)
			}
		}	
		section("Sensor Pin 6") {
			input(name: "sensorType_6", type: "enum", title:"Sensor Type", required: false, multiple: false, options: pageConfigurationGetSensorType(), submitOnChange: true)
			if (sensorType_6) { 
				input(name: "sensorName_6", type: "text", title:"Sensor Name", required: false)
			}
		}	
		section("Sensor Pin 7") {
			input(name: "sensorType_7", type: "enum", title:"Sensor Type", required: false, multiple: false, options: pageConfigurationGetSensorType(), submitOnChange: true)
			if (sensorType_7) { 
				input(name: "sensorName_7", type: "text", title:"Sensor Name", required: false)
			}
		}	
	}
}
Map pageConfigurationGetSensorType() { return [ "contact":"Open/Close Sensor", "motion":"Motion Sensor", "smoke":"Smoke Detector" ] }
Map pageDiscoveryGetAlarmPanels() {
	def alarmPanels = [:]
	def alarmPanelsVerified = getAlarmPanels().findAll{ it.value.verified == true }
	alarmPanelsVerified.each { alarmPanels["${it.value.mac}"] = it.value.name ?: "KabutoAlarmPanel_${it.value.mac[-6..-1]}" }
	return alarmPanels
}
def installed() { 
	initialize() 
	runEvery3Hours(alarmPanelDiscover)
}
def updated() { initialize() }
def initialize() {
	unsubscribe()
	unschedule()
	alarmPanelDiscoverySubscribe(true)	
	configureSensors()
    alarmPanelSyncSettings()
}
def alarmPanelDiscover() { sendHubCommand(new physicalgraph.device.HubAction("lan discovery ${getDeviceType()}", physicalgraph.device.Protocol.LAN)) }
void alarmPanelDiscoverySubscribe(force=false) {
	if (force) {
		unsubscribe()
		state.subscribe = false
	}
	if(!state.subscribe) {
		subscribe(location, "ssdpTerm.${getDeviceType()}", alarmPanelDiscoveryHandler, [filterEvents:false])
		state.subscribe = true
	}
}
def alarmPanelDiscoveryHandler(evt) {
	def event = parseLanMessage(evt.description)
	event << ["hub":evt?.hubId]
	def devices = getAlarmPanels()
	String ssdpUSN = event.ssdpUSN.toString()
	if (!devices."${ssdpUSN}") { devices << ["${ssdpUSN}": event] }
	if (state.alarmPanel) {
		if (state.alarmPanel.mac == event.mac) {
			if (state.alarmPanel.ip != event.networkAddress) or (state.alarmPanel.port != event.deviceAddress) {
				state.alarmPanel.ip   = event.networkAddress 
				state.alarmPanel.port = event.deviceAddress
				state.alarmPanel.host = "${convertHexToIP(event.networkAddress)}:${convertHexToInt(event.deviceAddress)}"
			}
		}
	}
}
def alarmPanelSystemStatusSubscribe() {
	subscribe(location, "alarmSystemStatus", alarmPanelSystemStatusHandler)
}
def alarmPanelSystemStatusHandler(evt) {
	// log.debug "Alarm Handler value: ${evt.value}"
	// if different from state.systemStatus
	// send status to alarm panel
	// set state.systemStatus = evt.value
}
def getSelectedAlarmPanel() {
	if (!state.alarmPanel) { 
		state.alarmPanel = []
		def selecteAlarmPanels = [] + settings.selectedAlarmPanels
		selecteAlarmPanels.each { alarmPanel -> 
			def selectedAlarmPanel = getAlarmPanels().find { it.value.mac == alarmPanel } 
			state.alarmPanel = [
				mac : selectedAlarmPanel.value.mac, 
				ip  : selectedAlarmPanel.value.networkAddress, 
				port: selectedAlarmPanel.value.deviceAddress,
				hub : selectedAlarmPanel.value.hub,
				host: "${convertHexToIP(selectedAlarmPanel.value.networkAddress)}:${convertHexToInt(selectedAlarmPanel.value.deviceAddress)}"
			]
		}
	} 
	return state.alarmPanel
}
def getAlarmPanels() {
	if (!state.devices) { state.devices = [:] }
	return state.devices
}
def alarmPanelVerify() {
	def alarmPanels = getAlarmPanels().findAll { it?.value?.verified != true }
	alarmPanels.each {
		String host = "${convertHexToIP(it.value.networkAddress)}:${convertHexToInt(it.value.deviceAddress)}"
		sendHubCommand(new physicalgraph.device.HubAction("""GET ${it.value.ssdpPath} HTTP/1.1\r\nHOST: ${host}\r\n\r\n""", physicalgraph.device.Protocol.LAN, host, [callback: alarmPanelVerifyHandler]))
	}
}
void alarmPanelVerifyHandler(physicalgraph.device.HubResponse hubResponse) {
	def body = hubResponse.xml
	def devices = getAlarmPanels()
	def device = devices.find { it?.key?.contains(body?.device?.UDN?.text()) }
	if (device) { device.value << [name: body?.device?.roomName?.text(), model:body?.device?.modelName?.text(), serialNumber:body?.device?.serialNum?.text(), verified: true] }
}
private Integer convertHexToInt(hex) { Integer.parseInt(hex,16) }
private String convertHexToIP(hex) { [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".") }
void alarmPanelSyncSettings() {
	if(!state.accessToken) { createAccessToken() }	
	def body = [
		token : state.accessToken,
		apiUrl : apiServerUrl + "/api/smartapps/installations/" + app.id,
		sensors : []
	]
	getAllChildDevices().each { body.sensors = body.sensors + [ pin : it.deviceNetworkId.split("\\|")[1] ] }
	def selectedAlarmPanel = getSelectedAlarmPanel()
	sendHubCommand(new physicalgraph.device.HubAction([method: "POST", path: "/SyncSettings", headers: [ HOST: selectedAlarmPanel.host, "Content-Type": "application/json" ], body : groovy.json.JsonOutput.toJson(body)], selectedAlarmPanel.host))
}
def configureSensors() {
	getAllChildDevices().each { deleteChildDevice(it.deviceNetworkId) }
	for (i in 1..8) {	
		def sensorDNI = [ state.alarmPanel?.mac, "${i}"].join('|') 
		if (settings."sensorType_${i}") {
			if (!getChildDevice(sensorDNI)) {
				def sensorType = ""
				switch(settings."sensorType_${i}") {
					case "contact": 
						sensorType = "Kabuto Contact Sensor"
						break
					case "motion": 
						sensorType = "Kabuto Motion Sensor"
						break
					case "smoke":
						sensorType = "Kabuto Smoke Sensor"
						break
				}
				addChildDevice("copy-ninja", sensorType, sensorDNI, state.alarmPanel?.hub, [ "label": settings."sensorName_${i}" ?: sensorType ]) 
			}
		} 
	}
}
def updateSensor() {
	def sensorDNI = [ state.alarmPanel?.mac, params.id].join('|')
	def sensor = getChildDevice(sensorDNI)
	if (sensor) sensor.setStatus(params.state)
}
def updateAlarmSystemStatus() {
	//kabuto sends in alarm status initiated from kabuto
	//set state.systemStatus = status
	//send location event
	//sendLocationEvent(name: "alarmSystemStatus", value: "away")
	//sendLocationEvent(name: "alarmSystemStatus", value: "stay")
	//sendLocationEvent(name: "alarmSystemStatus", value: "off")
}