package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    public static int width = 580;
    public static int height = 450;

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("gui.fxml"));
        primaryStage.setTitle("PLSE - Pokémon Level Script Editor");
        primaryStage.setMinWidth(width); primaryStage.setMinHeight(height);
        Scene s = new Scene(root, width, height);
        //s.getStylesheets().add("resources/dark-theme.css");
        primaryStage.setScene(s);
        primaryStage.show();

    }


    public static void main(String[] args) {
        launch(args);
    }
}
