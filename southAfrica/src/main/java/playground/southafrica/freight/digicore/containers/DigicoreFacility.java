package playground.southafrica.freight.digicore.containers;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Customizable;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.scenario.CustomizableUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.utils.objectattributes.attributable.Attributes;

public class DigicoreFacility implements ActivityFacility {
		
	private Id<ActivityFacility> id;
	private Id<Link> linkId;
	private Coord coord;
	private Attributes attributes = new Attributes();
	private Map<String, ActivityOption> options = new TreeMap<String, ActivityOption>();
	private Customizable customizableDelegate;
	
	public DigicoreFacility(Id<ActivityFacility> facilityId) {
		this.id = facilityId;
	}

	public Id<ActivityFacility> getId() {
		return this.id;
	}

	public Coord getCoord() {
		return this.coord;
	}

	public void setCoord(Coord coord) {
		this.coord = coord;
	}
	
	@Override
	public Id<Link> getLinkId() {
		return this.linkId;
	}

	@Override
	public Map<String, Object> getCustomAttributes() {
		if (this.customizableDelegate == null) {
			this.customizableDelegate = CustomizableUtils.createCustomizable();
		}
		return this.customizableDelegate.getCustomAttributes();
	}

	@Override
	public Map<String, ActivityOption> getActivityOptions() {
		return this.options;
	}

	@Override
	public void addActivityOption(ActivityOption option) {
		this.options.put(option.getType(), option);
	}

	@Override
	public Attributes getAttributes() {
		return this.attributes;
	}

}
