package playground.sergioo.mixedtraffic2016;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

import playground.sergioo.mixedTraffic2017.qsim.QSimModule;
import playground.sergioo.mixedTraffic2017.qsim.QSimProvider;
import playground.sergioo.mixedTraffic2017.qsim.qnetsimengine.ConfigurableQNetworkFactory;
import playground.sergioo.mixedTraffic2017.qsim.qnetsimengine.QNetworkFactory;

public class PrepareScenarioFinal {

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
		controler.run();
	}

}
