/**
 *  LightwaveRF API Child Device Handler Energy Meter
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
    input("powerGraph", "bool", title: "Power Graph", description: "Display Power Graph")
    input("energyGraph", "bool", title: "Energy Graph", description: "Display Energy Graph")
    input("configLoglevel", "enum", title: "Log level?",
        required: false, multiple:false, value: "nothing", options: ["0","1","2","3","4"])
}

metadata {
	definition (name: "lwrfEnergy", namespace: "jay0873", author: "Jason Reid") {
		
        capability "Energy Meter"
		capability "Power Meter"
		capability "Refresh"
		capability "Polling"
		capability "Sensor"
		capability "Health Check"
        capability "Notification"

        attribute "lastUpdate", "string"

		command "regsiter"
	}

	// tile definitions
	tiles(scale:2) {
		valueTile("currentUsage", "device.power", width: 6, height: 4, canChangeIcon: true) {
			state "default", label:'${currentValue} Watts', unit:"W",
            backgroundColors:[
                [value: 500, color: "#44b621"],
                [value: 1000, color: "#f1d801"],
                [value: 2000, color: "#d04e00"],
                [value: 3000, color: "#bc2323"]
			]
		}
        
        valueTile("todayUsage", "device.energy", width: 2, height: 2) {
			state "default", label:'${currentValue} KWh', unit:"KWh",
            backgroundColors:[
                [value: 5, color: "#44b621"],
                [value: 10, color: "#f1d801"],
                [value: 14, color: "#d04e00"],
                [value: 18, color: "#bc2323"]
			]
		}

		standardTile("lastUpdate", "device.lastUpdate", width: 2, height: 2, inactiveLabel: false, decoration: "flat", wordWrap: true) {
			state "default", label:'Last Updated\n ${currentValue}', icon:""
		}

		standardTile("refresh", "device.power", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        
        htmlTile(name:"graphHTML",
			action: "getGraphHTML",
			refreshInterval: 10,
			width: 6,
			height: 4,
			whitelist: ["www.gstatic.com"]) 
        
		main (["currentUsage"])
		details(["currentUsage", 
        "todayUsage", ,"lastUpdate", "refresh",
        "graphHTML"
        ])
	}
}

mappings {
	path("/getGraphHTML") {action: [GET: "getGraphHTML"]}
}

public processJson(j) {
    logger('info', "Parsing JSON Message from Parent DTH")
    int _slot = j.slot
    int _cUse = j.cUse
    int _todUse = j.todUse
    
    if (_slot != null) {
        logger('info', "power: " + _cUse + "W, energy: " + _todUse.div(1000) + "KWh")
        sendEvent(name: "power", value: _cUse, unit: "W")
        sendEvent(name: "energy", value: _todUse.div(1000), unit: "KWh")
        sendEvent(name: "lastUpdate", value: new Date((j.time as long)*1000).format("dd-MM-yyyy HH:mm:ss"), unit: "")

        if (state.powerTable == null) {
            state.powerTable = []
        } else {
            state.powerTable.clear()
        }
        if (state.energyTable == null) {
            state.energyTable = []
        } else {
            state.energyTable.clear()
        }

        if (settings.powerGraph == true) {
            // Parse historic data for power table
            def powerTable = state.powerTable
            // initialise powerTable if empty using stored data
            if (powerTable.size() == 0) {
                if (settings.debugStatus == true) { log.debug "powerTable is empty" }
                def newValues
                powerTable = []

                def powerData = device.statesBetween("power", timeToday("23:00", location.timeZone), timeToday("23:59", location.timeZone), [max:120])
                for (int i=22; i >= 0; i--) {
                    powerData += device.statesBetween("power", timeToday(i.toString() + ":00", location.timeZone), timeToday(i.toString() + ":59", location.timeZone), [max:120])
                }

                if (powerData.size()) {
                    powerData.reverse().each() {
                        powerTable.add([it.date.format("H", location.timeZone),it.date.format("m", location.timeZone),it.value])
                    }
                }
            }
            state.powerTable = powerTable
            if (settings.debugStatus == true) { log.debug "powerTable: " + state.powerTable }
            logger('debug', "powerTable records: " + powerTable.size())
        }

        if (settings.energyGraph == true) {
            // Parse historic data for energy table
            def energyTable = state.energyTable
            // initialise energyTable if empty using stored data
            if (energyTable.size() == 0) {
                if (settings.debugStatus == true) { log.debug "energyTable is empty" }
                def startOfToday = timeToday("00:00", location.timeZone)
                def newValues
                energyTable = []

                def energyData = device.statesBetween("energy", timeToday("23:00", location.timeZone), timeToday("23:59", location.timeZone), [max:100])
                for (int i=22; i >= 0; i--) {
                    energyData += device.statesBetween("energy", timeToday(i.toString() + ":00", location.timeZone), timeToday(i.toString() + ":59", location.timeZone), [max:100])
                }

                if (energyData.size()) {
                    energyData.reverse().each() {
                        energyTable.add([it.date.format("H", location.timeZone),it.date.format("m", location.timeZone),it.value])
                    }
                }
            }
            state.energyTable = energyTable
            if (settings.debugStatus == true) { log.debug "energyTable: " + state.energyTable }
            logger('debug', "energyTable records: " + energyTable.size())        
        }
    }
    return events
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
        

//
//System Calls
///
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
    logger ('info', "Refresh")
    return apiGet ("/api/device/" + getDataValue("slotID"))
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
			dataTable = state.powerTable
			break
		case 2:
			dataTable = state.energyTable
			break
		case 3:
			dataTable = state.powerTable
			break
		case 4:
			dataTable = state.powerTable
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
	if (state.powerTable && state.powerTable.size()) {
		startTime = state.powerTable.min{it[0].toInteger()}[0].toInteger()
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
							data.addColumn('number', 'Power (Today)');
							data.addColumn('number', 'Energy (Today)');
							data.addColumn('number', 'Power (Today)');
							data.addColumn('number', 'Power (Today)');
							data.addRows([
								${getDataString(1)}
                                ${getDataString(2)}
							]);
							var options = {
								fontName: 'San Francisco, Roboto, Arial',
								height: 240,
								hAxis: {
									format: 'H:mm',
									minValue: [${getStartTime()},0,0],
									slantedText: false
								},
								series: {
									0: {targetAxisIndex: 0, color: '#FF0000'},
                                    1: {targetAxisIndex: 1, color: '#0000FF'}
								},
								vAxes: {
									0: {
										title: 'Power (W)',
										format: 'decimal',
										textStyle: {color: '#FF4C00'},
										titleTextStyle: {color: '#FF4C00'}
									},
                                    1: {
										title: 'Energy (KWh)',
										format: 'decimal',
										textStyle: {color: '#004CFF'},
										titleTextStyle: {color: '#004CFF'}
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
							var chart = new google.visualization.AreaChart(document.getElementById('chart_div'));
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