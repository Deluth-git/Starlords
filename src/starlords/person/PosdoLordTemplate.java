package starlords.person;

import java.util.HashMap;

public class PosdoLordTemplate {
    public String name;
    public String factionId;
    public String fleetName;
    public boolean isMale;
    public String personality;
    public String flagShip;
    public String lore;
    public HashMap<String, Integer> shipPrefs;
    public String fief;
    public String portrait;
    public int level;
    public String battlePersonality;
    public int ranking;
    public String preferredItemId;
    public PosdoLordTemplate(){
    }
    public PosdoLordTemplate(String name,String factionId,String fleetName, boolean isMale, String personality, String flagShip, String lore, HashMap<String, Integer> shipPrefs,String fief,String portrait,int level,String battlePersonality,int ranking,String preferredItemId){
        this.name=name;
        this.factionId=factionId;
        this.fleetName=fleetName;
        this.isMale=isMale;
        this.personality=personality;
        this.flagShip=flagShip;
        this.lore=lore;
        this.shipPrefs=shipPrefs;
        this.fief=fief;
        this.portrait=portrait;
        this.level=level;
        this.battlePersonality=battlePersonality;
        this.ranking=ranking;
        this.preferredItemId=preferredItemId;
    }
}
