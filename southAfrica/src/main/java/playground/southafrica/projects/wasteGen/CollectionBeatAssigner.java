/* *********************************************************************** *
 * project: org.matsim.*
 * CollectionBeatAssigner.java
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
package playground.southafrica.projects.wasteGen;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.households.Household;
import org.matsim.households.HouseholdsReaderV10;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.attributeconverters.CoordConverter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

import playground.southafrica.utilities.Header;

/**
 * Class to assign each household to a (geographical) collection beat.
 * 
 * @author jwjoubert
 */
public class CollectionBeatAssigner {
	final private static Logger LOG = Logger.getLogger(CollectionBeatAssigner.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(CollectionBeatAssigner.class.toString(), args);
		CollectionBeatAssigner.run(args);
		Header.printFooter();
	}
	
	public static void run(String[] args) {
		String shapefile = args[0];
		String households = args[1];
		String householdAttributes = args[2];
		String output = args[3];
		
		LOG.info("Building household QuadTree...");
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new HouseholdsReaderV10(sc.getHouseholds()).readFile(households);
		ObjectAttributesXmlReader oar = new ObjectAttributesXmlReader(sc.getHouseholds().getHouseholdAttributes());
		oar.putAttributeConverter(Coord.class, new CoordConverter());
		oar.readFile(householdAttributes);
		
		Map<Id<Household>, Household> hhMap = new TreeMap<Id<Household>, Household>();
		Map<Id<Household>, Coord> coordMap = new TreeMap<Id<Household>, Coord>();
		Map<Id<Household>, Coord> coordMapWgs = new TreeMap<Id<Household>, Coord>();
		List<Id<Household>> hhMapped = new ArrayList<>(hhMap.size());
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(
				TransformationFactory.HARTEBEESTHOEK94_LO19, TransformationFactory.WGS84);
		
		/* Get QuadTree extent. */
		double xMin = Double.POSITIVE_INFINITY;
		double xMax = Double.NEGATIVE_INFINITY;
		double yMin = Double.POSITIVE_INFINITY;
		double yMax = Double.NEGATIVE_INFINITY;
		for(Id<Household> hhId : sc.getHouseholds().getHouseholds().keySet()) {
			Household hh = sc.getHouseholds().getHouseholds().get(hhId);
			Object o = sc.getHouseholds().getHouseholdAttributes().getAttribute(hhId.toString(), "homeCoord");
			if(o instanceof Coord) {
				Coord c = (Coord) o;
				Coord cWgs = ct.transform(c);
				hhMap.put(hhId, hh);
				coordMap.put(hhId, c);
				coordMapWgs.put(hhId, cWgs);
				xMin = Math.min(xMin, cWgs.getX());
				xMax = Math.max(xMax, cWgs.getX());
				yMin = Math.min(yMin, cWgs.getY());
				yMax = Math.max(yMax, cWgs.getY());
			}
		}
		/* Populate QuadTree. */
		QuadTree<Id<Household>> qt = new QuadTree<Id<Household>>(xMin, yMin, xMax, yMax);
		for(Id<Household> hhId : hhMap.keySet()) {
			Coord c = coordMapWgs.get(hhId);
			qt.put(c.getX(), c.getY(), hhId);
		}
		LOG.info("Done building QuadTree. Total of " + qt.size() + " households.");
		
		LOG.info("Parsing beat shapefile...");
		List<String> lines = new ArrayList<String>(hhMap.size());
		GeometryFactory gf = new GeometryFactory();
		ShapeFileReader sfr = new ShapeFileReader();
		Collection<SimpleFeature> features = sfr.readFileAndInitialize(shapefile);
		int foundHouseholds = 0;
		Counter counter = new Counter("   beats # ");
		for(SimpleFeature sf : features) {
			Object o = sf.getDefaultGeometry();
			if(o instanceof MultiPolygon) {
				MultiPolygon mp = (MultiPolygon) o;
				String colDay = sf.getAttribute("COL_DAY").toString();
				String colDayColour = sf.getAttribute("COL_DAY_CL").toString();
				String serviceArea = sf.getAttribute("SW_SRV_ARE").toString();
				String provider = sf.getAttribute("OTSD").toString();
				
				String beatId = sf.getID();
				Geometry envelope = mp.getEnvelope();
				Point centroid = envelope.getCentroid();
				Coordinate[] ca = envelope.getCoordinates();
				double radiusAroundCentroid = Double.parseDouble(String.format("%.8f", centroid.distance(gf.createPoint(ca[0]))));
				
				Collection<Id<Household>> possibleHouseholds = qt.getDisk(centroid.getX(), centroid.getY(), radiusAroundCentroid);
				Iterator<Id<Household>> iterator = possibleHouseholds.iterator();
				while(iterator.hasNext()) {
					Id<Household> hhId = iterator.next();
					Coord hhCoordWgs = coordMapWgs.get(hhId);
					Coord hhCoord = coordMap.get(hhId);
					Point hhPoint = gf.createPoint(new Coordinate(hhCoordWgs.getX(), hhCoordWgs.getY()));
					if(envelope.covers(hhPoint)) {
						if(mp.covers(hhPoint)) {
							foundHouseholds++;	
							hhMapped.add(hhId);
							
							int members = hhMap.get(hhId).getMemberIds().size();
							
							/* Build the output string. TODO Expand later when necessary */
							String line = String.format("%s,%.6f,%.6f,%.0f,%.0f,%d,%s,%s,%s,%s,%s", 
									hhId,
									hhCoordWgs.getX(), hhCoordWgs.getY(),
									hhCoord.getX(), hhCoord.getY(),
									members,
									beatId,
									colDay, colDayColour, serviceArea, provider);
							lines.add(line);
						}
					}
				}
			}
			counter.incCounter();
		}
		counter.printCounter();
		LOG.info("Found households: " + foundHouseholds);
		
		/* Write the output to file. */
		LOG.info("Writing output to file...");
		BufferedWriter bw = IOUtils.getBufferedWriter(output);
		counter = new Counter("   lines # ");
		try {
			bw.write("hhId,lon,lat,x,y,hhSize,beatId,colDay,colDayCol,serviceArea,provider");
			bw.newLine();
			for(String line : lines) {
				bw.write(line);
				bw.newLine();
				counter.incCounter();
			}
			counter.printCounter();
			
			/* Write all the unmapped households. */
			LOG.info("Identifying unserved households (" + (hhMap.size() - hhMapped.size()) + ")...");
			counter = new Counter("   lines # ");
			for(Id<Household> hhId : hhMap.keySet()) {
				if(!hhMapped.contains(hhId)) {
					Coord hhCoord = coordMap.get(hhId);
					Coord hhCoordWgs = coordMapWgs.get(hhId);
					int members = hhMap.get(hhId).getMemberIds().size();
					String line = String.format("%s,%.6f,%.6f,%.0f,%.0f,%d,%s,%s,%s,%s,%s", 
							hhId,
							hhCoordWgs.getX(), hhCoordWgs.getY(),
							hhCoord.getX(), hhCoord.getY(),
							members,
							-1,
							"NA", "NA", "NA", "NA");
					bw.write(line);
					bw.newLine();
					counter.incCounter();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + output);
		} finally {
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + output);
			}
		}
		counter.printCounter();
		LOG.info("Done writing output.");
	}

}
