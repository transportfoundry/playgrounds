package sharedmobility;

import org.matsim.contrib.carsharing.manager.demand.RentalInfo;
import org.matsim.contrib.carsharing.manager.supply.costs.CostCalculation;
import org.matsim.contrib.carsharing.manager.supply.costs.CostCalculationExample;

public class CostCalculationHenrik implements CostCalculation {

	private final static double betaTT = 1.0;
	private final static double betaRentalTIme = 1.0;
	private final static double scaleTOMatchCar = 4.0;
	
	@Override
	public double getCost(RentalInfo rentalInfo) {

		double rentalTIme = rentalInfo.getEndTime() - rentalInfo.getStartTime();
		double inVehicleTime = rentalInfo.getInVehicleTime();
		
		
		if (rentalInfo.getCarsharingType().equals("bikeshare")) {
			
			return CostCalculationHenrik.scaleTOMatchCar * 
					(inVehicleTime /60.0 * 0.3 + (rentalTIme - inVehicleTime) / 60.0 * 0.15);
		
		} else {
			
			return CostCalculationHenrik.scaleTOMatchCar * 
					(inVehicleTime /60.0 * 0.15);
			
		}
		
		
		
	}
	
}
