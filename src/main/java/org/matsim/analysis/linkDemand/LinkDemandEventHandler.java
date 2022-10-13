/* *********************************************************************** *
 * project: org.matsim.*
 * LinksEventHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package org.matsim.analysis.linkDemand;


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
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

/**
 * @author Ihab
 *
 */
public class LinkDemandEventHandler implements  LinkLeaveEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {
	private static final Logger log = LogManager.getLogger(LinkDemandEventHandler.class);
	
	private Map<Id<Link>,Integer> linkId2vehicles = new HashMap<Id<Link>, Integer>();
	private Map<Id<Link>,Integer> linkId2passengers = new HashMap<Id<Link>, Integer>();

	private VehicleFilter vehicleFilter;
	private Map<Id<Vehicle>, Integer> vehicleId2passengers = new HashMap<>();

	public LinkDemandEventHandler(VehicleFilter vehicleFilter) {
		this.vehicleFilter = vehicleFilter;
	}
	
	public LinkDemandEventHandler() {
		this.vehicleFilter = null;
	}

	@Override
	public void reset(int iteration) {
		this.linkId2vehicles.clear();
		this.linkId2passengers.clear();
		this.vehicleId2passengers.clear();
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		
		if (vehicleFilter == null || vehicleFilter.considerVehicle(event.getVehicleId())) {
			if (this.linkId2vehicles.containsKey(event.getLinkId())) {
				int vehicles = this.linkId2vehicles.get(event.getLinkId());
				this.linkId2vehicles.put(event.getLinkId(), vehicles + 1);
				
				int passengers = this.linkId2passengers.get(event.getLinkId());
				this.linkId2passengers.put(event.getLinkId(), passengers + this.vehicleId2passengers.get(event.getVehicleId()));
				
			} else {
				this.linkId2vehicles.put(event.getLinkId(), 1);
				this.linkId2passengers.put(event.getLinkId(), this.vehicleId2passengers.get(event.getVehicleId()));
			}
		}
	}

	public void printResults(String fileNameWithoutEnding) {
		{
			String fileName;
			if (this.vehicleFilter == null) {
				fileName = fileNameWithoutEnding + "link_dailyTrafficVolume_vehicles.csv";
			} else {
				fileName = fileNameWithoutEnding + "link_dailyTrafficVolume_vehicles" + this.vehicleFilter.toFileName() +".csv";
			}
			File file = new File(fileName);
			
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(file));
				bw.write("link;agents");
				bw.newLine();
				
				for (Id<Link> linkId : this.linkId2vehicles.keySet()){
					double volume = this.linkId2vehicles.get(linkId);
					bw.write(linkId + ";" + volume);
					bw.newLine();
				}
				
				bw.close();
				log.info("Output written to " + fileName);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		{
			String fileName;
			if (this.vehicleFilter == null) {
				fileName = fileNameWithoutEnding + "link_dailyTrafficVolume_passengers.csv";
			} else {
				fileName = fileNameWithoutEnding + "link_dailyTrafficVolume_passengers" + this.vehicleFilter.toFileName() +".csv";
			}
			File file = new File(fileName);
			
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(file));
				bw.write("link;agents");
				bw.newLine();
				
				for (Id<Link> linkId : this.linkId2passengers.keySet()){
					
					double volume = this.linkId2passengers.get(linkId);
					
					bw.write(linkId + ";" + volume);
					bw.newLine();
				}
				
				bw.close();
				log.info("Output written to " + fileName);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (vehicleFilter == null || vehicleFilter.considerVehicle(event.getVehicleId())) {
			if (this.vehicleId2passengers.get(event.getVehicleId()) == null) {
				this.vehicleId2passengers.put(event.getVehicleId(), 1);
			} else {
				int currentPassengers = this.vehicleId2passengers.get(event.getVehicleId());
				this.vehicleId2passengers.put(event.getVehicleId(), currentPassengers + 1);
			}
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (vehicleFilter == null || vehicleFilter.considerVehicle(event.getVehicleId())) {
			int currentPassengers = this.vehicleId2passengers.get(event.getVehicleId());
			int updatedPassengerNumber = currentPassengers - 1;
			
			if (updatedPassengerNumber < 0) {
				throw new RuntimeException("Negative number of passengers: " + event.toString() + " Aborting...");
			}
			
			this.vehicleId2passengers.put(event.getVehicleId(), currentPassengers - 1);
		}
	}

	public Map<Id<Link>, Integer> getLinkId2demand() {
		return linkId2vehicles;
	}

}
