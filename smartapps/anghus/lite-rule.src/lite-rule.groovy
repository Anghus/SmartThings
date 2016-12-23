/**
 *  Lite Rule
 *
 *  Copyright 2016 Jerry Honeycutt
 *
 *  Version 0.1   8 Dec 2016
 *
 *	Version History
 *
 *  0.1		08 Dec 2016		Initial version
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
    name: "Lite Rule",
    namespace: "Anghus",
    author: "Jerry Honeycutt",
    description: "Manage lights by using motion sensors and other devices' events.",
    category: "My Apps",

    parent: "Anghus:Home Rules",
    iconUrl: "http://cdn.device-icons.smartthings.com/Home/home2-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home2-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Home/home2-icn@3x.png")

/**/

preferences {
	page(name: "lightsPage")
    page(name: "triggerPage")
	page(name: "installPage")
}

def lightsPage() {
	dynamicPage(name: "lightsPage", nextPage: "triggerPage", uninstall: true) {
        section("LIGHTS") {
            input name: "lights", type: "capability.switch", title: "Lights", submitOnChange: true, multiple: true, required: true
            if(lights) {
            	input name: "actionType", type: "enum", title: "Type of action?", options: actionTypes(), submitOnChange: true, required: true
                switch(actionType) {
            		case "Switch":
                    	input name: "action", type: "enum", title: "Turn on or off?", options: ["Turn on", "Turn off"], defaultValue: "Turn on", required: true
                        break;
                }
            }
        }
	}
}
def actionTypes() { ["Switch"] }

def triggerPage() {
	dynamicPage(name: "triggerPage", nextPage: "installPage", uninstall: true) {
        section("TRIGGER") {
            input name: "triggerType", type: "enum", title: "Type of trigger?", options: triggerTypes(), submitOnChange: true, required: true
		}
        switch(triggerType) {
            case "Motion":
                section("MOTION") {
                    input name: "motionSensors", type: "capability.motionSensor", title: "Motion sensors", multiple: true, submitOnChange: true, required: true
                    if(motionSensors) {
                        input name: "motionScope", type: "enum", title: "How many?", options: triggerScope(), defaultValue: "Any", required: true
                        input name: "motionDir", type: "enum", title: "When motion", options: motionTriggers(), defaultValue: "Starts", required: true
                    }
                }
				if(motionSensors) {
                    section("TOGGLE") {
                        input name: "motionToggle", type: "bool", title: "Toggle after state change?", default: false, submitOnChange: true, required: true
                        if(motionToggle)
                            input name: "motionTimeout", type: "number", title: "Delay (minutes)", defaultValue: 5, required: true
                    }
                }
				break;

            case "Switch":
                section("SWITCH") {
                    input name: "switches", type: "capability.switch", title: "Switches", multiple: true, submitOnChange: true, required: true
                    if(switches) {
                        input name: "switchScope", type: "enum", title: "How many?", options: triggerScope(), defaultValue: "Any", required: true
                        input name: "switchState", type: "enum", title: "When switches", options: switchTriggers(), defaultValue: "Turn on", required: true
                    }
                }
				if(switches) {
                    section("TOGGLE") {
                        input name: "switchToggle", type: "bool", title: "Toggle after state change?", default: false, submitOnChange: true, required: true
                        if(switchToggle)
                            input name: "switchTimeout", type: "number", title: "Delay (minutes)", defaultValue: 5, required: true
                    }
                }
				break;
		}
	}
}
def triggerScope() 		{ ["Any", "All"] }
def triggerTypes() 		{ ["Motion", "Switch" ] }
def motionTriggers()	{ ["Starts", "Stops"] }
def switchTriggers()	{ ["Turn on", "Turn off"] }

def installPage() {
	dynamicPage(name: "installPage", uninstall: true, install: true) {
    	section("NAME") {
        	label title: "Rule name", defaultValue: "$action ${lights[0]}", required: false
        }
        section("MODES") {
			input name: "modes", type: "mode", title: "During which modes?", multiple: true, required: false
        }
        section("DEBUG") {
        	input name: "debug", type: "bool", title: "Debug rule?", defaultValue: false, required: true
        }
    }
}

/**/

def installed() {
	trace("installed()")
	initialize()
}

def updated() {
	trace("updated()")
    unschedule()
	unsubscribe()
	initialize()
}

def initialize() {
	trace("initialize()")

	// State variables:
	// Event subscriptions:

	if(motionSensors) subscribe(motionSensors, "motion", motionEvent)
	if(switches) subscribe(switches, "switch", switchEvent)
}

/**/

def motionEvent(evt) {
	trace("motionEvent($evt.value)")

	if((triggerType == "Motion") && inMode() && evt.isStateChange) {
        if(motionRule()) {
            debug("Evaluating $motionSensors")
            debug("Motion needs to ${translate(motionDir)}")
            debug("Motion is currently $evt.value")
            debug("Running $lights")

            doLight()
        }
        else
            if(motionToggle) {
            	debug("Toggling after $motionTimeout minutes")
	            runIn(motionTimeout*60, doLight, [overwrite: true, data: [toggle: true]])
        	}
    }
}

def motionRule() {
	trace("motionRule()")

	def result = true
	def value = translate(motionDir)

	if(motionSensors) {
        def all = true
        def any = false

        motionSensors.each {

            // Any matching value will change $any to true.
			// Any non-matching value will change $all to false.

            any |= (it.currentValue("motion") == value)
            all &= (it.currentValue("motion") == value)
        }
		result = (motionScope == "All") ? all : any
	}
	debug("Rule '${motionScope.toLowerCase()} are $value' is $result")
	return result
}

/**/

def switchEvent(evt) {
	trace("switchEvent($evt.value)")

	if((triggerType == "Switch") && inMode() && evt.isStateChange) {
        if(switchRule()) {
            debug("Evaluating $switches")
            debug("Switch needs to be ${translate(switchState)}")
            debug("Switch is currently $evt.value")
            debug("Running $lights")

            doLight()
        }
        else
            if(switchToggle) {
				debug("Toggling after $switchTimeout minutes")
                runIn(switchTimeout*60, doLight, [overwrite: true, data: [toggle: true]])
			}
    }
}

def switchRule() {
	trace("switchRule()")

	def result = true
	def value = translate(switchState)

	if(switches) {
        def all = true
        def any = false

        switches.each {

            // Any matching value will change $any to true.
			// Any non-matching value will change $all to false.

            any |= (it.currentValue("switch") == value)
            all &= (it.currentValue("switch") == value)
        }
		result = (switchScope == "All") ? all : any
	}
	debug("Rule '${switchScope.toLowerCase()} are $value' is $result")
	return result
}

/**/

def doLight(data) {
	trace("doLight(${data?.toggle})")
    debug("Action type is $actionType")

	lights.each {
    	debug("$it.label is ${it.currentValue('switch')}")
    	switch(actionType) {
        	case "Switch":
            	if(action == "Turn on") {
					// Don't touch the light if it's already on.
                	if(it.currentValue("switch") != "on") {
                    	debug("Turning ON $it.label")
                		it.on()
                    }
                    else if(data?.toggle) {
                    	debug("Toggling OFF $it.label")                    
                    	it.off()
					}
				}
                else if(action == "Turn off") {
                	// Don't touch the light if it's already off.
                	if(it.currentValue("switch") != "off") {
                    	debug("Turning OFF $it.label")
                		it.off()
                    }
                    else if (data?.toggle) {
                    	debug("Toggling ON $it.label")                    
                    	it.on()
                    }
				}
            	break;
        }
    }
}

/**/

private translate(state) {
	def value = ""
    switch(state) {
    	case "Turn on"	: value = "on"; 		break
        case "Turn off"	: value = "off"; 		break;
        case "Starts"	: value = "active"; 	break;
        case "Stops"	: value = "inactivce"; 	break;
    }
    return value
}

private inMode() {
	return (!modes || (modes && location.mode in modes))
}

private debug(message) {
	if(debug)
    	log.debug(message)
}

private trace(function) {
	if(debug)
    	log.trace(function)
}