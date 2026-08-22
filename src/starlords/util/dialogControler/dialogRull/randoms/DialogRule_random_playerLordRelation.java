package starlords.util.dialogControler.dialogRull.randoms;

import starlords.person.Lord;

public class DialogRule_random_playerLordRelation extends DialogRule_random_base{
    public DialogRule_random_playerLordRelation(double multi) {
        super(multi);
    }
    @Override
    public double value(Lord lord) {
        int value = lord.getPlayerRel();
        return super.value(lord) * value;
    }
}
