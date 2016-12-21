/**
 *  Home Rules
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
    name: "Home Rules",
    namespace: "Anghus",
    author: "Jerry Honeycutt",
    description: "Manage home automation rules.",
    category: "My Apps",

    singleInstance: true,
    iconUrl: "http://cdn.device-icons.smartthings.com/Home/home2-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home2-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Home/home2-icn@3x.png")

/**/

preferences {
	page(name: "mainPage", title: "HOME RULES", install: true, uninstall: true, submitOnChange: true) {
        section {
            href "doorPage", title: "Door Rules", description: "Manage aspects of a door, including locks, knock sensors, contact sensors, and doorbells."
            href "modePage", title: "Mode Rules", description: "Change modes and run actions based upon presence devices, motion sensors, and time of day."
            href "pollPage", title: "Poll Rules", description: "Poll devices based upon a timer or motion sensor state changes."
        }
    }
    page(name: "doorPage", title: "DOOR RULES", install: true, uninstall: false, submitOnChange: true) {
        section {
            app(name: "doorRule", appName: "Door Rule", namespace: "Anghus", title: "Create New Door Rule", multiple: true)
        }
    }
    page(name: "modePage", title: "MODE RULES", install: true, uninstall: false, submitOnChange: true) {
        section {
            app(name: "modeRule", appName: "Mode Rule", namespace: "Anghus", title: "Create New Mode Rule", multiple: true)
        }
    }
    page(name: "pollPage", title: "POLL RULES", install: true, uninstall: false, submitOnChange: true) {
        section {
            app(name: "pollRule", appName: "Poll Rule", namespace: "Anghus", title: "Create New Poll Rule", multiple: true)
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
}