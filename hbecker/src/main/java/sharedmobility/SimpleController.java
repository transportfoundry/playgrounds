package sharedmobility;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.carsharing.runExample.CarsharingUtils;
import org.matsim.contrib.carsharing.runExample.RunCarsharing;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class SimpleController {

	public static void main(String[] args) {
		Logger.getLogger( "org.matsim.core.controler.Injector" ).setLevel(Level.OFF);

		final Config config = ConfigUtils.loadConfig(args[0]);
		
		if(Integer.parseInt(config.getModule("qsim").getValue("numberOfThreads")) > 1)
			Logger.getLogger( "org.matsim.core.controler" ).warn("Carsharing contrib is not stable for parallel qsim!! If the error occures please use 1 as the number of threads.");
		
		CarsharingUtils.addConfigModules(config);

		final Scenario sc = ScenarioUtils.loadScenario(config);

		final Controler controler = new Controler( sc );
		
		RunCarsharing.installCarSharing(controler);
		
		controler.run();

	}

}
