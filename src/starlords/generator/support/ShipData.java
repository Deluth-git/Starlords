package starlords.generator.support;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;

public class ShipData {
    //this is a array to meet all the different types of things one variant can be.
    @Getter
    private HashMap<String, Float> spawnWeight = new HashMap<>();
    @Getter
    private String hullID;
    @Getter
    private String hullSize;
    @Getter
    private String hullType;
    public ShipData(String hullID,String hullSize,String hullType){
        this.hullID=hullID;
        this.hullSize=hullSize;
        this.hullType=hullType;
    }
    public void addVariant(String id, float weight){
        if (spawnWeight.get(id) != null && spawnWeight.get(id) > weight){
            return;
        }
        spawnWeight.put(id,weight);
    }
}
