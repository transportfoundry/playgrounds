package sharedmobility;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.events.EndRentalEvent;
import org.matsim.contrib.carsharing.events.StartRentalEvent;
import org.matsim.contrib.carsharing.events.handlers.EndRentalEventHandler;
import org.matsim.contrib.carsharing.events.handlers.StartRentalEventHandler;
import org.matsim.contrib.carsharing.manager.demand.AgentRentals;
import org.matsim.contrib.carsharing.manager.demand.RentalInfo;
import org.matsim.contrib.carsharing.manager.demand.VehicleRentals;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

public class BikeshareDemandHandler implements StartRentalEventHandler, EndRentalEventHandler {

	
	@Inject Scenario scenario;
	@Inject CarsharingSupplyInterface carsharingSupplyContainer;

	private Map<Id<Person>, AgentRentals> agentRentalsMap = new HashMap<Id<Person>, AgentRentals>();	
	private Map<Id<Vehicle>, VehicleRentals> vehicleRentalsMap = new HashMap<Id<Vehicle>, VehicleRentals>();
	private Map<Id<Vehicle>, Id<Person>> vehiclePersonMap = new HashMap<Id<Vehicle>, Id<Person>>();
	private Map<Id<Person>, Double> enterVehicleTimes = new HashMap<Id<Person>, Double>();	


	@Override
	public void reset(int iteration) {
		agentRentalsMap = new HashMap<Id<Person>, AgentRentals>();	
		vehicleRentalsMap = new HashMap<Id<Vehicle>, VehicleRentals>();
		vehiclePersonMap = new HashMap<Id<Vehicle>, Id<Person>>();
		enterVehicleTimes = new HashMap<Id<Person>, Double>();
	}
	
	
	@Override
	public void handleEvent(EndRentalEvent event) {
		
		AgentRentals agentRentals = this.agentRentalsMap.get(event.getPersonId());
		
		RentalInfo info = agentRentals.getStatsPerVehicle().get(event.getvehicleId());
		agentRentals.getStatsPerVehicle().remove(event.getvehicleId());
		info.setEndTime(event.getTime());
		info.setEndLinkId(event.getLinkId());

		agentRentals.getArr().add(info);

		Id<Vehicle> vehicleId = Id.create(event.getvehicleId(), Vehicle.class);
		if (!this.vehicleRentalsMap.containsKey(vehicleId)) {
			this.vehicleRentalsMap.put(vehicleId, new VehicleRentals(vehicleId));
		}

		this.vehicleRentalsMap.get(vehicleId).getRentals().add(info);
	}
	
	
	@Override
	public void handleEvent(StartRentalEvent event) {
		RentalInfo info = new RentalInfo();
		info.setCarsharingType(event.getCarsharingType());
		info.setAccessStartTime(event.getTime());
		info.setStartTime(event.getTime());
		info.setOriginLinkId(event.getOriginLinkId());
		info.setPickupLinkId(event.getPickuplinkId());

		if (agentRentalsMap.containsKey(event.getPersonId())) {
			AgentRentals agentRentals = this.agentRentalsMap.get(event.getPersonId());
			agentRentals.getStatsPerVehicle().put(event.getvehicleId(), info);
			
		}
		else {
			AgentRentals agentRentals = new AgentRentals(event.getPersonId());
			agentRentalsMap.put(event.getPersonId(), agentRentals);			
			agentRentals.getStatsPerVehicle().put(event.getvehicleId(), info);			
		}		
	}
	
	
	private boolean carsharingTrip(Id<Vehicle> vehicleId) {

		return this.carsharingSupplyContainer.getAllVehicles().containsKey(vehicleId.toString());		
	}

	public Map<Id<Person>, AgentRentals> getAgentRentalsMap() {
		return agentRentalsMap;
	}

	public Map<Id<Vehicle>, VehicleRentals> getVehicleRentalsMap() {
		return this.vehicleRentalsMap;
	}
	
	
	
	
}
