package playground.balac.contribs.carsharing.demand;

import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.manager.demand.RentalInfo;
import org.matsim.contrib.carsharing.manager.demand.VehicleChoiceAgent;
import org.matsim.contrib.carsharing.manager.demand.membership.MembershipContainer;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.contrib.carsharing.manager.supply.costs.CostsCalculatorContainer;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

public class VehicleChoiceTechnologyPriceAgent implements VehicleChoiceAgent  {

	@Inject private CarsharingSupplyInterface carsharingSupply;
	@Inject private CostsCalculatorContainer costCalculator;
	@Inject private Scenario scenario;	
	@Inject private LeastCostPathCalculatorFactory pathCalculatorFactory ;	
	@Inject private Map<String, TravelTime> travelTimes ;
	@Inject private Map<String, TravelDisutilityFactory> travelDisutilityFactories ;
	
	@Override
	public CSVehicle chooseVehicle(Set<CSVehicle> vehicleOptions, Link startLink, Leg leg, double currentTime, Person person) {

		CSVehicle chosenVehicle = null;
		double maxUtility = Integer.MIN_VALUE;
		double marginalUtilityOfMoney = ((PlanCalcScoreConfigGroup)scenario.getConfig().getModule("planCalcScore")).getMarginalUtilityOfMoney();
		for (CSVehicle vehicle : vehicleOptions) {
			Link vehicleLocation = this.carsharingSupply.getCompany(vehicle.getCompanyId()).getVehicleContainer(vehicle.getCsType())
					.getVehicleLocation(vehicle);
			
			double walkTravelTime = estimateWalkTravelTime(startLink, vehicleLocation);
			
			RentalInfo rentalInfo = new RentalInfo();
			double time = estimatetravelTime(leg, vehicleLocation, currentTime);
			rentalInfo.setInVehicleTime(time);
			rentalInfo.setStartTime(currentTime);
			rentalInfo.setEndTime(currentTime + walkTravelTime + time);
			double utilTechnology = 0.0;
			if (vehicle.getCompanyId().equals("Catchacar") && (Boolean)person.getAttributes().getAttribute("technology"))
				utilTechnology = 0.5;
			double utility = -costCalculator.getCost(vehicle.getCompanyId(), vehicle.getCsType(), rentalInfo) * marginalUtilityOfMoney
					+ utilTechnology;
			
			if (utility > maxUtility) {
				maxUtility = utility;
				chosenVehicle = vehicle;
			}			
		}
		
		return chosenVehicle;
		
	}
	@Override
	public CSVehicle chooseVehicleActivityTimeIncluded(Set<CSVehicle> vehicleOptions, 
			Link startLink, Leg leg, double currentTime, Person person, double durationOfNextActivity, boolean keepthecar) {

		CSVehicle chosenVehicle = null;
		double maxUtility = Integer.MIN_VALUE;
		double marginalUtilityOfMoney = ((PlanCalcScoreConfigGroup)scenario.getConfig().getModule("planCalcScore")).getMarginalUtilityOfMoney();
		for (CSVehicle vehicle : vehicleOptions) {
			Link vehicleLocation = this.carsharingSupply.getCompany(vehicle.getCompanyId()).getVehicleContainer(vehicle.getCsType())
					.getVehicleLocation(vehicle);
			
			double walkTravelTime = estimateWalkTravelTime(startLink, vehicleLocation);
			
			RentalInfo rentalInfo = new RentalInfo();
			double time = estimatetravelTime(leg, vehicleLocation, currentTime);
			rentalInfo.setInVehicleTime(time);
			rentalInfo.setStartTime(currentTime);
			
			if (keepthecar) {
				rentalInfo.setEndTime(currentTime + walkTravelTime + time + durationOfNextActivity +
						time);
			}
			else
				rentalInfo.setEndTime(currentTime + walkTravelTime + time);
			double utilTechnology = 0.0;
			if (vehicle.getCompanyId().equals("Catchacar") && (Boolean)person.getAttributes().getAttribute("technology"))
				utilTechnology = 0.3;
			double utility = -costCalculator.getCost(vehicle.getCompanyId(), vehicle.getCsType(), rentalInfo) * marginalUtilityOfMoney
					+ utilTechnology;
			
			if (utility > maxUtility) {
				maxUtility = utility;
				chosenVehicle = vehicle;
			}			
		}
		
		return chosenVehicle;
		
	}
	
	
	private double estimatetravelTime(Leg leg, Link vehicleLocation, double now) {	
		
		Network network = scenario.getNetwork();
		TravelTime travelTime = travelTimes.get( TransportMode.car ) ;
		
		TravelDisutility travelDisutility = travelDisutilityFactories.get( TransportMode.car ).createTravelDisutility(travelTime) ;
		LeastCostPathCalculator pathCalculator = pathCalculatorFactory.createPathCalculator(scenario.getNetwork(),
				travelDisutility, travelTime ) ;
		Link endLink  = network.getLinks().get(leg.getRoute().getEndLinkId());
		Vehicle vehicle = null ;
		Path path = pathCalculator.calcLeastCostPath(vehicleLocation.getToNode(), endLink.getFromNode(), 
				now, null, vehicle ) ;
		
		return path.travelTime;
		
	}
	
	private double estimateWalkTravelTime(Link startLink, Link endLink) {	
		
		return  CoordUtils.calcEuclideanDistance(startLink.getCoord(), endLink.getCoord()) * 1.3 / 1.0;
		
	}

}
