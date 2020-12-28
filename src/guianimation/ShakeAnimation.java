package guianimation;

import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

public class ShakeAnimation {
    private static TranslateTransition tt;
    public final static int DEFAULT = 20;

    public static void play (Node node) {

        tt = new TranslateTransition(Duration.millis(75));
        tt.setFromX(0);
        tt.setToX(DEFAULT);

        tt.setByX(200f);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);

        tt.setNode(node);
        tt.play();
    }

    public static void play (Node node, int toX) {

        tt = new TranslateTransition(Duration.millis(75));
        tt.setFromX(0);
        tt.setToX(toX);

        tt.setByX(200f);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);

        tt.setNode(node);
        tt.play();
    }
}
