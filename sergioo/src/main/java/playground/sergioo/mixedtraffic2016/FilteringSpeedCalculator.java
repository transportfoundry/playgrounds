package playground.sergioo.mixedtraffic2016;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleAbortsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

import playground.sergioo.mixedTraffic2017.qsim.qnetsimengine.QVehicle;
import playground.sergioo.mixedTraffic2017.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;

public class FilteringSpeedCalculator implements LinkSpeedCalculator, LinkEnterEventHandler, LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, VehicleAbortsEventHandler {

	private static double PS = 0.4;
	private Map<Id<Link>, Double> currentNumber = new HashMap<>();
	private Set<String> heavyModes;
	private Map<Id<Vehicle>, String> modes = new HashMap<>();
	private Map<String, Double> heavyModesLengths;
	
	
	public FilteringSpeedCalculator(Set<String> heavyModes, Map<String, Double> heavyModesLengths) {
		super();
		this.heavyModes = heavyModes;
		this.heavyModesLengths = heavyModesLengths;
	}

	public FilteringSpeedCalculator(Set<String> heavyModes, Map<String, Double> heavyModesLengths, double PS) {
		super();
		this.heavyModes = heavyModes;
		this.heavyModesLengths = heavyModesLengths;
		FilteringSpeedCalculator.PS = PS;
	}
	
	@Override
	public double getMaximumVelocity(QVehicle vehicle, Link link, double time) {
		double normalSpeed = Math.min(vehicle.getMaximumVelocity(), link.getFreespeed(time));
		if(!heavyModes.contains(vehicle.getVehicle().getType().getId().toString())) {
			Double c = currentNumber.get(link.getId());
			if(c==null)
				c = 0.0;
			normalSpeed/=(1-((c/link.getNumberOfLanes())/link.getLength())*(1-1/PS));
		}
		return normalSpeed;
	}

	@Override
	public void reset(int iteration) {
		currentNumber.clear();
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if(heavyModes.contains(modes.get(event.getVehicleId())))
			currentNumber.put(event.getLinkId(), currentNumber.get(event.getLinkId())-heavyModesLengths.get(modes.get(event.getVehicleId())));
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if(heavyModes.contains(modes.get(event.getVehicleId()))) {
			Double num = currentNumber.get(event.getLinkId());
			if(num == null)
				num = 0.0;
			currentNumber.put(event.getLinkId(), num + heavyModesLengths.get(modes.get(event.getVehicleId())));
		}
	}

	@Override
	public void handleEvent(VehicleAbortsEvent event) {
		if(heavyModes.contains(modes.get(event.getVehicleId())))
			currentNumber.put(event.getLinkId(), currentNumber.get(event.getLinkId())-heavyModesLengths.get(modes.get(event.getVehicleId())));
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		if(heavyModes.contains(modes.get(event.getVehicleId())))
			currentNumber.put(event.getLinkId(), currentNumber.get(event.getLinkId())-heavyModesLengths.get(modes.get(event.getVehicleId())));
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		modes.put(event.getVehicleId(), event.getNetworkMode());
		if(heavyModes.contains(modes.get(event.getVehicleId()))) {
			Double num = currentNumber.get(event.getLinkId());
			if(num == null)
				num = 0.0;
			currentNumber.put(event.getLinkId(), num + heavyModesLengths.get(modes.get(event.getVehicleId())));
		}
	}

}
