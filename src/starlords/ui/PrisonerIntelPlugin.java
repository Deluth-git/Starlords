package starlords.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.*;
import starlords.controllers.LordController;
import org.lwjgl.input.Keyboard;
import starlords.person.Lord;
import starlords.person.LordAction;
import starlords.plugins.PrisonerInteractionDialogPlugin;

import java.awt.*;
import java.util.Set;

// Intel entry describing one lord prisoner of the player
public class PrisonerIntelPlugin extends BaseIntelPlugin {

    public static final Object OPEN_COMMS_BUTTON = new Object();
    private final String lordStr;
    private static final float pad = 3;
    private static final float opad = 10;

    public PrisonerIntelPlugin(String lordStr) {
        this.lordStr = lordStr;
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        Lord lord = LordController.getLordById(lordStr);
        FactionAPI faction = lord.getFaction();
        info.addImages(256, 128, pad, 0, faction.getCrest(),
                lord.getLordAPI().getPortraitSprite());
        if (lord.getCurrAction() != LordAction.IMPRISONED
                || !Global.getSector().getPlayerPerson().getId().equals(lord.getCaptor())) {
            info.addPara("This prisoner was recently freed.", opad);
            return;
        }
        info.addPara("You are currently holding " + lord.getLordAPI().getNameString()
                + " prisoner. Talk to them to ransom them or set them free for future goodwill.", opad);
        ButtonAPI button = info.addButton("Open Comms", OPEN_COMMS_BUTTON, 150, 20, opad);
        button.setShortcut(Keyboard.KEY_C, true);
    }

    @Override
    public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
        if (buttonId == OPEN_COMMS_BUTTON) {
            PrisonerInteractionDialogPlugin conversationDelegate = new PrisonerInteractionDialogPlugin();
            conversationDelegate.setUi(ui);
            ui.showDialog(LordController.getLordById(lordStr).getFleet(), conversationDelegate);
        }
    }

    @Override
    public boolean hasSmallDescription() {
        return true;
    }

    @Override
    public boolean hasLargeDescription() {
        return false;
    }

    @Override
    public boolean isDone() {
        Lord lord = LordController.getLordById(lordStr);
        return lord.getCurrAction() != LordAction.IMPRISONED
                || !Global.getSector().getPlayerPerson().getId().equals(lord.getCaptor());
    }

    @Override
    public boolean shouldRemoveIntel() {
        return isDone();
    }

    @Override
    public String getName() {
        return "Prisoner: " + LordController.getLordById(lordStr).getLordAPI().getNameString();
    }

    @Override
    public FactionAPI getFactionForUIColors() {
        return LordController.getLordById(lordStr).getFaction();
    }

    @Override
    public String getIcon() {
        return LordController.getLordById(lordStr).getLordAPI().getPortraitSprite();
    }

    public Color getTitleColor(ListInfoMode mode) {
        return getFactionForUIColors().getBaseUIColor();
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add("Prisoners");
        return tags;
    }
}