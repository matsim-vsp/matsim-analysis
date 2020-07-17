/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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
import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;

/**
* @author ikaddoura
*/

public class VehicleAnalysisFilter implements VehicleFilter {
	private static final Logger log = Logger.getLogger(VehicleAnalysisFilter.class);	

	private String substring;
	private StringComparison stringComparison;
	private String filterName;
	
	public enum StringComparison {StartsWith, Contains, EndsWith};
	
	public VehicleAnalysisFilter(String name, String substring, StringComparison stringComparison) {
		this.filterName = name;
		this.substring = substring;
		this.stringComparison = stringComparison;
	}

	@Override
	public boolean considerVehicle(Id<Vehicle> vehicleId) {
		if (substring.equals("")) return true;
		else {
			switch(stringComparison) {
			case StartsWith: if (vehicleId.toString().startsWith(substring)) return true;
			case Contains: if (vehicleId.toString().contains(substring)) return true;
			case EndsWith: if (vehicleId.toString().endsWith(substring)) return true;
			default: log.warn("Unknown string comparison approach.");
		}
		return false;
		}
	}

	@Override
	public String toFileName() {	
		String fileName = "_VEHICLEFILTER-" + filterName;
		return fileName;
	}

}

