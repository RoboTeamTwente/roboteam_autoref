package nl.roboteamtwente.autoref.ui;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import nl.roboteamtwente.autoref.SSLAutoRef;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class AutoRefController implements Initializable {
    private SSLAutoRef sslAutoRef;
    private boolean isHeadless;

    @FXML
    public ComboBox<String> modeBox;

    @FXML
    public ListView<TextFlow> logList;

    @FXML
    public GameCanvas canvas;

    @FXML
    public Button clearButton;

    @FXML
    public Label worldStatus;

    @FXML
    public Label gcStatus;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        sslAutoRef = new SSLAutoRef();

        canvas.setSslAutoRef(sslAutoRef);

        sslAutoRef.setOnViolation((violation) -> {
            double time = sslAutoRef.getReferee().getGame().getTime();
            String timeString = String.format("%d:%05.2f", (int) (time / 60), time % 60);
            System.out.println("[" + timeString + "] " + violation);

            Text timeText = new Text("[" + timeString + "] ");
            timeText.setStyle("-fx-font-weight: bold");
            if(!isHeadless){
                Platform.runLater(() -> {
                    logList.getItems().add(new TextFlow(timeText, new Text(violation.toString())));
                    logList.scrollTo(logList.getItems().size() - 1);
                });
            }
        });

        modeBox.getItems().addAll("No GameController Connection", "Automatically Connect");

        modeBox.setOnAction((event) -> {
            sslAutoRef.setAutoConnect(Objects.equals(modeBox.getValue(), "Automatically Connect"));
        });

        clearButton.setOnAction((event) -> {
            logList.getItems().clear();
        });

        AnimationTimer anim = new AnimationTimer() {
            public void handle(long now) {
                worldStatus.setTextFill(sslAutoRef.isWorldConnected() ? Color.GREEN : Color.RED);
                gcStatus.setTextFill(sslAutoRef.isGCConnected() ? Color.GREEN : Color.RED);
                canvas.redraw();
            }
        };
        anim.start();
    }

    public void initialize_headless() {
        sslAutoRef = new SSLAutoRef();

        sslAutoRef.setOnViolation((violation) -> {
            double time = sslAutoRef.getReferee().getGame().getTime();
            String timeString = String.format("%d:%05.2f", (int) (time / 60), time % 60);
            System.out.println("[" + timeString + "] " + violation);

            Text timeText = new Text("[" + timeString + "] ");
            timeText.setStyle("-fx-font-weight: bold");
        });
    }

    public void start(String ipWorld, String portWorld, String ipGameController, 
                        String portGameController, boolean noGC, boolean headless) {
        try {
            setHeadless(headless);
            if(!isHeadless){
                modeBox.setValue(noGC ? "No GameController Connection" : "Automatically Connect");
            }
            sslAutoRef.setAutoConnect(!noGC);
            sslAutoRef.start(ipWorld, ipGameController,
                            Integer.valueOf(portWorld),
                            Integer.valueOf(portGameController));

        } catch (NumberFormatException e) {
            System.err.println("Failed to parse port program argument.");
            System.exit(1);
        }
    }

    public void stop() {
        sslAutoRef.stop();
    }

    private void setHeadless(boolean headless){
        isHeadless=headless;
    }
}
