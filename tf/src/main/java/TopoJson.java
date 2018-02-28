import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by gregmacfarlane on 7/11/17.
 */
public class TopoJson {
    public String type;
    public Map<String, GeometryCollection> objects;
    public ArrayList<Double[][]> arcs;
    public Transform transform;
    public Double[] bbox;

    public static TopoJson reader(File filePath){
        JsonReader reader = null;
        try {
            reader = new JsonReader(new FileReader(filePath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Gson gson = new Gson();
        return gson.fromJson(reader, TopoJson.class);
    }

    public String getType(){
        return this.type;
    }

    public Map<Integer, Coord> getCentroids(String idField, CoordinateTransformation ct){
        Map<Integer, Coord> centroidCoords = new HashMap<>();

        // Loop through centroid points
        Geometry[] centroidArray = this.getGeometries("centroids");
        for(Geometry centroid:centroidArray){
            Integer zoneId = Integer.parseInt(centroid.getCentroidValue(idField));
            Double[] coordinates = centroid.getCoordinates();
            Coord coord = new Coord(coordinates[0], coordinates[1]);
            coord = ct.transform(coord);
            centroidCoords.put(zoneId, coord);
        }

        return centroidCoords;
    }

    public ArrayList<Double[][]> getArcs(){
        return this.arcs;
    }

    public Transform getTransform(){
        return this.transform;
    }

    public Map<String, GeometryCollection> getObjects(){
        return this.objects;
    }

    public Geometry[] getGeometries(String collectionName){
        GeometryCollection collection = this.objects.get(collectionName);
        return collection.getGeometries();
    }

    private class GeometryCollection {
        String type;
        Geometry[] geometries;

        public Geometry[] getGeometries(){
            return this.geometries;
        }

        public String toString(){
            String outString;
            outString = "Collection type : " + this.type +
                    ", Number of objects: " + String.valueOf(this.geometries.length);

            return outString;
        }
    }

    public static double getArcLength(Double [][] arc, TopoJson.Transform transform,
                                      CoordinateTransformation ct){

        double arcLength = 0;
        // initial points in arcs
        double lon1 = arc[0][0];
        double lat1 = arc[0][1];

        for(int i = 0; i < (arc.length - 1); i++){

            double lon2 = arc[i + 1][0] + lon1;
            double lat2 = arc[i + 1][1] + lat1;

            Coord coord1 = ct.transform(new Coord(transform.applyTransformX(lon1),
                    transform.applyTransformY(lat1)));
            Coord coord2 = ct.transform(new Coord(transform.applyTransformX(lon2),
                    transform.applyTransformY(lat2)));

            double dy = Math.abs(coord2.getY() - coord1.getY());
            double dx = Math.abs(coord2.getX() - coord1.getX());

            double d = Math.hypot(dy, dx);
            arcLength = arcLength + d;

            //reset initial point
            lat1 = lat2;
            lon1 = lat2;

        }

        return arcLength;

    }

    public class Transform {
        Double[] scale;
        Double[] translate;
        Double[] bbox;

        public double applyTransformX(double x){
            return x * this.scale[0] + this.translate[0];
        }

        public double applyTransformY(double y){
            return y * this.scale[1] + this.translate[1];
        }

    }
}




