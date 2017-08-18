/* *********************************************************************** *
 * project: org.matsim.*
 * DgTaController
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package signals.laemmer.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.model.AbstractSignalController;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalController;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.lanes.data.Lane;
import org.matsim.lanes.data.Lanes;

import com.google.inject.Provider;

import playground.dgrether.koehlerstrehlersignal.analysis.TtTotalDelay;
import signals.Analyzable;
import signals.downstreamSensor.DownstreamSensor;
import signals.sensor.LinkSensorManager;


/**
 * @author dgrether
 * @author tthunig
 * @author nkuehnel
 */
public class LaemmerSignalController extends AbstractSignalController implements SignalController, Analyzable {

    public static final String IDENTIFIER = "LaemmerSignalController";

    private Queue<LaemmerSignal> regulationQueue = new LinkedList<>();

    private final LaemmerConfig laemmerConfig;
    private final List<LaemmerSignal> laemmerSignals = new ArrayList<>();

    private double desiredPeriod;

    private final double MIN_G;
    private double maxPeriod;

    private Request activeRequest = null;
    private LinkSensorManager sensorManager;
    
    private DownstreamSensor downstreamSensor;

    private Network network;
    private Lanes lanes;

    private final double DEFAULT_INTERGREEN;
    private double tIdle;

    private double flowSum;

    private TtTotalDelay delayCalculator;


    public final static class SignalControlProvider implements Provider<SignalController> {
        private final LaemmerConfig laemmerConfig;
        private final LinkSensorManager sensorManager;
        private final Network network;
        private final TtTotalDelay delayCalculator;
        private final Lanes lanes;
		private final DownstreamSensor downstreamSensor;

        public SignalControlProvider(LaemmerConfig laemmerConfig, LinkSensorManager sensorManager, Network network, Lanes lanes, TtTotalDelay delayCalculator, DownstreamSensor downstreamSensor) {
            this.laemmerConfig = laemmerConfig;
            this.sensorManager = sensorManager;
            this.network = network;
            this.lanes = lanes;
            this.delayCalculator = delayCalculator;
            this.downstreamSensor = downstreamSensor;
        }

        @Override
        public SignalController get() {
            return new LaemmerSignalController(laemmerConfig, sensorManager, network, lanes, delayCalculator, downstreamSensor);
        }
    }


    private LaemmerSignalController(LaemmerConfig laemmerConfig, LinkSensorManager sensorManager, Network network, Lanes lanes, TtTotalDelay delayCalculator, DownstreamSensor downstreamSensor) {
        this.laemmerConfig = laemmerConfig;
        this.sensorManager = sensorManager;
        this.network = network;
        this.lanes = lanes;
        this.delayCalculator = delayCalculator;
        desiredPeriod = laemmerConfig.getDesiredCycleTime();
        maxPeriod = laemmerConfig.getMaxCycleTime();
        this.MIN_G = laemmerConfig.getMinGreenTime();
        DEFAULT_INTERGREEN = laemmerConfig.getDefaultIntergreenTime();
        this.downstreamSensor = downstreamSensor;
    }

    @Override
    public void simulationInitialized(double simStartTimeSeconds) {
        this.initializeSensoring();
        for (SignalGroup group : this.system.getSignalGroups().values()) {
            this.system.scheduleDropping(simStartTimeSeconds, group.getId());
            LaemmerSignal laemmerSignal = new LaemmerSignal(group);
            laemmerSignals.add(laemmerSignal);
        }
    }

    @Override
    public boolean analysisEnabled() {
        return this.laemmerConfig.isAnalysisEnabled();
    }


    @Override
    public void updateState(double now) {
        updateRepresentativeDriveways(now);
        if (!laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.OPTIMIZING)) {
            updateActiveRegulation(now);
        }
        updateSignals(now);
        if(activeRequest != null && activeRequest.signal.group.getState().equals(SignalGroupState.GREEN)) {
            double remainingMinG = activeRequest.time + MIN_G - now;
//            double remainingInBetweenTime = Math.max(activeRequest.time - now, 0);
//            double remainingMinG = Math.max(activeRequest.time - now + MIN_G - remainingInBetweenTime, 0);
            if (remainingMinG > 0) {
                return;
            }
        }
        LaemmerSignal selection = selectSignal();
        processSelection(now, selection);
    }

    private void updateActiveRegulation(double now) {
        if (activeRequest != null && !regulationQueue.isEmpty() && regulationQueue.peek().equals(activeRequest.signal)) {
            LaemmerSignal signal = regulationQueue.peek();
            int n;
            if (signal.determiningLane != null) {
                n = getNumberOfExpectedVehiclesOnLane(now, signal.determiningLink, signal.determiningLane);
            } else {
                n = getNumberOfExpectedVehiclesOnLink(now, signal.determiningLink);
            }
            if (activeRequest.signal.regulationTime + activeRequest.time - now <= 0 || n == 0) {
                regulationQueue.poll();
            }
        }
    }

    private LaemmerSignal selectSignal() {
        LaemmerSignal max = null;
        if (!laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.OPTIMIZING)) {
            max = regulationQueue.peek();
        }
        if (!laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.STABILIZING)) {
            if (max == null) {
                double index = 0;
                for (LaemmerSignal signal : laemmerSignals) {
                    if (signal.index > index) {
                    	// if downstream check enabled, only select signals that do not lead to occupied links
                    	if (!laemmerConfig.isCheckDownstream() || downstreamSensor.allDownstreamLinksEmpty(system.getId(), signal.group.getId())){
                    		max = signal;
                        	index = signal.index;
                    	}
                    }
                }
            }
        }
        return max;
    }

    private void processSelection(double now, LaemmerSignal max) {

        if (activeRequest != null && activeRequest.signal != max) {
            this.system.scheduleDropping(now, activeRequest.signal.group.getId());
            activeRequest = null;
        }

        if (activeRequest == null && max != null || activeRequest != null && max != null && !max.equals(activeRequest.signal)) {
            activeRequest = new Request(now + DEFAULT_INTERGREEN, max);
        }

        if (activeRequest != null) {
            if (activeRequest.isDue(now)) {
                this.system.scheduleOnset(now, activeRequest.signal.group.getId());
            } else if (activeRequest.time > now) {
            }
        }
    }

    private void updateSignals(double now) {
        for (LaemmerSignal signal : laemmerSignals) {
            signal.update(now);
            // this is already done in updateStabilization in LaemmerSignal called by the above, theresa jul'17
//            if (signal.stabilize && !regulationQueue.contains(signal)) {
//                regulationQueue.add(signal);
//            }
        }
    }

    private void updateRepresentativeDriveways(double now) {
        flowSum = 0;
        tIdle = desiredPeriod;
        for (LaemmerSignal signal : laemmerSignals) {
            signal.determineRepresentativeDriveway(now);
            flowSum += signal.outflowSum;
            tIdle -= Math.max(signal.determiningLoad * desiredPeriod + DEFAULT_INTERGREEN, MIN_G);
        }
        tIdle = Math.max(0, tIdle);
    }

    private int getNumberOfExpectedVehiclesOnLink(double now, Id<Link> linkId) {
        return this.sensorManager.getNumberOfCarsInDistance(linkId, 0., now);
    }

    private int getNumberOfExpectedVehiclesOnLane(double now, Id<Link> linkId, Id<Lane> laneId) {
        if (lanes.getLanesToLinkAssignments().get(linkId).getLanes().size() == 1) {
            return getNumberOfExpectedVehiclesOnLink(now, linkId);
        } else {
            return this.sensorManager.getNumberOfCarsInDistanceOnLane(linkId, laneId, 0., now);
        }
    }

    private double getAverageArrivalRate(double now, Id<Link> linkId) {
        if (this.laemmerConfig.getLinkArrivalRate(linkId) != null) {
            return this.laemmerConfig.getLinkArrivalRate(linkId);
        } else {
            return this.sensorManager.getAverageArrivalRateOnLink(linkId, now);
        }
    }

    private double getAverageLaneArrivalRate(double now, Id<Link> linkId, Id<Lane> laneId) {
        if (lanes.getLanesToLinkAssignments().get(linkId).getLanes().size() > 1) {
            if (this.laemmerConfig.getLaneArrivalRate(linkId, laneId) != null) {
                return this.laemmerConfig.getLaneArrivalRate(linkId, laneId);
            } else {
                return this.sensorManager.getAverageArrivalRateOnLane(linkId, laneId, now);
            }
        } else {
            return getAverageArrivalRate(now, linkId);
        }
    }


    @Override
    public void reset(Integer iterationNumber) {
    }

    private void initializeSensoring() {
        for (SignalGroup group : this.system.getSignalGroups().values()) {
            for (Signal signal : group.getSignals().values()) {
                if (signal.getLaneIds() != null && !(signal.getLaneIds().isEmpty())) {
                    for (Id<Lane> laneId : signal.getLaneIds()) {
                        this.sensorManager.registerNumberOfCarsOnLaneInDistanceMonitoring(signal.getLinkId(), laneId, 0.);
                        this.sensorManager.registerAverageNumberOfCarsPerSecondMonitoringOnLane(signal.getLinkId(), laneId);
                    }
                }
                //always register link in case only one lane is specified (-> no LaneEnter/Leave-Events?)
                this.sensorManager.registerNumberOfCarsInDistanceMonitoring(signal.getLinkId(), 0.);
                this.sensorManager.registerAverageNumberOfCarsPerSecondMonitoring(signal.getLinkId());
            }
        }
        if (laemmerConfig.isCheckDownstream()){
			downstreamSensor.registerDownstreamSensors(system);
        }
    }

    @Override
    public String getStatFields() {

        StringBuilder builder = new StringBuilder();
        builder.append("T_idle;selected;total delay;");
        for (LaemmerSignal laemmerSignal : laemmerSignals) {
            laemmerSignal.getStatFields(builder);
        }
        return builder.toString();
    }

    @Override
    public String getStepStats(double now) {

        StringBuilder builder = new StringBuilder();
        String selected = "none";
        if (activeRequest != null) {
            selected = activeRequest.signal.group.getId().toString();
        }
        builder.append(tIdle + ";" + selected + ";" + delayCalculator.getTotalDelay() + ";");
        for (LaemmerSignal laemmerSignal : laemmerSignals) {
            laemmerSignal.getStepStats(builder, now);
        }
        return builder.toString();
    }

    class Request {
        private final double time;
        private final LaemmerSignal signal;

        Request(double time, LaemmerSignal laemmerSignal) {
            this.signal = laemmerSignal;
            this.time = time;
        }

        private boolean isDue(double timeSeconds) {
            return timeSeconds == this.time;
        }
    }

    class LaemmerSignal {

        SignalGroup group;

        double index = 0;
        private double abortionPenalty = 0;
        private boolean stabilize = false;

        private double a = DEFAULT_INTERGREEN;
        private double regulationTime = 0;

        private Id<Lane> determiningLane;
        private Id<Link> determiningLink;
        private double determiningArrivalRate;
        private double determiningLoad;
        private double outflowSum;

        LaemmerSignal(SignalGroup signalGroup) {
            this.group = signalGroup;
        }

        private void determineRepresentativeDriveway(double now) {
            this.determiningLoad = 0;
            this.determiningLink = null;
            this.determiningLane = null;
            this.outflowSum = 0;
            for (Signal signal : group.getSignals().values()) {
                if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty()) {
                    for (Id<Lane> laneId : signal.getLaneIds()) {
                        double arrivalRate = getAverageLaneArrivalRate(now, signal.getLinkId(), laneId);
                        double outflow = lanes.getLanesToLinkAssignments().get(signal.getLinkId()).getLanes().get(laneId).getCapacityVehiclesPerHour() / 3600;
                        outflowSum += outflow;
                        double tempLoad = arrivalRate / outflow;
                        if (tempLoad >= this.determiningLoad) {
                            this.determiningLoad = tempLoad;
                            this.determiningArrivalRate = arrivalRate;
                            this.determiningLane = laneId;
                            this.determiningLink = signal.getLinkId();
                        }
                    }
                } else {
                    sensorManager.registerAverageNumberOfCarsPerSecondMonitoring(signal.getLinkId());
                    double outflow = network.getLinks().get(signal.getLinkId()).getCapacity() / 3600;
                    outflowSum += outflow;
                    double arrivalRate = getAverageArrivalRate(now, signal.getLinkId());
                    double tempLoad = arrivalRate / outflow;
                    if (tempLoad >= this.determiningLoad) {
                        this.determiningLoad = tempLoad;
                        this.determiningArrivalRate = arrivalRate;
                        this.determiningLane = null;
                        this.determiningLink = signal.getLinkId();
                    }
                }
            }
        }

        private void update(double now) {
            updateAbortionPenalty(now);

            if (!laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.OPTIMIZING)) {
                updateStabilization(now);
            }
            calculatePriorityIndex(now);
        }

        private void updateAbortionPenalty(double now) {
            this.abortionPenalty = 0;
            if (activeRequest != null && this.equals(activeRequest.signal)) {
                double waitingTimeSum = 0;
                double remainingInBetweenTime = Math.max(activeRequest.time - now, 0);
                for (double i = remainingInBetweenTime; i < DEFAULT_INTERGREEN; i++) {
                    for (Signal signal : group.getSignals().values()) {
                        if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty()) {
                            for (Id<Lane> laneId : signal.getLaneIds()) {
                                waitingTimeSum += getNumberOfExpectedVehiclesOnLane(now + i, signal.getLinkId(), laneId);
                            }
                        } else {
                            waitingTimeSum += getNumberOfExpectedVehiclesOnLink(now + i, signal.getLinkId());
                        }
                    }
                }
                double n = 0;
                for (Signal signal : group.getSignals().values()) {
                    if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty()) {
                        for (Id<Lane> laneId : signal.getLaneIds()) {
                            n += getNumberOfExpectedVehiclesOnLane(now + DEFAULT_INTERGREEN, signal.getLinkId(), laneId);
                        }
                    } else {
                        n += getNumberOfExpectedVehiclesOnLink(now + DEFAULT_INTERGREEN, signal.getLinkId());
                    }
                }
                if (n > 0) {
                    this.abortionPenalty += waitingTimeSum / n;
                }
            }
        }

        private void calculatePriorityIndex(double now) {
            this.index = 0;
            if (activeRequest != null && activeRequest.signal == this) {
                double remainingInBetweenTime = Math.max(activeRequest.time - now, 0);
                double remainingMinG = Math.max(activeRequest.time - now + MIN_G - remainingInBetweenTime, 0);
                for (double i = remainingInBetweenTime; i <= DEFAULT_INTERGREEN; i++) {
                    double nExpected = 0;
                    double reqGreenTime = remainingMinG;
                    for (Signal signal : this.group.getSignals().values()) {
                        if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty()) {
                            for (Id<Lane> laneId : signal.getLaneIds()) {
                                double nTemp = getNumberOfExpectedVehiclesOnLane(now + i + remainingMinG, signal.getLinkId(), laneId);
                                nExpected += nTemp;
                                double laneFlow = lanes.getLanesToLinkAssignments().get(signal.getLinkId()).getLanes().get(laneId).getCapacityVehiclesPerHour() / 3600;
                                double tempGreenTime = nTemp / laneFlow;
                                if (tempGreenTime > reqGreenTime) {
                                    reqGreenTime = tempGreenTime;
                                }
                            }
                        } else {
                            double nTemp = getNumberOfExpectedVehiclesOnLink(now + i + remainingMinG, signal.getLinkId());
                            nExpected += nTemp;
                            double linkFlow = network.getLinks().get(signal.getLinkId()).getCapacity() / 3600;
                            double tempGreenTime = nTemp / linkFlow;
                            if (tempGreenTime > reqGreenTime) {
                                reqGreenTime = tempGreenTime;
                            }
                        }
                        double tempIndex = 0;
                        if (nExpected > 0) {
                            tempIndex = nExpected / (i + reqGreenTime);
                        }
                        if (tempIndex > index) {
                            index = tempIndex;
                        }
                    }
                }
            } else {
                double nExpected = 0;
                double reqGreenTime = MIN_G;
                for (Signal signal : this.group.getSignals().values()) {
                    if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty()) {
                        for (Id<Lane> laneId : signal.getLaneIds()) {
                            double nTemp = getNumberOfExpectedVehiclesOnLane(now + DEFAULT_INTERGREEN + MIN_G, signal.getLinkId(), laneId);
                            nExpected += nTemp;
                            double laneFlow = lanes.getLanesToLinkAssignments().get(signal.getLinkId()).getLanes().get(laneId).getCapacityVehiclesPerHour() / 3600;
                            double tempGreenTime = nTemp / laneFlow;
                            if (tempGreenTime > reqGreenTime) {
                                reqGreenTime = tempGreenTime;
                            }
                        }
                    } else {
                        double nTemp = getNumberOfExpectedVehiclesOnLink(now + DEFAULT_INTERGREEN + MIN_G, signal.getLinkId());
                        nExpected += nTemp;
                        double linkFlow = network.getLinks().get(signal.getLinkId()).getCapacity() / 3600;
                        double tempGreenTime = nTemp / linkFlow;
                        if (tempGreenTime > reqGreenTime) {
                            reqGreenTime = tempGreenTime;
                        }
                    }
                }
                double penalty = 0;
                if (activeRequest != null) {
                    penalty = activeRequest.signal.abortionPenalty;
                }
                index = nExpected / (penalty + DEFAULT_INTERGREEN + reqGreenTime);
            }
        }

        private void updateStabilization(double now) {

            if (determiningArrivalRate == 0) {
                return;
            }

            double n = 0;
            if (determiningLane != null) {
                n = getNumberOfExpectedVehiclesOnLane(now, determiningLink, determiningLane);
            } else {
                n = getNumberOfExpectedVehiclesOnLink(now, determiningLink);
            }

            if (n == 0) {
                a = DEFAULT_INTERGREEN;
            } else {
                a++;
            }

            if (regulationQueue.contains(this)) {
                return;
            }

            this.regulationTime = 0;
            this.stabilize = false;
            double nCrit = determiningArrivalRate * desiredPeriod
                    * ((maxPeriod - (a / (1 - determiningLoad)))
                    / (maxPeriod - desiredPeriod));

            if (n >= nCrit) {
            	/* TODO actually, this is the wrong place to check downstream conditions, since situation can change until the group has moved up to the queue front. 
            	 * a better moment would be while polling from the queue: poll the first element with downstream empty. but we would need a linked list instead of queue for this
            	 * and could no longer check for empty regulationQueue to decide for stabilization vs optimization... I would prefer to have some tests before! theresa, jul'17 */
				if (!laemmerConfig.isCheckDownstream() || downstreamSensor.allDownstreamLinksEmpty(system.getId(), group.getId())) {
					regulationQueue.add(this);
					// signalLog.debug("Regulation time parameters: lambda: " + determiningLoad + " | T: " + desiredPeriod + " | qmax: " + determiningOutflow + " | qsum: " + flowSum + " | T_idle:" +
					// tIdle);
					this.regulationTime = Math.max(Math.rint(determiningLoad * desiredPeriod + (outflowSum / flowSum) * Math.max(tIdle, 0)), MIN_G);
					this.stabilize = true;
				}
            }
        }

        public void getStatFields(StringBuilder builder) {
            builder.append("state_" + this.group.getId() +";");
            builder.append("index_" + this.group.getId() + ";");
            builder.append("load_" + this.group.getId() + ";");
            builder.append("a_" + this.group.getId() + ";");
            builder.append("abortionPen_" + this.group.getId() + ";");
            builder.append("regTime_" + this.group.getId() + ";");
            builder.append("nTotal_" + this.group.getId() + ";");
        }

        public void getStepStats(StringBuilder builder, double now) {
            int totalN = 0;
            for (Signal signal : group.getSignals().values()) {
                if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty()) {
                    for (Id<Lane> laneId : signal.getLaneIds()) {
                        totalN += getNumberOfExpectedVehiclesOnLane(now, signal.getLinkId(), laneId);
                    }
                } else {
                    totalN += getNumberOfExpectedVehiclesOnLink(now, signal.getLinkId());
                }
            }
            builder.append(this.group.getState().name()+ ";")
                    .append(this.index + ";")
                    .append(this.determiningLoad + ";")
                    .append(this.a + ";")
                    .append(this.abortionPenalty + ";")
                    .append(this.regulationTime + ";")
                    .append(totalN + ";");
        }
    }
}