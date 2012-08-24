/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.andreas.P2.ana;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;

import playground.andreas.P2.ana.helper.BVGLines2PtModes;
import playground.andreas.P2.ana.helper.PtMode2LineSetter;
import playground.andreas.P2.ana.modules.AbstractPAnalyisModule;
import playground.andreas.P2.ana.modules.AverageLoadPerDeparturePerMode;
import playground.andreas.P2.ana.modules.AverageNumberOfStopsPerMode;
import playground.andreas.P2.ana.modules.AverageTripDistanceMeterPerMode;
import playground.andreas.P2.ana.modules.AverageInVehicleTripTravelTimeSecondsPerMode;
import playground.andreas.P2.ana.modules.AverageWaitingTimeSecondsPerMode;
import playground.andreas.P2.ana.modules.CountCapacityMeterPerMode;
import playground.andreas.P2.ana.modules.CountPassengerMeterPerMode;
import playground.andreas.P2.ana.modules.CountTransfersPerModeModeCombination;
import playground.andreas.P2.ana.modules.CountTripsPerMode;
import playground.andreas.P2.ana.modules.CountTripsPerPtModeCombination;
import playground.andreas.P2.ana.modules.CountVehPerMode;
import playground.andreas.P2.ana.modules.CountVehicleMeterPerMode;
import playground.andreas.P2.helper.PConfigGroup;

/**
 * Plugs in all analysis.
 * 
 * @author aneumann
 *
 */
public class PAnalysisManager implements StartupListener, IterationStartsListener, IterationEndsListener{
	private final static Logger log = Logger.getLogger(PAnalysisManager.class);
	
	private final String ptDriverPrefix;
	
	private final String pIdentifier;
	private List<AbstractPAnalyisModule> pAnalyzesList = new LinkedList<AbstractPAnalyisModule>();
	private HashMap<String, BufferedWriter> pAnalyis2Writer = new HashMap<String, BufferedWriter>();
	private boolean firstIteration = true;

	public PAnalysisManager(PConfigGroup pConfig, String ptDriverPrefix) {
		log.info("enabled");
		this.pIdentifier = pConfig.getPIdentifier();
		this.ptDriverPrefix = ptDriverPrefix;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		// create all analyzes
		this.pAnalyzesList.add(new CountTripsPerMode(this.ptDriverPrefix));
		this.pAnalyzesList.add(new CountVehPerMode(this.ptDriverPrefix));
		this.pAnalyzesList.add(new CountVehicleMeterPerMode(ptDriverPrefix, event.getControler().getNetwork()));
		this.pAnalyzesList.add(new CountPassengerMeterPerMode(ptDriverPrefix, event.getControler().getNetwork()));
		this.pAnalyzesList.add(new AverageTripDistanceMeterPerMode(ptDriverPrefix, event.getControler().getNetwork()));
		this.pAnalyzesList.add(new AverageInVehicleTripTravelTimeSecondsPerMode(ptDriverPrefix));
		this.pAnalyzesList.add(new AverageWaitingTimeSecondsPerMode(ptDriverPrefix));
		this.pAnalyzesList.add(new AverageNumberOfStopsPerMode(ptDriverPrefix));
		this.pAnalyzesList.add(new CountTransfersPerModeModeCombination(ptDriverPrefix));
		this.pAnalyzesList.add(new CountTripsPerPtModeCombination(ptDriverPrefix));
		this.pAnalyzesList.add(new AverageLoadPerDeparturePerMode(ptDriverPrefix));
		this.pAnalyzesList.add(new CountCapacityMeterPerMode(ptDriverPrefix, event.getControler().getNetwork()));
		
		// register all analyzes
		for (AbstractPAnalyisModule ana : this.pAnalyzesList) {
			event.getControler().getEvents().addHandler((EventHandler) ana);
		}
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		// update pt mode for each line in schedule
		updateLineId2ptModeMap(event.getControler().getScenario().getTransitSchedule());
		updateVehicleTypes(event.getControler().getScenario().getVehicles());
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (this.firstIteration) {
			// create one output stream for each analysis
			for (AbstractPAnalyisModule ana : this.pAnalyzesList) {
				try {
					BufferedWriter writer = new BufferedWriter(new FileWriter(new File(event.getControler().getControlerIO().getOutputFilename("pAna_" + ana.getName() + ".txt"))));
					writer.write("# iteration" + ana.getHeader());
					writer.newLine();
					this.pAnalyis2Writer.put(ana.getName(), writer);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			this.firstIteration = false;
		}
		
		// write results to corresponding files
		for (AbstractPAnalyisModule ana : this.pAnalyzesList) {
			BufferedWriter writer = this.pAnalyis2Writer.get(ana.getName());
			try {
				writer.write(event.getIteration() + ana.getResult());
				writer.newLine();
				writer.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void updateLineId2ptModeMap(TransitSchedule transitSchedule) {
		// TODO [AN] This is currently hardcoded and should be configurable
		PtMode2LineSetter lineSetter = new BVGLines2PtModes();
		
		lineSetter.setPtModesForEachLine(transitSchedule, this.pIdentifier);
		HashMap<Id, String> lineIds2ptModeMap = lineSetter.getLineId2ptModeMap();
		
		for (AbstractPAnalyisModule ana : this.pAnalyzesList) {
			ana.setLineId2ptModeMap(lineIds2ptModeMap);
		}
	}

	private void updateVehicleTypes(Vehicles vehicles) {
		for (AbstractPAnalyisModule ana : this.pAnalyzesList) {
			ana.updateVehicles(vehicles);
		}		
	}
}
