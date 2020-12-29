package levelscript;

public class InvalidFieldsException extends Exception {
    public InvalidFieldsException(String fields) {
        LSTrigger.customAlert("Only integers in the range 0-65535 are allowed.\nInvalid field(s):" + fields);
    }
}
