package starlords.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.util.Misc;
import lombok.Getter;
import lombok.Setter;
import starlords.util.GenderUtils;
import starlords.util.StringUtil;
import starlords.util.Utils;

import java.awt.*;
import java.util.Random;

import static starlords.util.Constants.BASE_RANSOM;

public class PrisonerInteractionDialogPlugin extends LordInteractionDialogPluginImpl {
    private enum PrisonerOptionId {
        INIT,
        RELEASE_RESOLVE,
        RANSOM_RESOLVE
    }

    private int ransomAmount;
    @Getter
    @Setter
    private static IntelUIAPI ui; // this is the intel ui that should be updated if prisoners are released
    @Override
    protected String setDialogType() {
        return "prisoner";
    }
    @Override
    protected String setStartingDialogSet(){
        return "optionSet_greetings_prisoner";
    }

    @Override
    protected String setStartingDialog() {
        return "greetings_prisoner";
    }
}
