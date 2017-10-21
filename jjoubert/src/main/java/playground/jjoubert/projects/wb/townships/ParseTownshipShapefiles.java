/* *********************************************************************** *
 * project: org.matsim.*
 * ParseTownshipShapefiles.java
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
package playground.jjoubert.projects.wb.townships;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.households.Household;
import org.matsim.households.HouseholdsReaderV10;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.attributeconverters.CoordConverter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

/**
 *
 * @author jwjoubert
 */
public class ParseTownshipShapefiles {
	final private static Logger LOG = Logger.getLogger(ParseTownshipShapefiles.class);
	private final static GeometryFactory gf = new GeometryFactory(); 
	private static Map<String, MultiPolygon> geometryMap;
	private static Map<SyntheticPopulation, String[]> townships;
	private static String populationFolder;
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ParseTownshipShapefiles.class.toString(), args);
		String shapefile = args[0];
		populationFolder = args[1];
		populationFolder += populationFolder.endsWith("/") ? "" : "/";
		String output = args[2];
		
		/* Set up output file. */
		File outputFile= new File(output);
		if(outputFile.exists()) {
			LOG.warn("The output file exists and will be overwritten!!");
			LOG.warn(outputFile.getAbsoluteFile());
			FileUtils.delete(outputFile);
		}
		BufferedWriter bw = IOUtils.getAppendingBufferedWriter(output);
		try {
			bw.write("Population,Township,Employed,UnemployedFemale,UnemployedMale");
			bw.newLine();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + output);
		} finally {
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + output);
			}
		}
		
		geometryMap = parseShapefile(shapefile);
		townships = getTownships();
		
		for(SyntheticPopulation pop : townships.keySet()) {
			if(pop.equals(SyntheticPopulation.BUFFALO_CITY)) {
			}
			processPopulation(pop, outputFile.getAbsolutePath());
		}
		
		Header.printFooter();
	}
	
	
	private static void processPopulation(SyntheticPopulation population, String output) {
		String[] sa = townships.get(population);
		
		Map<String, Geometry> townshipGeometries = new TreeMap<>();
		Map<String, List<Id<Household>>> householdsMap = new TreeMap<>();
		for(String township : sa) {
			townshipGeometries.put(township, geometryMap.get(township));
			householdsMap.put(township, new ArrayList<>());
		}
		
		File populationFile = new File(populationFolder + population.folder + "/population.xml.gz");
		LOG.info("-----------------------------------------------");
		LOG.info("==> Popualtion exists ("+ population.folder + "): " + populationFile.exists());
		LOG.info("-----------------------------------------------");
		File populationAttrFile = new File(populationFolder + population.folder + "/populationAttributes.xml.gz");
		File householdsFile = new File(populationFolder + population.folder + "/households.xml.gz");
		File householdAttrFile = new File(populationFolder + population.folder + "/householdAttributes.xml.gz");
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(sc).readFile(populationFile.getAbsolutePath());
		new ObjectAttributesXmlReader(sc.getPopulation().getPersonAttributes()).readFile(populationAttrFile.getAbsolutePath());
		new HouseholdsReaderV10(sc.getHouseholds()).readFile(householdsFile.getAbsolutePath());
		ObjectAttributesXmlReader oar = new ObjectAttributesXmlReader(sc.getHouseholds().getHouseholdAttributes());
		oar.putAttributeConverter(Coord.class, new CoordConverter());
		oar.readFile(householdAttrFile.getAbsolutePath());
		
		Counter counter = new Counter("  households # ");
		for(Id<Household> hhId : sc.getHouseholds().getHouseholds().keySet()) {
//			int age = Integer.parseInt(sc.getPopulation().getPersonAttributes().getAttribute(pid.toString(), "age").toString());
//			String gender = sc.getPopulation().getPersonAttributes().getAttribute(pid.toString(), "age").toString();
			Coord homeCoord = (Coord) sc.getHouseholds().getHouseholdAttributes().getAttribute(hhId.toString(), "homeCoord");
			
			Point p = gf.createPoint(new Coordinate(homeCoord.getX(), homeCoord.getY()));
			
			boolean found = false;
			Iterator<String> townshipIterator = householdsMap.keySet().iterator();
			while(!found && townshipIterator.hasNext()) {
				String township = townshipIterator.next();
				Geometry geometry = townshipGeometries.get(township);
				Geometry envelope = geometry.getEnvelope();
				
				if(envelope.covers(p)) {
					if(geometry.covers(p)) {
						householdsMap.get(township).add(hhId);
						found = true;
					}
				}
			}
			counter.incCounter();
		}
		counter.printCounter();
		
		/* Report number of households in each township. */
		for(String township : householdsMap.keySet()) {
			List<Id<Household>> households = householdsMap.get(township); 
			LOG.info(township + ": " + households.size());
			int employed = 0;
			int unemployedMale = 0;
			int unemployedFemale = 0;

			/* Process those households. */
			for(Id<Household> hhId : households) {
				Household household = sc.getHouseholds().getHouseholds().get(hhId);
				for(Id<Person> id : household.getMemberIds()) {
					Person person = sc.getPopulation().getPersons().get(id);
					boolean isEmployed = (boolean) person.getAttributes().getAttribute("employed");
					if(isEmployed) {
						employed++;
					} else if(isEmployable(person)) {
						String gender = sc.getPopulation().getPersonAttributes().getAttribute(id.toString(), "gender").toString();
						if(gender.equalsIgnoreCase("Male")) {
							unemployedMale++;
						} else {
							unemployedFemale++;
						}
					};
				}
			}
			LOG.info("########### Employed: " + employed);
			LOG.info("## Unemployed female: " + unemployedFemale);
			LOG.info("#### Unemployed male: " + unemployedMale);
			
			/* Write to file. */
			BufferedWriter bw = IOUtils.getAppendingBufferedWriter(output);
			String s = String.format("%s,%s,%d,%d,%d\n", 
					population.folder, township,
					employed, unemployedFemale, unemployedMale);
			try {
				bw.write(s);
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot write to " + output);
			} finally {
				try {
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException("Cannot close " + output);
				}
			}
		}
	}
	
	private static boolean isEmployable(Person person) {
		boolean employable = false;
		int age = (int) person.getAttributes().getAttribute("age");
		if(age > 18 && age <= 65) {
			employable = true;
		}
		
		return employable;
	}
	
	
	
	private static Map<String, MultiPolygon> parseShapefile(String shapefile) {
		LOG.info("Parsing shapefile...");
		
		Map<String, MultiPolygon> geometryMap = new TreeMap<>();
		
		ShapeFileReader sfr = new ShapeFileReader();
		sfr.readFileAndInitialize(shapefile);
		
		Iterator<SimpleFeature> features = sfr.getFeatureSet().iterator();
		int i = 0;
		while(features.hasNext()) {
			SimpleFeature feature = features.next();
			
			Object oName = feature.getAttribute("MP_NAME");
			Object o = feature.getDefaultGeometry();
			MultiPolygon mp = null;
			if(o instanceof MultiPolygon) {
				mp = (MultiPolygon) o;
				geometryMap.put(oName.toString(), mp);
			} else{
				LOG.warn("Geometry is not of type multipolygon.");
			}
			
			i++;
		}
		LOG.info("Total number of features: " + i);
		
		LOG.info("-----------------------------------------------");
		for(String s : geometryMap.keySet()) {
			boolean b = isAssociatedWithPopulation(s);
			if(b) { 	LOG.info(s + " --: " + b); }
			else  { LOG.warn(s + " --: " + b); }
		}
		LOG.info("-----------------------------------------------");
		LOG.info("Done parsing shapefile.");
		return geometryMap;
	}
	
	private static boolean isAssociatedWithPopulation(String township) {
		boolean result = false;
		
		Map<SyntheticPopulation, String[]> map = getTownships();
		Iterator<SyntheticPopulation> popIterator = map.keySet().iterator();
		while(!result & popIterator.hasNext()) {
			
			String[] sa = map.get(popIterator.next());
			int i = 0;
			while(!result & i < sa.length) {
				if(sa[i].equalsIgnoreCase(township)) {
					result = true;
				} else {
					i++;
				}
			}
		}
		
		return result;
	}
	
	private static Map<SyntheticPopulation, String[]> getTownships(){
		Map<SyntheticPopulation, String[]> map = new TreeMap<ParseTownshipShapefiles.SyntheticPopulation, String[]>();
		String[] sa1 = {"Mdantsane"};
		map.put(SyntheticPopulation.BUFFALO_CITY, sa1 );
		String[] sa2 = {"Gugulethu", "Khayelitsha", "Mitchells Plain"};
		map.put(SyntheticPopulation.CAPE_TOWN, sa2);
		String[] sa3 = {"Umlazi", "KwaMashu", "Ntuzuma", "Mpumalanga"};
		map.put(SyntheticPopulation.ETHEKWINI, sa3);
		String[] sa4 = {"Evaton", "Sebokeng", "Sharpeville", "Kagiso", "Tembisa", "Etwatwa", "Daveyton", 
				"Kwa-Thema", "Tsakane", "Duduza", "Tokoza", "Katlehong", "Vosloorus", "Diepsloot", 
				"Ivory Park", "Alexandra", "Soweto", "Orange Farm", "Soshanguve", "Mabopane", "Ga-Rankuwa",
				"Mamelodi", "Saulsville"};
		map.put(SyntheticPopulation.GAUTENG, sa4);
		String[] sa5 = {"Thabong", "Mangaung", "Botshabelo"};
		map.put(SyntheticPopulation.MANGAUNG, sa5);
		String[] sa6 = {"Govan Mbeki", "Mbombela"};
		map.put(SyntheticPopulation.MBOMBELA, sa6);
		String[] sa7 = {"iBhayi", "KwaNobuhle", "Motherwell"};
		map.put(SyntheticPopulation.NMBM, sa7);
		String[] sa8 = {"Seshego", "Mankweng"};
		map.put(SyntheticPopulation.POLOKWANE, sa8);
		
		return map;
	}
	
	private enum SyntheticPopulation{
		BUFFALO_CITY("BuffaloCity"),
		CAPE_TOWN("CapeTown"),
		ETHEKWINI("eThekwini"),
		GAUTENG("Gauteng"),
		MANGAUNG("Mangaung"),
		MBOMBELA("Mbombela"),
		NMBM("NelsonMandelaBay"),
		POLOKWANE("Polokwane"),
		RUSTENBUTG("Rustenburg");
		
		private final String folder;
		
		SyntheticPopulation(String folder) {
			this.folder = folder;
		}
	}

}
