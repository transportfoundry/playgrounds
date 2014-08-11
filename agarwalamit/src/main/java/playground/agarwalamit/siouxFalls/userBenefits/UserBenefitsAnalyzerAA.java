/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.siouxFalls.userBenefits;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioImpl;

import playground.vsp.analysis.modules.AbstractAnalyisModule;
import playground.vsp.analysis.modules.userBenefits.UserBenefitsAnalyzer;
import playground.vsp.analysis.modules.userBenefits.UserBenefitsCalculator;
import playground.vsp.analysis.modules.userBenefits.WelfareMeasure;

/**
 * This module calculates the logsum for each user and the sum of all user logsums in monetary units.
 * Furthermore, it analyzes users with no valid plan, that are not considered for the logsum calculation.
 * 
 * @author ikaddoura, benjamin
 *
 */
public class UserBenefitsAnalyzerAA extends AbstractAnalyisModule{
	private final static Logger log = Logger.getLogger(UserBenefitsAnalyzer.class);
	private ScenarioImpl scenario;
	private UserBenefitsCalculator userWelfareCalculator;
	
	private double allUsersLogSum;
	private int personWithNoValidPlanCnt;
	private Map<Id, Double> personId2UserWelfare;
	private Map<Id, Double> personId2MonetarizedUserWelfare;
	private WelfareMeasure welfareMeasure;
	
	public UserBenefitsAnalyzerAA() {
		super(UserBenefitsAnalyzerAA.class.getSimpleName());
	}
	
	public void init(ScenarioImpl scenario, WelfareMeasure welfareMeasure) {
		this.scenario = scenario;
		this.welfareMeasure = welfareMeasure;
		this.userWelfareCalculator = new UserBenefitsCalculator(this.scenario.getConfig(), this.welfareMeasure, false);
		this.userWelfareCalculator.reset();
	}
	
	@Override
	public List<EventHandler> getEventHandler() {
		// nothing to return
		return new LinkedList<EventHandler>();
	}

	@Override
	public void preProcessData() {
		this.allUsersLogSum = this.userWelfareCalculator.calculateUtility_money(this.scenario.getPopulation());
		this.personWithNoValidPlanCnt = this.userWelfareCalculator.getPersonsWithoutValidPlanCnt();
		log.warn("users with no valid plan (all scores ``== null'' or ``<= 0.0''): " + personWithNoValidPlanCnt);
		this.personId2MonetarizedUserWelfare = this.userWelfareCalculator.getPersonId2MonetizedUtility();
		this.personId2UserWelfare = this.userWelfareCalculator.getPersonId2Utility();
	}

	@Override
	public void postProcessData() {
		// nothing to do
	}

	@Override
	public void writeResults(String outputFolder) {
		String fileName = outputFolder + "userBenefits"+this.welfareMeasure+".txt";
		File file = new File(fileName);
				
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("monetary user benefits (all users logsum): " + this.allUsersLogSum);
			bw.newLine();
			bw.write("users with no valid plan (all scores ``== null'' or ``<= 0.0''): " + this.personWithNoValidPlanCnt);
			bw.newLine();
			
			bw.newLine();
			bw.write("userID \t monetary user logsum");
			bw.newLine();
			
			for (Id id : this.personId2UserWelfare.keySet()){
				String row = id + "\t" + this.personId2UserWelfare.get(id);
				bw.write(row);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public double getAllUsersLogSum() {
		return allUsersLogSum;
	}

	public Map<Id, Double> getPersonId2UserWelfare_utils() {
		return personId2UserWelfare;
	}
	
	public Map<Id, Double> getPersonId2MonetarizedUserWelfare(){
		return personId2MonetarizedUserWelfare;
	}
}

