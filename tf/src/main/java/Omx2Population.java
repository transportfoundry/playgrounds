import omx.OmxFile;
import omx.OmxMatrix;
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.io.File;
import java.util.*;
import java.util.stream.IntStream;

import static java.lang.Math.round;

public class Omx2Population {
    private final static Logger log = Logger.getLogger(Omx2Population.class);
    private Population population;
    private PopulationFactory pf;
    private TopoJson topoNetwork;
    private Map<Integer, Coord> centroids;
    private Double samplingRate;
    private CoordinateTransformation ct;
    private File omxFile;
    private File centroidsFile;
    private Random r;

    /**
     * The class constructor
     * @param scenario The MATSim scenario
     * @param omxFile An OMX file containing trip tables for non-modeled trips.
     * @param centroidsFile A TopoJson file containing a 'centroids' Geometry Collection with
     *                      lat-long coordinates representing the loading points of the zones in
     *                      the OMX File.
     */
    public Omx2Population(Scenario scenario, File omxFile, File centroidsFile,
                                    String crs, Double samplingRate) {
        this.r = new Random(12);
        this.omxFile = omxFile;
        this.centroidsFile = centroidsFile;
        this.population = scenario.getPopulation();
        this.pf = population.getFactory();
        this.ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, crs);
        this.samplingRate = samplingRate;
    }

    public void buildPopulation(){
        log.info("Getting centroid coordinates from " + centroidsFile);
        this.topoNetwork = TopoJson.reader(centroidsFile);
        this.centroids = topoNetwork.getCentroids("ID", this.ct);

        log.info("Getting matrices from " + omxFile);
        Map<String, OmxMatrix.OmxDoubleMatrix> tripTable = readTripTable(omxFile);

        log.info("Adding trip tables to scenario population");
        for(String table: tripTable.keySet()){
            makeDummyTrips(table, tripTable.get(table));
        }
    }


    /**
     * Make dummy trips between points to simulate an existing trip table.
     * @param tableName The name of the matrix core, including the period and the type of vehicle.
     * @param matrix A matrix with rows representing origin points and columns destinations.
     */
    private void makeDummyTrips(String tableName, OmxMatrix.OmxDoubleMatrix matrix){
        double[][] matrixData = matrix.getData();
        int[] matrixShape = matrix.getShape();
        Double missingValue = matrix.getMissingValue();
        Coord origin;
        Coord destination;
        int[] iArray = IntStream.range(0, matrixShape[1]).toArray();

        // loop over origin zones
        for(int i = 0; i < matrixShape[0]; i++){
            origin = centroids.get(i);

            // get all the trips originating in this zone
            double[] probabilities = matrixData[i];

            // if the cell is missing, it should have probability 0
            for(int j = 0; j < probabilities.length; j++){
                if(probabilities[j] == missingValue){
                    probabilities[j] = 0.;
                }
            }

            double sumProbabilities = Arrays.stream(probabilities).sum();
            int totalOrigins = (int) round(sumProbabilities * samplingRate);

            // load attractions at j into a distribution we can draw from
            if(totalOrigins > 0){
                EnumeratedIntegerDistribution ed = new EnumeratedIntegerDistribution(iArray, probabilities);

                log.debug("Making " + totalOrigins + " trips from zone " + i);
                for(int trip = 0; trip < totalOrigins; trip++){
                    destination = centroids.get(ed.sample());
                    makeDummyTripIJ(tableName, origin, destination);
                }
            }
        }

    }

    /**
     * Add a dummy trip between two points to a scenario. The agent ID is a function of the
     * matrix name, the departure time, and a unique hash.
     * @param matrixName The name of the matrix core. The period is passed along to 'getTripTime'
     * @param origin The origin coordinate
     * @param destination The destination coordinate
     */
    private void makeDummyTripIJ(String matrixName, Coord origin, Coord destination){

        Plan plan = pf.createPlan();
        Double tripTime = getTripTime(matrixName);

        Activity startActivity = pf.createActivityFromCoord("Home", origin);
        startActivity.setEndTime(tripTime);
        Leg leg = pf.createLeg(TransportMode.car);
        Activity endActivity = pf.createActivityFromCoord("Home", destination);
        endActivity.setEndTime(28 * 3600);

        plan.addActivity(startActivity);
        plan.addLeg(leg);
        plan.addActivity(endActivity);

        String uniqueId = UUID.randomUUID().toString();
        Id<Person> pId = Id.createPersonId(uniqueId);
        Person person = pf.createPerson(pId);
        //person.getAttributes().putAttribute("matrix", matrixName);
        population.addPerson(person);
        person.addPlan(plan);
    }

    /**
     * Get the trip time for a random trip
     * @param matrixName The name of the matrix core. If the matrix contains "am", then the
     *                   trip will happen between 7:00 and 9:59:
     *                     - 'am': 7:00 - 9:59
     *                     - 'md': 10:00 - 13:59
     *                     - 'pm': 16:00 - 18:59
     *                     - 'nt': 19:00 - 6:59
     * @return The number of seconds from midnight, which is the MATSim time indication.
     */
    private Double getTripTime(String matrixName){
        Integer hour = 12;
        Integer minute = randomInt(0, 59, r);
        Integer second = randomInt(0, 59, r);

        if(matrixName.contains("am")){
            hour = randomInt(7, 9, r);
        } else if(matrixName.contains("pm")){
            hour = randomInt(16, 18, r);
        } else if(matrixName.contains("md")){
            hour = randomInt(10, 15, r);
        } else if(matrixName.contains("nt")){
            //TODO check this math
            hour = randomInt(19, 30, r) % 24;
        }

        return hour * 3600.0 + minute * 60.0 + second;

    }

    /**
     * This is a simple random integer generator
     * @return A random integer on [min, max]
     */
    private static Integer randomInt() {
        Random random = new Random();
        return randomInt(0, 10, random);
    }

    /**
     * This is a simple random integer generator
     * @param min The minimum possible integer
     * @param max The maximum possible integer
     * @param r A Random
     * @return A random integer on [min, max]
     */
    private static Integer randomInt(Integer min, Integer max, Random r){
        return r.nextInt(max - min) + min;
    }


    /**
     * Get all the tables that exist in an OMX file with their names
     * @param omxFileName
     * @return A map with the name and values of all the matrix cores in the OMX file
     */
    public static Map<String, OmxMatrix.OmxDoubleMatrix> readTripTable(File omxFileName){
        OmxFile omxFile = new OmxFile(omxFileName.toString());
        omxFile.openReadOnly();
        Set<String> omxCoreNames = omxFile.getMatrixNames();
        Map<String, OmxMatrix.OmxDoubleMatrix> omxCores = new HashMap<>();

        Integer i = 1;
        for(String coreName:omxCoreNames){
            log.info("Getting " + coreName + ": " + i + " of " + omxCoreNames.size());
            OmxMatrix.OmxDoubleMatrix core = (OmxMatrix.OmxDoubleMatrix) omxFile.getMatrix(coreName);
            omxCores.put(coreName, core);
            i++;
            break;
        }

        omxFile.close();
        return(omxCores);
    }


}
