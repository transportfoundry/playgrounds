package playground.sergioo.mixedTraffic2017;

import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

import playground.sergioo.mixedTraffic2017.qsim.QSimModule;
import playground.sergioo.mixedTraffic2017.qsim.QSimProvider;

/**
 * Created by sergioo on 24/2/17.
 */
public class RunControler {
    public static void main(String[] args) {
        final Controler controler = new Controler(ScenarioUtils.loadScenario(ConfigUtils.loadConfig(args[0])));
        controler.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
        controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				install(new QSimModule());
				this.bindMobsim().toProvider(QSimProvider.class);
			}
		});
        controler.run();
    }
}
