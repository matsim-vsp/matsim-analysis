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

package org.matsim.analysis.vehicleOperatingTimes;

import org.matsim.analysis.VehicleAnalysisFilter;
import org.matsim.analysis.VehicleAnalysisFilter.StringComparison;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

/**
* @author ikaddoura
*/

public class RunVehicleOperatingTimesAnalysis {

	public static void main(String[] args) {
		
		String outputDirectory = "../runs-svn/avoev/snz-vulkaneifel/output-drtFleetA/";
		String runId = "drtFleetA";
		EventsManager events = EventsUtils.createEventsManager();
				
		VehicleAnalysisFilter vehicleFilter = new VehicleAnalysisFilter("drt-vehicles", "rt", StringComparison.Contains);
		OperatingTimesEventHandler handler = new OperatingTimesEventHandler(vehicleFilter);
		events.addHandler(handler);
		
		String eventsFile = outputDirectory + runId + ".output_events.xml.gz";
		MatsimEventsReader reader = new MatsimEventsReader(events);

		events.initProcessing();
		reader.readFile(eventsFile);
		events.finishProcessing();

		handler.printResults(outputDirectory + runId + ".");
	}

}

