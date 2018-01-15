/* *********************************************************************** *
 * project: org.matsim.*
 * MyCiTiVehicles.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 */
package playground.jjoubert.projects.capeTownMultimodal.gtfs;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleCapacityImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;

/**
 * Class to assign vehicles to specific trips. This is in an effort to match 
 * the GTFS trips (from the feed) to actual vehicles.
 *   
 * @author jwjoubert
 */
public class MetrorailVehicles {
	final private static Logger LOG = Logger.getLogger(MetrorailVehicles.class);
	private Map<String, Vehicle> vehicleMap;
	
	public MetrorailVehicles() {
		populateVehicleMap();
	}

	public Vehicle getVehicle(String block_id){
		if(!this.vehicleMap.containsKey(block_id)){
			LOG.error("Cannot find a vehicle for block_id=" + block_id);
			return null;
		}
		return this.vehicleMap.get(block_id);
	}
	
	private void populateVehicleMap(){
		LOG.info("Populating vehicle map...");
		/*TODO */
		LOG.info("Done populating vehicle map.");
	}
	
	public enum Type{
		Metrorail; 
		
		public VehicleType getVehicleType(){
			double length = 0.0;
			VehicleCapacity capacity = new VehicleCapacityImpl();
			switch (this) {
			case Metrorail:
				length = 60.0;
				capacity.setSeats(100);
				capacity.setStandingRoom(300);
				break;
			default:
				throw new RuntimeException("Unknown vehicle type");	
			}
			VehicleType type = new VehicleTypeImpl(Id.create(this.name(), VehicleType.class));
			type.setLength(length);
			type.setCapacity(capacity);
			return type;
		}

		
	}
}
