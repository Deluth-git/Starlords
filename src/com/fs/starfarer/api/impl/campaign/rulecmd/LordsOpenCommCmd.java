package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;
import starlords.plugins.LordInteractionDialogPluginImpl;

import java.util.List;
import java.util.Map;

public class LordsOpenCommCmd extends BaseCommandPlugin {

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        LordInteractionDialogPluginImpl conversationDelegate = new LordInteractionDialogPluginImpl();
        conversationDelegate.init(dialog);
        return true;
    }
}
