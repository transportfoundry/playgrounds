/* *********************************************************************** *
 * project: org.matsim.*
 * UCSBStops2PlansConverter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.dziemke.cemdapMatsimCadyts.cemdap2matsim;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkReaderMatsimV1;
import org.matsim.core.population.algorithms.XY2Links;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.opengis.feature.simple.SimpleFeature;

import playground.dziemke.utils.LogToOutputSaver;


/**
 * @author dziemke
 * based on "ucsb\demand\UCSBStops2PlansConverter.java"
 */
public class CemdapStops2MatsimPlansConverter {
	private static final Logger log = Logger.getLogger(CemdapStops2MatsimPlansConverter.class);
	
	// Parameters
	private int numberOfFirstCemdapOutputFile = -1;
	private int numberOfPlans = -1;
	private boolean addStayHomePlan = false;
	
	// Input and output
	private String outputDirectory;
	private String tazShapeFile;
	private String networkFile;
	private String cemdapDataRoot;
	private String cemdapStopsFilename = "stops.out";
	
	public static void main(String[] args) {
		int numberOfFirstCemdapOutputFile = 87;
		int numberOfPlans = 3;
		boolean addStayHomePlan = true;
		
		int numberOfPlansFile = 34;
		String outputDirectory = "../../../shared-svn/projects/cemdapMatsimCadyts/scenario/cemdap2matsim/" + numberOfPlansFile + "/";
		String tazShapeFile = "../../../shared-svn/projects/cemdapMatsimCadyts/scenario/shapefiles/gemeindenLOR_DHDN_GK4.shp";
		String networkFile = "../../../shared-svn/studies/countries/de/berlin/counts/iv_counts/network.xml";
		String cemdapDataRoot = "../../../shared-svn/projects/cemdapMatsimCadyts/scenario/cemdap_output/";
		
		CemdapStops2MatsimPlansConverter converter = new CemdapStops2MatsimPlansConverter(
				tazShapeFile, 
				networkFile, 
				cemdapDataRoot);
		
		converter.setOutputDirectory(outputDirectory);
		converter.setNumberOfFirstCemdapOutputFile(numberOfFirstCemdapOutputFile);
		converter.setNumberOfPlans(numberOfPlans);
		converter.setAddStayHomePlan(addStayHomePlan);
		
		try {
			converter.convert();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public CemdapStops2MatsimPlansConverter(String tazShapeFile, String networkFile, String cemdapDataRoot) {
		this.tazShapeFile = tazShapeFile;
		this.networkFile = networkFile;
		this.cemdapDataRoot = cemdapDataRoot;
	}
	
	public void convert() throws IOException {
		if (!areDependenciesSet()) return;
		LogToOutputSaver.setOutputDirectory(outputDirectory);
		// find respective stops file
		Map<Integer, String> cemdapStopsFilesMap = new HashMap<>();
//		Map<Integer, String> cemdapToursFilesMap = new HashMap<>();
//		Map<Integer, Map<String,String>> mapOfTourAttributesMaps = new HashMap<Integer, Map<String,String>>();
		for (int i=0; i<numberOfPlans; i++) {
			int numberOfCurrentInputFile = numberOfFirstCemdapOutputFile + i;
			String cemdapStopsFile = cemdapDataRoot + numberOfCurrentInputFile + "/" + cemdapStopsFilename;
//			String cemdapToursFile = cemdapOutputRoot + numberOfCurrentInputFile + "/tours.out";
//			Map<String,String> tourAttributesMap = new HashMap<String,String>();
			cemdapStopsFilesMap.put(i, cemdapStopsFile);
//			cemdapToursFilesMap.put(i, cemdapToursFile);
//			mapOfTourAttributesMaps.put(i, tourAttributesMap);
		}
	
		// create ObjectAttrubutes for each agent
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Map<Integer, ObjectAttributes> personObjectAttributesMap = new HashMap<Integer, ObjectAttributes>();
		for (int i=0; i<numberOfPlans; i++) {
			ObjectAttributes personObjectAttributes = new ObjectAttributes();
			personObjectAttributesMap.put(i, personObjectAttributes);
		}
		
		// read in network
		new NetworkReaderMatsimV1(scenario.getNetwork()).readFile(networkFile);
		
		// write all (geographic) features of planning area to a map
		Map<String,SimpleFeature> combinedFeatures = new HashMap<String, SimpleFeature>();
		for (SimpleFeature feature: ShapeFileReader.getAllFeatures(tazShapeFile)) {
			Integer schluessel = Integer.parseInt((String) feature.getAttribute("NR"));
			String id = schluessel.toString();
			combinedFeatures.put(id,feature);
		}

//		// parse cemdap tours file
//		for (int i=0; i<numberOfPlans; i++) {
//			new CemdapToursParser().parse(cemdapToursFilesMap.get(i), mapOfTourAttributesMaps.get(i));
//		}
		
		// parse cemdap stops file
		for (int i=0; i<numberOfPlans; i++) {
			new CemdapStopsParser().parse(cemdapStopsFilesMap.get(i), i, //mapOfTourAttributesMaps.get(i), 
					scenario,
					personObjectAttributesMap.get(i), false);
			new Feature2Coord().assignCoords(scenario, i, personObjectAttributesMap.get(i), combinedFeatures);
		}
				
		// if applicable, add a stay-home plan
		if (addStayHomePlan == true) {
			int planNumber = numberOfPlans; // Thus, number of stay-home plan is one more than number of last plan.
			new CemdapStopsParser().parse(cemdapStopsFilesMap.get(0), planNumber, //mapOfTourAttributesMaps.get(0), 
					scenario,
					personObjectAttributesMap.get(0), true);
			new Feature2Coord().assignCoords(scenario, planNumber, personObjectAttributesMap.get(0), combinedFeatures);
		}
			
		// check if number of plans that each agent has is correct
		int counter = 0;
		int expectedNumberOfPlans;
		if (addStayHomePlan == true) {
			expectedNumberOfPlans = numberOfPlans + 1;
		} else {
			expectedNumberOfPlans = numberOfPlans;
		}
		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (person.getPlans().size() < expectedNumberOfPlans) {
				log.warn("Person with ID=" + person.getId() + " has less than " + expectedNumberOfPlans + " plans");
			}
			if (person.getPlans().size() > expectedNumberOfPlans) {
				log.warn("Person with ID=" + person.getId() + " has more than " + expectedNumberOfPlans + " plans");
				}
			if (person.getPlans().size() == expectedNumberOfPlans) {
				counter++;
			}
		}
		log.info(counter + " persons have " + expectedNumberOfPlans + " plans.");
		
		// assign activities to links
		new XY2Links((MutableScenario)scenario).run(scenario.getPopulation());
		
		// write population file
		new File(outputDirectory).mkdir();
		new PopulationWriter(scenario.getPopulation(), null).write(outputDirectory + "plans.xml.gz");
		//new ObjectAttributesXmlWriter(personObjectAttributesMap.get(0)).writeFile(outputBase+"personObjectAttributes0.xml.gz");
	}

	private boolean areDependenciesSet() {
		if (numberOfFirstCemdapOutputFile == -1) {
			log.warn("NumberOfFirstCemdapOutputFile not set.");
			return false;
		}
		if (numberOfPlans == -1) {
			log.warn("NumberOfPlans not set.");
			return false;
		}
		if (!outputDirectory.isEmpty()) {
			log.warn("OutputDirectory is empty.");
			return false;
		}
		if (!cemdapStopsFilename.isEmpty()) {
			log.warn("CemdapStopsFilename is empty.");
			return false;
		}
		return true;
	}

	public int getNumberOfFirstCemdapOutputFile() {
		return numberOfFirstCemdapOutputFile;
	}

	public void setNumberOfFirstCemdapOutputFile(int numberOfFirstCemdapOutputFile) {
		this.numberOfFirstCemdapOutputFile = numberOfFirstCemdapOutputFile;
	}

	public int getNumberOfPlans() {
		return numberOfPlans;
	}

	public void setNumberOfPlans(int numberOfPlans) {
		this.numberOfPlans = numberOfPlans;
	}

	public boolean isAddStayHomePlan() {
		return addStayHomePlan;
	}

	public void setAddStayHomePlan(boolean addStayHomePlan) {
		this.addStayHomePlan = addStayHomePlan;
	}

	public String getOutputDirectory() {
		return outputDirectory;
	}

	public void setOutputDirectory(String outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	public String getCemdapStopFilename() {
		return cemdapStopsFilename;
	}

	public void setCemdapStopFilename(String cemdapStopFilename) {
		this.cemdapStopsFilename = cemdapStopFilename;
	}
}