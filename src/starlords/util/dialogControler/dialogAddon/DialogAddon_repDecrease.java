package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import starlords.person.Lord;
import starlords.util.Utils;
import starlords.util.dialogControler.DialogSet;

import java.util.HashMap;

public class DialogAddon_repDecrease extends DialogAddon_Base{
    int max, min;
    public DialogAddon_repDecrease(int max,int min){
        this.max = max;
        this.min = min;
    }
    @Override
    public void apply(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket){
        int ranChange = max - min;
        if (ranChange > 0) ranChange = Utils.rand.nextInt(ranChange);
        int change = min + ranChange;
        lord.getLordAPI().getRelToPlayer().adjustRelationship((float) (change*-0.01), null);

        HashMap<String,String> inserts = new HashMap<>();
        inserts.put("%c0",""+change);
        DialogSet.addParaWithInserts("relation_decrease",lord,targetLord,targetMarket,textPanel,options,dialog,false,inserts);

    }
}
