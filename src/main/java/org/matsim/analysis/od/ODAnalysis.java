/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.analysis.od;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * 
 * 
* @author ikaddoura
*/
public final class ODAnalysis {

	private static final Logger log = LogManager.getLogger(ODAnalysis.class);
	private final String analysisOutputFolder = "od-analysis/";
	
	private final String outputDirectory;
    private final Network network;
	private final String shapeFile;
	private final String runId;
	private final String zoneId;
	private final List<String> modes;
	private final Coord dummyCoord = new Coord(0.0, 0.0);
	private final Coordinate dummyCoordinateOutside = new Coordinate(0.0,0.0);
	private final double scaleFactor;
	private final String shapeFileCRS;
	
	private boolean printODSHPfiles = true;
	private boolean printTripSHPfiles = false;
	private int noArrivalLinkWarnCounter;

    /**
     * @param outputDirectory
     * @param network
     * @param runId
     * @param shapeFile
     * @param shapeFileCRS
     * @param zoneId
     * @param modes
     * @param scaleFactor
     */
    public ODAnalysis(String outputDirectory, Network network, String runId, String shapeFile, String shapeFileCRS, String zoneId, List<String> modes, double scaleFactor) {
        if (!outputDirectory.endsWith("/")) outputDirectory = outputDirectory + "/";

        this.outputDirectory = outputDirectory + analysisOutputFolder;
        this.shapeFile = shapeFile;
        this.runId = runId;
        this.zoneId = zoneId;
        this.modes = modes;
        this.scaleFactor = scaleFactor;
        this.shapeFileCRS = shapeFileCRS;
		this.network = network;
	}

	public void process(ODEventAnalysisHandler handler1) {
		
		if (network != null && this.shapeFileCRS != null) {
			String crsNetwork = (String) network.getAttributes().getAttribute("coordinateReferenceSystem");
	        if (!shapeFileCRS.equalsIgnoreCase(crsNetwork)) {
	        	if (
	        			(shapeFileCRS.equalsIgnoreCase("DHDN_GK4") && crsNetwork.equalsIgnoreCase("GK4")) ||
	        			(shapeFileCRS.equalsIgnoreCase("GK4") && crsNetwork.equalsIgnoreCase("DHDN_GK4")) ||
	        			(shapeFileCRS.equalsIgnoreCase("EPSG:31468") && crsNetwork.equalsIgnoreCase("DHDN_GK4")) ||
	        			(shapeFileCRS.equalsIgnoreCase("DHDN_GK4") && crsNetwork.equalsIgnoreCase("EPSG:31468")) ||
	        			(shapeFileCRS.equalsIgnoreCase("GK4") && crsNetwork.equalsIgnoreCase("EPSG:31468")) ||
	        			(shapeFileCRS.equalsIgnoreCase("EPSG:31468") && crsNetwork.equalsIgnoreCase("GK4"))
	        			) {
	        		// should not cause any problems.
	        	} else {
	        		throw new RuntimeException("Coordinate transformation not yet implemented. Expecting shape file to have the following coordinate reference system: " + crsNetwork);
			        // TODO: add coordinate transformation
	        	}
			}
		}
		
		{
			File file = new File(outputDirectory);
			file.mkdirs();
		}
		
		if (printODSHPfiles) {
			File file = new File(outputDirectory + "shapefiles_aggregated-od-analysis/");
			file.mkdirs();
		}
		
		if (printTripSHPfiles) {
			File file = new File(outputDirectory + "shapefiles_disaggegated-od-analysis/");
			file.mkdirs();
		}

    	Map<String, Geometry> zones = new HashMap<>();
        if (shapeFile != null) {
            Collection<SimpleFeature> features;
        	if (shapeFile.startsWith("http")) {
                URL shapeFileAsURL = null;
    			try {
    				shapeFileAsURL = new URL(shapeFile);
    			} catch (MalformedURLException e) {
    				e.printStackTrace();
    			}
                features = ShapeFileReader.getAllFeatures(shapeFileAsURL);
            } else {
                features = ShapeFileReader.getAllFeatures(shapeFile);
            }
        	
        	if (zoneId == null || zoneId == "") {
        		int idCounter = 0;
        		for (SimpleFeature feature : features) {
        			Geometry geometry = (Geometry) feature.getDefaultGeometry();
        			zones.put(String.valueOf(idCounter), geometry);
        			idCounter++;
        		}
        	} else {
        		for (SimpleFeature feature : features) {
        			String id = feature.getAttribute(zoneId).toString();
        			Geometry geometry = (Geometry) feature.getDefaultGeometry();
        			zones.put(id, geometry);
        		}
        	}
        }

		List<ODTrip> odTrips = new ArrayList<>();
		{
			int counter = 0;
			log.info("persons (sample size): " + handler1.getPersonId2tripNumber2departureTime().size());
			for (Id<Person> personId : handler1.getPersonId2tripNumber2departureTime().keySet()) {
				counter++;
				if (counter%1000 == 0.) log.info("person # " + counter);
				for (Integer tripNr : handler1.getPersonId2tripNumber2departureTime().get(personId).keySet()) {
					ODTrip odTrip = new ODTrip();
					odTrip.setPersonId(personId);
					Id<Link> departureLink = handler1.getPersonId2tripNumber2departureLink().get(personId).get(tripNr);
					if (network.getLinks().get(departureLink) == null) {
						throw new RuntimeException("departure link is null. Aborting...");
					}
					Coord departureLinkCoord = network.getLinks().get(departureLink).getCoord();
					odTrip.setOriginCoord(departureLinkCoord);
					odTrip.setOrigin(getDistrictId(zones, departureLinkCoord ));

					Coord arrivalLinkCoord;
					if (handler1.getPersonId2tripNumber2arrivalLink().get(personId) == null || handler1.getPersonId2tripNumber2arrivalLink().get(personId).get(tripNr) == null) {
						log.warn("no arrival link for person " + personId + " / trip # " + tripNr + ". Probably a stucking agent.");
						arrivalLinkCoord = dummyCoord;
					} else {
						Id<Link> arrivalLink = handler1.getPersonId2tripNumber2arrivalLink().get(personId).get(tripNr);
						arrivalLinkCoord = network.getLinks().get(arrivalLink).getCoord();
					}
					odTrip.setDestinationCoord(arrivalLinkCoord);
					odTrip.setDestination(getDistrictId(zones, arrivalLinkCoord));
					odTrip.setMode(handler1.getPersonId2tripNumber2legMode().get(personId).get(tripNr));
					odTrip.setDepartureTime(handler1.getPersonId2tripNumber2departureTime().get(personId).get(tripNr));
					odTrips.add(odTrip);
				}
			}
		}
		
		List<ODTrip> odLegs = new ArrayList<>();
		{
			int counter = 0;
			log.info("persons (sample size): " + handler1.getPersonId2legNumber2departureTime().size());
			for (Id<Person> personId : handler1.getPersonId2legNumber2departureTime().keySet()) {
				counter++;
				if (counter%1000 == 0.) log.info("person # " + counter);
				for (Integer tripNr : handler1.getPersonId2legNumber2departureTime().get(personId).keySet()) {
					ODTrip odLeg = new ODTrip();
					odLeg.setPersonId(personId);
					Id<Link> departureLink = handler1.getPersonId2legNumber2departureLink().get(personId).get(tripNr);
					if (network.getLinks().get(departureLink) == null) {
						throw new RuntimeException("departure link is null. Aborting...");
					}
					Coord departureLinkCoord = network.getLinks().get(departureLink).getCoord();
					odLeg.setOriginCoord(departureLinkCoord);
					odLeg.setOrigin(getDistrictId(zones, departureLinkCoord ));

					Coord arrivalLinkCoord;
					if (handler1.getPersonId2legNumber2arrivalLink().get(personId) == null || handler1.getPersonId2legNumber2arrivalLink().get(personId).get(tripNr) == null) {
						if (this.noArrivalLinkWarnCounter <= 5) {
							log.warn("no arrival link for person " + personId + " / trip # " + tripNr + ". Probably a stucking agent.");
							if (this.noArrivalLinkWarnCounter == 5) {
								log.warn("Further warnings of this type will not be printed out.");
							}
							this.noArrivalLinkWarnCounter++;
						}
						arrivalLinkCoord = dummyCoord;
					} else {
						Id<Link> arrivalLink = handler1.getPersonId2legNumber2arrivalLink().get(personId).get(tripNr);
						arrivalLinkCoord = network.getLinks().get(arrivalLink).getCoord();
					}
					odLeg.setDestinationCoord(arrivalLinkCoord);
					odLeg.setDestination(getDistrictId(zones, arrivalLinkCoord));
					odLeg.setMode(handler1.getPersonId2legNumber2legMode().get(personId).get(tripNr));
					odLeg.setDepartureTime(handler1.getPersonId2legNumber2departureTime().get(personId).get(tripNr));
					odLegs.add(odLeg);
				}
			}
		}
		
		List<Tuple<Double, Double>> timeBinsDay = new ArrayList<>();
		timeBinsDay.add(new Tuple<Double, Double>(0., 36.));
		filterAndPrintInformation("trips", odTrips, zones, modes, timeBinsDay);
		filterAndPrintInformation("legs", odLegs, zones, modes, timeBinsDay);

		if (modes.size() > 1) {
			for (String mode : modes) {
				List<String> eachModeSeparately = new ArrayList<>();
				eachModeSeparately.add(mode);
				filterAndPrintInformation("trips", odTrips, zones, eachModeSeparately, timeBinsDay);
				filterAndPrintInformation("legs", odLegs, zones, eachModeSeparately, timeBinsDay);
			}
		}
		
		List<Tuple<Double, Double>> timeBinsPeriods = new ArrayList<>();
		timeBinsPeriods.add(new Tuple<Double, Double>(0., 6.));
		timeBinsPeriods.add(new Tuple<Double, Double>(6., 9.));
		timeBinsPeriods.add(new Tuple<Double, Double>(9., 12.));
		timeBinsPeriods.add(new Tuple<Double, Double>(12., 15.));
		timeBinsPeriods.add(new Tuple<Double, Double>(15., 18.));
		timeBinsPeriods.add(new Tuple<Double, Double>(18., 21.));
		timeBinsPeriods.add(new Tuple<Double, Double>(21., 24.));
		filterAndPrintInformation("trips", odTrips, zones, modes, timeBinsPeriods);
		filterAndPrintInformation("legs", odLegs, zones, modes, timeBinsPeriods);		
		if (modes.size() > 1) {
			for (String mode : modes) {
				List<String> eachModeSeparately = new ArrayList<>();
				eachModeSeparately.add(mode);
				filterAndPrintInformation("trips", odTrips, zones, eachModeSeparately, timeBinsPeriods);
				filterAndPrintInformation("legs", odLegs, zones, eachModeSeparately, timeBinsPeriods);
			}
		}
	}

	private void filterAndPrintInformation(String filePrefix, List<ODTrip> odTrips, Map<String, Geometry> zones, List<String> modes, List<Tuple<Double, Double>> timeBins) {

		LinkedHashMap<String, Map<String, ODRelation>> time2odRelations = new LinkedHashMap<>();
		
		String modesString = "";
		for (String mode : modes) {
			modesString = modesString + "_" + mode;
		}

		for (Tuple<Double,Double> timeBin : timeBins) {
			
			Map<String, ODRelation> filteredOdRelations = new HashMap<>();
			List<ODTrip> filteredTrips = new ArrayList<>();
			int filteredTripCounter = 0;

			double from = timeBin.getFirst() * 3600.;
			double to = timeBin.getSecond() * 3600.;

			ODTripFilter hourFilter = new ODTripFilter(from, to, "", modes);
			log.info("###### " + from + " to " + to);
			log.info("total number of trips or legs (sample size) " + odTrips.size());

			for (ODTrip trip : odTrips) {
				if (hourFilter.considerTrip(trip)) {
					filteredTripCounter++;
					filteredTrips.add(trip);
					String od = trip.getOrigin() + "-" + trip.getDestination();

					if (filteredOdRelations.get(od) == null) {
						filteredOdRelations.put(od, new ODRelation(od, trip.getOrigin(), trip.getDestination()));
					} else {
						double tripsSoFar = filteredOdRelations.get(od).getTrips();
						filteredOdRelations.get(od).setTrips(tripsSoFar + 1);
					}
				} else {
					// skip trip
				}
			}
			log.info("filtered (considered mode, time bin, ...) trips/legs (sample size): " + filteredTripCounter);
			try {
				writeData(filteredOdRelations, zones, outputDirectory + runId + ".od-analysis_" + timeBin.getFirst() + "-" + timeBin.getSecond() + modesString + "_" + filePrefix +".csv");
				writeDataTable(filteredOdRelations, outputDirectory + runId + ".od-analysis_" + timeBin.getFirst() + "-" + timeBin.getSecond() + modesString + "_from-to-format_" + filePrefix + ".csv");
				if (printTripSHPfiles) printODLinesForEachAgent(filteredTrips, outputDirectory + "shapefiles_disaggegated-od-analysis/" + runId + ".disaggregated-od-analysis_" + timeBin.getFirst() + "-" + timeBin.getSecond() + modesString + "_" + filePrefix + ".shp");

			} catch (IOException e) {
				e.printStackTrace();
			}
			
			boolean writeHourlyShapefiles = true;

			if (writeHourlyShapefiles) {
				Map<String, Map<String, ODRelation>> time2odRelation = new HashMap<>();
				time2odRelation.put(from + "-" + to, filteredOdRelations);
				try {
					if (printODSHPfiles) printODLines(time2odRelation, zones, outputDirectory + "shapefiles_aggregated-od-analysis/" + runId + ".od-analysis_" + timeBin.getFirst() + "-" + timeBin.getSecond() + modesString + "_" + filePrefix + ".shp");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			time2odRelations.put(timeBin.getFirst() + "-" + timeBin.getSecond(), filteredOdRelations);
		}

		try {
			writeDataTableTimeBins(time2odRelations, zones, outputDirectory + runId + ".od-analysis_AllTimeBins" + modesString + "_from-to-format_" + filePrefix + ".csv");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			if (printODSHPfiles) printODLines(time2odRelations, zones, outputDirectory + "shapefiles_aggregated-od-analysis/" + runId + ".od-analysis_AllTimeBins" + modesString + "_" + filePrefix + ".shp");
		} catch (Exception e) {
			e.printStackTrace();
		}	
		
	}

	private void writeData(Map<String, ODRelation> odRelations, Map<String, Geometry> zones,  String fileName) throws IOException {
		BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
		writer.write("from/to;");

		List<String> zoneIds = new ArrayList<>();
		zoneIds.addAll(zones.keySet());
		zoneIds.add("other");

		for (String zone : zoneIds) {
			writer.write(zone + ";");
		}
		writer.newLine();

		double tripsInMatrixCounter = 0;
		for (String zoneFrom : zoneIds) {
			writer.write(zoneFrom + ";");
			for (String zoneTo : zoneIds) {

				double trips = 0;
				if (odRelations.get(zoneFrom + "-" + zoneTo) != null) {
					trips = odRelations.get(zoneFrom + "-" + zoneTo).getTrips() * this.scaleFactor;
					tripsInMatrixCounter = tripsInMatrixCounter + trips;
				}
				writer.write(trips + ";");
			}
			writer.newLine();
		}

		writer.close();

		log.info("Matrix written to file.");
		log.info("Total number of trips in Matrix: " + tripsInMatrixCounter);
	}
	
	private void writeDataTable(Map<String, ODRelation> odRelations, String fileName) throws IOException {
		BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
		writer.write("origin;destination;number");
		writer.newLine();

		for (ODRelation odRelation : odRelations.values()) {
			writer.write(odRelation.getOrigin() + ";" + odRelation.getDestination() + ";" + odRelation.getTrips() * this.scaleFactor);
			writer.newLine();
		}

		writer.close();

		log.info("Table written to file.");
	}
	
	private void writeDataTableTimeBins(Map<String, Map<String, ODRelation>> time2odRelations, Map<String, Geometry> zones, String fileName) throws IOException {
		
		BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
		writer.write("origin;destination");
		for (String time : time2odRelations.keySet()) {
			writer.write(";" + time);
		}
		writer.newLine();
		
		List<String> zoneIds = new ArrayList<>();
		zoneIds.addAll(zones.keySet());
		zoneIds.add("other");

		for (String zoneFrom : zoneIds) {
			for (String zoneTo : zoneIds) {
				writer.write(zoneFrom + ";" + zoneTo);
				for (String time : time2odRelations.keySet()) {
					
					double trips = 0.;				
					if (time2odRelations.get(time).get(zoneFrom + "-" + zoneTo) != null) {
						trips = time2odRelations.get(time).get(zoneFrom + "-" + zoneTo).getTrips();
					}
					writer.write(";" + trips * this.scaleFactor);
				}
				writer.newLine();
			}
		}
		
		writer.close();

		log.info("Table written to file.");
	}

	private String getDistrictId(Map<String, Geometry> districts, Coord coord) {
		Point point = MGC.coord2Point(coord);
		for (String nameDistrict : districts.keySet()) {
			Geometry geo = districts.get(nameDistrict);
			if (geo.contains(point)) {
				return nameDistrict;
			}
		}
		return "other";
	}

	private void printODLines(Map<String, Map<String, ODRelation>> time2odRelations, Map<String, Geometry> zones, String fileName) throws IOException {

		CoordinateReferenceSystem crs;
		if (this.shapeFileCRS != null) {
			try {
				crs = MGC.getCRS(this.shapeFileCRS);
			} catch (IllegalArgumentException e) {
				log.warn("Couldn't get CRS from geotools.");
				log.error(e.getMessage());
				crs = null;
			}
		} else {
			crs = null;
		}
		
		PolylineFeatureFactory factory = new PolylineFeatureFactory.Builder()
        		.setCrs(crs)
        		.setName("OD")
        		.addAttribute("OD_ID", String.class)
        		.addAttribute("O", String.class)
        		.addAttribute("D", String.class)
        		.addAttribute("number", Integer.class)
        		.addAttribute("startTime", String.class)
        		.addAttribute("endTime", String.class)
        		.create();

        		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();

        for (String time : time2odRelations.keySet() ) {

        	Map<String, ODRelation> odRelations = time2odRelations.get(time);

			for (String od : odRelations.keySet()) {

             	if (odRelations.get(od) != null) {

             		Coordinate originCoord;
             		if (odRelations.get(od).getOrigin().equals("other")) {
             			originCoord = dummyCoordinateOutside ;
                 	} else {
                 		originCoord = zones.get(odRelations.get(od).getOrigin()).getCentroid().getCoordinate();
                 	}

             		Coordinate destinationCoord;
                 	if (odRelations.get(od).getDestination().equals("other")) {
                 		destinationCoord = dummyCoordinateOutside ;
                 		} else {
                    			destinationCoord = zones.get(odRelations.get(od).getDestination()).getCentroid().getCoordinate();
                 		}

                 		String[] fromTo = time.split("-");
						double from = Double.valueOf(fromTo[0]);
						double to = Double.valueOf(fromTo[1]);
						SimpleFeature feature = factory.createPolyline(

                        		new Coordinate[] {
             						new Coordinate(originCoord),
             						new Coordinate(destinationCoord) }

             					, new Object[] { od, odRelations.get(od).getOrigin(), odRelations.get(od).getDestination(), odRelations.get(od).getTrips()  * this.scaleFactor, Time.writeTime(from * 3600.), Time.writeTime(to * 3600.) }
                        		, null
             			);
             		features.add(feature);
                 }
             }
        }

        if (!features.isEmpty()) {
			try {
				ShapeFileWriter.writeGeometries(features, fileName);
			} catch (Exception e) {
				log.warn("Shape file was not written out: " + fileName);
			}
		} else {
			log.warn("Shape file was not written out (empty).");
		}
	}

	private void printODLinesForEachAgent(List<ODTrip> filteredTrips, String fileName) throws IOException {

		CoordinateReferenceSystem crs;
		if (this.shapeFileCRS != null) {
			crs = MGC.getCRS(this.shapeFileCRS);
		} else {
			crs = null;
		}
		
		PolylineFeatureFactory factory = new PolylineFeatureFactory.Builder()
        		.setCrs(crs)
        		.setName("trip")
        		.addAttribute("personId", String.class)
        		.addAttribute("O", String.class)
        		.addAttribute("D", String.class)
        		.addAttribute("depTime", Double.class)
        		.create();

        		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();

        for (ODTrip trip : filteredTrips) {

        	SimpleFeature feature = factory.createPolyline(

            		new Coordinate[] {
 						new Coordinate(trip.getOriginCoord().getX(), trip.getOriginCoord().getY()),
 						new Coordinate(trip.getDestinationCoord().getX(), trip.getDestinationCoord().getY()) }

 					, new Object[] { trip.getPersonId(), trip.getOrigin(), trip.getDestination(), trip.getDepartureTime() }
            		, null
 			);
 		features.add(feature);
        }

        if (!features.isEmpty()) {
			try {
				ShapeFileWriter.writeGeometries(features, fileName);
			} catch (Exception e) {
				log.warn("Shape file was not written out: " + fileName);
			}
		} else {
			log.warn("Shape file was not written out (empty).");
		}
	}

	public void setPrintODSHPfiles(boolean printODSHPfiles) {
		this.printODSHPfiles = printODSHPfiles;
	}

	public void setPrintTripSHPfiles(boolean printTripSHPfiles) {
		this.printTripSHPfiles = printTripSHPfiles;
	}

}
