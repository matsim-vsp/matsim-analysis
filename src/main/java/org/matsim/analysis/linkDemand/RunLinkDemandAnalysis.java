/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.analysis.linkDemand;

import org.matsim.analysis.VehicleAnalysisFilter;
import org.matsim.analysis.VehicleAnalysisFilter.StringComparison;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

/**
* @author ikaddoura
*/

public class RunLinkDemandAnalysis {

	public static void main(String[] args) {
		
//		String outputDirectory = "../runs-svn/avoev/snz-gladbeck/output-snzDrtO443g/";
//		String runId = "snzDrtO443g";
		
//		String outputDirectory = "../runs-svn/avoev/snz-gladbeck/output-snzDrtO443l/";
//		String runId = "snzDrtO443l";
		
//		String outputDirectory = "../runs-svn/avoev/snz-gladbeck/output-snzDrtO442g/";
//		String runId = "snzDrtO442g";
		
//		String outputDirectory = "../runs-svn/avoev/snz-gladbeck/output-snzDrtO442l/";
//		String runId = "snzDrtO442l";
		
//		String outputDirectory = "../runs-svn/avoev/snz-vulkaneifel/output-snzDrtO320g/";
//		String runId = "snzDrtO320g";
		
		String outputDirectory = "../runs-svn/avoev/snz-vulkaneifel/output-snzDrtO321g/";
		String runId = "snzDrtO321g";
		
		VehicleAnalysisFilter vehicleFilter1 = new VehicleAnalysisFilter("pt-vehicles", "tr", StringComparison.Contains);
		LinkDemandEventHandler handler1 = new LinkDemandEventHandler(vehicleFilter1);
		
		VehicleAnalysisFilter vehicleFilter2 = new VehicleAnalysisFilter("drt-vehicles", "rt", StringComparison.Contains);
		LinkDemandEventHandler handler2 = new LinkDemandEventHandler(vehicleFilter2);
		
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(handler1);
		events.addHandler(handler2);
		
		String eventsFile = outputDirectory + runId + ".output_events.xml.gz";
		MatsimEventsReader reader = new MatsimEventsReader(events);
		events.initProcessing();
		reader.readFile(eventsFile);
		events.finishProcessing();

		handler1.printResults(outputDirectory + runId + ".");
		handler2.printResults(outputDirectory + runId + ".");
	}

}

