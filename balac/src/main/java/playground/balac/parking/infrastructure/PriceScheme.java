package playground.balac.parking.infrastructure;

import org.matsim.api.core.v01.Identifiable;

public interface PriceScheme extends Identifiable {

	public abstract String getType();
	
}
