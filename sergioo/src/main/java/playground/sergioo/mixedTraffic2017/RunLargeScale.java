package playground.sergioo.mixedTraffic2017;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.eventsBasedPTRouter.TransitRouterEventsWSFactory;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeCalculator;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTime;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeStuckCalculator;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTime;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;
import org.matsim.pt.router.TransitRouter;
import playground.sergioo.mixedTraffic2017.qsim.QSimModule;
import playground.sergioo.mixedTraffic2017.qsim.QSimProvider;
import playground.sergioo.mixedTraffic2017.qsim.qnetsimengine.ConfigurableQNetworkFactory;
import playground.sergioo.mixedTraffic2017.qsim.qnetsimengine.QNetworkFactory;
import playground.sergioo.mixedTraffic2017.router.EBikeTravelDisutilityFactory;
import playground.sergioo.mixedtraffic2016.FilteringSpeedCalculator;
import playground.singapore.springcalibration.run.SingaporeConfigGroup;
import playground.singapore.springcalibration.run.SingaporeControlerListener;
import playground.singapore.springcalibration.run.SingaporeIterationEndsListener;
import playground.singapore.springcalibration.run.SubpopTravelDisutility;
import playground.singapore.springcalibration.run.roadpricing.SubpopRoadPricingModule;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class RunLargeScale {
	private final static Logger log = Logger.getLogger(RunLargeScale.class);

	public static void main(String[] args) {
		Controler controler = new Controler(ScenarioUtils.loadScenario(ConfigUtils.loadConfig(args[0])));
		controler.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				install(new QSimModule());
				this.bindMobsim().toProvider(QSimProvider.class);
			}
		});
		String[] modes = args[1].split(",");
		Map<String, Double> sizes = new HashMap<>();
		Double size = Double.parseDouble(args[2]);
		for(String mode:modes)
			sizes.put(mode, size);
		FilteringSpeedCalculator csc = new FilteringSpeedCalculator(new HashSet<>(Arrays.asList(modes)), sizes, Double.parseDouble(args[3]));
		final ConfigurableQNetworkFactory factory = new ConfigurableQNetworkFactory( controler.getEvents(), controler.getScenario() ) ;
		factory.setLinkSpeedCalculator(csc);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(QNetworkFactory.class).toInstance(factory);
				addEventHandlerBinding().toInstance(csc);
			}
		});
		Config config = controler.getConfig();
		Scenario scenario = controler.getScenario();
		final SingaporeConfigGroup singaporeConfigGroup = ConfigUtils.addOrGetModule(
				scenario.getConfig(), SingaporeConfigGroup.GROUP_NAME, SingaporeConfigGroup.class);

		ScoringParametersForPerson parameters = new SubpopulationScoringParameters( controler.getScenario() );

		// scoring function
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {

			@Inject
			Network network;
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				final ScoringParameters params = parameters.getScoringParameters( person );

				SumScoringFunction sumScoringFunction = new SumScoringFunction();
				sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, network));
				// this is the Singaporean scorer with Open times:
				sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params, new FacilityOpeningIntervalCalculator(scenario.getActivityFacilities())));
				//sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params)) ;

				sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
				sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(params));

				return sumScoringFunction;
			}
		}) ;

		final SubpopTravelDisutility.Builder builder_schoolbus =  new SubpopTravelDisutility.Builder("schoolbus", parameters);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding("schoolbus").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("schoolbus").toInstance(builder_schoolbus);
			}
		});

		final SubpopTravelDisutility.Builder builder_passenger =  new SubpopTravelDisutility.Builder("passenger", parameters);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding("passenger").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("passenger").toInstance(builder_passenger);
			}
		});

		final SubpopTravelDisutility.Builder builder_other =  new SubpopTravelDisutility.Builder(TransportMode.other, parameters);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding(TransportMode.other).to(networkTravelTime());
				addTravelDisutilityFactoryBinding(TransportMode.other).toInstance(builder_other);
			}
		});

		final SubpopTravelDisutility.Builder builder_freight =  new SubpopTravelDisutility.Builder("freight", parameters);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding("freight").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("freight").toInstance(builder_freight);
			}
		});

		final SubpopTravelDisutility.Builder builder_taxi =  new SubpopTravelDisutility.Builder("taxi", parameters);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding("taxi").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("taxi").toInstance(builder_taxi);
			}
		});

		SubpopRoadPricingModule rpModule = new SubpopRoadPricingModule(scenario, config);
		controler.setModules(rpModule);

		controler.addControlerListener(new SingaporeControlerListener());

		controler.addControlerListener(new SingaporeIterationEndsListener());

		// Singapore transit router: --------------------------------------------------
		final WaitTimeStuckCalculator waitTimeCalculator = new WaitTimeStuckCalculator(
				controler.getScenario().getPopulation(),
				controler.getScenario().getTransitSchedule(),
				controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(),
				(int) (controler.getConfig().qsim().getEndTime() - controler.getConfig().qsim().getStartTime()));
		controler.getEvents().addHandler(waitTimeCalculator);

		log.info("About to init StopStopTimeCalculator...");
		final StopStopTimeCalculator stopStopTimeCalculator = new StopStopTimeCalculator(
				controler.getScenario().getTransitSchedule(),
				controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(),
				(int) (controler.getConfig().qsim().getEndTime() - controler.getConfig().qsim().getStartTime()));
		controler.getEvents().addHandler(stopStopTimeCalculator);
		log.info("About to init TransitRouterWSImplFactory...");

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(WaitTime.class).toInstance(waitTimeCalculator.getWaitTimes());
				bind(StopStopTime.class).toInstance(stopStopTimeCalculator.getStopStopTimes());
				bind(TransitRouter.class).toProvider(TransitRouterEventsWSFactory.class);
				addTravelDisutilityFactoryBinding("ebike").toInstance(
						new EBikeTravelDisutilityFactory(getConfig().planCalcScore()));
			}
		});
		// TODO: also take into account waiting times and stop times in scoring?!
		// -----------------------------------------------------------------------------

		controler.run();
		log.info("finished SingaporeControlerRunner");
	}

}
