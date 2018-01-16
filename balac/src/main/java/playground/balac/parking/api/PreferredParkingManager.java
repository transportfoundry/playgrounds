package playground.balac.parking.api;

import org.matsim.api.core.v01.Id;

import playground.balac.parking.infrastructure.ActInfo;
import playground.balac.parking.infrastructure.PreferredParking;


/**
 * 
 * @author wrashid
 *
 */
public interface PreferredParkingManager {

	boolean considerForChoiceSet(PreferredParking preferredParking, Id personId, double OPTIONALtimeOfDayInSeconds, ActInfo targetActInfo);
	
	boolean isPersonLookingForCertainTypeOfParking(Id personId, double oPTIONALtimeOfDayInSeconds, ActInfo targetActInfo);
	
}
