package playground.sebhoerl.avtaxi.passenger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import playground.sebhoerl.avtaxi.data.AVOperator;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.routing.AVRoute;
import playground.sebhoerl.avtaxi.schedule.AVDropoffTask;
import playground.sebhoerl.avtaxi.schedule.AVPickupTask;

public class AVRequest implements PassengerRequest {
	private final Id<Request> id;
	private final double submissionTime;
	private final double earliestStartTime;

	final private Link pickupLink;
    final private Link dropoffLink;
    final private MobsimPassengerAgent passengerAgent;
    final private AVOperator operator;
    final private AVDispatcher dispatcher;
    final private AVRoute route;

    private AVPickupTask pickupTask;
    private AVDropoffTask dropoffTask;
    

    public AVRequest(Id<Request> id, MobsimPassengerAgent passengerAgent, Link pickupLink, Link dropoffLink, double pickupTime, double submissionTime, AVRoute route, AVOperator operator, AVDispatcher dispatcher) {
		this.id = id;
		this.submissionTime = submissionTime;
		this.earliestStartTime = pickupTime;
        this.passengerAgent = passengerAgent;
        this.pickupLink = pickupLink;
        this.dropoffLink = dropoffLink;
        this.operator = operator;
        this.route = route;
        this.dispatcher = dispatcher;
    }

	@Override
	public Id<Request> getId() {
		return id;
	}

	@Override
	public double getSubmissionTime() {
		return submissionTime;
	}

	@Override
	public double getEarliestStartTime() {
		return earliestStartTime;
	}

    @Override
    public Link getFromLink() {
        return pickupLink;
    }

    @Override
    public Link getToLink() {
        return dropoffLink;
    }

    @Override
    public MobsimPassengerAgent getPassenger() {
        return passengerAgent;
    }

    public AVPickupTask getPickupTask() {
        return pickupTask;
    }

    public void setPickupTask(AVPickupTask pickupTask) {
        this.pickupTask = pickupTask;
    }

    public AVDropoffTask getDropoffTask() {
        return dropoffTask;
    }

    public void setDropoffTask(AVDropoffTask dropoffTask) {
        this.dropoffTask = dropoffTask;
    }

    public AVOperator getOperator() {
        return operator;
    }

    public AVDispatcher getDispatcher() {
        return dispatcher;
    }

    public AVRoute getRoute() {
        return route;
    }
}
