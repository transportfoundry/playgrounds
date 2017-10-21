/* *********************************************************************** *
 * project: org.matsim.*
 * GHitgePopulationExtractor.java
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
package playground.southafrica.population.analysis;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.households.Household;
import org.matsim.households.HouseholdsReaderV10;
import org.matsim.households.Income;

import playground.southafrica.utilities.Header;

/**
 * Class to convert the survey population of the City of Cape Town into a
 * flat file so PhD candidate Gerhard Hitge can work in MS Excel on it.
 * 
 * @author jwjoubert
 */
public class GHitgePopulationExtractor {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(GHitgePopulationExtractor.class.toString(), args);
		
		String surveyFolder = args[0];
		surveyFolder += surveyFolder.endsWith("/") ? "" : "/";
		String output = args[1];
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(sc).readFile(surveyFolder + "populationCleaned.xml.gz");
		new HouseholdsReaderV10(sc.getHouseholds()).readFile(surveyFolder + "households.xml.gz");
		
		BufferedWriter bw = IOUtils.getBufferedWriter(output);
		try{
			for(Household hh : sc.getHouseholds().getHouseholds().values()){
				for(Id<Person> pid : hh.getMemberIds()){
					bw.write(pid.toString());
					bw.write(",");
					double incomeValue = 0.0;
					Income income = hh.getIncome();
					if(income != null){
						incomeValue = income.getIncome();
					}
					bw.write(String.valueOf(incomeValue));
					Plan plan = sc.getPopulation().getPersons().get(pid).getSelectedPlan();
					for(PlanElement pe : plan.getPlanElements()){
						bw.write(",");
						if(pe instanceof Activity){
							Activity act = (Activity) pe;
							bw.write(act.getType());
						} else if(pe instanceof Leg){
							Leg leg = (Leg) pe;
							bw.write(leg.getMode());
						} else{
							throw new RuntimeException("Don't know what to do with PlanElement type!");
						}
					}
					bw.newLine();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + bw.toString());
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + bw.toString());
			}
		}
		
		Header.printFooter();
	}

}
