package playground.balac.parking.parkingChoice.handler;

import org.matsim.core.events.handler.EventHandler;

import playground.balac.parking.parkingChoice.events.ParkingDepartureEvent;


public interface ParkingDepartureEventHandler extends EventHandler {
	public void handleEvent (ParkingDepartureEvent event);

}
