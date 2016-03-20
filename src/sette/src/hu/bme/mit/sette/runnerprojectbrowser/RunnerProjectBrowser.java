package hu.bme.mit.sette.runnerprojectbrowser;

import java.io.IOException;
import java.util.Locale;

import com.google.common.io.Resources;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class RunnerProjectBrowser extends Application {

    public static void main(String[] args) {
        try {
            Locale.setDefault(new Locale("en", "GB"));
        } catch (Exception ex) {
            Locale.setDefault(Locale.ENGLISH);
        }
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Parent root;
        try {
            root = (GridPane) FXMLLoader
                    .load(Resources.getResource("RunnerProjectBrowser.fxml"));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("SETTE Runner Project Browser");
        primaryStage.show();
    }
}
