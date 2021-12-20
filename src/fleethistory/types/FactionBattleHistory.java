package fleethistory.types;

public class FactionBattleHistory implements Comparable<FactionBattleHistory> {

  public String factionId;
  public int battles = 0;
  public int battlesWon = 0;
  public int battlesLost = 0;
  public int officers = 0;
  public int totalShips = 0;
  public int totalFleetPoints = 0;
  public int frigates = 0;
  public int destroyers = 0;
  public int cruisers = 0;
  public int capitalShips = 0;
  public int stations = 0;

  public FactionBattleHistory(String id) {
    this.factionId = id;
  }

  @Override
  public int compareTo(FactionBattleHistory f) {
    return f.totalFleetPoints - this.totalFleetPoints;
  }

}
