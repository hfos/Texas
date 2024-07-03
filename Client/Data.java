package Client;

import java.util.*;

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

    public Player(Card C1, Card C2, int Money, int Bet, boolean Ready, String Name) {
        c1 = C1;
        c2 = C2;
        money = Money;
        bet = Bet;
        name = Name;
    }
}

public class Data {

    public volatile int status;
    public volatile int roomId;
    public volatile List<Integer> rooms;
    public volatile List<Player> players;
    public volatile CardGroup5 publicCards;
    public volatile int playerNumber, readyNumber;

    public Data() {
        rooms = Arrays.asList();
        players = Arrays.asList();
    }
}
