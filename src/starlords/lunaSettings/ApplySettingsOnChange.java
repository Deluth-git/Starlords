package starlords.lunaSettings;

import lunalib.lunaSettings.LunaSettingsListener;
import org.jetbrains.annotations.NotNull;

public class ApplySettingsOnChange implements LunaSettingsListener {
    @Override
    public void settingsChanged(@NotNull String s) {
        //if (!s.equals("starlords")) return;
        StoredSettings.getSettings();
        //apply all settings here.
    }
}
