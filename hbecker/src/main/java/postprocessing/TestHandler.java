package postprocessing;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.events.EndRentalEvent;
import org.matsim.contrib.carsharing.events.StartRentalEvent;
import org.matsim.contrib.carsharing.events.handlers.EndRentalEventHandler;
import org.matsim.contrib.carsharing.events.handlers.StartRentalEventHandler;
import org.matsim.contrib.carsharing.manager.demand.AgentRentals;
import org.matsim.contrib.carsharing.manager.demand.RentalInfo;
import org.matsim.contrib.carsharing.manager.demand.VehicleRentals;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;


public class TestHandler  implements ActivityEndEventHandler, ActivityStartEventHandler, PersonEntersVehicleEventHandler,
PersonLeavesVehicleEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler {
	
	Set<String> vehicles = new TreeSet<String>();
	
	int trips = 0;
	double times = 0.0;
	Map<String,Double> enterTimes = new HashMap<String, Double>();
	
	
	// additions
	int bstrips = 0;

	Map<String,RentalInfo> rInfos = new HashMap<String, RentalInfo>();
	Map<String,Integer> nRentals = new HashMap<String, Integer>();
	
	
/*
		RentalInfo info = new RentalInfo();
		info.setCarsharingType(event.getCarsharingType());
		info.setAccessStartTime(event.getTime());
		info.setStartTime(event.getTime());
		info.setOriginLinkId(event.getOriginLinkId());
		info.setPickupLinkId(event.getPickuplinkId());
*/

	
	
	
	public String definePersid(PersonDepartureEvent event)  {
		
		String persid = new String();
		
		if(!nRentals.containsKey(event.getPersonId())) {
			nRentals.put(event.getPersonId().toString(),1);
			persid = event.getPersonId().toString();
			
		} else if(nRentals.get(event.getPersonId()).equals(1)){
			persid = event.getPersonId().toString();
			
		} else {
			persid = event.getPersonId().toString() + "-" + nRentals.get(event.getPersonId().toString());
		}
		
		return persid;
	}
	
	
	public String definePersid(PersonArrivalEvent event)  {
		
		String persid = new String();
		
		if(!nRentals.containsKey(event.getPersonId())) {
			nRentals.put(event.getPersonId().toString(),1);
			persid = event.getPersonId().toString();
			
		} else if(nRentals.get(event.getPersonId()).equals(1)){ 	
			persid = event.getPersonId().toString();
			
		} else { 
			persid = event.getPersonId().toString() + "-" + nRentals.get(event.getPersonId().toString()); 
		}
		
		return persid;
	}
	
		
	
	@Override
	public void handleEvent(PersonDepartureEvent event){
		
		if(event.getLegMode().equals("access_walk_bs")){
			bstrips += 1;
			
			RentalInfo info = new RentalInfo();
			info.setAccessStartTime(event.getTime());
			info.setOriginLinkId(event.getLinkId());
			
			if (!rInfos.containsKey(event.getPersonId().toString())) {
				
				rInfos.put(event.getPersonId().toString(), info);
				nRentals.put(event.getPersonId().toString(),1);
				
			} else {
				
				String newPersId = event.getPersonId().toString() + "-" + nRentals.get(event.getPersonId().toString());
				rInfos.put(newPersId, info);
				nRentals.put(event.getPersonId().toString(),nRentals.get(event.getPersonId().toString()) + 1);
			}
			
		} else if(event.getLegMode().equals("bikeshare_vehicle")){
			
			String persid = definePersid(event);
			RentalInfo info = rInfos.get(persid);
			
			info.setStartTime(event.getTime());
			info.setPickupLinkId(event.getLinkId());
			info.setCarsharingType("bikeshare");
			rInfos.put(persid, info);
			
		} else if(event.getLegMode().equals("egress_walk_bs")){
			
			String persid = definePersid(event);
			RentalInfo info = rInfos.get(persid);
		
			info.setEgressStartTime(event.getTime());
			rInfos.put(persid, info);
			
		}	
		
	}
	
	@Override
	public void handleEvent(PersonArrivalEvent event){
		
		if(event.getLegMode().equals("access_walk_bs")){
			
			String persid = definePersid(event);
			RentalInfo info = rInfos.get(persid);
	
			info.setAccessEndTime(event.getTime());
			rInfos.put(persid, info);
			
		} else if(event.getLegMode().equals("bikeshare_vehicle")){
			
			String persid = definePersid(event);
			RentalInfo info = rInfos.get(persid);
			
			info.setEndTime(event.getTime());
			info.setDropoffLinkId(event.getLinkId());
			rInfos.put(persid, info);
						
		} else if(event.getLegMode().equals("egress_walk_bs")){
			
			String persid = definePersid(event);
			RentalInfo info = rInfos.get(persid);
			
			info.setEgressEndTime(event.getTime());
			info.setEndLinkId(event.getLinkId());
			rInfos.put(persid, info);
		}
	}
	
	
	public Map<String,RentalInfo> getRentalInfos() {
		
		return rInfos;
		
	}
	
	
	
	public int getnbstrips() {
		
		return bstrips;
		
	}
	
	
	
	
	@Override
	public void handleEvent(ActivityStartEvent event) {		
	
	}
	
	@Override
	public void handleEvent(ActivityEndEvent event) {		
	
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
	
		if (!event.getVehicleId().toString().startsWith("FF")) {
		enterTimes.put(event.getVehicleId().toString(), event.getTime());
	
		}
	}
	
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (!event.getVehicleId().toString().startsWith("FF")) {
		
		times += event.getTime() - enterTimes.get(event.getVehicleId().toString());
		vehicles.add(event.getVehicleId().toString());
		trips++;
		}
	}
	
	public double avgTripLength() {
	
	return times/trips;
	}
	
	public double avgTripsPerCar() {
	
	return (double)trips/(double)vehicles.size();
	}
	
	public static void main(String[] args) throws IOException {
		//final BufferedReader readLink = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/garageParkingIds.txt");
		
		EventsManager events = EventsUtils.createEventsManager();
		
		
		TestHandler occ = new TestHandler();
		events.addHandler(occ); 
		
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.readFile(args[0]);
		
		System.out.println(occ.getnbstrips());
		System.out.println(occ.rInfos.isEmpty());
		
		
		
		
		final BufferedWriter outLink = IOUtils.getBufferedWriter("C:/Users/beckerh/SiouxFalls/outputbs1/BS.txt");
		try {
			outLink.write("personID,carsharingType,startTime,endTIme,startLink,pickupLink,dropoffLink,endLink,distance,inVehicleTime,accessTime,egressTime,vehicleID,"
					+ "companyID,vehicleType");
			outLink.newLine();		
		
		for (String personId : occ.getRentalInfos().keySet()) {
		
			outLink.write(personId + "," + occ.getRentalInfos().get(personId));
			outLink.newLine();
			
			/*
			for (RentalInfo i : agentRentalsMap.get(personId).getArr()) {
				CSVehicle vehicle = this.carsharingSupply.getAllVehicles().get(i.getVehId().toString());		
				numberOfRentals++;
				outLink.write(personId + "," + i.toString() + "," + vehicle.getCompanyId() + "," + vehicle.getType());
				outLink.newLine();
			}
			*/
			
		}
		
		outLink.flush();
		outLink.close();
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	

		
		
		
		
		
	}
	

}
