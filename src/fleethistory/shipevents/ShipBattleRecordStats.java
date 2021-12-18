package fleethistory.shipevents;

import fleethistory.U;

public class ShipBattleRecordStats {

  String hullId;
  int kills = 0;
  int assists = 0;
  int fleetPoints = 0;

  public ShipBattleRecordStats(String hullId) {
    this.setHullId(hullId);
  }

  // for getting from compressed string... this stinks
  public ShipBattleRecordStats(String compressedString, Object o) {
    String[] str = compressedString.split("\\|", -1);
    this.hullId = str[0];
    this.kills = Integer.parseInt("0" + str[1]);
    this.assists = Integer.parseInt("0" + str[2]);
    this.fleetPoints = Integer.parseInt("0" + str[3]);
  }

  public String getCompressedString() {
    return String.format(
            "%s|%s|%s|%s",
            this.hullId,
            this.kills == 0 ? "" : this.kills,
            this.assists == 0 ? "" : this.assists,
            this.fleetPoints == 0 ? "" : this.fleetPoints
    );
  }

  public final void setHullId(String str) {
    this.hullId = U.getCache().cacheString(str);
  }

  public String getHullId() {
    return U.getCache().getCachedString(this.hullId);
  }

  public int getKills() {
    return kills;
  }

  public int getAssists() {
    return assists;
  }

  public int getFleetPoints() {
    return fleetPoints;
  }

}
