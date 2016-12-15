/**
 *  Mode Rule
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
    name: "Mode Rule",
    namespace: "Anghus",
    author: "Jerry Honeycutt",
    description: "Change modes automatically based upon presence devices, motion sensors, and the time of day.",
    category: "My Apps",

    parent: "Anghus:Home Rules",
    iconUrl: "http://cdn.device-icons.smartthings.com/Home/home2-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home2-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Home/home2-icn@3x.png")

/**/

preferences {
	page(name: "schedulePage")
    page(name: "presencePage")
	page(name: "motionPage")
    page(name: "actionsPage")
	page(name: "notifyPage")
	page(name: "installPage")
}

def schedulePage() {
	dynamicPage(name: "schedulePage", nextPage: "presencePage", uninstall: true) {
    	section("START") {
    		input name: "startType", type: "enum", title: "Value type", options: ["Specific", "Sunrise", "Sunset"], submitOnChange: true, required: true
			switch(startType) {
            	case "Specific":
                	input name: "startTime", type: "time", title: "Specific time", required: true
                	break;
				case "Sunrise":
                case "Sunset":
                	input name: "startOffset", type: "number", title: "Offset (+/- minutes)", range: "-60..60", defaultValue: 0, required: true
                    break;
            }
        }
    	section("FINISH") {
    		input name: "finishType", type: "enum", title: "Value type", options: ["Specific", "Sunrise", "Sunset"], submitOnChange: true, required: true
			switch(finishType) {
            	case "Specific":
                	input name: "finishTime", type: "time", title: "Specific time", required: true
                	break;
				case "Sunrise":
                case "Sunset":
                	input name: "finishOffset", type: "number", title: "Offset (+/- minutes)", range: "-60..60", defaultValue: 0, required: true
                    break;
            }
        }

    }
}

def presencePage() {
	dynamicPage(name: "presencePage", nextPage: motionPage, uninstall: true) {
    	section("PRESENCE") {
			input name: "presenceSensors", type: "capability.presenceSensor", title: "People", multiple: true, submitOnChange: true, required: false
        }
		if(presenceSensors) {
            section("RULE") {
                    input name: "presenceScope", type: "enum", title: "Scope", options: ["Any", "All"], defaultValue: "Any", required: true
                    input name: "presenceComparison", type: "enum", title: "Comparison", options: ["Are", "Are not"], defaultValue: "Are", required: true
                    input name: "presenceValue", type: "enum", title: "Presence", options: ["Present", "Not present"], defaultValue: "Present", required: true
			}
        }
	}
}

def motionPage() {
	dynamicPage(name: "motionPage", nextPage: "actionsPage", uninstall: true) {
    	section("MOTION") {
			input name: "motionSensors", type: "capability.motionSensor", title: "Motion sensors", multiple: true, submitOnChange: true, required: false
        }
        if(motionSensors) {
        	section("RULE") {
                input name: "motionScope", type: "enum", title: "Scope", options: ["Any", "All"], defaultValue: "Any", required: true
                input name: "motionComparison", type: "enum", title: "Comparison", options: ["Are", "Are not"], defaultValue: "Are", required: true
                input name: "motionValue", type: "enum", title: "Motion", options: ["Active", "Inactive"], defaultValue: "Active", required: true
            }
        }
	}
}

def actionsPage() {
	dynamicPage(name: "actionsPage", nextPage: "notifyPage", uninstall: true) {
    	section("ACTIONS") {
        	input name: "targetMode", type: "mode", title: "Change mode to", required: false
            def actions = location.helloHome?.getPhrases()*.label
            if(actions) {
            	actions.sort()
            	input name: "targetRoutine", type: "enum", title: "Run these routines", options: actions, multiple: true, required: false
			}
        }
    	section("MODES") {
        	input name: "modes", type: "mode", title: "Only in these modes", multiple: true, required: false
        }
    }
}

def notifyPage() {
	dynamicPage(name: "notifyPage", nextPage: "installPage", uninstall: true) {
    	section("NOTIFICATIONS") {
            input name: "sendPush", type: "bool", title: "Send a push notification?", defaultValue: false, required: true
        }
        section("TEXT MESSAGES") {
        	input name: "sendText", type: "bool", title: "Send a text message?", defaultValue: false, submitOnChange: true, required: true
            if (sendText) {
            	input name: "phoneNumber", type: "phone", title: "Phone number", required: true
			}
        }
    }
}

def installPage() {
	dynamicPage(name: "installPage", uninstall: true, install: true) {
    	section("NAME") {
        	label title: "Rule name", defaultValue: targetMode, required: false
        }
        section("DEBUG") {
        	input name: "debug", type: "bool", title: "Debug rule?", defaultValue: false, required: true
        }
    }
}

/**/

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
    unschedule()
	initialize()
}

def initialize() {
	setupSchedule()
	if(presenceSensors) subscribe(presenceSensors, "presence", presenceEvent)
	if(motionSensors) subscribe(motionSensors, "motion.active", motionEvent)
}

def setupSchedule() {

	// Setup hard schedules if specified.

	if(startType == "Specific")  {
    	schedule(startTime, startCallback);
        state.startTime = startTime
	}

	if(finishType == "Specific") {
    	schedule(finishTime, finishCallback);
        state.finishTime = finishTime
    }

	// Subscribe to sunrise and sunset events.

	subscribe(location, "position", sunSetRiseEvent)
    subscribe(location, "sunriseTime", sunSetRiseEvent)
    subscribe(location, "sunsetTime", sunSetRiseEvent)

    astroCheck() // Go ahead and schedule missed events.
}

/**/

def startCallback()  { evaluateMode() }
def finishCallback() { evaluateMode() }

def sunSetRiseEvent(evt) {
	astroCheck()
}

def astroCheck() {
    def now = new Date()
	def sun = getSunriseAndSunset(zipCode: location.zipCode)
    def sunrise = sun.sunrise
    def sunset = sun.sunset

	// Missed today? Schedule tomorrow's sunrise/sunset events.

	if(sunrise.before(now)) sunrise = sunrise.next()
	if(sunset.before(now)) sunset = sunset.next()

	debug("Sunrise is ${sunrise.format('yyyy-M-d hh:mm:ss a', location.timeZone)}")
    debug("Sunset is ${sunset.format('yyyy-M-d hh:mm:ss a', location.timeZone)}")

	// Schedule the starting event for the schedule.

	if(startType == "Sunrise") {
    	runOnce(new Date(sunrise.time + (startOffset * 60000)), evaluateMode, [overwrite: false])
        state.startTime = sunrise
    }
    else
    	if(startType == "Sunset") {
        	runOnce(new Date(sunset.time + (startOffset * 60000)), evaluateMode, [overwrite: false])
            state.startTime = sunset
        }

	// Schedule the finishing event for the schedule.

	if(finishType == "Sunrise") {
    	runOnce(new Date(sunrise.time + (finishOffset * 60000)), evaluateMode, [overwrite: false])
        state.finishTime = sunrise
    }
    else
    	if(finishType == "Sunset") {
        	runOnce(new Date(sunset.time + (finishOffset * 60000)), evaluateMode, [overwrite: false])
            state.finishTime = sunset
        }
}

/**/

def presenceEvent(evt) {
	if(evt.isStateChange)
		evaluateMode()
}

def motionEvent(evt) {
	if(evt.isStateChange)
		evaluateMode()
}

/**/

def evaluateMode() {
	now = new Date()

	debug("Current time is $now.format('yyyy-M-d hh:mm:ss a', location.timeZone)")
    debug("Schedule starts at $state.startTime.format('yyyy-M-d hh:mm:ss a', location.timeZone)")
    debug("Schedule finishes at $state.finishTime.format('yyyy-M-d hh:mm:ss a', location.timeZone)")

	if(timeOfDayIsBetween(startTime, finishTime, now, location.timeZone)) {
        if(targetMode && targetMode != location.mode) {
            if(location.modes?.find{it.name == targetMode}) {
                setLocationMode(targetMode)
                notify(true, "", "Setting mode to $targetMode.")
            }
            else
                debug("The mode '$targetMode' is not defined")
        }
        else
        	debug("The current mode is already '$targetMode'")
    }
    else
    	debug("The current time is not within the rule's schedule")
}

/**/

private notify(enabled, useCustom, customText, defaultText) {
	if(enabled) {
    	def message = useCustom ? customText : defaultText
		if(sendPush) sendPush(message)
		if(sendText) sendSms(phoneNumber, message)
    }
    log.info(defaultText)
}

private debug(message) {
	if(debug)
    	log.debug(message)
}