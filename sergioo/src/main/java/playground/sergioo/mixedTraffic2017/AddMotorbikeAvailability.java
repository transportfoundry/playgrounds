package playground.sergioo.mixedTraffic2017;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.pt.router.TransitActsRemover;
import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;
import playground.sergioo.mixedTraffic2017.population.StreamingPopulationWriter;

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by sergioo on 11/8/17.
 */
public class AddMotorbikeAvailability {

    private static class Household {
        int numBikes;
        Map<String, Integer> licenses = new HashMap<>() ;

        public Household(int numBikes) {
            this.numBikes = numBikes;
        }
    }
    private static Map<String,Household> households = new HashMap<>();
    private static Map<String,Household> persons = new HashMap<>();

    public static void main4(String[] args) throws IOException {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(args[0]);
        for(Link link:scenario.getNetwork().getLinks().values()) {
            Set<String> modes = link.getAllowedModes();
            if (modes.contains("motorbike")) {
                Set<String> newModes = new HashSet<>(modes);
                newModes.add("ebike");
                link.setAllowedModes(newModes);
            }
        }
        new NetworkWriter(scenario.getNetwork()).write(args[1]);
    }

    public static void main(String[] args) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IOException, NoConnectionException {
        DataBaseAdmin admin = new DataBaseAdmin(new File(args[0]));

        ResultSet resultSet = admin.executeQuery("SELECT hhid, simulated_choice_motor, persid, motorcycle_license " +
                "FROM p_bmw_motor.motorcycle_ownership_simulated_hh_persons_increase");
        while(resultSet.next()) {
            String hid = resultSet.getString(1);
            Household household = households.get(hid);
            if(household == null) {
                household = new Household(resultSet.getInt(2));
                households.put(hid, household);
            }
            household.licenses.put(resultSet.getString(3), resultSet.getInt(4));
            persons.put(resultSet.getString(3), household);
        }
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        StreamingPopulationReader reader = new StreamingPopulationReader(scenario) ;
        StreamingPopulationWriter writer = new StreamingPopulationWriter();
        reader.addAlgorithm(new PersonAlgorithm() {
            @Override
            public void run(Person person) {
                String pid = person.getId().toString();
                Household household = persons.get(pid);
                if(household != null) {
                    boolean avail = household.numBikes > 0;
                    boolean license = household.licenses.get(pid) > 0;
                    person.getAttributes().putAttribute("motoA", avail ? "yes" : "never");
                    person.getAttributes().putAttribute("motoL", license ? "yes" : "no");
                    if (avail && license)
                        for (Plan plan : person.getPlans())
                            for (PlanElement element : plan.getPlanElements())
                                if (element instanceof Leg && ((Leg) element).getMode().equals("car"))
                                    ((Leg) element).setMode("motorbike");
                }
            }
        });
        /*reader.addAlgorithm(new PersonAlgorithm() {
            private final TransitActsRemover transitActsRemover = new TransitActsRemover();
            @Override
            public void run(Person person) {
                for(Plan plan:person.getPlans())
                    transitActsRemover.run(plan);
            }
        });*/
        reader.addAlgorithm(writer);
        writer.startStreaming(args[2]);
        reader.readFile(args[1]);
        writer.closeStreaming();
    }

    public static void main2(String[] args) throws IOException {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimFacilitiesReader(scenario).readFile(args[3]);
        StreamingPopulationReader reader = new StreamingPopulationReader(scenario) ;
        final Map<String, Set<String>> noActs = new HashMap<>();
        reader.addAlgorithm(new PersonAlgorithm() {
            @Override
            public void run(Person person) {
                for(Plan plan:person.getPlans())
                    for(PlanElement planElement:plan.getPlanElements())
                        if(planElement instanceof Activity) {
                            Activity act = (Activity)planElement;
                            if(act.getFacilityId()!=null) {
                                ActivityFacility facility = scenario.getActivityFacilities().getFacilities().get(act.getFacilityId());
                                if(facility != null && facility.getActivityOptions().get(act.getType()) == null) {
                                    facility.addActivityOption(new ActivityOptionImpl(act.getType()));
                                }
                            }
                        }
            }
        });
        reader.readFile(args[4]);
        new FacilitiesWriter(scenario.getActivityFacilities()).write(args[5]);
    }
    public static void main1(String[] args) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(args[2]));
        String line = reader.readLine();
        for(int i = 0; i<100; i++) {
            System.out.println(line);
            line = reader.readLine();
        }
        reader.close();
    }
    public static void main0(String[] args) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IOException, NoConnectionException {
        DataBaseAdmin admin = new DataBaseAdmin(new File(args[0]));

        ResultSet resultSet = admin.executeQuery("SELECT hhid, simulated_choice_motor, persid, motorcycle_license " +
                                                    "FROM p_bmw_motor.motorcycle_ownership_simulated_hh_persons_increase");
        while(resultSet.next()) {
            String hid = resultSet.getString(1);
            Household household = households.get(hid);
            if(household == null) {
                household = new Household(resultSet.getInt(2));
                households.put(hid, household);
            }
            household.licenses.put(resultSet.getString(3), resultSet.getInt(4));
            persons.put(resultSet.getString(3), household);
        }
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(args[1]);
        for(Person person:scenario.getPopulation().getPersons().values()) {
            String pid = person.getId().toString();
            Household household = persons.get(pid);
            if(household != null) {
                boolean avail = household.numBikes > 0;
                boolean license = household.licenses.get(pid) > 0;
                person.getAttributes().putAttribute("motoA", avail ? "yes" : "never");
                person.getAttributes().putAttribute("motoL", license ? "yes" : "no");
                if (avail && license)
                    for (Plan plan : person.getPlans())
                        for (PlanElement element : plan.getPlanElements())
                            if (element instanceof Leg && ((Leg) element).getMode().equals("car"))
                                ((Leg) element).setMode("motorbike");
            }
        }
        new PopulationWriter(scenario.getPopulation()).write(args[2]);
    }
}
