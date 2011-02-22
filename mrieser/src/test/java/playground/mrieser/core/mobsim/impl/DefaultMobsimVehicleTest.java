/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.core.mobsim.impl;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

import playground.mrieser.core.mobsim.fakes.FakeDriverAgent;

/**
 * @author mrieser
 */
public class DefaultMobsimVehicleTest {

	@Test
	public void testSetGetDriver() {
		DefaultMobsimVehicle vehicle = new DefaultMobsimVehicle(null);
		FakeDriverAgent driver = new FakeDriverAgent();
		Assert.assertNull(vehicle.getDriver());
		vehicle.setDriver(driver);
		Assert.assertEquals(driver, vehicle.getDriver());
		vehicle.setDriver(null);
		Assert.assertNull(vehicle.getDriver());
	}

	@Test
	public void testGetSizeInEquivalents() {
		DefaultMobsimVehicle vehicle = new DefaultMobsimVehicle(null);
		Assert.assertEquals(1.0, vehicle.getSizeInEquivalents(), MatsimTestUtils.EPSILON);
		vehicle = new DefaultMobsimVehicle(null, 1.2);
		Assert.assertEquals(1.2, vehicle.getSizeInEquivalents(), MatsimTestUtils.EPSILON);
		vehicle = new DefaultMobsimVehicle(null, 6.0);
		Assert.assertEquals(6.0, vehicle.getSizeInEquivalents(), MatsimTestUtils.EPSILON);
	}

}
