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

public class CrowdingSpeedCalculator implements LinkSpeedCalculator, LinkEnterEventHandler, LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, VehicleAbortsEventHandler {

	private static final double RATE = 2;
	private static final double SPEED_RATE = 3;
	private Map<Id<Link>, Integer> currentNumber = new HashMap<>();
	private Set<String> heavyModes;
	private Map<Id<Vehicle>, String> modes = new HashMap<>();
	private double heavyModesLength;
	
	
	public CrowdingSpeedCalculator(Set<String> heavyModes, double heavyModesLength) {
		super();
		this.heavyModes = heavyModes;
		this.heavyModesLength = heavyModesLength;
	}

	@Override
	public double getMaximumVelocity(QVehicle vehicle, Link link, double time) {
		Integer c = currentNumber.get(link.getId());
		if(c==null)
			c=0;
		double normalSpeed = Math.min(vehicle.getMaximumVelocity(), link.getFreespeed(time));
		if(!heavyModes.contains(vehicle.getVehicle().getType().getId().toString()))
			return RATE*c>link.getLength()/heavyModesLength?SPEED_RATE*vehicle.getMaximumVelocity():normalSpeed;
		else
			return normalSpeed;
	}

	@Override
	public void reset(int iteration) {
		currentNumber.clear();
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if(heavyModes.contains(modes.get(event.getVehicleId())))
			currentNumber.put(event.getLinkId(), currentNumber.get(event.getLinkId())-1);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if(heavyModes.contains(modes.get(event.getVehicleId()))) {
			Integer num = currentNumber.get(event.getLinkId());
			if(num==null)
				num = 0;
			currentNumber.put(event.getLinkId(), num+1);
		}
	}

	@Override
	public void handleEvent(VehicleAbortsEvent event) {
		if(heavyModes.contains(modes.get(event.getVehicleId())))
			currentNumber.put(event.getLinkId(), currentNumber.get(event.getLinkId())-1);
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		if(heavyModes.contains(modes.get(event.getVehicleId())))
			currentNumber.put(event.getLinkId(), currentNumber.get(event.getLinkId())-1);
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		modes.put(event.getVehicleId(), event.getNetworkMode());
		if(heavyModes.contains(modes.get(event.getVehicleId()))) {
			Integer num = currentNumber.get(event.getLinkId());
			if(num==null)
				num = 0;
			currentNumber.put(event.getLinkId(), num+1);
		}
	}

}
