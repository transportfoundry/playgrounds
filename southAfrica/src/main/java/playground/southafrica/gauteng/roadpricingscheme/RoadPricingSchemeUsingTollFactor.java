/* *********************************************************************** *
 * project: kai
 * GautengRoadPricingScheme.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.southafrica.gauteng.roadpricingscheme;

import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.roadpricing.RoadPricingSchemeImpl.Cost;

/**
 * @author nagel
 *
 */
public class RoadPricingSchemeUsingTollFactor implements RoadPricingScheme {
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger( RoadPricingSchemeUsingTollFactor.class ) ;

	private RoadPricingScheme delegate = null ;
	private final TollFactorI tollFactor ;
	
	public RoadPricingSchemeUsingTollFactor( String tollLinksFileName, TollFactorI tollFactor ) {
//		log.warn("for me, using this as cordon toll did not work; using it as new scheme `link' " +
//				"toll for the time being.  needs to be debugged?!?!  kai, mar'12") ;
		// it is, in reality, a link toll, not a cordon toll.  Marcel had implemented it as cordon toll since link toll at that point did not exist.  kai, jan'14
		
		// read the road pricing scheme from file
		RoadPricingSchemeImpl scheme = new RoadPricingSchemeImpl();
		RoadPricingReaderXMLv1 rpReader = new RoadPricingReaderXMLv1(scheme);
		try {
			rpReader.parse( tollLinksFileName  );
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		this.delegate = scheme ;
		this.tollFactor = tollFactor ;
				
	}

	@Override
	public String getDescription() {
		return delegate.getDescription();
	}

	@Override
	public Cost getLinkCostInfo(Id linkId, double time, Id personId, Id vehicleId) {
		Cost baseToll = delegate.getLinkCostInfo(linkId, time, personId, vehicleId );
		if (baseToll == null) {
			return null ;
		}
		final double tollFactorVal = tollFactor.getTollFactor(personId, vehicleId, linkId, time);
		return new Cost( baseToll.startTime, baseToll.endTime, baseToll.amount * tollFactorVal );
	}
	
	@Override
	public Cost getTypicalLinkCostInfo( Id linkId, double time ) {
		return this.getLinkCostInfo( linkId, time, null, null ) ;
	}

	@Override
	public Set<Id> getTolledLinkIds() {
		return delegate.getTolledLinkIds();
	}

	@Override
	public String getName() {
		return delegate.getName();
	}

	@Override
	public String getType() {
		return delegate.getType();
	}

}