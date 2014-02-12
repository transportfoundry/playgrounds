/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.julia.toi;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class FirstSimulation {
 
	static String networkFile = "input/oslo/trondheim_network.xml";
	static String cvsSplitBy = ",";
	static String outputDir = "output/oslo/";
	//static String plansFile = "input/oslo/plans_from_csv.xml";
	static String plansFile = "input/oslo/plans_from_start_og.xml";
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		/*
		 * TODO boundingbox aus den plänen auslesen
		 * mit osmosis netzwerk generieren
		 */
		

		Config config = ConfigUtils.createConfig();	
		config.addCoreModules();
		config.controler().setLastIteration(2);
		config.controler().setOutputDirectory(outputDir);
		config.controler().setWriteEventsInterval(1);
		config.controler().setWritePlansInterval(1);
		config.controler().setRoutingAlgorithmType(RoutingAlgorithmType.Dijkstra);
	
		ActivityParams home = new ActivityParams("home");
		home.setTypicalDuration(13 * 3600);
		ActivityParams work = new ActivityParams("work");
		work.setTypicalDuration(8 * 3600);
		ActivityParams other = new ActivityParams("other");
		other.setTypicalDuration(1*3600);
		config.planCalcScore().addActivityParams(home);
		config.planCalcScore().addActivityParams(work);
		config.planCalcScore().addActivityParams(other);
		
		StrategySettings reRoute = new StrategySettings(new IdImpl(1));
		reRoute.setModuleName("ReRoute");
		reRoute.setProbability(0.3);
		reRoute.setDisableAfter(17);
		
		StrategySettings change = new StrategySettings(new IdImpl(2));
		change.setModuleName("ChangeExpBeta");
		change.setProbability(0.7);
 
		config.strategy().addStrategySettings(reRoute);
		config.strategy().addStrategySettings(change);
		
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		
		//new MatsimNetworkReader(scenario).readFile("input/oslo/trondheim.xml");
		new MatsimNetworkReader(scenario).readFile(networkFile);
		
//		Network network = scenario.getNetwork();
		Population population = scenario.getPopulation();
//		PopulationFactory populationFactory = population.getFactory();


		new MatsimPopulationReader(scenario).readFile(plansFile);	
		
		Controler controler = new Controler(config);
		controler.setOverwriteFiles(true);
		controler.getConfig().controler().setMobsim("qsim");
		NetworkConfigGroup ncg = controler.getConfig().network();
		ncg.setInputFile(networkFile);
		controler.getConfig().plans().setInputFile(plansFile);
				 
		 
		controler.run();
	}

}
