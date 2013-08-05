/* *********************************************************************** *
 * project: org.matsim.*
 * SecureLegPerformingIdentifier.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.withinday.replanning.identifiers;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.core.mobsim.qsim.comparators.PersonAgentComparator;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.identifiers.tools.LinkReplanningMap;

import playground.christoph.evacuation.config.EvacuationConfig;

public class SecureLegPerformingIdentifier extends DuringLegIdentifier {
	
	private static final Logger log = Logger.getLogger(SecureLegPerformingIdentifier.class);
	
	private final LinkReplanningMap linkReplanningMap;
	private final MobsimDataProvider mobsimDataProvider;
	private final Coord centerCoord;
	private final double secureDistance;
	private final Network network;
	
	/*package*/ SecureLegPerformingIdentifier(LinkReplanningMap linkReplanningMap, MobsimDataProvider mobsimDataProvider,
			Network network, Coord centerCoord, double secureDistance) {
		this.linkReplanningMap = linkReplanningMap;
		this.mobsimDataProvider = mobsimDataProvider;
		this.network = network;
		this.centerCoord = centerCoord;
		this.secureDistance = secureDistance;
	}
	
	public Set<PlanBasedWithinDayAgent> getAgentsToReplan(double time) {
		
		Set<Id> legPerformingAgents = new HashSet<Id>(this.linkReplanningMap.getLegPerformingAgents());
		Map<Id, MobsimAgent> mapping = this.mobsimDataProvider.getAgents();
		
		
		// apply filter to remove agents that should not be replanned
		this.applyFilters(legPerformingAgents, time);
		
		Set<PlanBasedWithinDayAgent> agentsToReplan = new TreeSet<PlanBasedWithinDayAgent>(new PersonAgentComparator());
		
		for (Id id : legPerformingAgents) {
			
			PlanBasedWithinDayAgent agent = (PlanBasedWithinDayAgent) mapping.get(id);
			
			/*
			 * Remove the Agent from the list, if the current Link is in a insecure Area.
			 */
			Link currentLink = this.network.getLinks().get(agent.getCurrentLinkId());
			double distanceToStartNode = CoordUtils.calcDistance(currentLink.getFromNode().getCoord(), centerCoord);
			double distanceToEndNode = CoordUtils.calcDistance(currentLink.getToNode().getCoord(), centerCoord);
			if (distanceToStartNode <= secureDistance || distanceToEndNode <= secureDistance) {
				continue;
			}
			
			/*
			 * Add the Agent to the Replanning List
			 */
			agentsToReplan.add(agent);
		}
		
		if (time == EvacuationConfig.evacuationTime) log.info("Found " + agentsToReplan.size() + " Agents performing a Leg in a secure area.");

		return agentsToReplan;
	}

}
