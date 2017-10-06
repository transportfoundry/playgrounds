package playground.jmolloy.externalitiesAnalysis.handlers;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.contrib.emissions.types.WarmPollutant;
import playground.vsp.congestion.events.CongestionEvent;
import playground.vsp.congestion.handlers.CongestionEventHandler;

import java.util.*;

/**
 * Created by molloyj on 18.07.2017.
 */
public class CongestionAggregator implements CongestionEventHandler, LinkEnterEventHandler, PersonDepartureEventHandler, WarmEmissionEventHandler, ColdEmissionEventHandler {
    private static final Logger log = Logger.getLogger(CongestionAggregator.class);
    private final double binSize_s; //30 hours, how do we split them
    private int num_bins; //30 hours, how do we split them

    private Map<Id<Link>, Map<String, double[]>> linkId2timeBin2values = new HashMap<>();
    private Map<Id<Link>, double[]> linkId2timeBin2enteringAndDepartingAgents = new HashMap<Id<Link>, double[]>();

    private List<CongestionEvent> congestionEvents = new ArrayList<CongestionEvent>();
    private List<LinkEnterEvent> linkEnterEvents = new ArrayList<LinkEnterEvent>();
    private List<PersonDepartureEvent> personDepartureEvents = new ArrayList<PersonDepartureEvent>();

    private boolean setMethodsExecuted = false;

    private double vtts_car;
    private double congestionTollFactor;

    public CongestionAggregator(Scenario scenario, double congestionTollFactor, double binSize_s) {
        this.vtts_car = (scenario.getConfig().planCalcScore().getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() - scenario.getConfig().planCalcScore().getPerforming_utils_hr()) / scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
        this.congestionTollFactor = congestionTollFactor;
        log.info("VTTS_car: " + vtts_car);
        log.info("Congestion toll factor: " + congestionTollFactor);
        this.binSize_s = binSize_s;
        this.num_bins = (int) (30 * 3600 / binSize_s);
        log.info("Number of congestion bins: " + num_bins);

        setUpBinsForLinks(scenario);

    }

    private void setUpBinsForLinks(Scenario scenario) {
        scenario.getNetwork().getLinks().keySet().forEach(l -> {
            linkId2timeBin2values.put(l, new HashMap<>());
            linkId2timeBin2enteringAndDepartingAgents.put(l, new double[num_bins]);
        });
    }

    public CongestionAggregator(Scenario scenario, int binSize_s) {
        this(scenario, 1.0, binSize_s);
    }

    @Override
    public void reset(int iteration) {
        this.linkId2timeBin2values.clear();
        this.linkId2timeBin2enteringAndDepartingAgents.clear();

        this.congestionEvents.clear();
        this.linkEnterEvents.clear();
        this.personDepartureEvents.clear();

        this.setMethodsExecuted = false;
    }

    /*package*/ int getTimeBin(double time) {

        double timeAfterSimStart = time;

		/*
		 * Agents who end their first activity before the simulation has started
		 * will depart in the first time step.
		 */
        if (timeAfterSimStart <= 0.0) return 0;

		/*
		 * Calculate the bin for the given time. Increase it by one if the result
		 * of the modulo operation is > 0. If it is 0, it is the last time value
		 * which is part of the previous bin.
		 */
        int bin = (int) ((timeAfterSimStart/60) / binSize_s);
        if (timeAfterSimStart % binSize_s != 0.0) bin++;

        return bin;
    }

    @Override
    public void handleEvent(CongestionEvent event) {

        //this.congestionEvents.add(event);
        int bin = getTimeBin(event.getEmergenceTime());
        //this.linkId2timeBin2delaySum.putIfAbsent(event.getLinkId(), new double[num_bins]);
        this.linkId2timeBin2values.get(event.getLinkId()).putIfAbsent("delay", new double[num_bins]);
        this.linkId2timeBin2values.get(event.getLinkId()).get("delay")[bin] += event.getDelay();

    }


    @Override
    public void handleEvent(ColdEmissionEvent event) {
        int bin = getTimeBin(event.getTime());
        //this.linkId2timeBin2delaySum.putIfAbsent(event.getLinkId(), new double[num_bins]);
        Map<ColdPollutant, Double> pollutants = event.getColdEmissions();
        for (Map.Entry<ColdPollutant, Double> p : pollutants.entrySet()) {
            String pollutant = p.getKey().getText();
            this.linkId2timeBin2values.get(event.getLinkId()).putIfAbsent(pollutant, new double[num_bins]);
            this.linkId2timeBin2values.get(event.getLinkId()).get(pollutant)[bin] += p.getValue();
        }
        this.linkId2timeBin2values.get(event.getLinkId()).putIfAbsent("ColdEmissionsCount", new double[num_bins]);
        this.linkId2timeBin2values.get(event.getLinkId()).get("ColdEmissionsCount")[bin]++;


    }

    @Override
    public void handleEvent(WarmEmissionEvent event) {
        int bin = getTimeBin(event.getTime());
        //this.linkId2timeBin2delaySum.putIfAbsent(event.getLinkId(), new double[num_bins]);
        Map<WarmPollutant, Double> pollutants = event.getWarmEmissions();
        for (Map.Entry<WarmPollutant, Double> p : pollutants.entrySet()) {
            String pollutant = p.getKey().getText();
            this.linkId2timeBin2values.get(event.getLinkId()).putIfAbsent(pollutant, new double[num_bins]);
            this.linkId2timeBin2values.get(event.getLinkId()).get(pollutant)[bin] += p.getValue();
        }
        this.linkId2timeBin2values.get(event.getLinkId()).putIfAbsent("WarmEmissionsCount", new double[num_bins]);
        this.linkId2timeBin2values.get(event.getLinkId()).get("WarmEmissionsCount")[bin]++;
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        int bin = getTimeBin(event.getTime());

        this.linkEnterEvents.add(event);
        //this.linkId2timeBin2enteringAndDepartingAgents.putIfAbsent(event.getLinkId(), new double[num_bins]);
        this.linkId2timeBin2enteringAndDepartingAgents.get(event.getLinkId())[bin]++; //TODO: do we have to consider PCU;s here?
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (event.getLegMode().equals(TransportMode.car.toString())) {
            this.personDepartureEvents.add(event);

            int bin = getTimeBin(event.getTime());
            //this.linkId2timeBin2enteringAndDepartingAgents.putIfAbsent(event.getLinkId(), new double[num_bins]);
            this.linkId2timeBin2enteringAndDepartingAgents.get(event.getLinkId())[bin]++; //TODO: do we have to consider PCU;s here?

        } else {
            // other simulated modes are not accounted for
        }
    }

    public Map<Id<Link>, double[]> getLinkIdAverageDelays() {
        Map<Id<Link>, double[]> averageDelays = new HashMap<>();
        for (Map.Entry<Id<Link>, Map<String, double[]>> e : linkId2timeBin2values.entrySet()) {
            double[] a = e.getValue().get("delay").clone();
            double[] counts = linkId2timeBin2enteringAndDepartingAgents.get(e.getKey());
            for (int i=0; i<counts.length; i++) {
                a[i] /= counts[i];
            }
            averageDelays.put(e.getKey(), a);
        }
        return averageDelays;
    }

}
