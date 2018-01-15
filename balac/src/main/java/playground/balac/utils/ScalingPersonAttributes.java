package playground.balac.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

public class ScalingPersonAttributes {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimReader populationReader = new PopulationReader(sc);
		//MatsimNetworkReader networkReader = new MatsimNetworkReader(sc.getNetwork());
		//networkReader.readFile(args[0]);
		populationReader.readFile(args[0]);
				
		BufferedReader readLink = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/scenario/scenario_10p/abmt_population_attributes.xml.gz");
		final BufferedWriter outLink = IOUtils.getBufferedWriter("C:/Users/balacm/Desktop/scenario/scenario_10p/abmt_population_attributes_scaled.xml.gz");

		outLink.write(readLink.readLine());
		outLink.newLine();
		outLink.write(readLink.readLine());
		outLink.newLine();

		outLink.write(readLink.readLine());
		outLink.newLine();

		outLink.write(readLink.readLine());
		outLink.newLine();

		String s = readLink.readLine();
				
		while(!s.contains("</objectAttributes>")) {
			if (s.contains("object")) {
				if (sc.getPopulation().getPersons().containsKey(Id.create(s.substring(s.indexOf("=") + 2, s.indexOf(">") - 1), Person.class))){
					outLink.write(s);
					outLink.newLine();
					s = readLink.readLine();
					while (!s.contains("</object>")) {
						
						outLink.write(s);
						outLink.newLine();
	
						s = readLink.readLine();
						
					}
					outLink.write(s);
					outLink.newLine();
					s = readLink.readLine();
				}
				else
					s = readLink.readLine();
			}
			else
				s = readLink.readLine();
			
		}
		outLink.write(s);
		outLink.flush();
		outLink.close();

	}

}
