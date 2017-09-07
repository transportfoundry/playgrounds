package sharedmobility;

import java.util.Set;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.manager.demand.AgentRentals;
import org.matsim.contrib.carsharing.manager.demand.DemandHandler;
import org.matsim.contrib.carsharing.manager.demand.RentalInfo;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.contrib.carsharing.manager.supply.costs.CostsCalculatorContainer;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.functions.ScoringParameters;

import com.google.common.collect.ImmutableSet;

public class CSBSLegScoringFunction extends org.matsim.core.scoring.functions.CharyparNagelLegScoring {

	private Config config;	
	
	private CostsCalculatorContainer costsCalculatorContainer;
	private DemandHandler demandHandler;
	private BikeshareDemandHandler bsDemandHandler;
	private Person person;
	private CarsharingSupplyInterface carsharingSupplyContainer;
	
	private static final  Set<String> walkingLegs = ImmutableSet.of("egress_walk_ow", "access_walk_ow",
			"egress_walk_tw", "access_walk_tw", "egress_walk_ff", "access_walk_ff", "access_walk_bs", "access_walk_bs");
	
	private static final  Set<String> carsharingLegs = ImmutableSet.of("oneway_vehicle", "twoway_vehicle",
			"freefloating_vehicle");
	
	private static final  Set<String> bikeshareLegs = ImmutableSet.of("bikeshare_vehicle");
	
	
	public CSBSLegScoringFunction(ScoringParameters params, 
			Config config,  Network network, DemandHandler demandHandler, BikeshareDemandHandler bsDemandHandler,
			CostsCalculatorContainer costsCalculatorContainer, CarsharingSupplyInterface carsharingSupplyContainer,
			Person person)
	{
		super(params, network);
		this.config = config;
		this.demandHandler = demandHandler;
		this.bsDemandHandler = bsDemandHandler;
		this.carsharingSupplyContainer = carsharingSupplyContainer;
		this.costsCalculatorContainer = costsCalculatorContainer;
		this.person = person;		
	}

	@Override
	public void handleEvent(Event event) {
		super.handleEvent(event);		
	}	
	
	@Override
	public void finish() {		
		super.finish();		
		
		AgentRentals agentRentals = this.demandHandler.getAgentRentalsMap().get(person.getId());
		if (agentRentals != null) {
			double marginalUtilityOfMoney = ((PlanCalcScoreConfigGroup)this.config.getModule("planCalcScore")).getMarginalUtilityOfMoney();
			for(RentalInfo rentalInfo : agentRentals.getArr()) {
				CSVehicle vehicle = this.carsharingSupplyContainer.getAllVehicles().get(rentalInfo.getVehId().toString());
				if (marginalUtilityOfMoney != 0.0)
					score += -1 * this.costsCalculatorContainer.getCost(vehicle.getCompanyId(), 
							rentalInfo.getCarsharingType(), rentalInfo) * marginalUtilityOfMoney;
			}			
		}			
		
		AgentRentals agentBSRentals = this.bsDemandHandler.getAgentRentalsMap().get(person.getId());
		if (agentBSRentals != null) {
			double marginalUtilityOfMoney = ((PlanCalcScoreConfigGroup)this.config.getModule("planCalcScore")).getMarginalUtilityOfMoney();
			for(RentalInfo rentalInfo : agentBSRentals.getArr()) {
//				CSVehicle vehicle = this.carsharingSupplyContainer.getAllVehicles().get(rentalInfo.getVehId().toString());
				if (marginalUtilityOfMoney != 0.0)
					score += -1 * this.costsCalculatorContainer.getCost("Smide", 
							rentalInfo.getCarsharingType(), rentalInfo) * marginalUtilityOfMoney;
			}			
		}			
		
		
	}	
	
	@Override
	protected double calcLegScore(double departureTime, double arrivalTime, Leg leg) {
		
		
		double tmpScore = 0.0D;
		double travelTime = arrivalTime - departureTime;
		String mode = leg.getMode();
		if (carsharingLegs.contains(mode)) {
					
			if (("oneway_vehicle").equals(mode)) {				
				tmpScore += Double.parseDouble(this.config.getModule("OneWayCarsharing").getParams().get("constantOneWayCarsharing"));
				tmpScore += travelTime * Double.parseDouble(this.config.getModule("OneWayCarsharing").getParams().get("travelingOneWayCarsharing")) / 3600.0;
			}		
		
			else if (("freefloating_vehicle").equals(mode)) {				
				
				tmpScore += Double.parseDouble(this.config.getModule("FreeFloating").getParams().get("constantFreeFloating"));
				tmpScore += travelTime * Double.parseDouble(this.config.getModule("FreeFloating").getParams().get("travelingFreeFloating")) / 3600.0;
			}		
			
			else if (("twoway_vehicle").equals(mode)) {				
				
				tmpScore += Double.parseDouble(this.config.getModule("TwoWayCarsharing").getParams().get("constantTwoWayCarsharing"));
				tmpScore += travelTime * Double.parseDouble(this.config.getModule("TwoWayCarsharing").getParams().get("travelingTwoWayCarsharing")) / 3600.0;
			}
		}
		
		else if (walkingLegs.contains(mode)) {
			
			tmpScore += getWalkScore(leg.getRoute().getDistance(), travelTime);
			
		}			
		
		else if (bikeshareLegs.contains(mode)) {
			
			tmpScore += Double.parseDouble(this.config.getModule("Bikeshare").getParams().get("constantBikeshare"));
			tmpScore += travelTime * Double.parseDouble(this.config.getModule("Bikeshare").getParams().get("travelingBikeshare")) / 3600.0;
			
		}			
		
		return tmpScore;
	}

	private double getWalkScore(double distance, double travelTime)
	{
		double score = 0.0D;

		score += travelTime * this.params.modeParams.get(TransportMode.walk).marginalUtilityOfTraveling_s + this.params.modeParams.get(TransportMode.walk).marginalUtilityOfDistance_m * distance;

		return score;
	}

	
}
