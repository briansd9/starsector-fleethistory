/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fleethistory.types;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.thoughtworks.xstream.XStream;
import fleethistory.U;

/**
 *
 * @author joshi
 */
public class ShipInfo {

  private String shipName;
  private String hullId;
  public float damagePct;

  public ShipInfo(FleetMemberAPI ship, boolean isEnemyShip) {    
    if (!isEnemyShip) {
      this.setShipName(ship.getShipName());
    }
    ShipHullSpecAPI hull = ship.getHullSpec();
    String tempHullId = hull.getDParentHullId();
    if(tempHullId == null) {
      tempHullId = hull.getHullId();
    }
    this.setHullId(tempHullId);
  }
  
  public final void setShipName(String str) {
    this.shipName = U.getCache().cacheString(str);
  }  
  public String getShipName() {
    return U.getCache().getCachedString(this.shipName);
  }
  
  public final void setHullId(String str) {
    this.hullId = U.getCache().cacheString(str);
  }
  public String getHullId() {
    return U.getCache().getCachedString(this.hullId);
  }

  public ShipInfo(FleetMemberAPI ship) {
    this(ship, false);
  }

  public ShipInfo(FleetMemberAPI ship, float dmg, float maxHP) {
    this(ship, true);
    this.damagePct = (float) Math.round(dmg * 100f / maxHP) / 100f;
  }

  public static void alias(XStream x) {
    x.aliasAttribute(ShipInfo.class, "shipName", "s");
    x.aliasAttribute(ShipInfo.class, "hullId", "h");
    x.aliasAttribute(ShipInfo.class, "damagePct", "d");
  }

  public ShipHullSpecAPI getHullSpec() {
    return Global.getSettings().getHullSpec(this.getHullId());
  }

  @Override
  public String toString() {
    return this.getShipName() + ", " + this.getHullSpec().getNameWithDesignationWithDashClass();
  }

  public int getFP() {
    return this.getHullSpec().getFleetPoints();
  }

}
