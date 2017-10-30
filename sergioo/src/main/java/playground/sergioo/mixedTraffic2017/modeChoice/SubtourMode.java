package playground.sergioo.mixedTraffic2017.modeChoice;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.config.groups.TimeAllocationMutatorConfigGroup;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacilities;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Created by cuauhtemoc on 22/6/17.
 */
public class SubtourMode implements Provider<PlanStrategy> {
    @Inject
    private GlobalConfigGroup globalConfigGroup;
    @Inject
    private SubtourModeChoiceConfigGroup subtourModeChoiceConfigGroup;
    @Inject
    private PlansConfigGroup plansConfigGroup;
    @Inject
    private Provider<TripRouter> tripRouterProvider;
    @Inject
    private Population population;
    @Inject
    private ActivityFacilities facilities;

    public SubtourMode() {

    }

    public PlanStrategy get() {
        PlanStrategyImpl strategy = (PlanStrategyImpl) new PlanStrategyImpl.Builder(new RandomPlanSelector<Plan, Person>()).build();
        strategy.addStrategyModule(new SubtourModeWithBike(population, tripRouterProvider, globalConfigGroup, subtourModeChoiceConfigGroup));
        strategy.addStrategyModule(new ReRoute(facilities, tripRouterProvider, globalConfigGroup));
		return strategy;
    }
}
