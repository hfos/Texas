package Client;

import java.io.*;
import java.net.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Client {

  final static String SERVER_IP = "43.143.223.49";
  final static int SERVER_PORT = 8848;
  static Socket socket;
  static DataOutputStream webOut;
  static DataInputStream webIn;
  static Data data;
  static DataInputStream userIn;

  public static void main(String[] args) {
    try {
      socket = new Socket(SERVER_IP, SERVER_PORT);
      webOut = new DataOutputStream(socket.getOutputStream());
      webIn = new DataInputStream(socket.getInputStream());
      socket.setSoTimeout(200000);
    } catch (IOException e) {
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
    try {
      pipe = new PipedInputStream(ui.pipe);
      userIn = new DataInputStream(pipe);
    } catch (IOException e) {
      System.err.println("User Interface Error");
      System.exit(1);
    }
    // userIn = new DataInputStream(System.in);
    UIThread.start();

    Thread debugThread = new Thread(() -> {
      while (true) {
        try {
          Thread.sleep(3000);
        } catch (InterruptedException e) {
        }
      }
    });
    debugThread.start();
    OUT: while (true) {
      System.out.println("MAIN data.status : " + data.status);
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
          break;
        default:
          break OUT;
      }
    }

    try {
      webOut.close();
      webIn.close();
      socket.close();
    } catch (IOException e) {
    }
    System.exit(0);
  }

  static void hall() {
    System.out.println("Enter hall");
    while (true) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
      }
      sendRoomId();
      data.status = recvStatus();
      recvRoomList();
      if (data.status != 0) {
        data.roomId = data.status;
        data.status = 1;
        return;
      }
      data.roomId = tryUserReadInt();
      System.out.println("room id = " + data.roomId);
    }
  }

  static void room() {
    System.out.println("Enter room");
    while (true) {
      sendRoomId();
      sendStatus();
      data.status = recvStatus();
      if (data.status != 1 && data.status != 2) {
        return;
      }
      try {
        data.playerNumber = webIn.readInt();
        data.readyNumber = webIn.readInt();
        // System.out.println("get ready number: " + data.readyNumber);
      } catch (IOException e) {
        System.err.println("Network disconnected");
        System.exit(1);
      }
      int x = tryUserReadInt();
      if (x == 1) {
        data.status = 2;
      }
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
      }
    }
  }

  static void game() {
    System.out.println("enter game");
    data.myPos = webReadInt();
    webReadInt();
    while (true) {
      data.showAll = false;
      boolean res = round();
      data.showAll = true;
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
      }
      if (res) {
        data.status = 0;
        data.roomId = 0;
        break;
      }
    }
    return;
  }

  static boolean round() { // return true when game over
    System.out.println("Enter round");
    data.pot = 0;
    data.showedCardsNumber = 0;
    data.dealer = webReadInt();
    Card a = webReadCard(), b = webReadCard(), c = webReadCard(), d = webReadCard(), e = webReadCard();
    data.publicCards = new CardGroup5(a, b, c, d, e);
    data.players = new CopyOnWriteArrayList<Player>();
    for (int i = 0; i < data.playerNumber; ++i) {
      data.players.add(new Player());
      data.players.get(i).c1 = webReadCard();
      data.players.get(i).c2 = webReadCard();
    }
    System.out.println("Read all informations");
    while (true) {
      int x = webReadBetAndPot();
      System.out.println("x = " + x);
      data.mygo = false;
      if (x == 114514 + 0) {
        return false; // next round
      } else if (x == 114514 + 1) {
        return true; // game over
      } else if (x == 114514 + 2) {
        openPublicCard();
      } else if (x == 1) {
        sendOption();
      }
    }
  }

  static void openPublicCard() {
    ++data.showedCardsNumber;
  }

  static void sendOption() {
    data.mygo = true;
    System.out.println("input opt in UI: ");
    System.out.println("my money " + data.players.get(data.myPos).money);
    int x = userReadInt();
    System.out.println("your opt is " + x);
    webSendInt(-2);
    webSendInt(x);
  }

  static int webReadBetAndPot() {
    System.out.println("read bet and pot");
    int x = webReadInt();
    if (x != 0) {
      return x;
    }
    try {
      x = (webIn.readBoolean() ? 1 : 0);
    } catch (IOException e) {
    }
    data.pot = webReadInt();
    for (int i = 0; i < data.playerNumber; ++i) {
      data.players.get(i).money = webReadInt();
      data.players.get(i).bet = webReadInt();
      try {
        data.players.get(i).folded = webIn.readBoolean();
      } catch (IOException e) {
      }
    }
    return x;
  }

  static Card webReadCard() {
    webReadInt();
    try {
      webIn.readByte();
    } catch (IOException e) {
    }
    int x = webReadInt(), y = webReadInt();
    return new Card(x, y);
  }

  static int webReadInt() {
    int x = 0;
    try {
      x = webIn.readInt();
    } catch (IOException e) {
      System.err.println("Network disconnected");
      System.exit(1);
    }
    return x;
  }

  static void webSendInt(int x) {
    try {
      webOut.writeInt(x);
    } catch (IOException e) {
      System.err.println("Network disconnected");
      System.exit(1);
    }
  }

  static int tryUserReadInt() {
    int x = 0;
    try {
      if (userIn.available() == 0) {
        return x;
      }
      x = userIn.readInt();
    } catch (IOException e) {
      x = 0;
    }
    return x;
  }

  static int userReadInt() {
    int x = 0;
    try {
      x = userIn.readInt();
    } catch (IOException e) {
      System.err.println("User Interface Error");
      System.exit(1);
    }
    return x;
  }

  static int recvStatus() {
    int x = -1;
    try {
      x = webIn.readInt();
    } catch (IOException e) {
      System.err.println("Network disconnected");
      System.exit(1);
    }
    return x;
  }

  static void sendStatus() {
    webSendInt(data.status);
  }

  static void sendRoomId() {
    webSendInt(data.roomId);
  }

  static void recvRoomList() {
    try {
      int n = webIn.readInt();
      data.rooms.clear();
      for (int i = 0; i < n; ++i) {
        data.rooms.add(webIn.readInt());
      }
    } catch (IOException e) {
      System.err.println("Network disconnected");
      System.exit(1);
    }
  }
}
