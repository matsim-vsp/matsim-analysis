/* *********************************************************************** *
 * project: org.matsim.*
 * LinksEventHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 */
package org.matsim.analysis.vehicleOperatingTimes;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.analysis.VehicleFilter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.vehicles.Vehicle;

/**
 * @author Ihab
 *
 */
public class OperatingTimesEventHandler implements  LinkLeaveEventHandler {
	private static final Logger log = LogManager.getLogger(OperatingTimesEventHandler.class);
	
	private Map<Id<Vehicle>,Double> vehId2earliestLinkLeaveTime = new HashMap<>();
	private Map<Id<Vehicle>,Double> vehId2latestLinkLeaveTime = new HashMap<>();

	private VehicleFilter vehicleFilter;

	public OperatingTimesEventHandler(VehicleFilter vehicleFilter) {
		this.vehicleFilter = vehicleFilter;
	}

	@Override
	public void reset(int iteration) {
		this.vehId2earliestLinkLeaveTime.clear();
		this.vehId2latestLinkLeaveTime.clear();
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (vehicleFilter == null || vehicleFilter.considerVehicle(event.getVehicleId())) {
			if (this.vehId2latestLinkLeaveTime.get(event.getVehicleId()) == null) {
				this.vehId2earliestLinkLeaveTime.put(event.getVehicleId(), event.getTime());
			}
			this.vehId2latestLinkLeaveTime.put(event.getVehicleId(), event.getTime());			
		}
	}

	public void printResults(String fileNameWithoutEnding) {
		String fileName;
		if (this.vehicleFilter == null) {
			fileName = fileNameWithoutEnding + "vehicle-operation-time.csv";
		} else {
			fileName = fileNameWithoutEnding + "vehicle-operation-time" + this.vehicleFilter.toFileName() +".csv";
		}
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("vehicle ID;earliest link leave event; latest link leave event");
			bw.newLine();
			
			for (Id<Vehicle> vehicleId : this.vehId2earliestLinkLeaveTime.keySet()){
				
				bw.write(vehicleId + ";" + this.vehId2earliestLinkLeaveTime.get(vehicleId) + ";" + this.vehId2latestLinkLeaveTime.get(vehicleId));
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
