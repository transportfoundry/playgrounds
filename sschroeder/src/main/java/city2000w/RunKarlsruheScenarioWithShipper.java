/**
 * 
 */
package city2000w;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.mzilske.city2000w.AgentObserver;
import playground.mzilske.city2000w.City2000WMobsimFactory;
import playground.mzilske.city2000w.DefaultLSPShipmentTracker;
import playground.mzilske.freight.CarrierAgentFactory;
import playground.mzilske.freight.CarrierAgentTracker;
import playground.mzilske.freight.CarrierImpl;
import playground.mzilske.freight.CarrierPlan;
import playground.mzilske.freight.Carriers;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.TSPAgentTracker;
import playground.mzilske.freight.TSPContract;
import playground.mzilske.freight.TSPPlan;
import playground.mzilske.freight.TransportServiceProviderImpl;
import playground.mzilske.freight.TransportServiceProviders;
import freight.AnotherCarrierAgentFactory;
import freight.CarrierPlanReader;
import freight.ShipperAgentTracker;
import freight.ShipperPlanReader;
import freight.Shippers;
import freight.TSPPlanReader;

/**
 * @author schroeder
 *
 */
public class RunKarlsruheScenarioWithShipper implements StartupListener, BeforeMobsimListener, AfterMobsimListener, IterationEndsListener {

	private static Logger logger = Logger.getLogger(RunKarlsruheScenarioWithShipper.class);
	
	private Carriers carriers;
	
	private TransportServiceProviders transportServiceProviders;
	
	private CarrierAgentTracker carrierAgentTracker;
	
	private TSPAgentTracker tspAgentTracker;
	
	private ShipperAgentTracker shipperAgentTracker;
	
	private Shippers shippers;

	private ScenarioImpl scenario;
	
	private AgentObserver agentObserver;
	
	private static final String NETWORK_FILENAME = "../playgrounds/sschroeder/networks/karlsruheNetwork.xml";
	
	private static final String TSPPLAN_FILENAME = "../playgrounds/sschroeder/anotherInput/karlsruheTsps.xml";
	
	private static final String CARRIERNPLAN_FILENAME = "../playgrounds/sschroeder/anotherInput/karlsruheCarriers.xml";

	private static final String SHIPPERPLAN_FILENAME = "../playgrounds/sschroeder/anotherInput/karlsruheShipperPlans.xml";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Logger.getRootLogger().setLevel(org.apache.log4j.Level.INFO);
		RunKarlsruheScenarioWithShipper runner = new RunKarlsruheScenarioWithShipper();
		runner.run();
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		Controler controler = event.getControler();
		
		readCarriers();
		
		readTransportServiceProviders();
		
		readShippers();
		
		shipperAgentTracker = new ShipperAgentTracker(shippers.getShippers());
		
		tspAgentTracker = new TSPAgentTracker(transportServiceProviders.getTransportServiceProviders());
		
		tspAgentTracker.getCostListeners().add(new DefaultLSPShipmentTracker());
		
		createTspContracts(shipperAgentTracker.createTSPContracts());
		
		createTspPlans();
		
		createCarrierContracts(tspAgentTracker.createCarrierContracts());
		
		createCarrierPlans();
		
		event.getControler().getScenario().addScenarioElement(carriers);
		
		CarrierAgentFactory carrierAgentFactory = new AnotherCarrierAgentFactory(scenario.getNetwork(), controler.createRoutingAlgorithm());
		carrierAgentTracker = new CarrierAgentTracker(carriers.getCarriers().values(), controler.createRoutingAlgorithm(), scenario.getNetwork(), carrierAgentFactory);
		carrierAgentTracker.getShipmentStatusListeners().add(tspAgentTracker);
		carrierAgentTracker.getCostListeners().add(tspAgentTracker);
		
		City2000WMobsimFactory mobsimFactory = new City2000WMobsimFactory(0, carrierAgentTracker);
		mobsimFactory.setUseOTFVis(true);
		event.getControler().setMobsimFactory(mobsimFactory);
		
		agentObserver = new AgentObserver("foo",scenario.getNetwork());
		agentObserver.setOutFile("../playgrounds/sschroeder/output/karlsruhe.txt");
		event.getControler().getEvents().addHandler(agentObserver);
	
		
	}

	private void createTspPlans() {
		for(TransportServiceProviderImpl tsp : transportServiceProviders.getTransportServiceProviders()){
			SimpleTSPPlanBuilder tspPlanBuilder = new SimpleTSPPlanBuilder(scenario.getNetwork());
			tspPlanBuilder.setCarriers(carriers.getCarriers().values());
			tspPlanBuilder.setTransshipmentCentres(Collections.EMPTY_LIST);
			TSPPlan directPlan = tspPlanBuilder.buildPlan(tsp.getContracts(),tsp.getTspCapabilities());
			tsp.getPlans().add(directPlan);
			tsp.setSelectedPlan(directPlan);
		}
	}

	private void createTspContracts(Collection<TSPContract> tspContracts) {
		for(TSPContract c : tspContracts){
			TransportServiceProviderImpl tsp = findTsp(c.getOffer().getTspId());
			tsp.getContracts().add(c);
		}
		
	}

	private TransportServiceProviderImpl findTsp(Id tspId) {
		for(TransportServiceProviderImpl tsp : transportServiceProviders.getTransportServiceProviders()){
			if(tsp.getId().equals(tspId)){
				return tsp;
			}
		}
		return null;
	}

	private void readShippers() {
		shippers = new Shippers();
		new ShipperPlanReader(shippers.getShippers()).read(SHIPPERPLAN_FILENAME);
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		Controler controler = event.getControler();
		controler.getEvents().addHandler(carrierAgentTracker);
		carrierAgentTracker.createPlanAgents();
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		Controler controler = event.getControler();
		controler.getEvents().removeHandler(carrierAgentTracker);
	}


	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		logger.info("Reset costs/score of tspAgents");
		tspAgentTracker.reset();
		agentObserver.reset(0);
		agentObserver.writeStats();
	}


	private void run(){
		Config config = new Config();
		config.addCoreModules();
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		scenario = (ScenarioImpl)ScenarioUtils.createScenario(config);
		readNetwork(NETWORK_FILENAME);
		NetworkCleaner networkCleaner = new NetworkCleaner();
		networkCleaner.run(scenario.getNetwork());
		
		Controler controler = new Controler(scenario);
		/*
		 * muss ich auf 'false' setzen, da er mir sonst eine exception wirft, weil er das matsim-logo nicht finden kann
		 * ich hab keine ahnung wo ich den pfad des matsim-logos setzen kann
		 * 
		 */
		controler.setCreateGraphs(false);
		controler.addControlerListener(this);
		controler.setOverwriteFiles(true);
		controler.run();
	}

	private void readNetwork(String networkFilename) {
		new MatsimNetworkReader(scenario).readFile(networkFilename);
	}

	private void createCarrierPlans() {
		for(CarrierImpl carrier : carriers.getCarriers().values()){
			RuinAndRecreatePickupAndDeliveryCarrierPlanBuilder planBuilder = new RuinAndRecreatePickupAndDeliveryCarrierPlanBuilder(scenario.getNetwork());
//			RuinAndRecreateCarrierPlanBuilder planBuilder = new RuinAndRecreateCarrierPlanBuilder(scenario.getNetwork());
//			ClarkeAndWrightCarrierPlanBuilder planBuilder = new ClarkeAndWrightCarrierPlanBuilder(scenario.getNetwork());
			CarrierPlan plan = planBuilder.buildPlan(carrier.getCarrierCapabilities(), carrier.getContracts());
			carrier.getPlans().add(plan);
			carrier.setSelectedPlan(plan);
		}
	}

	private void createCarrierContracts(List<Contract> contracts) {
		for(Contract contract : contracts){
			Id carrierId = contract.getOffer().getCarrierId();
			carriers.getCarriers().get(carrierId).getContracts().add(contract);
		}
	}

	private void readCarriers() {
		Collection<CarrierImpl> carriers = new ArrayList<CarrierImpl>();
		new CarrierPlanReader(carriers).read(CARRIERNPLAN_FILENAME);
		this.carriers = new Carriers(carriers);
	}

	private void readTransportServiceProviders() {
		transportServiceProviders = new TransportServiceProviders();
		new TSPPlanReader(transportServiceProviders.getTransportServiceProviders()).read(TSPPLAN_FILENAME);
	}

}
