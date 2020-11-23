package org.matsim.analysis;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

public class HomeLocationFilter implements AgentFilter {
	private static final Logger log = Logger.getLogger(HomeLocationFilter.class);

	private Set<Person> personsToRemove = new HashSet<>();
	private String homeActivityTypePrefix = "home";
	private final Geometry analysisArea;

	public HomeLocationFilter(String analysisAreaShapeFile) {
		Collection<SimpleFeature> features = getFeatures(analysisAreaShapeFile);

		if (features.size() < 1) {
			throw new RuntimeException("There is no feature (zone) in the shape file. Aborting...");
		}
		analysisArea = (Geometry) features.iterator().next().getDefaultGeometry();
		if (features.size() > 1) {
			for (SimpleFeature simpleFeature : features) {
				Geometry subArea = (Geometry) simpleFeature.getDefaultGeometry();
				analysisArea.union(subArea);
			}
		}
	}

	@Override
	public boolean considerAgent(Person person) {
		if (personsToRemove.contains(person)) {
			return false;
		}
		return true;
	}

	@Override
	public String toFileName() {
		return "homeLocationFilter";
	}

	public void analyzePopulation(Scenario scenario) {
		if (scenario != null) {
			for (Person person : scenario.getPopulation().getPersons().values()) {
				// identify home location (usually the first activity)
				Activity firstActivity = (Activity) person.getSelectedPlan().getPlanElements().get(0);
				Coord homeCoord = firstActivity.getCoord();

				// check if home location is within the analysis area
				// (Note: If the first activity is not home activity, we also don't consider
				// that person)
				Point point = MGC.coord2Point(homeCoord);
				if (!point.within(analysisArea) || !firstActivity.getType().startsWith(homeActivityTypePrefix)) {
					personsToRemove.add(person);
				}
			}
			
			log.info("There are " + personsToRemove.size() + " persons to be removed from analysis");
			log.info("The total population size is " + scenario.getPopulation().getPersons().values().size());
			
		} else {
			throw new RuntimeException("The scenario does not exist. Aborting...");
		}

	}

	private Collection<SimpleFeature> getFeatures(String shapeFile) {
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
			return features;
		} else {
			return null;
		}
	}

}
