/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.analysis.modeSwitchAnalysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.analysis.AgentAnalysisFilter;
import org.matsim.analysis.TripAnalysisFilter;
import org.matsim.analysis.detailedPersonTripAnalysis.handler.BasicPersonTripAnalysisHandler;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;

/**
 * ikaddoura
 * 
 */
public class PersonTripScenarioComparison {
	private final static Logger log = Logger.getLogger(PersonTripScenarioComparison.class);
	
	private final String analysisOutputDirectory;
	private final Scenario scenario1;
	private final BasicPersonTripAnalysisHandler basicHandler1;
	private final Scenario scenarioToCompareWith;
	private final BasicPersonTripAnalysisHandler basicHandlerToCompareWith;
	
	private final Map<Id<Person>, Map<Integer, Coord>> personId2actNr2coord;
	private final Map<Id<Person>, Coord> personId2homeActCoord;
	private final List<String> modes;
	private final AgentAnalysisFilter agentFilter;
	
    public PersonTripScenarioComparison(
    		String homeActivity,
    		String analysisOutputDirectory,
    		Scenario scenario1,
    		BasicPersonTripAnalysisHandler basicHandler1,
    		Scenario scenarioToCompareWith,
    		BasicPersonTripAnalysisHandler basicHandlerToCompareWith,
    		List<String> modes,
    		AgentAnalysisFilter agentFilter) {
    	
		this.analysisOutputDirectory = analysisOutputDirectory;
		this.scenario1 = scenario1;
		this.basicHandler1 = basicHandler1;
		this.scenarioToCompareWith = scenarioToCompareWith;
		this.basicHandlerToCompareWith = basicHandlerToCompareWith;
		this.modes = modes;
		this.agentFilter = agentFilter;
		
		log.info("Getting activity coordinates from plans...");
		
		personId2actNr2coord = new HashMap<>();
    	personId2homeActCoord = new HashMap<>();
    	  		
    	for (Person person : scenarioToCompareWith.getPopulation().getPersons().values()) {
			int actCounter = 1;
			for (PlanElement pE : person.getSelectedPlan().getPlanElements()) {
				if (pE instanceof Activity) {
					Activity act = (Activity) pE;
					
					if (act.getType().startsWith(homeActivity)) {
						personId2homeActCoord.put(person.getId(), act.getCoord());
					}
					
					if (actCounter == 1) {
						Map<Integer, Coord> actNr2Coord = new HashMap<>();
						actNr2Coord.put(actCounter, act.getCoord());
						personId2actNr2coord.put(person.getId(), actNr2Coord);
					
					} else {
						personId2actNr2coord.get(person.getId()).put(actCounter, act.getCoord());
					}
					
					actCounter++;
				}
			}				
		}
    	
    	log.info("Getting activity coordinates from plans... Done.");
	}

	public void analyzeByMode(TripAnalysisFilter tripFilter) throws IOException {
    	
		Map<String, Map<String, Coord>> switchAndCoordType2Coord = new HashMap<>();
		Map<String, Map<Id<Person>, Integer>> switchType2agents = new HashMap<>();
		Map<String, List<Double>> switchType2tripTTwithStuckingAgents = new HashMap<>();
		Map<String, List<Double>> switchType2tripTTwithoutStuckingAgents = new HashMap<>();
		Map<String, List<Double>> switchType2tripBeelineSpeedWithoutStuckingAgents = new HashMap<>();

		Map<String, BufferedWriter> bufferedWriter = new HashMap<>();
		
		bufferedWriter.put("all", IOUtils.getBufferedWriter( analysisOutputDirectory + "modeSwitchAnalysis_all" + agentFilter.toFileName() + tripFilter.toFileName() + ".csv"));
		
		for (String mode : modes) {
			switchAndCoordType2Coord.put(mode+"2"+mode+"Origin", new HashMap<>());
			switchAndCoordType2Coord.put(mode+"2"+mode+"Destination", new HashMap<>());
			switchAndCoordType2Coord.put(mode+"2"+mode+"HomeCoord", new HashMap<>());

			switchAndCoordType2Coord.put(mode+"2x"+"Origin", new HashMap<>());
			switchAndCoordType2Coord.put(mode+"2x"+"Destination", new HashMap<>());
			switchAndCoordType2Coord.put(mode+"2x"+"HomeCoord", new HashMap<>());

			switchAndCoordType2Coord.put("x2"+mode+"Origin", new HashMap<>());
			switchAndCoordType2Coord.put("x2"+mode+"Destination", new HashMap<>());
			switchAndCoordType2Coord.put("x2"+mode+"HomeCoord", new HashMap<>());
			
			bufferedWriter.put(mode, IOUtils.getBufferedWriter( analysisOutputDirectory + "modeSwitchAnalysis_" + mode + agentFilter.toFileName() + tripFilter.toFileName() + ".csv"));
			bufferedWriter.put("x2" + mode, IOUtils.getBufferedWriter( analysisOutputDirectory + "modeSwitchAnalysis_x2" + mode + agentFilter.toFileName() + tripFilter.toFileName() + ".csv"));
			bufferedWriter.put(mode + "2x", IOUtils.getBufferedWriter( analysisOutputDirectory + "modeSwitchAnalysis_" + mode + "2x" + agentFilter.toFileName() + tripFilter.toFileName() + ".csv"));
			bufferedWriter.put(mode + "2" + mode, IOUtils.getBufferedWriter( analysisOutputDirectory + "modeSwitchAnalysis_" + mode + "2" + mode + agentFilter.toFileName() + tripFilter.toFileName() + ".csv"));

			for (String mode2 : modes) {

				switchType2tripTTwithStuckingAgents.put(mode2 + "2" + mode2, new ArrayList<>());
				switchType2tripTTwithoutStuckingAgents.put(mode2 + "2" + mode2, new ArrayList<>());
				switchType2tripBeelineSpeedWithoutStuckingAgents.put(mode2 + "2" + mode2, new ArrayList<>());


				if (!mode2.equals(mode)) {
					bufferedWriter.put(mode2 + "2" + mode, IOUtils.getBufferedWriter( analysisOutputDirectory + "modeSwitchAnalysis_" + mode2 + "2" + mode + agentFilter.toFileName() + tripFilter.toFileName() + ".csv"));
					bufferedWriter.put(mode + "2" + mode2, IOUtils.getBufferedWriter( analysisOutputDirectory + "modeSwitchAnalysis_" + mode + "2" + mode2 + agentFilter.toFileName() + tripFilter.toFileName() + ".csv"));
					
					switchType2tripTTwithStuckingAgents.put(mode2 + "2" + mode, new ArrayList<>());
					switchType2tripTTwithStuckingAgents.put(mode + "2" + mode2, new ArrayList<>());
					
					switchType2tripTTwithoutStuckingAgents.put(mode2 + "2" + mode, new ArrayList<>());
					switchType2tripTTwithoutStuckingAgents.put(mode + "2" + mode2, new ArrayList<>());
					
					switchType2tripBeelineSpeedWithoutStuckingAgents.put(mode2 + "2" + mode, new ArrayList<>());
					switchType2tripBeelineSpeedWithoutStuckingAgents.put(mode + "2" + mode2, new ArrayList<>());
				}
			}
			
			switchType2agents.put(mode+"2"+mode, new HashMap<>());
			switchType2agents.put("x2"+mode, new HashMap<>());
			switchType2agents.put(mode+"2x", new HashMap<>());
		}
	    
		for (BufferedWriter writer : bufferedWriter.values()) {
			writer.write("personId;subpopulation;tripNr;stuck0;stuck1;main-mode0;main-mode1;beeline-distance0;beeline-distance1;main-mode-distance0;main-mode-distance1;all-legs-travelTime0;all-legs-travelTime1;payments0;payments1");
			writer.newLine();
		}
	        
		// mode switch analysis
		log.info("Comparing the two scenarios for each trip and person... (total number of persons: " + basicHandler1.getPersonId2tripNumber2legMode().size() + ")");
	
		int personCounter = 0;
		for (Id<Person> personId : basicHandler1.getPersonId2tripNumber2legMode().keySet()) {
			
			String subpopulationOfPerson = (String) scenario1.getPopulation().getPersons().get(personId).getAttributes().getAttribute(scenario1.getConfig().plans().getSubpopulationAttributeName());
						
			if (agentFilter.considerAgent(scenario1.getPopulation().getPersons().get(personId))) {
				Map<Integer, String> tripNr2legMode = basicHandler1.getPersonId2tripNumber2legMode().get(personId);
				
				for (Integer tripNr : tripNr2legMode.keySet()) {
					
					Coord originCoord = basicHandler1.getPersonId2tripNumber2originCoord().get(personId).get(tripNr);
					Coord destinationCoord = basicHandler1.getPersonId2tripNumber2destinationCoord().get(personId).get(tripNr);

					if (tripFilter != null && !tripFilter.considerTrip(originCoord, destinationCoord)) {
						// skip trip
						
					} else {
						String mode1 = tripNr2legMode.get(tripNr);
						
						if (basicHandlerToCompareWith.getPersonId2tripNumber2legMode().get(personId) == null) {
							throw new RuntimeException("Person " + personId + " from run directory1 " + analysisOutputDirectory + "doesn't exist in run directory0 " + analysisOutputDirectory + ". Are you comparing the same scenario? Aborting...");
						}

						String mode0 = "unknown";
						if (basicHandlerToCompareWith.getPersonId2tripNumber2legMode().get(personId).get(tripNr) == null) {
							log.warn("Could not identify the trip mode of person " + personId + " and trip number " + tripNr + ". Setting mode to 'unknown'.");
						} else {
							mode0 = basicHandlerToCompareWith.getPersonId2tripNumber2legMode().get(personId).get(tripNr);
						}
						
						String stuck0 = "no";
						if (basicHandlerToCompareWith.getPersonId2tripNumber2stuckAbort().get(personId) != null && basicHandlerToCompareWith.getPersonId2tripNumber2stuckAbort().get(personId).get(tripNr) != null) {
							stuck0 = "yes";
						}
						
						String stuck1 = "no";
						if (basicHandler1.getPersonId2tripNumber2stuckAbort().get(personId) != null && basicHandler1.getPersonId2tripNumber2stuckAbort().get(personId).get(tripNr) != null) {
							stuck1 = "yes";
						}
						
						bufferedWriter.get("all").write(personId + ";" + subpopulationOfPerson + ";" + tripNr + ";"
								+ stuck0 + ";" + stuck1 + ";"
								+ mode0 + ";" + mode1 + ";" 
								+ basicHandlerToCompareWith.getPersonId2tripNumber2tripBeelineDistance().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2tripBeelineDistance().get(personId).get(tripNr) + ";"
								+ basicHandlerToCompareWith.getPersonId2tripNumber2tripDistance().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2tripDistance().get(personId).get(tripNr) + ";"
								+ basicHandlerToCompareWith.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) + ";"
								+ basicHandlerToCompareWith.getPersonId2tripNumber2payment().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2payment().get(personId).get(tripNr) + ";"
								);
						bufferedWriter.get("all").newLine();
						
						
						// mode-specific analysis
						
						for (String modeA : modes) {
												
							// x --> mode

							if (mode1.equals(modeA) && !mode0.equals(modeA)) {
								String modeSwitchType = "x2" + modeA;
								bufferedWriter.get(modeSwitchType).write(personId + ";" + subpopulationOfPerson + ";" + tripNr + ";"
										+ stuck0 + ";" + stuck1 + ";"
										+ mode0 + ";" + mode1 + ";"
										+ basicHandlerToCompareWith.getPersonId2tripNumber2tripBeelineDistance().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2tripBeelineDistance().get(personId).get(tripNr) + ";"
										+ basicHandlerToCompareWith.getPersonId2tripNumber2tripDistance().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2tripDistance().get(personId).get(tripNr) + ";"
										+ basicHandlerToCompareWith.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) + ";"
										+ basicHandlerToCompareWith.getPersonId2tripNumber2payment().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2payment().get(personId).get(tripNr) + ";"
										);
								bufferedWriter.get(modeSwitchType).newLine();
								
								Map<Id<Person>, Integer> x2modeAgents = switchType2agents.get(modeSwitchType);
								
								if (x2modeAgents.get(personId) == null) {
									x2modeAgents.put(personId, 1);
								} else {
									x2modeAgents.put(personId, x2modeAgents.get(personId) + 1);
								}
								
								switchAndCoordType2Coord.get("x2" + modeA + "Origin").put(personId + "Trip" + tripNr, personId2actNr2coord.get(personId).get(tripNr));
								switchAndCoordType2Coord.get("x2" + modeA + "Destination").put(personId + "Trip" + (tripNr), personId2actNr2coord.get(personId).get(tripNr + 1));
			                	
			                	if (personId2homeActCoord.get(personId) != null) {
			                		switchAndCoordType2Coord.get("x2" + modeA + "HomeCoord").put(personId.toString(), personId2homeActCoord.get(personId));
			                	} else {
									log.warn("No home activity coordinate for person " + personId);
								}
							}
		                	
		                	// mode --> x
		                	
		                	if (!mode1.equals(modeA) && mode0.equals(modeA)) {
								String modeSwitchType = modeA + "2x";
								bufferedWriter.get(modeSwitchType).write(personId + ";" + subpopulationOfPerson + ";" + tripNr + ";"
										+ stuck0 + ";" + stuck1 + ";"
										+ mode0 + ";" + mode1 + ";"
										+ basicHandlerToCompareWith.getPersonId2tripNumber2tripBeelineDistance().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2tripBeelineDistance().get(personId).get(tripNr) + ";"
										+ basicHandlerToCompareWith.getPersonId2tripNumber2tripDistance().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2tripDistance().get(personId).get(tripNr) + ";"
										+ basicHandlerToCompareWith.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) + ";"
										+ basicHandlerToCompareWith.getPersonId2tripNumber2payment().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2payment().get(personId).get(tripNr) + ";"
										);
								bufferedWriter.get(modeSwitchType).newLine();
								
								Map<Id<Person>, Integer> mode2xAgents = switchType2agents.get(modeSwitchType);
								
								if (mode2xAgents.get(personId) == null) {
									mode2xAgents.put(personId, 1);
								} else {
									mode2xAgents.put(personId, mode2xAgents.get(personId) + 1);
								}
								
								switchAndCoordType2Coord.get(modeA + "2xOrigin").put(personId + "Trip" + tripNr, personId2actNr2coord.get(personId).get(tripNr));
								switchAndCoordType2Coord.get(modeA + "2xDestination").put(personId + "Trip" + (tripNr), personId2actNr2coord.get(personId).get(tripNr + 1));
			                	
			                	if (personId2homeActCoord.get(personId) != null) {
			                		switchAndCoordType2Coord.get(modeA + "2xHomeCoord").put(personId.toString(), personId2homeActCoord.get(personId));
			                	} else {
									log.warn("No home activity coordinate for person " + personId);
								}
							}
		                	
		                	// mode --> mode
		                	                	
		                	if (mode1.equals(modeA) && mode0.equals(modeA)) {
								String modeSwitchType = modeA + "2" + modeA;
								bufferedWriter.get(modeSwitchType).write(personId + ";" + subpopulationOfPerson + ";" + tripNr + ";"
										+ stuck0 + ";" + stuck1 + ";" 
										+ mode0 + ";" + mode1 + ";"
										+ basicHandlerToCompareWith.getPersonId2tripNumber2tripBeelineDistance().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2tripBeelineDistance().get(personId).get(tripNr) + ";"
										+ basicHandlerToCompareWith.getPersonId2tripNumber2tripDistance().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2tripDistance().get(personId).get(tripNr) + ";"
										+ basicHandlerToCompareWith.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) + ";"
										+ basicHandlerToCompareWith.getPersonId2tripNumber2payment().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2payment().get(personId).get(tripNr) + ";"
										);
								bufferedWriter.get(modeSwitchType).newLine();
								
								double ttDiff = basicHandler1.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) - basicHandlerToCompareWith.getPersonId2tripNumber2travelTime().get(personId).get(tripNr);
								switchType2tripTTwithStuckingAgents.get(modeSwitchType).add(ttDiff);
								
								if (stuck0.equals("no") && stuck1.equals("no") && !mode0.equals("unknown") && !mode1.equals("unknown")) {
									double ttDiffnoStuck = basicHandler1.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) - basicHandlerToCompareWith.getPersonId2tripNumber2travelTime().get(personId).get(tripNr);
									switchType2tripTTwithoutStuckingAgents.get(modeSwitchType).add(ttDiffnoStuck);
									
									double beelineSpeedDiffnoStuck = (basicHandler1.getPersonId2tripNumber2tripBeelineDistance().get(personId).get(tripNr) / basicHandler1.getPersonId2tripNumber2travelTime().get(personId).get(tripNr)) - (basicHandlerToCompareWith.getPersonId2tripNumber2tripBeelineDistance().get(personId).get(tripNr) / basicHandlerToCompareWith.getPersonId2tripNumber2travelTime().get(personId).get(tripNr));
									switchType2tripBeelineSpeedWithoutStuckingAgents.get(modeSwitchType).add(beelineSpeedDiffnoStuck);
								}
							
								Map<Id<Person>, Integer> mode2modeAgents = switchType2agents.get(modeSwitchType);
								
								if (mode2modeAgents.get(personId) == null) {
									mode2modeAgents.put(personId, 1);
								} else {
									mode2modeAgents.put(personId, mode2modeAgents.get(personId) + 1);
								}
		                	
								switchAndCoordType2Coord.get(modeA + "2" + modeA + "Origin").put(personId + "Trip" + tripNr, personId2actNr2coord.get(personId).get(tripNr));
								switchAndCoordType2Coord.get(modeA + "2" + modeA + "Destination").put(personId + "Trip" + (tripNr), personId2actNr2coord.get(personId).get(tripNr + 1));
			                	
			                	if (personId2homeActCoord.get(personId) != null) {
			                		switchAndCoordType2Coord.get(modeA + "2" + modeA + "HomeCoord").put(personId.toString(), personId2homeActCoord.get(personId));
			                	} else {
									log.warn("No home activity coordinate for person " + personId);
								}
		                	}
		                	
		                	// mode

		    				if (mode1.equals(modeA) || mode0.equals(modeA)) {
		    					// at least one trip was a car trip
		    					bufferedWriter.get(modeA).write(personId + ";" + subpopulationOfPerson + ";" + tripNr + ";"
				    				+ stuck0 + ";" + stuck1 + ";" 
				    				+ mode0 + ";" + mode1 + ";"
				    				+ basicHandlerToCompareWith.getPersonId2tripNumber2tripBeelineDistance().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2tripBeelineDistance().get(personId).get(tripNr) + ";"
				    				+ basicHandlerToCompareWith.getPersonId2tripNumber2tripDistance().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2tripDistance().get(personId).get(tripNr) + ";"
				    				+ basicHandlerToCompareWith.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) + ";"
				    				+ basicHandlerToCompareWith.getPersonId2tripNumber2payment().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2payment().get(personId).get(tripNr) + ";"
				    				);
		    					bufferedWriter.get(modeA).newLine();
		    				}
							
							for (String modeB : modes) {
								if (!modeA.equals(modeB)) {
									
									if (modeA.equals(mode0) && modeB.equals(mode1)) {
										// A --> B
										String modeSwitchType = modeA + "2" + modeB;
										bufferedWriter.get(modeSwitchType).write(personId + ";" + subpopulationOfPerson + ";" + tripNr + ";"
												+ stuck0 + ";" + stuck1 + ";" 
												+ mode0 + ";" + mode1 + ";" 
												+ basicHandlerToCompareWith.getPersonId2tripNumber2tripBeelineDistance().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2tripBeelineDistance().get(personId).get(tripNr) + ";"
												+ basicHandlerToCompareWith.getPersonId2tripNumber2tripDistance().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2tripDistance().get(personId).get(tripNr) + ";"
												+ basicHandlerToCompareWith.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) + ";"
												+ basicHandlerToCompareWith.getPersonId2tripNumber2payment().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2payment().get(personId).get(tripNr) + ";"
												);
										bufferedWriter.get(modeSwitchType).newLine();
										
										double ttDiff = basicHandler1.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) - basicHandlerToCompareWith.getPersonId2tripNumber2travelTime().get(personId).get(tripNr);
										switchType2tripTTwithStuckingAgents.get(modeSwitchType).add(ttDiff);
										
										if (stuck0.equals("no") && stuck1.equals("no") && !mode0.equals("unknown") && !mode1.equals("unknown")) {
											double ttDiffnoStuck = basicHandler1.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) - basicHandlerToCompareWith.getPersonId2tripNumber2travelTime().get(personId).get(tripNr);
											switchType2tripTTwithoutStuckingAgents.get(modeSwitchType).add(ttDiffnoStuck);
											
											double beelineSpeedDiffnoStuck = (basicHandler1.getPersonId2tripNumber2tripBeelineDistance().get(personId).get(tripNr) / basicHandler1.getPersonId2tripNumber2travelTime().get(personId).get(tripNr)) - (basicHandlerToCompareWith.getPersonId2tripNumber2tripBeelineDistance().get(personId).get(tripNr) / basicHandlerToCompareWith.getPersonId2tripNumber2travelTime().get(personId).get(tripNr));
											switchType2tripBeelineSpeedWithoutStuckingAgents.get(modeSwitchType).add(beelineSpeedDiffnoStuck);
										}
									}										
								}
							}
						}
					}
					
					if (personCounter%100000 == 0) {
						log.info("person #" + personCounter);
					}
					
					personCounter++;
				}
			} else {
				log.info("skipping " + personId);
			}
		}
			
		for (BufferedWriter writer : bufferedWriter.values()) {
			writer.close();
		}
		
		log.info("Comparing the two scenarios for each trip and person... Done.");
		        	
		for (String modeSwitchType : switchAndCoordType2Coord.keySet()) {
			printCoordinates(switchAndCoordType2Coord.get(modeSwitchType), analysisOutputDirectory  + "/modeSwitchAnalysis_actCoord_" + modeSwitchType + agentFilter.toFileName() + tripFilter.toFileName() + ".csv");
		}
		
		{
			BufferedWriter aggregatedTripWriter = IOUtils.getBufferedWriter(analysisOutputDirectory  + "/modeSwitchAnalysis_aggregated" + agentFilter.toFileName() + tripFilter.toFileName() + ".csv");
			aggregatedTripWriter.write("from;to;number of trips (with stucking agents, sample size);number of trips (without stucking agents, sample size);average change in trip travel time (with stucking agents) [sec];average change in trip travel time (without stucking agents) [sec];average change in beeline speed (without stucking agents) [m/s]");
			aggregatedTripWriter.newLine();
			
			for (String switchType : switchType2tripTTwithStuckingAgents.keySet()) {
		       
				double tripsWithStuckingAgents = switchType2tripTTwithStuckingAgents.get(switchType).size();
				double tripsWithoutStuckingAgents = switchType2tripTTwithoutStuckingAgents.get(switchType).size();
				
				double diffTTSumWithStuckingAgents = 0.;
				for (Double ttDiff : switchType2tripTTwithStuckingAgents.get(switchType)) {
	        		diffTTSumWithStuckingAgents += ttDiff;
	        	}
				
				double diffTTSumWithoutStuckingAgents = 0.;
				for (Double ttDiff : switchType2tripTTwithoutStuckingAgents.get(switchType)) {
					diffTTSumWithoutStuckingAgents += ttDiff;
	        	}
				
				double diffBeelineSpeedSumWithoutStuckingAgents = 0.;
				for (Double beelineSpeedDiff : switchType2tripBeelineSpeedWithoutStuckingAgents.get(switchType)) {
					diffBeelineSpeedSumWithoutStuckingAgents += beelineSpeedDiff;
	        	}
				
				double avgDiffTTwithStuckingAgents = Double.NaN;
				double avgDiffTTwithoutStuckingAgents = Double.NaN;
				double avgDiffBeelineSpedWithoutStuckingAgents = Double.NaN;
				
				if (tripsWithStuckingAgents != 0.) avgDiffTTwithStuckingAgents = diffTTSumWithStuckingAgents / tripsWithStuckingAgents;
				if (tripsWithoutStuckingAgents != 0.) {
					avgDiffTTwithoutStuckingAgents = diffTTSumWithoutStuckingAgents / tripsWithoutStuckingAgents;
					avgDiffBeelineSpedWithoutStuckingAgents = diffBeelineSpeedSumWithoutStuckingAgents / tripsWithoutStuckingAgents;
				}
				
				aggregatedTripWriter.write(switchType.split("2")[0] +";" + switchType.split("2")[1] + ";" + tripsWithStuckingAgents + ";" + tripsWithoutStuckingAgents + ";" + avgDiffTTwithStuckingAgents + ";" + avgDiffTTwithoutStuckingAgents + ";" + avgDiffBeelineSpedWithoutStuckingAgents);
				aggregatedTripWriter.newLine();
			}
			
			aggregatedTripWriter.close();
		}
		

		{
			BufferedWriter aggregatedTripWriter = IOUtils.getBufferedWriter(analysisOutputDirectory  + "/modeSwitchAnalysisMatrix_aggregated_numberOfTrips" + agentFilter.toFileName() + tripFilter.toFileName() + ".csv");
			BufferedWriter aggregatedDiffAvgTTWriter = IOUtils.getBufferedWriter(analysisOutputDirectory  + "/modeSwitchAnalysisMatrix_aggregated_diffAvgTT" + agentFilter.toFileName() + tripFilter.toFileName() + ".csv");
			for (String mode : modes) {
				aggregatedTripWriter.write(";" + mode);
				aggregatedDiffAvgTTWriter.write(";" + mode);
		    }
		    aggregatedTripWriter.newLine();
		    aggregatedDiffAvgTTWriter.newLine();

		    
		    for (String modeA : modes) {
		        aggregatedTripWriter.write(modeA);
		        aggregatedDiffAvgTTWriter.write(modeA); 

			    for (String modeB : modes) {
			    	
			    	double trips = 0;
			        double diffTTSum = 0.;
			        
			    	for (String switchType : switchType2tripTTwithStuckingAgents.keySet()) {
				        String fromMode = switchType.split("2")[0];
				        String toMode = switchType.split("2")[1];
				        
				        if (fromMode.equals(modeA) && toMode.equals(modeB)) {
				        	trips = switchType2tripTTwithStuckingAgents.get(switchType).size();
				        	for (Double ttDiff : switchType2tripTTwithStuckingAgents.get(switchType)) {
				        		diffTTSum += ttDiff;
				        	}
				        }
					}
			        aggregatedTripWriter.write(";" + trips);
			        
			        double avgDiffTT = 0.;
			        if (trips != 0.) avgDiffTT = diffTTSum / trips;
			        
			        aggregatedDiffAvgTTWriter.write(";" + avgDiffTT); 
			    }
		        aggregatedTripWriter.newLine();
		        aggregatedDiffAvgTTWriter.newLine();
		    }
		    aggregatedTripWriter.close();
		    aggregatedDiffAvgTTWriter.close();
		}
		
		log.info("Comparing the two scenarios for each person...");

		{
			BufferedWriter writer = IOUtils.getBufferedWriter(analysisOutputDirectory + "/winner-loser-analysis_all" + agentFilter.toFileName() + tripFilter.toFileName() + ".csv");
	        writer.write("PersonId;subpopulation;homeCoordX;homeCoordY;totalTrips;score0 [utils];score1 [utils];stuck0;stuck1");
	        writer.newLine();
	       
	        double score0Sum = 0.;
	        double score1Sum = 0.;
	        
			for (Id<Person> personId : scenario1.getPopulation().getPersons().keySet()) {
				
				String subpopulationOfPerson = (String) scenario1.getPopulation().getPersons().get(personId).getAttributes().getAttribute(scenario1.getConfig().plans().getSubpopulationAttributeName());
				if (agentFilter.considerAgent(scenario1.getPopulation().getPersons().get(personId))) {
					double score0 = scenarioToCompareWith.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
			        double score1 = scenario1.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
					
			        int numberOfTrips = 0;
			        if (basicHandler1.getPersonId2tripNumber2legMode().get(personId) != null) {
			        		numberOfTrips = basicHandler1.getPersonId2tripNumber2legMode().get(personId).size();
			        }
			        
			        double homeX = 0.;
			        double homeY = 0.;
			        
			        if (personId2homeActCoord.get(personId) == null) {
			        		log.warn("No home coordinate for " + personId + ".");
			        } else {
			        		homeX = personId2homeActCoord.get(personId).getX();
			        		homeY = personId2homeActCoord.get(personId).getY();
			        }
			        
			        String stuck0 = "no";
			        if (basicHandlerToCompareWith.getPersonId2stuckAndAbortEvents().get(personId) != null) {
			        	stuck0 = "yes";
			        }
			        
			        String stuck1 = "no";
			        if (basicHandler1.getPersonId2stuckAndAbortEvents().get(personId) != null) {
			        	stuck1 = "yes";
			        }
			        
					writer.write(personId + ";"+ subpopulationOfPerson + ";"
	    	        + homeX + ";"
	    	        + homeY + ";"    
		        	+ numberOfTrips + ";"
				+ score0 + ";"
				+ score1 + ";"
				+ stuck0 + ";"
				+ stuck1
				);
		        		writer.newLine();
		        	
		        		score0Sum += score0;
		        		score1Sum += score1;
				} else {
					// skip person
				}
	        } 
			
			writer.newLine();
        	writer.write("Average score difference: " + (score1Sum - score0Sum) / (double) scenario1.getPopulation().getPersons().size() );
        	log.info("all agents: Average score difference: " + (score1Sum - score0Sum) / (double) scenario1.getPopulation().getPersons().size() );
		
        	writer.close();
		}
		
		{
			BufferedWriter writer = IOUtils.getBufferedWriter(analysisOutputDirectory + "/winner-loser-analysis_all-non-stucking-persons" + agentFilter.toFileName() + tripFilter.toFileName() + ".csv");
	        writer.write("PersonId;subpopulation;homeCoordX;homeCoordY;totalTrips;score0 [utils];score1 [utils];monetary payments 0 [EUR]; monetary payments 1 [EUR]");
	        writer.newLine();
	       
	        double score0Sum = 0.;
	        double score1Sum = 0.;
	        double tolls0Sum = 0.;
	        double tolls1Sum = 0.;
	        
			for (Id<Person> personId : scenario1.getPopulation().getPersons().keySet()) {
				
				String subpopulationOfPerson = (String) scenario1.getPopulation().getPersons().get(personId).getAttributes().getAttribute(scenario1.getConfig().plans().getSubpopulationAttributeName());
				if (agentFilter.considerAgent(scenario1.getPopulation().getPersons().get(personId))) {
					boolean analyzePerson = true;
					if (basicHandler1.getPersonId2tripNumber2stuckAbort().get(personId) != null) {
						log.info("Person " + personId + " is stucking in policy case. Excluding person from score comparison.");
						analyzePerson = false;
					}
					if (basicHandlerToCompareWith.getPersonId2tripNumber2stuckAbort().get(personId) != null) {
						log.info("Person " + personId + " is stucking in base case. Excluding person from score comparison.");
						analyzePerson = false;
					}
					
					if (analyzePerson) {
						double score0 = scenarioToCompareWith.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
				        double score1 = scenario1.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
						
				        double tolls0 = 0.;
				        if (basicHandlerToCompareWith.getPersonId2tripNumber2payment().get(personId) != null) {
				        	for (Double toll : basicHandlerToCompareWith.getPersonId2tripNumber2payment().get(personId).values()) {
					        	tolls0 += toll;
					        }
				        }
				        
				        double tolls1 = 0.;
				        if (basicHandler1.getPersonId2tripNumber2payment().get(personId) != null) {
				        	for (Double toll : basicHandler1.getPersonId2tripNumber2payment().get(personId).values()) {
					        	tolls1 += toll;
					        }
				        }
				        
				        int numberOfTrips = 0;
				        if (basicHandler1.getPersonId2tripNumber2legMode().get(personId) != null) {
				        		numberOfTrips = basicHandler1.getPersonId2tripNumber2legMode().get(personId).size();
				        }
				        
				        double homeX = 0.;
				        double homeY = 0.;
				        
				        if (personId2homeActCoord.get(personId) == null) {
				        		log.warn("No home coordinate for " + personId + ".");
				        } else {
				        		homeX = personId2homeActCoord.get(personId).getX();
				        		homeY = personId2homeActCoord.get(personId).getY();
				        }
				        
						writer.write(personId + ";" + subpopulationOfPerson + ";"
		    	        + homeX + ";"
		    	        + homeY + ";"    
			        	+ numberOfTrips + ";"
			        	+ score0 + ";"
			        	+ score1 + ";"
			        	+ tolls0 + ";"
			        	+ tolls1
						);
			        	
						writer.newLine();
			        	
			        	score0Sum += score0;
			        	score1Sum += score1;
			        	tolls0Sum += tolls0;
			        	tolls1Sum += tolls1;
					}
				} else {
					// skip person
				}
				
	        } 
			
			writer.newLine();
        	writer.write("Score sum base case; " +  score0Sum);
			writer.newLine();
        	writer.write("Score sum policy case; " +  score1Sum);
			writer.newLine();
			writer.write("Tolls sum base case; " +  tolls0Sum);
			writer.newLine();
        	writer.write("Tolls sum policy case; " +  tolls1Sum);
			writer.newLine();
        	writer.write("Number of agents; " + scenario1.getPopulation().getPersons().size() );
			writer.newLine();
        	writer.write("Average score difference per agent; " + (score1Sum - score0Sum) / (double) scenario1.getPopulation().getPersons().size() );
			writer.newLine();	
        	writer.close();
		}
		
		{
	        for (String modeA : modes) {
				BufferedWriter writer = IOUtils.getBufferedWriter(analysisOutputDirectory + "/winner-loser-analysis_" + modeA + "2" +  modeA + agentFilter.toFileName() + tripFilter.toFileName() + ".csv");
				writer.write("PersonId;subpopulation;homeCoordX;homeCoordY;totalTrips;mode2modeTrips;score0 [utils];score1 [utils];stuck0;stuck1");
	        	writer.newLine();
	       
	        	double score0Sum = 0.;
	        	double score1Sum = 0.;
	        
	        	for (Id<Person> personId : switchType2agents.get(modeA + "2" + modeA).keySet()) {
					
	    			String subpopulationOfPerson = (String) scenario1.getPopulation().getPersons().get(personId).getAttributes().getAttribute(scenario1.getConfig().plans().getSubpopulationAttributeName());
					if (agentFilter.considerAgent(scenario1.getPopulation().getPersons().get(personId))) {
						double score0 = scenarioToCompareWith.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
				        double score1 = scenario1.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
				        
				        String stuck0 = "no";
						if (basicHandlerToCompareWith.getPersonId2tripNumber2stuckAbort().get(personId) != null) {
							stuck0 = "yes";
						}
						
						String stuck1 = "no";
						if (basicHandler1.getPersonId2tripNumber2stuckAbort().get(personId) != null) {
							stuck1 = "yes";
						}
						
				        double homeX = 0.;
				        double homeY = 0.;
				        
				        if (personId2homeActCoord.get(personId) == null) {
				        		log.warn("No home coordinate for " + personId + ".");
				        } else {
				        		homeX = personId2homeActCoord.get(personId).getX();
				        		homeY = personId2homeActCoord.get(personId).getY();
				        }
				        
				        writer.write(personId + ";" + subpopulationOfPerson + ";"
					    	    + homeX + ";"
					    		+ homeY + ";"
					        	+ basicHandler1.getPersonId2tripNumber2legMode().get(personId).size() + ";"
					        	+ switchType2agents.get(modeA + "2" + modeA).get(personId) + ";"
								+ score0 + ";"
								+ score1 + ";"
								+ stuck0 + ";"
								+ stuck1);
			        		writer.newLine();
			        	
			        		score0Sum += score0;
			        		score1Sum += score1;
					} else {
						// skip person
					}
		        } 
				
				writer.newLine();
	        	writer.write("Average score difference: " + (score1Sum - score0Sum) / (double) switchType2agents.get(modeA + "2" + modeA).size() );
	        	log.info(modeA + "2" + modeA + ": Average score difference: " + (score1Sum - score0Sum) / (double) switchType2agents.get(modeA + "2" + modeA).size() );
			
	        	writer.close();
	        }
			
		}
		
		{
	        for (String modeA : modes) {

	        	BufferedWriter writer = IOUtils.getBufferedWriter(analysisOutputDirectory + "/winner-loser-analysis_x2" + modeA + agentFilter.toFileName() + tripFilter.toFileName() + ".csv");
	        	writer.write("PersonId;subpopulation;homeCoordX;homeCoordY;totalTrips;x2modeTrips;score0 [utils];score1 [utils];stuck0;stuck1");
	        	writer.newLine();
	       
	        	double score0Sum = 0.;
	        	double score1Sum = 0.;
	        
	        	for (Id<Person> personId : switchType2agents.get("x2" + modeA).keySet()) {
	        		
	    			String subpopulationOfPerson = (String) scenario1.getPopulation().getPersons().get(personId).getAttributes().getAttribute(scenario1.getConfig().plans().getSubpopulationAttributeName());
					if (agentFilter.considerAgent(scenario1.getPopulation().getPersons().get(personId))) {
	        			double score0 = scenarioToCompareWith.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
				        double score1 = scenario1.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
						
				        String stuck0 = "no";
						if (basicHandlerToCompareWith.getPersonId2tripNumber2stuckAbort().get(personId) != null) {
							stuck0 = "yes";
						}
						
						String stuck1 = "no";
						if (basicHandler1.getPersonId2tripNumber2stuckAbort().get(personId) != null) {
							stuck1 = "yes";
						}
				        
				        double homeX = 0.;
				        double homeY = 0.;
				        				        
				        if (personId2homeActCoord.get(personId) == null) {
				        		log.warn("No home coordinate for " + personId + ".");
				        } else {
				        		homeX = personId2homeActCoord.get(personId).getX();
				        		homeY = personId2homeActCoord.get(personId).getY();
				        }
				        
				        writer.write(personId + ";" + subpopulationOfPerson + ";"
					    	    + homeX + ";"
					    		+ homeY + ";"
					        	+ basicHandler1.getPersonId2tripNumber2legMode().get(personId).size() + ";"
					        	+ switchType2agents.get("x2" + modeA).get(personId) + ";"
					        	+ score0 + ";"
					        	+ score1 + ";"
					        	+ stuck0 + ";"
					        	+ stuck1);
			        	writer.newLine();
			        	
			        	score0Sum += score0;
			        	score1Sum += score1;
					} else {
						// skip person
					}
		        } 
				
				writer.newLine();
	        	writer.write("Average score difference: " + (score1Sum - score0Sum) / (double) switchType2agents.get("x2" + modeA).size() );
	        	log.info("x2 " + modeA + " agents: Average score difference: " + (score1Sum - score0Sum) / (double) switchType2agents.get("x2" + modeA).size() );
			
	        	writer.close();
	        }
		}
		
		{
	        for (String modeA : modes) {

				BufferedWriter writer = IOUtils.getBufferedWriter(analysisOutputDirectory + "/winner-loser-analysis_" + modeA + "2x" + agentFilter.toFileName() + tripFilter.toFileName() + ".csv");
	        	writer.write("PersonId;subpopulation;homeCoordX;homeCoordY;totalTrips;mode2xTrips;score0 [utils];score1 [utils];stuck0;stuck1");
	        	writer.newLine();
	       
	        	double score0Sum = 0.;
	        	double score1Sum = 0.;
	        
	        	for (Id<Person> personId : switchType2agents.get(modeA + "2x").keySet()) {
					
	    			String subpopulationOfPerson = (String) scenario1.getPopulation().getPersons().get(personId).getAttributes().getAttribute(scenario1.getConfig().plans().getSubpopulationAttributeName());
					if (agentFilter.considerAgent(scenario1.getPopulation().getPersons().get(personId))) {
						double score0 = scenarioToCompareWith.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
				        double score1 = scenario1.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
						
				        String stuck0 = "no";
						if (basicHandlerToCompareWith.getPersonId2tripNumber2stuckAbort().get(personId) != null) {
							stuck0 = "yes";
						}
						
						String stuck1 = "no";
						if (basicHandler1.getPersonId2tripNumber2stuckAbort().get(personId) != null) {
							stuck1 = "yes";
						}
				        
				        double homeX = 0.;
				        double homeY = 0.;
				        
				        if (personId2homeActCoord.get(personId) == null) {
				        		log.warn("No home coordinate for " + personId + ".");
				        } else {
				        		homeX = personId2homeActCoord.get(personId).getX();
				        		homeY = personId2homeActCoord.get(personId).getY();
				        }
				        
				        writer.write(personId + ";" + subpopulationOfPerson + ";"
					    	    + homeX + ";"
					    		+ homeY + ";"
					        	+ basicHandler1.getPersonId2tripNumber2legMode().get(personId).size() + ";"
					        	+ switchType2agents.get(modeA + "2x").get(personId) + ";"
					        	+ score0 + ";"
					        	+ score1 + ";"
					        	+ stuck0 + ";"
					        	+ stuck1);
				        
			        	writer.newLine();
			        	
			        	score0Sum += score0;
			        	score1Sum += score1;
					}
		        } 
				
				writer.newLine();
	        	writer.write("Average score difference: " + (score1Sum - score0Sum) / (double) switchType2agents.get(modeA + "2x").size() );
	        	log.info(modeA + "2x: " + "Average score difference: " + (score1Sum - score0Sum) / (double) switchType2agents.get(modeA + "2x").size() );
			
	        	writer.close();
	        }
		}
		
		log.info("Comparing the two scenarios for each person... Done.");
    }

	private void printCoordinates(Map<String, Coord> id2Coord, String fileName) throws IOException {
        BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
        writer.write("Id;xCoord;yCoord");
        writer.newLine();
        for (String personTripNr : id2Coord.keySet()) {
        		if (id2Coord.get(personTripNr) != null) {
        			writer.write(personTripNr + ";" + id2Coord.get(personTripNr).getX() + ";" + id2Coord.get(personTripNr).getY());
            		writer.newLine();
        		}
        } 
        writer.close();
	}

	public void analyzeByScore(double scoreDifferenceTolerance) {
		
		Map<Id<Person>, Tuple<Double, Double>> person2score0score1 = new HashMap<>();
		Set<Id<Person>> winners = new HashSet<>();
		Set<Id<Person>> losers = new HashSet<>();
		Set<Id<Person>> sameScorePersons = new HashSet<>();
		
		for (Person person : this.scenario1.getPopulation().getPersons().values()) {
			
			if (agentFilter.considerAgent(person)) {
				double score1 = person.getSelectedPlan().getScore();
				double score0 = this.scenarioToCompareWith.getPopulation().getPersons().get(person.getId()).getSelectedPlan().getScore();
				
				person2score0score1.put(person.getId(), new Tuple<Double, Double>(score0, score1));
				
				if (score1 > score0 + scoreDifferenceTolerance) {
					winners.add(person.getId());
					
				} else if (score0 > score1 + scoreDifferenceTolerance) {
					losers.add(person.getId());
				
				} else {
					sameScorePersons.add(person.getId());
				}
			}
		}
	
		try {
			
			printCSVFile(sameScorePersons, person2score0score1, this.analysisOutputDirectory + "winner-loser-analysis_same-score-persons_score-tolerance-" + scoreDifferenceTolerance + agentFilter.toFileName() + ".csv");
			printCSVFile(winners, person2score0score1, this.analysisOutputDirectory + "winner-loser-analysis_winners_score-tolerance-" + scoreDifferenceTolerance + agentFilter.toFileName() + ".csv");
			printCSVFile(losers, person2score0score1, this.analysisOutputDirectory + "winner-loser-analysis_losers_score-tolerance-" + scoreDifferenceTolerance + agentFilter.toFileName() + ".csv");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void printCSVFile(Set<Id<Person>> persons, Map<Id<Person>, Tuple<Double, Double>> person2score0score1, String fileName) throws IOException {
		BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
        writer.write("Id;subpopulation;homeCoordX;homeCoordY;score0 [utils];score1 [utils];score-difference [utils]");
        writer.newLine();
        
        for (Id<Person> personId : persons) {
        		double homeX = 0.;
        		double homeY = 0.;
        		
        		if (this.personId2homeActCoord.get(personId) != null) {
        			homeX = this.personId2homeActCoord.get(personId).getX();
        			homeY = this.personId2homeActCoord.get(personId).getY();
        		}
    			String subpopulationOfPerson = (String) scenario1.getPopulation().getPersons().get(personId).getAttributes().getAttribute(scenario1.getConfig().plans().getSubpopulationAttributeName());
				writer.write(personId + ";" + subpopulationOfPerson  + ";"
        + homeX
        + ";" + homeY
        + ";" + person2score0score1.get(personId).getFirst()
        + ";" + person2score0score1.get(personId).getSecond()
        + ";" + (person2score0score1.get(personId).getSecond() - person2score0score1.get(personId).getFirst())
        );
        		writer.newLine();
        } 
        writer.close();
	}
}
