package Client;

import java.util.concurrent.CopyOnWriteArrayList;

class Card {

    public int color, value;

    public Card(int x, int y) {
        color = x;
        value = y;
    }
}

class CardGroup5 {

    public Card[] cards;

    public CardGroup5(Card a, Card b, Card c, Card d, Card e) {
        cards = new Card[]{a, b, c, d, e};
    }
}

class Player {

    public Card c1, c2;
    public int money, bet;
    public String name;
    public boolean folded;

    public Player(){}
}

public class Data {

    public volatile int status;
    public volatile int roomId;
    public volatile CopyOnWriteArrayList<Integer> rooms;
    public volatile CopyOnWriteArrayList<Player> players;
    public volatile CardGroup5 publicCards;
    public volatile int showedCardsNumber;
    public volatile int playerNumber, readyNumber;
    public volatile int myPos;
    public volatile int dealer;
    public volatile int pot;

    public Data() {
        rooms = new CopyOnWriteArrayList<Integer>();
        players = new CopyOnWriteArrayList<Player>();
        showedCardsNumber = 0;
    }
}
