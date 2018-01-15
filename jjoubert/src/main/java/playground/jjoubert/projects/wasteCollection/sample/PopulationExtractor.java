/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationExtractor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jjoubert.projects.wasteCollection.sample;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.households.Household;
import org.matsim.households.Income;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.attributeconverters.CoordConverter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

import playground.southafrica.utilities.Header;

/**
 * Class to extract the population for a specific area.
 * 
 * @author jwjoubert
 */
public class PopulationExtractor {
	final private static Logger LOG = Logger.getLogger(PopulationExtractor.class);
	private Scenario sc;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(PopulationExtractor.class.toString(), args);
		String populationFolder = args[0];
		String shapefile = args[1];
		String ouputFile = args[2];
		
		PopulationExtractor pe = new PopulationExtractor(populationFolder);
		Geometry g = pe.parseShapefile(shapefile);
		List<String> observations = pe.extract(g);
		pe.printObservationsToFile(observations, ouputFile);
		
		Header.printFooter();
	}
	
	public Geometry parseShapefile(String shapefile) {
		LOG.info("Parsing shapefile from " + shapefile);
		Geometry g = null;
		
		ShapeFileReader sfr = new ShapeFileReader();
		sfr.readFileAndInitialize(shapefile);
		Collection<SimpleFeature> features = sfr.getFeatureSet();
		if(features.size() > 1) {
			LOG.warn("There are " + features.size() + " features. Only using the first.");
		}
		SimpleFeature feature = features.iterator().next();
		Object o = feature.getDefaultGeometry();
		if(o instanceof MultiPolygon) {
			g = (Geometry) o;
		} else {
			LOG.warn("First feature is not of type MultiPolygon, but " + o.getClass().toString());
		}
		
		LOG.info("Done parsing shapefile.");
		return g;
	}
	
	public PopulationExtractor(String folder) {
		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(folder + "population.xml.gz");
		config.plans().setInputPersonAttributeFile(folder + "populationAttributes.xml.gz");
		config.households().setInputFile(folder + "households.xml.gz");
		this.sc = ScenarioUtils.loadScenario(config );

		ObjectAttributesXmlReader oar = new ObjectAttributesXmlReader(sc.getHouseholds().getHouseholdAttributes());
		oar.putAttributeConverter(Coord.class, new CoordConverter());
		oar.readFile(folder + "householdAttributes.xml.gz");
	}
	
	
	public List<String> extract(Geometry g) {
		LOG.info("Extracting the population for the given geometry...");
		GeometryFactory gf = new GeometryFactory();
		int numberOfHouseholds = 0;
		int numberOfPersons = 0;
		
		List<String> observations = new ArrayList<String>();
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84_SA_Albers, TransformationFactory.WGS84);
		
		Geometry envelope = g.getEnvelope();
		Counter counter = new Counter("  household # ");
		for(Id<Household> hhId : this.sc.getHouseholds().getHouseholds().keySet()) {
			Coord c = (Coord) sc.getHouseholds().getHouseholdAttributes().getAttribute(hhId.toString(), "homeCoord");
			Point p = gf.createPoint(new Coordinate(c.getX(), c.getY()));
			
			if(envelope.covers(p)) {
				if(g.covers(p)){
					Household household = sc.getHouseholds().getHouseholds().get(hhId);
					/* Household is inside the area. */
					numberOfHouseholds++;
					numberOfPersons += household.getMemberIds().size();
					
					Income income = household.getIncome();
					double incomeValue = 0.0;
					if(income != null) {
						incomeValue = income.getIncome();
					}
					
					Coord cWgs = ct.transform(c);
					
					for(Id<Person> id : household.getMemberIds()) {
						Person person = sc.getPopulation().getPersons().get(id);
						String gender = person.getAttributes().getAttribute("sex").toString();
						int age = Integer.parseInt(person.getAttributes().getAttribute("age").toString());
						boolean isEmployed = Boolean.parseBoolean(person.getAttributes().getAttribute("employed").toString());
						
						String s = String.format("%s,%.6f,%.6f,%.0f,%s,%d,%s,%s", 
								hhId.toString(), cWgs.getX(), cWgs.getY(), incomeValue,
								id.toString(), age, gender, isEmployed);
						observations.add(s);
					}
				}
			}
			
			counter.incCounter();
		}
		counter.printCounter();
		
		LOG.info("Done extracting the population (" + numberOfHouseholds + " households; " + numberOfPersons + " persons)");
		return observations;
	}
	
	public void printObservationsToFile(List<String> observations, String file) {
		LOG.info("Printing " + observations.size() + " observations to " + file);
		
		BufferedWriter bw = IOUtils.getBufferedWriter(file);
		try {
			bw.write("hhId,lon,lat,income,pId,age,gender,isEmployed");
			bw.newLine();
			
			for(String s : observations) {
				bw.write(s);
				bw.newLine();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + file);
		} finally {
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + file);
			}
		}
		
		
		LOG.info("Done printing to file.");
	}

}
