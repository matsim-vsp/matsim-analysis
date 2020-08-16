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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.analysis.actDurations.ActDurationHandler;
import org.matsim.analysis.detailedPersonTripAnalysis.BasicPersonTripAnalysisHandler;
import org.matsim.analysis.detailedPersonTripAnalysis.PersonTripAnalysis;
import org.matsim.analysis.dynamicLinkDemand.DynamicLinkDemandEventHandler;
import org.matsim.analysis.gisAnalysis.GISAnalyzer;
import org.matsim.analysis.gisAnalysis.MoneyExtCostHandler;
import org.matsim.analysis.linkDemand.LinkDemandEventHandler;
import org.matsim.analysis.modalSplitUserType.ModeAnalysis;
import org.matsim.analysis.modeSwitchAnalysis.PersonTripScenarioComparison;
import org.matsim.analysis.od.ODAnalysis;
import org.matsim.analysis.od.ODEventAnalysisHandler;
import org.matsim.analysis.pngSequence2Video.MATSimVideoUtils;
import org.matsim.analysis.shapes.Network2Shape;
import org.matsim.analysis.visualizationScripts.VisualizationScriptAdjustment;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.decongestion.handler.DelayAnalysis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * 
 * Provides several analysis:
 * 
 * - some aggregated analysis 
 * - person-specific information
 * - trip-specific information
 * - time-specific trip travel times, distances, money payments
 * - delay information
 * - spatial information:
 * 		- daily traffic volume per link
 * 		- hourly traffic volume per link
 * 		- number of activities per zone, average toll payment, user benefit etc. per zone
 * - mode switch analysis
 * - modal split for different population filters
 * - writes out the network as a shapefile.
 *  
 */
public class MatsimAnalysis {
	private static final Logger log = Logger.getLogger(MatsimAnalysis.class);

	private String scenarioCRS;	
	private String shapeFileZones;
	private String zonesCRS;
	private String zoneId;
	private String homeActivityPrefix = "home";
	private int scalingFactor;
	private AnalysisMainModeIdentifier mainModeIdentifier = new DefaultAnalysisMainModeIdentifier();
	private String[] helpLegModes = {TransportMode.transit_walk, TransportMode.non_network_walk, TransportMode.walk}; // for backward compatibility 
	private List<String> modes;	
	private String visualizationScriptInputDirectory = null;
	private String analysisOutputDirectory;
	private boolean printODSHPfiles = true;
	private boolean printTripSHPfiles = false;
	
	// policy case
	private Scenario scenario1;
	private List<AgentFilter> agentFilters;
	private List<TripFilter> tripFilters;
	private List<VehicleFilter> vehicleFilters;

	// base case (optional)
	private Scenario scenario0;
	
	private final String outputDirectoryName = "analysis-v3.0";
	private final String stageActivitySubString = "interaction";

	public void run() {
		
		String outputDirectoryName = this.outputDirectoryName;
	
		final String runId = scenario1.getConfig().controler().getRunId();
		String runDirectory = scenario1.getConfig().controler().getOutputDirectory();
		if (!runDirectory.endsWith("/")) runDirectory = runDirectory + "/";
		
		String runIdToCompareWith = null;
		String runDirectoryToCompareWith = null;
		if (scenario0 != null) {
			runIdToCompareWith = scenario0.getConfig().controler().getRunId();
			runDirectoryToCompareWith = scenario0.getConfig().controler().getOutputDirectory();
			if (!runDirectoryToCompareWith.endsWith("/")) runDirectoryToCompareWith = runDirectoryToCompareWith + "/";
			outputDirectoryName = this.outputDirectoryName + "-comparison";
		}
		
		String outputDirectoryForAnalysisFiles = null;
		if (this.analysisOutputDirectory == null) {
			outputDirectoryForAnalysisFiles = runDirectory + outputDirectoryName + "/";
		} else {
			outputDirectoryForAnalysisFiles = this.analysisOutputDirectory + outputDirectoryName + "/";
		}
		File folder = new File(outputDirectoryForAnalysisFiles);			
		folder.mkdirs();
		
		OutputDirectoryLogging.catchLogEntries();
		try {
			OutputDirectoryLogging.initLoggingWithOutputDirectory(outputDirectoryForAnalysisFiles);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		log.info("Starting analysis...");
		
		log.info("Run directory: " + runDirectory);
		log.info("Run ID: " + runId);
		
		log.info("Run directory to compare with: " + runDirectoryToCompareWith);
		log.info("Run ID to compare with: " + runIdToCompareWith);
	

		if (this.vehicleFilters == null) {
			this.vehicleFilters = new ArrayList<>();
			VehicleFilter vehicleFilter = null;
			this.vehicleFilters.add(vehicleFilter);
		}

		// #####################################
		// Create and add the event handlers
		// #####################################

		EventsManager events1 = null;
		
		BasicPersonTripAnalysisHandler basicHandler1 = null;
		DelayAnalysis delayAnalysis1 = null;

		List<LinkDemandEventHandler> trafficVolumeAnalysis1 = new ArrayList<>();
		List<DynamicLinkDemandEventHandler> dynamicTrafficVolumeAnalysis1 = new ArrayList<>();
		MoneyExtCostHandler personMoneyHandler1 = null;
		ActDurationHandler actHandler1 = null;
		ODEventAnalysisHandler odHandler1 = null;

		if (scenario1 != null) {
			basicHandler1 = new BasicPersonTripAnalysisHandler(this.helpLegModes, this.stageActivitySubString);
			basicHandler1.setScenario(scenario1);

			delayAnalysis1 = new DelayAnalysis();
			delayAnalysis1.setScenario(scenario1);
			
			for (VehicleFilter vehicleFilter : this.vehicleFilters) {
				LinkDemandEventHandler trafficVolumeAnalysis = new LinkDemandEventHandler(vehicleFilter);
				trafficVolumeAnalysis1.add(trafficVolumeAnalysis);

				DynamicLinkDemandEventHandler dynamicTrafficVolumeAnalysis = new DynamicLinkDemandEventHandler(vehicleFilter);
				dynamicTrafficVolumeAnalysis1.add(dynamicTrafficVolumeAnalysis);
			}
			
			personMoneyHandler1 = new MoneyExtCostHandler();
			
			actHandler1 = new ActDurationHandler(this.stageActivitySubString);
						
			odHandler1 = new ODEventAnalysisHandler(helpLegModes, stageActivitySubString);
			
			events1 = EventsUtils.createEventsManager();
			events1.addHandler(basicHandler1);
			events1.addHandler(delayAnalysis1);
			for (LinkDemandEventHandler trafficVolumeAnalysis : trafficVolumeAnalysis1) {
				events1.addHandler(trafficVolumeAnalysis);
			}
			for (DynamicLinkDemandEventHandler dynamicTrafficVolumeAnalysis : dynamicTrafficVolumeAnalysis1) {
				events1.addHandler(dynamicTrafficVolumeAnalysis);
			}
			events1.addHandler(personMoneyHandler1);
			events1.addHandler(actHandler1);
			events1.addHandler(odHandler1);
		}
		
		EventsManager events0 = null;
		
		BasicPersonTripAnalysisHandler basicHandler0 = null;
		DelayAnalysis delayAnalysis0 = null;
		List<LinkDemandEventHandler> trafficVolumeAnalysis0 = new ArrayList<>();
		List<DynamicLinkDemandEventHandler> dynamicTrafficVolumeAnalysis0 = new ArrayList<>();
		MoneyExtCostHandler personMoneyHandler0 = null;
		ActDurationHandler actHandler0 = null;
		ODEventAnalysisHandler odHandler0 = null;
		
		if (scenario0 != null) {
			basicHandler0 = new BasicPersonTripAnalysisHandler(this.helpLegModes, this.stageActivitySubString);
			basicHandler0.setScenario(scenario0);

			delayAnalysis0 = new DelayAnalysis();
			delayAnalysis0.setScenario(scenario0);

			for (VehicleFilter vehicleFilter : this.vehicleFilters) {
				LinkDemandEventHandler trafficVolumeAnalysis = new LinkDemandEventHandler(vehicleFilter);
				trafficVolumeAnalysis0.add(trafficVolumeAnalysis);

				DynamicLinkDemandEventHandler dynamicTrafficVolumeAnalysis = new DynamicLinkDemandEventHandler(vehicleFilter);
				dynamicTrafficVolumeAnalysis0.add(dynamicTrafficVolumeAnalysis);
			}
			
			personMoneyHandler0 = new MoneyExtCostHandler();
			
			actHandler0 = new ActDurationHandler(this.stageActivitySubString);
						
			odHandler0 = new ODEventAnalysisHandler(helpLegModes, stageActivitySubString);

			events0 = EventsUtils.createEventsManager();
			events0.addHandler(basicHandler0);
			events0.addHandler(delayAnalysis0);
			for (LinkDemandEventHandler trafficVolumeAnalysis : trafficVolumeAnalysis0) {
				events0.addHandler(trafficVolumeAnalysis);
			}
			for (DynamicLinkDemandEventHandler dynamicTrafficVolumeAnalysis : dynamicTrafficVolumeAnalysis0) {
				events0.addHandler(dynamicTrafficVolumeAnalysis);
			}
			events0.addHandler(personMoneyHandler0);
			events0.addHandler(actHandler0);
			events0.addHandler(odHandler0);
		}

		// #####################################
		// Read the events file
		// #####################################
		
		if (scenario1 != null) readEventsFile(runDirectory, runId, events1);
		if (scenario0 != null) readEventsFile(runDirectoryToCompareWith, runIdToCompareWith, events0);
				
		// #####################################
		// Post process and read plans file
		// #####################################
		
		Map<Id<Person>, Double> personId2userBenefit1 = null;
		Map<Id<Person>, Double> personId2userBenefit0 = null;
		
		List<ModeAnalysis> modeAnalysisList1 = new ArrayList<>();
		List<ModeAnalysis> modeAnalysisList0 = new ArrayList<>();
								
		if (scenario1 != null) {
			
			personId2userBenefit1 = getPersonId2UserBenefit(scenario1.getPopulation());
			
			if (agentFilters != null) {
				for (AgentFilter agentFilter : agentFilters) {
					ModeAnalysis modeAnalysis1 = new ModeAnalysis(scenario1, agentFilter, null, mainModeIdentifier);
					modeAnalysis1.run();
					modeAnalysisList1.add(modeAnalysis1);
				}
			}

			if (tripFilters != null) {
				for (TripFilter tripFilter : tripFilters) {
					ModeAnalysis modeAnalysis1 = new ModeAnalysis(scenario1, null, tripFilter, mainModeIdentifier);
					modeAnalysis1.run();
					modeAnalysisList1.add(modeAnalysis1);
				}
			}
			
		}	
		
		if (scenario0 != null) {
			
			personId2userBenefit0 = getPersonId2UserBenefit(scenario0.getPopulation());
			
			if (agentFilters != null) {
				for (AgentFilter agentFilter : agentFilters) {
					ModeAnalysis modeAnalysis0 = new ModeAnalysis(scenario0, agentFilter, null, mainModeIdentifier);
					modeAnalysis0.run();
					modeAnalysisList0.add(modeAnalysis0);
				}
			}

			if (tripFilters != null) {
				for (TripFilter tripFilter : tripFilters) {
					ModeAnalysis modeAnalysis0 = new ModeAnalysis(scenario0, null, tripFilter, mainModeIdentifier);
					modeAnalysis0.run();
					modeAnalysisList0.add(modeAnalysis0);
				}
			}
			
		}	
		
		// #####################################
		// Print the results
		// #####################################

		log.info("Printing results...");
		if (scenario1 != null) printResults(
				scenario1,
				outputDirectoryForAnalysisFiles,
				personId2userBenefit1, basicHandler1,
				delayAnalysis1,
				trafficVolumeAnalysis1,
				dynamicTrafficVolumeAnalysis1,
				personMoneyHandler1,
				actHandler1,
				modeAnalysisList1,
				modes,
				odHandler1,
				tripFilters);
		
		log.info("Printing results...");
		if (scenario0 != null) printResults(scenario0,
				outputDirectoryForAnalysisFiles,
				personId2userBenefit0,
				basicHandler0,
				delayAnalysis0,
				trafficVolumeAnalysis0,
				dynamicTrafficVolumeAnalysis0,
				personMoneyHandler0,
				actHandler0,
				modeAnalysisList0,
				modes,
				odHandler0,
				tripFilters);

		// #####################################
		// Scenario comparison
		// #####################################
		
		String personTripScenarioComparisonOutputDirectory = null;
		
		if (scenario1 != null & scenario0 != null) {
			
			personTripScenarioComparisonOutputDirectory = outputDirectoryForAnalysisFiles + "scenario-comparison_" + runId + "-vs-" + runIdToCompareWith + "/";
			createDirectory(personTripScenarioComparisonOutputDirectory);

			for (AgentFilter agentFilter : this.agentFilters) {
				
				log.info("Person trip scenario comparison: " + agentFilter.toFileName());
				
				PersonTripScenarioComparison scenarioComparisonFiltered = new PersonTripScenarioComparison(this.homeActivityPrefix, personTripScenarioComparisonOutputDirectory, scenario1, basicHandler1, scenario0, basicHandler0, modes, agentFilter);
				
				try {
					// do not apply trip filter in addition to person filter
					TripAnalysisFilter tripFilter1 = new TripAnalysisFilter("");
					tripFilter1.preProcess(this.scenario1);
					scenarioComparisonFiltered.analyzeByMode(tripFilter1);

					scenarioComparisonFiltered.analyzeByScore(0.0);
					scenarioComparisonFiltered.analyzeByScore(1.0);
					scenarioComparisonFiltered.analyzeByScore(10.0);
					scenarioComparisonFiltered.analyzeByScore(100.0);			
				
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			for (TripFilter tripFilter : this.tripFilters) {

				// do not apply person filter in addition to trip filter
				AgentAnalysisFilter filter1 = new AgentAnalysisFilter("");
				filter1.preProcess(scenario1);
				agentFilters.add(filter1);

				PersonTripScenarioComparison scenarioComparisonFiltered = new PersonTripScenarioComparison(this.homeActivityPrefix, personTripScenarioComparisonOutputDirectory, scenario1, basicHandler1, scenario0, basicHandler0, modes, filter1);

				try {
					scenarioComparisonFiltered.analyzeByMode(tripFilter);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}

		// #####################################
		// Write the visualization scripts
		// #####################################
		if (visualizationScriptInputDirectory != null) {
			// traffic volumes
			if (scenario1 != null & scenario0 != null) {
				String visScriptTemplateFile = visualizationScriptInputDirectory + "traffic-volume_absolute-difference_noCRS.qgs";
				String visScriptOutputFile = outputDirectoryForAnalysisFiles + "link-volume-analysis/" + "traffic-volume_absolute-difference_" + runId + "-vs-" + runIdToCompareWith + ".qgs";
				
				VisualizationScriptAdjustment script = new VisualizationScriptAdjustment(visScriptTemplateFile, visScriptOutputFile);
				script.setRunId(runId);
				script.setRunIdToCompareWith(runIdToCompareWith);
				script.setScalingFactor(String.valueOf(this.scalingFactor));
				script.setCRS(this.scenarioCRS);
				script.write();
			}
			
			// absolute traffic volumes policy case
			if (scenario1 != null) {
				String visScriptTemplateFile = visualizationScriptInputDirectory + "traffic-volume_absolute_noCRS.qgs";
				String visScriptOutputFile = outputDirectoryForAnalysisFiles + "link-volume-analysis/" + runId + ".traffic-volume_absolute.qgs";
				
				VisualizationScriptAdjustment script = new VisualizationScriptAdjustment(visScriptTemplateFile, visScriptOutputFile);
				script.setRunId(runId);
				script.setScalingFactor(String.valueOf(this.scalingFactor));
				script.setCRS(this.scenarioCRS);
				script.write();
			}
			
			// spatial zone-based analysis
			if (scenario1 != null & scenario0 != null) {
				String visScriptTemplateFile = visualizationScriptInputDirectory + "zone-based-analysis_welfare_modes.qgs";
				String visScriptOutputFile = outputDirectoryForAnalysisFiles + "zone-based-analysis_welfare_modes/" + "zone-based-analysis_welfare_modes_" + runId + "-vs-" + runIdToCompareWith + ".qgs";
				
				VisualizationScriptAdjustment script = new VisualizationScriptAdjustment(visScriptTemplateFile, visScriptOutputFile);
				script.setRunId(runId);
				script.setRunIdToCompareWith(runIdToCompareWith);
				script.setScalingFactor(String.valueOf(this.scalingFactor));
				script.setCRS(this.zonesCRS);
				script.write();
			}
			
			// scenario comparison: person-specific mode-shift effects
			if (scenario1 != null & scenario0 != null) {
				String visScriptTemplateFile = visualizationScriptInputDirectory + "scenario-comparison_person-specific-mode-switch-effects.qgs";
				String visScriptOutputFile = personTripScenarioComparisonOutputDirectory + "scenario-comparison_person-specific-mode-switch-effects_" + runId + "-vs-" + runIdToCompareWith + ".qgs";
				
				VisualizationScriptAdjustment script = new VisualizationScriptAdjustment(visScriptTemplateFile, visScriptOutputFile);
				script.setRunId(runId);
				script.setRunIdToCompareWith(runIdToCompareWith);
				script.setScalingFactor(String.valueOf(this.scalingFactor));
				script.setCRS(this.scenarioCRS);
				script.write();
			}
			
			// scenario comparison: person-specific winner-loser analysis
			if (scenario1 != null & scenario0 != null) {
				String visScriptTemplateFile = visualizationScriptInputDirectory + "scenario-comparison_person-specific-winner-loser.qgs";
				String visScriptOutputFile = personTripScenarioComparisonOutputDirectory + "scenario-comparison_person-specific-winner-loser_" + runId + "-vs-" + runIdToCompareWith + ".qgs";
				
				VisualizationScriptAdjustment script = new VisualizationScriptAdjustment(visScriptTemplateFile, visScriptOutputFile);
				script.setRunId(runId);
				script.setRunIdToCompareWith(runIdToCompareWith);
				script.setScalingFactor(String.valueOf(this.scalingFactor));
				script.setCRS(this.scenarioCRS);
				script.write();
			}
		
			// externality-specific toll payments
			{
				String visScriptTemplateFile = visualizationScriptInputDirectory + "extCostPerTimeOfDay-cne_percentages.R";
				String visScriptOutputFile = outputDirectoryForAnalysisFiles + "person-trip-data/" + "extCostPerTimeOfDay-cne_percentages_" + runId + ".R";
						
				VisualizationScriptAdjustment script = new VisualizationScriptAdjustment(visScriptTemplateFile, visScriptOutputFile);
				script.setRunId(runId);
				script.setRunIdToCompareWith(runIdToCompareWith);
				script.setScalingFactor(String.valueOf(this.scalingFactor));
				script.setCRS(this.scenarioCRS);
				script.write();
			} 
		}
		
		log.info("Analysis completed.");
	}

	private void printResults(Scenario scenario,
			String analysisOutputDirectory,
			Map<Id<Person>, Double> personId2userBenefit,
			BasicPersonTripAnalysisHandler basicHandler,
			DelayAnalysis delayAnalysis,
			List<LinkDemandEventHandler> trafficVolumeAnalysis,
			List<DynamicLinkDemandEventHandler> dynamicTrafficVolumeAnalysis,
			MoneyExtCostHandler personMoneyHandler,
			ActDurationHandler actHandler,
			List<ModeAnalysis> modeAnalysisList,
			List<String> modes,
			ODEventAnalysisHandler odHandler,
			List<TripFilter> tripFilters
			) {
		
		// #####################################
		// Print results: person / trip analysis
		// #####################################
		
		String personTripAnalysOutputDirectory = analysisOutputDirectory + "person-trip-data/";
		createDirectory(personTripAnalysOutputDirectory);
		String personTripAnalysisOutputDirectoryWithPrefix = personTripAnalysOutputDirectory + scenario.getConfig().controler().getRunId() + ".";
		
		PersonTripAnalysis analysis = new PersonTripAnalysis();

		// trip-based analysis
		for (TripFilter tripFilter : tripFilters) {
			analysis.printTripInformation(personTripAnalysisOutputDirectoryWithPrefix, null, basicHandler, tripFilter);
			for (String mode : modes) {
				analysis.printTripInformation(personTripAnalysisOutputDirectoryWithPrefix, mode, basicHandler, tripFilter);
			}
		}

		// person-based analysis
		for (AgentFilter agentFilter : agentFilters) {
			analysis.printPersonInformation(personTripAnalysisOutputDirectoryWithPrefix, null, personId2userBenefit, basicHandler, agentFilter);
			for (String mode : modes) {
				analysis.printPersonInformation(personTripAnalysisOutputDirectoryWithPrefix, mode, personId2userBenefit, basicHandler, agentFilter);	
			}
		}
		
		// TODO: Add combined agent and trip filters...

		// aggregated analysis
		analysis.printAggregatedResults(personTripAnalysisOutputDirectoryWithPrefix, null, personId2userBenefit, basicHandler);
		for (String mode : modes) {
			analysis.printAggregatedResults(personTripAnalysisOutputDirectoryWithPrefix, mode, personId2userBenefit, basicHandler);
		}
		analysis.printAggregatedResults(personTripAnalysisOutputDirectoryWithPrefix, personId2userBenefit, basicHandler, delayAnalysis);
		
		// time-specific trip distance analysis
		for (String mode : modes) {
			SortedMap<Double, List<Double>> departureTime2traveldistance = analysis.getParameter2Values(mode, basicHandler, basicHandler.getPersonId2tripNumber2departureTime(), basicHandler.getPersonId2tripNumber2tripDistance(), 3600., 30 * 3600.);
			analysis.printAvgValuePerParameter(personTripAnalysisOutputDirectoryWithPrefix + "distancePerDepartureTime_" + mode + "_3600.csv", departureTime2traveldistance);
			
			// time-specific trip travel time analysis
			SortedMap<Double, List<Double>> departureTime2travelTime = analysis.getParameter2Values(mode, basicHandler, basicHandler.getPersonId2tripNumber2departureTime(), basicHandler.getPersonId2tripNumber2travelTime(), 3600., 30 * 3600.);
			analysis.printAvgValuePerParameter(personTripAnalysisOutputDirectoryWithPrefix + "travelTimePerDepartureTime_" + mode + "_3600.csv", departureTime2travelTime);
		
			// time-specific toll payments analysis
			SortedMap<Double, List<Double>> departureTime2tolls = analysis.getParameter2Values(mode, basicHandler, basicHandler.getPersonId2tripNumber2departureTime(), basicHandler.getPersonId2tripNumber2payment(), 3600., 30 * 3600.);
			analysis.printAvgValuePerParameter(personTripAnalysisOutputDirectoryWithPrefix + "tollsPerDepartureTime_"+ mode +"_3600.csv", departureTime2tolls);
				
		}

		// #####################################
		// Print results: link traffic volumes
		// #####################################
		
		String trafficVolumeAnalysisOutputDirectory = analysisOutputDirectory + "link-volume-analysis/";
		createDirectory(trafficVolumeAnalysisOutputDirectory);
		String trafficVolumeAnalysisOutputDirectoryWithPrefix = trafficVolumeAnalysisOutputDirectory + scenario.getConfig().controler().getRunId() + ".";
		
		// daily traffic volumes
		for (LinkDemandEventHandler linkDemandEventHandler : trafficVolumeAnalysis) {
			linkDemandEventHandler.printResults(trafficVolumeAnalysisOutputDirectoryWithPrefix);
		}
		
		// hourly traffic volumes
		for (DynamicLinkDemandEventHandler dynamicLinkDemandEventHandler : dynamicTrafficVolumeAnalysis) {
			dynamicLinkDemandEventHandler.printResults(trafficVolumeAnalysisOutputDirectoryWithPrefix);
		}

		// #####################################
		// Print results: spatial analysis
		// #####################################
		
		if (shapeFileZones != null && zonesCRS != null && scenarioCRS != null && scalingFactor != 0) {		
			
			String spatialAnalysisOutputDirectory = analysisOutputDirectory + "zone-based-analysis_welfare_modes/";
			createDirectory(spatialAnalysisOutputDirectory);
			String spatialAnalysisOutputDirectoryWithPrefix = spatialAnalysisOutputDirectory + scenario.getConfig().controler().getRunId() + ".";
			
			GISAnalyzer gisAnalysis = new GISAnalyzer(scenario, shapeFileZones, scalingFactor, homeActivityPrefix, zonesCRS, scenarioCRS, modes);
			try {
				gisAnalysis.analyzeZoneTollsUserBenefits(spatialAnalysisOutputDirectoryWithPrefix, "tolls_userBenefits_travelTime_modes_zones.shp", personId2userBenefit, personMoneyHandler.getPersonId2toll(), basicHandler);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// #####################################
		// Print results: network shape file
		// #####################################

		if (scenarioCRS != null) {
			String networkOutputDirectory = analysisOutputDirectory + "network-shp/";
			String outputDirectoryWithPrefix = networkOutputDirectory + scenario.getConfig().controler().getRunId() + ".";
			try {
				Network2Shape.exportNetwork2Shp(scenario, outputDirectoryWithPrefix, scenarioCRS, TransformationFactory.getCoordinateTransformation(scenarioCRS, scenarioCRS));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// #####################################
		// Print results: activity durations
		// #####################################
		
		String actDurationsOutputDirectory = analysisOutputDirectory + "activity-durations/";
		createDirectory(actDurationsOutputDirectory);
		
		List<String> skippedPersonIdStrings = new ArrayList<>();
		skippedPersonIdStrings.add("freight");
		actHandler.process(scenario.getPopulation(), skippedPersonIdStrings);
		
		actHandler.writeOutput(scenario.getPopulation(), actDurationsOutputDirectory + scenario.getConfig().controler().getRunId() + "." + "activity-durations.csv", Double.POSITIVE_INFINITY);
		actHandler.writeOutput(scenario.getPopulation(), actDurationsOutputDirectory + scenario.getConfig().controler().getRunId() + "." + "activity-durations_below-900-sec.csv", 900.);
		actHandler.writeSummary(scenario.getPopulation(), actDurationsOutputDirectory + scenario.getConfig().controler().getRunId() + "." + "activity-durations_summary.csv");

		// #####################################
		// Print results: mode statistics
		// #####################################
		
		String modeAnalysisOutputDirectory = analysisOutputDirectory + "mode-statistics/";
		createDirectory(modeAnalysisOutputDirectory);
		
		for (ModeAnalysis modeAnalysis : modeAnalysisList) {		
			modeAnalysis.writeModeShares(modeAnalysisOutputDirectory + scenario.getConfig().controler().getRunId() + ".");
			modeAnalysis.writeTripRouteDistances(modeAnalysisOutputDirectory + scenario.getConfig().controler().getRunId() + ".");
			modeAnalysis.writeTripEuclideanDistances(modeAnalysisOutputDirectory + scenario.getConfig().controler().getRunId() + ".");
			
			final List<Tuple<Double, Double>> distanceGroups1 = new ArrayList<>();
			distanceGroups1.add(new Tuple<>(0., 1000.));
			distanceGroups1.add(new Tuple<>(1000., 3000.));
			distanceGroups1.add(new Tuple<>(3000., 5000.));
			distanceGroups1.add(new Tuple<>(5000., 10000.));
			distanceGroups1.add(new Tuple<>(10000., 20000.));
			distanceGroups1.add(new Tuple<>(20000., 100000.));
			distanceGroups1.add(new Tuple<>(100000., 999999999999.));
			modeAnalysis.writeTripRouteDistances(modeAnalysisOutputDirectory + scenario.getConfig().controler().getRunId() + ".1-3-5-10-xxx.", distanceGroups1);
			modeAnalysis.writeTripEuclideanDistances(modeAnalysisOutputDirectory + scenario.getConfig().controler().getRunId() + ".1-3-5-10-xxx.", distanceGroups1);
			
			final List<Tuple<Double, Double>> distanceGroups2 = new ArrayList<>();
			distanceGroups2.add(new Tuple<>(0., 1000.));
			distanceGroups2.add(new Tuple<>(1000., 2000.));
			distanceGroups2.add(new Tuple<>(2000., 3000.));
			distanceGroups2.add(new Tuple<>(3000., 4000.));
			distanceGroups2.add(new Tuple<>(4000., 5000.));
			distanceGroups2.add(new Tuple<>(5000., 6000.));
			distanceGroups2.add(new Tuple<>(6000., 7000.));
			distanceGroups2.add(new Tuple<>(7000., 8000.));
			distanceGroups2.add(new Tuple<>(8000., 9000.));
			distanceGroups2.add(new Tuple<>(9000., 999999999999.));
			modeAnalysis.writeTripRouteDistances(modeAnalysisOutputDirectory + scenario.getConfig().controler().getRunId() + ".1-2-3-4-xxx.", distanceGroups2);
			modeAnalysis.writeTripEuclideanDistances(modeAnalysisOutputDirectory + scenario.getConfig().controler().getRunId() + ".1-2-3-4-xxx.", distanceGroups2);
		}
		
		// #####################################
		// Print leg histogram videos
		// #####################################
		
		String legHistogramOutputDirectory = analysisOutputDirectory + "legHistograms/";
		createDirectory(legHistogramOutputDirectory);
		
		log.info("Creating leg histogram video for all modes...");
		try {
			MATSimVideoUtils.createVideo(scenario.getConfig().controler().getOutputDirectory(), scenario.getConfig().controler().getRunId(), legHistogramOutputDirectory, 1, "legHistogram_all");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		log.info("Creating leg histogram video for all modes... Done.");

		if (scenario.getConfig().controler().getLastIteration() - scenario.getConfig().controler().getFirstIteration() > 0) {
			for (String mode : scenario.getConfig().plansCalcRoute().getNetworkModes()) {
				try {
					log.info("Creating leg histogram video for mode " + mode);
					MATSimVideoUtils.createVideo(scenario.getConfig().controler().getOutputDirectory(), scenario.getConfig().controler().getRunId(), legHistogramOutputDirectory, 1, "legHistogram_" + mode);
					log.info("Creating leg histogram video for mode " + mode + " Done.");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		// #####################################
		// Print OD analysis
		// #####################################
		ODAnalysis odAnalysis = new ODAnalysis(analysisOutputDirectory, scenario.getNetwork(), scenario.getConfig().controler().getRunId(), this.shapeFileZones, this.zonesCRS, this.zoneId, this.modes, this.scalingFactor);
		odAnalysis.setPrintODSHPfiles(printODSHPfiles);
		odAnalysis.setPrintTripSHPfiles(printTripSHPfiles);
		odAnalysis.process(odHandler);
	}
	
	private void createDirectory(String directory) {
		File file = new File(directory);
		file.mkdirs();
	}
	
	private Map<Id<Person>, Double> getPersonId2UserBenefit(Population population) {
		if (population != null) {
			int countWrn = 0;
			Map<Id<Person>, Double> personId2userBenefit = new HashMap<>();
			for (Person person : population.getPersons().values()) {
				
				if (countWrn <= 5) {
					if (person.getSelectedPlan().getScore() == null || person.getSelectedPlan().getScore() < 0.) {
						log.warn("The score of person " + person.getId() + " is null or negative: " + person.getSelectedPlan().toString());
						if (countWrn == 5) {
							log.warn("Further warnings of this type are not printed out.");
						}
						countWrn++;
					}						
				}
				
				personId2userBenefit.put(person.getId(), person.getSelectedPlan().getScore());
			}
			return personId2userBenefit;
		} else {
			return null;
		}
	}

	private void readEventsFile(String runDirectory, String runId, EventsManager events) {
		if (runDirectory.startsWith("http")) {
			String eventsFile = runDirectory + runId + ".output_events.xml.gz";
			log.info("Trying to read " + eventsFile + " as URL...");
			URL eventsFileURL = null;
			try {
				eventsFileURL = new URL(eventsFile);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			new MatsimEventsReader(events).readURL(eventsFileURL);
		} else {
			if (new File(runDirectory + runId + ".output_events.xml.gz").exists()) {
				String eventsFile = runDirectory + runId + ".output_events.xml.gz";
				log.info("Trying to read " + eventsFile + "...");
				new MatsimEventsReader(events).readFile(eventsFile);

			} else if (new File(runDirectory + "output_events.xml.gz").exists()){
				log.info(runDirectory + runId + ".output_events.xml.gz not found. Trying to read file without runId prefix...");
				String eventsFile = runDirectory + "output_events.xml.gz";
				log.info("Trying to read " + eventsFile + "...");
				new MatsimEventsReader(events).readFile(eventsFile);
				
			} else {
				throw new RuntimeException("Events file not found for runDirectory: " + runDirectory + " and runId: " + runId + ". Aborting...");
			}
		}
	}

	public void setScenarioCRS(String scenarioCRS) {
		this.scenarioCRS = scenarioCRS;
	}

	public void setHomeActivityPrefix(String homeActivityPrefix) {
		this.homeActivityPrefix = homeActivityPrefix;
	}

	public void setScalingFactor(int scalingFactor) {
		this.scalingFactor = scalingFactor;
	}

	public void setHelpLegModes(String[] helpLegModes) {
		this.helpLegModes = helpLegModes;
	}

	public void setMainModeIdentifier(AnalysisMainModeIdentifier mainModeIdentifier) {
		this.mainModeIdentifier = mainModeIdentifier;
	}

	public void setModes(List<String> modes) {
		this.modes = modes;
	}

	public void setVisualizationScriptInputDirectory(String visualizationScriptInputDirectory) {
		this.visualizationScriptInputDirectory = visualizationScriptInputDirectory;
	}

	public void setScenario1(Scenario scenario1) {
		this.scenario1 = scenario1;
	}

	public void setAgentFilters(List<AgentFilter> filters) {
		this.agentFilters = filters;
	}

	public void setTripFilters(List<TripFilter> tripFilters) {
		this.tripFilters = tripFilters;
	}

	public void setScenario0(Scenario scenario0) {
		this.scenario0 = scenario0;
	}

	public void setZoneInformation(String shapeFileZones, String zonesCRS, String zoneId) {
		
		if (shapeFileZones == null || zoneId == null
				|| shapeFileZones.endsWith("null") || zoneId.endsWith("null")
				|| shapeFileZones.equals("") || zoneId.equals("")) {
			this.shapeFileZones = null;
			this.zoneId = null;
		} else {
			this.shapeFileZones = shapeFileZones;
			this.zoneId = zoneId;
		}
		
		this.zonesCRS = zonesCRS;
	}

	public void setAnalysisOutputDirectory(String analysisOutputDirectory) {
		if (analysisOutputDirectory != null && !analysisOutputDirectory.endsWith("/")) analysisOutputDirectory = analysisOutputDirectory + "/";
		this.analysisOutputDirectory = analysisOutputDirectory;
	}

	public void setPrintODSHPfiles(boolean printODSHPfiles) {
		this.printODSHPfiles = printODSHPfiles;
	}

	public void setPrintTripSHPfiles(boolean printTripSHPfiles) {
		this.printTripSHPfiles = printTripSHPfiles;
	}

	public void setVehicleFilters(List<VehicleFilter> vehicleFilters) {
		this.vehicleFilters = vehicleFilters;
	}

}
		

