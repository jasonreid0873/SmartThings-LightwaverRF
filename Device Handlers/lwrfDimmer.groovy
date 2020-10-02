/**
 *  LightwaveRF API Device Handler Dimmer
 *
 *  Copyright 2019 Jason Reid
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
 
import java.security.MessageDigest
 
preferences {
    input("ip", "text", title: "API IP Address", description: "API IP Address")
	input("roomID", "text", title: "Room ID", description: "Room ID")
    input("deviceID", "text", title: "Device ID", description: "Device ID")
}
 
metadata {
	definition (name: "lwrfDimmer", namespace: "jay0873", author: "Jason Reid", vid: "generic-dimmer", ocfDeviceType: "oic.d.light") {
		capability "Actuator"
        capability "Switch"
        capability "Switch Level"
        capability "Health Check"
        command "register"
	}

	simulator {}
       
    tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821", nextState:"off"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"On"
			}
			tileAttribute ("device.level", key: "SLIDER_CONTROL") {
				attributeState "level", action:"switch level.setLevel", range:"(0..100)"
			}
		}

        standardTile("register", "device.status", inactiveLabel:false, decoration:"flat",height: 2, width: 2) {
            state "default", label:"Register", icon:"http://www.mocet.com/pic/link-icon.png", action:"register"
        }
        
        standardTile("unlock", "device.status", inactiveLabel:false, decoration:"flat",height: 2, width: 2) {
            state "default", label:"Unlock", icon:"http://www.mocet.com/pic/link-icon.png", action:"unlock"
        }

		standardTile("plock", "device.status", inactiveLabel:false, decoration:"flat",height: 2, width: 2) {
            state "default", label:"Partial Lock", icon:"http://www.mocet.com/pic/link-icon.png", action:"plock"
        }
        
         standardTile("flock", "device.status", inactiveLabel:false, decoration:"flat",height: 2, width: 2) {
            state "default", label:"Full Lock", icon:"http://www.mocet.com/pic/link-icon.png", action:"flock"
        }

		main "switch"
		details(["switch", "level", "unlock", "plock", "flock"])
	}

}

// parse events into attributes
def parse(String description) {
	def msg = parseLanMessage(description)
    if (msg.status == 200 && msg.json.result == "OK") {
    	log.info "http POST OK"
    }
    if (msg.status == 200 && msg.json.result == "ERR") {
    	log.error "http POST error"
    }
    if (msg.status < 200 || msg.status > 299) {
        log.error "http POST exception"
        log.error msg
    }
    if (msg.status < 200 || msg.status > 299) {
        log.error "http POST exception"
        log.error msg
    }
}

// handle commands
def on() {
	sendEvent(name: "switch", value: 'on')
	//apiGet('/lighting', 'on')
    apiSend("/api/Switch", "on", 100)
}

def off() {
	sendEvent(name: "switch", value: 'off')
    apiSend("/api/Switch","off",-1)
}

def setLevel(value) {
    if (value == 0) {
		sendEvent(name: "switch", value: 'off')
		sendEvent(name: "level", value: '0')
        apiSend("/api/Switch","off",-1)
	} else {
		sendEvent(name: "switch", value: 'on')
		sendEvent(name: "level", value: value)
        apiSend("/api/Switch","dim", value)
    	}
}

def register() {
	
}

def apiSend(path, action, value) {
    def uri = settings.ip + ":8100"
    def json = new groovy.json.JsonBuilder("Action":"${action}","Room":roomID,"Device":deviceID,"Level":value)
    log.info "http POST to $uri$path with JSON $json"
    
    def headers = [:]
    headers.put("HOST", "$uri")
    headers.put("Content-Type", "application/json")
    
    def method = "POST"
    
    try {
    	def hubAction = new physicalgraph.device.HubAction(
            method: method,
            path: path,
			headers: headers,
            body: json.toString()
    	)
    	hubAction
    }
    catch (Exception e) {
    	log.debug "http POST EXCEPTION!"
        log.debug e.tostring()
    }
}