package util;

import java.awt.*;

public class Constants {
    // How much wealth each lord gains as a baseline per month.
    // Lords are intended to gain most of their wealth through trade/governance,
    // this is just to make sure they can build something.
    public static final int LORD_MONTHLY_INCOME = 10000;
    // percent chance lord is captured when defeated in combat against other lords
    public static final int LORD_CAPTURE_CHANCE = 70;

    public static final Color LIGHT_RED = new Color(194, 29, 29);
    public static final Color LIGHT_GREEN = new Color(29, 194, 29);
    public static final Color DARK_GOLD = new Color(119, 80, 0);

    public static final String MOD_ID = "lords";
    public static final long ONE_DAY = 24L * 60 * 60 * 1000;
    public static final boolean DEBUG_MODE = true;

    // Keys for persistent data
    public static final String LORD_TABLE_KEY = "starlords_lords";
    public static final String FIEF_TABLE_KEY = "starlords_fiefs";

    // string categories
    public static final String CATEGORY_UI = "starlords_ui";
    public static final String CATEGORY_TITLES = "starlords_title";

    // constants for defection
    public static final String CLAIM_UPSTANDING = "claim_upstanding";
    public static final String CLAIM_MARTIAL = "claim_martial";
    public static final String CLAIM_CALCULATING = "claim_calculating";
    public static final String CLAIM_QUARRELSOME = "claim_quarrelsome";
    public static final int COMPLETELY_UNJUSTIFIED = -10;
    public static final int SOMEWHAT_JUSTIFIED = 5;
    public static final int FULLY_JUSTIFIED = 10;
    public static final int SMALL_BRIBE = 500000;
    public static final int LARGE_BRIBE = 2000000;

}
