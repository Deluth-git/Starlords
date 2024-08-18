package plugins;

import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import controllers.LordController;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Map;

public class SelectItemDialogPlugin implements InteractionDialogPlugin {

    private enum OptionId {
        INIT,
        END
    }

    private String message;
    private ArrayList<String> choices;
    private ArrayList<String> retVals;
    @Getter
    private String retVal;
    @Setter
    private Script postScript;

    private InteractionDialogPlugin prevPlugin;
    private InteractionDialogAPI dialog;
    private TextPanelAPI textPanel;
    private OptionPanelAPI options;
    private VisualPanelAPI visual;

    public SelectItemDialogPlugin(String message, ArrayList<String> choices, ArrayList<String> retVals) {
        this.message = message;
        this.choices = choices;
        this.retVals = retVals;
    }

    @Override
    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        prevPlugin = dialog.getPlugin();
        dialog.setPlugin(this);
        textPanel = dialog.getTextPanel();
        options = dialog.getOptionPanel();
        visual = dialog.getVisualPanel();
        optionSelected(null, OptionId.INIT);

    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        OptionId option;
        if (optionData instanceof OptionId) {
            option = (OptionId) optionData;
        } else {
            option = OptionId.END;
        }
        switch (option) {
            case INIT:
                textPanel.addPara(message);
                options.clearOptions();
                for (int i = 0; i < choices.size(); i++) {
                    options.addOption(choices.get(i), retVals.get(i));
                }
                options.addOption("Cancel", null);
                break;
            case END:
                retVal = (String) optionData;
                if (postScript != null) {
                    postScript.run();
                }
                dialog.dismiss();
        }
    }

    @Override
    public void optionMousedOver(String optionText, Object optionData) {

    }

    @Override
    public void advance(float amount) {

    }

    @Override
    public void backFromEngagement(EngagementResultAPI battleResult) {

    }

    @Override
    public Object getContext() {
        return null;
    }

    @Override
    public Map<String, MemoryAPI> getMemoryMap() {
        return null;
    }
}
