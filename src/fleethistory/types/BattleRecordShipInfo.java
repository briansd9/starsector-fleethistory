package fleethistory.types;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import fleethistory.U;
import org.apache.log4j.Logger;

public class BattleRecordShipInfo implements Comparable<BattleRecordShipInfo> {

  private String id;
  private String name;
  private String hullId;
  public String status;

  public BattleRecordShipInfo(FleetMemberAPI ship, boolean isPlayerShip) {
    
    if (isPlayerShip) {
      // only store name for player ships
      this.setName(ship.getShipName());
    }
    
    this.setId(ship.getId());
    
    ShipHullSpecAPI hull = ship.getHullSpec();
    String tempHullId = hull.getDParentHullId();
    if(tempHullId == null) {
      tempHullId = hull.getHullId();
    }    
    this.setHullId(tempHullId);
    
  }
  
  public BattleRecordShipInfo(String compressedString) {
    String[] str = compressedString.split("\\|");
    this.id = str[0];
    this.name = str[1];
    this.hullId = str[2];
    this.status = str[3].length() == 0 ? null : str[3];
  }
  public String getCompressedString() {
    return String.format(
            "%s|%s|%s|%s",
            this.id,
            this.name,
            this.hullId,
            this.status
    );
  }
  
  public final void setId(String str) {
    
    if(this.name != null) {
      // for named (player) ships, cache the fleet member ID
      // because it will be used repeatedly in battle logs
      this.id = U.getCache().cacheString(str);
    } else {
      // for enemy ships, just encode the ID (which should be a hexadecimal number)
      // because it will only be used once
      try {
        int decimalID = Integer.parseInt(str, 16);
        this.id = U.encodeNum(decimalID);
      } catch(NumberFormatException e) {
        this.id = U.getCache().cacheString(str);
      }
    }    
    
  }
  
  public String getId() {
    String cachedId = U.getCache().getCachedString(this.id);
    if(cachedId != null) {
      return cachedId;      
    } else {
      return Long.toHexString(U.decodeNum(this.id));
    }
  }
  
  
  public final void setName(String str) {
    if(str != null) {
      this.name = U.getCache().cacheString(str);
    }
  }
  public String getName() {
    return this.name == null ? null : U.getCache().getCachedString(this.name);
  }
  
  public final void setHullId(String str) {
    this.hullId = U.getCache().cacheString(str);
  }
  public String getHullId() {
    return U.getCache().getCachedString(this.hullId);
  }

  public ShipHullSpecAPI getHullSpec() {
    return Global.getSettings().getHullSpec(this.getHullId());
  }

  public int getFP() {
    return this.getHullSpec().getFleetPoints();
  }

  public boolean isStation() {
    return U.isStation(this.getHullSpec());
  }

  @Override
  public int compareTo(BattleRecordShipInfo s2) {
    return s2.getFP() - this.getFP();
  }

}
