/* *********************************************************************** *
 * project: org.matsim.*
 * EndActivityAndEvacuateReplanner.java
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

package playground.christoph.evacuation.withinday.replanning.replanners;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplanner;
import org.matsim.withinday.utils.EditRoutes;

import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;
import playground.christoph.evacuation.mobsim.decisiondata.PersonDecisionData;
import playground.christoph.evacuation.trafficmonitoring.SwissPTTravelTime;
import playground.christoph.evacuation.withinday.replanning.utils.ModeAvailabilityChecker;

public class CurrentActivityToMeetingPointReplanner extends WithinDayDuringActivityReplanner {
	
	private static final Logger log = Logger.getLogger(CurrentActivityToMeetingPointReplanner.class);
	
	private static final String activityType = "meetHousehold";
	
	protected final DecisionDataProvider decisionDataProvider;
	protected final ModeAvailabilityChecker modeAvailabilityChecker;
	protected final SwissPTTravelTime ptTravelTime;
	protected final TripRouter tripRouter;
	protected final EditRoutes editRoutes;
	
	/*package*/ CurrentActivityToMeetingPointReplanner(Id id, Scenario scenario,
			InternalInterface internalInterface, DecisionDataProvider decisionDataProvider,
			ModeAvailabilityChecker modeAvailabilityChecker, SwissPTTravelTime ptTravelTime,
			TripRouter tripRouter) {
		super(id, scenario, internalInterface);
		this.decisionDataProvider = decisionDataProvider;
		this.modeAvailabilityChecker = modeAvailabilityChecker;
		this.ptTravelTime = ptTravelTime;
		this.tripRouter = tripRouter;
		this.editRoutes = new EditRoutes();
	}
	
	@Override
	public boolean doReplanning(PlanBasedWithinDayAgent withinDayAgent) {		
		
		// If we don't have a valid PersonAgent
		if (withinDayAgent == null) return false;
	
		PlanImpl executedPlan = (PlanImpl)withinDayAgent.getSelectedPlan();

		// If we don't have an executed plan
		if (executedPlan == null) return false;
		
		Activity currentActivity;
		int currentActivityIndex;
		
		/*
		 *  Get the current PlanElement and check if it is an Activity
		 */
		PlanElement currentPlanElement = withinDayAgent.getCurrentPlanElement();
		if (currentPlanElement instanceof Activity) {
			currentActivity = (Activity) currentPlanElement;
			
			// get the index of the currently performed activity in the selected plan
			currentActivityIndex = executedPlan.getActLegIndex(currentActivity);
		} else return false;
				
		/*
		 * Check whether the agent is already at the meeting point.
		 * If yes, remove activities that are scheduled at a later point in time.
		 * Otherwise create a new activity at the meeting point, add it to the plan
		 * and remove all other remaining activities.
		 */
		PersonDecisionData pdd = decisionDataProvider.getPersonDecisionData(withinDayAgent.getId());
		Id householdId = pdd.getHouseholdId();
		Id meetingPointId = decisionDataProvider.getHouseholdDecisionData(householdId).getMeetingPointFacilityId(); 
		
		if (currentActivity.getFacilityId().equals(meetingPointId)) {
			currentActivity.setType(activityType);
			currentActivity.setMaximumDuration(Time.UNDEFINED_TIME);
			currentActivity.setEndTime(Double.POSITIVE_INFINITY);
			
			// Remove all legs and activities after the next activity.
			while (executedPlan.getPlanElements().size() - 1 > currentActivityIndex) {
				executedPlan.removeActivity(executedPlan.getPlanElements().size() - 1);
			}
		} else {
			/*
			 * The agent is currently not at the Meeting Point. Therefore, we create a new Activity
			 * which is located there. Additionally, we set a new end time for the current Activity.
			 */		
			ActivityFacility meetingFacility = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(meetingPointId);
			Activity meetingActivity = scenario.getPopulation().getFactory().createActivityFromLinkId(activityType, meetingFacility.getLinkId());
			((ActivityImpl) meetingActivity).setFacilityId(meetingPointId);
			((ActivityImpl)meetingActivity).setCoord(meetingFacility.getCoord());
			meetingActivity.setEndTime(Double.POSITIVE_INFINITY);
			
			/*
			 * Create Leg from the current Activity to the Meeting Point
			 */		
			// identify the TransportMode
			Id vehicleId = getVehicleId(executedPlan);
			String transportMode = modeAvailabilityChecker.identifyTransportMode(currentActivityIndex, executedPlan, vehicleId);
			
			Leg legToMeeting = scenario.getPopulation().getFactory().createLeg(transportMode);
			
			double newEndTime = this.time;
			currentActivity.setMaximumDuration(newEndTime - currentActivity.getStartTime());
			currentActivity.setEndTime(newEndTime);
			legToMeeting.setDepartureTime(newEndTime);

			/*
			 * Adapt the plan by first removing all not yet performed Activities and Legs and
			 * then adding the new Leg and Activity at the Meeting Point.
			 */
			// Remove all legs and activities after the current activity.
			while (executedPlan.getPlanElements().size() - 1 > currentActivityIndex) {
				executedPlan.removeActivity(executedPlan.getPlanElements().size() - 1);
			}
			
			// add new activity
			int position = executedPlan.getActLegIndex(currentActivity) + 1;
			executedPlan.insertLegAct(position, legToMeeting, meetingActivity);
			
			// calculate route for the leg to the rescue facility using the identified mode
			this.editRoutes.relocateFutureLegRoute(legToMeeting, currentActivity.getLinkId(), meetingActivity.getLinkId(), 
					executedPlan.getPerson(), scenario.getNetwork(), tripRouter);
			
			// if the person has to walk, we additionally try pt
			if (transportMode.equals(TransportMode.walk)) {
				Tuple<Double, Coord> tuple = ptTravelTime.calcSwissPtTravelTime(currentActivity, meetingActivity, this.time, executedPlan.getPerson()); 
				double travelTimePT = tuple.getFirst();
				double travelTimeWalk = legToMeeting.getTravelTime();
				
				// If using pt is faster than walking switch to pt.
				if (travelTimePT < travelTimeWalk) {
					legToMeeting.setMode(TransportMode.pt);
										
					this.editRoutes.relocateFutureLegRoute(legToMeeting, currentActivity.getLinkId(), meetingActivity.getLinkId(), 
							executedPlan.getPerson(), scenario.getNetwork(), tripRouter);
					
					// set travel time
					legToMeeting.getRoute().setTravelTime(travelTimePT);
					
					// set speed
					double routeLength = RouteUtils.calcDistance((NetworkRoute) legToMeeting.getRoute(), scenario.getNetwork());
					ptTravelTime.setPersonSpeed(withinDayAgent.getId(), routeLength / travelTimePT);
				}
			}
			
			meetingActivity.setStartTime(legToMeeting.getDepartureTime() + legToMeeting.getTravelTime());
			
			/*
			 * If it is a car leg, set the vehicleId.
			 */
			if (transportMode.equals(TransportMode.car)) {
				((NetworkRoute) legToMeeting.getRoute()).setVehicleId(vehicleId);
			}
		}
		
		/*
		 * Reschedule the currently performed Activity in the Mobsim - there
		 * the activityEndsList has to be updated.
		 */
		// yyyy a method getMobsim in MobimAgent would be useful here. cdobler, Oct'10
		// Intuitively I would agree.  We should think about where to set this so that, under normal circumstances,
		// it can't become null.  kai, oct'10
		if (withinDayAgent instanceof PersonDriverAgentImpl) {			
			((ExperimentalBasicWithindayAgent) withinDayAgent).calculateAndSetDepartureTime(currentActivity);
			this.internalInterface.rescheduleActivityEnd(withinDayAgent);
			return true;
		}
		else {
			log.warn("PersonAgent is no PersonDriverAgentImpl - the new departure time cannot be calculated!");
			return false;
		}	
	}
	
	/**
	 * Return the id of the first vehicle used by the agent.
	 * Without Within-Day Replanning, an agent will use the same
	 * vehicle during the whole day. When Within-Day Replanning
	 * is enabled, this method should not be called anymore...
	 */
	private Id getVehicleId(Plan plan) {
		for (PlanElement planElement : plan.getPlanElements()) {
			if (planElement instanceof Leg) {
				Leg leg = (Leg) planElement;
				if (leg.getMode().equals(TransportMode.car)) {
					Route route = leg.getRoute();
					if (route instanceof NetworkRoute) {
						NetworkRoute networkRoute = (NetworkRoute) route;
						return networkRoute.getVehicleId();
					}					
				}
			}
		}
		return null;
	}
	
}
