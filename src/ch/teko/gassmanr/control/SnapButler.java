package ch.teko.gassmanr.control;

import ch.teko.gassmanr.view.Login;
import javafx.application.Application;

/**
 * Created by raphaelgassmann on 12.09.16.
 */
public class SnapButler{


    public static void main(String[] args) {

        //Init login scene in seperate thread to not block while initialize.
        new Thread() {
            @Override
            public void run(){
                //javafx.application.Application.launch(Login.class);
                Application.launch(Login.class);
                //new Login = Login();
            }
        }.start();

        Login login = Login.waitForStartUpTest();
        //login.printStatus(); //Just for startup test


    }
}
