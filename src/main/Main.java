package main;

import java.awt.Taskbar;
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

        String os= (System.getProperty("os.name").toLowerCase());
        if (os.contains("mac"))
        {
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", panel.getTitle());
        }

        final Taskbar taskbar = Taskbar.getTaskbar();
        Image img = panel.getIconImage();

        try {
            //set icon for mac os (and other systems which do support this method)
            taskbar.setIconImage(img);
        } catch (final UnsupportedOperationException e) {
            System.out.println("The os does not support: 'taskbar.setIconImage'");
        } catch (final SecurityException e) {
            System.out.println("There was a security exception for: 'taskbar.setIconImage'");
        }
    }
}
