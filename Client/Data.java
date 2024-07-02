package Client;

import java.util.*;

public class Data {
  public volatile int status;
  public volatile int roomId;
  public volatile List<Integer> rooms;
  public Data(){
    rooms = Arrays.asList(1,2,3,4,5);
  }
}
