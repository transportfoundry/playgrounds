package playground.sergioo.mixedTraffic2017.cityphi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
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
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

public class LinksAnalyzer implements LinkEnterEventHandler, LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, VehicleAbortsEventHandler {
	
	private class IntervalInfo {
		double sumSpeed = 0;
		double weightsSpeed = 0;
		double flow = 0;
		double sumDensity = 0;
		double weightsDensity = 0;
		Map<String, Double> sumSpeedM = new HashMap<>();
		Map<String, Double> weightsSpeedM = new HashMap<>();
		Map<String, Double> flowM = new HashMap<>();
		Map<String, Double> sumDensityM = new HashMap<>();
		Map<String, Double> weightsDensityM = new HashMap<>();
		
		private IntervalInfo(Collection<String> modes) {
			for(String mode:modes) {
				sumSpeedM.put(mode, 0.0);
				weightsSpeedM.put(mode, 0.0);
				flowM.put(mode, 0.0);
				sumDensityM.put(mode, 0.0);
				weightsDensityM.put(mode, 0.0);
			}
		}
	}
	
	private Double interval = 15.0*60;
	private Map<Id<Link>, Map<Integer, IntervalInfo>> intervals = new HashMap<>();
	private Map<Id<Link>, Double> lengths = new HashMap<>();
	
	private Map<Id<Link>, Map<Id<Vehicle>, Double>> prevTimes = new HashMap<>();
	private Map<Id<Link>, Double> prevDTimes = new HashMap<>();
	private Map<Id<Link>, Integer> numVehicles = new HashMap<>();
	private Map<Id<Link>, Map<String, Double>> numVehiclesM = new HashMap<>();
	private Map<Id<Vehicle>, String> modes = new HashMap<>();
	public double maxNumVehicles;
	
	public LinksAnalyzer(Double interval, Collection<String> modes, Network network, double totalTime) {
		if(interval != null && interval>0)
			this.interval = interval;
		for(Link link:network.getLinks().values()) {
			this.lengths.put(link.getId(), link.getLength());
			HashMap<Integer, IntervalInfo> intervalsLink = new HashMap<>();
			intervals.put(link.getId(), intervalsLink);
			for(int time=0; time<totalTime/interval; time++)
				intervalsLink.put(time, new IntervalInfo(modes));
			Map<String, Double> numVehiclesMLink = new HashMap<>();
			numVehiclesM.put(link.getId(), numVehiclesMLink);
			for(String mode:modes)
				numVehiclesMLink.put(mode, 0.0);
		}
	}

	@Override
	public void reset(int iteration) {
		
	}

	public double getDensity(Id<Link> linkId, int time) {
		double weight = intervals.get(linkId).get(time).weightsDensity;
		return intervals.get(linkId).get(time).sumDensity/(weight==0?1:weight);
	}
	
	public double getSpeed(Id<Link> linkId, int time) {
		double weight = intervals.get(linkId).get(time).weightsSpeed;
		return intervals.get(linkId).get(time).sumSpeed/(weight==0?1:weight);
	}
	
	public double getFlow(Id<Link> linkId, int time) {
		return intervals.get(linkId).get(time).flow/interval;
	}
	
	public double getDensity(Id<Link> linkId, String mode, int time) {
		double weight = intervals.get(linkId).get(time).weightsDensityM.get(mode);
		return intervals.get(linkId).get(time).sumDensityM.get(mode)/(weight==0?1:weight);
	}
	
	public double getSpeed(Id<Link> linkId, String mode, int time) {
		double weight = intervals.get(linkId).get(time).weightsSpeedM.get(mode);
		return intervals.get(linkId).get(time).sumSpeedM.get(mode)/(weight==0?1:weight);
	}
	
	public double getFlow(Id<Link> linkId, String mode, int time) {
		return intervals.get(linkId).get(time).flowM.get(mode)/interval;
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
			String mode = modes.get(event.getVehicleId());
			Double prevTime = prevTimes.get(event.getLinkId()).get(event.getVehicleId());
			Double time = prevTime;
			for(int i=(int) (prevTime/interval); i<(int) (event.getTime()/interval); i++) {
				double weight = ((i+1)*interval - time);
				intervals.get(event.getLinkId()).get(i).weightsSpeed += weight;
				Map<String, Double> map = intervals.get(event.getLinkId()).get(i).weightsSpeedM;
				map.put(mode, map.get(mode) + weight);
				intervals.get(event.getLinkId()).get(i).sumSpeed += weight*lengths.get(event.getLinkId())/(event.getTime()-prevTime);
				map = intervals.get(event.getLinkId()).get(i).sumSpeedM;
				map.put(mode, map.get(mode) + weight*lengths.get(event.getLinkId())/(event.getTime()-prevTime));
				time = (i+1)*interval;
			}
			double weight = (event.getTime()-time);
			intervals.get(event.getLinkId()).get((int)(event.getTime()/interval)).weightsSpeed += weight;
			Map<String, Double> map = intervals.get(event.getLinkId()).get((int)(event.getTime()/interval)).weightsSpeedM;
			map.put(mode, map.get(mode) + weight);
			intervals.get(event.getLinkId()).get((int)(event.getTime()/interval)).sumSpeed += weight*lengths.get(event.getLinkId())/(event.getTime()-prevTime);
			map = intervals.get(event.getLinkId()).get((int)(event.getTime()/interval)).sumSpeedM;
			map.put(mode, map.get(mode) + weight*lengths.get(event.getLinkId())/(event.getTime()-prevTime));
			time = prevDTimes.get(event.getLinkId());
			if(time == null)
				time = 0.0;
			for(int i=(int) (time/interval); i<(int) (event.getTime()/interval); i++) {
				weight = ((i+1)*interval - time);
				intervals.get(event.getLinkId()).get(i).weightsDensity += weight;
				map = intervals.get(event.getLinkId()).get(i).weightsDensityM;
				map.put(mode, map.get(mode) + weight);
				intervals.get(event.getLinkId()).get(i).sumDensity += weight*numVehicles.get(event.getLinkId())/lengths.get(event.getLinkId());
				map = intervals.get(event.getLinkId()).get(i).sumDensityM;
				map.put(mode, map.get(mode) + weight*numVehiclesM.get(event.getLinkId()).get(mode)/lengths.get(event.getLinkId()));
				time = (i+1)*interval;
			}
			weight = (event.getTime() - time);
			intervals.get(event.getLinkId()).get((int)(event.getTime()/interval)).weightsDensity += weight;
			map = intervals.get(event.getLinkId()).get((int)(event.getTime()/interval)).weightsDensityM;
			map.put(mode, map.get(mode) + weight);
			intervals.get(event.getLinkId()).get((int)(event.getTime()/interval)).sumDensity += weight*numVehicles.get(event.getLinkId())/lengths.get(event.getLinkId());
			map = intervals.get(event.getLinkId()).get((int)(event.getTime()/interval)).sumDensityM;
			map.put(mode, map.get(mode) + weight*numVehiclesM.get(event.getLinkId()).get(mode)/lengths.get(event.getLinkId()));
			numVehicles.put(event.getLinkId(), numVehicles.get(event.getLinkId())-1);
			numVehiclesM.get(event.getLinkId()).put(mode, numVehiclesM.get(event.getLinkId()).get(mode)-1);
			prevDTimes.put(event.getLinkId(), event.getTime());
			intervals.get(event.getLinkId()).get((int)(event.getTime()/interval)).flow++;
			map = intervals.get(event.getLinkId()).get((int)(event.getTime()/interval)).flowM;
			map.put(mode, map.get(mode) + 1);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		String mode = modes.get(event.getVehicleId());
		Double time = prevDTimes.get(event.getLinkId());
		if(time==null)
			time = 0.0;
		for(int i=(int) (time/interval); i<(int) (event.getTime()/interval); i++) {
			double weight = ((i+1)*interval - time);
			intervals.get(event.getLinkId()).get(i).weightsDensity += weight;
			Map<String, Double> map = intervals.get(event.getLinkId()).get(i).weightsDensityM;
			map.put(mode, map.get(mode) + weight);
			Integer num = numVehicles.get(event.getLinkId());
			if(num == null)
				num = 0;
			intervals.get(event.getLinkId()).get(i).sumDensity += weight*num/lengths.get(event.getLinkId());
			map = intervals.get(event.getLinkId()).get(i).sumDensityM;
			map.put(mode, map.get(mode) + weight*numVehiclesM.get(event.getLinkId()).get(mode)/lengths.get(event.getLinkId()));
			time = (i+1)*interval;
		}
		double weight = (event.getTime() - time);
		intervals.get(event.getLinkId()).get((int)(event.getTime()/interval)).weightsDensity += weight;
		Map<String, Double> map = intervals.get(event.getLinkId()).get((int)(event.getTime()/interval)).weightsDensityM;
		map.put(mode, map.get(mode) + weight);
		Integer num = numVehicles.get(event.getLinkId());
		if(num == null)
			num = 0;
		intervals.get(event.getLinkId()).get((int)(event.getTime()/interval)).sumDensity += weight*num/lengths.get(event.getLinkId());
		map = intervals.get(event.getLinkId()).get((int)(event.getTime()/interval)).sumDensityM;
		map.put(mode, map.get(mode) + weight*numVehiclesM.get(event.getLinkId()).get(mode)/lengths.get(event.getLinkId()));
		numVehicles.put(event.getLinkId(), num+1);
		if(numVehicles.get(event.getLinkId())>maxNumVehicles)
			maxNumVehicles = numVehicles.get(event.getLinkId());
		numVehiclesM.get(event.getLinkId()).put(mode, numVehiclesM.get(event.getLinkId()).get(mode)+1);
		prevDTimes.put(event.getLinkId(), event.getTime());
		Map<Id<Vehicle>, Double> prevTimesLink = prevTimes.get(event.getLinkId());
		if(prevTimesLink==null) {
			prevTimesLink = new HashMap<>();
			prevTimes.put(event.getLinkId(), prevTimesLink);
		}
		prevTimesLink.put(event.getVehicleId(), event.getTime());
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		modes.put(event.getVehicleId(), event.getNetworkMode());
		Integer num = numVehicles.get(event.getLinkId());
		if(num == null)
			num = 0;
		numVehicles.put(event.getLinkId(), num+1);
		numVehiclesM.get(event.getLinkId()).put(event.getNetworkMode(), numVehiclesM.get(event.getLinkId()).get(event.getNetworkMode())+1);
		Map<Id<Vehicle>, Double> prevTimesLink = prevTimes.get(event.getLinkId());
		if(prevTimesLink==null) {
			prevTimesLink = new HashMap<>();
			prevTimes.put(event.getLinkId(), prevTimesLink);
		}
		prevTimesLink.put(event.getVehicleId(), event.getTime());
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		numVehicles.put(event.getLinkId(), numVehicles.get(event.getLinkId())-1);
		numVehiclesM.get(event.getLinkId()).put(event.getNetworkMode(), numVehiclesM.get(event.getLinkId()).get(event.getNetworkMode())-1);
	}

	@Override
	public void handleEvent(VehicleAbortsEvent event) {
		// TODO Auto-generated method stub
		System.out.println();
	}
	
	public static void main(String[] args) throws FileNotFoundException {
		File configFile = new File(args[0]);
		Scenario scenario =ScenarioUtils.createScenario(ConfigUtils.loadConfig(configFile.getAbsolutePath()));
		File networkFile = new File(configFile.getParentFile(),scenario.getConfig().network().getInputFile());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile.getAbsolutePath());
		EventsManager events = EventsUtils.createEventsManager();
		double endTime = scenario.getConfig().qsim().getEndTime();
		if(endTime==0)
			endTime = Double.parseDouble(args[4]);
		double interval = Double.parseDouble(args[2]);
		Collection<String> modes = scenario.getConfig().plansCalcRoute().getNetworkModes();
		LinksAnalyzer analyzer = new LinksAnalyzer(interval, modes, scenario.getNetwork(), endTime);
		events.addHandler(analyzer);
		new MatsimEventsReader(events).readFile(args[1]);
		System.out.println(analyzer.maxNumVehicles);
		PrintWriter writer = new PrintWriter(args[3]);
		writer.println("Id,Time,Mode,Density,Flow,Speed");
		for(Id<Link> id:scenario.getNetwork().getLinks().keySet())
			for(int i=0; i<endTime/interval; i++) {
				writer.println(id.toString()+","+interval*i+",all,"+analyzer.getDensity(id,i)+","+analyzer.getFlow(id,i)+","+analyzer.getSpeed(id,i));
				for(String mode:modes)
					writer.println(id.toString()+","+interval*i+","+mode+","+analyzer.getDensity(id,mode,i)+","+analyzer.getFlow(id,mode,i)+","+analyzer.getSpeed(id,mode,i));
			}
		writer.close();
	}

}
