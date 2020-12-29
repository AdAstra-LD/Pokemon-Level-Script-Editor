package levelscript;

public class DuplicateTriggerException extends Exception {
    public DuplicateTriggerException() {
        LSTrigger.customAlert("The trigger you're trying to add already exists.");
    }
}
