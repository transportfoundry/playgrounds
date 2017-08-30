package sharedmobility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.carsharing.manager.routers.RouteCarsharingTrip;
import org.matsim.contrib.carsharing.router.CarsharingRoute;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import sharedmobility.RouterUtilsHenrik;

import com.google.inject.Inject;
/** 
 * @author balac
 * Bikeshare addition in progress
 */

public class RouteCarsharingTripImplHenrik implements RouteCarsharingTrip {

	@Inject private Scenario scenario;
	@Inject private LeastCostPathCalculatorFactory pathCalculatorFactory ;
	
	@Inject private Map<String, TravelTime> travelTimes ;
	@Inject private Map<String, TravelDisutilityFactory> travelDisutilityFactories ;
	
	private final ArrayList<String> carsharingLegs = new ArrayList<>(Arrays.asList("oneway", "twoway",
			"freefloating", "bikeshare"));
	
	private final String[] carsharingVehicleLegs = {"oneway_vehicle", "twoway_vehicle",
			"freefloating_vehicle", "bikeshare_bike"};
	private final String[] accessCSLegs = {"access_walk_ow", "access_walk_tw",
	"access_walk_ff", "access_walk:bs"};
	
	private final String[] egressCSLegs = {"egress_walk_ow", "egress_walk_tw",
	"egress_walk_ff", "egress_walk_bs"};
	
	private final String[] csInteraction = {"ow_interaction", "tw_interaction",
	"ff_interaction", "bs_interaction"}; // really necessary for bikeshare?
	
	@Override
	public List<PlanElement> routeCarsharingTrip(Plan plan, Leg legToBeRouted, double time, 
			CSVehicle vehicle, Link vehicleLinkLocation, Link parkingLocation, boolean keepTheCarForLaterUse, boolean hasVehicle) {
		
		PopulationFactory pf = scenario.getPopulation().getFactory();
		TravelTime travelTime = travelTimes.get( TransportMode.car ) ;
		
		TravelDisutility travelDisutility = travelDisutilityFactories.get( TransportMode.car ).createTravelDisutility(travelTime) ;
		LeastCostPathCalculator pathCalculator = pathCalculatorFactory.createPathCalculator(scenario.getNetwork(),
				travelDisutility, travelTime ) ;
		
		String mainMode = legToBeRouted.getMode();
		int index = carsharingLegs.indexOf(mainMode);
		final List<PlanElement> trip = new ArrayList<PlanElement>();		

		Person person = plan.getPerson();
		CarsharingRoute route = (CarsharingRoute) legToBeRouted.getRoute();
		final Link currentLink = scenario.getNetwork().getLinks().get(route.getStartLinkId());
		final Link destinationLink = scenario.getNetwork().getLinks().get(route.getEndLinkId());
		
		if (hasVehicle) {
			
			if (!mainMode.equals("bikeshare")) {
				
				//=== car leg			
	
				trip.add(RouterUtilsHenrik.createCarLeg(pf, pathCalculator,
						person, currentLink, parkingLocation, carsharingVehicleLegs[index], 
						vehicle.getVehicleId(), time));		
				
				if (!keepTheCarForLaterUse) { 			
	
					Activity activityE = scenario.getPopulation().getFactory().createActivityFromLinkId(csInteraction[index],
							parkingLocation.getId());
					activityE.setMaximumDuration(0);
					
					trip.add(activityE);
					
					trip.add( RouterUtilsHenrik.createWalkLeg(pf, 
							parkingLocation, destinationLink, egressCSLegs[index], time) );		
				}
				
			} else {
				
				//=== bike leg			
	
				trip.add(RouterUtilsHenrik.createBikeLeg(pf, pathCalculator,
						person, currentLink, parkingLocation, carsharingVehicleLegs[index], 
						vehicle.getVehicleId(), time));		
				
				if (!keepTheCarForLaterUse) { 			
	
					Activity activityE = scenario.getPopulation().getFactory().createActivityFromLinkId(csInteraction[index],
							parkingLocation.getId());
					activityE.setMaximumDuration(0);
					
					trip.add(activityE);
					
					trip.add( RouterUtilsHenrik.createWalkLeg(pf, 
							parkingLocation, destinationLink, egressCSLegs[index], time) );		
				}
					
			}			
				
		
		}
		else {		
			
			String ffVehId = vehicle.getVehicleId();			
			trip.add( RouterUtilsHenrik.createWalkLeg(scenario.getPopulation().getFactory(),
					currentLink, vehicleLinkLocation, accessCSLegs[index], time) );
			
			Activity activityS = scenario.getPopulation().getFactory().createActivityFromLinkId(csInteraction[index],
					vehicleLinkLocation.getId());
			activityS.setMaximumDuration(0);
			
			trip.add(activityS);
			
			
			// === car leg: ===							
				
			if (!keepTheCarForLaterUse)  {	
				
				
				if (!mainMode.equals("bikeshare")) {
				// car	
				trip.add(RouterUtilsHenrik.createCarLeg(pf, pathCalculator,
						person, vehicleLinkLocation, parkingLocation, carsharingVehicleLegs[index],
						ffVehId, time));
				} else {
				// bike	
				trip.add(RouterUtilsHenrik.createBikeLeg(pf, pathCalculator,
						person, vehicleLinkLocation, parkingLocation, carsharingVehicleLegs[index],
						ffVehId, time));	
				}
				
				Activity activityE = scenario.getPopulation().getFactory().createActivityFromLinkId(csInteraction[index],
						parkingLocation.getId());
				activityE.setMaximumDuration(0);
				
				trip.add(activityE);
				
				trip.add( RouterUtilsHenrik.createWalkLeg(pf, 
						parkingLocation, destinationLink, egressCSLegs[index], time) );
			}
			else {
				if (!mainMode.equals("bikeshare")) {
				// car	
				trip.add(RouterUtilsHenrik.createCarLeg(pf, pathCalculator,
						person, vehicleLinkLocation, parkingLocation, carsharingVehicleLegs[index],
						ffVehId, time));
				} else {
				// bike	
				trip.add(RouterUtilsHenrik.createBikeLeg(pf, pathCalculator,
						person, vehicleLinkLocation, parkingLocation, carsharingVehicleLegs[index],
						ffVehId, time));	
				}
				
			}
			
		}			
		return trip;
	}
	
	
}
