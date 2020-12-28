package levelscript;

public class MapScreenLoadTrigger extends LSTrigger implements Comparable<MapScreenLoadTrigger> {
    public static final int triggerSizeInBytes = 5;

    public MapScreenLoadTrigger(int type, int scriptTriggered) {
        super(type, scriptTriggered);
    }

    @Override
    public String toString() {
        String message = super.toString();
        switch (getType()) {
            case LSTrigger.MAPCHANGE:
                message += " upon entering the LS map.";
                break;
            case LSTrigger.SCREENRESET:
                message += " when a fadescreen happens in the LS map.";
                break;
            case LSTrigger.LOADGAME:G:
                message += " when the game resumes in the LS map.";
                break;
        }
        return message;
    }

    @Override
    public int compareTo(MapScreenLoadTrigger other) {
        int i;

        i = -Integer.compare(this.getType(), other.getType());
        if (i != 0) return i;

        i = Integer.compare(this.getScriptTriggered(), other.getScriptTriggered());
        return i;
    }
}
