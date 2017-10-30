package playground.sergioo.mixedTraffic2017.cityphi;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

public class Roads {

	/**
	 * 0 - Network file
	 * 1 - Roads shape file
	 * 2 - Roads props file
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(args[0]);
		PrintWriter writer = new PrintWriter(args[1]);
		writer.println("id,seq,x,y,z");
		for(Link link:network.getLinks().values()) {
			Coord coord = link.getFromNode().getCoord();
			writer.println(link.getId()+",0,"+coord.getX()+","+coord.getY()+",0");
			coord = link.getToNode().getCoord();
			writer.println(link.getId()+",1,"+coord.getX()+","+coord.getY()+",0");
		}
		writer.close();
		writer = new PrintWriter(args[2]);
		writer.println("id,speed,capacity,numlanes,modes");
		for(Link link:network.getLinks().values()) {
			String modes = "";
			for(String mode:link.getAllowedModes())
				modes+="["+mode+"]";
			writer.println(link.getId()+","+link.getFreespeed()+","+link.getCapacity()+","+link.getNumberOfLanes()+","+modes);
		}
		writer.close();
	}

}
