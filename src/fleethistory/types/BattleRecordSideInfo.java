package fleethistory.types;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import fleethistory.U;
import fleethistory.shipevents.ShipBattleRecord;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BattleRecordSideInfo implements JSONConvertible {

  public transient Map<String, BattleRecordFleetInfo> tempFleets;
  public transient Map<String, BattleRecordPersonInfo> tempOfficers;
  public transient Map<String, BattleRecordShipInfo> tempShips;
  public transient boolean finalized = false;
  
  public transient BattleRecordSideCount deployedCount;
  public transient BattleRecordSideCount lostCount;
  
  public List<BattleRecordFleetInfo> fleets;
  public List<BattleRecordPersonInfo> officers;
  // ships = notable ships (player-controlled), stored individually
  public List<BattleRecordShipInfo> ships;
  // shipCounts = other ships, grouped by hull type
  public List<BattleRecordShipCount> shipCounts;
  public BattleRecordFighterCount fighters;

  public BattleRecordSideInfo() {
    this.tempFleets = new LinkedHashMap<>();
    this.tempOfficers = new LinkedHashMap<>();
    this.tempShips = new LinkedHashMap<>();
  }

  @Override
  public JSONObject toJSONObject() {

    JSONObject obj = new JSONObject();    

    try {

      JSONArray f = new JSONArray();
      for(BattleRecordFleetInfo fleet : this.fleets) {
        f.put(fleet.getCompressedString());
      }
      obj.put("f", f);
      
      JSONArray o = new JSONArray();
      for(BattleRecordPersonInfo officer : this.officers) {
        o.put(officer.getCompressedString());
      }
      obj.put("o", o);
      
      JSONArray s = new JSONArray();
      for(BattleRecordShipInfo ship : this.ships) {
        s.put(ship.getCompressedString());
      }
      obj.put("s", s);

      JSONArray c = new JSONArray();
      for(BattleRecordShipCount shipCount : this.shipCounts) {
        c.put(shipCount.getCompressedString());
      }
      obj.put("c", c);
      
      if(this.fighters != null) {
        obj.put("d", this.fighters.getCompressedString());
      }
      
    } catch(JSONException e) {
      Logger.getLogger(BattleRecordSideInfo.class).error(e.getMessage(), e);
    }

    return obj;
    
  }
  
  public void finalizeStats() {
    
    if(this.finalized) {
      Logger.getLogger(this.getClass()).info("BattleRecordSideInfo already finalized");
      return;
    }

    this.fleets = new ArrayList(this.tempFleets.values());
    this.officers = new ArrayList(this.tempOfficers.values());
    this.ships = new ArrayList<>();

    HashMap<String, BattleRecordShipCount> tempShipCount = new HashMap<>();
    for (BattleRecordShipInfo s : this.tempShips.values()) {
      if (s.getName() != null) {
        this.ships.add(s);
      } else {
        String hullId = s.getHullId();
        if (!tempShipCount.containsKey(hullId)) {
          tempShipCount.put(hullId, new BattleRecordShipCount(hullId));
        }
        BattleRecordShipCount count = tempShipCount.get(hullId);
        if (s.status != null) {
          count.fleetPoints += s.getFP();
          switch (s.status) {
            case ShipBattleResult.DEPLOYED:
              count.deployed++;
              break;
            case ShipBattleResult.DESTROYED:
              count.destroyed++;
              count.lostFleetPoints += s.getFP();
              break;
            case ShipBattleResult.DISABLED:
              count.disabled++;
              count.lostFleetPoints += s.getFP();
              break;
            case ShipBattleResult.RETREATED:
              count.retreated++;
              break;
          }
        }
      }
    }
    this.shipCounts = new ArrayList(tempShipCount.values());
    
    
    Collections.sort(shipCounts, new Comparator<BattleRecordShipCount>() {
      @Override
      public int compare(BattleRecordShipCount c1, BattleRecordShipCount c2) {
        return c2.getFP() - c1.getFP();
      }
    });
    
    this.finalized = true;

  }

  public void putShip(FleetMemberAPI ship, boolean isPlayerShip, String status) {
    String id = ship.getId();
    if (tempShips.containsKey(id) && status != null) {
      // if already added, update status
      ((BattleRecordShipInfo) tempShips.get(id)).status = status;
    } else {
      BattleRecordShipInfo i = new BattleRecordShipInfo(ship, isPlayerShip);
      i.status = status;
      tempShips.put(id, i);
    }
  }

  public BattleRecordSideCount getDeployedCount() {
    
    if(this.deployedCount != null) {
      return this.deployedCount;
    }

    BattleRecordSideCount bc = new BattleRecordSideCount();
    bc.officers = this.officers.size();
    
    for (BattleRecordShipInfo s : this.ships) {
      bc.ships++;
      bc.fp += s.getFP();
      switch (s.getHullSpec().getHullSize()) {
        case FRIGATE:
          bc.frigates++;
          break;
        case DESTROYER:
          bc.destroyers++;
          break;
        case CRUISER:
          bc.cruisers++;
          break;
        case CAPITAL_SHIP:
          if (s.isStation()) {
            bc.stations++;
          } else {
            bc.capitalShips++;
          }
          break;
      }      
    }

    for (BattleRecordShipCount s : this.shipCounts) {
      int totalShips = s.getDeployed() + s.getRetreated() + s.getLost();      
      bc.ships += totalShips;
      bc.fp += s.getFP();
      ShipHullSpecAPI hull = s.getHullSpec();
      switch (hull.getHullSize()) {
        case FRIGATE:
          bc.frigates += totalShips;
          break;
        case DESTROYER:
          bc.destroyers += totalShips;
          break;
        case CRUISER:
          bc.cruisers += totalShips;
          break;
        case CAPITAL_SHIP:
          if (U.isStation(hull)) {
            bc.stations += totalShips;
          } else {
            bc.capitalShips += totalShips;
          }
          break;
      }      
    }
    
    this.deployedCount = bc;
    return bc;
    
  }

  public BattleRecordSideCount getLostCount() {
    
    if(this.lostCount != null) {
      return this.lostCount;
    }
    
    BattleRecordSideCount bc = new BattleRecordSideCount();
    
    for (BattleRecordPersonInfo p : this.officers) {
      if (ShipBattleResult.isLost(p.shipStatus)) {
        bc.officers++;
      }
    }
    
    for (BattleRecordShipInfo s : this.ships) {
      if(!ShipBattleResult.isLost(s.status)) {
        continue;
      }
      bc.ships++;
      bc.fp += s.getFP();
      ShipHullSpecAPI hull = s.getHullSpec();
      switch (hull.getHullSize()) {
        case FRIGATE:
          bc.frigates++;
          break;
        case DESTROYER:
          bc.destroyers++;
          break;
        case CRUISER:
          bc.cruisers++;
          break;
        case CAPITAL_SHIP:
          if (U.isStation(hull)) {
            bc.stations++;
          } else {
            bc.capitalShips++;
          }
          break;
      }
    }

    for (BattleRecordShipCount s : this.shipCounts) {
      int lost = s.getLost();
      if (lost == 0) {
        continue;
      }
      bc.ships += lost;
      bc.fp += s.getLostFP();
      ShipHullSpecAPI hull = s.getHullSpec();
      switch (hull.getHullSize()) {
        case FRIGATE:
          bc.frigates += lost;
          break;
        case DESTROYER:
          bc.destroyers += lost;
          break;
        case CRUISER:
          bc.cruisers += lost;
          break;
        case CAPITAL_SHIP:
          if (U.isStation(hull)) {
            bc.stations += lost;
          } else {
            bc.capitalShips += lost;
          }
          break;
      }
    }
    
    if(this.fighters != null && this.fighters.getCount() != null) {      
      Map<String, Integer> lostFighterCount = this.fighters.getCount();
      for(String hullID : lostFighterCount.keySet()) {
        bc.fighters += lostFighterCount.get(hullID);
      }
    }
    
    this.lostCount = bc;
    return bc;
    
  }

}
