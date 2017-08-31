/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package sharedmobility;

import org.matsim.core.config.ReflectiveConfigGroup;
/** 
 * @author balac
 */

public class BikeShareConfigGroup extends ReflectiveConfigGroup {
	
	public static final String GROUP_NAME = "Bikeshare";
		
	private String travelingBikeshare = null;
	
	private String constantBikeshare = null;
	
	private String vehiclelocationsInputFile = null;

	private String areasInputFile = null;
	
	private String timeFeeBikeshare = null;
	
	private String timeParkingFeeBikeshare = null;
	
	private String distanceFeeBikeshare = null;
	
	private boolean useFeeBikeshare = false;	
	
	private String specialTimeStart = null; //in seconds
	
	private String specialTimeEnd = null;  //in seconds
	
	private String specialTimeFee = null;

	
	public BikeShareConfigGroup() {
		super(GROUP_NAME);
	}
	
	@StringGetter( "travelingBikeshare" )
	public String getUtilityOfTravelling() {
		return this.travelingBikeshare;
	}

	@StringSetter( "travelingBikeshare" )
	public void setUtilityOfTravelling(final String travelingBikeshare) {
		this.travelingBikeshare = travelingBikeshare;
	}

	@StringGetter( "constantBikeshare" )
	public String constantBikeshare() {
		return this.constantBikeshare;
	}

	@StringSetter( "constantBikeshare" )
	public void setConstantBikeshare(final String constantBikeshare) {
		this.constantBikeshare = constantBikeshare;
	}
	
	@StringGetter( "vehiclelocationsBikeshare" )
	public String getvehiclelocations() {
		return this.vehiclelocationsInputFile;
	}

	@StringSetter( "vehiclelocationsBikeshare" )
	public void setvehiclelocations(final String vehiclelocationsInputFile) {
		this.vehiclelocationsInputFile = vehiclelocationsInputFile;
	}

	@StringGetter( "areasBikeshare" )
	public String getAreas() {
		return this.areasInputFile;
	}

	@StringSetter( "areasBikeshare" )
	public void setAreas(final String areasInputFile) {
		this.areasInputFile = areasInputFile;
	}

	@StringGetter( "timeFeeBikeshare" )
	public String timeFeeBikeshare() {
		return this.timeFeeBikeshare;
	}

	@StringSetter( "timeFeeBikeshare" )
	public void setTimeFeeBikeshare(final String timeFeeBikeshare) {
		this.timeFeeBikeshare = timeFeeBikeshare;
	}
	
	@StringGetter( "timeParkingFeeBikeshare" )
	public String timeParkingFeeBikeshare() {
		return this.timeParkingFeeBikeshare;
	}

	@StringSetter( "timeParkingFeeBikeshare" )
	public void setTimeParkingFeeBikeshare(final String timeParkingFeeBikeshare) {
		this.timeParkingFeeBikeshare = timeParkingFeeBikeshare;
	}
	
	@StringGetter( "distanceFeeBikeshare" )
	public String distanceFeeBikeshare() {
		return this.distanceFeeBikeshare;
	}

	@StringSetter( "distanceFeeBikeshare" )
	public void setDistanceFeeBikeshare(final String distanceFeeBikeshare) {
		this.distanceFeeBikeshare = distanceFeeBikeshare;
	}
	
	@StringGetter( "useBikeshare" )
	public boolean useFeeBikeshare() {
		return this.useFeeBikeshare;
	}

	@StringSetter( "useBikeshare" )
	public void setUseFeeBikeshare(final boolean useFeeBikeshare) {
		this.useFeeBikeshare = useFeeBikeshare;
	}
	
	@StringGetter( "specialTimeStart" )
	public String specialTimeStart() {
		return this.specialTimeStart;
	}

	@StringSetter( "specialTimeStart" )
	public void setSpecialTimeStart(final String specialTimeStart) {
		this.specialTimeStart = specialTimeStart;
	}
	
	@StringGetter( "specialTimeEnd" )
	public String specialTimeEnd() {
		return this.specialTimeEnd;
	}

	@StringSetter( "specialTimeEnd" )
	public void setSpecialTimeEnd(final String specialTimeEnd) {
		this.specialTimeEnd = specialTimeEnd;
	}
	
	@StringGetter( "specialTimeFee" )
	public String specialTimeFee() {
		return this.specialTimeFee;
	}

	@StringSetter( "specialTimeFee" )
	public void setSpecialTimeFee(final String specialTimeFee) {
		this.specialTimeFee = specialTimeFee;
	}
	
}
