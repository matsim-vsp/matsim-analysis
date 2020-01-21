/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.analysis.detailedPersonTripAnalysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.analysis.AgentFilter;
import org.matsim.analysis.TripFilter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.decongestion.handler.DelayAnalysis;

/**
 * @author ikaddoura
 *
 */
public class PersonTripAnalysis {
	private static final Logger log = Logger.getLogger(PersonTripAnalysis.class);
	
	public void printAvgValuePerParameter(String csvFile, SortedMap<Double, List<Double>> parameter2values) {
		String fileName = csvFile;
		File file = new File(fileName);			

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));

			for (Double parameter : parameter2values.keySet()) {
				double sum = 0.;
				int counter = 0;
				for (Double value : parameter2values.get(parameter)) {
					sum = sum + value;
					counter++;
				}
				
				bw.write(String.valueOf(parameter) + ";" + sum / counter);
				bw.newLine();
			}
			log.info("Output written to " + fileName);
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void printPersonInformation(String outputPath,
			String mode,
			Map<Id<Person>, Double> personId2userBenefit,
			BasicPersonTripAnalysisHandler basicHandler,
			AgentFilter agentFilter) {
		
		boolean ignoreModes = false;
		if (mode == null) {
			mode = "all_transport_modes";
			ignoreModes = true;
		}
		
		String agentFilterFileName;
		if (agentFilter == null) {
			agentFilterFileName = "";
		} else {
			agentFilterFileName = agentFilter.toFileName();
		}	

		String fileName = outputPath + "person_info_" + mode + agentFilterFileName + ".csv";
		File file = new File(fileName);			

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));

			bw.write( "person Id;"
					+ "number of " + mode + " trips;"
					+ "at least one stuck and abort " + mode + " trip (yes/no);"
					+ "number of stuck and abort events (day);"
					+ mode + " total travel time (day) [sec];"
					+ mode + " total in-vehicle time (day) [sec];"
					+ mode + " total travel distance (day) [m];"
					+ "travel related user benefits (based on the selected plans score) [monetary units];"
					+ "total money payments (day) [monetary units]"
					);
			bw.newLine();
			
			for (Id<Person> id : basicHandler.getScenario().getPopulation().getPersons().keySet()) {
				
				boolean considerPerson;
				if (agentFilter == null) {
					considerPerson = true;
				} else {
					considerPerson = agentFilter.considerAgent(basicHandler.getScenario().getPopulation().getPersons().get(id));
				}
				
				if (considerPerson) {
					double userBenefit = Double.NEGATIVE_INFINITY;
					if (personId2userBenefit.containsKey(id)) {
						userBenefit = personId2userBenefit.get(id);
					}
					int mode_trips = 0;
					String mode_stuckAbort = "no";
					int numberOfStuckAndAbortEvents = 0;
					double mode_travelTime = 0.;
					double mode_inVehTime = 0.;
					double mode_travelDistance = 0.;
					
					double tollPayments = 0.;
					
					if (basicHandler.getPersonId2tripNumber2tripMainMode().containsKey(id)) {
						for (Integer trip : basicHandler.getPersonId2tripNumber2tripMainMode().get(id).keySet()) {
							
							if (basicHandler.getPersonId2tripNumber2payment().containsKey(id) && basicHandler.getPersonId2tripNumber2payment().get(id).containsKey(trip)) {
								tollPayments = tollPayments + basicHandler.getPersonId2tripNumber2payment().get(id).get(trip);
							}
							
							if (ignoreModes || basicHandler.getPersonId2tripNumber2tripMainMode().get(id).get(trip).equals(mode)) {
								
								mode_trips++;
								
								if (basicHandler.getPersonId2tripNumber2stuckAbort().containsKey(id) && basicHandler.getPersonId2tripNumber2stuckAbort().get(id).containsKey(trip)) {
									if (basicHandler.getPersonId2tripNumber2stuckAbort().get(id).get(trip)) {
										mode_stuckAbort = "yes";
									}
								}
								
								if (basicHandler.getPersonId2stuckAndAbortEvents().containsKey(id)) {
									numberOfStuckAndAbortEvents = basicHandler.getPersonId2stuckAndAbortEvents().get(id);
								}
								
								if (basicHandler.getPersonId2tripNumber2travelTime().containsKey(id) && basicHandler.getPersonId2tripNumber2travelTime().get(id).containsKey(trip)) {
									mode_travelTime = mode_travelTime + basicHandler.getPersonId2tripNumber2travelTime().get(id).get(trip);
								}
								
								if (basicHandler.getPersonId2tripNumber2inVehicleTime().containsKey(id) && basicHandler.getPersonId2tripNumber2inVehicleTime().get(id).containsKey(trip)) {
									mode_inVehTime = mode_inVehTime + basicHandler.getPersonId2tripNumber2inVehicleTime().get(id).get(trip);
								}
								
								if (basicHandler.getPersonId2tripNumber2tripDistance().containsKey(id) && basicHandler.getPersonId2tripNumber2tripDistance().get(id).containsKey(trip)) {
									mode_travelDistance = mode_travelDistance + basicHandler.getPersonId2tripNumber2tripDistance().get(id).get(trip);
								}			
							}
						}
					}
					
					bw.write(id + ";"
							+ mode_trips + ";"
							+ mode_stuckAbort + ";"
							+ numberOfStuckAndAbortEvents + ";"
							+ mode_travelTime + ";"
							+ mode_inVehTime + ";"
							+ mode_travelDistance + ";"
							
							+ userBenefit + ";"
							+ tollPayments + ";"
							);
					
					bw.newLine();		
				}
			}
			
			log.info("Output written to " + fileName);
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public void printTripInformation(String outputPath,
			String mode,
			BasicPersonTripAnalysisHandler basicHandler,
			TripFilter tripFilter) {
		
		boolean ignoreModes = false;
		if (mode == null) {
			mode = "all_transport_modes";
			ignoreModes = true;
		}
		
		String tripFilterFileName;
		if (tripFilter == null) {
			tripFilterFileName = "";
		} else {
			tripFilterFileName = tripFilter.toFileName();
		}
				
		String fileName = outputPath + "trip_info_" + mode + tripFilterFileName + ".csv";
		File file = new File(fileName);			

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			bw.write( "person Id;"
					+ "trip no.;"
					+ "trip main mode;"
					+ "trip modes (excl. help leg modes);"
					+ "stuck and abort trip (yes/no);"
					+ "departure time (trip) [sec];"
					+ "arrival time (trip) [sec];"
					+ "travel time (trip) [sec];"
					+ "in-vehicle time (trip) [sec];"
					+ "travel distance (trip) [m];"
					+ "origin X coordinate (trip);"
					+ "origin Y coordinate (trip);"
					+ "destination X coordinate (trip);"
					+ "destination Y coordinate (trip);"
					+ "beeline distance (trip) [m];"
					+ "toll payments (trip) [monetary units]");
			
			bw.newLine();
			
			for (Id<Person> id : basicHandler.getPersonId2tripNumber2tripMainMode().keySet()) {
				
				for (Integer trip : basicHandler.getPersonId2tripNumber2tripMainMode().get(id).keySet()) {
							
					boolean considerTrip;
					if (tripFilter == null) {
						considerTrip = true;
					} else {
						considerTrip = tripFilter.considerTrip(basicHandler.getPersonId2tripNumber2originCoord().get(id).get(trip), basicHandler.getPersonId2tripNumber2destinationCoord().get(id).get(trip));
					}
					if (considerTrip) {
						if (ignoreModes || basicHandler.getPersonId2tripNumber2tripMainMode().get(id).get(trip).equals(mode)) {
							
							String tripMainMode = basicHandler.getPersonId2tripNumber2tripMainMode().get(id).get(trip);
							
							String nonHelpLegModesThisTrip = basicHandler.getPersonId2tripNumber2tripModes().get(id).get(trip);
	
							String stuckAbort = "no";
							if (basicHandler.getPersonId2tripNumber2stuckAbort().containsKey(id) && basicHandler.getPersonId2tripNumber2stuckAbort().get(id).containsKey(trip)) {
								if (basicHandler.getPersonId2tripNumber2stuckAbort().get(id).get(trip)) {
									stuckAbort = "yes";
								}
							}
							
							String departureTime = "unknown";
							if (basicHandler.getPersonId2tripNumber2departureTime().containsKey(id) && basicHandler.getPersonId2tripNumber2departureTime().get(id).containsKey(trip)) {
								departureTime = String.valueOf(basicHandler.getPersonId2tripNumber2departureTime().get(id).get(trip));
							}
							
							
							String arrivalTime = "unknown";
							if (basicHandler.getPersonId2tripNumber2arrivalTime().containsKey(id) && basicHandler.getPersonId2tripNumber2arrivalTime().get(id).containsKey(trip)){
								arrivalTime = String.valueOf(basicHandler.getPersonId2tripNumber2arrivalTime().get(id).get(trip));
							}
							
							String travelTime = "unknown";
							if (basicHandler.getPersonId2tripNumber2travelTime().containsKey(id) && basicHandler.getPersonId2tripNumber2travelTime().get(id).containsKey(trip)) {
								travelTime = String.valueOf(basicHandler.getPersonId2tripNumber2travelTime().get(id).get(trip));
							}
							
							String inVehTime = "unknown";
							if (basicHandler.getPersonId2tripNumber2inVehicleTime().containsKey(id) && basicHandler.getPersonId2tripNumber2inVehicleTime().get(id).containsKey(trip)) {
								inVehTime = String.valueOf(basicHandler.getPersonId2tripNumber2inVehicleTime().get(id).get(trip));
							}
							
							String travelDistance = "unknown";
							if (basicHandler.getPersonId2tripNumber2tripDistance().containsKey(id) && basicHandler.getPersonId2tripNumber2tripDistance().get(id).containsKey(trip)) {
								travelDistance = String.valueOf(basicHandler.getPersonId2tripNumber2tripDistance().get(id).get(trip));
							}
							
							String tripOriginCoordinateX = "unknown";
							if (basicHandler.getPersonId2tripNumber2originCoord().containsKey(id) && basicHandler.getPersonId2tripNumber2originCoord().get(id).containsKey(trip)) {
								tripOriginCoordinateX = String.valueOf(basicHandler.getPersonId2tripNumber2originCoord().get(id).get(trip).getX());
							}
							
							String tripOriginCoordinateY = "unknown";
							if (basicHandler.getPersonId2tripNumber2originCoord().containsKey(id) && basicHandler.getPersonId2tripNumber2originCoord().get(id).containsKey(trip)) {
								tripOriginCoordinateY = String.valueOf(basicHandler.getPersonId2tripNumber2originCoord().get(id).get(trip).getY());
							}
							
							String tripDestinationCoordinateX = "unknown";
							if (basicHandler.getPersonId2tripNumber2destinationCoord().containsKey(id) && basicHandler.getPersonId2tripNumber2destinationCoord().get(id).containsKey(trip)) {
								tripDestinationCoordinateX = String.valueOf(basicHandler.getPersonId2tripNumber2destinationCoord().get(id).get(trip).getX());
							}
							
							String tripDestinationCoordinateY = "unknown";
							if (basicHandler.getPersonId2tripNumber2destinationCoord().containsKey(id) && basicHandler.getPersonId2tripNumber2destinationCoord().get(id).containsKey(trip)) {
								tripDestinationCoordinateY = String.valueOf(basicHandler.getPersonId2tripNumber2destinationCoord().get(id).get(trip).getY());
							}
							
							String beelineDistance = "unknown";
							if (basicHandler.getPersonId2tripNumber2tripBeelineDistance().containsKey(id) && basicHandler.getPersonId2tripNumber2tripBeelineDistance().get(id).containsKey(trip)) {
								beelineDistance = String.valueOf(basicHandler.getPersonId2tripNumber2tripBeelineDistance().get(id).get(trip));
							}
							
							String tollPayment = "unknown";
							if (basicHandler.getPersonId2tripNumber2payment().containsKey(id) && basicHandler.getPersonId2tripNumber2payment().get(id).containsKey(trip)) {
								tollPayment = String.valueOf(basicHandler.getPersonId2tripNumber2payment().get(id).get(trip));
							}
							
							bw.write(id + ";"
							+ trip + ";"
							+ tripMainMode + ";"
							+ nonHelpLegModesThisTrip + ";"
							+ stuckAbort + ";"
							+ departureTime + ";"
							+ arrivalTime + ";"
							+ travelTime + ";"
							+ inVehTime + ";"
							+ travelDistance + ";"
							+ tripOriginCoordinateX + ";"
							+ tripOriginCoordinateY + ";"
							+ tripDestinationCoordinateX + ";"
							+ tripDestinationCoordinateY + ";"
							+ beelineDistance + ";"
							+ tollPayment
							);
							bw.newLine();						
						}
					}
				}
			}
			
			log.info("Output written to " + fileName);
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void printAggregatedResults(String outputPath,
			String mode,
			Map<Id<Person>, Double> personId2userBenefit,
			BasicPersonTripAnalysisHandler basicHandler) {
		
		boolean ignoreModes = false;
		if (mode == null) {
			mode = "all_transport_modes";
			ignoreModes = true;
		}
	
		String fileName = outputPath + "aggregated_info_" + mode + ".csv";
		File file = new File(fileName);			

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			double userBenefits = 0.;
			for (Double userBenefit : personId2userBenefit.values()) {
				userBenefits = userBenefits + userBenefit;
			}
			
			int mode_trips = 0;
			int mode_StuckAndAbortTrips = 0;
			int stuckAndAbortEvents = 0;
			double mode_TravelTime = 0.;
			double mode_inVehTime = 0.;
			double mode_TravelDistance = 0.;			
			
			for (Id<Person> id : basicHandler.getScenario().getPopulation().getPersons().keySet()) {
				
				if (basicHandler.getPersonId2tripNumber2tripMainMode().containsKey(id)) {
					
					for (Integer trip : basicHandler.getPersonId2tripNumber2tripMainMode().get(id).keySet()) {
						
						// only for the predefined mode
						
						if (ignoreModes || basicHandler.getPersonId2tripNumber2tripMainMode().get(id).get(trip).equals(mode)) {
							
							mode_trips++;
							
							if (basicHandler.getPersonId2tripNumber2stuckAbort().containsKey(id) && basicHandler.getPersonId2tripNumber2stuckAbort().get(id).containsKey(trip)) {
								if (basicHandler.getPersonId2tripNumber2stuckAbort().get(id).get(trip)) {
									mode_StuckAndAbortTrips++;
								}
							}
							
							if (basicHandler.getPersonId2stuckAndAbortEvents().containsKey(id)) {
								stuckAndAbortEvents = basicHandler.getPersonId2stuckAndAbortEvents().get(id);
							}
							
							if (basicHandler.getPersonId2tripNumber2travelTime().containsKey(id) && basicHandler.getPersonId2tripNumber2travelTime().get(id).containsKey(trip)) {
								mode_TravelTime = mode_TravelTime + basicHandler.getPersonId2tripNumber2travelTime().get(id).get(trip);
							}
							
							if (basicHandler.getPersonId2tripNumber2inVehicleTime().containsKey(id) && basicHandler.getPersonId2tripNumber2inVehicleTime().get(id).containsKey(trip)) {
								mode_inVehTime = mode_inVehTime + basicHandler.getPersonId2tripNumber2inVehicleTime().get(id).get(trip);
							}
							
							if (basicHandler.getPersonId2tripNumber2tripDistance().containsKey(id) && basicHandler.getPersonId2tripNumber2tripDistance().get(id).containsKey(trip)) {
								mode_TravelDistance = mode_TravelDistance + basicHandler.getPersonId2tripNumber2tripDistance().get(id).get(trip);
							}		
						}
					}
				}	
			}
			
			bw.write("path;" + outputPath);
			bw.newLine();

			bw.newLine();
			
			bw.write("number of " + mode + " trips (sample size);" + mode_trips);
			bw.newLine();
			
			bw.write("number of " + mode + " stuck and abort trip (sample size);" + mode_StuckAndAbortTrips);
			bw.newLine();
			
			bw.write("number of stuck and abort events (sample size);" + stuckAndAbortEvents);
			bw.newLine();
			
			bw.newLine();
						
			bw.write(mode + " travel distance (sample size) [km];" + mode_TravelDistance / 1000.);
			bw.newLine();
			
			bw.write(mode + " travel time (sample size) [hours];" + mode_TravelTime / 3600.);
			bw.newLine();
			
			bw.write(mode + " in-vehicle time (sample size) [hours];" + mode_inVehTime / 3600.);
			bw.newLine();
		
			log.info("Output written to " + fileName);
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public void printAggregatedResults(String outputPath,
			Map<Id<Person>, Double> personId2userBenefit,
			BasicPersonTripAnalysisHandler basicHandler,
			DelayAnalysis delayAnalysis
			) {
	
		String fileName = outputPath + "aggregated_info.csv";
		File file = new File(fileName);			

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			bw.write("path;" + outputPath);
			bw.newLine();
			
			bw.write("-----------------------------");
			bw.newLine();
			bw.write("## Mode-specific analysis ##");
			bw.newLine();
			bw.write("-----------------------------");
			bw.newLine();

			
			for (String mode : basicHandler.getScenario().getConfig().planCalcScore().getModes().keySet()) {
				int mode_trips = 0;
				int mode_StuckAndAbortTrips = 0;
				double mode_TravelTime = 0.;
				double mode_inVehTime = 0.;
				double mode_TravelDistance = 0.;
				
				for (Id<Person> id : basicHandler.getScenario().getPopulation().getPersons().keySet()) {
					
					if (basicHandler.getPersonId2tripNumber2tripMainMode().containsKey(id)) {
						
						for (Integer trip : basicHandler.getPersonId2tripNumber2tripMainMode().get(id).keySet()) {
														
							// only for the predefined mode
							
							if (basicHandler.getPersonId2tripNumber2tripMainMode().get(id).get(trip).equals(mode)) {
								
								mode_trips++;
								
								if (basicHandler.getPersonId2tripNumber2stuckAbort().containsKey(id) && basicHandler.getPersonId2tripNumber2stuckAbort().get(id).containsKey(trip)) {
									if (basicHandler.getPersonId2tripNumber2stuckAbort().get(id).get(trip)) {
										mode_StuckAndAbortTrips++;
									}
								}
								
								if (basicHandler.getPersonId2tripNumber2travelTime().containsKey(id) && basicHandler.getPersonId2tripNumber2travelTime().get(id).containsKey(trip)) {
									mode_TravelTime = mode_TravelTime + basicHandler.getPersonId2tripNumber2travelTime().get(id).get(trip);
								}
								
								if (basicHandler.getPersonId2tripNumber2inVehicleTime().containsKey(id) && basicHandler.getPersonId2tripNumber2inVehicleTime().get(id).containsKey(trip)) {
									mode_inVehTime = mode_inVehTime + basicHandler.getPersonId2tripNumber2inVehicleTime().get(id).get(trip);
								}
								
								if (basicHandler.getPersonId2tripNumber2tripDistance().containsKey(id) && basicHandler.getPersonId2tripNumber2tripDistance().get(id).containsKey(trip)) {
									mode_TravelDistance = mode_TravelDistance + basicHandler.getPersonId2tripNumber2tripDistance().get(id).get(trip);
								}		
							}
						}
					}	
				}
				
				double vtts_mode_traveling = (basicHandler.getScenario().getConfig().planCalcScore().getPerforming_utils_hr() - basicHandler.getScenario().getConfig().planCalcScore().getModes().get(mode).getMarginalUtilityOfTraveling()) / basicHandler.getScenario().getConfig().planCalcScore().getMarginalUtilityOfMoney();
				
				bw.write("number of " + mode + " trips (sample size);" + mode_trips);
				bw.newLine();
				
				bw.write(mode + " mode specific costs (sample size) [monetary units];" + mode_trips * basicHandler.getScenario().getConfig().planCalcScore().getModes().get(mode).getConstant() * (-1));
				bw.newLine();
				
				bw.write("number of " + mode + " stuck and abort trip (sample size);" + mode_StuckAndAbortTrips);
				bw.newLine();
				
				bw.write("vtts traveling / in-vehicle " + mode + " [monetary units / hour];" + vtts_mode_traveling);
				bw.newLine();
				
				bw.write(mode + " travel distance (sample size) [km];" + mode_TravelDistance / 1000.);
				bw.newLine();
				
				bw.write(mode + " distance costs (sample size) [monetary units];" + (-1) * mode_TravelDistance * basicHandler.getScenario().getConfig().planCalcScore().getModes().get(mode).getMonetaryDistanceRate() );
				bw.newLine();
				
				bw.write(mode + " travel time (sample size) [hours];" + mode_TravelTime / 3600.);
				bw.newLine();
				
				bw.write(mode + " travel time costs (sample size) [monetary units];" + (mode_TravelTime / 3600.) * vtts_mode_traveling);
				bw.newLine();
				
				bw.write(mode + " in-vehicle time (sample size) [hours];" + mode_inVehTime / 3600.);
				bw.newLine();
				
				bw.write(mode + " in-vehicle time costs (sample size) [monetary units];" + (mode_inVehTime / 3600.) * vtts_mode_traveling);
				bw.newLine();
				
				bw.write("-----------------------------");
				bw.newLine();	

			}	

			double userBenefitsIncludingMonetaryPayments = 0.;
			for (Double userBenefit : personId2userBenefit.values()) {
				userBenefitsIncludingMonetaryPayments = userBenefitsIncludingMonetaryPayments + userBenefit;
			}
			
			int allTrips = 0;
			int allStuckAndAbortTrips = 0;
			double moneyPaymentsByUsers = 0.;
			
			for (Id<Person> id : basicHandler.getScenario().getPopulation().getPersons().keySet()) {
				
				if (basicHandler.getPersonId2tripNumber2tripMainMode().containsKey(id)) {
					
					for (Integer trip : basicHandler.getPersonId2tripNumber2tripMainMode().get(id).keySet()) {
						
						// for all modes
						
						allTrips++;
						
						if (basicHandler.getPersonId2tripNumber2stuckAbort().containsKey(id) && basicHandler.getPersonId2tripNumber2stuckAbort().get(id).containsKey(trip)) {
							if (basicHandler.getPersonId2tripNumber2stuckAbort().get(id).get(trip)) {
								allStuckAndAbortTrips++;
							}
						}
						
						if (basicHandler.getPersonId2tripNumber2payment().containsKey(id) && basicHandler.getPersonId2tripNumber2payment().get(id).containsKey(trip)) {
							moneyPaymentsByUsers = moneyPaymentsByUsers + basicHandler.getPersonId2tripNumber2payment().get(id).get(trip);
						}
						
					}
				}
		
			}
			
			bw.write("-----------------------------");
			bw.newLine();
			bw.write("## Analysis for all modes ##");
			bw.newLine();
			bw.write("-----------------------------");
			bw.newLine();
									
			bw.write("number of trips (sample size, all modes);" + allTrips);
			bw.newLine();
			
			bw.write("number of stuck and abort trips (sample size, all modes);" + allStuckAndAbortTrips);
			bw.newLine();
			
			bw.write("number of persons with at least one stuck and abort event (sample size, all modes);" + basicHandler.getPersonId2stuckAndAbortEvents().size());
			bw.newLine();
			
			bw.write("-----------");
			bw.newLine();
			
			bw.write("total payments (sample size) [monetary units];" + basicHandler.getTotalPayments());
			bw.newLine();
			
			bw.write("total rewards (sample size) [monetary units];" + basicHandler.getTotalRewards());
			bw.newLine();
			
			bw.write("total amounts (sample size) [monetary units];" + basicHandler.getTotalAmounts());
			bw.newLine();
			
			if (delayAnalysis != null) {
				bw.write("total delay (sample size) [hours];" + delayAnalysis.getTotalDelay() / 3600.);
				bw.newLine();
			}
			
			bw.write("-----------");
			bw.newLine();
			
			bw.write("total reward of all transport users (sample size) [monetary units];" + basicHandler.getTotalRewardsByPersons());
			bw.newLine();
			
			bw.write("-----------");
			bw.newLine();
			bw.write("-----------");
			bw.newLine();
			
			bw.write("travel related user benefits (sample size) (including toll payments) [monetary units];" + userBenefitsIncludingMonetaryPayments);
			bw.newLine();
			
			bw.write("revenues (sample size) (tolls/fares paid by private car users or passengers) [monetary units];" + moneyPaymentsByUsers);
			bw.newLine();
		
			bw.write("-----------");
			bw.newLine();
			bw.write("-----------");
			bw.newLine();
			
			log.info("Output written to " + fileName);
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public SortedMap<Double, List<Double>> getParameter2Values(
			String mode, String[] excludedIdPrefixes,
			BasicPersonTripAnalysisHandler basicHandler,
			Map<Id<Person>, Map<Integer, Double>> personId2tripNumber2parameter,
			Map<Id<Person>, Map<Integer, Double>> personId2tripNumber2value,
			double intervalLength, double finalInterval) {
		
		Map<Id<Person>, Map<Integer, String>> personId2tripNumber2legMode = basicHandler.getPersonId2tripNumber2tripMainMode();
		
		SortedMap<Double, List<Double>> parameter2values = new TreeMap<>();
		Map<Integer, List<Double>> nr2values = new HashMap<>();
		
		for (Id<Person> id : personId2tripNumber2legMode.keySet()) {
			
			if (excludePerson(id, excludedIdPrefixes)) {

			} else {
				
				for (Integer trip : personId2tripNumber2legMode.get(id).keySet()) {
					
					if (personId2tripNumber2legMode.get(id).get(trip).equals(mode)) {
						
						double departureTime = personId2tripNumber2parameter.get(id).get(trip);
						int nr = (int) (departureTime / intervalLength) + 1;
						
						double value = 0.;
						if (personId2tripNumber2value.containsKey(id) && personId2tripNumber2value.get(id).containsKey(trip)) {
							value = personId2tripNumber2value.get(id).get(trip);
						}
						
						if (nr2values.containsKey(nr)) {
							List<Double> values = nr2values.get(nr);
							values.add(value);
							nr2values.put(nr, values);
						} else {
							List<Double> values = new ArrayList<>();
							values.add(value);
							nr2values.put(nr, values);
						}				
					}
				}
			}
		}
		for (Integer nr : nr2values.keySet()) {
			parameter2values.put(nr * intervalLength, nr2values.get(nr));
		}
		return parameter2values;
	}
	
	private boolean excludePerson(Id<Person> id, String[] excludedIdPrefixes) {
		
		boolean excludePerson = false;
		
		for (String prefix : excludedIdPrefixes) {
			if (id.toString().startsWith(prefix)) {
				excludePerson = true;
			}
		}
		return excludePerson;
	}

	public SortedMap<Double, List<Double>> getParameter2Values(
			String mode,
			BasicPersonTripAnalysisHandler basicHandler,
			Map<Id<Person>, Map<Integer, Double>> personId2tripNumber2parameter,
			Map<Id<Person>, Map<Integer, Double>> personId2tripNumber2value,
			double intervalLength, double finalInterval) {
		
		Map<Id<Person>, Map<Integer, String>> personId2tripNumber2legMode = basicHandler.getPersonId2tripNumber2tripMainMode();
		
		SortedMap<Double, List<Double>> parameter2values = new TreeMap<>();
		Map<Integer, List<Double>> nr2values = new HashMap<>();
		
		for (Id<Person> id : personId2tripNumber2legMode.keySet()) {
			
			for (Integer trip : personId2tripNumber2legMode.get(id).keySet()) {
				
				if (personId2tripNumber2legMode.get(id).get(trip).equals(mode)) {
					
					double departureTime = personId2tripNumber2parameter.get(id).get(trip);
					int nr = (int) (departureTime / intervalLength) + 1;
					
					double value = 0.;
					if (personId2tripNumber2value.containsKey(id) && personId2tripNumber2value.get(id).containsKey(trip)) {
						value = personId2tripNumber2value.get(id).get(trip);
					}
					
					if (nr2values.containsKey(nr)) {
						List<Double> values = nr2values.get(nr);
						values.add(value);
						nr2values.put(nr, values);
					} else {
						List<Double> values = new ArrayList<>();
						values.add(value);
						nr2values.put(nr, values);
					}				
				}
			}
		}
		for (Integer nr : nr2values.keySet()) {
			parameter2values.put(nr * intervalLength, nr2values.get(nr));
		}
		return parameter2values;
	}
}
