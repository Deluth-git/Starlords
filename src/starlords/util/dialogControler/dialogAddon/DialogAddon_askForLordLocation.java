package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import starlords.person.Lord;
import starlords.util.Utils;
import starlords.util.dialogControler.DialogSet;

import java.util.HashMap;

public class DialogAddon_askForLordLocation extends DialogAddon_Base {
    Lord savedLord;
    public DialogAddon_askForLordLocation(Lord savedLord){
        this.savedLord=savedLord;
    }
    @Override
    public void apply(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        //NO LONGER IN USE.
        if (savedLord != null && savedLord.getLordAPI().getFleet().isAlive()) {
            HashMap<String,String> insirts = new HashMap<>();
            insirts.put("%c0",savedLord.getTitle());
            insirts.put("%c1",savedLord.getLordAPI().getNameString());
            insirts.put("%c2",Utils.getNearbyDescription(savedLord.getLordAPI().getFleet()));
            DialogSet.addParaWithInserts("foundLordsLocation",lord,targetLord,targetMarket,textPanel,options,dialog,false,insirts);
        } else {
            HashMap<String,String> insirts = new HashMap<>();
            insirts.put("%c0",savedLord.getTitle());
            insirts.put("%c1",savedLord.getLordAPI().getNameString());
            DialogSet.addParaWithInserts("failedToFindLordsLocation",lord,targetLord,targetMarket,textPanel,options,dialog,false,insirts);
        }
    }
}
