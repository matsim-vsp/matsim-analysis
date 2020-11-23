package org.matsim.analysis.od;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;

public class RunKomodnextODAnalysis {

	public static void main(String[] args) throws IOException {

		final String runDirectory = "C:/Users/cluac/MATSimScenarios/Dusseldorf/output/S207";
		final double scaleFactor = 0.1;
		final String scenarioCRS = "EPSG:25832";
		final String shapeFile = "C:/Users/cluac/MATSimScenarios/Dusseldorf/dusseldorfShapeFiles/duesseldorf-stadtteile.shp";
		final String[] helpLegModes = { TransportMode.transit_walk, TransportMode.non_network_walk, "access_walk",
				"egress_walk" }; // for backward compatibility
		final String stageActivitySubString = "interaction";
		final String zoneId = null;
		final String runId = null;

		final List<String> modes = new ArrayList<>();
		modes.add(TransportMode.car);

		EventsManager events = EventsUtils.createEventsManager();

		ODEventAnalysisHandler handler1 = new ODEventAnalysisHandler(helpLegModes, stageActivitySubString);
		events.addHandler(handler1);

		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(runDirectory + "/output_events.xml.gz");

		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(runDirectory + "/output_network.xml.gz");
		config.network().setInputCRS(scenarioCRS);
		config.global().setCoordinateSystem(scenarioCRS);
		Network network = ScenarioUtils.loadScenario(config).getNetwork();

		ODAnalysis odAnalysis = new ODAnalysis(runDirectory, network, runId, shapeFile, scenarioCRS, zoneId, modes,
				scaleFactor);
		odAnalysis.process(handler1);
	}

}
