package Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.applet.*;

class MainInterface extends Applet {

}

public class UserInterface implements Runnable {

    Data data;

    public UserInterface(Data datat) {
        data = datat;
    }

    @Override
    public void run() {

    }
}
