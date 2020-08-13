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
package org.matsim.analysis.dynamicLinkDemand;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.analysis.VehicleFilter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

/**
 * @author Ihab
 *
 */
public class DynamicLinkDemandEventHandler implements  LinkLeaveEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {
	private static final Logger log = Logger.getLogger(DynamicLinkDemandEventHandler.class);
	
	private double timeBinSize = 3600.;
	private VehicleFilter vehicleFilter;
	private Map<Id<Vehicle>, Integer> vehicleId2passengers = new HashMap<>();
	
	private SortedMap<Double, Map<Id<Link>, Integer>> timeBinEndTime2linkId2vehicles = new TreeMap<Double, Map<Id<Link>, Integer>>();
	private SortedMap<Double, Map<Id<Link>, Integer>> timeBinEndTime2linkId2vehiclePassengers = new TreeMap<Double, Map<Id<Link>, Integer>>();

	public DynamicLinkDemandEventHandler() {
		this.vehicleFilter = null;
	}
	
	public DynamicLinkDemandEventHandler(VehicleFilter vehicleFilter) {
		this.vehicleFilter = vehicleFilter;
	}

	@Override
	public void reset(int iteration) {
		this.timeBinEndTime2linkId2vehicles.clear();
		this.timeBinEndTime2linkId2vehiclePassengers.clear();
		this.vehicleId2passengers.clear();
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		
		if (vehicleFilter == null || vehicleFilter.considerVehicle(event.getVehicleId())) {
			double currentTimeBin = (Math.floor(event.getTime() / this.timeBinSize) + 1) * this.timeBinSize;
			
			if (this.timeBinEndTime2linkId2vehicles.containsKey(currentTimeBin)) {
				if (this.timeBinEndTime2linkId2vehicles.get(currentTimeBin).containsKey(event.getLinkId())) {
					
					int vehicles = this.timeBinEndTime2linkId2vehicles.get(currentTimeBin).get(event.getLinkId());
					this.timeBinEndTime2linkId2vehicles.get(currentTimeBin).put(event.getLinkId(), vehicles + 1);
					
					int passengers = this.timeBinEndTime2linkId2vehiclePassengers.get(currentTimeBin).get(event.getLinkId());
					this.timeBinEndTime2linkId2vehiclePassengers.get(currentTimeBin).put(event.getLinkId(), passengers + this.vehicleId2passengers.get(event.getVehicleId()));
					
				} else {
					this.timeBinEndTime2linkId2vehicles.get(currentTimeBin).put(event.getLinkId(), 1);
					this.timeBinEndTime2linkId2vehiclePassengers.get(currentTimeBin).put(event.getLinkId(), this.vehicleId2passengers.get(event.getVehicleId()));
				}
				
			} else {
				Map<Id<Link>, Integer> linkId2vehicles = new HashMap<Id<Link>, Integer>();
				linkId2vehicles.put(event.getLinkId(), 1);
				this.timeBinEndTime2linkId2vehicles.put(currentTimeBin, linkId2vehicles);
				
				Map<Id<Link>, Integer> linkId2passengers = new HashMap<Id<Link>, Integer>();
				linkId2passengers.put(event.getLinkId(), this.vehicleId2passengers.get(event.getVehicleId()));
				this.timeBinEndTime2linkId2vehiclePassengers.put(currentTimeBin, linkId2passengers);

			}
		}
	}

	public void printResults(String path) {
		// print results for vehicles
		{
			
			Set<Id<Link>> linkIDs = new HashSet<>();
			for (Double timeBinEndTime : this.timeBinEndTime2linkId2vehicles.keySet()) {
				for (Id<Link> linkID : this.timeBinEndTime2linkId2vehicles.get(timeBinEndTime).keySet()) {
					if (!linkIDs.contains(linkID)) {
						linkIDs.add(linkID);
					}
				}
			}

			
			String fileName;
			if (this.vehicleFilter == null) {
				fileName = path + "link_hourlyTrafficVolume_vehicles.csv";
			} else {
				fileName = path + "link_hourlyTrafficVolume_vehicles" + this.vehicleFilter.toFileName() + ".csv";
			}
			
			File file1 = new File(fileName);
			
			try {
				BufferedWriter bw1 = new BufferedWriter(new FileWriter(file1));

				bw1.write("link");
				
				for (Double timeBinEndTime : this.timeBinEndTime2linkId2vehicles.keySet()) {
					bw1.write(";" + Time.writeTime(timeBinEndTime, Time.TIMEFORMAT_HHMMSS));
				}
				bw1.newLine();
				
				for (Id<Link> linkId : linkIDs){
					
					bw1.write(linkId.toString());
					
					for (Double timeBinEndTime : this.timeBinEndTime2linkId2vehicles.keySet()) {
						int agents = 0;
						if (this.timeBinEndTime2linkId2vehicles.get(timeBinEndTime).containsKey(linkId)) {
							agents = this.timeBinEndTime2linkId2vehicles.get(timeBinEndTime).get(linkId);
						}
						bw1.write(";" + agents);
					}
					bw1.newLine();
				}
				
				bw1.close();
				log.info("Output written to " + fileName);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		// print results for passengers
		{
			
			Set<Id<Link>> linkIDs = new HashSet<>();
			for (Double timeBinEndTime : this.timeBinEndTime2linkId2vehicles.keySet()) {
				for (Id<Link> linkID : this.timeBinEndTime2linkId2vehicles.get(timeBinEndTime).keySet()) {
					if (!linkIDs.contains(linkID)) {
						linkIDs.add(linkID);
					}
				}
			}
			
			String fileName;
			if (this.vehicleFilter == null) {
				fileName = path + "link_hourlyTrafficVolume_passengers.csv";
			} else {
				fileName = path + "link_hourlyTrafficVolume_passengers" + this.vehicleFilter.toFileName() + ".csv";
			}
			
			File file1 = new File(fileName);
			
			try {
				BufferedWriter bw1 = new BufferedWriter(new FileWriter(file1));

				bw1.write("link");
				
				for (Double timeBinEndTime : this.timeBinEndTime2linkId2vehiclePassengers.keySet()) {
					bw1.write(";" + Time.writeTime(timeBinEndTime, Time.TIMEFORMAT_HHMMSS));
				}
				bw1.newLine();
				
				for (Id<Link> linkId : linkIDs){
					
					bw1.write(linkId.toString());
					
					for (Double timeBinEndTime : this.timeBinEndTime2linkId2vehiclePassengers.keySet()) {
						int agents = 0;
						if (this.timeBinEndTime2linkId2vehiclePassengers.get(timeBinEndTime).containsKey(linkId)) {
							agents = this.timeBinEndTime2linkId2vehiclePassengers.get(timeBinEndTime).get(linkId);
						}
						bw1.write(";" + agents);
					}
					bw1.newLine();
				}
				
				bw1.close();
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

}
