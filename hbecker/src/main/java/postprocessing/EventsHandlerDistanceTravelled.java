package postprocessing;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.io.IOUtils;

import sharedmobility.CarsharingXmlReaderHenrik;
import sharedmobility.RentalInfo;

public class EventsHandlerDistanceTravelled implements LinkLeaveEventHandler {

	private Map<String,Integer> ptTimes = new HashMap<String, Integer>();
	private Map<String,Integer> ffcsTimes = new HashMap<String, Integer>();
	private Map<String,Integer> carTimes = new HashMap<String, Integer>();

	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		
		
		
		if (event.getVehicleId().toString().contains("bus")) {
			
			if (ptTimes.containsKey(event.getLinkId().toString())) {
				ptTimes.put(event.getLinkId().toString(), ptTimes.get(event.getLinkId().toString()) + 1);
			} else {
				ptTimes.put(event.getLinkId().toString(), 1);
			}
			
			
		} else if (event.getVehicleId().toString().contains("FF")) {
			
			if (ffcsTimes.containsKey(event.getLinkId().toString())) {
				ffcsTimes.put(event.getLinkId().toString(), ffcsTimes.get(event.getLinkId().toString()) + 1);
			} else {
				ffcsTimes.put(event.getLinkId().toString(), 1);
			}
			

		} else {
			
			if (carTimes.containsKey(event.getLinkId().toString())) {
				carTimes.put(event.getLinkId().toString(), carTimes.get(event.getLinkId().toString()) + 1);
			} else {
				carTimes.put(event.getLinkId().toString(), 1);
			}
		
		}
		
		
	}
	
	
	public Map<String,Integer> getPTdata() {
		
		return ptTimes;	
	}

	
	public Map<String,Integer> getFFdata() {
		
		return ffcsTimes;	
	}
	
	
	public Map<String,Integer> getCARdata() {
		
		return carTimes;	
	}
	
	
	
	
	public static void main (String[] args) throws IOException {
		
		EventsManager events = EventsUtils.createEventsManager();
				
		EventsHandlerDistanceTravelled occ = new EventsHandlerDistanceTravelled();
		events.addHandler(occ); 
		
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.readFile(args[0]);
				
		
		try {
		
			
		final BufferedWriter outLink1 = IOUtils.getBufferedWriter("C:/Users/beckerh/SiouxFalls/output_1/LinkFrequencies_pt.txt");
		
			outLink1.write("LinkId,ptCrossings");
			outLink1.newLine();		
		
		for (String linkID : occ.getPTdata().keySet()) {
		
			outLink1.write(linkID + "," + occ.getPTdata().get(linkID));
			outLink1.newLine();	
		}
		
		outLink1.flush();
		outLink1.close();
		
		
		

		final BufferedWriter outLink2 = IOUtils.getBufferedWriter("C:/Users/beckerh/SiouxFalls/output_1/LinkFrequencies_ff.txt");

			outLink2.write("LinkId,ffCrossings");
			outLink2.newLine();		
		
		for (String linkID : occ.getFFdata().keySet()) {
					
			outLink2.write(linkID + "," + occ.getFFdata().get(linkID));
			outLink2.newLine();	
		}
		
		outLink2.flush();
		outLink2.close();

		
		final BufferedWriter outLink3 = IOUtils.getBufferedWriter("C:/Users/beckerh/SiouxFalls/output_1/LinkFrequencies_car.txt");

			outLink3.write("LinkId,carCrossings");
			outLink3.newLine();		
		
		for (String linkID : occ.getCARdata().keySet()) {
		
			outLink3.write(linkID + "," + occ.getCARdata().get(linkID));
			outLink3.newLine();	
		}
		
		outLink3.flush();
		outLink3.close();

		
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	


		
		
	}
	
	
}
