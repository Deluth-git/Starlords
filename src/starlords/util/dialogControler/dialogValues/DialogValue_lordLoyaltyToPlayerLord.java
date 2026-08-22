package starlords.util.dialogControler.dialogValues;

import org.json.JSONObject;
import starlords.controllers.RelationController;
import starlords.person.Lord;
import starlords.util.Utils;

public class DialogValue_lordLoyaltyToPlayerLord extends DialogValue_base{
    public DialogValue_lordLoyaltyToPlayerLord(JSONObject json, String key) {
        super(json, key);
    }

    @Override
    public int value(Lord lord, Lord targetLord) {
        return RelationController.getLoyalty(lord, Utils.getRecruitmentFaction().getId());
    }
}
