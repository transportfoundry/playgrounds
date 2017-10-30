package playground.sergioo.mixedTraffic2017.cityphi;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.vehicles.Vehicle;

import playground.sergioo.weeklySimulation.scenario.ScenarioUtils;

public class Trajectories implements LinkEnterEventHandler, LinkLeaveEventHandler, VehicleEntersTrafficEventHandler {

	private class Point {
		String id;
		double time;
		double x;
		double y;
		double z;
		public Point(String id, double time, double x, double y, double z) {
			super();
			this.id = id;
			this.time = time;
			this.x = x;
			this.y = y;
			this.z = z;
		}
		public String getString() {
			return id+","+time+","+x+","+y+","+z;
		}
	}

	private Map<String, Collection<Point>> pointModes = new HashMap<>();
	private Map<Id<Vehicle>, String> vehicleModes = new HashMap<>();
	private Network network;
	
	public Trajectories(Network network) {
		this.network = network;
	}

	private void save(String fileName) throws FileNotFoundException {
		for(Entry<String,Collection<Point>> points:pointModes.entrySet()) {
			PrintWriter writer = new PrintWriter(fileName+"_"+points.getKey()+".csv");
			writer.println("id,time,x,y,z");
			for(Point point:points.getValue())
				writer.println(point.getString());
			writer.close();
		}
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		String mode = vehicleModes.get(event.getVehicleId());
		Collection<Point> points = pointModes.get(mode);
		if(points == null) {
			points = new ArrayList<>();
			pointModes.put(mode, points);
		}
		Coord coord = network.getLinks().get(event.getLinkId()).getFromNode().getCoord();
		points.add(new Point(event.getVehicleId().toString(), event.getTime(), coord.getX(), coord.getY(), 0));
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		String mode = vehicleModes.get(event.getVehicleId());
		Collection<Point> points = pointModes.get(mode);
		if(points == null) {
			points = new ArrayList<>();
			pointModes.put(mode, points);
		}
		Coord coord = network.getLinks().get(event.getLinkId()).getToNode().getCoord();
		points.add(new Point(event.getVehicleId().toString(), event.getTime(), coord.getX(), coord.getY(), 0));
	}
	
	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		vehicleModes.put(event.getVehicleId(), event.getNetworkMode());
	}

	/**
	 * @param args
	 * 0 - Network file
	 * 1 - Events file
	 * 2 - Output file
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(args[0]);
		Trajectories trajectories = new Trajectories(network);
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(trajectories);
		new MatsimEventsReader(events).readFile(args[1]);
		trajectories.save(args[2]);
	}
	
}
