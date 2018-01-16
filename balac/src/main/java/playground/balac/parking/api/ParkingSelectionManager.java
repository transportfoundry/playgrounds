package playground.balac.parking.api;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

import playground.balac.parking.infrastructure.ActInfo;
import playground.balac.parking.infrastructure.PParking;


public interface ParkingSelectionManager {

	// arrivalTime and estimatedParkingDuration can be both null, if not know at time of execution
	public PParking selectParking(Coord targtLocationCoord, ActInfo targetActInfo, Id personId, Double arrivalTime, Double estimatedParkingDuration);

}
