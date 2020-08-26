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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.router.TripStructureUtils;
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
	private String zoneCRS = null;
	private final String filterName;
	private double buffer = 0.;
	private TripConsiderType tripConsiderType = TripConsiderType.OriginAndDestination;
	private int originDestinationNullCounter;
	
	public enum TripConsiderType { OriginAndDestination, OriginOrDestination }
	
	public TripAnalysisFilter(String filterName) {
		this.filterName = filterName;
	}
	
	@Deprecated
	public TripAnalysisFilter() {
		this.filterName = "";
	}

	@Override
	public boolean considerTrip(TripStructureUtils.Trip trip, Scenario scenario) {
		Coord origin = getCoordFromActivity(trip.getOriginActivity(), scenario);
		Coord destination = getCoordFromActivity(trip.getDestinationActivity(), scenario);
		return considerTrip(origin, destination);
	}

	@Override
	public boolean considerTrip(Coord origin, Coord destination) {
		
		if (dataPreprocessed == false) {
			throw new RuntimeException("Can't use the filter without pre-processing. Aborting...");
		}
		
		if (origin == null || destination == null) {
			if (this.originDestinationNullCounter <= 5) {
				log.warn("Origin or destination null. Probably a stucking agent. Can't interpret this trip. Origin: " + origin + "--> Destination: " +  destination);
				if (this.originDestinationNullCounter == 5) {
					log.warn("Further warnings of this type will not be printed out.");
				}
				this.originDestinationNullCounter++;
			}
			return false;
		}
		
		if (zoneFeatures.size() == 0) {
			// no zoneFeatures loaded --> consider all trips
			return true;
		}
		
		// assuming the same CRS!
		boolean originWithinProvidedGeometry = false;
		Point originPoint = MGC.coord2Point(origin);
		for (Geometry geometry : zoneFeatures.values()) {
			if (originPoint.isWithinDistance(geometry, buffer)) {
				originWithinProvidedGeometry = true;
				break;
			}
		}
		boolean destinationWithinProvidedGeometry = false;
		Point destinationPoint = MGC.coord2Point(destination);
		for (Geometry geometry : zoneFeatures.values()) {
			if (destinationPoint.isWithinDistance(geometry, buffer)) {
				destinationWithinProvidedGeometry = true;
				break;
			}
		}
		
		if (this.tripConsiderType == TripConsiderType.OriginAndDestination) {
			if (originWithinProvidedGeometry && destinationWithinProvidedGeometry) return true;
		} else if (this.tripConsiderType == TripConsiderType.OriginOrDestination) {
			if (originWithinProvidedGeometry || destinationWithinProvidedGeometry) return true;
		} else {
			throw new RuntimeException("Unknown TripConsiderType. Aborting...");
		}
				
		return false;
	}

	@Override
	public String toFileName() {
		String fileName = "_TRIPFILTER-" + filterName;
				
		if (zoneFile == null) {
			// all trips considered (no-shapefile-provided)
			fileName = "";	
			
		} else {
			if (zoneFeatures.size() == 0) {
				// no zoneFeatures loaded --> consider all trips
				fileName = fileName + "_all-trips-considered_no-zone-features";	
			} else {
				fileName = fileName + "_" +  this.tripConsiderType.toString() + "-in-zone_buffer-" + this.buffer ;
			}
		}
	
		return fileName;
	}

	public String getZoneFile() {
		return zoneFile;
	}

	public void setZoneInformation(String zoneFile, String zonesCRS) {
		if (zoneFile == null || zoneFile.endsWith("null") || zoneFile.equals("")) {
			this.zoneFile = null;
		} else {
			this.zoneFile = zoneFile;
		}		
		this.zoneCRS  = zonesCRS;
	}
	
	public void preProcess(Scenario scenario) {
		this.dataPreprocessed = true;
	    	    
		if (scenario != null &&  zoneFile != null) {
			
			if (scenario.getNetwork() != null && this.zoneCRS != null) {
				String crsNetwork = (String) scenario.getNetwork().getAttributes().getAttribute("coordinateReferenceSystem");
		        if (!this.zoneCRS.equalsIgnoreCase(crsNetwork)) {
		        	if ( (this.zoneCRS.equalsIgnoreCase("DHDN_GK4") && crsNetwork.equalsIgnoreCase("GK4"))
		        			|| (this.zoneCRS.equalsIgnoreCase("GK4") && crsNetwork.equalsIgnoreCase("DHDN_GK4"))
		        			|| (this.zoneCRS.equalsIgnoreCase("GK4") && crsNetwork.equalsIgnoreCase("EPSG:31468"))
		        			|| (this.zoneCRS.equalsIgnoreCase("EPSG:31468") && crsNetwork.equalsIgnoreCase("GK4"))
		        			|| (this.zoneCRS.equalsIgnoreCase("DHDN_GK4") && crsNetwork.equalsIgnoreCase("EPSG:31468"))
		        			|| (this.zoneCRS.equalsIgnoreCase("EPSG:31468") && crsNetwork.equalsIgnoreCase("DHDN_GK4"))
		        			) {
		        		// should not cause any problems.
		        	} else {
		        		throw new RuntimeException("Coordinate transformation not yet implemented. Expecting shape file to have the following coordinate reference system: " + crsNetwork + " instead of " + this.zoneCRS);
				        // TODO: add coordinate transformation
		        	}
				}
			}
			
			log.info("Reading shape file...");
			Collection<SimpleFeature> features = null;
			if (zoneFile.startsWith("http")) {
				try {
					features = ShapeFileReader.getAllFeatures(new URL(zoneFile));
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			} else {
				features = ShapeFileReader.getAllFeatures(zoneFile);
			}
			int counter = 0;
			for (SimpleFeature feature : features) {
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
//                Geometry geometryWithBuffer = geometry.buffer(buffer);
                zoneFeatures.put(String.valueOf(counter), geometry);
                counter++;
            }
			log.info("Reading shape file... Done.");	
		}
		
	}

	public void setBuffer(double buffer) {
		this.buffer = buffer;
	}

	public void setTripConsiderType(TripConsiderType tripConsiderType) {
		this.tripConsiderType = tripConsiderType;
	}

	// there is PopulationUtils.decideOnCoordForActivity, but it throws exceptions when the coord of a activityFacility is null
	// not sure whether this is a problem, keeping the existing Joschka methods from TripsAndLegsCSVWriter

	// copy from TripsAndLegsCSVWriter
	private Coord getCoordFromActivity(Activity activity, Scenario scenario) {
		if (activity.getCoord() != null) {
			return activity.getCoord();
		} else if (activity.getFacilityId() != null && scenario.getActivityFacilities().getFacilities().containsKey(activity.getFacilityId())) {
			Coord coord = scenario.getActivityFacilities().getFacilities().get(activity.getFacilityId()).getCoord();
			return coord != null ? coord : this.getCoordFromLink(activity.getLinkId(), scenario);
		} else {
			return this.getCoordFromLink(activity.getLinkId(), scenario);
		}
	}

	// copy from TripsAndLegsCSVWriter
	private Coord getCoordFromLink(Id<Link> linkId, Scenario scenario) {
		return scenario.getNetwork().getLinks().get(linkId).getToNode().getCoord();
	}
}

