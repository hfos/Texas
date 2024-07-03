package Client;
import java.io.*;
import java.net.*;
import java.util.ArrayList;


public class Client {
  final static String SERVER_IP = "localhost";
  final static int SERVER_PORT = 8848;
  static Socket socket;
  static DataOutputStream webOut;
  static DataInputStream webIn;
  static Data data;
  static DataInputStream userIn;
  public static void main(String[] args){
    try {
      socket = new Socket(SERVER_IP,SERVER_PORT);
      webOut = new DataOutputStream(socket.getOutputStream());
      webIn = new DataInputStream(socket.getInputStream());
      socket.setSoTimeout(2000);
    } catch(IOException e){
      System.err.println("Cannot connect to the server");
      return;
    }
    System.out.println("connected");
    data = new Data();
    data.status = 0;
    data.roomId = 0;
    UserInterface ui = new UserInterface(data);
    Thread UIThread = new Thread(ui);
    PipedInputStream pipe;
    try {pipe = new PipedInputStream(ui.pipe); userIn = new DataInputStream(pipe);} 
    catch(IOException e) {System.err.println("User Interface Error"); System.exit(1);}
    // userIn = new DataInputStream(System.in);
    UIThread.start();

    Thread debugThread = new Thread(()->{
      while(true){
        try{Thread.sleep(3000);}catch(InterruptedException e){}
        System.out.println(data.rooms);
      }
    });
    debugThread.start();
    OUT:
    while(true){
      switch (data.status) {
        case 0:
          hall();
          break;
        case 1:
        case 2:
          room();
          break;
        case 3:
          game();
        default:
          break OUT;
      }
    }

    try {
      webOut.close();
      webIn.close();
      socket.close();
    } catch(IOException e) {}
  }
  static void hall(){
    while (true) {
      try{Thread.sleep(100);}catch(InterruptedException e){}
      sendRoomId();
      data.status = recvStatus();
      recvRoomList();
      if(data.status!=0) {
        data.roomId=data.status;
        data.status=1;
        return;
      }
      data.roomId=tryUserReadInt();
    }
  }
  static void room(){
    while (true) {
      try{Thread.sleep(100);}catch(InterruptedException e){}
      sendRoomId();
      sendStatus();
      data.status = recvStatus();
      if(data.status!=1||data.status!=2) return;
      try {
        data.playerNumber = webIn.readInt();
        data.readyNumber = webIn.readInt();
      } catch(IOException e) {
        System.err.println("Network disconnected");
        System.exit(1);
      }
      int x = tryUserReadInt();
      if(x==1) data.status=2;
    }
  }
  static void game(){
    data.status=-1;
    return;
  }
  static void webSendInt(int x){
    try{
      webOut.writeInt(x);
    } catch(IOException e) {
      System.err.println("Network disconnected");
      System.exit(1);
    }
  }
  static int tryUserReadInt(){
    int x = 0;
    try{
      if(userIn.available()==0) return x;
      x = userIn.readInt();
    } catch(IOException e) {
      x = 0;
    }
    return x;
  }
  static int userReadInt(){
    int x = 0;
    try{
      x = userIn.readInt();
    } catch(IOException e) {
      System.err.println("User Interface Error");
      System.exit(1);
    }
    return x;
  }
  static int recvStatus(){
    int x = -1;
    try{
      x = webIn.readInt();
    }catch(IOException e){
      System.err.println("Network disconnected");
      System.exit(1);
    }
    return x;
  }
  static void sendStatus(){
    webSendInt(data.status);
  }
  static void sendRoomId(){
    webSendInt(data.roomId);
  }
  static void recvRoomList(){
    try {
      int n = webIn.readInt();
      data.rooms = new ArrayList<>();
      for(int i=0;i<n;++i) data.rooms.add(webIn.readInt());
    } catch (IOException e){
      System.err.println("Network disconnected");
      System.exit(1);
    }
  }
}