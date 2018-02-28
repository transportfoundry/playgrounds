import java.util.Map;

public class Geometry {

    String type;
    Map<String, String> properties;
    Integer[][] arcs; // lines will have only a single arc
    Double[] coordinates;

    public Map<String, String> getProperties(){
        return this.properties;
    }

    public String toString(){
        String outString = "type: " + this.type +  " id: " + properties.get("id");
        return outString;
    }

    public String getId(){
        return properties.get("id");
    }

    public String getStartId(){
        return properties.get("start");
    }

    public String getEndId(){
        return properties.get("end");
    }

    public Integer getToLanes(){
        return Integer.parseInt(properties.get("to_lanes"));
    }

    public Double getToCapacity(){
        return Double.parseDouble(properties.get("to_capacity"));
    }

    public Integer getFromLanes(){
        return Integer.parseInt(properties.get("from_lanes"));
    }

    public Double getFromCapacity(){
        return Double.parseDouble(properties.get("from_capacity"));
    }

    public String getCentroidValue(String fieldName){
        return properties.get(fieldName);
    }

    public Double[] getCoordinates(){ return coordinates; }

    /**
     * Return the link free-flow speed. TF uses km/hr as the free-flow speed on
     * the topojson network, but MATSim requires meters per second. This function
     * applies the conversion directly.
     * @return
     */
    public Double getFreeSpeed(){
        Double freeSpeed = Double.parseDouble(properties.get("ffs"));
        return freeSpeed * 0.277778; // km/hr to m/s
    }

    public Integer getFunctionalClass(){
        return Integer.parseInt(properties.get("class"));
    }
    public void setDirection(){

        if(this.getToLanes().equals(0) & this.getFromLanes().equals(0)){
            this.properties.put("direction", "B"); // no lanes
            this.properties.put("to_lanes", "1");
            this.properties.put("to_capacity", "200");
            this.properties.put("from_lanes", "1");
            this.properties.put("from_capacity", "200");
        } else if(this.getToLanes() > 0 & this.getFromLanes() > 0){
            this.properties.put("direction", "B");
        } else if(this.getFromLanes() > 0){
            this.properties.put("direction", "F");
        } else if(this.getToLanes() > 0){
            this.properties.put("direction", "T");
        }

    }

    public Integer getArcIndex(){
        return arcs[0][0];
    }

    public String getDirection(){
        return properties.get("direction");
    }

    public String printArcIndex(){
        return String.valueOf(arcs[0][0]);
    }

}
