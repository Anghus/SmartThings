/**
 *  Denon Receiver
 *
 *  Copyright 2016 Jerry Honeycutt
 *
 *  Version 0.1   21 Dec 2016
 *
 *	Version History
 *
 *  0.1		21 Dec 2016		Based on code by Kristopher Kubicki
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
metadata {
	definition(
    	name: "Denon Receiver",
        namespace: "Anghus", 
    	author: "Jerry Honeycutt") {
            capability "Actuator"
            capability "Switch"
            capability "Polling"
            capability "Switch Level"

            attribute "mute", "string"
            attribute "input", "string"
            attribute "lineIn", "enum"

            command "mute"
            command "unmute"
            command "toggleMute"
            command "inputSelect", ["string"]
            command "inputNext"
            command "inputSAT"
            command "inputDVD"
      	}

	simulator {}

	tiles(scale: 2) {
		multiAttributeTile(name:"master", type:"generic", width:6, height:4) {
			tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action: "switch.off", nextState:"off", backgroundColor:"#79b821", icon: "st.Electronics.electronics19"
				attributeState "off", label:'${name}', action: "switch.on", nextState:"on", backgroundColor:"#ffffff", icon: "st.Electronics.electronics19"
			}
			tileAttribute("device.level", key: "SLIDER_CONTROL") {
				attributeState "level", action:"setLevel", defaultState: true
			}
		}
        standardTile("input", "device.input", width: 2, height: 2, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
			state "input", label: '${currentValue}', action: "inputNext", backgroundColor: "#FFFFFF", icon: "st.Electronics.electronics6"
		}
        standardTile("mute", "device.mute", width: 2, height: 2, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
            state "muted", label: '${name}', action:"unmute", nextState: "unmuted", backgroundColor: "#79b821", icon: "st.Electronics.electronics16"
            state "unmuted", label: '${name}', action:"mute", nextState: "muted", backgroundColor: "#ffffff", icon: "st.Electronics.electronics16"
		}
		standardTile("poll", "device.poll", width: 2, height: 2, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
			state "poll", label: "", action: "polling.poll", icon: "st.secondary.refresh", backgroundColor: "#FFFFFF"
		}
        standardTile("input", "device.input", width: 2, height: 2, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
			state "input", label: "SAT/CBL", action: "inputSAT", backgroundColor: "#FFFFFF", icon: "st.Electronics.electronics6"
		}
        standardTile("input", "device.input", width: 2, height: 2, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
			state "input", label: "DVD", action: "inputDVD", backgroundColor: "#FFFFFF", icon: "st.Electronics.electronics6"
		}

		main "master"
        details(["master", "input","mute","poll"])
	}
}

preferences {
	section("IP") {
        input("destIp", "text", title: "IP", description: "The device IP")
        input("destPort", "number", title: "Port", description: "The port you wish to connect", defaultValue: 80)
    }
    section("DEBUG") {
        input("debug", "bool", title: "Debug handler?", description: "Enable debug output", defaultValue: false)
    }
}

/**/

def on() {
	trace("on()")
	sendEvent(name: "switch", value: "on")
	request('cmd0=PutZone_OnOff%2FON')
}

def off() { 
	trace("off()")
	sendEvent(name: "switch", value: 'off')
	request('cmd0=PutZone_OnOff%2FOFF')
}

/**/

def mute() {
	trace("mute()")
	sendEvent(name: "mute", value: "muted")
	request('cmd0=PutVolumeMute%2FON')
}

def unmute() { 
	trace("unmute()")
	sendEvent(name: "mute", value: "unmuted")
	request('cmd0=PutVolumeMute%2FOFF')
}

def toggleMute() {
	trace("toggleMute()")
    if(device.currentValue("mute") == "muted")
    	unmute()
	else
    	mute()
}

def setLevel(value) {
	trace("setLevel($value)")
	sendEvent(name: "mute", value: "unmuted")     
    sendEvent(name: "level", value: value)
	def int scaledValue = value - 80
	request("cmd0=PutMasterVolumeSet%2F$scaledValue")
}

/**/

def inputSAT() {
	trace("inputSAT()")
    inputSelect("SAT/CBL")
}

def inputDVD() {
	trace("inputDVD()")
    inputSelect("DVD")
}

def inputNext() { 
	trace("inputNext()")

	def cur = device.currentValue("input")
    def selectedInputs = device.currentValue("lineIn").substring(1,device.currentValue("lineIn").length()-1).split(', ').collect{it}
    selectedInputs.push(selectedInputs[0])
    debug("SELECTED: $selectedInputs")

    def semaphore = 0
    for(selectedInput in selectedInputs) {
    	if(semaphore == 1) { 
			debug("SELECT: ($semaphore) '$selectedInput'")
        	return inputSelect(selectedInput)
        }
    	if(cur == selectedInput) { 
        	semaphore = 1
        }
    }
}

def inputSelect(line) {
	trace("inputSelect($line)")
 	sendEvent(name: "input", value: line)
	request("cmd0=PutZone_InputFunction%2F$line")
}

/**/

def poll() {
	trace("poll()")
	refresh()
}

def refresh() {
	trace("refresh()")

    def hosthex = convertIPtoHex(destIp)
    def porthex = convertPortToHex(destPort)
    device.deviceNetworkId = "$hosthex:$porthex" 

    def hubAction = new physicalgraph.device.HubAction(
		'method': 'GET',
		'path': "/goform/formMainZone_MainZoneXml.xml",
		'headers': [ HOST: "$destIp:$destPort" ] 
		) 

    hubAction
}

def request(body) { 
	trace("request($body)")

    def hosthex = convertIPtoHex(destIp)
    def porthex = convertPortToHex(destPort)
    device.deviceNetworkId = "$hosthex:$porthex" 

    def hubAction = new physicalgraph.device.HubAction(
		'method': 'POST',
		'path': "/MainZone/index.put.asp",
		'body': body,
		'headers': [ HOST: "$destIp:$destPort" ]
		) 

    hubAction
}

def parse(String description) {
	trace("parse(description)")

 	def map = stringToMap(description)
    if(!map.body || map.body == "DQo=") { return }

	debug("${map.body}")
	def body = new String(map.body.decodeBase64())

	def statusrsp = new XmlSlurper().parseText(body)
	def power = statusrsp.Power.value.text()
    if(power == "ON") { 
    	sendEvent(name: "switch", value: 'on')
    }
    if(power != "" && power != "ON") { 
    	sendEvent(name: "switch", value: 'off')
    }

    def muteLevel = statusrsp.Mute.value.text()
    if(muteLevel == "on") { 
    	sendEvent(name: "mute", value: 'muted')
	}
    if(muteLevel != "" && muteLevel != "on") {
	    sendEvent(name: "mute", value: 'unmuted')
    }

	def inputCanonical = statusrsp.InputFuncSelect.value.text()
    def inputTmp = []

	//check to see if the VideoSelectLists node is available

	if(!statusrsp.VideoSelectLists.isEmpty()){
    	debug("VideoSelectLists is available... parsing")
        statusrsp.VideoSelectLists.value.each {
        	log.debug "$it"
            if(it.@index != "ON" && it.@index != "OFF") {
                inputTmp.push(it.'@index')
                debug("Adding Input ${it.@index}")
                if(it.toString().trim() == inputCanonical) {     
                    sendEvent(name: "input", value: it.'@index')
                }
            }
        }
    }

    //if the VideoSelectLists node is not available, let's try the InputFuncList

    else if(!statusrsp.InputFuncList.isEmpty()){
    	debug("InputFuncList is available... parsing")
        statusrsp.InputFuncList.value.each {
            if(it != "ON" && it != "OFF") {
                inputTmp.push(it)
                debug("Adding Input ${it}")
                if(it.toString().trim() == inputCanonical) {     
                    sendEvent(name: "input", value: it)
                }
            }
        }
    }

    sendEvent(name: "lineIn", value: inputTmp)

    if(statusrsp.MasterVolume.value.text()) { 
    	def int volLevel = (int) statusrsp.MasterVolume.value.toFloat() ?: -40.0
        volLevel = volLevel + 80
        
   		def int curLevel = 36
        try {
        	curLevel = device.currentValue("level")
        }
        catch(NumberFormatException nfe) { 
        	curLevel = 36
        }

        if(curLevel != volLevel) {
    		sendEvent(name: "level", value: volLevel)
        }
    } 
}

/**/

private String convertIPtoHex(ipAddress) { 
	trace("convertIPtoHex(ipAddress)")
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02X', it.toInteger() ) }.join()
    return hex
}

private String convertPortToHex(port) {
	trace("convertPortToHex(port)")
	String hexport = port.toString().format( '%04X', port.toInteger() )
    return hexport
}

/**/

private debug(message) {
	if(debug)
    	log.debug(message)
}

private trace(function) {
	if(debug)
    	log.trace(function)
}