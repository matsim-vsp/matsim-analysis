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

package org.matsim.analysis.shapes;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory.Builder;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
* @author ikaddoura
*/

public class Network2Shape {
	private final static Logger log = Logger.getLogger(Network2Shape.class);

	public static void exportNetwork2Shp(Scenario scenario, String outputDirectory, String scenarioCrs, CoordinateTransformation ct){
		
		String outputPath = outputDirectory + "network-shp/";
		File file = new File(outputPath);
		file.mkdirs();
		
		log.info("Writing shape file...");
		
		Builder featureFactoryBuilder = new PolylineFeatureFactory.Builder();
		CoordinateReferenceSystem crs;
		try {
			crs = MGC.getCRS(scenarioCrs);
			featureFactoryBuilder.setCrs(crs);
		} catch (Exception e) {
			e.printStackTrace();
			log.warn("Assuming all coordinates to be in the correct coordinate reference system.");
			crs = null;
		}
				
		featureFactoryBuilder.setName("Link");
		featureFactoryBuilder.addAttribute("Id", String.class);
		featureFactoryBuilder.addAttribute("Length", Double.class);
		featureFactoryBuilder.addAttribute("capacity", Double.class);
		featureFactoryBuilder.addAttribute("lanes", Double.class);
		featureFactoryBuilder.addAttribute("Freespeed", Double.class);
		featureFactoryBuilder.addAttribute("Modes", String.class);
		
		PolylineFeatureFactory featureFactory = featureFactoryBuilder.create();

		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		
		for (Link link : scenario.getNetwork().getLinks().values()){
			
			Coord fromCoord = ct.transform(link.getFromNode().getCoord());
			Coord toCoord = ct.transform(link.getToNode().getCoord());
			
			Coordinate[] coordinates = new Coordinate[]{ new Coordinate(MGC.coord2Coordinate(fromCoord)), new Coordinate(MGC.coord2Coordinate(toCoord))};
			Map<String, Object> attributes = new LinkedHashMap<String, Object>();
			attributes.put("Id", link.getId().toString());
			attributes.put("Length", link.getLength());
			attributes.put("capacity", link.getCapacity());
			attributes.put("lanes", link.getNumberOfLanes());
			attributes.put("Freespeed", link.getFreespeed());
			attributes.put("Modes", String.join(",", link.getAllowedModes()));

			SimpleFeature feature = featureFactory.createPolyline(coordinates , attributes , link.getId().toString());
			features.add(feature);
		}
		
		log.info("Writing network to shapefile... ");
		if (scenario.getConfig().controler().getRunId() == null) {
			ShapeFileWriter.writeGeometries(features, outputPath + "network.shp");
		} else {
			ShapeFileWriter.writeGeometries(features, outputPath + scenario.getConfig().controler().getRunId() + ".network.shp");
		}
		log.info("Writing network to shapefile... Done.");
	}

}

