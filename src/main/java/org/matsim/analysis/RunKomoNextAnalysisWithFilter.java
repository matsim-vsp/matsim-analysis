package org.matsim.analysis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.analysis.modalSplitUserType.ModeAnalysis;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;

public class RunKomoNextAnalysisWithFilter {

	private static final Logger log = Logger.getLogger(RunKomoNextAnalysisWithFilter.class);

	public static void main(String[] args) throws IOException {

		final String runDirectory = "C:/Users/cluac/MATSimScenarios/Dusseldorf/output/S211";
		final String analysisOutputDirectory = runDirectory + "/modeAnalysisResults";
		final String modesString = "car,pt,walk,bike,ride";
		final String analysisAreaShapeFile = "C:\\Users\\cluac\\MATSimScenarios\\Dusseldorf\\DusseldorfAreaShapeFile\\duesseldorf-area.shp";
		final String scenarioCRS = "EPSG:25832";
		final AnalysisMainModeIdentifier mainModeIdentifier = new DefaultAnalysisMainModeIdentifier();

		Scenario scenario = loadScenario(runDirectory, scenarioCRS);

		List<AgentFilter> filters = new ArrayList<>();
		HomeLocationFilter homeLocationFilter = new HomeLocationFilter(analysisAreaShapeFile);
		TripFilter tripFilter = null;

		filters.add(homeLocationFilter);
		homeLocationFilter.analyzePopulation(scenario);

		List<String> modes = new ArrayList<>();
		for (String mode : modesString.split(",")) {
			modes.add(mode);
		}

		ModeAnalysis modeAnalysis = new ModeAnalysis(scenario, homeLocationFilter, tripFilter, mainModeIdentifier);

		modeAnalysis.run();
		writeResults(analysisOutputDirectory, scenario, modeAnalysis);

	}

	private static Scenario loadScenario(String runDirectory, String scenarioCRS) {
		log.info("Loading scenario...");

		String networkFile;
		String populationFile;
		String facilitiesFile;

		networkFile = runDirectory + "/output_network.xml.gz";
		populationFile = runDirectory + "/output_plans.xml.gz";
		facilitiesFile = runDirectory + "/output_facilities.xml.gz";

		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem(scenarioCRS);
		config.controler().setOutputDirectory(runDirectory);

		config.plans().setInputFile(populationFile);
		config.network().setInputFile(networkFile);
		config.facilities().setInputFile(facilitiesFile);

		return ScenarioUtils.loadScenario(config);
	}

	private static void createDirectory(String directory) {
		File file = new File(directory);
		file.mkdirs();
	}

	private static void writeResults(String analysisOutputDirectory, Scenario scenario, ModeAnalysis modeAnalysis) {
		String modeAnalysisOutputDirectory = analysisOutputDirectory + "/";
		createDirectory(modeAnalysisOutputDirectory);

		modeAnalysis.writeModeShares(modeAnalysisOutputDirectory);
		modeAnalysis.writeTripRouteDistances(modeAnalysisOutputDirectory);
		modeAnalysis.writeTripEuclideanDistances(modeAnalysisOutputDirectory);

		// mode share for different distance groups
		final List<Tuple<Double, Double>> distanceGroups1 = new ArrayList<>();
		distanceGroups1.add(new Tuple<>(0., 1000.));
		distanceGroups1.add(new Tuple<>(1000., 3000.));
		distanceGroups1.add(new Tuple<>(3000., 5000.));
		distanceGroups1.add(new Tuple<>(5000., 10000.));
		distanceGroups1.add(new Tuple<>(10000., 999999999999.));
		modeAnalysis.writeTripRouteDistances(modeAnalysisOutputDirectory + "DistanceGroup_1-3-5-10", distanceGroups1);
		modeAnalysis.writeTripEuclideanDistances(modeAnalysisOutputDirectory + "DistanceGroup_1-3-5-10",
				distanceGroups1);

		final List<Tuple<Double, Double>> distanceGroups2 = new ArrayList<>();
		distanceGroups2.add(new Tuple<>(0., 1000.));
		distanceGroups2.add(new Tuple<>(1000., 2000.));
		distanceGroups2.add(new Tuple<>(2000., 3000.));
		distanceGroups2.add(new Tuple<>(3000., 4000.));
		distanceGroups2.add(new Tuple<>(4000., 5000.));
		distanceGroups2.add(new Tuple<>(5000., 6000.));
		distanceGroups2.add(new Tuple<>(6000., 7000.));
		distanceGroups2.add(new Tuple<>(7000., 8000.));
		distanceGroups2.add(new Tuple<>(8000., 9000.));
		distanceGroups2.add(new Tuple<>(9000., 999999999999.));
		modeAnalysis.writeTripRouteDistances(modeAnalysisOutputDirectory + "DistanceGroup_1-2-3-4.", distanceGroups2);
		modeAnalysis.writeTripEuclideanDistances(modeAnalysisOutputDirectory + "DistanceGroup_1-2-3-4.",
				distanceGroups2);

	}

}
