package starlords.util.dialogControler;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.Getter;
import org.apache.log4j.Logger;
import starlords.person.Lord;
import starlords.util.dialogControler.dialogAddon.DialogAddon_Base;

import java.util.ArrayList;

public class DialogOption {
    public String optionID;
    ArrayList<DialogAddon_Base> addons;
    @Getter
    Lord targetLord;
    @Getter
    MarketAPI targetMarket;
    public DialogOption(String optionID, ArrayList<DialogAddon_Base> addons,Lord targetLord,MarketAPI targetMarket){
        this(optionID, addons);
        this.targetLord = targetLord;
        this.targetMarket=targetMarket;
    }
    public DialogOption(String optionID, ArrayList<DialogAddon_Base> addons){
        this.optionID = optionID;
        this.addons=addons;
        int size = 0;
        if (addons != null) size = addons.size();
        Logger log = Global.getLogger(DialogOption.class);
        log.info("creating a option with a link of: "+optionID+" and a set of addons of size: "+size);
    }
    public void applyAddons(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord){
        Logger log = Global.getLogger(DialogOption.class);
        log.info("applying addon's from option data with link to: "+optionID);
        if (addons == null) return;
        for (DialogAddon_Base a : addons){
            log.info("  applying addon from option of class name: "+a.getClass().getName());
            a.apply(textPanel, options, dialog,lord,targetLord,targetMarket);
        }
    }
}
