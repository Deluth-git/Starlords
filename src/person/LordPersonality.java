package person;

public enum LordPersonality {
    UPSTANDING(4),
    MARTIAL(6),
    CALCULATING(3),
    QUARRELSOME(1);

    public final int releaseRepGain;

    LordPersonality(int releaseRepGain) {
        this.releaseRepGain = releaseRepGain;
    }
}
