/* *********************************************************************** *
 * project: org.matsim.*
 * CANetwork.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.gregor.casim.simulation.physics;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;

import playground.gregor.casim.monitoring.CALinkMonitorExact;
import playground.gregor.casim.simulation.CANetsimEngine;
import playground.gregor.casim.simulation.physics.CAEvent.CAEventType;
import playground.gregor.sim2d_v4.events.XYVxVyEventImpl;

/**
 * Centerpiece of the bidirectional 1d ca simulation. Basic idea is based on
 * Flötteröd and Lämmel (forthcoming); Bidirectional pedestrian fundamental
 * diagram. Transportation Research Part B
 * 
 * @author laemmel
 *
 */
abstract public class AbstractCANetwork implements CANetwork {

	public static double RHO = 1;

	// Floetteroed Laemmel parameters
	public static final double RHO_HAT = 6.69;
	public static final double V_HAT = 1.27;

	public static final double ALPHA = 0.;
	public static final double BETA = 0.39;
	public static final double GAMMA = 1.43;

	public static final double PED_WIDTH = .61;

	private static final Logger log = Logger.getLogger(AbstractCANetwork.class);

	public static boolean EMIT_VIS_EVENTS = false;

	// private final PriorityQueue<CAEvent> events = new PriorityQueue<CAEvent>(
	// 1000000);

	private final CAEventsPaulPriorityQueue events = new CAEventsPaulPriorityQueue();
	protected final Network net;

	protected final Map<Id<Node>, CANode> caNodes = new HashMap<Id<Node>, CANode>();
	protected final Map<Id<Link>, CALink> caLinks = new HashMap<Id<Link>, CALink>();
	private final EventsManager em;

	private List<CALinkMonitorExact> monitors = new ArrayList<>();

	private Set<CAMoveableEntity> agents = new HashSet<CAMoveableEntity>();

	// private CANetsimEngine engine;

	private static int EXP_WARN_CNT;

	public static int NR_THREADS = 4;
	private final CyclicBarrier barrier1 = new CyclicBarrier(NR_THREADS + 1);
	private final CyclicBarrier barrier2 = new CyclicBarrier(NR_THREADS + 1);
	private final Worker[] workers = new Worker[NR_THREADS];
	private final Map<String, Worker> workerMap = new HashMap<>();

	private double eventChunkSizeCoefficient = 1;
	private final int desiredChunkSizeAvgPerThread = 400;

	private final CASimDensityEstimator dens;

	protected double tFreeMin;

	private final CANetsimEngine engine;

	public AbstractCANetwork(Network net, EventsManager em,
			CANetsimEngine engine) {
		this.net = net;
		this.em = em;
		this.engine = engine;
		init();
		this.dens = new CASimDensityEstimator(this);
	}

	private void init() {
		for (int i = 0; i < NR_THREADS; i++) {
			Worker w = new Worker(barrier1, barrier2);
			workers[i] = w;
			Thread t = new Thread(w);
			t.setDaemon(true);
			t.setName(Worker.class.toString() + i);
			t.start();
			this.workerMap.put(t.getName(), w);
		}

	}

	@Override
	public synchronized void registerAgent(CAMoveableEntity a) {
		if (!this.agents.add(a)) {
			throw new RuntimeException("Agent: " + a
					+ " has already been registered!");
		}
	}

	/* package */void unregisterAgent(CAMoveableEntity a) {
		if (!this.agents.remove(a)) {
			throw new RuntimeException("Could not unregister agent: " + a + "!"
					+ " has already been removed:");
		}
	}

	/* package */void updateRho() {

		for (CAMoveableEntity a : this.agents) {
			this.dens.handle(a);
		}
		this.dens.await();
	}

	@Override
	public void doSimStep(double time) {
		updateRho();
		// draw2();
		while (this.events.peek() != null
				&& this.events.peek().getEventExcexutionTime() < time + 1) {

			double timeFrameEnd = Math.min(events.peek()
					.getEventExcexutionTime()
					+ this.tFreeMin
					/ this.eventChunkSizeCoefficient, time + 1);
			for (int i = 0; i < NR_THREADS; i++) {
				this.workers[i].setEndOfFrame(timeFrameEnd);
			}
			int cnt = 0;
			while (this.events.peek() != null
					&& this.events.peek().getEventExcexutionTime() <= timeFrameEnd) {
				CAEvent e = this.events.poll();
				cnt++;
				// log.info("==> " + e);
				int thread = e.getCANetworkEntity().threadNR();
				// log.info("added to thread: " + thread);
				this.workers[thread].add(e);
			}
			int avgPerThread = cnt / NR_THREADS;
			if (avgPerThread > desiredChunkSizeAvgPerThread + 100) {
				this.eventChunkSizeCoefficient /= .9;
				// log.info("everage events per thread: " + avgPerThread
				// + " events chunk size  coefficient set to: "
				// + this.eventChunkSizeCoefficient);
			} else if (avgPerThread < desiredChunkSizeAvgPerThread - 50
					&& this.eventChunkSizeCoefficient > 1) {
				this.eventChunkSizeCoefficient *= .9;
				// log.info("everage events per thread: " + avgPerThread
				// + " events chunk size  coefficient set to: "
				// + this.eventChunkSizeCoefficient);

			}
			// log.info(cnt);
			for (int i = 0; i < NR_THREADS; i++) {
				this.workers[i].add(new CAEvent(timeFrameEnd, null, null,
						CAEventType.END_OF_FRAME));
			}
			try {
				this.barrier1.await();
				for (int i = 0; i < NR_THREADS; i++) {
					Worker w = this.workers[i];
					while (w.getUnhandledGlobalEvents().size() > 1) {
						// the last one is the END_OF_FRAME event

						CAEvent ee = w.getUnhandledGlobalEvents().poll();
						this.events.add(ee);
					}
					w.getUnhandledGlobalEvents().poll();
					while (w.getUnhandledLocalEvents().peek() != null) {
						CAEvent ee = w.getUnhandledLocalEvents().poll();
						if (!ee.isObsolete()) {
							this.events.add(ee);
						}
					}
					while (w.getCachedEvents().peek() != null) {
						CAEvent ee = w.getCachedEvents().poll();
						if (!ee.isObsolete()) {
							this.events.add(ee);
						}
					}
				}
				this.barrier2.await();
			} catch (InterruptedException | BrokenBarrierException e) {
				throw new RuntimeException(e);
			}
		}
		if (EMIT_VIS_EVENTS) {
			draw2(time);
		}
	}

	@Override
	@Deprecated
	public void run() {
		// draw2();
		updateRho();
		double time = 0;
		while (this.events.peek() != null) {
			if (this.events.peek().getEventExcexutionTime() > time + 1) {
				updateRho();
				time = this.events.peek().getEventExcexutionTime();
			}

			CAEvent e = this.events.poll();
			// log.info("==> " + e);

			if (this.monitors.size() > 0) {
				for (CALinkMonitorExact monitor : this.monitors) {
					monitor.trigger(e.getEventExcexutionTime());
				}
			}

			if (e.isObsolete()) {
				if (EXP_WARN_CNT++ < 10) {
					log.info("dropping obsolete event: " + e);
					if (EXP_WARN_CNT == 10) {
						log.info(Gbl.FUTURE_SUPPRESSED);
					}
				}
				continue;
			}

			// this.workers[e.getCANetworkEntity().threadNR()].add(e);
			e.getCANetworkEntity().handleEvent(e);
		}
		if (EMIT_VIS_EVENTS) {
			// updateDensity();
			draw2(time);
		}

		afterSim();
	}

	private void draw2(double time) {
		for (CALink ll : this.caLinks.values()) {
			if (ll instanceof CASingleLaneLink) {
				drawCALinkDynamic((CASingleLaneLink) ll, time);
			} else if (ll instanceof CAMultiLaneLink) {
				drawCALinkParallelQueues((CAMultiLaneLink) ll, time);
			}
		}
		for (CANode n : this.caNodes.values()) {
			if (n instanceof CASingleLaneNode) {
				drawCANodeDynamic((CASingleLaneNode) n, time);
			} else if (n instanceof CAMultiLaneNode) {
				drawCANodeParallelQueues((CAMultiLaneNode) n, time);
			}

		}
	}

	private void drawCANodeParallelQueues(CAMultiLaneNode n, double time) {
		int lanes = n.getNRLanes();
		double laneWidth = n.getWidth() / lanes;

		double x = n.getNode().getCoord().getX();
		double y0 = n.getNode().getCoord().getY() - laneWidth * lanes / 2
				+ laneWidth / 2;

		for (int slot = 0; slot < lanes; slot++) {
			CAMoveableEntity agent = n.peekForAgentInSlot(slot);
			if (agent != null) {
				XYVxVyEventImpl e = new XYVxVyEventImpl(agent.getId(), x, y0,
						0, 0, time);
				this.em.processEvent(e);
			}
			y0 += laneWidth;
		}

	}

	private void drawCANodeDynamic(CASingleLaneNode n, double time) {
		if (n.peekForAgent() != null) {
			double x = n.getNode().getCoord().getX();
			double y = n.getNode().getCoord().getY();
			XYVxVyEventImpl e = new XYVxVyEventImpl(n.peekForAgent().getId(),
					x, y, 0, 0, time);
			this.em.processEvent(e);
		}

	}

	private void drawCALinkParallelQueues(CAMultiLaneLink l, double time) {
		double dx = l.getLink().getToNode().getCoord().getX()
				- l.getLink().getFromNode().getCoord().getX();
		double dy = l.getLink().getToNode().getCoord().getY()
				- l.getLink().getFromNode().getCoord().getY();
		double length = Math.sqrt(dx * dx + dy * dy);
		dx /= length;
		dy /= length;
		double ldx = dx;
		double ldy = dy;
		double incr = l.getLink().getLength() / l.getNumOfCells();
		dx *= incr;
		dy *= incr;
		double laneWidth = l.getLaneWidth();
		double hx = -ldy;
		double hy = ldx;
		hx *= laneWidth;
		hy *= laneWidth;
		int lanes = l.getNrLanes();
		double x0 = l.getLink().getFromNode().getCoord().getX() - hx * lanes
				/ 2 + hx / 2 + dx / 2;
		double y0 = l.getLink().getFromNode().getCoord().getY() - hy * lanes
				/ 2 + hy / 2 + dy / 2;
		for (int lane = 0; lane < l.getNrLanes(); lane++) {
			double x = x0 + lane * hx;
			double y = y0 + lane * hy;
			for (int i = 0; i < l.getNumOfCells(); i++) {
				if (l.getParticles(lane)[i] != null) {
					double ddx = 1;
					if (l.getParticles(lane)[i].getDir() == -1) {
						ddx = -1;
					}
					XYVxVyEventImpl e = new XYVxVyEventImpl(
							l.getParticles(lane)[i].getId(), x, y, ldx * ddx,
							ldy * ddx, time);
					this.em.processEvent(e);
				}
				x += dx;
				y += dy;
			}
		}
	}

	private void drawCALinkDynamic(CASingleLaneLink l, double time) {
		double dx = l.getLink().getToNode().getCoord().getX()
				- l.getLink().getFromNode().getCoord().getX();
		double dy = l.getLink().getToNode().getCoord().getY()
				- l.getLink().getFromNode().getCoord().getY();
		double length = Math.sqrt(dx * dx + dy * dy);
		dx /= length;
		dy /= length;
		double ldx = dx;
		double ldy = dy;

		double hy = dx;
		double hx = -dy;

		double hy0 = -hy * l.getLink().getCapacity() / 2;
		double hx0 = -hx * l.getLink().getCapacity() / 2;
		hx *= PED_WIDTH;
		hy *= PED_WIDTH;

		double lanes = l.getLink().getCapacity() / PED_WIDTH;

		double incr = l.getLink().getLength() / l.getNumOfCells();
		dx *= incr;
		dy *= incr;
		double width = l.getLink().getCapacity();
		double x = l.getLink().getFromNode().getCoord().getX();// +dx/2;
		double y = l.getLink().getFromNode().getCoord().getY();// +dy/2;
		for (int i = 0; i < l.getNumOfCells(); i++) {
			if (l.getParticles()[i] != null) {

				double lane = l.getParticles()[i].hashCode() % lanes;

				double ddx = 1;
				if (l.getParticles()[i].getDir() == -1) {
					ddx = -1;
				}
				;
				XYVxVyEventImpl e = new XYVxVyEventImpl(
						l.getParticles()[i].getId(), x + dx / 2 + hx0 + lane
								* hx, y + dy / 2 + hy0 + lane * hy, ldx * ddx,
						ldy * ddx, time);

				this.em.processEvent(e);
				// System.out.println(l.getParticles()[i]);
			} else {
				// RectEvent e = new RectEvent(time, x, y+width/2, dx,
				// width, false);
				// this.em.processEvent(e);
			}
			x += dx;
			y += dy;
		}

	}

	@Override
	public void pushEvent(CAEvent event) {

		// if (event.getEventExcexutionTime() < this.currentTimeFrameEnd) {
		// log.warn("Event excecution time is "
		// + (this.currentTimeFrameEnd - event
		// .getEventExcexutionTime())
		// + "s before end of time frame.");
		// log.warn("current event: " + event.getCAAgent().getCurrentEvent()
		// + "\n new event: " + event);
		// }
		// log.info("<== " + event);
		event.getCAAgent().setCurrentEvent(event);
		// this.events.add(event);
		Worker w = this.workerMap.get(Thread.currentThread().getName());
		if (w != null) {
			w.addLocalEvent(event);
		} else {
			this.events.add(event);
		}
	}

	public CAEvent pollEvent() {
		return this.events.poll();
	}

	public CAEvent peekEvent() {
		return this.events.peek();
	}

	@Override
	public CALink getCALink(Id<Link> nextLinkId) {
		return this.caLinks.get(nextLinkId);
	}

	public EventsManager getEventsManager() {
		return this.em;
	}

	@Override
	public void addMonitor(CALinkMonitorExact m) {
		this.monitors.add(m);
	}

	public Map<Id<Link>, CALink> getLinks() {
		return this.caLinks;
	}

	public Map<Id<Node>, CANode> getNodes() {
		return this.caNodes;
	}

	private static final class Worker implements Runnable {

		private static final Logger log = Logger.getLogger(Worker.class);

		private final LinkedBlockingQueue<CAEvent> queue = new LinkedBlockingQueue<>();
		private final Queue<CAEvent> local = new PriorityQueue<>();
		private final Queue<CAEvent> cache = new ArrayDeque<>();

		private CyclicBarrier barrier2;
		private CyclicBarrier barrier1;

		private int cnt = 0;
		private int handled = 0;

		private double endOfFrame = 0;

		private final Deque<CAEvent> locks = new ArrayDeque<>();

		public Worker(CyclicBarrier barrier1, CyclicBarrier barrier2) {
			this.barrier1 = barrier1;
			this.barrier2 = barrier2;
		}

		synchronized public void addLocalEvent(CAEvent event) {
			if (event.getEventExcexutionTime() <= this.endOfFrame) {
				this.local.add(event);
			} else {
				this.cache.add(event);
			}

		}

		@Override
		public void run() {

			try {
				while (true) {
					CAEvent event = queue.take();
					cnt++;
					boolean gotLock = true;
					while (local.peek() != null
							&& local.peek().getEventExcexutionTime() < event
									.getEventExcexutionTime()) {
						CAEvent levent = local.poll();
						cnt++;
						int state = levent.tryLock();
						if (state != -1) {
							// this.locks.push(levent);
							if (!levent.isObsolete()) {
								handled++;
								levent.getCANetworkEntity().handleEvent(levent);
								levent.setObsolete();
							}
							if (state == 1) {
								levent.unlock();
							}
						} else {
							local.add(levent);
							gotLock = false;
							break;
						}
					}
					if (!gotLock) {
						if (event.getCAEventType() != CAEventType.END_OF_FRAME) {
							local.add(event);
						}
						endFrame();
						continue;
						// log.info("local lock failed");

					}

					if (event.getCAEventType() == CAEventType.END_OF_FRAME) {
						// log.info("end of frame");
						endFrame();
						continue;
					} else if (event.getCAEventType() == CAEventType.END_OF_SIM) {
						break;
					}

					int state = event.tryLock();
					if (state != -1) {
						// locks.push(event);
						if (!event.isObsolete()) {
							handled++;
							event.getCANetworkEntity().handleEvent(event);
							event.setObsolete();
						}
						if (state == 1) {
							event.unlock();
						}
					} else {
						local.add(event);
						// log.info("global event lock failed");
						endFrame();
					}

				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			Gbl.printCurrentThreadCpuTime();
		}

		private void endFrame() {
			// log.info("events polled: " + cnt + " events handled:" + handled
			// + "\t\t:" + Thread.currentThread().getName());
			cnt = 0;
			handled = 0;
			waitBarrier1();
			unlockAll();
			waitBarrier2();
		}

		private void unlockAll() {
			while (locks.size() > 0) {
				locks.pop().unlock();
			}
		}

		private void waitBarrier1() {
			try {
				this.barrier1.await();
			} catch (InterruptedException | BrokenBarrierException e) {
				throw new RuntimeException(e);
			}
		}

		private void waitBarrier2() {
			try {
				this.barrier2.await();
			} catch (InterruptedException | BrokenBarrierException e) {
				throw new RuntimeException(e);
			}
		}

		public void add(CAEvent e) {
			this.queue.add(e);
		}

		public Queue<CAEvent> getUnhandledGlobalEvents() {
			return this.queue;
		}

		public Queue<CAEvent> getUnhandledLocalEvents() {
			return this.local;
		}

		public Queue<CAEvent> getCachedEvents() {
			return this.cache;
		}

		public void setEndOfFrame(double time) {
			this.endOfFrame = time;
		}
	}

	@Override
	public void afterSim() {
		for (CALink caLink : this.getLinks().values()) {
			caLink.reset();
		}
		this.dens.shutdown();
		for (int i = 0; i < NR_THREADS; i++) {
			this.workers[i].add(new CAEvent(Double.NaN, null, null,
					CAEventType.END_OF_SIM));
		}

	}

	@Override
	public List<CALinkMonitorExact> getMonitors() {
		return monitors;
	}

	public Network getNetwork() {
		return this.net;
	}

	public CANetsimEngine getEngine() {
		return this.engine;
	}

}