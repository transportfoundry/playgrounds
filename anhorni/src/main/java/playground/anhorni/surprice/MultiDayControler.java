/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.anhorni.surprice;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

public class MultiDayControler {
	
	private final static Logger log = Logger.getLogger(MultiDayControler.class);
			
	public static void main (final String[] args) {		
		if (args.length != 1) {
			log.error("Provide correct number of arguments ...");
			System.exit(-1);
		}		
		String configFile = args[0];
		Config config = ConfigUtils.loadConfig(configFile);
		String path = config.plans().getInputFile();
		String outPath = config.controler().getOutputDirectory();
		
		AgentMemories memories = new AgentMemories();
				
		for (String day : Surprice.days) {			
			config.setParam("controler", "outputDirectory", outPath + "/" + day);
			config.setParam("plans", "inputPlansFile", path + "/" + day + "/plans.xml");
			config.setParam("controler", "runId", day);
			
		    ObjectAttributes votFactors = new ObjectAttributes();
		    
		    ObjectAttributesXmlReader attributesReader = new ObjectAttributesXmlReader(votFactors);
			attributesReader.parse(path + "incomes.xml");
			
			DayControler controler = new DayControler(config, memories, day, votFactors);
			controler.run();
		}		
		UtilityAnalyzer analyzer = new UtilityAnalyzer();
		Config configCreate = ConfigUtils.loadConfig("C:/l/studies/surprice/configCreate.xml");
		double sideLength = Double.parseDouble(configCreate.findParam(Surprice.SURPRICE_PREPROCESS, "sideLength"));
		analyzer.analyze(config, outPath, sideLength);
		
		log.info("Week simulated, yep, .................................................................");
    }
}
