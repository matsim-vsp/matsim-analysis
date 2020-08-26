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
import org.matsim.analysis.TripAnalysisFilter.TripConsiderType;
import org.matsim.analysis.VehicleAnalysisFilter.StringComparison;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class AnalysisRunExampleSnzScenario {
	private static final Logger log = Logger.getLogger(AnalysisRunExampleSnzScenario.class);
			
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

		final String[] helpLegModes = {TransportMode.transit_walk, TransportMode.walk, TransportMode.non_network_walk, "access_walk", "egress_walk"}; // to be able to analyze old runs
		final int scalingFactor = 4;
		final String homeActivityPrefix = "home";
		final String modesString = TransportMode.car + "," + TransportMode.pt + "," + TransportMode.bike + "," + TransportMode.walk + "," + TransportMode.ride + "," + TransportMode.drt;

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
			
			// Berlin
			
//			runDirectory = "../runs-svn/avoev/snz-model/output_2019-06-11_snz-drt-3/";
//			runId = "snz-drt-3";		
//			
//			runDirectoryToCompareWith = null;
//			runIdToCompareWith = null;
//			
//			visualizationScriptInputDirectory = "./visualization-scripts/";
//			
//			scenarioCRS = "EPSG:25832";
//			
//			shapeFileZones = "../../shared-svn/projects/avoev/data/berlkoenig-od-trips/Bezirksregionen_zone_UTM32N/Bezirksregionen_zone_UTM32N_fixed.SHP";
//			shapFileZonesCRS = "EPSG:25832";
//			zoneId = "NO";
//						
//			shapeFileAgentFilter = "../../shared-svn/projects/avoev/data/berlin-area/berlin-area_EPSG25832.shp";
//			
//			shapeFileTripFilter = "../../shared-svn/projects/avoev/matsim-input-files/berlin/v1/berlkoenig-area-shp/berlkoenig-area.shp";
//			shapeFileTripFilterCRS = "EPSG:25832";
			
			// Vulkaneifel
			
//			runDirectory = "../runs-svn/avoev/snz-vulkaneifel/output-snzDrtO321g/";
//			runId = "snzDrtO321g";		
//			
//			runDirectoryToCompareWith = null;
//			runIdToCompareWith = null;
//			
//			visualizationScriptInputDirectory = null;
//			
//			scenarioCRS = "EPSG:25832";
//			
//			shapeFileZones = "../shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v1/shp-files/hexagon-grid-3000.shp";
//			shapFileZonesCRS = "EPSG:25832";
//			zoneId = "id";
//						
//			shapeFileAgentFilter = "../shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v0/vulkaneifel.shp";
//			
//			shapeFileTripFilter = "../shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v0/vulkaneifel.shp";
//			shapeFileTripFilterCRS = "EPSG:25832";
//			bufferMAroundTripFilterShp = "2000";

			// Gladbeck

			runDirectory = "../runs-svn/avoev/snz-gladbeck/output-snzDrtO443l/";
			runId = "snzDrtO443l";

			runDirectoryToCompareWith = null;
			runIdToCompareWith = null;

			visualizationScriptInputDirectory = null;

			scenarioCRS = "EPSG:25832";

			shapeFileZones = "../shared-svn/projects/avoev/matsim-input-files/gladbeck_umland/v1/shp-files/hexagon-grid-1000.shp";
			shapFileZonesCRS = "EPSG:25832";
			zoneId = "id";

			shapeFileTripFilter = "../shared-svn/projects/avoev/matsim-input-files/gladbeck/v0/gladbeck.shp";

			shapeFileTripFilter = "../shared-svn/projects/avoev/matsim-input-files/gladbeck/v0/gladbeck.shp";
			shapeFileTripFilterCRS = "EPSG:25832";
			bufferMAroundTripFilterShp = "2000";

		}
		
		Scenario scenario1 = loadScenario(runDirectory, runId, scenarioCRS);
		Scenario scenario0 = loadScenario(runDirectoryToCompareWith, runIdToCompareWith, scenarioCRS);
		
		List<AgentFilter> agentFilters = new ArrayList<>();
		
		AgentAnalysisFilter filter1a = new AgentAnalysisFilter("");
		filter1a.preProcess(scenario1);
		agentFilters.add(filter1a);
		
		AgentAnalysisFilter filter1b = new AgentAnalysisFilter("residents-in-area");
		filter1b.setZoneFile(shapeFileAgentFilter);
		filter1b.setRelevantActivityType(homeActivityPrefix);
		filter1b.preProcess(scenario1);
		agentFilters.add(filter1b);
		
		List<TripFilter> tripFilters = new ArrayList<>();
		
		TripAnalysisFilter tripFilter1a = new TripAnalysisFilter("");
		tripFilter1a.preProcess(scenario1);
		tripFilters.add(tripFilter1a);
		
		TripAnalysisFilter tripFilter1b = new TripAnalysisFilter("origin-or-destination-in-area");
		tripFilter1b.setZoneInformation(shapeFileTripFilter, shapeFileTripFilterCRS);
		tripFilter1b.preProcess(scenario1);
		tripFilter1b.setBuffer(Double.valueOf(bufferMAroundTripFilterShp));
		tripFilter1b.setTripConsiderType(TripConsiderType.OriginOrDestination);
		tripFilters.add(tripFilter1b);
		
		TripAnalysisFilter tripFilter1c = new TripAnalysisFilter("origin-and-destination-in-area");
		tripFilter1c.setZoneInformation(shapeFileTripFilter, shapeFileTripFilterCRS);
		tripFilter1c.preProcess(scenario1);
		tripFilter1c.setBuffer(Double.valueOf(0));
		tripFilter1c.setTripConsiderType(TripConsiderType.OriginAndDestination);
		tripFilters.add(tripFilter1c);
		
		final List<VehicleFilter> vehicleFilters = new ArrayList<>();

		VehicleAnalysisFilter vehicleAnalysisFilter0 = null;
		vehicleFilters.add(vehicleAnalysisFilter0);

		VehicleAnalysisFilter vehicleAnalysisFilter1 = new VehicleAnalysisFilter("drt-vehicles", "drt", StringComparison.Contains);
		vehicleFilters.add(vehicleAnalysisFilter1);

		VehicleAnalysisFilter vehicleAnalysisFilter2 = new VehicleAnalysisFilter("pt-vehicles", "tr", StringComparison.Contains);
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
		analysis.setScalingFactor(scalingFactor);
		analysis.setModes(modes);
		analysis.setHelpLegModes(helpLegModes);
		analysis.setZoneInformation(shapeFileZones, shapFileZonesCRS, zoneId);
		analysis.setVisualizationScriptInputDirectory(visualizationScriptInputDirectory);

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
		
		config.controler().setRunId(runId);
		config.global().setCoordinateSystem(scenarioCRS);
		config.controler().setOutputDirectory(runDirectory);
		config.plans().setInputFile(populationFile);
		config.network().setInputFile(networkFile);
		config.facilities().setInputFile(facilitiesFile);
		
		return ScenarioUtils.loadScenario(config);
	}

}
		

