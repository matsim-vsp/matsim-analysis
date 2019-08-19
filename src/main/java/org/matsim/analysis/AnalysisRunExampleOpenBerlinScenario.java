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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class AnalysisRunExampleOpenBerlinScenario {
	private static final Logger log = Logger.getLogger(AnalysisRunExampleOpenBerlinScenario.class);
			
	public static void main(String[] args) throws IOException {
		
		final String runDirectory = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.3-1pct/output-berlin-v5.3-1pct/";
		final String runId = "berlin-v5.3-1pct";		
		
		final String runDirectoryToCompareWith = null;
		final String runIdToCompareWith = null;
		
		final String visualizationScriptInputDirectory = "./visualization-scripts/";
		
		final String scenarioCRS = TransformationFactory.DHDN_GK4;	
		
		final String shapeFileZones = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/berlin-sav-v5.2-10pct/input/shp-stadtteile-split-zone-3/Bezirksregionen_zone_GK4_fixed.shp";
		final String zonesCRS = TransformationFactory.DHDN_GK4;
		final String zoneId = "NO";
		
		final String homeActivityPrefix = "home";
		final int scalingFactor = 100;
		
		final String modesString = "car,pt,bicycle,walk,ride";
		
		final String analysisOutputDirectory = "./scenarios/";
				
		final String[] helpLegModes = {TransportMode.transit_walk, TransportMode.non_network_walk, TransportMode.access_walk, TransportMode.egress_walk};
		final StageActivityTypes stageActivities = new StageActivityTypesImpl("pt interaction", "car interaction", "ride interaction", "bike interaction", "bicycle interaction", "drt interaction");
		
		Scenario scenario1 = loadScenario(runDirectory, runId, analysisOutputDirectory);
		Scenario scenario0 = loadScenario(runDirectoryToCompareWith, runIdToCompareWith, analysisOutputDirectory);
		
		List<AgentAnalysisFilter> filter1 = new ArrayList<>();

		AgentAnalysisFilter filter1a = new AgentAnalysisFilter();
		filter1a.setSubpopulation("person");
		filter1a.setPersonAttribute("berlin");
		filter1a.setPersonAttributeName("home-activity-zone");
		filter1a.preProcess(scenario1);
		filter1.add(filter1a);
		
		AgentAnalysisFilter filter1b = new AgentAnalysisFilter();
		filter1b.preProcess(scenario1);
		filter1.add(filter1b);
		
		AgentAnalysisFilter filter1c = new AgentAnalysisFilter();
		filter1c.setSubpopulation("person");
		filter1c.setPersonAttribute("brandenburg");
		filter1c.setPersonAttributeName("home-activity-zone");
		filter1c.preProcess(scenario1);
		filter1.add(filter1c);
		
		List<AgentAnalysisFilter> filter0 = null;
		
		List<String> modes = new ArrayList<>();
		for (String mode : modesString.split(",")) {
			modes.add(mode);
		}

		MatsimAnalysis analysis = new MatsimAnalysis(
				scenario1,
				scenario0,
				visualizationScriptInputDirectory,
				scenarioCRS,
				shapeFileZones,
				zonesCRS,
				homeActivityPrefix,
				scalingFactor,
				filter1,
				filter0,
				modes,
				zoneId,
				helpLegModes,
				stageActivities);
		analysis.run();
	}
	
	private static Scenario loadScenario(String runDirectory, String runId, String analysisOutputDirectory) {
		log.info("Loading scenario...");
		
		if (runDirectory == null) {
			return null;	
		}
		
		if (runDirectory.equals("")) {
			return null;	
		}
		
		if (runDirectory.equals("null")) {
			return null;	
		}
		
		if (!runDirectory.endsWith("/")) runDirectory = runDirectory + "/";

		String networkFile;
		String populationFile;
		String personAttributesFile;
		String configFile;
		
		configFile = runDirectory + runId + ".output_config.xml";
		
		networkFile = runDirectory + runId + ".output_network.xml.gz";
		populationFile = runDirectory + runId + ".output_plans.xml.gz";
		personAttributesFile = runDirectory + runId + ".output_personAttributes.xml.gz";
		
		Config config = ConfigUtils.createConfig();

		if (config.controler().getRunId() != null) {
			if (!runId.equals(config.controler().getRunId())) throw new RuntimeException("Given run ID " + runId + " doesn't match the run ID given in the config file. Aborting...");
		} else {
			config.controler().setRunId(runId);
		}

		config.controler().setOutputDirectory(analysisOutputDirectory);
		config.plans().setInputFile(populationFile);
		config.plans().setInsistingOnUsingDeprecatedPersonAttributeFile(true);
		config.plans().setInputPersonAttributeFile(personAttributesFile);
		config.network().setInputFile(networkFile);
		config.vehicles().setVehiclesFile(null);
		config.transit().setTransitScheduleFile(null);
		config.transit().setVehiclesFile(null);
		
		return ScenarioUtils.loadScenario(config);
	}

}
		

