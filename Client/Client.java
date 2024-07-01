package Client;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;


public class Client {
  final static String SERVER_IP = "localhost";
  final static int SERVER_PORT = 8848;
  static Socket socket;
  static DataOutputStream out;
  static DataInputStream in;
  static Data data;
  public static void main(String[] args){
    try {
      socket = new Socket(SERVER_IP,SERVER_PORT);
      out = new DataOutputStream(socket.getOutputStream());
      in = new DataInputStream(socket.getInputStream());
    } catch(IOException e){
      System.err.println("Cannot connect to the server");
      return;
    }
    System.out.println("connected");
    data.status = 0;
    UserInterface ui = new UserInterface(data);
    Thread UIThread = new Thread(ui);
    UIThread.start();

    

    try {
      out.close();
      in.close();
      socket.close();
    } catch(IOException e) {}
  }
}