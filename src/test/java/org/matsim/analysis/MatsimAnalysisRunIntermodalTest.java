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

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

public class MatsimAnalysisRunIntermodalTest {
	private static final Logger log = LogManager.getLogger(MatsimAnalysisRunIntermodalTest.class);
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	@Test
	public final void test1() {
		
		final String runId = "berlin-drt-v5.5-1pct";
		final String runDirectory = testUtils.getPackageInputDirectory() +  "intermodal-test-run/";

		final Scenario scenario1 = loadScenario(runDirectory, runId);
		
		final List<AgentFilter> agentFilters = new ArrayList<>();
		
		AgentAnalysisFilter filter1b = new AgentAnalysisFilter("B");
		filter1b.preProcess(scenario1);
		agentFilters.add(filter1b);
		
		final List<TripFilter> tripFilters = new ArrayList<>();
		
		TripAnalysisFilter tripAnalysisFilter0 = new TripAnalysisFilter("no-filter");
		tripAnalysisFilter0.preProcess(scenario1);
		tripFilters.add(tripAnalysisFilter0);
		
		final List<String> modes = new ArrayList<>();
		modes.add(TransportMode.car);
		modes.add(TransportMode.pt);
		modes.add("bicycle");
		
		final String homeActivityPrefix = "h";
		final int scalingFactor = 100;

		MatsimAnalysis analysis = new MatsimAnalysis();
		analysis.setScenarioCRS("EPSG:31468");
		analysis.setPrintTripSHPfiles(true);
		analysis.setScenario1(scenario1);
		analysis.setScenario0(null);
		analysis.setHomeActivityPrefix(homeActivityPrefix);
		analysis.setScalingFactor(scalingFactor);
		analysis.setAgentFilters(agentFilters);
		analysis.setTripFilters(tripFilters);
		analysis.setModes(modes);
		analysis.setVisualizationScriptInputDirectory("./visualization-scripts/");
		analysis.setAnalysisOutputDirectory(testUtils.getOutputDirectory());
		analysis.run();
	
		log.info("Done.");
	}
	
	@Test
	public final void test2() {
		
		final String runId = "berlin-drt-v5.5-1pct";
		final String runDirectory = testUtils.getPackageInputDirectory() +  "intermodal-test-run/";

		final Scenario scenario1 = loadScenario(runDirectory, runId);
		
		final List<AgentFilter> agentFilters = new ArrayList<>();
		
		AgentAnalysisFilter filter1b = new AgentAnalysisFilter("B");
		filter1b.preProcess(scenario1);
		agentFilters.add(filter1b);
		
		final List<TripFilter> tripFilters = new ArrayList<>();
		
		TripAnalysisFilter tripAnalysisFilter0 = new TripAnalysisFilter("no-filter");
		tripAnalysisFilter0.preProcess(scenario1);
		tripFilters.add(tripAnalysisFilter0);
		
		final List<String> modes = new ArrayList<>();
		modes.add(TransportMode.car);
		modes.add(TransportMode.pt);
		modes.add("bicycle");
		modes.add("pt_w_drt_used");
		
		final String homeActivityPrefix = "h";
		final int scalingFactor = 100;

		MatsimAnalysis analysis = new MatsimAnalysis();
		analysis.setScenarioCRS("EPSG:31468");
		analysis.setPrintTripSHPfiles(true);
		analysis.setScenario1(scenario1);
		analysis.setScenario0(null);
		analysis.setHomeActivityPrefix(homeActivityPrefix);
		analysis.setScalingFactor(scalingFactor);
		analysis.setAgentFilters(agentFilters);
		analysis.setTripFilters(tripFilters);
		analysis.setModes(modes);
		analysis.setVisualizationScriptInputDirectory("./visualization-scripts/");
		analysis.setAnalysisOutputDirectory(testUtils.getOutputDirectory());
		analysis.setMainModeIdentifier(new IntermodalPtDrtRouterAnalysisModeIdentifier());
		analysis.run();
	
		log.info("Done.");
	}
	
	private static Scenario loadScenario(String runDirectory, String runId) {
		
		if (runDirectory == null) {
			return null;
			
		} else {
			
			if (!runDirectory.endsWith("/")) runDirectory = runDirectory + "/";

			String networkFile;
			String populationFile;
			String configFile = runDirectory + runId + ".output_config.xml";
			log.info("Setting config file to " + configFile);
			
			configFile = runDirectory + runId + ".output_config.xml";
			networkFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz";
			populationFile = runId + ".output_plans.xml.gz";
				
			

			Config config = ConfigUtils.loadConfig(configFile);
			
			log.info("Setting run directory to " + runDirectory);
			config.controler().setOutputDirectory(runDirectory);

			if (config.controler().getRunId() != null) {
				if (!runId.equals(config.controler().getRunId())) throw new RuntimeException("Given run ID " + runId + " doesn't match the run ID given in the config file. Aborting...");
			} else {
				log.info("Setting run Id to " + runId);
				config.controler().setRunId(runId);
			}

			config.plans().setInputFile(populationFile);
			config.network().setInputFile(networkFile);
			config.vehicles().setVehiclesFile(null);
			config.transit().setTransitScheduleFile(null);
			config.transit().setVehiclesFile(null);
			
			return ScenarioUtils.loadScenario(config);
		}
	}
}
		

