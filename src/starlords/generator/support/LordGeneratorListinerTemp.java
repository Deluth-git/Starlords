package starlords.generator.support;

import starlords.listeners.LordGeneratorListener_base;
import starlords.person.PosdoLordTemplate;

public class LordGeneratorListinerTemp extends LordGeneratorListener_base {
    public int tier = 0;
    public String fief=null;
    @Override
    public void editLord(PosdoLordTemplate lord) {
        lord.ranking=tier;
        lord.fief=fief;
    }
}
