package nl.roboteamtwente.autoref.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class AutoRefUi extends Application {
    private AutoRefController controller;

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/auto_ref.fxml")));
        Parent root = fxmlLoader.load();
        controller = fxmlLoader.getController();

        if(!getParameters().getUnnamed().contains("--cli"))
        {
            primaryStage.setTitle("RoboTeam Twente: AutoRef");
            primaryStage.setScene(new Scene(root, 640, 480));
            primaryStage.show();
        }

        controller.start(getParameters());
    }

    @Override
    public void stop() throws Exception {
        controller.stop();
    }
}
