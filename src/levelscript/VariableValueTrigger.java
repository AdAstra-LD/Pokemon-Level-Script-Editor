package levelscript;

import java.util.Objects;

public class VariableValueTrigger extends LSTrigger implements Comparable<VariableValueTrigger> {
    private int variableToWatch;
    private int expectedValue;

    public VariableValueTrigger(int ID, int variableToWatch, int expectedValue) {
        super(VARIABLEVALUE, ID);
        this.variableToWatch = variableToWatch;
        this.expectedValue = expectedValue;
    }

    public int getVariableToWatch() {
        return variableToWatch;
    }

    public void setVariableToWatch(int variableToWatch) {
        this.variableToWatch = variableToWatch;
    }

    public int getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(int expectedValue) {
        this.expectedValue = expectedValue;
    }

    @Override
    public String toString() {
        return super.toString() + " when Var " + variableToWatch + " == " + expectedValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VariableValueTrigger)) return false;
        VariableValueTrigger that = (VariableValueTrigger) o;
        return this.getType() == that.getType()
                && this.getScriptTriggered() == that.getScriptTriggered()
                && this.variableToWatch == that.variableToWatch
                && this.expectedValue == that.expectedValue;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getType(), this.getScriptTriggered(), variableToWatch, expectedValue);
    }

    @Override
    public int compareTo(VariableValueTrigger other) {
        int i;

        i = -Integer.compare(this.getType(), other.getType());
        if (i != 0) return i;

        i = Integer.compare(this.getScriptTriggered(), other.getScriptTriggered());
        if (i != 0) return i;

        i = -Integer.compare(this.variableToWatch, other.variableToWatch);
        if (i != 0) return i;

        i = Integer.compare(this.expectedValue, other.expectedValue);
        return i;
    }
}
