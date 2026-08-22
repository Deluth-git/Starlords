package starlords.util.dialogControler.dialogRull;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import starlords.controllers.EventController;
import starlords.lunaSettings.StoredSettings;
import starlords.person.Lord;
import starlords.person.LordAction;
import starlords.person.LordEvent;

import java.util.ArrayList;
import java.util.Iterator;

public class DialogRule_currentAction extends DialogRule_Base{
    ArrayList<CurrentAction_base> actions = new ArrayList<>();
    @SneakyThrows
    public DialogRule_currentAction(JSONObject json, String key){
        json = json.getJSONObject(key);
        for (Iterator it = json.keys(); it.hasNext();) {
            String key2 = (String) it.next();
            boolean has = json.getBoolean(key2);
            switch (key2) {
                case "IS_UPGRADING":
                    actions.add(new CurrentAction_IS_UPGRADING(has));
                    break;
                case "IS_ON_PATROL":
                    actions.add(new CurrentAction_IS_ON_PATROL(has));
                    break;
                case "IS_RAIDING":
                    actions.add(new CurrentAction_IS_RAIDING(has));
                    break;
                case "IS_FOLLOWING_PLAYER":
                    actions.add(new CurrentAction_IS_FOLLOWING_PLAYER(has));
                    break;
                case "IS_FOLLOWING":
                    actions.add(new CurrentAction_IS_FOLLOWING(has));
                    break;
                case "IS_ON_CAMPAIGN":
                    actions.add(new CurrentAction_IS_ON_CAMPAIGN(has));
                    break;
                case "IS_ORGANIZING_CAMPAIGN":
                    actions.add(new CurrentAction_IS_ORGANIZING_CAMPAIGN(has));
                    break;
                case "IS_FEASTING":
                    actions.add(new CurrentAction_IS_FEASTING(has));
                    break;
                case "IS_ORGANIZING_FEAST":
                    actions.add(new CurrentAction_IS_ORGANIZING_FEAST(has));
                    break;
                case "IS_COLLECTING_TAXES":
                    actions.add(new CurrentAction_IS_COLLECTING_TAXES(has));
                    break;
                case "IS_DEFENDING":
                    actions.add(new CurrentAction_IS_DEFENDING(has));
                    break;
                case "IS_IMPRISONED":
                    actions.add(new CurrentAction_IS_IMPRISONED(has));
                    break;
                case "IS_RESPAWNING":
                    actions.add(new CurrentAction_IS_RESPAWNING(has));
                    break;
                case "IS_ON_VENTURE":
                    actions.add(new CurrentAction_IS_ON_VENTURE(has));
                    break;
            }
        }
    }

    @Override
    public boolean condition(Lord lord, Lord targetLord, MarketAPI targetMarket) {
        for (CurrentAction_base a : actions){
            if (!a.condition(lord,targetLord)) return false;
        }
        return true;
    }
}
class CurrentAction_base {
    protected boolean has;
    public CurrentAction_base(boolean bol){
        has=bol;
    }
    public boolean condition(Lord lord, Lord targetLord){
        return isCurAction(lord, targetLord) == has;
    }
    protected boolean isCurAction(Lord lord, Lord targetLord){
        return false;
    }
}
/*
 *       IS_UPGRADING: boolean
 *       IS_ON_PATROL: boolean
 *       IS_RAIDING: boolean
 *       IS_FOLLOWING_PLAYER: boolean
 *       IS_FOLLOWING: boolean
 *       IS_ON_CAMPAIGN: boolean
 *       IS_ORGANIZING_CAMPAIGN: boolean
 *       IS_ORGANIZING_FEAST: boolean
 *       IS_FEASTING: boolean
 *       IS_COLLECTING_TAXES: boolean
 *       IS_DEFENDING: boolean
 *       IS_IMPRISONED: boolean
 *       IS_RESPAWNING: boolean
 *       IS_ON_VENTURE: boolean
 * */
class CurrentAction_IS_UPGRADING extends CurrentAction_base {
    public CurrentAction_IS_UPGRADING(boolean bol) {
        super(bol);
    }
    @Override
    protected boolean isCurAction(Lord lord, Lord targetLord) {
        return lord.getCurrAction() == LordAction.UPGRADE_FLEET;
    }
}
class CurrentAction_IS_ON_PATROL extends CurrentAction_base {
    public CurrentAction_IS_ON_PATROL(boolean bol) {
        super(bol);
    }
    @Override
    protected boolean isCurAction(Lord lord, Lord targetLord) {
        return lord.getCurrAction() == LordAction.PATROL;
    }
}
class CurrentAction_IS_RAIDING extends CurrentAction_base {
    public CurrentAction_IS_RAIDING(boolean bol) {
        super(bol);
    }
    @Override
    protected boolean isCurAction(Lord lord, Lord targetLord) {
        return lord.getCurrAction() == LordAction.RAID;
    }
}
class CurrentAction_IS_FOLLOWING_PLAYER extends CurrentAction_base {
    public CurrentAction_IS_FOLLOWING_PLAYER(boolean bol) {
        super(bol);
    }
    @Override
    protected boolean isCurAction(Lord lord, Lord targetLord) {
        return lord.getCurrAction() == LordAction.FOLLOW && Global.getSector().getPlayerFleet().equals(lord.getTarget());
    }
}
class CurrentAction_IS_FOLLOWING extends CurrentAction_base {
    public CurrentAction_IS_FOLLOWING(boolean bol) {
        super(bol);
    }
    @Override
    protected boolean isCurAction(Lord lord, Lord targetLord) {
        return lord.getCurrAction() == LordAction.FOLLOW;
    }
}
class CurrentAction_IS_ON_CAMPAIGN extends CurrentAction_base {
    public CurrentAction_IS_ON_CAMPAIGN(boolean bol) {
        super(bol);
    }
    @Override
    protected boolean isCurAction(Lord lord, Lord targetLord) {
        return lord.getCurrAction() == LordAction.CAMPAIGN;
    }
}
class CurrentAction_IS_ORGANIZING_CAMPAIGN extends CurrentAction_base {
    public CurrentAction_IS_ORGANIZING_CAMPAIGN(boolean bol) {
        super(bol);
    }
    @Override
    protected boolean isCurAction(Lord lord, Lord targetLord) {
        LordEvent campaign = EventController.getCurrentCampaign(lord.getLordAPI().getFaction());
        return (campaign != null && campaign.getOriginator().equals(lord));
    }
}
class CurrentAction_IS_FEASTING extends CurrentAction_base {
    public CurrentAction_IS_FEASTING(boolean bol) {
        super(bol);
    }
    @Override
    protected boolean isCurAction(Lord lord, Lord targetLord) {
        return lord.getCurrAction() == LordAction.FEAST;
    }
}
class CurrentAction_IS_ORGANIZING_FEAST extends CurrentAction_base {
    public CurrentAction_IS_ORGANIZING_FEAST(boolean bol) {
        super(bol);
    }
    @Override
    protected boolean isCurAction(Lord lord, Lord targetLord) {
        boolean check = (lord.getCurrAction() == LordAction.FEAST);
        if (!check) return false;
        if (EventController.getCurrentFeast(lord.getLordAPI().getFaction()) == null || EventController.getCurrentFeast(lord.getLordAPI().getFaction()).getOriginator() == null) return false;
        if (EventController.getCurrentFeast(lord.getLordAPI().getFaction()) == null || EventController.getCurrentFeast(lord.getLordAPI().getFaction()).getOriginator().equals(lord)) return true;
        return false;
    }
}
class CurrentAction_IS_COLLECTING_TAXES extends CurrentAction_base {
    public CurrentAction_IS_COLLECTING_TAXES(boolean bol) {
        super(bol);
    }
    @Override
    protected boolean isCurAction(Lord lord, Lord targetLord) {
        return lord.getCurrAction() == LordAction.COLLECT_TAXES;
    }
}
class CurrentAction_IS_DEFENDING extends CurrentAction_base {
    public CurrentAction_IS_DEFENDING(boolean bol) {
        super(bol);
    }
    @Override
    protected boolean isCurAction(Lord lord, Lord targetLord) {
        return lord.getCurrAction() == LordAction.DEFEND;
    }
}
class CurrentAction_IS_IMPRISONED extends CurrentAction_base {
    public CurrentAction_IS_IMPRISONED(boolean bol) {
        super(bol);
    }
    @Override
    protected boolean isCurAction(Lord lord, Lord targetLord) {
        return lord.getCurrAction() == LordAction.IMPRISONED;
    }
}
class CurrentAction_IS_RESPAWNING extends CurrentAction_base {
    public CurrentAction_IS_RESPAWNING(boolean bol) {
        super(bol);
    }
    @Override
    protected boolean isCurAction(Lord lord, Lord targetLord) {
        return lord.getCurrAction() == LordAction.RESPAWNING;
    }
}
class CurrentAction_IS_ON_VENTURE extends CurrentAction_base {
    public CurrentAction_IS_ON_VENTURE(boolean bol) {
        super(bol);
    }
    @Override
    protected boolean isCurAction(Lord lord, Lord targetLord) {
        return lord.getCurrAction() == LordAction.VENTURE;
    }
}
