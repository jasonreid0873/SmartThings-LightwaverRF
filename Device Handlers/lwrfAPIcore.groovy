/**
 *  LightwaveRF API Parent Device Handler
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
 
 import groovy.json.*
 
 preferences {
    input("configLoglevel", "enum", title: "Log level?",
        required: false, multiple:false, value: "nothing", options: ["0","1","2","3","4"])
}
 
metadata {
    definition (name: "lwrfAPicore", namespace: "jay0873", author: "Jason Reid") {
        capability "Actuator"
        capability "Bridge"
        capability "Refresh"
        capability "Notification"
        
		attribute "lwrfHub", "string"        
        attribute "lwrfAPI", "string"
        attribute "localIP", "string"
    }

    simulator {
        // TODO: define status and reply messages here
    }

    tiles(scale: 2) {
        standardTile("mainTile", "device", width: 6, height: 2, decoration: "flat") {
            state "default", icon:"https://raw.githubusercontent.com/vervallsweg/smartthings/master/icn/ic-cast-web-api-gray-1200.png", label:'Active', action:null, defaultState: true
        }
        standardTile("lwrfAPI", "device.lwrfAPI", width:4, height:1, decoration: "flat") {
        	state "default", label:'LWRF API: ${currentValue}', action:null, defaultState: true
        }
		standardTile("localIP", "device.localIP", width:4, height:1, decoration: "flat") {
        	state "default", label:'ST IP: ${currentValue}', action:null, defaultState: true
        }
		standardTile("refresh", "device.refresh", width: 2, height: 2, decoration: "flat") {
            state "default", icon:"st.secondary.refresh-icon", label:'All devices', action:refresh, defaultState: true
        }

        //main "mainTile"
        details(["mainTile", "localIP", "lwrfAPI", "refresh"])
    }
}


//
// Parse API messages and hand over to child devices
//
def parse(String description) {
    def message = parseLanMessage(description)
    def children = getChildDevices()
    logger('debug', "Parsing json: " + message.json)
    if(message.json) {
        if(message.json.slot) {
            int slot = message.json.slot as int
            def dni = "LWRF" + slot
            children.each { child ->
                if ( child.deviceNetworkId == dni ){
                    logger('info', "Message received for " + child.displayName + ", dni: " + child.deviceNetworkId)
                    logger('debug', message.json)
                    child.processJson(message.json)
                }
            }
        }
    }
}


//
// System Functions
//

def installed() {
    def devices = stringToMap(getDataValue("devices")[1..-2])
    logger('debug', "installed | devices: " + devices)
    installDevices(devices)
}

def updated() {
    def devices = stringToMap(getDataValue("devices")[1..-2])
    logger('debug', "updated | devices: " + devices)
	installDevices(devices)
}

def refresh() {
    getCallBackAddress()
    getLwrfApiAddress()
    getChildDevices().each { child ->
        child.refresh()
    }
}


//
// Private Functions
//
def getCallBackAddress() {
    logger('info', "localIP: " + device.hub.getDataValue("localIP") + ":" + device.hub.getDataValue("localSrvPortTCP"))
    sendEvent(name: "localIP", value: device.hub.getDataValue("localIP") + ":" + device.hub.getDataValue("localSrvPortTCP"), unit: "")
}

def getLwrfApiAddress() {
	sendEvent(name: "lwrfAPI", value: "192.168.1.6:8100", unit: "")
}

def stringToMap(fs) {
	def map = fs.tokenize(', ')*.tokenize(':').collectEntries()
    return map     
}

def qsToMap(qs) {
	def map = qs.tokenize('&')*.tokenize('=').collectEntries()
    return map     
}

def installDevices(devices) {
    def devicesToCreate = [:]
    def children = getChildDevices()
    logger('info', "installDevices | discovered " + children.size() + " child devices")
    
    devices.each { device ->
        def exists = false
		def params = qsToMap(device.value)
		def dni = "LWRF" + device.key
        def serial = params.serial
        def product = params.prod
        
		logger('debug', "installDevices | dni: " + dni)
        logger('debug', "installDevices | serial: " + serial)
        logger('debug', "installDevices | product: " + product)
        
        if (children) {
            children.each { child ->
                if( dni.equals(child.deviceNetworkId)) { 
                    exists = true
                }
            }
        }
        
        if (exists) {
            logger('warn', "installDevices | dni: " + dni + ", exists!")
        } else {
            logger('warn', "installDevices | dni: " + dni + ", doesn't exist")
            devicesToCreate.put(device.key, device.value)
        }
    }
    
    createDevices(devicesToCreate)
}

def createDevices(devices) {
    logger('debug', "createDevices | " + devices)
    
    devices.each {
        if(it != null && it != "") {
            def params = qsToMap(it.value)
            def dni = "LWRF" + it.key
            def slot = it.key
            def serial = params.serial
            def product = params.prod
            
            logger('debug', "createDevices | slot: " + slot)
            logger('debug', "createDevices | serial: " + serial)
            logger('debug', "createDevices | product: " + product)
            
            if (product == "pwrMtr") {
                //Add Energy Monitor
                logger('info', "createDevices | Adding device: Energy Monitor")
                addChildDevice("jay0873", "lwrfEnergy", dni, location.hubs[0].id, [
                    label: "LightwaveRF Energy Monitor " + serial,
                    data: [
                        "ip": getDataValue("apiHost"),
                        "slotID": slot,
                        "Serial": serial
                    ]
                ])
			}
            
            if (product == "valve") {
                //Add TRV
                logger('info', "createDevices | Adding device: TRV")
                addChildDevice("jay0873", "lwrfTRV", dni, location.hubs[0].id, [
                    label: "LightwaveRF TRV " + serial,
                    data: [
                        "ip": getDataValue("apiHost"),
                        "slotID": slot,
                        "Serial": serial
                    ]
                ])
			}
		}
    }
}


//
// Logging Levels
//
def logger(level, message) {
    def logLevel=1
    if(settings.configLoglevel) {
        logLevel = settings.configLoglevel.toInteger() ?: 0
    }
    if(level=="error"&&logLevel>0) {
        log.error message
    }
    if(level=="warn"&&logLevel>1) {
        log.warn message
    }
    if(level=="info"&&logLevel>2) {
        log.info message
    }
    if(level=="debug"&&logLevel>3) {
        log.debug message
    }
}