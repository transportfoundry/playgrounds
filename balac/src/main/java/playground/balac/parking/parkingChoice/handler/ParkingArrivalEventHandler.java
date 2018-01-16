package playground.balac.parking.parkingChoice.handler;

import org.matsim.core.events.handler.EventHandler;

import playground.balac.parking.parkingChoice.events.ParkingArrivalEvent;


public interface ParkingArrivalEventHandler extends EventHandler {
	public void handleEvent (ParkingArrivalEvent event);
}
