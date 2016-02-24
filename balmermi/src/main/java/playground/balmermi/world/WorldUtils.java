/* *********************************************************************** *
 * project: org.matsim.*
 * WorldUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.balmermi.world;

import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * A collection of some helper methods for the world.<br />
 * It all started with getRandomCoordInZone, which I needed in
 * several places. Integrating into Zone didn't seem very clever,
 * as I still had to pass the layer as parameter. Thus I decided to make
 * a static method out of it.
 *
 * @author mrieser, and maybe others
 */
public abstract class WorldUtils {

	/**
	 * Returns a random coordinate within the given zone. If the zone has a real
	 * bounding box set (min and max coordinates are not equal to center), than a
	 * random point within this bounding box will be chosen. If the bounding box
	 * is equal the center coordinate, a random point within a circle with radius
	 * 0.7 * distance-to-nearest-neighbor-zone will be returned. The random point
	 * is generated by randomly choosing polar-coordinates for this point. This
	 * leads to a higher density of randomly generated points near the center of
	 * the circle when multiple random points are generated.
	 *
	 * @param zone
	 * @param layer the layer <code>zone</code> is part of
	 * @return a random coordinate within the zone
	 *
	 * @author mrieser
	 */
	public static final Coord getRandomCoordInZone(final Zone zone, final Layer layer) {
		Coord min = zone.getMin();

		if ((min != null) && (!(min.equals(zone.getMax())))) {
			// we know min and max of the zone-area, choose randomly in this area
			Coord max = zone.getMax();
			double x = min.getX() + MatsimRandom.getRandom().nextDouble()*(max.getX() - min.getX());
			double y = min.getY() + MatsimRandom.getRandom().nextDouble()*(max.getY() - min.getY());
			return new Coord(x, y);
		}

		double x, y;
		// min is not defined --> place the random point within a circle around the center
		// first, determine radius of circle. for this, search the nearest (neighbor) zone
		Coord center = zone.getCoord();
		ArrayList<? extends BasicLocation> nearestZones = layer.getNearestLocations(center, zone);
		double shortestDistance = Double.MAX_VALUE;
		Iterator<? extends BasicLocation> zoneIter = nearestZones.iterator();
		while (zoneIter.hasNext()) {
			Zone aZone = (Zone)zoneIter.next();
			Coord zoneMin = aZone.getMin();
			Coord zoneCenter = aZone.getCoord();
			double radius;
			if (zoneMin == null || zoneMin.equals(aZone.getMax())) {
				// the distance is center-to-center, only take 0.7 times the distance as radius
				radius = 0.7*CoordUtils.calcEuclideanDistance(zoneCenter, center);
			} else {
				// the other zone has an extent(min/max), so just use the full distance
				radius = CoordUtils.calcEuclideanDistance(aZone.getCoord(), center);
			}
			if (radius < shortestDistance) {
				shortestDistance = radius;
			}
		}
		if (shortestDistance < Double.MAX_VALUE) {
			// choose a 'random' point within the circle around 'center' with radius 'shortestDistance'
			// by choosing a random polar coordinate, the densitiy of points in the center area should
			// be higher than in the area far away from the center, which is plausible for the distribution
			// of people in a rural area with a town as a center somewhere
			double angle = MatsimRandom.getRandom().nextDouble()*2*Math.PI;
			double radius = MatsimRandom.getRandom().nextDouble()*shortestDistance;
			x = center.getX() + radius * Math.cos(angle);
			y = center.getY() + radius * Math.sin(angle);
		} else {
			x = center.getX();
			y = center.getY();
		}
		return new Coord(x, y);
	}

}
