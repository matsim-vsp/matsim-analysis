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

package org.matsim.analysis;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AnalysisRunExample {
	private static final Logger log = Logger.getLogger(AnalysisRunExample.class);

	/**
	 * analysis can be invoked with commandline args. If any of the arguments are missing the hardcoded ones are taken.
	 * Example:
	 * "-runDir /path/to/run/directory -runId run-id -modes car,pt,bike,walk"
	 *
	 * @param args Command Line args. Possible parameters are -runDir, -runId, -modes
	 */
	public static void main(String[] args) {

		Args arguments = new Args();
		JCommander.newBuilder().addObject(arguments).build().parse(args);

		final String runDirectory = arguments.runDirectory.isEmpty() ? "/path-to-run-directory/" : arguments.runDirectory;
		final String runId = arguments.runId.isEmpty() ? "run-id" : arguments.runId;
		final String modesString = arguments.modes.isEmpty() ? "car,pt" : arguments.modes;

		Scenario scenario1 = loadScenario(runDirectory, runId);

		List<AgentFilter> filters1 = new ArrayList<>();
		AgentAnalysisFilter filter1a = new AgentAnalysisFilter("A");
		filter1a.preProcess(scenario1);
		filters1.add(filter1a);

		List<String> modes = new ArrayList<>(Arrays.asList(modesString.split(",")));

		MatsimAnalysis analysis = new MatsimAnalysis();
		analysis.setScenario1(scenario1);
		analysis.setAgentFilters(filters1);
		analysis.setModes(modes);
		analysis.run();
	}
	
	private static Scenario loadScenario(String runDirectory, String runId) {
		log.info("Loading scenario...");

		if (StringUtils.isBlank(runDirectory) || runDirectory.equals("null")) {
			return null;
		}

		if (!runDirectory.endsWith("/")) runDirectory = runDirectory + "/";

		String configFile = runDirectory + runId + ".output_config.xml";
		String networkFile = runId + ".output_network.xml.gz";
		String populationFile = runId + ".output_plans.xml.gz";

		Config config = ConfigUtils.loadConfig(configFile);

		if (config.controler().getRunId() != null) {
			if (!runId.equals(config.controler().getRunId()))
				throw new RuntimeException("Given run ID " + runId + " doesn't match the run ID given in the config file. Aborting...");
		} else {
			config.controler().setRunId(runId);
		}

		config.controler().setOutputDirectory(runDirectory);
		config.plans().setInputFile(populationFile);
		config.network().setInputFile(networkFile);
		config.vehicles().setVehiclesFile(null);
		config.transit().setTransitScheduleFile(null);
		config.transit().setVehiclesFile(null);

		return ScenarioUtils.loadScenario(config);
	}

	private static class Args {

		@Parameter(names = {"-runDir", "-d"})
		private String runDirectory = "";

		@Parameter(names = {"-runId", "-r"})
		private String runId = "";

		@Parameter(names = {"-mode", "-m"})
		private String modes = "";
	}
}
		

