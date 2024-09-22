package starlords.faction;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import starlords.controllers.LordController;
import lombok.Getter;
import lombok.Setter;
import starlords.person.Lord;
import starlords.util.StringUtil;

import static starlords.util.Constants.CATEGORY_UI;


@Getter
@Setter
public class Lawset {
    public enum LawType {
        CROWN_AUTHORITY(StringUtil.getString(CATEGORY_UI, "laws_crown_authority"), 3, 2, 0, 2, 4),
        NOBLE_AUTHORITY(StringUtil.getString(CATEGORY_UI, "laws_noble_authority"), 3, 2, 2, 2, 1),
        TRADE_LAW(StringUtil.getString(CATEGORY_UI, "laws_trade_law"), 2, 4, 0, 2, 2),
        FEAST_LAW(StringUtil.getString(CATEGORY_UI, "laws_feast_law"), 3, 3, 1, 0, 2),
        APPOINT_MARSHAL,
        AWARD_FIEF,
        DECLARE_WAR,
        SUE_FOR_PEACE,
        REVOKE_FIEF,
        CHANGE_RANK,
        EXILE_LORD;

        public String lawName;
        public int[] pref; // preferred level for law for upstanding, martial, calculating, quarrelsome, and ruler

        LawType() {}

        LawType(String s, int u, int m, int c, int q, int r) {
            pref = new int[]{u, m, c, q, r};
            lawName = s;
        }
    }

    private String factionId;
    private String marshal;
    private LawLevel crownAuthority;
    private LawLevel nobleAuthority; // power of high-rank nobles
    private LawLevel tradeFavor;
    private LawLevel feastLaw;
    private MarketAPI fiefAward;

    public Lawset(FactionAPI faction) {
        factionId = faction.getId();
        crownAuthority = LawLevel.MEDIUM;
        nobleAuthority = LawLevel.MEDIUM;
        tradeFavor = LawLevel.MEDIUM;
        feastLaw = LawLevel.MEDIUM;
        marshal = null;
        for (Lord lord : LordController.getLordsList()) {
            if (faction.equals(lord.getLordAPI().getFaction()) && lord.getRanking() == 2) {
                marshal = lord.getLordAPI().getId();
            }
        }
    }

    public LawLevel getLawLevel(LawType type) {
        switch(type) {
            case CROWN_AUTHORITY:
                return crownAuthority;
            case NOBLE_AUTHORITY:
                return nobleAuthority;
            case TRADE_LAW:
                return tradeFavor;
            case FEAST_LAW:
                return feastLaw;
        }
        return null;
    }
}
