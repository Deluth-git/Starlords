package starlords.person;

public enum LordAction {
    PATROL(4),
    PATROL_TRANSIT(PATROL),
    FEAST(3),
    FEAST_TRANSIT(FEAST),
    COLLECT_TAXES(3),
    COLLECT_TAXES_TRANSIT(COLLECT_TAXES),
    UPGRADE_FLEET(3),
    UPGRADE_FLEET_TRANSIT(UPGRADE_FLEET),
    DEFEND(3),
    DEFEND_TRANSIT(DEFEND),
    RAID(3),
    RAID_TRANSIT(RAID),
    FOLLOW(1),
    VENTURE(4),
    VENTURE_TRANSIT(VENTURE),
    CAMPAIGN(2),
    IMPRISONED(1),
    RESPAWNING(1);

    public final LordAction base;
    public final int priority; // 1 is highest. lower priority tasks can be cancelled when a pre-empting event is triggered

    LordAction(LordAction base) {
        this.base = base;
        this.priority = this.base.priority;
    }

    LordAction(int priority) {
        this.base = this;
        this.priority = priority;
    }

    public boolean isTransit() {
        return this.base != this;
    }

    public static LordAction base(LordAction action) {
        if (action == null) return null;
        return action.base;
    }
}
