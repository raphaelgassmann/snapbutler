package ch.raphigassmann.snapbutler;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ch.raphigassmann.snapbutler.view.Login;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Login login = Login.waitForStartUpTest();
    }
    public static void main(String[] args) {
        new Thread() {
            @Override
            public void run(){
                Application.launch(Login.class);
            }
        }.start();
    }
}
