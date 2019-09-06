/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.analysis.modalSplitUserType;

import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.MainModeIdentifier;

/**
 * A main mode identifier similar to the one we use in the matsim-berlin repository.
 * Note: This main mode identifier should only be used for analysis purposes. It returns a more precise description of the actually used mode in case no transit route was found.
 * 
 * @author ikaddoura based on thibaut
 */
public final class AnalysisMainModeIdentifier implements MainModeIdentifier {
	@Override
	public String identifyMainMode( final List<? extends PlanElement> tripElements) {
		
		if (tripElements.get(0) instanceof Activity) {
			throw new RuntimeException("This main mode identifier processes trip elements only. The first plan element must not be an instance of Activity. " + tripElements);
		}
		
		if (tripElements.size() == 1) {
			String mode = ((Leg) tripElements.get( 0 )).getMode();
			return mode;
		}
		
		for ( PlanElement pe : tripElements ) {
			if ( pe instanceof Leg ) {
				Leg leg = (Leg) pe ;
				String mode = leg.getMode() ;
				if ( !mode.contains( TransportMode.non_network_walk ) &&
						!mode.contains( "access_walk") && // for backward compatibility
						!mode.contains( "egress_walk" ) && // for backward compatibility
						!mode.contains( "transit_walk" ) ) {
					return mode ;
				}
			}
		}
		
		throw new RuntimeException( "could not identify main mode "+ tripElements) ;
		
	}
}
