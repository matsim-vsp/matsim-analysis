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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.matsim.analysis.TripAnalysisFilter.TripConsiderType.OriginAndDestination;
import static org.matsim.analysis.TripAnalysisFilter.TripConsiderType.OriginOrDestination;

public class AnalysisRunExampleOpenBerlinScenarioIntermodal {
	private static final Logger log = Logger.getLogger(AnalysisRunExampleOpenBerlinScenarioIntermodal.class);
			
	public static void main(String[] args) throws IOException {

		String runDirectory = null;
		String runId = null;
		String runDirectoryToCompareWith = null;
		String runIdToCompareWith = null;
		String visualizationScriptInputDirectory = null;
		String scenarioCRS = null;
		String shapeFileZones = null;
		String shapFileZonesCRS = null;
		String shapeFileAgentFilter = null;
		String zoneId = null;
		String shapeFileTripFilter = null;
		String shapeFileTripFilterCRS = null;
		String bufferMAroundTripFilterShp = null;

		final String[] helpLegModes = {TransportMode.walk, TransportMode.non_network_walk};
		final int scalingFactor = 10;
		final String homeActivityPrefix = "home";
		final String modesString = TransportMode.car + "," + TransportMode.pt + "," + "bicycle" + "," + TransportMode.walk + "," + TransportMode.ride + "," + TransportMode.drt + "," + IntermodalPtDrtRouterAnalysisModeIdentifier.ANALYSIS_MAIN_MODE_PT_WITH_DRT_USED_FOR_ACCESS_OR_EGRESS;

		if (args.length > 0) {
			runDirectory = args[0];
			runId = args[1];

			runDirectoryToCompareWith = args[2];
			runIdToCompareWith = args[3];

			if(!args[4].equals("null")) visualizationScriptInputDirectory = args[4];

			scenarioCRS = args[5];

			shapeFileZones = args[6];
			shapFileZonesCRS = args[7];
			zoneId = args[8];

			shapeFileAgentFilter = args[9];

			shapeFileTripFilter = args[10];
			shapeFileTripFilterCRS = args[11];

			bufferMAroundTripFilterShp = args[12];
		} else {
			//TODO
			runDirectory = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-1pct/output-berlin-v5.5-1pct/";
			runId = "berlin-v5.5-1pct";
			runDirectoryToCompareWith = null;
			runIdToCompareWith = null;
			visualizationScriptInputDirectory = "./visualization-scripts/";
			scenarioCRS = TransformationFactory.DHDN_GK4;
			shapeFileZones = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/shp-files/shp-stadtteile-split-zone-3/Bezirksregionen_zone_GK4_fixed.shp";
			shapFileZonesCRS = TransformationFactory.DHDN_GK4;
			zoneId = "NO";
			shapeFileAgentFilter = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/shp-files/shp-spandau/spandau_b.shp";
			shapeFileTripFilter = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/shp-files/shp-spandau/spandau_b.shp";
			shapeFileTripFilterCRS = TransformationFactory.DHDN_GK4;
			bufferMAroundTripFilterShp = "2000";
		}

		Scenario scenario1 = loadScenario(runDirectory, runId, scenarioCRS);
		Scenario scenario0 = loadScenario(runDirectoryToCompareWith, runIdToCompareWith, scenarioCRS);

		final List<AgentFilter> agentFilters = new ArrayList<>();

		AgentAnalysisFilter agentFilterShape = new AgentAnalysisFilter("A");
		agentFilterShape.setSubpopulation("person");
		agentFilterShape.setZoneFile(shapeFileAgentFilter);
		agentFilterShape.setRelevantActivityType(homeActivityPrefix);
		agentFilterShape.preProcess(scenario1); // has to be done after setting shape file
		agentFilters.add(agentFilterShape);

		AgentAnalysisFilter filter1b = new AgentAnalysisFilter("B");
		filter1b.preProcess(scenario1);
		agentFilters.add(filter1b);

		AgentAnalysisFilter filter1c = new AgentAnalysisFilter("C");
		filter1c.setSubpopulation("person");
		filter1c.setPersonAttribute("brandenburg");
		filter1c.setPersonAttributeName("home-activity-zone");
		filter1c.preProcess(scenario1);
		agentFilters.add(filter1c);

		AgentAnalysisFilter agentFilter1d = new AgentAnalysisFilter("Berlin");
		agentFilter1d.setSubpopulation("person");
		agentFilter1d.setPersonAttribute("berlin");
		agentFilter1d.setPersonAttributeName("home-activity-zone");
		agentFilter1d.preProcess(scenario1);
		agentFilters.add(agentFilter1d);

		final List<TripFilter> tripFilters = new ArrayList<>();

		TripAnalysisFilter tripFilter1a = new TripAnalysisFilter("A");
		tripFilter1a.preProcess(scenario1);
		tripFilters.add(tripFilter1a);

		TripAnalysisFilter tripFilter1b = new TripAnalysisFilter("B");
		tripFilter1b.setZoneInformation(shapeFileTripFilter, shapeFileTripFilterCRS);
		tripFilter1b.setBuffer(Double.valueOf(bufferMAroundTripFilterShp));
		tripFilter1b.setTripConsiderType(OriginOrDestination);
		tripFilter1b.preProcess(scenario1);
		tripFilters.add(tripFilter1b);

		TripAnalysisFilter tripFilter1c = new TripAnalysisFilter("C");
		tripFilter1c.setZoneInformation(shapeFileTripFilter, shapeFileTripFilterCRS);
		tripFilter1c.setBuffer(0);
		tripFilter1c.setTripConsiderType(OriginAndDestination);
		tripFilter1c.preProcess(scenario1);
		tripFilters.add(tripFilter1c);

		final List<VehicleFilter> vehicleFilters = new ArrayList<>();

		VehicleAnalysisFilter vehicleAnalysisFilter0 = null;
		vehicleFilters.add(vehicleAnalysisFilter0);

		VehicleAnalysisFilter vehicleAnalysisFilter1 = new VehicleAnalysisFilter("drt-vehicles", "rt", VehicleAnalysisFilter.StringComparison.Contains);
		vehicleFilters.add(vehicleAnalysisFilter1);

		VehicleAnalysisFilter vehicleAnalysisFilter2 = new VehicleAnalysisFilter("pt-vehicles", "tr", VehicleAnalysisFilter.StringComparison.Contains);
		vehicleFilters.add(vehicleAnalysisFilter2);

		List<String> modes = new ArrayList<>();
		for (String mode : modesString.split(",")) {
			modes.add(mode);
		}

		MatsimAnalysis analysis = new MatsimAnalysis();

		analysis.setScenario1(scenario1);
		analysis.setScenario0(scenario0);

		analysis.setAgentFilters(agentFilters);
		analysis.setTripFilters(tripFilters);
		analysis.setVehicleFilters(vehicleFilters);

		analysis.setScenarioCRS(scenarioCRS);
		analysis.setZoneInformation(shapeFileZones, shapFileZonesCRS, zoneId);

		analysis.setModes(modes);
		analysis.setVisualizationScriptInputDirectory(visualizationScriptInputDirectory);
		analysis.setHomeActivityPrefix(homeActivityPrefix);
		analysis.setScalingFactor(scalingFactor);
		analysis.setHelpLegModes(helpLegModes);
		analysis.setMainModeIdentifier(new IntermodalPtDrtRouterAnalysisModeIdentifier());

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
		String facilitiesFile = runDirectory + runId + ".output_facilities.xml.gz";

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