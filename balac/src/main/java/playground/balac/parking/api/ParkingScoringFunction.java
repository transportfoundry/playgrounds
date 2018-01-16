package playground.balac.parking.api;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

import playground.balac.parking.infrastructure.ActInfo;
import playground.balac.parking.infrastructure.ParkingImpl;


public interface ParkingScoringFunction {

	public void assignScore(ParkingImpl parking, Coord targtLocationCoord, ActInfo targetActInfo, Id personId, Double arrivalTime,
			Double estimatedParkingDuration);

	public Double getScore(ParkingImpl parking, Coord targtLocationCoord, ActInfo targetActInfo, Id personId, Double arrivalTime,
			Double estimatedParkingDuration);
}
