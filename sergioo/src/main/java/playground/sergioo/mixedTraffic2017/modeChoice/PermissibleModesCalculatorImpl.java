/* *********************************************************************** *
 * project: org.matsim.*
 * PermissibleModesCalculatorImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.sergioo.mixedTraffic2017.modeChoice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.inject.Inject;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;

public class PermissibleModesCalculatorImpl implements PermissibleModesCalculator {
	private final static String BIKE_LICENSE = "motoL";
	private final static String BIKE_AVAIL = "motoA";
	private final List<String> availableModes;
	private final boolean considerCarAvailability;
	private final boolean considerBikeAvailability;
	private Population population;

	public PermissibleModesCalculatorImpl(
			Population population,
			final String[] availableModes,
			final boolean considerCarAvailability,
			final boolean considerBikeAvailability) {
		this.population = population;
		this.availableModes = Arrays.asList(availableModes);
		
		this.considerCarAvailability = considerCarAvailability;
		this.considerBikeAvailability = considerBikeAvailability;
	}

	@Override
	public Collection<String> getPermissibleModes(final Plan plan) {
		if (!considerCarAvailability && !considerBikeAvailability) return availableModes;

		final Person person;
		try {
			person = plan.getPerson();
		}
		catch (ClassCastException e) {
			throw new IllegalArgumentException( "I need a PersonImpl to get car availability" );
		}

		final boolean license = "1".equals( population.getPersonAttributes().getAttribute(plan.getPerson().getId().toString(), "license") );
		final boolean carAvail = "1".equals( population.getPersonAttributes().getAttribute(plan.getPerson().getId().toString(), "car") );
		final boolean bikeAvail =
				"yes".equals( person.getAttributes().getAttribute(BIKE_LICENSE) ) &&
				"yes".equals( person.getAttributes().getAttribute(BIKE_AVAIL) );

		final List<String> l = new ArrayList<String>( this.availableModes );
		if(!license || Math.random()<.75)
			l.remove("ebike");
		if(considerCarAvailability && !(carAvail && license))
			l.remove("car");
		if(considerCarAvailability && !carAvail)
			l.remove("passenger");
		if(considerBikeAvailability && !bikeAvail)
			l.remove("motorbike");
		String ageStr = (String) population.getPersonAttributes().getAttribute(plan.getPerson().getId().toString(), "age");
		// if there is no age given, e.g., for freight agents
		int age = 25;
		String cleanedAge = ageStr != null?ageStr.replace("age", ""):"25";
		cleanedAge = cleanedAge.replace("up", "");
		if (ageStr != null) age = Integer.parseInt(cleanedAge);
		if (age < 20) l.remove(TransportMode.other);
		if (age > 20) l.remove("schoolbus");
		return l;
	}
}
