package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {
    public static int width = 600;
    public static int height = 500;

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader l = new FXMLLoader(getClass().getResource("gui.fxml"));
        Parent root = l.load();
        Controller main = l.getController();

        primaryStage.setTitle("Pokémon Level Script Editor 1.2");
        primaryStage.setMinWidth(width); primaryStage.setMinHeight(height);
        Scene s = new Scene(root, width, height);

        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/resources/LSE.png")));
        //s.getStylesheets().add("resources/dark-theme.css");
        primaryStage.setScene(s);
        primaryStage.show();
        main.setStage(primaryStage);

    }


    public static void main(String[] args) {
        launch(args);
    }
}
