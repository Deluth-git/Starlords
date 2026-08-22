package starlords.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import org.apache.log4j.Logger;
import starlords.lunaSettings.StoredSettings;
import starlords.person.PosdoLordTemplate;

import java.util.ArrayList;

public class LordGeneratorListener_base {
    private static ArrayList<LordGeneratorListener_base> listiners = new ArrayList<>();
    public LordGeneratorListener_base(){
        listiners.add(this);
    }
    public void editLordPerson(PersonAPI lord){

    }
    public void editLordPerson(PersonAPI lord, MarketAPI market){

    }
    public void editLordPerson(PersonAPI lord,com.fs.starfarer.api.campaign.SectorEntityToken system, float x, float y){

    }
    public void editLord(PosdoLordTemplate lord){

    }
    public void editLord(PosdoLordTemplate lord,MarketAPI market){

    }
    public void editLord(PosdoLordTemplate lord,com.fs.starfarer.api.campaign.SectorEntityToken system, float x, float y){

    }

    public static void runEditLordPersons(PersonAPI lord, MarketAPI market, com.fs.starfarer.api.campaign.SectorEntityToken system, float x, float y){
        for (LordGeneratorListener_base a : listiners){
            a.editLordPerson(lord);
            a.editLordPerson(lord,market);
            a.editLordPerson(lord,system,x,y);
        }
    }
    public static void runEditLord(PosdoLordTemplate lord, MarketAPI market, com.fs.starfarer.api.campaign.SectorEntityToken system, float x, float y){
        for (LordGeneratorListener_base a : listiners){
            a.editLord(lord);
            a.editLord(lord,market);
            a.editLord(lord,system,x,y);
        }
    }
    public static void removeListener(LordGeneratorListener_base listener){
        listiners.remove(listener);
    }
}
