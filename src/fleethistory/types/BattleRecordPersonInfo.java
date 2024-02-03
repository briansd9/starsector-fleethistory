package fleethistory.types;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import java.util.Objects;
import fleethistory.U;
import org.apache.log4j.Logger;

public class BattleRecordPersonInfo {

  private String name;
  private String spriteId;
  private String shipName;
  private String shipHullId;
  private String rank;
  private int level = 0;
  
  public String shipStatus;
  public boolean isFleetCommander = false;

  public BattleRecordPersonInfo(String name, String spriteId, int level, String rank) {
    this.setName(name);
    this.setSpriteId(spriteId);
    this.setLevel(level);
    this.setRank(rank);
  }
  
  public BattleRecordPersonInfo(String name, String spriteId) {
    this.setName(name);
    this.setSpriteId(spriteId);
  }
  
  public BattleRecordPersonInfo(String compressedString) {
    
//    Logger.getLogger(this.getClass()).info(compressedString);
    
    String[] str = compressedString.split("\\|");
    
//    Logger.getLogger(this.getClass()).info(str.length);
    
    this.name = str[0];
    this.spriteId = str[1];
    this.shipName = str[2].length() == 0 ? null : str[2];
    this.shipHullId = str[3].length() == 0 ? null : str[3];    
    this.shipStatus = str[4].length() == 0 ? null : str[4];
    this.isFleetCommander = (Integer.parseInt(str[5]) == 1);
    
    this.level = (str.length > 6 && str[6].length() > 0) ? (int)U.decodeNum(str[6]) : 0;
    this.rank = (str.length > 7 && str[7].length() > 0) ? str[7] : null;
    
  }
  public String getCompressedString() {
    return String.format(
            "%s|%s|%s|%s|%s|%d|%s|%s",
            this.name,
            this.spriteId,
            this.shipName == null ? "" : this.shipName,
            this.shipHullId == null ? "" : this.shipHullId,
            this.shipStatus == null ? "" : this.shipStatus,
            this.isFleetCommander ? 1 : 0,
            this.level == 0 ? "" : U.encodeNum(this.level),
            this.rank == null ? "" : this.rank
    );
  }
  
  public final void setName(String str) {
    StringBuilder sb = new StringBuilder();
    for(String s : str.split(" ")) {
      sb.append(sb.length() > 0 ? " " : "").append(U.getCache().cacheString(s));
    }
    this.name = sb.toString();
  }
  public String getName() {
    StringBuilder sb = new StringBuilder();
    for(String s : this.name.split(" ")) {
      sb.append(sb.length() > 0 ? " " : "").append(U.getCache().getCachedString(s));
    }
    return sb.toString();
  }
  
  public final void setSpriteId(String str) {
    this.spriteId = U.getCache().cacheString(str);
  }  
  public String getSpriteId() {
    return U.getCache().getCachedString(this.spriteId);
  }
  
  public void setShip(FleetMemberAPI ship) {
    this.setShipName(ship.getShipName());
    this.setShipHullId(ship.getHullId());
  }
  
  public final void setShipName(String str) {
    this.shipName = U.getCache().cacheString(str);
  }  
  public String getShipName() {
    if(this.shipName == null) return null;
    return U.getCache().getCachedString(this.shipName);
  }  
  
  public final void setShipHullId(String str) {
    this.shipHullId = U.getCache().cacheString(str);
  }  
  public String getShipHullId() {
    if(this.shipHullId == null) return null;
    return U.getCache().getCachedString(this.shipHullId);
  }

  public final void setRank(String str) {
    this.rank = U.getCache().cacheString(str);
  }  
  public String getRank() {
    if(this.rank == null) return null;
    return U.getCache().getCachedString(this.rank);
  }
  
  public final void setLevel(int level) {
    this.level = level;
  }
  public int getLevel() {
    return level;
  }


  public String getFullShipName() {
    return String.format(
            "%s, %s",
            this.getShipName(),
            Global.getSettings().getHullSpec(this.getShipHullId()).getNameWithDesignationWithDashClass()
    );
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof BattleRecordPersonInfo && this.name.equals(((BattleRecordPersonInfo) obj).name) && this.spriteId.equals(((BattleRecordPersonInfo) obj).spriteId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, spriteId);
  }

}
