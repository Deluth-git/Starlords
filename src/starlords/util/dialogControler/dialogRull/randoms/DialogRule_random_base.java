package starlords.util.dialogControler.dialogRull.randoms;

import starlords.person.Lord;

public class DialogRule_random_base {
    protected double multi;
    public DialogRule_random_base(double multi){
        this.multi=multi;
    }
    public double value(Lord lord){
        return multi;
    }
}
