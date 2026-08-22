package starlords.generator.support;

import com.fs.starfarer.api.characters.PersonAPI;
import starlords.listeners.LordGeneratorListener_base;

public class LifeAndDeath_LordGeneratorListiner extends LordGeneratorListener_base {
    public PersonAPI person;
    @Override
    public void editLordPerson(PersonAPI lord) {
        person = lord;
    }
}
