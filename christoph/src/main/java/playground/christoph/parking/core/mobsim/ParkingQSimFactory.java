/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingQSimFactory.java
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

package playground.christoph.parking.core.mobsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.SynchronizedEventsManagerImpl;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.core.mobsim.qsim.agents.ExperimentalBasicWithindayAgentFactory;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultQSimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.ParallelQNetsimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineFactory;
import org.matsim.withinday.mobsim.WithinDayEngine;

import playground.christoph.parking.core.mobsim.agents.ParkingAgentSource;
import playground.christoph.parking.withinday.utils.ParkingAgentsTracker;
import playground.christoph.parking.withinday.utils.ParkingRouterFactory;

/**
 * This class is basically a copy of QSimFactory but instead of a
 * PopulationAgentSource, a ParkingPopulationAgentSource object is
 * created and added to the QSim.
 *
 * @author cdobler
 */
public class ParkingQSimFactory implements MobsimFactory {

    private final static Logger log = Logger.getLogger(ParkingQSimFactory.class);

    private final ParkingInfrastructure parkingInfrastructure;
    private final ParkingRouterFactory parkingRouterFactory;
    private final WithinDayEngine withinDayEngine;
    private final ParkingAgentsTracker parkingAgentsTracker; 
    
    public ParkingQSimFactory(ParkingInfrastructure parkingInfrastructure, ParkingRouterFactory parkingRouterFactory, 
    		WithinDayEngine withinDayEngine, ParkingAgentsTracker parkingAgentsTracker) {
    	this.parkingInfrastructure = parkingInfrastructure;
    	this.parkingRouterFactory = parkingRouterFactory;
    	this.withinDayEngine = withinDayEngine;
    	this.parkingAgentsTracker = parkingAgentsTracker;
    }
    
    @Override
    public Netsim createMobsim(Scenario sc, EventsManager eventsManager) {    

		QSimConfigGroup conf = sc.getConfig().qsim();
		if (conf == null) {
			throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}

		// Get number of parallel Threads
		int numOfThreads = conf.getNumberOfThreads();
		QNetsimEngineFactory netsimEngFactory;
		if (numOfThreads > 1) {
			eventsManager = new SynchronizedEventsManagerImpl(eventsManager);
			netsimEngFactory = new ParallelQNetsimEngineFactory();
			log.info("Using parallel QSim with " + numOfThreads + " threads.");
		} else {
			netsimEngFactory = new DefaultQSimEngineFactory();
		}
		QSim qSim = new QSim(sc, eventsManager);
		ActivityEngine activityEngine = new ActivityEngine();
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
		QNetsimEngine netsimEngine = netsimEngFactory.createQSimEngine(qSim);
		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
		TeleportationEngine teleportationEngine = new TeleportationEngine();
		qSim.addMobsimEngine(teleportationEngine);

		if (sc.getConfig().network().isTimeVariantNetwork()) {
			qSim.addMobsimEngine(new NetworkChangeEventsEngine());		
		}
		
		qSim.addMobsimEngine(this.withinDayEngine);
		qSim.addMobsimEngine(this.parkingAgentsTracker);
		
//		AgentFactory agentFactory = new DefaultAgentFactory(qSim);
		AgentFactory agentFactory = new ExperimentalBasicWithindayAgentFactory(qSim);
		ExperimentalBasicWithindayAgent.copySelectedPlan = true;
		
		AgentSource agentSource = new ParkingAgentSource(sc, agentFactory, qSim, parkingInfrastructure, parkingRouterFactory);
		qSim.addAgentSource(agentSource);

		return qSim;
    }
}