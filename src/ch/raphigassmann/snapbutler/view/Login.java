package ch.raphigassmann.snapbutler.view;

import ch.raphigassmann.snapbutler.control.Manager;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.concurrent.CountDownLatch;

/**
 * Created by raphaelgassmann on 12.09.16.
 */
public class Login extends Application {

    // ======== StartUp Prozedure ========
    public static final CountDownLatch latch = new CountDownLatch(1);
    public static Login startUpTest = null;



    public static Login waitForStartUpTest() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return startUpTest;
    }

    public static void setStartUpTest(Login startUpTest0) {
        startUpTest = startUpTest0;
        latch.countDown();
    }

    public Login() {
        setStartUpTest(this);
    }

    //Just for StartUp Test
    //public void printStatus() {System.out.println("StartUp completed");}

    // ======== StartUp Prozedure finished ========

    // ======== JavaFX Components ========

    Button btnLogin;
    javafx.scene.control.TextField txtViSrv = new javafx.scene.control.TextField();
    javafx.scene.control.TextField txtUsername = new javafx.scene.control.TextField();
    PasswordField txtPassword = new PasswordField();
    StackPane layout = new StackPane(); //Method ConnectionStart() need knowledge about this
    Scene scene = new Scene(layout, 900, 420); //Method ConnectionStart() need knowledge about this

    // ======== JavaFX Scene ========
    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("SnapButler - Login");
        primaryStage.setResizable(false);

        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("ico.png")));

        btnLogin = new Button();
        btnLogin.setText("Login");
        btnLogin.setPrefSize(300, 25);
        btnLogin.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent event) {
                ConnectionStart();
            }
        });

        txtViSrv.setMaxWidth(300);
        txtViSrv.setPromptText("vCenter DNS / vCenter IP-Adress");

        txtUsername.setMaxWidth(300);
        txtUsername.setPromptText("Username");

        txtPassword.setMaxWidth(300);
        txtPassword.setPromptText("Password");
        txtPassword.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                ConnectionStart();
            }
        });

        //Label Info
        Label lblInfo = new Label();
        lblInfo.setText("Info");
        lblInfo.setTextFill(Color.WHITE);
        lblInfo.underlineProperty().set(true);
        lblInfo.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                //TODO: Write some Info text
                //Messeage Box - SnapButler Info
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("SnapButler Information");
                alert.setHeaderText("SnapButler Information");
                alert.setContentText("Please visit: https://snapbutler.raphigassmann.ch");
                alert.showAndWait();
            }
        });

        VBox vbLoginComponents = new VBox();
        vbLoginComponents.setAlignment(Pos.CENTER_RIGHT);
        vbLoginComponents.setPadding(new Insets(0, 50, 0, 0));
        vbLoginComponents.getChildren().addAll(txtViSrv, txtUsername, txtPassword, btnLogin, lblInfo);



        layout.getChildren().add(vbLoginComponents);
        layout.setId("loginlayout"); // Set ID for CSS Stylesheet


        String css =this.getClass().getResource("Login.css").toExternalForm();
        scene.getStylesheets().add(css); // Add CSS to JavaFX Class

        btnLogin.requestFocus(); // Request Focus on Button
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Connect to ViServer. Collect all needed Data and give over to Manager Class.
     */
    public void ConnectionStart(){
        //Check if all needed Data is filed out
        if (txtViSrv.getText().isEmpty() || txtUsername.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning Dialog");
            alert.setHeaderText("Please provide Login Data");
            alert.setContentText("Please provide at least a VIServer and Username.");
            alert.show();
        } else {
            //Init Manager and do Connection to VIServer
            Manager manager = new Manager();
            manager.connectViSrv(txtViSrv.getText(), txtUsername.getText(), txtPassword.getText());

            if (manager.getManagerConnectionState() == true){
                Stage stageOfLogin = (Stage) scene.getWindow(); //Get Stage of actual scene
                stageOfLogin.close(); //Close Stage Login

                Stage stage;
                stage = new Stage();
                App app = new App();
                try {
                    app.startup(stage, manager);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }else{
                //Messeage Box/Alert - Connection unsucessful
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Connection Error");
                alert.setHeaderText("Connection was unsucessful.");
                alert.setContentText("Please check your login details and vCenter Inventory Service");
                alert.showAndWait();
            }
        }
    }

}
