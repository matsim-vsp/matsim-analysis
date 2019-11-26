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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFareModule;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFaresConfigGroup;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

public class MatsimAnalysisRunDrtTest {
	private static final Logger log = Logger.getLogger(MatsimAnalysisRunDrtTest.class);
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	@Test
	public final void test1() {
		
		{
			Config config = ConfigUtils.loadConfig(testUtils.getPackageInputDirectory() + "equi-drt/config-with-drt.xml", new MultiModeDrtConfigGroup(), new DvrpConfigGroup(), new DrtFaresConfigGroup());
			config.controler().setOutputDirectory(testUtils.getOutputDirectory() + "output1");
			config.controler().setRunId("run1");
			config.controler().setLastIteration(1);

			DrtConfigs.adjustMultiModeDrtConfig(MultiModeDrtConfigGroup.get(config), config.planCalcScore(), config.plansCalcRoute());
			
			Scenario scenario = ScenarioUtils.loadScenario(config);
			RouteFactories routeFactories = scenario.getPopulation().getFactory().getRouteFactories();
			routeFactories.setRouteFactory(DrtRoute.class, new DrtRouteFactory());
			
			Controler controler = new Controler(scenario);		
			controler.addOverridingModule(new MultiModeDrtModule());
			controler.addOverridingModule(new DvrpModule());
			controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(MultiModeDrtConfigGroup.get(controler.getConfig())));				
			controler.addOverridingModule(new DrtFareModule());
						
			controler.run();
		}
		
		{
			Config config = ConfigUtils.loadConfig(testUtils.getPackageInputDirectory() + "equi/config.xml");
			config.controler().setOutputDirectory(testUtils.getOutputDirectory() + "output0");
			config.controler().setRunId("run0");
			config.controler().setLastIteration(0);
			config.plans().setInsistingOnUsingDeprecatedPersonAttributeFile(true);
			Scenario scenario = ScenarioUtils.loadScenario(config) ;
			Controler controler = new Controler( scenario ) ;
			controler.run();
		}
		
		final String runId = "run1";
		final String runDirectory = testUtils.getOutputDirectory() +  "output1/";
		
		final String runIdBaseCase = "run0";
		final String runDirectoryBaseCase = testUtils.getOutputDirectory() + "output0/";

		final Scenario scenario1 = loadScenario(runDirectory, runId);
		final Scenario scenario0 = loadScenario(runDirectoryBaseCase, runIdBaseCase);
		
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
		analysis.setScenario0(scenario0);
		analysis.setHomeActivityPrefix(homeActivityPrefix);
		analysis.setScalingFactor(scalingFactor);
		analysis.setAgentFilters(agentFilters);
		analysis.setTripFilters(tripFilters);
		analysis.setModes(modes);
		analysis.setVisualizationScriptInputDirectory("./visualization-scripts/");
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
			
			if (new File(configFile).exists()) {

				configFile = runDirectory + runId + ".output_config.xml";
				networkFile = runId + ".output_network.xml.gz";
				populationFile = runId + ".output_plans.xml.gz";
				
			} else {

				configFile = runDirectory + "output_config.xml";
				log.info("Setting config file to " + configFile);
				
				networkFile = runDirectory + "output_network.xml.gz";
				populationFile = runDirectory + "output_plans.xml.gz";
				
				log.info("Trying to load config file " + configFile);
				
			}

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
		

