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
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class AnalysisRunExampleOpenBerlinScenario {
	private static final Logger log = Logger.getLogger(AnalysisRunExampleOpenBerlinScenario.class);
			
	public static void main(String[] args) throws IOException {
		
		final String runDirectory = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct/output-berlin-v5.4-1pct/";
		final String runId = "berlin-v5.4-1pct";		
		
		final String runDirectoryToCompareWith = null;
		final String runIdToCompareWith = null;
		
		final String scenarioCRS = TransformationFactory.DHDN_GK4;	

		Scenario scenario1 = loadScenario(runDirectory, runId, scenarioCRS);
		Scenario scenario0 = loadScenario(runDirectoryToCompareWith, runIdToCompareWith, scenarioCRS);
		
		final String modesString = "car,pt,bicycle,walk,ride";

		final int scalingFactor = 100;
		final String homeActivityPrefix = "home";

		final String shapeFileZones = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/shp-files/shp-stadtteile-split-zone-3/Bezirksregionen_zone_GK4_fixed.shp";
		final String zonesCRS = TransformationFactory.DHDN_GK4;
		final String zoneId = "NO";

		final String analysisOutputDirectory = "./test/output/analysis-" + runId;

		final String visualizationScriptInputDirectory = "./visualization-scripts/";

		final List<AgentFilter> agentFilters = new ArrayList<>();

		AgentAnalysisFilter filter1a = new AgentAnalysisFilter("A");
		filter1a.setSubpopulation("person");
		filter1a.setPersonAttribute("berlin");
		filter1a.setPersonAttributeName("home-activity-zone");
		filter1a.preProcess(scenario1);
		agentFilters.add(filter1a);
		
		AgentAnalysisFilter filter1b = new AgentAnalysisFilter("B");
		filter1b.preProcess(scenario1);
		agentFilters.add(filter1b);
		
		AgentAnalysisFilter filter1c = new AgentAnalysisFilter("C");
		filter1c.setSubpopulation("person");
		filter1c.setPersonAttribute("brandenburg");
		filter1c.setPersonAttributeName("home-activity-zone");
		filter1c.preProcess(scenario1);
		agentFilters.add(filter1c);
				
		final List<TripFilter> tripFilters = new ArrayList<>();
		
		TripAnalysisFilter tripFilter1a = new TripAnalysisFilter("A");
		tripFilter1a.preProcess(scenario1);
		tripFilters.add(tripFilter1a);
		
		TripAnalysisFilter tripFilter1b = new TripAnalysisFilter("B");
		tripFilter1b.setZoneInformation(shapeFileZones, zonesCRS);
		tripFilter1b.preProcess(scenario1);
		tripFilters.add(tripFilter1b);
				
		List<String> modes = new ArrayList<>();
		for (String mode : modesString.split(",")) {
			modes.add(mode);
		}
		
		MatsimAnalysis analysis = new MatsimAnalysis();
		
		analysis.setScenario1(scenario1);	
		analysis.setScenario0(scenario0);
		
		analysis.setAgentFilters(agentFilters);
		analysis.setTripFilters(tripFilters);
		
		analysis.setScenarioCRS(scenarioCRS);
		analysis.setZoneInformation(shapeFileZones, zonesCRS, zoneId);
		
		analysis.setModes(modes);
		analysis.setVisualizationScriptInputDirectory(visualizationScriptInputDirectory);
		analysis.setHomeActivityPrefix(homeActivityPrefix);
		analysis.setScalingFactor(scalingFactor);
		
		analysis.setAnalysisOutputDirectory(analysisOutputDirectory);		
		analysis.run();
	}
	
	private static Scenario loadScenario(String runDirectory, String runId, String scenarioCRS) {
		log.info("Loading scenario...");
		
		if (runDirectory == null || runDirectory.equals("") || runDirectory.equals("null")) {
			return null;	
		}
		
		if (!runDirectory.endsWith("/")) runDirectory = runDirectory + "/";
	
		String networkFile = runDirectory + runId + ".output_network.xml.gz";
		String populationFile = runDirectory + runId + ".output_plans.xml.gz";
		
		Config config = ConfigUtils.createConfig();

		config.global().setCoordinateSystem(TransformationFactory.GK4);
		config.controler().setRunId(runId);
		config.controler().setOutputDirectory(runDirectory);
		config.plans().setInputFile(populationFile);
		config.network().setInputFile(networkFile);
		
		return ScenarioUtils.loadScenario(config);
	}

}
		

