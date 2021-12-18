package fleethistory.types;

public class ShipBattleResult {
  
  // never mind readability, go for max compression - this is kind of smelly
  public static final String DEPLOYED = "A";
  public static final String DESTROYED = "B";
  public static final String DISABLED = "C";
  public static final String RETREATED = "D";
  
  public static final boolean isLost(String status) {
    return (DESTROYED.equals(status) || DISABLED.equals(status));
  }
  
};
