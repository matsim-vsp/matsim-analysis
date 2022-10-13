package org.matsim.analysis.detailedPersonTripAnalysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.DefaultAnalysisMainModeIdentifier;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author gthunig, ikaddoura
 * 
 * This class tests the basic analysis provided by the BasicPersonTripAnalysisHandler. 
 * In each test, a small events file is analyzed, and a scenario is created using the ForkNetworkCreator.
 * 
 */
public class BasicPersonTripAnalysisTest {
	
	private static final Logger log = LogManager.getLogger(BasicPersonTripAnalysisTest.class);

	private static final boolean printResults = true;
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	/**
	 * Scenario: 4 Persons
	 *  1.Person: has 2 trips with payments
	 *  2.Person: has 1 trip with payment
	 *  3.Person: has 1 trip with a payment-amount of "0"
	 *  4.Person: has no payments
	 */
	@Test
	public void testPersonMoney() {
		String eventsFile = utils.getInputDirectory() + "testPersonMoneyEvents.xml";
		
		Scenario scenario = createScenario(4);
		BasicPersonTripAnalysisHandler basicHandler = analyseScenario(eventsFile, scenario);
	
		Assert.assertEquals("Unexpected total payment: ", 80.0, basicHandler.getTotalPayments(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Unexpected payment: ", 20.0, 
				basicHandler.getPersonId2tripNumber2payment().get(Id.create(0, Person.class)).get(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Unexpected payment: ", 30.0, 
				basicHandler.getPersonId2tripNumber2payment().get(Id.create(0, Person.class)).get(2), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Unexpected payment: ", 30.0, 
				basicHandler.getPersonId2tripNumber2payment().get(Id.create(1, Person.class)).get(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Unexpected payment: ", 0.0, 
				basicHandler.getPersonId2tripNumber2payment().get(Id.create(2, Person.class)).get(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Unexpected payment: ", 0.0, 
				basicHandler.getPersonId2tripNumber2payment().get(Id.create(3, Person.class)).get(1), MatsimTestUtils.EPSILON);
		
		if (printResults) printResults(basicHandler, scenario);
	}
	
	/**
	 * Scenario: 3 Persons
	 *  1.Person: has 2 trips, stucks within the 2. trip
	 *  2.Person: has 1 trip, stucks after the trip
	 *  3.Person: has 1 trip, does not stuck
	 */
	@Test
	public void testPersonStuck() {
		String eventsFile = utils.getInputDirectory() + "testPersonStuckEvents.xml";
		
		Scenario scenario = createScenario(3);
		BasicPersonTripAnalysisHandler basicHandler = analyseScenario(eventsFile, scenario);
	
		// Person 0
		Assert.assertTrue(basicHandler.getPersonId2tripNumber2stuckAbort().get(Id.create(0, Person.class)).get(2));
		Assert.assertNull(basicHandler.getPersonId2tripNumber2stuckAbort().get(Id.create(0, Person.class)).get(1));
		Assert.assertEquals("Unexpected travelTime: ", 78000.0, 
				basicHandler.getPersonId2tripNumber2travelTime().get(Id.create(0, Person.class)).get(2), MatsimTestUtils.EPSILON);
		// Person 1
		Assert.assertTrue(basicHandler.getPersonId2tripNumber2stuckAbort().get(Id.create(1, Person.class)).get(1));
		Assert.assertEquals("Unexpected travelTime: ", Double.POSITIVE_INFINITY, 
				basicHandler.getPersonId2tripNumber2travelTime().get(Id.create(1, Person.class)).get(1), MatsimTestUtils.EPSILON);
		// Person 2
		Assert.assertNull(basicHandler.getPersonId2tripNumber2stuckAbort().get(Id.create(2, Person.class)));
		
		if (printResults) printResults(basicHandler, scenario);
	}
	
	/**
	 * Scenario: 3 Persons
	 * 	1.Person: 2 different trips
	 *  2.Person: 1 trips
	 *  3.Person: activity "home" ends and activity "work" starts on the same link
	 */
	@Test
	public void testVariousTripCounts() {
		
		String eventsFile = utils.getInputDirectory() + "testVariousTripCountsEvents.xml";
		
		Scenario scenario = createScenario(3);
		BasicPersonTripAnalysisHandler basicHandler = analyseScenario(eventsFile, scenario);
	
		// Person 0
		Assert.assertEquals("Unexpected tripDistance on trip 1 from Person 0: ", 4000.0, 
				basicHandler.getPersonId2tripNumber2tripDistance().get(Id.create(0, Person.class)).get(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Unexpected tripDistance on trip 2 from Person 0: ", 4000.0, 
				basicHandler.getPersonId2tripNumber2tripDistance().get(Id.create(0, Person.class)).get(2), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Unexpected departureTime on trip 1 from Person 0: ", 28800.0, 
				basicHandler.getPersonId2tripNumber2departureTime().get(Id.create(0, Person.class)).get(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Unexpected arrivalTime on trip 1 from Person 0: ", 29250.0, 
				basicHandler.getPersonId2tripNumber2arrivalTime().get(Id.create(0, Person.class)).get(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Unexpected departureTime on trip 2 from Person 0: ", 30000.0, 
				basicHandler.getPersonId2tripNumber2departureTime().get(Id.create(0, Person.class)).get(2), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Unexpected arrivalTime on trip 2 from Person 0: ", 30450.0, 
				basicHandler.getPersonId2tripNumber2arrivalTime().get(Id.create(0, Person.class)).get(2), MatsimTestUtils.EPSILON);
		// Person 1
		Assert.assertEquals("Unexpected tripDistance on trip 1 from Person 1: ", 4000.0, 
				basicHandler.getPersonId2tripNumber2tripDistance().get(Id.create(1, Person.class)).get(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Unexpected departureTime on trip 1 from Person 1: ", 30000.0, 
				basicHandler.getPersonId2tripNumber2departureTime().get(Id.create(1, Person.class)).get(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Unexpected arrivalTime on trip 1 from Person 1: ", 30950.0, 
				basicHandler.getPersonId2tripNumber2arrivalTime().get(Id.create(1, Person.class)).get(1), MatsimTestUtils.EPSILON);
		// Person 2
		Assert.assertEquals("Unexpected tripDistance on trip 1 from Person 2: ", 0.0, 
				basicHandler.getPersonId2tripNumber2tripDistance().get(Id.create(2, Person.class)).get(1), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("Unexpected departureTime on trip 1 from Person 2: ", 30500.0, 
				basicHandler.getPersonId2tripNumber2departureTime().get(Id.create(2, Person.class)).get(1), MatsimTestUtils.EPSILON);
		Assert.assertNull(basicHandler.getPersonId2tripNumber2arrivalTime().get(Id.create(2, Person.class)));
		
		if (printResults) printResults(basicHandler, scenario);
	}
	
	/**
	 * Scenario: 1 Person
	 * 	1.Person: 2 different trips with different vehicles
	 */
	@Test
	public void testVariousVehiclesPerPerson() {
		
		String eventsFile = utils.getInputDirectory() + "testVariousVehiclesPerPersonEvents.xml";
		
		Scenario scenario = createScenario(1);
		BasicPersonTripAnalysisHandler basicHandler = analyseScenario(eventsFile, scenario);
	
		if (printResults) printResults(basicHandler, scenario);
	}
	
	public Scenario createScenario(int personNumber) {

		Config config = ConfigUtils.createConfig();
		config.qsim().setEndTime(30 * 3600.0);
		config.qsim().setRemoveStuckVehicles(true);
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		// create Population
		for (int i = 0; i < personNumber; i++) {
			scenario.getPopulation().addPerson(scenario.getPopulation().getFactory().createPerson(Id.create(i, Person.class)));
		}
		
		ForkNetworkCreator fnc = new ForkNetworkCreator(scenario, false, false);
		fnc.createNetwork();
			
		return scenario;
	}
	
	public BasicPersonTripAnalysisHandler analyseScenario(String eventsFile, Scenario scenario) {

		final String stageActivitySubString = "interaction";
		
		BasicPersonTripAnalysisHandler basicHandler = new BasicPersonTripAnalysisHandler(stageActivitySubString, new DefaultAnalysisMainModeIdentifier());	
		basicHandler.setScenario(scenario);
		
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(basicHandler);
		
		log.info("Reading the events file...");
		MatsimEventsReader reader = new MatsimEventsReader(events);
		events.initProcessing();
		reader.readFile(eventsFile);
		events.finishProcessing();

		log.info("Reading the events file... Done.");
		
		return basicHandler;
	}
	
	public void printResults(BasicPersonTripAnalysisHandler basicHandler, Scenario scenario) {
		System.out.println("totalPayments: " + basicHandler.getTotalPayments());
		for (Person person : scenario.getPopulation().getPersons().values()) {
			System.out.println("Person Id : " + person.getId());
			System.out.println("currentTripNumber: " + basicHandler.getPersonId2currentTripNumber().get(person.getId()));
			System.out.println("distanceEnterValue: " + basicHandler.getPersonId2distanceEnterValue().get(person.getId()));
			System.out.println("tripNumber2amount: " + basicHandler.getPersonId2tripNumber2payment().get(person.getId()));
			System.out.println("tripNumber2arrivalTime: " + basicHandler.getPersonId2tripNumber2arrivalTime().get(person.getId()));
			System.out.println("tripNumber2departureTime: " + basicHandler.getPersonId2tripNumber2departureTime().get(person.getId()));
			System.out.println("tripNumber2legMode: " + basicHandler.getPersonId2tripNumber2tripMainMode().get(person.getId()));
			System.out.println("tripNumber2stuckAbort: " + basicHandler.getPersonId2tripNumber2stuckAbort().get(person.getId()));
			System.out.println("tripNumber2travelTime: " + basicHandler.getPersonId2tripNumber2travelTime().get(person.getId()));
			System.out.println("tripNumber2tripDistance: " + basicHandler.getPersonId2tripNumber2tripDistance().get(person.getId()));
		}
	}
	
	@Test
	public void testMainModeIdentification() {
		
		final String stageActivitySubString = "interaction";

		BasicPersonTripAnalysisHandler basicHandler = new BasicPersonTripAnalysisHandler(stageActivitySubString, new DefaultAnalysisMainModeIdentifier());
		Assert.assertEquals("Wrong main mode: ", "bike", basicHandler.getMainMode("bike"));
		Assert.assertEquals("Wrong main mode: ", "walk", basicHandler.getMainMode("walk-walk"));
		Assert.assertEquals("Wrong main mode: ", "pt", basicHandler.getMainMode("walk-pt-walk"));
		Assert.assertEquals("Wrong main mode: ", "car", basicHandler.getMainMode("walk-car-walk"));
		Assert.assertEquals("Wrong main mode: ", "pt", basicHandler.getMainMode("walk-pt-walk-drt-walk"));
		Assert.assertEquals("Wrong main mode: ", "pt", basicHandler.getMainMode("walk-drt2-walk-pt-walk"));
		Assert.assertEquals("Wrong main mode: ", "pt", basicHandler.getMainMode("walk-drt-walk-pt-walk-drt-walk"));

	}
	
	
}
