/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.analysis.od;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;


/**
 * @author zmeng
 */

public class ODAnalysisTest {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
   
	@Test
    public final void odAnalysisTest() {

            final String outputDirectory = testUtils.getOutputDirectory();
            final String runDirectory = testUtils.getPackageInputDirectory() + "test_runDirectory/";
            final String runId = "berlin-drtA-v5.2-1pct";
            final String shapeFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/shp-files/shp-bezirke/bezirke_berlin.shp";
            final String[] helpLegModes = {TransportMode.transit_walk, TransportMode.non_network_walk, "access_walk", "egress_walk"};
            final String stageActivitySubString = "interaction";

            final String zoneId = "SCHLUESSEL";
            final List<String> modes = new ArrayList<>();
            modes.add(TransportMode.drt);
            modes.add("access_walk");

            double scaleFactor = 10.;
			final String networkFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.2-10pct/output-berlin-v5.2-10pct/berlin-v5.2-10pct.output_network.xml.gz";
			
			EventsManager events = EventsUtils.createEventsManager();

			ODEventAnalysisHandler handler1 = new ODEventAnalysisHandler(helpLegModes, stageActivitySubString);
			events.addHandler(handler1);

			MatsimEventsReader reader = new MatsimEventsReader(events);
			events.initProcessing();
			reader.readFile(runDirectory + runId + ".output_events.xml.gz");
			events.finishProcessing();

			Config config = ConfigUtils.createConfig();
			config.network().setInputFile(networkFile);
			config.network().setInputCRS("GK4");
			config.global().setCoordinateSystem("GK4");
			config.controler().setOutputDirectory(outputDirectory);
			Network network = ScenarioUtils.loadScenario(config).getNetwork();
			
			ODAnalysis odAnalysis = new ODAnalysis(outputDirectory, network, runId, shapeFile, "EPSG:31468" , zoneId, modes, scaleFactor);
			odAnalysis.setPrintTripSHPfiles(true);
			odAnalysis.process(handler1);

            String csvFilename = outputDirectory+ "/od-analysis/" + runId + ".od-analysis_0.0-36.0_drt_trips.csv";
            List<ODRelation> odRelationsList = ODAnalysisCSVReader(csvFilename);

            for (ODRelation odRelation : odRelationsList) {
                if (odRelation.getOrigin().equals("010111") && odRelation.getDestination().equals("040309")) {
                    Assert.assertEquals(2. * scaleFactor, odRelation.getTrips(), MatsimTestUtils.EPSILON);
                } else if (odRelation.getOrigin().equals("040309") && odRelation.getDestination().equals("010111")) {
                    Assert.assertEquals(2. * scaleFactor, odRelation.getTrips(), MatsimTestUtils.EPSILON);
                } else if (odRelation.getOrigin().equals("080105") && odRelation.getDestination().equals("080105")) {
                    Assert.assertEquals(1. * scaleFactor, odRelation.getTrips(), MatsimTestUtils.EPSILON);
                } else {
                    Assert.assertEquals(0 * scaleFactor, odRelation.getTrips(), MatsimTestUtils.EPSILON);
                }
            }
        }


    public List<ODRelation> ODAnalysisCSVReader(String csvFilename) {
        File inputFile = new File(csvFilename);
        List<ODRelation> ODRelationsList = new ArrayList<>();
        List<String> destinationList = new ArrayList<>();

        try(BufferedReader in = new BufferedReader((new FileReader(inputFile)));){
            String line = in.readLine();
            String[] entries = line.split(";");
            for(int i=1; i<=24; i++){
                String destination = entries[i];
                destinationList.add(destination);
            }

            while ((line = in.readLine()) != null) {
                String[] entries2 = line.split(";");
                String origin = entries2[0];

                for(int i=1; i<=24; i++){
                    ODRelation odRelation= new ODRelation(null,origin,destinationList.get(i-1));
                    odRelation.setTrips(Double.valueOf(entries2[i]));
                    ODRelationsList.add(odRelation);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
        return ODRelationsList;
    }
}
