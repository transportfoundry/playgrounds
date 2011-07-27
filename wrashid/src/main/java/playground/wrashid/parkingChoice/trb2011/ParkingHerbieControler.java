package playground.wrashid.parkingChoice.trb2011;

import herbie.running.controler.HerbieControler;

import java.util.LinkedList;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.meisterk.kti.controler.KTIControler;
import playground.wrashid.lib.DebugLib;
import playground.wrashid.parkingChoice.ParkingModule;
import playground.wrashid.parkingChoice.apiDefImpl.ParkingScoringFunctionZhScenario_v1;
import playground.wrashid.parkingChoice.apiDefImpl.PriceAndDistanceParkingSelectionManager;
import playground.wrashid.parkingChoice.apiDefImpl.ShortestWalkingDistanceParkingSelectionManager;
import playground.wrashid.parkingChoice.infrastructure.FlatParkingFormatReaderV1;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;
import playground.wrashid.parkingChoice.scoring.ParkingScoreAccumulator;

public class ParkingHerbieControler {

	static String parkingDataBase="H:/data/experiments/TRBAug2011/parkings/flat/";
	static ParkingModule parkingModule;
	public static boolean isRunningOnServer=false;
	public static boolean isKTIMode=true;
	
	public static void main(String[] args) {
		Controler controler=null;
		if (isKTIMode){
			controler=new KTIControler(args);
		} else {
			controler=new HerbieControler(args);
		}
		
		
		parkingModule=new ParkingModule(controler, null);
		
		prepareParkingsForScenario(controler);
		
		controler.setOverwriteFiles(true);

		controler.run();
		
	}
	
	



	private static void prepareParkingsForScenario(Controler controler) {
		controler.addControlerListener(new StartupListener() {
			
			@Override
			public void notifyStartup(StartupEvent event) {
				String isRunningOnServer = event.getControler().getConfig().findParam("parking", "isRunningOnServer");
				if (Boolean.parseBoolean(isRunningOnServer)){
					parkingDataBase="/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/wrashid/data/experiments/TRBAug2011/parkings/flat/";
					ParkingHerbieControler.isRunningOnServer=true;
				} else {
					parkingDataBase="H:/data/experiments/TRBAug2011/parkings/flat/";
					ParkingHerbieControler.isRunningOnServer=false;
				}
				
				LinkedList<Parking> parkingCollection = getParkingsForScenario(event.getControler());
				parkingModule.getParkingManager().setParkingCollection(parkingCollection);
				
				ParkingScoreAccumulator.initializeParkingCounts(event.getControler());
				
				initParkingSelectionManager(event.getControler());
			}

			private void initParkingSelectionManager(Controler controler) {
				String parkingSelectionManager = controler.getConfig().findParam("parking", "parkingSelectionManager");
				if (parkingSelectionManager.equalsIgnoreCase("shortestWalkingDistance")){
					parkingModule.setParkingSelectionManager(new ShortestWalkingDistanceParkingSelectionManager(parkingModule.getParkingManager()) );
				} else if (parkingSelectionManager.equalsIgnoreCase("PriceAndDistance_v1")) {
					parkingModule.setParkingSelectionManager(new PriceAndDistanceParkingSelectionManager(parkingModule.getParkingManager(), new ParkingScoringFunctionZhScenario_v1()));				
					ParkingScoringFunctionZhScenario_v1.disutilityOfWalkingPerMeter=Double.parseDouble(controler.getConfig().findParam("parking", "disutilityOfWalkingPerMeter"));
					ParkingScoringFunctionZhScenario_v1.disutilityOfWalkingPerMeterForMoreThan300Meters=Double.parseDouble(controler.getConfig().findParam("parking", "disutilityOfWalkingPerMeterForMoreThan300Meters"));
					ParkingScoringFunctionZhScenario_v1.streetParkingPricePerSecond=Double.parseDouble(controler.getConfig().findParam("parking", "streetParkingPricePerSecond"));
					ParkingScoringFunctionZhScenario_v1.garageParkingPricePerSecond=Double.parseDouble(controler.getConfig().findParam("parking", "garageParkingPricePerSecond"));
				} else {
					DebugLib.stopSystemAndReportInconsistency("unknown parkingSelectionManager:" + parkingSelectionManager);
				}
				
			}
		});
		
	}
	
	



	public static LinkedList<Parking> getParkingsForScenario(Controler controler) {
		double parkingsOutsideZHCityScaling=Double.parseDouble(controler.getConfig().findParam("parking", "publicParkingsCalibrationFactorOutsideZHCity"));
		
		LinkedList<Parking> parkingCollection=getParkingCollectionZHCity(controler);
		String streetParkingsFile=null;
		if (isKTIMode){
			streetParkingsFile=parkingDataBase + "publicParkingsOutsideZHCity_v0_kti.xml";
		} else {
			streetParkingsFile=parkingDataBase + "publicParkingsOutsideZHCity_v0.xml";
		}
		
		readParkings(parkingsOutsideZHCityScaling, streetParkingsFile,parkingCollection);
		
		return parkingCollection;
	}

	public static LinkedList<Parking> getParkingCollectionZHCity(Controler controler) {
		double streetParkingCalibrationFactor=Double.parseDouble(controler.getConfig().findParam("parking", "streetParkingCalibrationFactorZHCity"));
		double garageParkingCalibrationFactor=Double.parseDouble(controler.getConfig().findParam("parking", "garageParkingCalibrationFactorZHCity"));
		double privateParkingCalibrationFactorZHCity=Double.parseDouble(controler.getConfig().findParam("parking", "privateParkingCalibrationFactorZHCity"));
		//double privateParkingsOutdoorCalibrationFactor=Double.parseDouble(controler.getConfig().findParam("parking", "privateParkingsOutdoorCalibrationFactorZHCity"));
		
		LinkedList<Parking> parkingCollection=new LinkedList<Parking>();
		
		String streetParkingsFile=parkingDataBase + "streetParkings.xml";
		readParkings(streetParkingCalibrationFactor, streetParkingsFile,parkingCollection);
		
		String garageParkingsFile=parkingDataBase + "garageParkings.xml";
		readParkings(garageParkingCalibrationFactor, garageParkingsFile,parkingCollection);
		
		String privateIndoorParkingsFile=null;
		if (isKTIMode){
			privateIndoorParkingsFile=parkingDataBase + "privateParkings_v1_kti.xml";
		} else {
			privateIndoorParkingsFile=parkingDataBase + "privateParkings_v1.xml";
		}
		
		readParkings(privateParkingCalibrationFactorZHCity, privateIndoorParkingsFile,parkingCollection);
		
		//String privateOutdoorParkingsFile=parkingDataBase + "privateParkingsOutdoor.xml";
		//readParkings(privateParkingsOutdoorCalibrationFactor, privateOutdoorParkingsFile,parkingCollection);
		
		return parkingCollection;
	}
	
	public static LinkedList<Parking> getParkingCollectionZHCity(){
		LinkedList<Parking> parkingCollection=new LinkedList<Parking>();
		
		String streetParkingsFile=parkingDataBase + "streetParkings.xml";
		readParkings(1.0, streetParkingsFile,parkingCollection);
		
		String garageParkingsFile=parkingDataBase + "garageParkings.xml";
		readParkings(1.0, garageParkingsFile,parkingCollection);
		
		String privateIndoorParkingsFile=parkingDataBase + "privateParkingsIndoor_v0.xml";
		readParkings(1.0, privateIndoorParkingsFile,parkingCollection);
		
		String privateOutdoorParkingsFile=parkingDataBase + "privateParkingsOutdoor_v0.xml";
		readParkings(1.0, privateOutdoorParkingsFile,parkingCollection);
		
		return parkingCollection;
	}

	public static void readParkings(double parkingCalibrationFactor, String parkingsFile, LinkedList<Parking> parkingCollection) {
		FlatParkingFormatReaderV1 flatParkingFormatReaderV1 = new FlatParkingFormatReaderV1();
		flatParkingFormatReaderV1.parse(parkingsFile);
		
		LinkedList<Parking> parkings= flatParkingFormatReaderV1.getParkings();
		calibarteParkings(parkings,parkingCalibrationFactor);
		
		parkingCollection.addAll(parkings);
	}
	
	private static void calibarteParkings(LinkedList<Parking> parkingCollection, double calibrationFactor){
		for (Parking parking:parkingCollection){
			double capacity = parking.getCapacity();
			parking.setCapacity(capacity*calibrationFactor);
		}
	}
	
	public static Coord getCoordinatesQuaiBridgeZH(){
		return new CoordImpl(683423.0,246819.0);
	}
	
	public static Coord getCoordinatesLindenhofZH(){
		return new CoordImpl(683235.0,247497.0);
	}
	
	
}
