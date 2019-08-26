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

package org.matsim.analysis;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

/**
* @author ikaddoura
*/

public class TripAnalysisFilter implements TripFilter {
	
	private static final Logger log = Logger.getLogger(TripAnalysisFilter.class);	
	private boolean dataPreprocessed = false;
	private final Map<String, Geometry> zoneFeatures = new HashMap<>();
	
	private String zoneFile = null;
	
	@Override
	public boolean considerTrip(Coord origin, Coord destination) {
		
		if (dataPreprocessed == false) {
			throw new RuntimeException("Can't use the filter without pre-processing. Aborting...");
		}
		
		// assuming the same CRS!
		boolean originWithProvidedGeometry = false;
		for (Geometry geometry : zoneFeatures.values()) {
			Point point = MGC.coord2Point(origin);
			if (point.within(geometry)) {
				originWithProvidedGeometry = true;
				break;
			}
		}
		boolean destinationWithProvidedGeometry = false;
		for (Geometry geometry : zoneFeatures.values()) {
			Point point = MGC.coord2Point(destination);
			if (point.within(geometry)) {
				destinationWithProvidedGeometry = true;
				break;
			}
		}
		if (originWithProvidedGeometry && destinationWithProvidedGeometry == false) return false;
		
		return true;
	}

	@Override
	public String toFileName() {
		String fileName = "_TRIPFILTER";
		
		boolean atLeastOneFilterApplied = false;
		
		if (zoneFile != null) {
			atLeastOneFilterApplied = true;
			fileName = fileName + "_trip-within-provided-zone-file";	
		}
		
		if (atLeastOneFilterApplied == false) {
			fileName = "";
		}
	
		return fileName;
	}

	public String getZoneFile() {
		return zoneFile;
	}

	public void setZoneFile(String zoneFile) {
		this.zoneFile = zoneFile;
	}
	
	public void preProcess(Scenario scenario) {
		this.dataPreprocessed = true;
	    	    
		if (scenario != null &&  zoneFile != null) {					
			log.info("Reading shape file...");
			Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(zoneFile);
			int counter = 0;
			for (SimpleFeature feature : features) {
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                zoneFeatures.put(String.valueOf(counter), geometry);
                counter++;
            }
			log.info("Reading shape file... Done.");	
		}
		
	}

}

