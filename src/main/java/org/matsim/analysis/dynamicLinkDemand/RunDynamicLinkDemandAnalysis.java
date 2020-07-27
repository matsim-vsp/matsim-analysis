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

package org.matsim.analysis.dynamicLinkDemand;

import org.matsim.analysis.VehicleAnalysisFilter;
import org.matsim.analysis.VehicleAnalysisFilter.StringComparison;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
* @author ikaddoura
*/

public class RunDynamicLinkDemandAnalysis {

	public static void main(String[] args) {
		
		String outputDirectory = "../runs-svn/avoev/snz-vulkaneifel/output-drt0/";
		String runId = "drt0";
		
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem("epsg:25832");
		config.network().setInputFile(outputDirectory + runId + ".output_network.xml.gz");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		VehicleAnalysisFilter vehicleFilter = new VehicleAnalysisFilter("pt-vehicles", "tr", StringComparison.Contains);
		DynamicLinkDemandEventHandler handler = new DynamicLinkDemandEventHandler(scenario.getNetwork(), vehicleFilter);
		
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(handler);
		
		String eventsFile = outputDirectory + runId + ".output_events.xml.gz";
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		
		handler.printResults(outputDirectory + runId + ".");
	}

}

