/**
 *  Door Rule
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
    name: "Door Rule",
    namespace: "Anghus",
    author: "Jerry Honeycutt",
    description: "Manage aspects of a door, including locks, knock sensors, contact sensors, and doorbells.",
    category: "My Apps",

    parent: "Anghus:Home Rules",
    iconUrl: "http://cdn.device-icons.smartthings.com/Home/home2-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home2-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Home/home2-icn@3x.png")

/**/

preferences {
	page(name: "doorPage")
    page(name: "knockPage")
    page(name: "doorbellPage")
    page(name: "lightsPage")
    page(name: "notifyPage")
    page(name: "installPage")
}

def doorPage() {
	dynamicPage(name: "doorPage", nextPage: "knockPage", uninstall: true) {
    	section("DOOR") {
			input name: "contactSensor", type: "capability.contactSensor", title: "Contact", submitOnChange: true, required: false
            if(contactSensor) {
    	    	input name: "doorLock", type: "capability.lock", title: "Door lock", submitOnChange: true, required: false
                if(doorLock) {
                    input name: "lockDelay", type: "number", title: "Delay (minutes)", defaultValue: 15, required: true
                    paragraph "The door lock engages after the specified delay only if the contact sensor is closed."
                }
            }
		}
        if(doorLock) {
            section("MOTION") {
                input name: "motionSensors", type: "capability.motionSensor", title: "Motion sensors", multiple: true, submitOnChange: true, required: false
                paragraph "If you specify one or more motion sensors, the door lock only engages after the sensors are inactive for the specified delay."
            }
        }
        section("MESSAGES") {
            input name: "notifyOnDoorEvents", type: "bool", title: "Notify on change?", defaultValue: false, submitOnChange: true, required: true
            if(notifyOnDoorEvents) {
                input name: "doorEvents", type: "enum", title: "Which events?", options: ["open", "closed", "locked", "unlocked"], multiple: true, required: true
                input name: "useCustomDoorMessages", type: "bool", title: "Custom messages?", defaultValue: false, submitOnChange: true, required: true
                if (useCustomDoorMessages) {
                    input name: "openMessage", type: "text", title: "Open message", defaultValue: "The door is open.", required: true
                    input name: "closeMessage", type: "text", title: "Close message", defaultValue: "The door is closed.", required: true
                    input name: "lockMessage", type: "text", title: "Lock message", defaultValue: "The door is locked.", required: true
                    input name: "unlockMessage", type: "text", title: "Unlock message", defaultValue: "The door is unlocked.", required: true
                }
            }
        }
	}
}

def knockPage() {
	dynamicPage(name: "knockPage", nextPage: "doorbellPage", uninstall: true) {
		section("KNOCK") {
	        input name: "knockSensor", type: "capability.accelerationSensor", title: "Acceleration sensor", submitOnChange: true, required: false
            if(knockSensor) {
                input name: "knockDelay", type: "number", title: "Knock delay (seconds)", defaultValue:5, required: true
                paragraph "The knock delay prevents false alarms by giving the door a chance to open, lock, or unlock before reporting the knock."
            }
        }
        if(knockSensor) {
            section("MOTION") {
                input name: "outsideSensor", type: "capability.motionSensor", title: "Motion sensor", required: false
                paragraph "If there was no recent activity on this motion sensor, the knock is probably a false alarm and is ignored."
            }
            section("MESSAGES") {
                input name: "notifyKnock", type: "bool", title: "Notify on knock?", defaultValue: false, required: true
                input name: "useCustomKnockMessage", type: "bool", title: "Custom message?", defaultValue: false, submitOnChange: true, required: true
                if (useCustomKnockMessage) {
                    input name: "knockMessage", type: "text", title: "Knock message", defaultValue: "Someone is knocking the door.", required: true
                }
            }
        }
    }
}

def doorbellPage() {
	dynamicPage(name: "doorbellPage", nextPage: "lightsPage", uninstall: true) {
    	section("DOORBELL") {
        	input name: "doorbellContact", type: "capability.contactSensor", title: "Doorbell contact", submitOnChange: true, required: false
        }
        if(doorbellContact) {
            section("MESSAGES") {
                input name: "notifyDoorbell", type: "bool", title: "Notify on ring?", defaultValue: false, required: true
                input name: "useCustomDoorbellMessage", type: "bool", title: "Custom message?", defaultValue: false, submitOnChange: true, required: true
                if (useCustomDoorbellMessage) {
                    input name: "doorbellMessage", type: "text", title: "Doorbell message", defaultValue: "Someone is ringing the doorbell.", required: true
                }
            }
        }
    }
}

def lightsPage() {
	dynamicPage(name: "lightsPage", nextPage: "notifyPage", uninstall: true) {
		section("LIGHTS") {
	        input name: "controlLights", type: "bool", title: "Control lights?", defaultValue: false, submitOnChange: true, required: true
            if(controlLights) {
                input name: "lights", type: "capability.switch", title: "Light switches", multiple: true, required: true
                input name: "lightTimeout", type: "number", title: "Turn off after (minutes)", defaultValue: 5, required: true
            }
        	paragraph "Knocking the door or ringing the doorbell turns on the specified lights for the time specified."
        }
        if(controlLights) {
            section("EVENTS") {
                input name: "limitLightEvents", type: "bool", title: "Choose events?", defaultValue: false, submitOnChange: true, required: true
                if(limitLightEvents) {
                    input name: "lightEvents", type: "enum", title: "Which events?", options: ["open", "closed", "locked", "unlocked", "doorbell", "knock", "motion"], multiple: true, required: true
                    input name: "lightModes", type: "mode", title: "During which modes?", multiple: true, required: true
                }
            }
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
        	label title: "Door name", defaultValue: contactSensor.label, required: false
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
	initialize()
}

def initialize() {
    state.lastLock = 0
	state.lastContact = 0
    state.lastMotion = 0
    state.lastKnocker = 0
	state.lockScheduled = false
    state.lightsScheduled = false
    state.whenUnlocked = 0

	// Lock events:
    
	if(doorLock) subscribe(doorLock, "lock", lockEvent)
	if(contactSensor) subscribe(contactSensor, "contact", contactEvent)
    if(motionSensors) subscribe(motionSensors, "motion.active", motionEvent)

	// Knock events:
    
	if(knockSensor) subscribe(knockSensor, "acceleration.active", knockEvent)
 	if(outsideSensor) subscribe(outsideSensor, "motion.active", outsideEvent)
    
    // Doorbell events:
    
    if(doorbellContact) subscribe(doorbellContact, "contact.closed", doorbellEvent)
}

/**/

def lockEvent(evt)	{
	if(evt.isStateChange) {
        def customMsg = (evt.value == "locked") ? lockMessage : unlockMessage
        def defaultMsg = "The ${getApp().label.toLowerCase()} was ${evt.value}."

        state.whenUnlocked = now()
        notify(notifyOnDoorEvents && evt.value in doorEvents, useCustomDoorMessages, customMsg, defaultMsg)
        if(!limitLightEvents || (limitLightEvents && evt.value in lightEvents))
            turnOnLights()

        evaluateLock()
        state.lastLock = now()
    }
}

def motionEvent(evt) {
	if(evt.isStateChange) {
        evaluateLock()
        state.lastMotion = now()
    }
}

def contactEvent(evt) {
	if(evt.isStateChange) {
        def customMsg = (evt.value == "open") ? openMessage : closeMessage
        def defaultMsg = "The ${getApp().label.toLowerCase()} was ${(evt.value == "open") ? "opened" : evt.value}."

        notify(notifyOnDoorEvents && evt.value in doorEvents, useCustomDoorMessages, customMsg, defaultMsg)
        if(!limitLightEvents || (limitLightEvents && evt.value in lightEvents))
            turnOnLights()

        evaluateLock()
        state.lastContact = now()
    }
}

def evaluateLock() {
	def doorLocked = (doorLock == null) ? true : doorLock.currentValue("lock") == "locked"
    def doorClosed = (contactSensor == null) ? true : contactSensor.currentValue("contact") == "closed"
	def motionActive = false
    motionSensors.each {
        motionActive |= it.currentValue("motion") == "active"
        //debug("Motion ${it.currentValue("motion") == "active" ? "is" : "is not"} detected on ${it.label}")
    }

	debug("${getApp().label} is ${doorLocked ? "locked" : "unlocked"}, ${doorClosed ? "closed" : "open"}, and ${motionActive ? "active" : "inactive"}")

	if(!doorLocked && doorClosed && !motionActive) {

		// Schedule the door lock if it's closed, unlocked, and no motion detected.

		runIn((lockDelay ?: 15)*60, lockDoor, [overwrite: true])
        debug("Scheduling the ${getApp().label.toLowerCase()} to lock in ${lockDelay} minutes")
        state.lockScheduled = true
    }
    else {

		// Otherwise, if the door lock was previously schedule, remove it from the queue.
        // This woudl only occur if the door was manually locked, closed, or motion stopped.

		if(state.lockScheduled) {
            unschedule(lockDoor)
            debug("Removing the ${getApp().label.toLowerCase()} lock from the schedule")
            state.lockScheduled = false
        }
    }
}

def lockDoor() {
	doorLock.lock()
	debug("Locking the ${getApp().label.toLowerCase()} after ${Math.round((now() - state.whenUnlocked)/60000)} minutes")
}

/**/

def knockEvent(evt) {
	if(evt.isStateChange) {
        runIn(knockDelay ?: 5, evaluateKnock, [overwrite: true])
        debug("Scheduling knock handler for ${getApp().label.toLowerCase()} to run in ${knockDelay} seconds");
    }
}

def outsideEvent(evt) {
	if(evt.isStateChange) {
        if(!limitLightEvents || (limitLightEvents && "motion" in lightEvents))
            turnOnLights()
        state.lastKnocker = now()
    }
}

def evaluateKnock() {
	def defaultMsg = "Someone is knocking at the ${getApp().label.toLowerCase()}."

	// Check whether sensor events are more recent than 60 seconds ago.

	def nowMinus60 = now() - 60000
	def recentContact = (contactSensor == null) ? false : state.lastContact > nowMinus60
	def recentLock    = (doorLock == null) ? false : state.lastLock > nowMinus60
    def recentMotion  = (outsideSensor == null) ? true : state.lastKnocker > nowMinus60

	if(!recentContact && !recentLock && recentMotion) {

		// If contact and lock events are't recent, send notification and log event.
        // Ignore the event if the door is open (i.e., door was answered or left open).
        
        if(contactSensor.currentValue("contact") == "closed") {
			notify(notifyKnock, useCustomKnockMessage, knockMessage, defaultMsg)
            if(!limitLightEvents || (limitLightEvents && "knock" in lightEvents))
                turnOnLights()
		}
	}
	else {
    
    	// If any of the sensors have recent activity, log the false alarm.
        
    	debug("Recent knock on the ${getApp().label.toLowerCase()} was just noise")
	}
}

/**/

def doorbellEvent(evt) {
	if(evt.isStateChange) {
        def defaultMsg = "Someone is ringing the ${getApp().label.toLowerCase()} doorbell."
        notify(notifyDoorbell, useCustomDoorbellMessage, doorbellMessage, defaultMsg)
        if(!limitLightEvents || (limitLightEvents && "doorbell" in lightEvents))
            turnOnLights()
    }
}

/**/

def turnOnLights() {
	if(controlLights) {
    	if(!lightModes || (lightModes && location.mode in lightModes)) {
            lights.each { it.on() }
            runIn(lightTimeout*60, turnOffLights, [overwrite: true])
            debug("Turning on ${lights} for ${lightTimeout} minutes")
        }
        else
        	debug("${location.mode} is not in ${lightModes}")
    }
    state.lightsScheduled = true
}

def turnOffLights() {
	if(state.lightsScheduled) {
    	lights.each { it.off() }
        state.lightsScheduled = false
        debug("Turning off ${lights} after ${lightTimeout} minutes")
    }
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