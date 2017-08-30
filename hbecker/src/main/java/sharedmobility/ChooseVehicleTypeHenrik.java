package sharedmobility;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.carsharing.manager.demand.membership.MembershipContainer;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.contrib.carsharing.models.ChooseVehicleType;

import com.google.inject.Inject;

/*
 * Change to current leg mode
 */

public class ChooseVehicleTypeHenrik implements ChooseVehicleType {

	@Inject CarsharingSupplyInterface carsharingSupply;
	@Inject MembershipContainer membershipContainer;
		
	@Override
	public String getPreferredVehicleType(Plan plan, Leg currentLeg){
		
		if (!currentLeg.equals("bikeshare")) {
		
			String vehicleType = "car";
			return vehicleType;
			
		} else {
			
			String vehicleType = "bike";
			return vehicleType;
			
		}
		
	}
	
}
