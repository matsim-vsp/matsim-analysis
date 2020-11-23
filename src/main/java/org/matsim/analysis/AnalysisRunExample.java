/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class AnalysisRunExample {
	private static final Logger log = Logger.getLogger(AnalysisRunExample.class);
			
	public static void main(String[] args) throws IOException {
			
		final String runDirectory = "C:\\Users\\cluac\\MATSimScenarios\\Berlin\\output\\001";
		final String runId = null;		
		final String modesString = "car,pt,walk,bike";
		final String scenarioCRS = null;
		
		Scenario scenario1 = loadScenario(runDirectory, runId, scenarioCRS);
		
		List<AgentFilter> filters1 = new ArrayList<>();
		AgentAnalysisFilter filter1a = new AgentAnalysisFilter("A");
		filter1a.preProcess(scenario1);
		filters1.add(filter1a);
				
		List<String> modes = new ArrayList<>();
		for (String mode : modesString.split(",")) {
			modes.add(mode);
		}
		
		MatsimAnalysis analysis = new MatsimAnalysis();
		analysis.setScenario1(scenario1);
		analysis.setAgentFilters(filters1);
		analysis.setModes(modes);
		analysis.setScenarioCRS(scenarioCRS);
		analysis.run();
	}
	
	private static Scenario loadScenario(String runDirectory, String runId, String scenarioCRS) {
		log.info("Loading scenario...");
		
		if (runDirectory == null || runDirectory.equals("") || runDirectory.equals("null")) {
			return null;	
		}
		
		if (!runDirectory.endsWith("/")) runDirectory = runDirectory + "/";

		String networkFile;
		String populationFile;
		String facilitiesFile;
		
		networkFile = runDirectory  + "output_network.xml.gz";
		populationFile = runDirectory  + "output_plans.xml.gz";
		facilitiesFile = runDirectory  + "output_facilities.xml.gz";

		Config config = ConfigUtils.createConfig();

		config.global().setCoordinateSystem(scenarioCRS);
		config.controler().setRunId(runId);
		config.controler().setOutputDirectory(runDirectory);
		config.plans().setInputFile(populationFile);
		config.network().setInputFile(networkFile);
		config.facilities().setInputFile(facilitiesFile);
		
		return ScenarioUtils.loadScenario(config);
	}

}
		

