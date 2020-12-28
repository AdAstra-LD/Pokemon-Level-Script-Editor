package levelscript;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Objects;

public abstract class LSTrigger {
    public static final int VARIABLEVALUE = 1;
    public static final int MAPCHANGE = 2;
    public static final int SCREENRESET = 3;
    public static final int LOADGAME = 4;

    private int type;
    private int scriptTriggered;

    public LSTrigger(int type, int scriptTriggered) {
        this.type = type;
        this.scriptTriggered = scriptTriggered;
    }

    public static final void customAlert(String contentText) {
        Alert a = new Alert(Alert.AlertType.ERROR, contentText, ButtonType.OK);
        a.show();
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getScriptTriggered() {
        return scriptTriggered;
    }

    public void setScriptTriggered(int scriptTriggered) {
        this.scriptTriggered = scriptTriggered;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LSTrigger)) return false;
        LSTrigger lst = (LSTrigger) o;
        return this.type == lst.getType() && this.scriptTriggered == lst.getScriptTriggered();
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, scriptTriggered);
    }

    @Override
    public String toString() {
        return "Starts Script " + getScriptTriggered();
    }
}
