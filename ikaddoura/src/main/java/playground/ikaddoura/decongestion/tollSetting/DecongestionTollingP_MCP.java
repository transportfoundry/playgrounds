/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.decongestion.tollSetting;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;

import com.google.inject.Inject;

import playground.ikaddoura.decongestion.data.DecongestionInfo;
import playground.ikaddoura.decongestion.data.LinkInfo;

/**
 * 
 * P-based toll adjustment, where e(t) = average delay and K_p = VTTS * number of delayed agents
 * 
 * @author ikaddoura
 */

public class DecongestionTollingP_MCP implements DecongestionTollSetting, LinkLeaveEventHandler {
	private static final Logger log = Logger.getLogger(DecongestionTollingP_MCP.class);
	
	@Inject
	private DecongestionInfo congestionInfo;
	
	private Map<Id<Link>, LinkInfo> linkId2infoPreviousTollComputation = new HashMap<>();	
	private int tollUpdateCounter = 0;
	private final Map<Id<Link>, Map<Integer, Integer>> linkId2time2leavingAgents = new HashMap<>();

	@Override
	public void updateTolls() {
		
		final double vtts = ( this.congestionInfo.getScenario().getConfig().planCalcScore().getPerforming_utils_hr()
				- this.congestionInfo.getScenario().getConfig().planCalcScore().getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() )
				/ this.congestionInfo.getScenario().getConfig().planCalcScore().getMarginalUtilityOfMoney();

			
		final double toleratedAvgDelay = this.congestionInfo.getDecongestionConfigGroup().getTOLERATED_AVERAGE_DELAY_SEC();
		final boolean msa = this.congestionInfo.getDecongestionConfigGroup().isMsa();
		final double blendFactorFromConfig = this.congestionInfo.getDecongestionConfigGroup().getTOLL_BLEND_FACTOR();
		
		for (Id<Link> linkId : this.congestionInfo.getlinkInfos().keySet()) {
			
			LinkInfo linkInfo = this.congestionInfo.getlinkInfos().get(linkId);
			for (Integer intervalNr : linkInfo.getTime2avgDelay().keySet()) {
				
				// average delay
				
				double averageDelay = linkInfo.getTime2avgDelay().get(intervalNr);	
				if (averageDelay <= toleratedAvgDelay) {
					averageDelay = 0.0;
				}
								
				// toll
				
				double demand = 1.0;
				if (this.linkId2time2leavingAgents.get(linkId) != null && this.linkId2time2leavingAgents.get(linkId).get(intervalNr) != null) {
					demand = this.linkId2time2leavingAgents.get(linkId).get(intervalNr);
				}
				
				double toll = vtts * demand * averageDelay / 3600.;

				// prevent negative tolls
				
				if (toll < 0) {
					log.warn("Negative tolls... Are you sure everything works fine?");
					toll = 0;
				}
				
				// smoothen the tolls
				
				Double previousToll = linkInfo.getTime2toll().get(intervalNr);

				double blendFactor;
				if (msa) {
					if (this.tollUpdateCounter > 0) {
						blendFactor = 1.0 / (double) this.tollUpdateCounter;
					} else {
						blendFactor = 1.0;
					}
				} else {
					blendFactor = blendFactorFromConfig;
				}
				
				double smoothedToll;
				if (previousToll != null && previousToll >= 0.) {
					smoothedToll = toll * blendFactor + previousToll * (1 - blendFactor);
				} else {
					smoothedToll = toll;
				}
								
				// store the updated toll
				
				linkInfo.getTime2toll().put(intervalNr, smoothedToll);

			}	
		}
		
		log.info("Updating tolls completed.");
		this.tollUpdateCounter++;
		
		// store the current link information for the next toll computation
		
		linkId2infoPreviousTollComputation = new HashMap<>();
		for (Id<Link> linkId : this.congestionInfo.getlinkInfos().keySet()) {

			Map<Integer, Double> time2previousDelay = new HashMap<>();
			for (Integer intervalNr : this.congestionInfo.getlinkInfos().get(linkId).getTime2avgDelay().keySet()) {
				time2previousDelay.put(intervalNr, this.congestionInfo.getlinkInfos().get(linkId).getTime2avgDelay().get(intervalNr));
			}
			
			LinkInfo linkInfoPreviousTollComputation = new LinkInfo(linkId);
			linkInfoPreviousTollComputation.setTime2avgDelay(time2previousDelay);
			linkId2infoPreviousTollComputation.put(linkId, linkInfoPreviousTollComputation);
		}
	}

	@Override
	public void reset(int iteration) {
		this.linkId2time2leavingAgents.clear();
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		int timeBinNr = getIntervalNr(event.getTime());

		Id<Link> linkId = event.getLinkId();
		
		if (linkId2time2leavingAgents.get(linkId) != null) {
			
			if (linkId2time2leavingAgents.get(linkId).get(timeBinNr) != null) {
				int leavingAgents = linkId2time2leavingAgents.get(linkId).get(timeBinNr) + 1;
				linkId2time2leavingAgents.get(linkId).put(timeBinNr, leavingAgents);
				
			} else {
				linkId2time2leavingAgents.get(linkId).put(timeBinNr, 1);
			}
			
		} else {
			Map<Integer, Integer> time2leavingAgents = new HashMap<>();
			time2leavingAgents.put(timeBinNr, 1);
			linkId2time2leavingAgents.put(linkId, time2leavingAgents);
			
		}
	}
	
	private int getIntervalNr(double time) {
		double timeBinSize = congestionInfo.getScenario().getConfig().travelTimeCalculator().getTraveltimeBinSize();		
		return (int) (time / timeBinSize);
	}

}
