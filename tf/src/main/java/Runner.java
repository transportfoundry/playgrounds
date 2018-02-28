import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.File;

public class Runner {

    private static void makeOmxTrips(String dir, Scenario scenario, Double samplingRate){
        String crs = "EPSG:3518";
        File omxFile = new File(dir + "asheville.omx");
        File topoFile = new File(dir + "centroids.topojson");

        Omx2Population o2p = new Omx2Population(
                scenario, omxFile, topoFile, crs, samplingRate);

        o2p.buildPopulation();

        new PopulationWriter(scenario.getPopulation())
                .write(dir + "population.xml.gz");
    }

    public static void main(String args[]){

        String dir = "tf/src/main/resources/";
        Config config = ConfigUtils.loadConfig(dir + "config.xml");
        Scenario scenario = ScenarioUtils.createScenario(config);

        //double is sampling rate
        Double samplingRate = 1.0;
        makeOmxTrips(dir, scenario, samplingRate);
        Controler controler = new Controler(config);
        controler.run();
    }
}
