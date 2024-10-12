package starlords.util;

import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;

// for dynamic words relating to gender
public class GenderUtils {

    public static String sirOrMaam(PersonAPI person, boolean caps) {
        if (caps) {
            return person.getGender() == FullName.Gender.FEMALE ? "Ma'am" : "Sir";
        }
        return person.getGender() == FullName.Gender.FEMALE ? "ma'am" : "sir";
    }

    public static String heOrShe(PersonAPI person, boolean caps) {
        if (caps) {
            return person.getGender() == FullName.Gender.FEMALE ? "She" : "He";
        }
        return person.getGender() == FullName.Gender.FEMALE ? "she" : "he";
    }

    public static String lordOrLady(PersonAPI person, boolean caps) {
        if (caps) {
            return person.getGender() == FullName.Gender.FEMALE ? "Lady" : "Lord";
        }
        return person.getGender() == FullName.Gender.FEMALE ? "lady" : "lord";
    }

    public static String manOrWoman(PersonAPI person, boolean caps) {
        if (caps) {
            return person.getGender() == FullName.Gender.FEMALE ? "Woman" : "Man";
        }
        return person.getGender() == FullName.Gender.FEMALE ? "woman" : "man";
    }

    public static String hisOrHer(PersonAPI person, boolean caps) {
        if (caps) {
            return person.getGender() == FullName.Gender.FEMALE ? "Her" : "His";
        }
        return person.getGender() == FullName.Gender.FEMALE ? "her" : "his";
    }

    public static String husbandOrWife(PersonAPI person, boolean caps) {
        if (caps) {
            return person.getGender() == FullName.Gender.FEMALE ? "Wife" : "Husband";
        }
        return person.getGender() == FullName.Gender.FEMALE ? "wife" : "husband";
    }
}
