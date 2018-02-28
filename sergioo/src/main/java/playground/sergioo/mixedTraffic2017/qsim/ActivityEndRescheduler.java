/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.sergioo.mixedTraffic2017.qsim;

import org.matsim.core.mobsim.framework.MobsimAgent;

/**
 * @author nagel
 *
 */
public interface ActivityEndRescheduler {
	@Deprecated // use same method from QSim directly and try to get rid of the handle to internal interface. kai, mar'15
	public void rescheduleActivityEnd(MobsimAgent agent);

}