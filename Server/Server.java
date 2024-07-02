package Server;
import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
  final static int PORT = 8848;
  final static int MAX_ROOMS = 100;
  static ServerSocket serverSocket;
  volatile static Set<User> users = new HashSet<User>();
  volatile static Map<Integer,Room> rooms = new HashMap<Integer,Room>();
  static Set<Game> games = new HashSet<Game>();
  public static void main(String[] args){
    try {
      serverSocket = new ServerSocket(PORT);
    } catch(IOException e){
      e.printStackTrace();
    }
    System.out.println("Server started");
    Thread debugThread = new Thread(()->{
      while(true){
        try{Thread.sleep(5000);}catch(InterruptedException e){}
        System.out.println("hall "+users.size());
        System.out.println("room "+rooms.size());
      }
    });
    debugThread.start();

    Thread accepterThread = new Thread(()->{accepter();});
    accepterThread.start();

    Thread handlerThread = new Thread(()->{handler();});
    handlerThread.start();

    while(true) {}
  }
  static void accepter(){
    while(true){
      try {
        Socket clientSocket = serverSocket.accept();
        clientSocket.setSoTimeout(1000);
        System.out.println("new client: "+clientSocket);
        synchronized(users){
          users.add(new User(clientSocket));
        }
      } catch(IOException e) {
        e.printStackTrace();
      }
    }
  }
  static void handler(){
    while(true){
      try{Thread.sleep(100);}catch(InterruptedException e){}
      Iterator<User> it = users.iterator();
      while(it.hasNext()){
        User user = it.next();
        try{
          int x=user.in.readInt();
          if(x==0) {
            user.sendStatus();
            continue;
          }
          if(x==-1){
            System.out.println("fuck");
            int roomKey = createRoom();
            if(roomKey==-1){
              System.err.println("fail to create room");
              continue;
            }
            user.out.writeInt(roomKey);
            System.out.println("fuckid: "+roomKey);
            rooms.get(roomKey).add(user);
            it.remove();
            continue;
          }
          if(!rooms.containsKey(x)){
            System.err.println("A user tries to visit invalid room");
            System.err.println("User: "+user);
            System.err.println("Room: "+x);
            continue;
          }
          rooms.get(x).add(user);
          it.remove();
          user.sendStatus();
        } catch(IOException e){
          it.remove();
          System.out.println("Client disconnected");
        } catch(IllegalStateException e){
        }
      }
    }
  }
  static int createRoom(){
    int key = 0;
    for(int i=1;i<=MAX_ROOMS;++i)
      if(!rooms.containsKey(i)){
        key=i;
        break;
      }
    if(key==0) return -1;
    rooms.put(key,new Room(key));
    return key;
  }
}
class User {
  Socket userSocket;
  DataInputStream in; // must read 0(hall)/room_id/-1(create)/-2(playing) first
  DataOutputStream out; // must sendStatus first
  int status; // 0=hall, 1=room, 2=ready, 3=playing
  public User(Socket clientSocket){
    userSocket=clientSocket;
    status=0;
    try {
      in = new DataInputStream(userSocket.getInputStream());
      out = new DataOutputStream(userSocket.getOutputStream());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  void enterHall(){
    status = 0;
  }
  void enterRoom(){
    status = 1;
  }
  void Ready(){
    status = 2;
  }
  void enterGame(){
    status = 3;
  }
  void sendStatus() throws IOException{
    out.writeInt(status);
  }
}
class Room {
  Set<User> users;
  int roomId;
  public Room(int key){
    users = new HashSet<User>();
    roomId = key;
    Thread handlerThread = new Thread(()->{handler();});
    handlerThread.start();
  }
  void handler(){
    OUT:
    while(true){
      try{Thread.sleep(100);}catch(InterruptedException e){}
      synchronized(users){
        Iterator<User> it = users.iterator();
        boolean all_ready = true;
        while(it.hasNext()){
          User user = it.next();
          try{
            int x=user.in.readInt();
            if(x!=roomId) {
              Server.users.add(user);
              user.enterHall();
              it.remove();
              continue;
            }
            int y=user.in.readInt();
            if(y==2) user.Ready();
            else {user.enterRoom();all_ready=false;}
            user.sendStatus();
          } catch(IOException e){
            it.remove();
          }
        }
        if(users.size()>0&&all_ready){
          break OUT;
        }
      }
    }
    Game game = new Game(this);
    Server.rooms.remove(roomId);
    Server.games.add(game);
  }
  void add(User user) throws IllegalStateException{
    synchronized(users){
      if(users.size()>=10) throw new IllegalStateException();
      users.add(user);
      user.enterRoom();
    }
  }
}
class Game {
  final static int MIN_BETS = 5;
  private Player[] players;
  private int n,active_number;
  private int dealer;
  public Game(Room r){
    synchronized(r.users){
      n=r.users.size();
      players = new Player[n];
      int cnt=0;
      for(User u : r.users){
        u.status=3;
        try{ u.sendStatus(); } catch(IOException e) {}
        players[cnt++]=new Player(u);
      }
      randomShuffle(players,n);
    }
    Thread gameThread = new Thread(()->{game();});
    gameThread.start();
  }
  private int getActiveNumber(){
    active_number = 0;
    for(int i=0;i<n;++i)
      if(players[i].active)
        ++active_number;
    return active_number;
  }
  private void game(){
    active_number = n;
    dealer = 0;
    while(active_number>1) {
      round();
      clearLoser();
      getActiveNumber();
      dealer = getNext(dealer);
    }
  }
  private void round(){
    Card[] cards = new Card[52];
    int top_card = 0;
    for(int j=0;j<13;++j)
      for(int i=0;i<4;++i)
        cards[i+j*4]=new Card(i,j+2);
    randomShuffle(cards,52);
    int pot = 0;
    for(int i=dealer;;){
      players[i].sendCard(players[i].x = cards[top_card++]);
      players[i].sendCard(players[i].y = cards[top_card++]);
      i=getNext(i);
      if(i==dealer) break;
    }
    Card flop1, flop2, flop3, turn, river;
    pot+=preflop_betting_round();
    if(getFoldNumber()+1==getActiveNumber()){
      roundEnd(pot);
      return;
    }
    top_card++;
    flop1 = cards[top_card++];
    sendCardAll(flop1);
    flop2 = cards[top_card++];
    sendCardAll(flop2);
    flop3 = cards[top_card++];
    sendCardAll(flop3);
    pot+=betting_round();
    if(getFoldNumber()+1==getActiveNumber()){
      roundEnd(pot);
      return;
    }
    top_card++;
    turn = cards[top_card++];
    sendCardAll(turn);
    pot+=betting_round();
    if(getFoldNumber()+1==getActiveNumber()){
      roundEnd(pot);
      return;
    }
    top_card++;
    river = cards[top_card++];
    sendCardAll(turn);
    pot+=betting_round();
    if(getFoldNumber()+1==getActiveNumber()){
      roundEnd(pot);
      return;
    }
    CardGroup7 mx7 = new CardGroup7(flop1,flop2,flop3,turn,river,players[0].x,players[0].y);
    CardGroup5 mx = mx7.value();
    int mxc = 0;
    for(int i=1;i<n;++i){
      CardGroup7 x = new CardGroup7(flop1,flop2,flop3,turn,river,players[i].x,players[i].y);
      CardGroup5 v = x.value();
      if(v.above(mx)) mx=v;
    }
    for(int i=0;i<n;++i){
      CardGroup7 x = new CardGroup7(flop1,flop2,flop3,turn,river,players[i].x,players[i].y);
      CardGroup5 v = x.value();
      if(!v.above(mx)&&!mx.above(v)) ++mxc;
    }
    for(int i=0;i<n;++i){
      CardGroup7 x = new CardGroup7(flop1,flop2,flop3,turn,river,players[i].x,players[i].y);
      CardGroup5 v = x.value();
      if(!v.above(mx)&&!mx.above(v))
        players[i].money += pot/mxc;
    }
  }
  private int preflop_betting_round(){
    int max_bet = 2*MIN_BETS, pot = 0, min_bet = 0;
    clearBet();
    getActiveNumber();
    for(int i=(active_number>2?getNext(dealer):dealer),j=0;;i=getNext(i)){
      if(players[i].folded) continue;
      if (j==0) {pot += players[i].bets(MIN_BETS); ++j;}
      else if(j==1) {pot += players[i].bets(MIN_BETS*2); ++j;}
      else pot += players[i].recvMoney();
      max_bet = max(max_bet,players[i].bets);
      if(!players[i].folded) min_bet = min(min_bet,players[i].bets);
      if(players[i].bets<max_bet&&!players[i].allIn)
        players[i].fold();
      if(min_bet==max_bet&&min_bet!=0) break;
    }
    return pot;
  }
  private int betting_round(){
    int max_bet = 0, pot = 0, min_bet = 0;
    clearBet();
    for(int i=getNext(dealer);;i=getNext(i)){
      if(players[i].folded) continue;
      pot += players[i].recvMoney();
      max_bet = max(max_bet,players[i].bets);
      if(!players[i].folded) min_bet = min(min_bet,players[i].bets);
      if(players[i].bets<max_bet&&!players[i].allIn)
        players[i].fold();
      if(min_bet==max_bet&&min_bet!=0) break;
    }
    return pot;
  }
  static int max(int x,int y){
    int t=((x-y)>>31);
    return (t&y)|((~t)&x);
  }
  static int min(int x,int y){
    int t=((x-y)>>31);
    return ((t&x)|((~t)&y));
  }
  private int getNext(int x){
    x=(x+1)%n;
    while(!players[x].active) x=(x+1)%n;
    return x;
  }
  private void randomShuffle(Object[] a,int n){
    Random rand = new Random();
    for(int i=n-1;i>0;--i){
      int j=rand.nextInt(i);
      Object u=a[i],v=a[j];
      a[j]=u;a[i]=v;
    }
  }
  private void sendCardAll(Card c){
    for(int i=0;i<n;++i)
      players[i].sendCard(c);
  }
  private void clearBet(){
    for(int i=0;i<n;++i){
      players[i].bets = 0;
    }
  }
  private void clearLoser(){
    for(int i=0;i<n;++i){
      players[i].allIn = false;
      if(players[i].money<=0) players[i].active = false;
    }
  }
  private void roundEnd(int pot){
    for(int i=0;i<n;++i)
      if(players[i].active&&!players[i].folded)
        players[i].money += pot;
  }
  private int getFoldNumber(){
    int res=0;
    for(int i=0;i<n;++i)
      if(players[i].active&&players[i].folded)
        ++res;
    return res;
  }
}
class Player {
  User u;
  final static int INIT_MONEY = 500;
  int money,bets;
  boolean allIn,folded;
  boolean active;
  Card x,y;
  public Player(User user){
    u = user;
    money = INIT_MONEY;
    active = true;
    try {
      u.userSocket.setSoTimeout(10000); // 10 seconds
    } catch(SocketException e){
      active = false;
    }
  }
  int bets(int v){
    if(v==-1) v=money;
    if(v<0) v=0;
    if(v<money) {
      money -= v;
      bets += v;
      return v;
    }
    else {
      v = money;
      bets += v;
      money = 0;
      allIn = true;
      return v;
    }
  }
  void fold(){
    folded = true;
  }
  void sendCard(Card c){
    if(!active) return;
    try {
      u.sendStatus();
      u.out.writeByte(0);
      u.out.writeInt(c.color);
      u.out.writeInt(c.value);
    } catch(IOException e) {
      active = false;
    }
  }
  void sendMoney(int player_pos,int money){
    if(!active) return;
    try {
      u.sendStatus();
      u.out.writeByte(1);
      u.out.writeInt(player_pos);
      u.out.writeInt(money);
    } catch(IOException e) {
      active = false;
    }
  }
  int recvMoney(){
    if(!active) return 0;
    try {
      int st = u.in.readInt();
      if(st!=-2){
        active = false;
        return 0;
      }
      int res = u.in.readInt();
      return bets(res);
    } catch(IOException e) {
      active = false;
      return 0;
    }
  }
}
class Card {
  int color, value;
  public Card(int x,int y){
    color=x; value=y;
  }
}
class CardGroup7 {
  Card[] cards;
  public CardGroup7(Card a,Card b,Card c,Card d,Card e,Card f,Card g){
    cards = new Card[]{a,b,c,d,e,f,g};
  }
  CardGroup5 value(){
    CardGroup5 mx = new CardGroup5(cards[0],cards[1],cards[2],cards[3],cards[4]);
    for(int i=0;i<7;++i)
      for(int j=0;j<i;++j){
        Card[] c = new Card[5];
        for(int k=0,l=0;k<7;++k)
          if(k!=i&&k!=j)
            c[l++]=cards[k];
        CardGroup5 t = new CardGroup5(c[0],c[1],c[2],c[3],c[4]);
        if(t.above(mx)) mx = t;
      }
    return mx;
  }
}
class CardGroup5 {
  Card[] cards;
  public CardGroup5(Card a,Card b,Card c,Card d,Card e){
    cards = new Card[]{a,b,c,d,e};
  }
  boolean above(CardGroup5 b){
    List<Integer> va = value(), vb = b.value();
    int n = Game.min(va.size(),vb.size());
    for(int i=0;i<n;++i)
      if(va.get(i)!=vb.get(i))
        return va.get(i)>vb.get(i);
    return false;
  }
  List<Integer> value(){
    int[] v = new int[]{cards[0].value,cards[1].value,cards[2].value,cards[3].value,cards[4].value};
    for(int i=0;i<5;++i)
      for(int j=0;j<4-i;++j)
        if(v[j]<v[j+1]){
          int t=v[j],u=v[j+1];
          v[j]=u;v[j+1]=t;
        }
    boolean is_flush = true;
    for(int i=1;i<5;++i)
      if(cards[i].color!=cards[0].color)
        is_flush=false;
    boolean is_straight = true;
    for(int i=1;i<5;++i) if(v[i]+i!=v[0]) is_straight = false;
    if(v[0]==14&&v[1]==5&&v[2]==4&&v[3]==3&&v[4]==2) {
      v[0]=5;v[1]=4;v[2]=3;v[1]=2;v[0]=1;
      is_straight = true;
    }
    // straight flush
    if(is_flush&&is_straight){
      return Arrays.asList(8,v[0]);
    }
    // four of a kind
    if(v[0]==v[3]) return Arrays.asList(7,v[0],v[4]);
    if(v[1]==v[4]) return Arrays.asList(7,v[4],v[0]);
    // full house
    if(v[0]==v[2]&&v[3]==v[4]) return Arrays.asList(6,v[0],v[4]);
    if(v[0]==v[1]&&v[2]==v[4]) return Arrays.asList(6,v[4],v[0]);
    // flush
    if(is_flush) return Arrays.asList(5,v[0],v[1],v[2],v[3],v[4]);
    // straight
    if(is_straight) return Arrays.asList(4,v[0]);
    // three of a kind, two pairs, pair, high card
    List<Integer> res = new ArrayList<Integer>(), res2 = new ArrayList<Integer>();
    res.add(0);
    int[] a = new int[15];
    for(int i=2;i<15;++i) a[i]=0;
    for(int i=0;i<5;++i) a[cards[i].value]++;
    for(int i=3;i>=1;--i)
      for(int j=2;j<15;++j)
        if(a[j]==i){
          res.add(i);
          res2.add(j);
        }
    for(int x : res2) res.add(x);
    return res;
  }
}