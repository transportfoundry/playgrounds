package postprocessing;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import sharedmobility.BikeshareDemandHandler;

public class RunSharedMobilityEventsHandler {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		//path to events file
		String inputFile = "C:/Users/beckerh/SiouxFalls/outputbs1/ITERS/it.5/5.events.xml.gz";

		//create an event object
		EventsManager events = EventsUtils.createEventsManager();

		//create the handler and add it
		EventsHandler1 handler1 = new EventsHandler1();
		EventsHandler2 handler2 = new EventsHandler2();
		
		BikeshareDemandHandler handler3 = new BikeshareDemandHandler();
		
		events.addHandler(handler1);
		events.addHandler(handler2);
		//events.addHandler(handler3);
				
        //create the reader and read the file
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(inputFile);
		
		System.out.println("average travel time: " + handler1.getTotalTravelTime());

		System.out.println(handler3.getAgentRentalsMap().isEmpty());
		
		
		
	}

}
