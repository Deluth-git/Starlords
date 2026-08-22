package starlords.util.dialogControler.dialogRull;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.controllers.PoliticsController;
import starlords.faction.LawProposal;
import starlords.person.Lord;
import starlords.util.Utils;
import starlords.util.dialogControler.dialogRull.randoms.DialogRule_random_base;
import starlords.util.dialogControler.dialogRull.randoms.DialogRule_random_opinionOfCurrProposal;
import starlords.util.dialogControler.dialogRull.randoms.DialogRule_random_opinionOfPlayerProposal;
import starlords.util.dialogControler.dialogRull.randoms.DialogRule_random_playerLordRelation;
import starlords.util.dialogControler.dialogValues.DialogValuesList;

import java.util.ArrayList;
import java.util.Iterator;

public class DialogRule_random extends DialogRule_Base {
    DialogValuesList range;
    DialogValuesList base;
    @SneakyThrows
    public DialogRule_random(JSONObject jsonObject) {
        range = new DialogValuesList(jsonObject,"range");
        base = new DialogValuesList(jsonObject,"value");
    }
    @Override
    public boolean condition(Lord lord,Lord targetLord, MarketAPI targetMarket) {
        int random = Utils.rand.nextInt(range.getValue(lord, targetLord,targetMarket));
        int value = base.getValue(lord, targetLord,targetMarket);
        return random <= value;
    }
}
