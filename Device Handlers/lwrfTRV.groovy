/**
 *  LightwaveRF API Child Device Handler TRV Control
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
    input("tempGraph", "bool", title: "Temperature Graph", description: "Display Temperature Graph")
    input("targGraph", "bool", title: "Target Graph", description: "Display Target Graph") 
    input("configLoglevel", "enum", title: "Log level?",
        required: false, multiple:false, value: "nothing", options: ["0","1","2","3","4"])   
}    

metadata {
	definition (name: "lwrfTRV", namespace: "jay0873", author: "Jason Reid", vid: "generic-thermostat-1", ocfDeviceType: "oic.d.thermostat") {
        capability "Actuator"
		capability "Polling"
		capability "Refresh"

        capability "Temperature Measurement"
		capability "Thermostat"
		capability "Thermostat Heating Setpoint"
        capability "Thermostat Operating State"
		capability "Thermostat Mode"
		//capability "Thermostat Setpoint"
        //capability "Thermostat Cooling Setpoint"

		capability "Sensor"
        capability "Battery"
        capability "Health Check"
        
        attribute "mode", "string"        
        attribute "output", "number"
        attribute "lastUpdate", "string"

		command "quickSetHeat"
        command "heatingSetpointUp"
		command "heatingSetpointDown"
		command "setThermostatMode"
        command "setHeatingSetpoint"
        command "trvOn"
        command "trvOff"
	}

	tiles(scale:2) {
 		multiAttributeTile(name:"thermostatFull", type:"thermostat", width:6, height:4) {
			tileAttribute("device.temperature", key: "PRIMARY_CONTROL") {
                attributeState("temp", label:'${currentValue}°', unit:"C", defaultState: true)
			}
            tileAttribute("device.output", key: "SECONDARY_CONTROL") {
        		attributeState("output", label:'${currentValue}%', defaultState: true)
    		}
			tileAttribute("device.heatingSetpoint", key: "VALUE_CONTROL") {
				attributeState("VALUE_UP", action: "heatingSetpointUp")
				attributeState("VALUE_DOWN", action: "heatingSetpointDown")
            }
            tileAttribute("device.thermostatOperatingState", key: "OPERATING_STATE") {
				attributeState("idle", backgroundColor:"#BBBBBB")
				attributeState("heating", backgroundColor:"#E86D13")
                //attributeState("cooling", backgroundColor:"#00A0DC")                
			}
            tileAttribute("device.thermostatMode", key: "THERMOSTAT_MODE") {
                attributeState("off", label:'${name}')
                attributeState("heat", label:'${name}')
                //attributeState("cool", label:'${name}')
        		//attributeState("auto", label:'${name}')                
			}
			tileAttribute("device.heatingSetpoint", key: "HEATING_SETPOINT") {
				attributeState("heatingSetpoint", label:'${currentValue}', unit:"C", defaultState: true)
			}
		}

		valueTile("Temperature", "device.temperature", decoration: "flat", width:2, height:2) {
            state "temperature", label:'${currentValue}°', unit:"C",
                backgroundColors:[
							// Temp Values
							[value: 62, color: "#153591"],
							[value: 70, color: "#44b621"],
							[value: 76, color: "#f1d801"],
							[value: 80, color: "#bc2323"] 
                ]
        }

        valueTile("setpoint", "device.heatingSetpoint", inactiveLabel:false, width:2, height:2) {
            state "default", icon:"st.Home.home1", label:'${currentValue}°', unit:"C",
                backgroundColors:[
							// Temp Values
							[value: 62, color: "#153591"],
							[value: 70, color: "#44b621"],
							[value: 76, color: "#f1d801"],
							[value: 80, color: "#bc2323"] 
                ]
        }
        
        valueTile("mode", "device.mode", inactiveLabel:false, decoration:"flat", width:2, height:1) {
            state "default", label:'------', action:"trvRun"
            state "run", label:'Run', icon:"st.Office.office7"
            state "man", label:'Run', icon:"st.Office.office7"
            state "boost", label:'Boost', icon:"st.thermostat.heating"
            state "calib", label:'Calibrate', icon:"st.motion.motion.active"
        }    

        standardTile("trvOff", "device.heatingOutput", inactiveLabel:false, decoration:"flat", width:1, height:1) {
            state "default", label:'Off', icon:"st.custom.buttons.add-icon", action:"trvOff"
        }

        standardTile("trvOn", "device.heatingOutput", inactiveLabel:false, decoration:"flat", width:1, height:1) {
            state "default", label:'On', icon:"st.custom.buttons.subtract-icon", action:"trvOn"
        }
        
        controlTile("heatSliderControl", "device.heatingSetpoint", "slider", height: 1, width: 2, inactiveLabel: false, range:"(0..40)") {
			state "setHeatingSetpoint", action:"setHeatingSetpoint", backgroundColor:"#d04e00"
		}        

		standardTile("lastUpdate", "device.lastUpdate", width: 2, height: 1, inactiveLabel: false, decoration: "flat", wordWrap: true) {
			state "default", label:'Last Updated\n ${currentValue}', icon:""
		}
        
        valueTile("battery", "device.battery", inactiveLabel:false, width:2, height:1) {
            state "default", icon:"st.quirky.spotter.quirky-spotter-plugged", label:'${currentValue}%', unit:"%",
                backgroundColors:[
                    [value: 25, color: "#cc3232"],
                    [value: 50, color: "#e7b416"],
                    [value: 75, color: "#99c140"],
                    [value: 100, color: "#44b621"]
                ]
        }        

        standardTile("refresh", "device.refresh", decoration:"flat", width:2, height:1) {
            state "default", icon:"st.secondary.refresh", label:"Refresh", action:"refresh.refresh"
            state "error", icon:"st.secondary.refresh", label:"Refresh", action:"refresh.refresh"
        }
        
        htmlTile(name:"graphHTML",
			action: "getGraphHTML",
			refreshInterval: 10,
			width: 6,
			height: 4,
			whitelist: ["www.gstatic.com"]) 
        
		valueTile("blank1x1", "", decoration: "flat", width: 1, height: 1) { }
		valueTile("blank2x1", "", decoration: "flat", width: 2, height: 1) { }
		valueTile("blank4x1", "", decoration: "flat", width: 4, height: 1) { }
		valueTile("blank2x2", "", decoration: "flat", width: 2, height: 2) { }
        
        standardTile("mainIcon", "device.thermostatOperatingState", inactiveLabel: true, decoration: "flat", width: 2, height: 2) {
			state "idle", label:'${currentValue}', icon: "st.Weather.weather2", backgroundColor:""
            //state "cooling", label:'${currentValue}', icon: "st.Weather.weather2", backgroundColor:"#00A0DC"
			state "heating", label:'${currentValue}', icon: "st.Weather.weather2", backgroundColor:"#E86D13"
		}

        main(["mainIcon"])
        details(["thermostatFull",
        	"mode", "trvOff", "trvOn", "heatSliderControl",
            "lastUpdate", "battery", "refresh",
            "graphHTML"
            ])
    }
}

mappings {
	path("/getGraphHTML") {action: [GET: "getGraphHTML"]}
}

public processJson(j) {
	logger('info', "Parsing JSON Message from Parent DTH")
    state.slot = j.slot
    state.currentMode = j.state

    if (j.output == 0) {
        sendEvent(name: "thermostatOperatingState", value: 'idle')
        sendEvent(name: "thermostatMode", value: 'heat')
        logger('info', "Operating State: Idle")
    } else {
        sendEvent(name: "thermostatOperatingState", value: 'heating')
        sendEvent(name: "thermostatMode", value: 'heat')
        logger('info', "Operating State: Heating")
    }

    if (j.cTarg >=50) {
        if (j.cTarg == 50) { 
            sendEvent(name: "heatingSetpoint", value: "off", unit: "C")
            logger('info', "Valve: Off")
        }
        if (j.cTarg == 60) { 
            sendEvent(name: "heatingSetpoint", value: "on", unit: "C") 
            logger('info', "Valve: On")
        }
    } else {
        sendEvent(name: "heatingSetpoint", value: j.cTarg, unit: "C")
        logger('info', "Target Temperature: " + j.cTarg + "°")
    }

    sendEvent(name: "temperature", value: j.cTemp, unit: "C")
    logger('info', "Current Temperature: " + j.cTemp + "°")

    sendEvent(name: "output", value: j.output as int, unit: "") 
    sendEvent(name: "mode", value: state.currentMode, unit: "")
    sendEvent(name: "battery", value: convertVoltToPercentage(j.batt), unit: "%")
    sendEvent(name: "lastUpdate", value: new Date((j.time as long)*1000).format("dd-MM-yyyy HH:mm:ss"), unit: "")
    
    if (state.tempTable == null) {
        state.tempTable = []
    } else {
        state.tempTable.clear()
    }
    
    if (state.targTable == null) {
        state.targTable = []
    } else {
        state.targTable.clear()
    }

    if (settings.tempGraph == true) {
        def tempTable = state.tempTable
        // initialise tempTable if empty using stored data
        if (tempTable.size() == 0) {
            if (settings.debugStatus == true) { log.debug "tempTable is empty" }
            def newValues
            tempTable = []

            def tempData = device.statesBetween("temperature", timeToday("23:00", location.timeZone), timeToday("23:59", location.timeZone), [max:120])
            for (int i=22; i >= 0; i--) {
                tempData += device.statesBetween("temperature", timeToday(i.toString() + ":00", location.timeZone), timeToday(i.toString() + ":59", location.timeZone), [max:120])
            }

            if (tempData.size()) {
                tempData.reverse().each() {
                    tempTable.add([it.date.format("H", location.timeZone),it.date.format("m", location.timeZone),it.value])
                }
            }
        }
        state.tempTable = tempTable
        if (settings.debugStatus == true) { log.debug "tempTable: " + state.tempTable }
        if (settings.debugStatus == true) { log.debug "tempTable records: " + tempTable.size() }
    }

    if (settings.targGraph == true) {
        def targTable = state.targTable
        // initialise targTable if empty using stored data
        if (targTable.size() == 0) {
            if (settings.debugStatus == true) { log.debug "targTable is empty" }
            def newValues
            targTable = []

            def targData = device.statesBetween("output", timeToday("23:00", location.timeZone), timeToday("23:59", location.timeZone), [max:120])
            for (int i=22; i >= 0; i--) {
                targData += device.statesBetween("output", timeToday(i.toString() + ":00", location.timeZone), timeToday(i.toString() + ":59", location.timeZone), [max:120])
            }

            if (targData.size()) {
                targData.reverse().each() {
                    targTable.add([it.date.format("H", location.timeZone),it.date.format("m", location.timeZone),it.value])
                }
            }
        }
        state.targTable = targTable
        if (settings.debugStatus == true) { log.debug "targTable: " + state.targTable }
        if (settings.debugStatus == true) { log.debug "targTable records: " + targTable.size() }
    }
    
    return events
}


//
// Send commands to TRV
//
def heatingSetpointUp(){
    float degreesC = device.currentValue("heatingSetpoint") + 1
 	setTargetSetpoint(degreesC)
 }

def heatingSetpointDown(){
	float degreesC = device.currentValue("heatingSetpoint") - 1
 	setTargetSetpoint(degreesC)
 }

def setHeatingSetpoint(degreesC) {
	setTargetSetpoint(degreesC)
}

def setTargetSetpoint(degreesC) {
	logger('info', "Setting TargetSetpoint to $degreesC")
	sendEvent(name: "heatingSetpoint", value: degreesC, unit: "C")
 	apiSend("/api/Heating","temp",degreesC)
}

def trvOn(){
  	logger('info', "Setting heat on")
 	apiSend("/api/Heating","on",0)
 }

def trvOff(){
  	logger('info', "Setting heat off")
 	apiSend("/api/Heating","off",0)
 }

def setThermostatMode(String newMode) {
	logger('info', "Setting mode to $newMode")
	sendEvent(name: "thermostatMode", value: newMode)
 	apiSend("/api/Heating","mode",newMode)
}


//
// LWRF API Calls
//
def apiGet(path) {
	def uri = getDataValue("ip")
    logger('debug', "http GET to $uri$path")
    
    def headers = [:]
    headers.put("HOST", "$uri")
    headers.put("Content-Type", "application/json")
    
    def method = "GET"
    
    try {
    	def hubAction = new physicalgraph.device.HubAction(
            method: method,
            path: path,
			headers: headers,
    	)
    	return hubAction
    }
    catch (Exception e) {
    	logger('error', "http GET EXCEPTION!")
        logger('error', e.tostring())
    }
}

def apiSend(path, action, value) {
    value = value as int
    def uri = getDataValue("ip")
    def json = new groovy.json.JsonBuilder("Action":"${action}","Slot":getDataValue("slotID"),"Temp":value)
    logger('debug', "http POST to $uri$path with JSON $json")
    
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
    	logger('error', "http POST EXCEPTION!")
        logger('error', e.tostring())
    }
}


//
// System Calls
//
def installed() {
	//
}

def updated() {
    //
}

def configure () {
    //
}

def poll() {
	//
}

def refresh() {
	logger('info', "refresh")
    return apiGet ("/api/device/" + getDataValue("slotID"))
}


//
// Private Calls
//
private Number convertVoltToPercentage(volt) {
	Number percent
    if (volt >= 3) {
    	percent = 100
    } else {
    	if (volt <= 2.4) {
        	percent = 0
            } else {
        percent = (((volt-2.4)/0.6)*100).toInteger()
    	}
    }
    return percent
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


//
// Generate Graph
//
String getDataString(Integer seriesIndex) {
	def dataString = ""
	def dataTable = []
	switch (seriesIndex) {
		case 1:
			dataTable = state.tempTable
			break
		case 2:
			dataTable = state.targTable
			break
		case 3:
			dataTable = state.tempTable
			break
		case 4:
			dataTable = state.tempTable
			break
	}
	dataTable.each() {
		def dataArray = [[it[0],it[1],0],null,null,null,null]
		dataArray[seriesIndex] = it[2]
		dataString += dataArray.toString() + ","
	}
	return dataString
}

def getStartTime() {
	def startTime = 24
	if (state.tempTable && state.tempTable.size()) {
		startTime = state.tempTable.min{it[0].toInteger()}[0].toInteger()
	}
	//if (state.powerTableYesterday && state.powerTableYesterday.size()) {
		//startTime = Math.min(startTime, state.powerTableYesterday.min{it[0].toInteger()}[0].toInteger())
	//}
	return startTime
}

def getGraphHTML() {
	def html = """
		<!DOCTYPE html>
			<html>
				<head>
					<meta http-equiv="cache-control" content="max-age=0"/>
					<meta http-equiv="cache-control" content="no-cache"/>
					<meta http-equiv="expires" content="0"/>
					<meta http-equiv="expires" content="Tue, 01 Jan 1980 1:00:00 GMT"/>
					<meta http-equiv="pragma" content="no-cache"/>
					<meta name="viewport" content="width = device-width">
					<meta name="viewport" content="initial-scale = 1.0, user-scalable=no">
					<style type="text/css">body,div {margin:0;padding:0}</style>
					<script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
					<script type="text/javascript">
						google.charts.load('current', {packages: ['corechart']});
						google.charts.setOnLoadCallback(drawGraph);
						function drawGraph() {
							var data = new google.visualization.DataTable();
							data.addColumn('timeofday', 'time');
							data.addColumn('number', 'Temp');
                            data.addColumn('number', 'Output');
                            data.addColumn('number', 'Temp (Today)');
                            data.addColumn('number', 'Temp (Today)');
							data.addRows([
								${getDataString(1)}
							]);
							var options = {
								fontName: 'San Francisco, Roboto, Arial',
								height: 240,
								trendlines: {
                                    0: {
                                      type: 'polynomial',
                                      degree: '2',
                                      color: 'green',
                                      lineWidth: 3,
                                      opacity: 0.3,
                                    }
                                },
                                hAxis: {
									format: 'H:mm',
									minValue: [${getStartTime()},0,0],
									slantedText: false
								},
                                series: {
									0: {targetAxisIndex: 0, color: '#FF0000', type: "line"}
								},
								vAxes: {
									0: {
										title: 'Temp (C)',
										format: 'decimal',
										textStyle: {color: '#FF0000'},
										titleTextStyle: {color: '#FF0000'},
									}
								},
								legend: {
									position: 'none'
								},
								chartArea: {
									width: '72%',
									height: '85%'
								}
							};
							var chart = new google.visualization.ComboChart(document.getElementById('chart_div'));
							chart.draw(data, options);
						}
					</script>
				</head>
				<body>
					<div id="chart_div"></div>
				</body>
			</html>
		"""
	render contentType: "text/html", data: html, status: 200
}