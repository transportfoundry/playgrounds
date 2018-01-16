package playground.balac.parking.api;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import playground.balac.parking.infrastructure.ActInfo;
import playground.balac.parking.infrastructure.ReservedParking;

/**
 * 
 * @author wrashid
 *
 */
public interface ReservedParkingManager {

	boolean considerForChoiceSet(ReservedParking reservedParking, Id<Person> personId, double OPTIONALtimeOfDayInSeconds, ActInfo targetActInfo);
	
}
