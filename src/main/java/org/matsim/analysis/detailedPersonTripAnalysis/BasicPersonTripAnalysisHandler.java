/* *********************************************************************** *
 * project: org.matsim.*
 * MoneyEventHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.analysis.detailedPersonTripAnalysis;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.vehicles.Vehicle;

import java.util.*;

/**
 * 
 * @author ikaddoura , lkroeger
 *
 */
public class BasicPersonTripAnalysisHandler implements PersonMoneyEventHandler, TransitDriverStartsEventHandler, ActivityEndEventHandler, ActivityStartEventHandler,
PersonDepartureEventHandler , PersonArrivalEventHandler , LinkEnterEventHandler, PersonEntersVehicleEventHandler ,
PersonLeavesVehicleEventHandler , PersonStuckEventHandler {
	
	private final static Logger log = Logger.getLogger(BasicPersonTripAnalysisHandler.class);
	private final String helpActivitySubString;
	private AnalysisMainModeIdentifier mainModeIdentifier;	
	
	public BasicPersonTripAnalysisHandler(String helpActivitySubString, AnalysisMainModeIdentifier mainModeIdentifier) {
		this.helpActivitySubString = helpActivitySubString;
		this.mainModeIdentifier = mainModeIdentifier;
	}

	@Inject
	private Scenario scenario;
	
	// temporary information
	private final Map<Id<Person>,Integer> personId2currentTripNumber = new HashMap<>();
	private final Map<Id<Person>,Double> personId2distanceEnterValue = new HashMap<>();
	private final Map<Id<Vehicle>,Double> networkModeVehicleId2totalDistance = new HashMap<>();
	private final Map<Id<Person>,Map<Integer,Double>> personId2tripNumber2previousEnterVehicleTime = new HashMap<>();

	private final Set<Id<Person>> ptDrivers = new HashSet<>();
	private final Set<Id<Vehicle>> ptVehicles = new HashSet<>();
	private final Set<Id<Person>> taxiOrDrtDrivers = new HashSet<>();

	// analysis information to be stored
	private final Map<Id<Person>,Map<Integer,String>> personId2tripNumber2tripModes = new HashMap<>();
	private final Map<Id<Person>,Map<Integer,String>> personId2tripNumber2tripMainMode = new HashMap<>();
	private final Map<Id<Person>,Map<Integer,Double>> personId2tripNumber2departureTime = new HashMap<>();
	private final Map<Id<Person>,Map<Integer,Coord>> personId2tripNumber2originCoord = new HashMap<>();
	private final Map<Id<Person>,Map<Integer,Double>> personId2tripNumber2arrivalTime = new HashMap<>();
	private final Map<Id<Person>,Map<Integer,Coord>> personId2tripNumber2destinationCoord = new HashMap<>();
	private final Map<Id<Person>,Map<Integer,Double>> personId2tripNumber2travelTime = new HashMap<>();
	private final Map<Id<Person>,Map<Integer,Double>> personId2tripNumber2inVehicleTime = new HashMap<>();
	private final Map<Id<Person>,Map<Integer,Double>> personId2tripNumber2tripDistance = new HashMap<>();
	private final Map<Id<Person>,Map<Integer,Double>> personId2tripNumber2tripBeelineDistance = new HashMap<>();

	private final Map<Id<Person>,Map<Integer,Double>> personId2tripNumber2amount = new HashMap<>();
	private final Map<Id<Person>,Map<Integer,Double>> personId2tripNumber2payment = new HashMap<>();
	private final Map<Id<Person>,Map<Integer,Double>> personId2tripNumber2reward = new HashMap<>();

	private final Map<Id<Person>,Map<Integer,Boolean>> personId2tripNumber2stuckAbort = new HashMap<>();
	private final Map<Id<Person>,Integer> personId2stuckAndAbortEvents = new HashMap<>();	

	private final Map<Id<Person>, Double> personId2totalamounts = new HashMap<>();
	private final Map<Id<Person>, Double> personId2totalpayments = new HashMap<>();
	private final Map<Id<Person>, Double> personId2totalrewards = new HashMap<>();
	
	private double totalAmounts = 0.;
	private double totalPayments = 0.;
	private double totalRewards = 0.;
	
	private int warnCnt0 = 0;
	private int warnCnt1 = 0;
	private int warnCnt2 = 0;
	private int warnCnt3 = 0;
	private int warnCnt4 = 0;
	private int stuckPersonWarnCounter;
	private int stuckPersonWarnCounter2;

	
	public void setScenario(Scenario scenario) {
		this.scenario = scenario;
	}

	@Override
	public void reset(int iteration) {
		personId2currentTripNumber.clear();
		personId2tripNumber2departureTime.clear();
		personId2tripNumber2arrivalTime.clear();
		personId2tripNumber2tripDistance.clear();
		personId2tripNumber2tripBeelineDistance.clear();
		personId2tripNumber2travelTime.clear();
		personId2tripNumber2payment.clear(); // negative amounts
		personId2tripNumber2reward.clear(); // positive amounts
		personId2tripNumber2amount.clear(); // all amounts
		personId2tripNumber2stuckAbort.clear();
		personId2tripNumber2tripModes.clear();
		personId2tripNumber2tripMainMode.clear();
		personId2tripNumber2previousEnterVehicleTime.clear();
		personId2tripNumber2inVehicleTime.clear();
		personId2totalpayments.clear();
		personId2totalrewards.clear();
		personId2totalamounts.clear();
		totalPayments = 0.;
		totalRewards = 0.;
		totalAmounts = 0.;
		personId2distanceEnterValue.clear();
		ptDrivers.clear();
		ptVehicles.clear();
		taxiOrDrtDrivers.clear();
		networkModeVehicleId2totalDistance.clear();
		personId2stuckAndAbortEvents.clear();
		personId2tripNumber2originCoord.clear();
		personId2tripNumber2destinationCoord.clear();
	}
	
	@Override
	public void handleEvent(PersonMoneyEvent event) {	
		
		if (event.getAmount() > 0.) {
			if (warnCnt1 <= 5) {
				log.warn("A person money event has a positive amount. This is interpreted as an income. For costs the amount should be negative! " + event.toString());
				if (warnCnt1 == 5) {
					log.warn("Further warnings of this type are not printed out.");
				}
				warnCnt1++;
			}
		}
		
		if (event.getAmount() < 0.) {
			totalPayments += ( -1. * event.getAmount() );
		} else if (event.getAmount() > 0.) {
			totalRewards += event.getAmount();
		}
		totalAmounts += event.getAmount();

		// trip
		
		if (this.taxiOrDrtDrivers.contains(event.getPersonId()) || this.ptDrivers.contains(event.getPersonId())) {
			if (warnCnt0 <= 5) {
				log.warn("A person money event is thrown for a public tranist driver or taxi driver: " + event.toString());
				if (warnCnt0 == 5) {
					log.warn("Further warnings of this type are not printed out.");
				}
				warnCnt0++;
			}
			
		} else {
			
			if (this.personId2currentTripNumber.get(event.getPersonId()) == null) {
				throw new RuntimeException("No trip number for " + event.getPersonId() + " at time " + event.getTime());
			}
			
			int tripNumber = this.personId2currentTripNumber.get(event.getPersonId());
			
			if (event.getAmount() < 0.) {
				double paymentBefore = personId2tripNumber2payment.get(event.getPersonId()).get(tripNumber);
				double updatedPayment = paymentBefore + (-1. * event.getAmount());
				Map<Integer,Double> tripNumber2payment = personId2tripNumber2payment.get(event.getPersonId());
				tripNumber2payment.put(tripNumber, updatedPayment);
				personId2tripNumber2payment.put(event.getPersonId(), tripNumber2payment);
			} else if (event.getAmount() > 0.) {
				double valueBefore = personId2tripNumber2reward.get(event.getPersonId()).get(tripNumber);
				double updatedValue = valueBefore + event.getAmount();
				Map<Integer,Double> tripNumber2value = personId2tripNumber2reward.get(event.getPersonId());
				tripNumber2value.put(tripNumber, updatedValue);
				personId2tripNumber2reward.put(event.getPersonId(), tripNumber2value);
			}
			double valueBefore = personId2tripNumber2amount.get(event.getPersonId()).get(tripNumber);
			double updatedValue = valueBefore + event.getAmount();
			Map<Integer,Double> tripNumber2value = personId2tripNumber2amount.get(event.getPersonId());
			tripNumber2value.put(tripNumber, updatedValue);
			personId2tripNumber2amount.put(event.getPersonId(), tripNumber2value);
			
			// person
			
			if (event.getAmount() < 0.) {
				if (this.personId2totalpayments.get(event.getPersonId()) == null) {
					this.personId2totalpayments.put(event.getPersonId(), event.getAmount() * (-1.));

				} else {
					double amountSoFar = this.personId2totalpayments.get(event.getPersonId());
					double amountNew = amountSoFar + ( event.getAmount() * (-1.) );
					this.personId2totalpayments.put(event.getPersonId(), amountNew);
				}
				
			} else if (event.getAmount() > 0.) {
				if (this.personId2totalrewards.get(event.getPersonId()) == null) {
					this.personId2totalrewards.put(event.getPersonId(), event.getAmount());

				} else {
					double amountSoFar = this.personId2totalrewards.get(event.getPersonId());
					double amountNew = amountSoFar + event.getAmount();
					this.personId2totalrewards.put(event.getPersonId(), amountNew);
				}
			}
			
			if (this.personId2totalamounts.get(event.getPersonId()) == null) {
				this.personId2totalamounts.put(event.getPersonId(), event.getAmount());

			} else {
				double amountSoFar = this.personId2totalamounts.get(event.getPersonId());
				double amountNew = amountSoFar + event.getAmount();
				this.personId2totalamounts.put(event.getPersonId(), amountNew);
			}
		}
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
				
		double linkLength = this.scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
		
		if (networkModeVehicleId2totalDistance.get(event.getVehicleId()) != null) {
			networkModeVehicleId2totalDistance.put(event.getVehicleId(), networkModeVehicleId2totalDistance.get(event.getVehicleId()) + linkLength);

		} else {
			if (warnCnt2 <= 5) {
				log.warn("Link enter event of a vehicle with a not considered mode: " + event.toString());
				if (warnCnt2 == 5) {
					log.warn("Further warnings of this type are not printed out.");
				}
				warnCnt2++;
			}
		}
	}
	
	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		ptDrivers.add(event.getDriverId());
		ptVehicles.add(event.getVehicleId());
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		
		if (event.getActType().equals(VrpAgentLogic.BEFORE_SCHEDULE_ACTIVITY_TYPE)) {
			this.taxiOrDrtDrivers.add(event.getPersonId());
		}
		
		if (ptDrivers.contains(event.getPersonId()) || taxiOrDrtDrivers.contains(event.getPersonId())){
			// activities by pt or taxi drivers are not considered
			
		} else {
			if (event.getActType().contains(helpActivitySubString)) {
				// pseudo activities are excluded

			} else {
				// a "real" activity

				if (personId2currentTripNumber.get(event.getPersonId()) != null) {
					// the following trip is at least the person's second trip
					personId2currentTripNumber.put(event.getPersonId(), personId2currentTripNumber.get(event.getPersonId()) + 1);

					Map<Integer, Double> tripNumber2departureTime = personId2tripNumber2departureTime.get(event.getPersonId());
					tripNumber2departureTime.put(personId2currentTripNumber.get(event.getPersonId()), event.getTime());
					personId2tripNumber2departureTime.put(event.getPersonId(), tripNumber2departureTime);
					
					Map<Integer,Coord> tripNumber2originCoord = personId2tripNumber2originCoord.get(event.getPersonId());
					Coord coordOrigin = null;
					if (event.getFacilityId() != null
							&& this.scenario.getActivityFacilities().getFacilities() != null
							&& this.scenario.getActivityFacilities().getFacilities().get(event.getFacilityId()) != null
							&& this.scenario.getActivityFacilities().getFacilities().get(event.getFacilityId()).getCoord() != null) {
						coordOrigin = this.scenario.getActivityFacilities().getFacilities().get(event.getFacilityId()).getCoord();
					} else {
						// the "old" way as backup
						coordOrigin = this.scenario.getNetwork().getLinks().get(event.getLinkId()).getCoord();
					}
					tripNumber2originCoord.put(personId2currentTripNumber.get(event.getPersonId()), coordOrigin);
					personId2tripNumber2originCoord.put(event.getPersonId(), tripNumber2originCoord);
					
					Map<Integer,Coord> tripNumber2destinationCoord = personId2tripNumber2destinationCoord.get(event.getPersonId());
					tripNumber2destinationCoord.put(personId2currentTripNumber.get(event.getPersonId()), null);
					personId2tripNumber2destinationCoord.put(event.getPersonId(), tripNumber2destinationCoord);
					
					Map<Integer,Double> tripNumber2tripDistance = personId2tripNumber2tripDistance.get(event.getPersonId());
					tripNumber2tripDistance.put(personId2currentTripNumber.get(event.getPersonId()), 0.0);
					personId2tripNumber2tripDistance.put(event.getPersonId(), tripNumber2tripDistance);
					
					Map<Integer,Double> tripNumber2tripBeelineDistance = personId2tripNumber2tripBeelineDistance.get(event.getPersonId());
					tripNumber2tripBeelineDistance.put(personId2currentTripNumber.get(event.getPersonId()), 0.0);
					personId2tripNumber2tripBeelineDistance.put(event.getPersonId(), tripNumber2tripBeelineDistance);
						
					Map<Integer,Double> tripNumber2payment = personId2tripNumber2payment.get(event.getPersonId());
					tripNumber2payment.put(personId2currentTripNumber.get(event.getPersonId()), 0.0);
					personId2tripNumber2payment.put(event.getPersonId(), tripNumber2payment);
					
					Map<Integer,Double> tripNumber2reward = personId2tripNumber2reward.get(event.getPersonId());
					tripNumber2reward.put(personId2currentTripNumber.get(event.getPersonId()), 0.0);
					personId2tripNumber2reward.put(event.getPersonId(), tripNumber2reward);
					
					Map<Integer,Double> tripNumber2amount = personId2tripNumber2amount.get(event.getPersonId());
					tripNumber2amount.put(personId2currentTripNumber.get(event.getPersonId()), 0.0);
					personId2tripNumber2amount.put(event.getPersonId(), tripNumber2amount);
			
				} else {
					// the following trip is the person's first trip
					personId2currentTripNumber.put(event.getPersonId(), 1);
					
					Map<Integer,Double> tripNumber2departureTime = new HashMap<>();
					tripNumber2departureTime.put(1, event.getTime());
					personId2tripNumber2departureTime.put(event.getPersonId(), tripNumber2departureTime);
					
					Map<Integer,Coord> tripNumber2originCoord = new HashMap<>();
					Coord coordOrigin = null;
					if (event.getFacilityId() != null
							&& this.scenario.getActivityFacilities().getFacilities() != null
							&& this.scenario.getActivityFacilities().getFacilities().get(event.getFacilityId()) != null
							&& this.scenario.getActivityFacilities().getFacilities().get(event.getFacilityId()).getCoord() != null) {
						coordOrigin = this.scenario.getActivityFacilities().getFacilities().get(event.getFacilityId()).getCoord();
					} else {
						// the "old" way as backup
						coordOrigin = this.scenario.getNetwork().getLinks().get(event.getLinkId()).getCoord();
					}
					tripNumber2originCoord.put(1, coordOrigin);
					personId2tripNumber2originCoord.put(event.getPersonId(), tripNumber2originCoord);
					
					Map<Integer,Double> tripNumber2tripDistance = new HashMap<>();
					tripNumber2tripDistance.put(1, 0.0);
					personId2tripNumber2tripDistance.put(event.getPersonId(), tripNumber2tripDistance);
					
					Map<Integer,Double> tripNumber2tripBeelineDistance = new HashMap<>();
					tripNumber2tripBeelineDistance.put(1, 0.0);
					personId2tripNumber2tripBeelineDistance.put(event.getPersonId(), tripNumber2tripBeelineDistance);
					
					Map<Integer,Coord> tripNumber2destinationCoord = new HashMap<>();
					tripNumber2destinationCoord.put(1, null);
					personId2tripNumber2destinationCoord.put(event.getPersonId(), tripNumber2destinationCoord);
					
					Map<Integer,Double> tripNumber2payment = new HashMap<>();
					tripNumber2payment.put(1, 0.0);
					personId2tripNumber2payment.put(event.getPersonId(), tripNumber2payment);
					
					Map<Integer,Double> tripNumber2reward = new HashMap<>();
					tripNumber2reward.put(1, 0.0);
					personId2tripNumber2reward.put(event.getPersonId(), tripNumber2reward);
					
					Map<Integer,Double> tripNumber2amount = new HashMap<>();
					tripNumber2amount.put(1, 0.0);
					personId2tripNumber2amount.put(event.getPersonId(), tripNumber2amount);
				}
			}	
		}
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (ptDrivers.contains(event.getPersonId()) || taxiOrDrtDrivers.contains(event.getPersonId())){
			// no normal person
			
		} else {
			
			if (personId2tripNumber2tripModes.get(event.getPersonId()) == null) {				
				// the person's first trip
				
				Map<Integer,String> tripNumber2tripMode = new HashMap<>();
				tripNumber2tripMode.put(1, event.getLegMode());
				personId2tripNumber2tripModes.put(event.getPersonId(), tripNumber2tripMode);
				
				Map<Integer,String> tripNumber2tripMainMode = new HashMap<>();
				tripNumber2tripMainMode.put(1, event.getLegMode());
				personId2tripNumber2tripMainMode.put(event.getPersonId(), tripNumber2tripMainMode);
			
			} else {	
				// at least the person's second trip
				
				int tripNumber = personId2currentTripNumber.get(event.getPersonId());

				Map<Integer,String> tripNumber2tripModes = personId2tripNumber2tripModes.get(event.getPersonId());
				Map<Integer,String> tripNumber2tripMainMode = personId2tripNumber2tripMainMode.get(event.getPersonId());
				
				if (tripNumber2tripModes.get(tripNumber) == null) {
//					
					tripNumber2tripModes.put(tripNumber, event.getLegMode());
					personId2tripNumber2tripModes.put(event.getPersonId(), tripNumber2tripModes);
					
					tripNumber2tripMainMode.put(tripNumber, event.getLegMode());
					personId2tripNumber2tripMainMode.put(event.getPersonId(), tripNumber2tripMainMode);
				
				} else {
					
					String tripModes = tripNumber2tripModes.get(tripNumber);
					tripModes = tripModes + "-" + event.getLegMode();
					
					// add the leg mode to the other modes stored for this trip
					tripNumber2tripModes.put(tripNumber, tripModes);
					personId2tripNumber2tripModes.put(event.getPersonId(), tripNumber2tripModes);
						
					// update the current mainMode candidate
					String tripMainMode = getMainMode(tripModes);
					tripNumber2tripMainMode.put(tripNumber, tripMainMode);
					personId2tripNumber2tripMainMode.put(event.getPersonId(), tripNumber2tripMainMode);
				}
				
			}
		}		
	}
	
	String getMainMode(String modes) {
						
		List<String> modesList = new ArrayList<>();
		String[] splitModes = modes.split("-");
		Collections.addAll(modesList, splitModes);
		
		if (modesList.size() == 0) return modesList.get(0);
		
		List<Leg> legs = new ArrayList<>();
		for (String mode : modesList) {
			legs.add(PopulationUtils.createLeg(mode));
		}	
		return mainModeIdentifier.identifyMainMode(legs);		
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
	
		if (ptDrivers.contains(event.getPersonId()) || taxiOrDrtDrivers.contains(event.getPersonId())) {
			// no normal person
			
		} else {
			int tripNumber = personId2currentTripNumber.get(event.getPersonId());
						
			Map<Integer, Double> tripNr2inVehTime;
			if (this.personId2tripNumber2inVehicleTime.get(event.getPersonId())  != null ) {
				tripNr2inVehTime = this.personId2tripNumber2inVehicleTime.get(event.getPersonId());
			} else {
				tripNr2inVehTime = new HashMap<>();
			}
			double inVehTime = event.getTime() - this.personId2tripNumber2previousEnterVehicleTime.get(event.getPersonId()).get(tripNumber);
			if (tripNr2inVehTime.get(tripNumber) == null) {
				tripNr2inVehTime.put(tripNumber, inVehTime);
			} else {
				// several pt legs...
				double inVehTimeSum = tripNr2inVehTime.get(tripNumber) + inVehTime;
				tripNr2inVehTime.put(tripNumber, inVehTimeSum);
			}
			this.personId2tripNumber2inVehicleTime.put(event.getPersonId(), tripNr2inVehTime);
			
			// distance	
			double distanceTravelled = (networkModeVehicleId2totalDistance.get(event.getVehicleId()) - personId2distanceEnterValue.get(event.getPersonId()));			
			
			Map<Integer,Double> tripNumber2distance = personId2tripNumber2tripDistance.get(event.getPersonId());
			tripNumber2distance.put(tripNumber, tripNumber2distance.get(tripNumber) + distanceTravelled);
			
			personId2distanceEnterValue.remove(event.getPersonId());
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
				
		// start vehicle kilometers tracking
		networkModeVehicleId2totalDistance.putIfAbsent(event.getVehicleId(), 0.);
		
		// ###################################################
		
		if (ptDrivers.contains(event.getPersonId()) || taxiOrDrtDrivers.contains(event.getPersonId())) {
			// no normal person
		} else {
			
			int tripNumber = personId2currentTripNumber.get(event.getPersonId());

			// enter time
			
			Map<Integer, Double> tripNr2enterTime;
			if (this.personId2tripNumber2previousEnterVehicleTime.get(event.getPersonId())  != null ) {
				tripNr2enterTime = this.personId2tripNumber2previousEnterVehicleTime.get(event.getPersonId());
			} else {
				tripNr2enterTime = new HashMap<>();
			}
			tripNr2enterTime.put(tripNumber, event.getTime());
			this.personId2tripNumber2previousEnterVehicleTime.put(event.getPersonId(), tripNr2enterTime);
			
			// distance
			personId2distanceEnterValue.put(event.getPersonId(), networkModeVehicleId2totalDistance.get(event.getVehicleId()));
			
			
		}		
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
			
		if (ptDrivers.contains(event.getPersonId()) || taxiOrDrtDrivers.contains(event.getPersonId())) {
			// no normal person
			
		} else {
			int currentTripNumber = this.personId2currentTripNumber.get(event.getPersonId());
			
			// travel time
			Map<Integer, Double> tripNumber2travelTime;
			if (this.personId2tripNumber2travelTime.get(event.getPersonId())  != null ) {
				tripNumber2travelTime = this.personId2tripNumber2travelTime.get(event.getPersonId());

			} else {
				tripNumber2travelTime = new HashMap<>();
			}
			tripNumber2travelTime.put(currentTripNumber, event.getTime() - this.personId2tripNumber2departureTime.get(event.getPersonId()).get(currentTripNumber));
			this.personId2tripNumber2travelTime.put(event.getPersonId(), tripNumber2travelTime);
			
			 // arrival time
			Map<Integer, Double> tripNumber2arrivalTime;
			if (this.personId2tripNumber2arrivalTime.get(event.getPersonId())  != null ) {
				tripNumber2arrivalTime = this.personId2tripNumber2arrivalTime.get(event.getPersonId());

			} else {
				tripNumber2arrivalTime = new HashMap<>();
			}
			tripNumber2arrivalTime.put(currentTripNumber, event.getTime());
			personId2tripNumber2arrivalTime.put(event.getPersonId(), tripNumber2arrivalTime);
		}
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		double endTime;
		if (this.scenario.getConfig().qsim().getEndTime().isUndefined()) {
			endTime = 30 * 3600.;
			if (this.stuckPersonWarnCounter <= 5) {
				log.warn("Trying to deal with person stuck events. Assuming " + endTime + " to be the simulation end time. If you are using a different value, set the qsim end time in your run script, e.g. config.qsim().setEndTime(24 * 3600.);");
				if (this.stuckPersonWarnCounter == 5) {
					log.warn("Further warnings of this type will not be printed.");
				}
				this.stuckPersonWarnCounter++;
			}
			
		} else {
			endTime = this.scenario.getConfig().qsim().getEndTime().seconds();
		}
		
		boolean removeStuckVehicles = false;
		if (this.scenario.getConfig().qsim().isRemoveStuckVehicles() == false) {
			if (this.stuckPersonWarnCounter2 <= 5) {
				log.warn("Trying to deal with person stuck events. Assuming stucking vehicles not to be removed. If you are removing stucking vehicles, set the qsim parameter in your run script accordingly.");
				if (this.stuckPersonWarnCounter2 == 5) {
					log.warn("Further warnings of this type will not be printed out.");
				}
				this.stuckPersonWarnCounter2++;
			}
		} else {
			removeStuckVehicles = true;
		}
		
		if (removeStuckVehicles || event.getTime() == endTime) { // the stuck event is thrown at the end of the simulation
						
			if (this.personId2currentTripNumber.get(event.getPersonId()) != null ) {
				int currentTripNumber = this.personId2currentTripNumber.get(event.getPersonId());	
				
				// travel time

				Map<Integer, Double> tripNumber2travelTime;
				if (this.personId2tripNumber2travelTime.get(event.getPersonId()) != null) {
					tripNumber2travelTime = this.personId2tripNumber2travelTime.get(event.getPersonId());

				} else {
					tripNumber2travelTime = new HashMap<>();
				}
										
				double traveltime;
				if (event.getTime() == endTime) {
					traveltime = event.getTime() - this.personId2tripNumber2departureTime.get(event.getPersonId()).get(currentTripNumber);
					if (warnCnt3 <= 5) {
						log.warn("The stuck event is thrown at the end of the simulation. Computing the travel time for this trip as follows: simulation end time - trip departure time");
						log.warn("The travel time is set to " + traveltime);
						if (warnCnt3 == 5) {
							log.warn("Further warnings of this type are not printed out.");
						}
					}
					warnCnt3++;
				} else {
					traveltime = Double.POSITIVE_INFINITY;

					if (warnCnt4 <= 5) {
						log.warn("Stucking vehicle will be removed and teleported to the destination activity. The travel time cannot be interpreted.");
						log.warn("The travel time is set to " + traveltime);
						if (warnCnt4 == 5) {
							log.warn("Further warnings of this type are not printed out.");
						}
					}
					warnCnt4++;
				}
				
				tripNumber2travelTime.put(currentTripNumber, traveltime);
				this.personId2tripNumber2travelTime.put(event.getPersonId(), tripNumber2travelTime);
				
				// stuck and abort 
				
				Map<Integer, Boolean> tripNr2StuckAbort;
				if (this.personId2tripNumber2stuckAbort.get(event.getPersonId()) != null) {
					tripNr2StuckAbort = this.personId2tripNumber2stuckAbort.get(event.getPersonId());
				} else {
					tripNr2StuckAbort = new HashMap<>();
				}
				
				tripNr2StuckAbort.put(currentTripNumber, true);
				this.personId2tripNumber2stuckAbort.put(event.getPersonId(), tripNr2StuckAbort);
				
				if (this.personId2stuckAndAbortEvents.get(event.getPersonId()) == null) {
					this.personId2stuckAndAbortEvents.put(event.getPersonId(), 1);
				} else {
					int updatedValue = this.personId2stuckAndAbortEvents.get(event.getPersonId()) + 1;
					this.personId2stuckAndAbortEvents.put(event.getPersonId(), updatedValue);
				}
			
			} else {
				// the agent has not yet departed
			}
					
		} else {
				// The agent should arrive and a travel time can be calculated.
		}
	}
	
	public Map<Id<Person>, Map<Integer, Boolean>> getPersonId2tripNumber2stuckAbort() {
		return personId2tripNumber2stuckAbort;
	}

	public Map<Id<Person>, Integer> getPersonId2currentTripNumber() {
		return personId2currentTripNumber;
	}

	public Map<Id<Person>, Double> getPersonId2distanceEnterValue() {
		return personId2distanceEnterValue;
	}

	public Map<Id<Person>, Map<Integer, String>> getPersonId2tripNumber2tripMainMode() {
		return personId2tripNumber2tripMainMode;
	}
	
	public Map<Id<Person>, Map<Integer, String>> getPersonId2tripNumber2tripModes() {
		return personId2tripNumber2tripModes;
	}
	
	public Map<Id<Person>, Map<Integer, Double>> getPersonId2tripNumber2departureTime() {
		return personId2tripNumber2departureTime;
	}

	public Map<Id<Person>, Map<Integer, Double>> getPersonId2tripNumber2arrivalTime() {
		return personId2tripNumber2arrivalTime;
	}

	public Map<Id<Person>, Map<Integer, Double>> getPersonId2tripNumber2travelTime() {
		return personId2tripNumber2travelTime;
	}

	public Map<Id<Person>, Map<Integer, Double>> getPersonId2tripNumber2tripDistance() {
		return personId2tripNumber2tripDistance;
	}
	
	public Map<Id<Person>, Map<Integer, Double>> getPersonId2tripNumber2tripBeelineDistance() {
		return personId2tripNumber2tripBeelineDistance;
	}

	int n = 0;
	public Map<Id<Person>, Map<Integer, Double>> getPersonId2tripNumber2payment() {
		
		if (n == 0) {
			log.warn("No guarantee that monetary payments are ascribed to the right trip (money events, i.e. tolls, may be charged after the person has started the next trip).");
			log.warn("Additional warnings of this type are suppressed.");
		}
		n++;

		return personId2tripNumber2payment;
	}
	
	int nn = 0;
	public Map<Id<Person>, Map<Integer, Double>> getPersonId2tripNumber2reward() {
		
		if (nn == 0) {
			log.warn("No guarantee that monetary rewards are ascribed to the right trip (money events, i.e. tolls, may be charged after the person has started the next trip).");
			log.warn("Additional warnings of this type are suppressed.");
		}
		nn++;

		return personId2tripNumber2reward;
	}

	public double getTotalPayments() {
		return totalPayments;
	}
	
	public double getTotalAmounts() {
		return totalAmounts;
	}

	public double getTotalRewards() {
		return totalRewards;
	}

	public double getTotalPaymentsByPersons() {
		double totalPaymentsByPersons = 0.;
		for (Id<Person> id : this.personId2totalpayments.keySet()) {
			totalPaymentsByPersons += this.personId2totalpayments.get(id);
		}
		return totalPaymentsByPersons;
	}
	
	public double getTotalRewardsByPersons() {
		double totalRewardsByPersons = 0.;
		for (Id<Person> id : this.personId2totalrewards.keySet()) {
			totalRewardsByPersons += this.personId2totalrewards.get(id);
		}
		return totalRewardsByPersons;
	}
	
	public Set<Id<Person>> getTaxiDrivers() {
		return taxiOrDrtDrivers;
	}

	public Scenario getScenario() {
		return scenario;
	}

	public Map<Id<Person>, Map<Integer, Double>> getPersonId2tripNumber2inVehicleTime() {
		return personId2tripNumber2inVehicleTime;
	}

	public Set<Id<Person>> getPtDrivers() {
		return ptDrivers;
	}

	public Double getTotalTravelTimeByPersons() {
		double totalTravelTimeByPersons = 0.;
		for (Id<Person> id : this.personId2tripNumber2travelTime.keySet()) {
			for (Integer tripNr : this.personId2tripNumber2travelTime.get(id).keySet()) {
				totalTravelTimeByPersons += this.personId2tripNumber2travelTime.get(id).get(tripNr);
			}
		}
		return totalTravelTimeByPersons;
	}

	public Map<Id<Vehicle>, Double> getCarVehicleId2totalDistance() {
		return networkModeVehicleId2totalDistance;
	}

	public Map<Id<Person>, Integer> getPersonId2stuckAndAbortEvents() {
		return personId2stuckAndAbortEvents;
	}
	
	public Map<Id<Person>, Map<Integer, Coord>> getPersonId2tripNumber2originCoord() {
		return personId2tripNumber2originCoord;
	}

	public Map<Id<Person>, Map<Integer, Coord>> getPersonId2tripNumber2destinationCoord() {
		return personId2tripNumber2destinationCoord;
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (ptDrivers.contains(event.getPersonId()) || taxiOrDrtDrivers.contains(event.getPersonId())){
			// activities by pt or taxi drivers are not considered
			
		} else {
			if (event.getActType().contains(helpActivitySubString)) {
				// pseudo activities are excluded

			} else {
				// a "real" activity
				if (personId2currentTripNumber.get(event.getPersonId()) == null) {
					throw new RuntimeException("This should not happen. Activity start event without activity end event?!");
				}
				int tripNumber = personId2currentTripNumber.get(event.getPersonId());

				Coord coordDestination = null;
				if (event.getFacilityId() != null
						&& this.scenario.getActivityFacilities().getFacilities() != null
						&& this.scenario.getActivityFacilities().getFacilities().get(event.getFacilityId()) != null
						&& this.scenario.getActivityFacilities().getFacilities().get(event.getFacilityId()).getCoord() != null) {
					coordDestination = this.scenario.getActivityFacilities().getFacilities().get(event.getFacilityId()).getCoord();
				} else {
					// the "old" way as backup
					coordDestination = this.scenario.getNetwork().getLinks().get(event.getLinkId()).getCoord();
				}

				double beelineDistance = NetworkUtils.getEuclideanDistance(this.personId2tripNumber2originCoord.get(event.getPersonId()).get(tripNumber), coordDestination);
				Map<Integer, Double> tripNumber2beelinedistance = personId2tripNumber2tripBeelineDistance.get(event.getPersonId());
				tripNumber2beelinedistance.put(tripNumber, beelineDistance);

				Map<Integer, Coord> tripNumber2destinationCoord = personId2tripNumber2destinationCoord.get(event.getPersonId());
				tripNumber2destinationCoord.put(tripNumber, coordDestination);
			}	
		}
	}
	
}
