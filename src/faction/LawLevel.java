package faction;

public enum LawLevel {
    VERY_LOW("V. Low"),
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    VERY_HIGH("V. High");

    public final String displayName;

    LawLevel(String s) {
        displayName = s;
    }
}
