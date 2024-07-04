package Server;
import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
  final static int PORT = 8848;
  final static int MAX_ROOMS = 100;
  static ServerSocket serverSocket;
  volatile static Set<User> users = Collections.synchronizedSet(new HashSet<User>());
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
        try{Thread.sleep(3000);}catch(InterruptedException e){}
        // System.out.println(games);
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
      synchronized(users){synchronized(rooms){
        Iterator<User> it = users.iterator();
        while(it.hasNext()){
          User user = it.next();
          try{
            int x=user.in.readInt();
            if(x==0) {
              user.sendStatus();
              user.sendRoomList();
              continue;
            }
            if(x==-1){
              int roomKey = createRoom();
              if(roomKey==-1){
                System.err.println("fail to create room");
                continue;
              }
              user.out.writeInt(roomKey);
              user.sendRoomList();
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
            user.sendRoomList();
          } catch(IOException e){
            it.remove();
            System.out.println("Client disconnected");
          } catch(IllegalStateException e){
          }
        }
      }}
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
    synchronized(rooms) {rooms.put(key,new Room(key));}
    return key;
  }
}
class User {
  Socket userSocket;
  DataInputStream in; // must read 0(hall)/room_id/-1(create)/-2(playing) first
  DataOutputStream out; // must sendStatus first
  int status; // 0=hall, 1=room, 2=ready, 3=playing
  String name;
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
  void sendRoomList() throws IOException{
    List<Integer> l = new ArrayList<>();
    for(Integer k : Server.rooms.keySet())
      l.add(k);
    out.writeInt(l.size());
    for(int i=0;i<l.size();++i)
      out.writeInt(l.get(i));
  }
}
class Room {
  Set<User> users;
  int roomId;
  public Room(int key){
    users = Collections.synchronizedSet(new HashSet<User>());
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
        int num = users.size(), ready_num = 0;
        while(it.hasNext()){
          User user = it.next();
          try{
            user.in.readInt();
            int y=user.in.readInt();
            if(y==2) {user.Ready(); ++ready_num;}
            else {user.enterRoom();}
          } catch(IOException e){
            user.status = -1;
          }
        }
        it = users.iterator();
        while(it.hasNext()){
          User user = it.next();
          try{
            user.sendStatus();
            user.out.writeInt(num);
            user.out.writeInt(ready_num);
          } catch(IOException e){
            user.status = -1;
          }
        }
        it = users.iterator();
        while(it.hasNext()){
          User user = it.next();
          if(user.status==-1) it.remove();
        }
        if(ready_num>=num&&num>1){
          break OUT;
        }
      }
    }
    Game game = new Game(this);
    synchronized(Server.rooms) {Server.rooms.remove(roomId);}
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
        u.enterGame();
        try{ u.in.readInt(); u.in.readInt(); u.sendStatus(); } catch(IOException e) {}
        players[cnt++]=new Player(u);
      }
      randomShuffle(players,n);
      for(int i=0;i<n;++i) {
        try {players[i].u.out.writeInt(i);} // player position
        catch (IOException e) {}
      }
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
      sendRoundBegin();
      round();
      clearLoser();
      getActiveNumber();
      dealer = getNext(dealer);
    }
    sendGameEnd();
    synchronized(Server.users){
      for(Player p : players)
        Server.users.add(p.u);
    }
  }
  private void sendRoundBegin(){
    for(int i=0;i<n;++i) {
      try {players[i].u.out.writeInt(114514 + 0);}
      catch (IOException e) {}
    }
  }
  private void sendGameEnd(){
    for(int i=0;i<n;++i) {
      try {players[i].u.out.writeInt(114514 + 1);}
      catch (IOException e) {}
    }
  }
  private void sendOpenMsg(){
    for(int i=0;i<n;++i) {
      try {players[i].u.out.writeInt(114514 + 2);}
      catch (IOException e) {}
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
      players[i].x = cards[top_card++];
      players[i].y = cards[top_card++];
      i=getNext(i);
      if(i==dealer) break;
    }
    Card flop1, flop2, flop3, turn, river;
    top_card++;
    flop1 = cards[top_card++];
    flop2 = cards[top_card++];
    flop3 = cards[top_card++];
    top_card++;
    turn = cards[top_card++];
    top_card++;
    river = cards[top_card++];
    for(int i=0;i<n;++i){
      try{
        players[i].u.out.writeInt(dealer);
        players[i].sendCard(flop1);
        players[i].sendCard(flop2);
        players[i].sendCard(flop3);
        players[i].sendCard(turn);
        players[i].sendCard(river);
        for(int j=0;j<n;++j){
          players[i].sendCard(players[j].x);
          players[i].sendCard(players[j].y);
        }
      } catch(IOException e) {}
    }
    
    sendBetAndPot(pot,-1);
    pot+=preflop_betting_round(pot);
    sendBetAndPot(pot,-1);
    sendOpenMsg();
    sendOpenMsg();
    sendOpenMsg();
    if(getFoldNumber()+1==getActiveNumber()){
      roundEnd(pot);
      return;
    }
    pot+=betting_round(pot);
    sendBetAndPot(pot,-1);
    sendOpenMsg();
    if(getFoldNumber()+1==getActiveNumber()){
      roundEnd(pot);
      return;
    }
    pot+=betting_round(pot);
    sendBetAndPot(pot,-1);
    sendOpenMsg();
    if(getFoldNumber()+1==getActiveNumber()){
      roundEnd(pot);
      return;
    }
    pot+=betting_round(pot);
    sendBetAndPot(pot,-1);
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
  void sendBetAndPot(int pot,int opt_player){
    for(int i=0;i<n;++i){
      try{
        players[i].u.out.writeInt(0);
        players[i].u.out.writeBoolean(i==opt_player);
        players[i].u.out.writeInt(pot);
        for(int j=0;j<n;++j){
          players[i].u.out.writeInt(players[j].money);
          players[i].u.out.writeInt(players[j].bets);
          players[i].u.out.writeBoolean(players[j].folded);
        }
      } catch(IOException e) {}
    }
  }
  private int preflop_betting_round(int prepot){
    int max_bet = MIN_BETS, pot = 0, check_cnt = 0;
    clearBet();
    getActiveNumber();
    for(int i=getNext(dealer),jj=0;;i=getNext(i)){
      System.out.println("");
      System.out.println("player "+i+" turn");
      if(players[i].folded) continue;
      if(jj==0) {pot+=players[i].bets(MIN_BETS); jj=1; sendBetAndPot(pot+prepot, -1);}
      else if(jj==1) {pot+=players[i].bets(MIN_BETS*2); jj=2; sendBetAndPot(pot+prepot, -1);}
      else { sendBetAndPot(pot+prepot, i); pot += players[i].recvMoney();}
      System.out.println("pot = "+pot);
      System.out.println("p0 bet = "+players[0].bets);
      System.out.println("p1 bet = "+players[1].bets);
      if(max_bet==0&&players[i].bets==0) { 
        ++check_cnt;
        if(check_cnt+getFoldNumber()==getActiveNumber())
          break;
        continue;
      }
      max_bet = max(max_bet,players[i].bets);
      if(players[i].bets<max_bet&&!players[i].allIn) players[i].fold();
      boolean flag = true;
      for(int j=0;j<n;++j)
        if(players[j].active&&!players[j].folded&&!players[j].allIn&&players[j].bets<max_bet)
          flag=false;
      if(flag) break;
    }
    System.out.println("betting round 1 end");
    return pot;
  }
  private int betting_round(int prepot){
    int max_bet = 0, pot = 0, check_cnt = 0;
    clearBet();
    getActiveNumber();
    for(int i=getNext(dealer);;i=getNext(i)){
      System.out.println("");
      System.out.println("player "+i+" turn");
      if(players[i].folded) continue;
      sendBetAndPot(pot+prepot, i);
      pot += players[i].recvMoney();
      System.out.println("pot = "+pot);
      System.out.println("p0 bet = "+players[0].bets);
      System.out.println("p1 bet = "+players[1].bets);
      if(max_bet==0&&players[i].bets==0) { 
        ++check_cnt;
        if(check_cnt+getFoldNumber()==getActiveNumber())
          break;
        continue;
      }
      max_bet = max(max_bet,players[i].bets);
      if(players[i].bets<max_bet&&!players[i].allIn) players[i].fold();
      boolean flag = true;
      for(int j=0;j<n;++j)
        if(players[j].active&&!players[j].folded&&!players[j].allIn&&players[j].bets<max_bet)
          flag=false;
      if(flag) break;
    }
    System.out.println("betting round end");
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
  private void clearBet(){
    for(int i=0;i<n;++i){
      players[i].bets = 0;
    }
  }
  private void clearLoser(){
    for(int i=0;i<n;++i){
      players[i].allIn = false;
      players[i].folded = false;
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
      u.userSocket.setSoTimeout(30000); // 20 seconds
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
        System.err.println("invalid state of "+this+"\n expected -2 read "+st);
        active = false;
        return 0;
      }
      int res = u.in.readInt();
      System.out.println("bets = "+res);
      return bets(res);
    } catch(IOException e) {
      System.err.println("cannot get money from "+this);
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