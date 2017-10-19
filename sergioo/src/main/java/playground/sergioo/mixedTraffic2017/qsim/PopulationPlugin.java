package playground.sergioo.mixedTraffic2017.qsim;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;

import com.google.inject.Module;

import playground.sergioo.mixedTraffic2017.qsim.agents.AgentFactory;
import playground.sergioo.mixedTraffic2017.qsim.agents.DefaultAgentFactory;
import playground.sergioo.mixedTraffic2017.qsim.agents.PopulationAgentSource;
import playground.sergioo.mixedTraffic2017.qsim.agents.TransitAgentFactory;

public class PopulationPlugin extends AbstractQSimPlugin {

	public PopulationPlugin(Config config) {
		super(config);
	}

	@Override
	public Collection<? extends Module> modules() {
		Collection<Module> result = new ArrayList<>();
		result.add(new com.google.inject.AbstractModule() {
			@Override
			protected void configure() {
				bind(PopulationAgentSource.class).asEagerSingleton();
				if (getConfig().transit().isUseTransit()) {
					bind(AgentFactory.class).to(TransitAgentFactory.class).asEagerSingleton();
				} else {
					bind(AgentFactory.class).to(DefaultAgentFactory.class).asEagerSingleton();
				}
			}
		});
		return result;
	}

	@Override
	public Collection<Class<? extends AgentSource>> agentSources() {
		Collection<Class<? extends AgentSource>> result = new ArrayList<>();
		result.add(PopulationAgentSource.class);
		return result;
	}
}
