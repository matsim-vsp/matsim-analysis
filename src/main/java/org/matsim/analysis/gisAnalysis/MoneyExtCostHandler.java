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

package org.matsim.analysis.gisAnalysis;


import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.population.Person;

/**
 * @author ikaddoura
 *
 */
public class MoneyExtCostHandler implements  PersonMoneyEventHandler {
	private static final Logger log = LogManager.getLogger(MoneyExtCostHandler.class);

	private final Map<Id<Person>, Double> personId2toll = new HashMap<>();
	
	@Override
	public void reset(int iteration) {
		this.personId2toll.clear();
	}

	@Override
	public void handleEvent(PersonMoneyEvent event) {
		
		if (personId2toll.get(event.getPersonId()) == null) {
			this.personId2toll.put(event.getPersonId(), -1. * event.getAmount());
		} else {
			double tollSoFar = this.personId2toll.get(event.getPersonId());
			this.personId2toll.put( event.getPersonId(), tollSoFar + (-1. * event.getAmount()) );
		}
	}

	public Map<Id<Person>, Double> getPersonId2toll() {
		if (personId2toll.isEmpty()) log.info("Returning an empty map; no person money events.");
		return personId2toll;
	}

}
