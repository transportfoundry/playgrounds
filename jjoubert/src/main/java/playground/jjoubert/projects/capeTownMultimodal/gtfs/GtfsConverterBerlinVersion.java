/* *********************************************************************** *
 * project: org.matsim.*
 * GtfsConverterBerlinVersion.java
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.gtfs.GtfsConverter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.pt.utils.CreatePseudoNetwork;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleCapacityImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.VehiclesFactory;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OTFVisConfigGroup.ColoringScheme;

import com.conveyal.gtfs.GTFSFeed;

import playground.mrieser.pt.utils.MergeNetworks;
import playground.southafrica.utilities.Header;

/**
 * Class to combine multiple modes into single network. The public transport 
 * networks are <i>pseudonetworks</i>, meaning they run on their own networks
 * with no interference by car.<br><br>
 * <b>Updates:</b><br>
 * 201604: Added MyCiTi GTFS; using WGS84_SA_Albers.
 * 201710: Added Metrorail GTFS; using HARTEBEESTHOEK94_Lo19. 
 * 
 * @author jwjoubert
 */
public class GtfsConverterBerlinVersion {
	final private static Logger LOG = Logger.getLogger(GtfsConverterBerlinVersion.class);
	final private static String CRS_CT = TransformationFactory.HARTEBEESTHOEK94_LO19;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(GtfsConverterBerlinVersion.class.toString(), args);
		String gtfsMyCiTi = args[0];
		String gtfsMetrorail = args[1];
		String transitFolder = args[2];
		transitFolder += transitFolder.endsWith("/") ? "" : "/";
		String roadNetworkFile = args[3];
		String outputNetworkFile = args[4];
		
		Scenario scMyCiTi = GtfsConverterBerlinVersion.convert(gtfsMyCiTi, "MyCiTi", "brt");
		Scenario scMetrorail = GtfsConverterBerlinVersion.convert(gtfsMetrorail, "Metrorail", "rail");

		List<Scenario> ptScenarios = new ArrayList<Scenario>();
		ptScenarios.add(scMyCiTi);
		ptScenarios.add(scMetrorail);
		
		GtfsConverterBerlinVersion.mergeNetworks(roadNetworkFile, ptScenarios, outputNetworkFile, transitFolder);
		
		Header.printFooter();
	}
	
	
	private static void mergeNetworks(String inputNetwork, List<Scenario> ptScenarios, String outputNetwork, String transitFolder){
		LOG.trace("Merging networks...");
		LOG.trace("    base: " + inputNetwork);
		Network baseNetwork = NetworkUtils.createNetwork();
		new MatsimNetworkReader(baseNetwork ).readFile(inputNetwork);
		String prefix = "";

		/* Merge the network. */
		for(Scenario sc : ptScenarios){
			MergeNetworks.merge(baseNetwork, prefix, sc.getNetwork());
		}
		LOG.trace("Done merging networks.");
		new NetworkWriter(baseNetwork).write(outputNetwork);

		LOG.trace("Merging transit services...");
		/* Merge the transit services. */
		Scenario baseTransit = ptScenarios.get(0);
		for(int i = 1; i < ptScenarios.size(); i++){
			baseTransit = mergeTransitServices(baseTransit, ptScenarios.get(i));
		}
		LOG.trace("Done merging transit services...");
		
		/* Set up the transit vehicles. Strt by adding all the 'known' vehicle
		 * types. */
		baseTransit.getTransitVehicles().addVehicleType(MyCiTiVehicles.Type.MyCiTi_9m.getVehicleType());
		baseTransit.getTransitVehicles().addVehicleType(MyCiTiVehicles.Type.MyCiTi_12m.getVehicleType());
		baseTransit.getTransitVehicles().addVehicleType(MyCiTiVehicles.Type.MyCiTi_18m.getVehicleType());
		baseTransit.getTransitVehicles().addVehicleType(MyCiTiVehicles.Type.MyCiTi_dummy.getVehicleType());
		baseTransit.getTransitVehicles().addVehicleType(MetrorailVehicles.Type.Metrorail.getVehicleType());
		VehiclesFactory vb = baseTransit.getVehicles().getFactory();
		int brtId = 0;
		int railId = 0;
		for(TransitLine line : baseTransit.getTransitSchedule().getTransitLines().values()){
			for(TransitRoute route : line.getRoutes().values()){
				String mode = route.getTransportMode();
				for(Departure departure : route.getDepartures().values()){
					Vehicle vehicle = null;
					switch (mode) {
					case "brt":
						/*FIXME This should be expanded to also look at the 
						 * route Id and the realistic vehicle sizes. */
						vehicle = vb.createVehicle(
								Id.createVehicleId("brt_" + String.valueOf(brtId++)), 
								MyCiTiVehicles.Type.MyCiTi_dummy.getVehicleType());
						baseTransit.getTransitVehicles().addVehicle(vehicle);
						departure.setVehicleId(vehicle.getId());
						break;
					case "rail":
						/*FIXME This should be expanded to also look at the 
						 * route Id and the realistic vehicle sizes. */
						vehicle = vb.createVehicle(
								Id.createVehicleId("rail_" + String.valueOf(railId++)), 
								MetrorailVehicles.Type.Metrorail.getVehicleType());
						baseTransit.getTransitVehicles().addVehicle(vehicle);
						departure.setVehicleId(vehicle.getId());
						break;

					default:
						throw new RuntimeException("Don't know what to do with mode " + mode);
					}
				}
			}
		}
		
		new TransitScheduleWriter(baseTransit.getTransitSchedule()).writeFile(transitFolder + "transitSchedule.xml.gz");
		new VehicleWriterV1(((MutableScenario)baseTransit).getTransitVehicles()).writeFile(transitFolder + "transitVehicles.xml.gz");
	}
	
	
	private static Scenario mergeTransitServices(Scenario base, Scenario add){
		/* Add stops. */
		for(TransitStopFacility stop : add.getTransitSchedule().getFacilities().values()){
			base.getTransitSchedule().addStopFacility(stop);
		}
		
		/* Add transit lines. */
		for(TransitLine line : add.getTransitSchedule().getTransitLines().values()){
			base.getTransitSchedule().addTransitLine(line);
		}
		return base;
	}
	
	
	private static Scenario convert(String gtfsFile, String prefix, String mode){
		Scenario sc = parseScenario(gtfsFile);

		/* Convert the GTFS feed. */
		System.out.println("Scenario has " + sc.getNetwork().getLinks().size() + " links.");
		sc.getConfig().controler().setMobsim("qsim");
		sc.getConfig().qsim().setSnapshotStyle( SnapshotStyle.queue );
		sc.getConfig().qsim().setSnapshotPeriod(1);
		sc.getConfig().qsim().setRemoveStuckVehicles(false);
		ConfigUtils.addOrGetModule(sc.getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setColoringScheme(ColoringScheme.gtfs);
		ConfigUtils.addOrGetModule(sc.getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setDrawTransitFacilities(false);
		sc.getConfig().transitRouter().setMaxBeelineWalkConnectionDistance(1.0);
		
		/* Build a pseudo-network. */
		CreatePseudoNetwork cpn = new CreatePseudoNetwork(
				sc.getTransitSchedule(),
				sc.getNetwork(),
				prefix + "_");
		cpn.createNetwork();
		
		/* Change all the links' allowed mode. */
		for(Link link : sc.getNetwork().getLinks().values()){
			link.setAllowedModes(Collections.singleton(mode));
		}
		for(TransitLine line : sc.getTransitSchedule().getTransitLines().values()){
			for(TransitRoute route : line.getRoutes().values()){
				route.setTransportMode(mode);
			}
		}
		
		
		return sc;
	}

	
	private static Scenario parseScenario(String gtfsFile){
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem(CRS_CT);
		config.controler().setLastIteration(0);
		config.transit().setUseTransit(true);
		
		Scenario sc = ScenarioUtils.createScenario(config);
		
		GTFSFeed feed = GTFSFeed.fromFile(gtfsFile);
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", CRS_CT);
		GtfsConverter gtfs = new GtfsConverter(feed, sc, ct);
		gtfs.convert();
		LOG.info("Number of transit lines: " + sc.getTransitSchedule().getTransitLines().size());
		
		return sc;
	}
}
