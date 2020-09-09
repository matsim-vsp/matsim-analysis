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

package org.matsim.analysis.shapes;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
* @author ikaddoura
*/

public class RunExportNetwork {

	public static void main(String[] args) {
		
		String outputDirectory = "../runs-svn/avoev/snz-gladbeck/output-snzDrtO443g/";
		String runId = "snzDrtO443g";
		
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem("epsg:25832");
		config.network().setInputFile(outputDirectory + runId + ".output_network.xml.gz");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Network2Shape.exportNetwork2Shp(scenario, outputDirectory, "epsg:25832", TransformationFactory.getCoordinateTransformation("epsg:25832", "epsg:25832"));
	}

}

