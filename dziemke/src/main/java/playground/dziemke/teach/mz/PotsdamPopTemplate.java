package playground.dziemke.teach.mz;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.dziemke.potsdam.population.ShapeReader;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class PotsdamPopTemplate implements Runnable {

	// private static Map<String, Geometry> zoneGeometries = new HashMap<String, Geometry>();
	private static Map<Integer, Geometry> zoneGeometries = ShapeReader.read("D:/Workspace/container/potsdam-tut/dlm_kreis.shp");

	private CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM35S);

	private Scenario scenario;

	private Population population;

	public static void main(String[] args) {
		PotsdamPopTemplate potsdamPop = new PotsdamPopTemplate();
		potsdamPop.run();
	}

	@Override
	public void run() {
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		population = scenario.getPopulation();
		generatePopulation();
		PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		populationWriter.write("D:/Workspace/container/potsdam-tut/plans.xml");
	}

	private void generatePopulation() {
		generateHomeWorkHomeTrips("P", 12054000, "B", 11000000, 20);
	}

	private void generateHomeWorkHomeTrips(String from, int fromNumber, String to, int toNumber, int quantity) {
		for (int i=0; i<quantity; ++i) {
			Geometry source = zoneGeometries.get(fromNumber);
			Geometry sink = zoneGeometries.get(toNumber);
			Person person = population.getFactory().createPerson(createId(from, to, i, TransportMode.car));
			Plan plan = population.getFactory().createPlan();
			Coord homeLocation = shoot(source);
			Coord workLocation = shoot(sink);
			plan.addActivity(createHome(homeLocation));
			plan.addLeg(createDriveLeg());
			plan.addActivity(createWork(workLocation));
			plan.addLeg(createDriveLeg());
			plan.addActivity(createHome(homeLocation));
			person.addPlan(plan);
			population.addPerson(person);
		}
	}

	private Leg createDriveLeg() {
		Leg leg = population.getFactory().createLeg(TransportMode.car);
		return leg;
	}

	private Coord shoot(Geometry zone) {
		Random r = new Random();
		Point point = getRandomPointInFeature(r, zone);
		CoordImpl coordImpl = new CoordImpl(point.getX(), point.getY());
		return coordImpl;
	}

	private Activity createWork(Coord workLocation) {
		Activity activity = population.getFactory().createActivityFromCoord("work", workLocation);
		activity.setEndTime(17*60*60);
		return activity;
	}

	private Activity createHome(Coord homeLocation) {
		Activity activity = population.getFactory().createActivityFromCoord("home", homeLocation);
		activity.setEndTime(9*60*60);
		return activity;
	}

	private static Point getRandomPointInFeature(Random rnd, Geometry g) {
		Point p = null;
		double x, y;
		do {
			x = g.getEnvelopeInternal().getMinX() + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxX() - g.getEnvelopeInternal().getMinX());
			y = g.getEnvelopeInternal().getMinY() + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxY() - g.getEnvelopeInternal().getMinY());
			p = MGC.xy2Point(x, y);
		} while (!g.contains(p));
		return p;
	}

	private Id createId(String source, String sink, int i, String transportMode) {
		return new IdImpl(transportMode + "_" + source + "_" + sink + "_" + i);
	}

}
