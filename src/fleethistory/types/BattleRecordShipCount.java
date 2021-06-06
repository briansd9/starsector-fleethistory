/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fleethistory.types;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import fleethistory.U;
import org.apache.log4j.Logger;

/**
 *
 * @author joshi
 */
public class BattleRecordShipCount implements Comparable<BattleRecordShipCount> {
  
  private String hullId;
  int deployed = 0;
  int destroyed = 0;
  int disabled = 0;
  int retreated = 0;
  int fleetPoints = 0;
  int lostFleetPoints = 0;

  public BattleRecordShipCount(String hullId) {
    this.setHullId(hullId);
  }
  
  // for getting from compressed string... this stinks  
  public BattleRecordShipCount(String compressedString, Object o) {
    String[] str = compressedString.split("\\|", -1);
    this.hullId = str[0];
    this.deployed = (int)U.decodeNum(str[1]);
    this.destroyed = (int)U.decodeNum(str[2]);
    this.disabled = (int)U.decodeNum(str[3]);
    this.retreated = (int)U.decodeNum(str[4]);
    this.fleetPoints = (int)U.decodeNum(str[5]);
    this.lostFleetPoints = (int)U.decodeNum(str[6]);
  }  
  public String getCompressedString() {
    return String.format(
            "%s|%s|%s|%s|%s|%s|%s",
            this.hullId,
            this.deployed == 0 ? "" : U.encodeNum(this.deployed),
            this.destroyed == 0  ? "" : U.encodeNum(this.destroyed),
            this.disabled == 0  ? "" : U.encodeNum(this.disabled),
            this.retreated == 0  ? "" : U.encodeNum(this.retreated),
            this.fleetPoints == 0  ? "" : U.encodeNum(this.fleetPoints),
            this.lostFleetPoints == 0  ? "" : U.encodeNum(this.lostFleetPoints)
    );
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
  
  public int getDeployed() {
    return deployed;
  }
  public int getDestroyed() {
    return destroyed;
  }
  public int getDisabled() {
    return disabled;
  }
  public int getRetreated() {
    return retreated;
  }
  
  public int getSurvived() {
    return deployed + retreated;
  }
  public int getLost() {
    return destroyed + disabled;
  }
  public int getTotal() {
    return deployed + retreated + destroyed + disabled;
  }
  
  public int getFP() {
    return fleetPoints;
  }
  public int getLostFP() {
    return lostFleetPoints;
  }
  
  @Override
  public int compareTo(BattleRecordShipCount o) {
    return this.getFP() - o.getFP();    
  }
  
}
