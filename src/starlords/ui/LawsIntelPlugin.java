package starlords.ui;

import com.fs.starfarer.api.Script;
import starlords.ai.LordAI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import starlords.controllers.EventController;
import starlords.controllers.FiefController;
import starlords.controllers.LordController;
import starlords.controllers.PoliticsController;
import starlords.faction.LawLevel;
import starlords.faction.LawProposal;
import starlords.faction.Lawset;
import starlords.person.Lord;
import starlords.person.LordEvent;
import starlords.plugins.SelectItemDialogPlugin;
import starlords.util.StringUtil;
import starlords.util.Utils;
import starlords.util.factionUtils.FactionTemplateController;

import java.awt.*;
import java.util.*;
import java.util.List;

import static starlords.util.Constants.CATEGORY_UI;
import static starlords.util.Constants.DARK_GOLD;

public class LawsIntelPlugin extends BaseIntelPlugin {

   static class ButtonObject {
        String type;
        int level;
        public ButtonObject(String s) {
            type = s;
        }

        public ButtonObject clone(int i) {
            ButtonObject ret = new ButtonObject(type);
            ret.level = i;
            return ret;
        }
    }

    private static ButtonObject CROWN_AUTHORITY_BUTTON = new ButtonObject(
            Lawset.LawType.CROWN_AUTHORITY.toString());
    private static ButtonObject NOBLE_AUTHORITY_BUTTON = new ButtonObject(
            Lawset.LawType.NOBLE_AUTHORITY.toString());
    private static ButtonObject TRADE_LAW_BUTTON = new ButtonObject(
            Lawset.LawType.TRADE_LAW.toString());
    private static ButtonObject FEAST_LAW_BUTTON = new ButtonObject(
            Lawset.LawType.FEAST_LAW.toString());
    private static Object APPOINT_MARSHAL_BUTTON = new Object();
    private static Object AWARD_FIEF_BUTTON = new Object();
    private static Object DECLARE_WAR_BUTTON = new Object();
    private static Object SUE_FOR_PEACE_BUTTON = new Object();
    private static Object REVOKE_FIEF_BUTTON = new Object();
    private static Object TRANSFER_FIEF_BUTTON = new Object();
    private static Object CHANGE_TITLE_BUTTON = new Object();
    private static Object EXILE_LORD_BUTTON = new Object();
    private static Object START_CAMPAIGN_BUTTON = new Object();
    private static Object END_CAMPAIGN_BUTTON = new Object();

    // these are only used for convenience and are not expected to hold useful/accurate data in the general case
    private Lawset laws;
    private FactionAPI faction;

    private static LawsIntelPlugin instance;

    private LawsIntelPlugin(){
        setNew(false);
    }

    @Override
    public void createLargeDescription(CustomPanelAPI panel, float width, float height) {
        float pad = 3;
        float opad = 10;
        float headerHeight = 25;
        float logoHeight = 160;
        TooltipMakerAPI header = panel.createUIElement(width, headerHeight, false);
        Color uiColor = Global.getSettings().getBasePlayerColor();
        faction = Misc.getCommissionFaction();
        if (faction == null || (!FactionTemplateController.getTemplate(faction).isCanPreformPolicy() && !FactionTemplateController.getTemplate(faction).isCanPreformDiplomacy())) {
            if (PoliticsController.playerFactionHasLaws()) {
                faction = Global.getSector().getPlayerFaction();
            } else {
                header.addPara("Please join or start an organized faction to participate in politics", pad);
                panel.addUIElement(header).inTL(0, 0);
                return;
            }
        }
        //header.addImage(faction.getCrest(), headerHeight, headerHeight, pad);
        header.addSectionHeading("Laws of the Realm",
                faction.getBrightUIColor(), faction.getDarkUIColor(), Alignment.LMID, opad);
        header.addTooltipToPrevious(new ToolTip(250, StringUtil.getString(CATEGORY_UI, "laws_tooltip")),
                TooltipMakerAPI.TooltipLocation.BELOW);
        panel.addUIElement(header).inTL(0, 0);

        TooltipMakerAPI logo = panel.createUIElement(logoHeight, logoHeight, false);
        logo.addImage(faction.getCrest(), logoHeight, logoHeight, pad);
        panel.addUIElement(logo).belowRight(header, 0);
        laws = PoliticsController.getLaws(faction);
        // Display all laws
        // Display crown/noble/feast/tax laws as a grid
        TooltipMakerAPI prev;
        prev = addLawRow(panel, header, Lawset.LawType.CROWN_AUTHORITY, faction, CROWN_AUTHORITY_BUTTON);
        prev = addLawRow(panel, prev, Lawset.LawType.NOBLE_AUTHORITY, faction, NOBLE_AUTHORITY_BUTTON);
        prev = addLawRow(panel, prev, Lawset.LawType.TRADE_LAW, faction, TRADE_LAW_BUTTON);
        prev = addLawRow(panel, prev, Lawset.LawType.FEAST_LAW, faction, FEAST_LAW_BUTTON);

        TooltipMakerAPI internalAffairsHeader = panel.createUIElement(width, headerHeight, false);
        internalAffairsHeader.addSectionHeading("Internal Affairs",
                faction.getBrightUIColor(), faction.getDarkUIColor(), Alignment.LMID, opad);
        panel.addUIElement(internalAffairsHeader).belowLeft(prev, 0);
        // Change Marshal

        TooltipMakerAPI marshalPanel = panel.createUIElement(width / 2, 80, false);
        Lord marshal = LordController.getLordOrPlayerById(laws.getMarshal());
        marshalPanel.addPara("Current Marshal:", uiColor, pad);
        if (marshal != null) {
            TooltipMakerAPI marshalImagePanel = marshalPanel.beginImageWithText(
                    marshal.getLordAPI().getPortraitSprite(), 48);
            marshalImagePanel.addPara(marshal.getLordAPI().getNameString(), faction.getBaseUIColor(), pad);
            marshalImagePanel.addButton("Change Marshal", APPOINT_MARSHAL_BUTTON, 150, 20, pad);
            if (marshal.isPlayer()) {
                if (EventController.getCurrentCampaign(marshal.getFaction()) != null) {
                    marshalImagePanel.addButton("End Campaign", END_CAMPAIGN_BUTTON, 150, 20, pad);
                } else {
                    ButtonAPI campaignButton = marshalImagePanel.addButton("Start Campaign", START_CAMPAIGN_BUTTON, 150, 20, pad);
                    // cannot start campaign if on cooldown
                    int daysPassed = (int) Utils.getDaysSince(
                            EventController.getLastCampaignTime(marshal.getFaction()));
                    if (daysPassed < LordAI.CAMPAIGN_COOLDOWN) {
                        campaignButton.setEnabled(false);
                        marshalImagePanel.addTooltipToPrevious(new ToolTip(250,
                                "Campaign on cooldown for " + (LordAI.CAMPAIGN_COOLDOWN - daysPassed) + " days"), TooltipMakerAPI.TooltipLocation.BELOW);
                    }
                }
            }
            marshalPanel.addImageWithText(pad);
        } else {
            marshalPanel.addPara("None", faction.getBaseUIColor(), pad);
            marshalPanel.addButton("Appoint Marshal", APPOINT_MARSHAL_BUTTON, 150, 20, pad);
        }
        panel.addUIElement(marshalPanel).belowLeft(internalAffairsHeader, 0);


        // Award Fief
        TooltipMakerAPI fiefPanel = panel.createUIElement(width / 2, 80, false);
        fiefPanel.addPara("Unclaimed Fief:", uiColor, pad);
        MarketAPI fief = laws.getFiefAward();
        if (fief != null) {
            fiefPanel.addPara(fief.getName(), faction.getBaseUIColor(), 3 * pad);
        } else {
            fiefPanel.addPara("None", faction.getBaseUIColor(), 3 * pad);
        }
        ButtonAPI fiefButton = fiefPanel.addButton("Award Fief", AWARD_FIEF_BUTTON, 150, 20, pad);
        if (fief == null) {
            fiefButton.setEnabled(false);
        }
        panel.addUIElement(fiefPanel).belowMid(internalAffairsHeader, 0);


        // War and Peace
        TooltipMakerAPI diplomacyPanel = panel.createUIElement(width, headerHeight, false);
        diplomacyPanel.addSectionHeading("Foreign Relations",
                faction.getBrightUIColor(), faction.getDarkUIColor(), Alignment.LMID, opad);
        ButtonAPI button1 = diplomacyPanel.addButton("Declare War", DECLARE_WAR_BUTTON, 150, 20, opad);
        ButtonAPI button2 = diplomacyPanel.addButton("Sue for Peace", SUE_FOR_PEACE_BUTTON, 150, 20, opad);
        button2.getPosition().rightOfTop(button1, opad);
        panel.addUIElement(diplomacyPanel).belowLeft(marshalPanel, opad);


        // King-only laws
        TooltipMakerAPI rulerPanel = null;
        if (faction.equals(Global.getSector().getPlayerFaction())) {
            // promote lord, demote lord, exile lord, revoke fief
            rulerPanel = panel.createUIElement(width, headerHeight, false);
            rulerPanel.addSectionHeading("Ruler Actions",
                    faction.getBrightUIColor(), faction.getDarkUIColor(), Alignment.LMID, opad);
            ButtonAPI button3 = rulerPanel.addButton("Grant/Revoke Title", CHANGE_TITLE_BUTTON, faction.getBrightUIColor(),
                    faction.getDarkUIColor(), 150, 20, opad);
            ButtonAPI button4 = rulerPanel.addButton("Revoke Fief", REVOKE_FIEF_BUTTON, faction.getBrightUIColor(),
                    faction.getDarkUIColor(), 150, 20, opad);
            ButtonAPI button5 = rulerPanel.addButton("Exile Lord", EXILE_LORD_BUTTON, faction.getBrightUIColor(),
                    faction.getDarkUIColor(), 150, 20, opad);
            button4.getPosition().rightOfTop(button3, opad);
            button5.getPosition().rightOfTop(button4, opad);
            panel.addUIElement(rulerPanel).belowLeft(diplomacyPanel, opad);
        }

        // Personal Decisions
        TooltipMakerAPI personalPanel = panel.createUIElement(width, headerHeight, false);
        personalPanel.addSectionHeading("Personal Decisions",
                faction.getBrightUIColor(), faction.getDarkUIColor(), Alignment.LMID, opad);
        ButtonAPI button6 = personalPanel.addButton("Transfer Fief", TRANSFER_FIEF_BUTTON, faction.getBrightUIColor(),
                faction.getDarkUIColor(), 150, 20, opad);

        if(LordController.getPlayerLord().getFiefs().isEmpty())
            button6.setEnabled(false);
        if(rulerPanel == null)
            panel.addUIElement(personalPanel).belowLeft(diplomacyPanel, opad);
        else
            panel.addUIElement(personalPanel).belowLeft(rulerPanel, opad);

    }

    private TooltipMakerAPI addLawRow(CustomPanelAPI panel, UIComponentAPI prev,
                                      Lawset.LawType type, FactionAPI faction, ButtonObject buttonObject) {
        float gridHeight = 50;
        TooltipMakerAPI label = panel.createUIElement(100, gridHeight, false);
        LabelAPI title = label.addPara(type.lawName, 0);
        title.setAlignment(Alignment.MID);
        label.addTooltipToPrevious(new ToolTip(200,
                StringUtil.getString(CATEGORY_UI, "laws_" + type.toString().toLowerCase() + "_tooltip")),
                TooltipMakerAPI.TooltipLocation.BELOW);
        panel.addUIElement(label).belowLeft(prev, 0);
        LawLevel level = PoliticsController.getLaws(faction).getLawLevel(type);
        prev = label;
        for (int i = 0; i < 5; i++) {
            TooltipMakerAPI buttonPanel = panel.createUIElement(50, gridHeight, false);
            ButtonAPI button;
            if (level.ordinal() == i) {
                Color bgColor = faction.isPlayerFaction() ? DARK_GOLD : faction.getDarkUIColor();
                button = buttonPanel.addButton(LawLevel.values()[i].displayName, buttonObject.clone(i), faction.getBrightUIColor(),
                        bgColor, 50, gridHeight, 0);
                button.setClickable(false);
                button.setMouseOverSound(null);
            } else if (Math.abs(level.ordinal() - i) == 1) {
                button = buttonPanel.addButton(LawLevel.values()[i].displayName, buttonObject.clone(i), 50, gridHeight, 0);
            } else {
                button = buttonPanel.addButton(LawLevel.values()[i].displayName, buttonObject.clone(i), 50, gridHeight, 0);
                button.setEnabled(false);
                button.setClickable(false);
                button.setMouseOverSound(null);
            }

            String message = StringUtil.getString(CATEGORY_UI, "laws_" + type.toString().toLowerCase() + "_" + i);
            List<String> messages = Arrays.asList(message.split("\\|"));
            buttonPanel.addTooltipToPrevious(new ToolTip(200, messages),
                    TooltipMakerAPI.TooltipLocation.BELOW);

            panel.addUIElement(buttonPanel).rightOfTop(prev, 0);
            prev = buttonPanel;
        }
        return label;
    }

    @Override
    public void createConfirmationPrompt(Object buttonId, TooltipMakerAPI prompt) {
        if (buttonId == START_CAMPAIGN_BUTTON) {
            prompt.addPara("This will call the lords of the realm to arms. Continue?", 3);
        } else if (buttonId == END_CAMPAIGN_BUTTON) {
            prompt.addPara("This will end the current campaign. Continue?", 3);
        } else if (buttonId instanceof ButtonObject) {
            prompt.addPara("You are submitting a new legislative proposal. Continue?", 3);
        }
    }

    @Override
    public boolean doesButtonHaveConfirmDialog(Object buttonId) {
        if (buttonId instanceof ButtonObject || buttonId == START_CAMPAIGN_BUTTON
                || buttonId == END_CAMPAIGN_BUTTON) return true;
        return super.doesButtonHaveConfirmDialog(buttonId);
    }

    @Override
    public void buttonPressConfirmed(Object buttonId, final IntelUIAPI ui) {
        if (buttonId == START_CAMPAIGN_BUTTON) {
            // TODO
            LordEvent campaign = new LordEvent(LordEvent.CAMPAIGN, LordController.getPlayerLord());
            EventController.addCampaign(campaign);
            ui.updateUIForItem(this);
            return;
        } else if (buttonId == END_CAMPAIGN_BUTTON) {
            LordEvent campaign = EventController.getCurrentCampaign(faction);
            EventController.endCampaign(campaign);
            ui.updateUIForItem(this);
            return;
        }

        // cant propose a new law if player's current proposal is being debated
        LawProposal currDebate = PoliticsController.getCurrProposal(Utils.getRecruitmentFaction());
        if (currDebate != null && currDebate.getOriginator().equals(
                LordController.getPlayerLord().getLordAPI().getId())) return;

        if (buttonId instanceof ButtonObject) {
            ButtonObject buttonObject = (ButtonObject) buttonId;
            Lawset.LawType law = Lawset.LawType.valueOf(buttonObject.type);
            LawProposal proposal = new LawProposal(law, LordController.getPlayerLord().getLordAPI().getId(),
                    null, null, null, buttonObject.level);
            proposal.setPlayerSupports(true);
            PoliticsController.addProposal(LordController.getPlayerLord(), proposal);
            PoliticsController.updateProposal(proposal);
            ui.updateIntelList();
        } else if (buttonId == APPOINT_MARSHAL_BUTTON || buttonId == AWARD_FIEF_BUTTON || buttonId == EXILE_LORD_BUTTON) {
            String message;
            final String targetFief;
            final Lawset.LawType law;
            final Lord playerLord = LordController.getPlayerLord();
            if (buttonId == APPOINT_MARSHAL_BUTTON) {
                law = Lawset.LawType.APPOINT_MARSHAL;
                targetFief = null;
                message = "You are proposing new legislation. Select who you nominate to be the next Marshal.";
            } else if (buttonId == AWARD_FIEF_BUTTON) {
                law = Lawset.LawType.AWARD_FIEF;
                targetFief = laws.getFiefAward().getId();
                message = "You are proposing new legislation. Select who you nominate to receive " + laws.getFiefAward().getName() + ".";
            } else {
                law = Lawset.LawType.EXILE_LORD;
                targetFief = null;
                message = "You are proposing new legislation. Select who you nominate to be exiled from the faction.";
            }
            ArrayList<Lord> lords = new ArrayList<>();
            ArrayList<String> options = new ArrayList<>();
            ArrayList<String> retVals = new ArrayList<>();
            // cant exile player from their own faction
            if (buttonId != EXILE_LORD_BUTTON && (
                    !playerLord.isMarshal() || buttonId == AWARD_FIEF_BUTTON)) {
                lords.add(LordController.getPlayerLord());
            }
            for (Lord lord : LordController.getLordsList()) {
                if (lord.isMarshal() && buttonId == APPOINT_MARSHAL_BUTTON) continue;
                if (lord.getFaction().equals(faction)) {
                    lords.add(lord);
                }
            }
            Utils.canonicalLordSort(lords);
            for (Lord lord : lords) {
                    options.add(lord.getTitle() + " " + lord.getLordAPI().getNameString());
                    retVals.add(lord.getLordAPI().getId());
            }
            final SelectItemDialogPlugin plugin = new SelectItemDialogPlugin(message, options, retVals);
            plugin.setPostScript(new Script() {
                @Override
                public void run() {
                    String selection = plugin.getRetVal();
                    if (selection != null) {
                        LawProposal proposal = new LawProposal(law, playerLord.getLordAPI().getId(),
                                selection, targetFief, null, 0);
                        proposal.setPlayerSupports(true);
                        PoliticsController.addProposal(LordController.getPlayerLord(), proposal);
                        PoliticsController.updateProposal(proposal);
                        ui.updateIntelList();
                    }
                }
            });
            ui.showDialog(null, plugin);

        } else if (buttonId == DECLARE_WAR_BUTTON || buttonId == SUE_FOR_PEACE_BUTTON) {
            String message;
            final Lawset.LawType law;
            if (buttonId == DECLARE_WAR_BUTTON) {
                law = Lawset.LawType.DECLARE_WAR;
                message = "You are proposing new legislation. Select the faction you propose to declare war on.";

            } else {
                law = Lawset.LawType.SUE_FOR_PEACE;
                message = "You are proposing new legislation. Select the faction you propose to offer peace to.";
            }

            ArrayList<String> options = new ArrayList<>();
            ArrayList<String> retVals = new ArrayList<>();
            // only can do diplo on factions with markets
            for (FactionAPI faction : LordController.getFactionsWithLords()) {
                if (faction.equals(this.faction)) continue;
                if (faction.equals(Global.getSector().getPlayerFaction())) continue;
                //if (!Utils.canHaveRelations(faction)) continue;
                if (!FactionTemplateController.getTemplate(faction).isCanPreformDiplomacy()) continue;
                if (!FactionTemplateController.getTemplate(this.faction).isCanPreformDiplomacy()) continue;
                if (faction.isHostileTo(this.faction) != (buttonId == DECLARE_WAR_BUTTON)) {
                    options.add(faction.getDisplayName());
                    retVals.add(faction.getId());
                }
            }
            final SelectItemDialogPlugin plugin = new SelectItemDialogPlugin(message, options, retVals);
            plugin.setPostScript(new Script() {
                @Override
                public void run() {
                    String selection = plugin.getRetVal();
                    if (selection != null) {
                        LawProposal proposal = new LawProposal(law, LordController.getPlayerLord().getLordAPI().getId(),
                                null, null, selection, 0);
                        proposal.setPlayerSupports(true);
                        PoliticsController.addProposal(LordController.getPlayerLord(), proposal);
                        PoliticsController.updateProposal(proposal);
                        ui.updateIntelList();
                    }
                }
            });
            ui.showDialog(null, plugin);

        } else if (buttonId == REVOKE_FIEF_BUTTON) {
            String message = "You are proposing new legislation. Select the fief to revoke.";
            ArrayList<String> options = new ArrayList<>();
            ArrayList<String> retVals = new ArrayList<>();
            FactionAPI faction = Utils.getRecruitmentFaction();
            for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
                Lord owner = FiefController.getOwner(market);
                if (faction.equals(market.getFaction()) && owner != null) {
                    options.add(market.getName() + ", owned by " + owner.getLordAPI().getNameString());
                    retVals.add(market.getId());
                }
            }
            final SelectItemDialogPlugin plugin = new SelectItemDialogPlugin(message, options, retVals);
            plugin.setPostScript(new Script() {
                @Override
                public void run() {
                    String selection = plugin.getRetVal();
                    if (selection != null) {
                        MarketAPI market = Global.getSector().getEconomy().getMarket(selection);
                        LawProposal proposal = new LawProposal(Lawset.LawType.REVOKE_FIEF,
                                LordController.getPlayerLord().getLordAPI().getId(),
                                FiefController.getOwner(market).getLordAPI().getId(),
                                selection, null, 0);
                        proposal.setPlayerSupports(true);
                        PoliticsController.addProposal(LordController.getPlayerLord(), proposal);
                        PoliticsController.updateProposal(proposal);
                        ui.updateIntelList();
                    }
                }
            });
            ui.showDialog(null, plugin);
        } else if (buttonId == CHANGE_TITLE_BUTTON) {
            String message = "You are proposing new legislation. Select the lord to be promoted/demoted.";
            ArrayList<Lord> lords = new ArrayList<>();
            ArrayList<String> options = new ArrayList<>();
            ArrayList<String> retVals = new ArrayList<>();
            FactionAPI faction = Utils.getRecruitmentFaction();
            for (Lord lord : LordController.getLordsList()) {
                if (lord.getFaction().equals(faction)) {
                    lords.add(lord);
                }
            }
            Utils.canonicalLordSort(lords);
            for (Lord lord : lords) {
                options.add(lord.getTitle() + " " + lord.getLordAPI().getNameString());
                retVals.add(lord.getLordAPI().getId());
            }

            final SelectItemDialogPlugin plugin = new SelectItemDialogPlugin(message, options, retVals);
            plugin.setPostScript(new Script() {
                @Override
                public void run() {
                    final String selection = plugin.getRetVal();
                    if (selection != null) {
                        // nested dialogs, yay
                        Lord lord = LordController.getLordOrPlayerById(selection);
                        String message2 = "Select new rank for " + lord.getLordAPI().getNameString();
                        int rank = lord.getRanking();
                        ArrayList<String> options2 = new ArrayList<>();
                        ArrayList<String> retVals2 = new ArrayList<>();
                        for (int i = 0; i < 3; i++) {
                            if (rank != i) {
                                options2.add(Utils.getTitle(lord.getFaction(), i));
                                retVals2.add(Integer.toString(i));
                            }
                        }
                        final SelectItemDialogPlugin plugin2 = new SelectItemDialogPlugin(message2, options2, retVals2);
                        plugin2.setPostScript(new Script() {
                            @Override
                            public void run() {
                                String selection2 = plugin2.getRetVal();
                                if (selection2 != null) {
                                    int newRank = Integer.valueOf(selection2);
                                    LawProposal proposal = new LawProposal(Lawset.LawType.CHANGE_RANK,
                                            LordController.getPlayerLord().getLordAPI().getId(),
                                            selection, null, null, newRank);
                                    proposal.setPlayerSupports(true);
                                    PoliticsController.addProposal(LordController.getPlayerLord(), proposal);
                                    PoliticsController.updateProposal(proposal);
                                    ui.updateIntelList();
                                }
                            }
                        });
                        ui.showDialog(null, plugin2);
                    }
                }
            });
            ui.showDialog(null, plugin);

        }
        else if (buttonId == TRANSFER_FIEF_BUTTON) {
            String message = "You are going to transfer one of your fiefs to another Lord. Select the fief to transfer.";
            ArrayList<Lord> lords = new ArrayList<>();
            ArrayList<String> optionsLords = new ArrayList<>();
            ArrayList<String> retValsLords = new ArrayList<>();
            for (Lord lord : LordController.getLordsList()) {
                if (lord.getFaction().equals(faction) && !lord.isPlayer()) {
                    lords.add(lord);
                }
            }
            Utils.canonicalLordSort(lords);
            for (Lord lord : lords) {
                optionsLords.add(lord.getTitle() + " " + lord.getLordAPI().getNameString());
                retValsLords.add(lord.getLordAPI().getId());
            }
            final SelectItemDialogPlugin plugin = new SelectItemDialogPlugin(message, optionsLords, retValsLords);
            plugin.setPostScript(new Script() {
                @Override
                public void run() {
                    final String selection = plugin.getRetVal();
                    if (selection != null) {
                        // nested dialogs, yay
                        Lord lord = LordController.getLordById(selection);
                        String messageFiefs = "Select whom to transfer fief to " + lord.getLordAPI().getNameString();
                        ArrayList<String> optionsFiefs = new ArrayList<>();
                        ArrayList<String> retValsFiefs = new ArrayList<>();
                        FactionAPI faction = Utils.getRecruitmentFaction();
                        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
                            Lord owner = FiefController.getOwner(market);
                            if (faction.equals(market.getFaction()) && owner == LordController.getPlayerLord()) {
                                optionsFiefs.add(market.getName() + ", owned by " + owner.getLordAPI().getNameString());
                                retValsFiefs.add(market.getId());
                            }
                        }
                        final SelectItemDialogPlugin plugin2 = new SelectItemDialogPlugin(messageFiefs, optionsFiefs, retValsFiefs);
                        plugin2.setPostScript(new Script() {
                            @Override
                            public void run() {
                                String selection2 = plugin2.getRetVal();
                                if (selection2 != null) {

                                    MarketAPI market = FiefController.getMarketByID(selection2);
                                    Lord targetLord = LordController.getLordById(selection);

                                    FiefController.playerTransferFief(targetLord,market);


                                }
                            }
                        });
                        ui.showDialog(null, plugin2);
                    }
                }
            });
            ui.showDialog(null, plugin);
        }
    }

    @Override
    public boolean hasSmallDescription() {
        return false;
    }

    @Override
    public boolean hasLargeDescription() {
        return true;
    }

    @Override
    public String getName() {
        return Utils.getRecruitmentFaction().getDisplayName() + " Realm Laws";
    }

    @Override
    public FactionAPI getFactionForUIColors() {
        return Utils.getRecruitmentFaction();
    }

    @Override
    public String getIcon() {
        return getFactionForUIColors().getCrest();
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add(StringUtil.getString(CATEGORY_UI, "politics_category"));
        return tags;
    }

    @Override
    public IntelSortTier getSortTier() {
        return IntelSortTier.TIER_1;
    }

    public static LawsIntelPlugin getInstance(boolean forceReset) {
        if (instance == null || forceReset) {
            List<IntelInfoPlugin> intel = Global.getSector().getIntelManager().getIntel(LawsIntelPlugin.class);
            if (intel.isEmpty()) {
                instance = new LawsIntelPlugin();
                Global.getSector().getIntelManager().addIntel(instance, true);
            } else {
                if (intel.size() > 1) {
                    throw new IllegalStateException("Should only be one LawsIntelPlugin intel registered");
                }
                instance = (LawsIntelPlugin) intel.get(0);
            }
        }
        return instance;
    }

    public static LawsIntelPlugin getInstance() {
        return getInstance(false);
    }
}
