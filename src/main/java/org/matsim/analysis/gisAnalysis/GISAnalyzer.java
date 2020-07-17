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

package org.matsim.analysis.gisAnalysis;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.analysis.detailedPersonTripAnalysis.BasicPersonTripAnalysisHandler;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.core.utils.gis.PolygonFeatureFactory.Builder;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * 
 * Reads in a shapeFile and performs a zone-based analysis:
 * Calculates the number of home activities, work activities, all activities per zone.
 * Calculates the congestion costs of the whole day mapped back to the causing agents' home zones.
 * 
 * The shape file has to contain a grid (e.g. squares, hexagons) which can be created using a QGIS plugin called MMQGIS.
 * 
 * @author ikaddoura
 *
 */
public class GISAnalyzer {
	private static final Logger log = Logger.getLogger(GISAnalyzer.class);

	private final int scalingFactor;
	private final Map<Integer, SimpleFeature> features = new HashMap<>();
	private final Map<Integer, Geometry> zoneId2geometry = new HashMap<Integer, Geometry>();
	private final String zonesCRS;
	private final CoordinateTransformation ct;
	private final Map<Id<Person>, Integer> personId2homeZoneId;
	private final List<String> modes;

	public GISAnalyzer(
			Scenario scenario,
			String shapeFileZones,
			int scalingFactor,
			String homeActivity,
			String zonesCRS,
			String scenarioCRS,
			List<String> modes) {
		
		this.scalingFactor = scalingFactor;
		this.zonesCRS = zonesCRS;
		this.ct = TransformationFactory.getCoordinateTransformation(scenarioCRS, zonesCRS);
		this.modes = modes;

		log.info("Reading zone shapefile...");
		int featureCounter = 0;
		Collection<SimpleFeature> allFeatures = null;
		if (shapeFileZones.startsWith("http")) {
			URL shapeFileAsURL = null;
			try {
				shapeFileAsURL = new URL(shapeFileZones);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			allFeatures = ShapeFileReader.getAllFeatures(shapeFileAsURL);
		} else {
			allFeatures = ShapeFileReader.getAllFeatures(shapeFileZones);
		}
		for (SimpleFeature feature : allFeatures) {
			features.put(featureCounter, feature);
			this.zoneId2geometry.put(featureCounter, (Geometry) feature.getDefaultGeometry());
			featureCounter++;
		}
		log.info("Reading zone shapefile... Done. Number of zones: " + featureCounter);
		
		log.info("Pre-processing the population data...");
		
		log.info("		--> Getting the persons' home coordinates.");
		SortedMap<Id<Person>,Coord> personId2homeCoord = getPersonId2Coordinates(scenario.getPopulation(), homeActivity);
		
		log.info("		--> Getting the persons' home zones.");
		personId2homeZoneId = getPersonId2homeZoneId(personId2homeCoord);
		
		log.info("Pre-processing the population data... Done.");
		
	}
	
	private Map<Id<Person>, Integer> getPersonId2homeZoneId(SortedMap<Id<Person>, Coord> personId2homeCoord) {

		Map<Id<Person>, Integer> personId2homeZone = new HashMap<>();
		
		for (Id<Person> personId : personId2homeCoord.keySet()) {
			if (personId2homeCoord.get(personId) == null) {
				// person without a home activity
			} else {

				Point p = MGC.coord2Point(ct.transform(personId2homeCoord.get(personId))); 

				for (Integer zoneId : zoneId2geometry.keySet()) {
					
					if (p.within(zoneId2geometry.get(zoneId))){
						personId2homeZone.put(personId, zoneId);
						break;
					}
				}
			}
		}
		return personId2homeZone;
	}

	public void analyzeZoneTollsUserBenefits(
			String runDirectory, String fileName,
			Map<Id<Person>, Double> personId2userBenefits,
			Map<Id<Person>, Double> personId2tollPayments,
			BasicPersonTripAnalysisHandler basicHandler) {
		
		String outputPath = runDirectory;
		
		Map<Id<Person>, Double> personId2travelTime = new HashMap<>();
		for (Id<Person> personId : basicHandler.getPersonId2tripNumber2travelTime().keySet()) {
			double tt = 0.;
			for (Integer tripNr : basicHandler.getPersonId2tripNumber2travelTime().get(personId).keySet()) {
				tt = tt + basicHandler.getPersonId2tripNumber2travelTime().get(personId).get(tripNr); 
			}
			personId2travelTime.put(personId, tt);
		}

		Map<String, Map<Id<Person>, Double>> mode2personId2trips = new HashMap<>();
		
		for (Id<Person> personId : basicHandler.getPersonId2tripNumber2tripMainMode().keySet()) {
			for (Integer tripNr : basicHandler.getPersonId2tripNumber2tripMainMode().get(personId).keySet()) {
				String mode = basicHandler.getPersonId2tripNumber2tripMainMode().get(personId).get(tripNr);
				if(mode2personId2trips.get(mode) == null) {
					Map<Id<Person>, Double> personId2trips = new HashMap<>();
					personId2trips.put(personId, 1.0);
					mode2personId2trips.put(mode, personId2trips);
				} else {
					if(mode2personId2trips.get(mode).get(personId) == null) {
						mode2personId2trips.get(mode).put(personId, 1.0);
					} else {
						double tripsSoFar = mode2personId2trips.get(mode).get(personId);
						mode2personId2trips.get(mode).put(personId, tripsSoFar + 1.0);
					}
				}
			}
		}
						
		Map<Integer,Integer> zoneNr2homeActivities = getZoneNr2activityLocations();
		
		// absolute numbers mapped back to home location
		log.info("Mapping absolute toll payments and user benefits to home location...");
		Map<Integer,Double> zoneNr2tollPayments = getZoneNr2totalAmount(personId2tollPayments);
		Map<Integer,Double> zoneNr2userBenefits = getZoneNr2totalAmount(personId2userBenefits);
		Map<Integer, Double> zoneNr2travelTime = getZoneNr2totalAmount(personId2travelTime);
		
		Map<String, Map<Integer, Double>> mode2zoneNr2Trips = new HashMap<>();
		for (String mode : modes) {
			mode2zoneNr2Trips.put(mode, getZoneNr2totalAmount(mode2personId2trips.get(mode)));
		}
		
		log.info("Mapping absolute toll payments and user benefits to home location... Done.");
		
		log.info("Writing shape file...");
		
		Builder featureFactoryBuilder = new PolygonFeatureFactory.Builder();
		CoordinateReferenceSystem crs;
		try {
			crs = MGC.getCRS(zonesCRS);
			featureFactoryBuilder.setCrs(crs);
		} catch (Exception e) {
			e.printStackTrace();
			log.warn("Assuming all coordinates to be in the correct coordinate reference system.");
			crs = null;
		}
		
		featureFactoryBuilder.setName("zone");
		featureFactoryBuilder.addAttribute("ID", Integer.class);
		featureFactoryBuilder.addAttribute("HomeAct", Integer.class);
		featureFactoryBuilder.addAttribute("Tolls", Double.class);
		featureFactoryBuilder.addAttribute("Scores", Double.class);
		featureFactoryBuilder.addAttribute("TT", Double.class);	
		for (String mode : modes) {
			featureFactoryBuilder.addAttribute(mode, Double.class);
		}
		PolygonFeatureFactory featureFactory = featureFactoryBuilder.create();
		
		Collection<SimpleFeature> featuresToWriteOut = new ArrayList<SimpleFeature>();

		for (Integer id : features.keySet()) {
			Map<String, Object> attributeValues = new HashMap<>();
			attributeValues.put("ID", id);
			attributeValues.put("HomeAct", zoneNr2homeActivities.get(id));
			attributeValues.put("Tolls", zoneNr2tollPayments.get(id));
			attributeValues.put("Scores", zoneNr2userBenefits.get(id));
			attributeValues.put("TT", zoneNr2travelTime.get(id));
			for (String mode : modes) {
				attributeValues.put(mode, mode2zoneNr2Trips.get(mode).get(id));
			}

			Geometry geometry = (Geometry) features.get(id).getDefaultGeometry();
			Coordinate[] coordinates = geometry.getCoordinates();
			
			SimpleFeature feature = featureFactory.createPolygon(coordinates, attributeValues, Integer.toString(id));
			featuresToWriteOut.add(feature);
		}
		
		log.info("Writing shape file to" +  outputPath + fileName + "...");

		ShapeFileWriter.writeGeometries(featuresToWriteOut, outputPath + fileName);

		log.info("Writing shape file... Done.");
		
	}

	private Map<Integer, Double> getZoneNr2totalAmount(Map<Id<Person>, Double> personId2amount) {

		Map<Integer, Double> zoneNr2totalAmount = new HashMap<Integer, Double>();	
		
		for (Integer zoneId : zoneId2geometry.keySet()) {
			zoneNr2totalAmount.put(zoneId, 0.);
		}
				
		if (personId2amount != null) {
			
			for (Id<Person> personId : personId2amount.keySet()) {
				
				if (personId2homeZoneId.get(personId) == null) {
					// person without a home activity
				} else {
					Integer zoneId = personId2homeZoneId.get(personId);
					double previousValue = zoneNr2totalAmount.get(zoneId);
					zoneNr2totalAmount.put(zoneId, previousValue + (personId2amount.get(personId) * scalingFactor));
				}
			}
		}
		
		return zoneNr2totalAmount;
	}

	private Map<Integer, Integer> getZoneNr2activityLocations() {
		Map<Integer, Integer> zoneNr2personCounter = new HashMap<Integer, Integer>();	

		for (Integer zoneId : zoneId2geometry.keySet()) {
			zoneNr2personCounter.put(zoneId, 0);
		}
			
		for (Id<Person> personId : personId2homeZoneId.keySet()) {
			int previousValue = zoneNr2personCounter.get(personId2homeZoneId.get(personId));
			zoneNr2personCounter.put(personId2homeZoneId.get(personId), previousValue  + scalingFactor);			
		}
		return zoneNr2personCounter;
	}
	
	private SortedMap<Id<Person>, Coord> getPersonId2Coordinates(Population population, String activity) {
		SortedMap<Id<Person>,Coord> personId2coord = new TreeMap<Id<Person>,Coord>();
		
		for(Person person : population.getPersons().values()){
			
			for (PlanElement pE : person.getSelectedPlan().getPlanElements()){
				
				if (pE instanceof Activity){
					Activity act = (Activity) pE;
					
					if (act.getType().startsWith(activity)) {
						
						Coord coord = act.getCoord();
						personId2coord.put(person.getId(), coord);
					
					} else {
						//  other activity type
					}
				}
			}
		}
		return personId2coord;
	}
}
