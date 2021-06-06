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
  public String shipStatus;
  public boolean isFleetCommander = false;

  public BattleRecordPersonInfo(String name, String spriteId) {
    this.setName(name);
    this.setSpriteId(spriteId);
  }
  
  public BattleRecordPersonInfo(String compressedString) {
    String[] str = compressedString.split("\\|");
    this.name = str[0];
    this.spriteId = str[1];
    this.shipName = str[2].length() == 0 ? null : str[2];
    this.shipHullId = str[3].length() == 0 ? null : str[3];    
    this.shipStatus = str[4].length() == 0 ? null : str[4];
    this.isFleetCommander = (Integer.parseInt(str[5]) == 1);
  }
  public String getCompressedString() {
    return String.format(
            "%s|%s|%s|%s|%s|%d",
            this.name,
            this.spriteId,
            this.shipName == null ? "" : this.shipName,
            this.shipHullId == null ? "" : this.shipHullId,
            this.shipStatus == null ? "" : this.shipStatus,
            this.isFleetCommander ? 1 : 0
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
