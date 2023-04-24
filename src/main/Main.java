package main;

import levelscript.DuplicateTriggerException;
import levelscript.InvalidFieldsException;

import java.awt.*;

public class Main {
    public static int width = 600;
    public static int height = 500;

    public static void main(String[] args) throws DuplicateTriggerException, InvalidFieldsException
    {
        EditorPanel panel = new EditorPanel();
        panel.setPreferredSize(new Dimension(width, height));

        panel.pack();
        panel.toFront();
        panel.setVisible(true);
    }
}
