package playground.dziemke.teach.mz.tasks2012;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.vis.otfvis.OTFFileWriter;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfoFactory;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo.AgentState;

public class OwnMobsim {

	private static class MyMobsim implements Mobsim {

		private EventsManager eventsManager;
		private Scenario scenario;
		private AgentSnapshotInfoFactory agentSnapshotInfoFactory = new AgentSnapshotInfoFactory(null);

		public MyMobsim(Scenario sc, EventsManager eventsManager) {
			this.scenario = sc;
			this.eventsManager = eventsManager;
		}

		@Override
		public void run() {
			OTFFileWriter writer = new OTFFileWriter(scenario, "output/movie.mvi");
			Node start = scenario.getNetwork().getNodes().get(new IdImpl("1"));
			Node end = scenario.getNetwork().getNodes().get(new IdImpl("14"));
			for (double i=0; i<1000; i++) {
				writer.beginSnapshot(i);
				CoordImpl pos = new CoordImpl(start.getCoord().getX() + ( (end.getCoord().getX() - start.getCoord().getX()) / 1000.0) * i,  
						start.getCoord().getY() + ( (end.getCoord().getY() - start.getCoord().getY()) / 1000.0)* i);
				AgentSnapshotInfo agentSnapshotInfo = agentSnapshotInfoFactory.createAgentSnapshotInfo(new IdImpl("1"), pos.getX(), pos.getY(), 0.0, 0.0);
				System.out.println(pos);
				agentSnapshotInfo.setAgentState(AgentState.PERSON_DRIVING_CAR);
				writer.addAgent(agentSnapshotInfo);
				writer.endSnapshot();			
			}
			writer.finish();
		}

	}

	private static class MyMobsimFactory implements MobsimFactory {

		@Override
		public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
			return new MyMobsim(sc, eventsManager);
		}

	}

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("examples/equil/config.xml");
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(1);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		controler.setOverwriteFiles(true);
		MobsimFactory mobsimFactory = new MyMobsimFactory();
		controler.setMobsimFactory(mobsimFactory );
		controler.run();
		OTFVis.playMVI("output/movie.mvi");
	}

}
