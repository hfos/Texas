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
  static int status,roomId;
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
    status = 0;

    OUT:
    while(true){
      switch (status) {
        case 0:
          Hall.run();
          break;
        case 1:
        case 2:
          Room.run(roomId);
          break;
        case 3:
          Game.run();
        case 4:
          break OUT;
      }
    }

    try {
      out.close();
      in.close();
      socket.close();
    } catch(IOException e) {}
  }
}
class Hall{
  void run(){
    while(Client.status==0) {

    }
  }
}