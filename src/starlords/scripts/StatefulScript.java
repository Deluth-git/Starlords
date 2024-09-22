package starlords.scripts;

import com.fs.starfarer.api.Script;

public abstract class StatefulScript implements Script {
    Object data;

    public StatefulScript(Object data) {
        this.data = data;
    }
}
