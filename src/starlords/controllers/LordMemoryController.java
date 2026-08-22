package starlords.controllers;

import com.fs.starfarer.api.Global;
import starlords.util.memoryUtils.LordOverridingMemory;

import java.util.HashMap;

public class LordMemoryController {
    private static final String memKey = "$STARLORDS_LORD_MEMORY_CONTROLLER";
    private HashMap<String, LordOverridingMemory> memory = new HashMap<>();
    private static LordMemoryController controller;
    public static LordOverridingMemory getLordMemory(String lordID){
        insureControlActive();
        if (controller.memory.containsKey(lordID)) return controller.memory.get(lordID);
        LordOverridingMemory newMemory = new LordOverridingMemory();
        controller.memory.put(lordID,newMemory);
        return newMemory;
    }
    public static boolean containsLord(String lordID){
        return controller.memory.containsKey(lordID);
    }

    public static void insureControlActive(){
        if (controller != null) return;
        load();
    }
    public static void save(){
        if (controller == null)return;
        Global.getSector().getMemory().set(memKey,controller);
    }
    public static void load(){
        if (!Global.getSector().getMemory().contains(memKey)){
            controller = new LordMemoryController();
            return;
        }
        Object dataT = Global.getSector().getMemory().get(memKey);
        controller = (LordMemoryController)dataT;
    }
}
