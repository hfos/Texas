package Client;

import java.util.*;

import javax.smartcardio.Card;

class Card {
  int color, value;
  public Card(int x,int y){
    color=x; value=y;
  }
}
class CardGroup5 {
  Card[] cards;
  public CardGroup5(Card a,Card b,Card c,Card d,Card e){
    cards = new Card[]{a,b,c,d,e};
  }
}
class Player{
  Card c1,c2;
  int money,bet;
  boolean ready;
  String name;
}

public class Data {
  public volatile int status;
  public volatile int roomId;
  public volatile List<Integer> rooms;
  public volatile List<Player> players;
  public volatile CardGroup5 publicCards;
  public Data(){
    rooms = Arrays.asList(1,2,3,4,5);
  }
}
