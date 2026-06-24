package fleethistory;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import java.util.HashMap;
import java.util.List;
import fleethistory.shipevents.ShipBattleRecord;
import fleethistory.types.BattleRecordFighterCount;
import org.lwjgl.util.vector.Vector2f;

public class BattleLogger extends BaseEveryFrameCombatPlugin {

  private CombatEngineAPI engine;
  private static float delta = 1.5f;

  @Override
  public void init(CombatEngineAPI e) {

    log("Battle logger initialized");

    this.engine = e;

    HashMap<String, Object> pd = U.getPersistentData();
    pd.put(U.MANUAL_BATTLE_INDICATOR, 1);

    // battle durations always reset on init, no need to keep running total
    // running total is tracked in BattleListener
    pd.put(U.ENGAGEMENT_DURATION, 0f);
    pd.put(U.CURR_BATTLE_SHIP_TIMES, new HashMap<FleetMemberAPI, Float>());

    if (!pd.containsKey(U.CURR_BATTLE_TIMESTAMP)) {
      
      pd.put(U.CURR_BATTLE_TIMESTAMP, Global.getSector().getClock().getTimestamp());
      pd.put(U.CURR_BATTLE_ENEMY_SHIP_MAX_HITPOINTS, new HashMap<String, Float>());
      pd.put(U.CURR_BATTLE_CHILD_PARENT_SHIPS, new HashMap<FleetMemberAPI, FleetMemberAPI>());
      pd.put(U.CURR_BATTLE_SHIP_BATTLE_RECORDS, new HashMap<FleetMemberAPI, ShipBattleRecord>());
      
      BattleRecordFighterCount[] arr = new BattleRecordFighterCount[2];
      arr[0] = new BattleRecordFighterCount();
      arr[1] = new BattleRecordFighterCount();
      pd.put(U.CURR_BATTLE_FIGHTER_COUNTS, arr);
      
    }
  }

  @Override
  public void advance(float amount, List<InputEventAPI> events) {

    if (engine == null || engine.isPaused()) {
      return;
    }

    delta += amount;
    
    if (delta > 1) {

      HashMap<String, Object> pd = U.getPersistentData();
      
      float currDuration = (float) pd.get(U.ENGAGEMENT_DURATION) + delta;
      pd.put(U.ENGAGEMENT_DURATION, currDuration);

      HashMap<String, Float> enemyShipMaxHps = (HashMap<String, Float>) pd.get(U.CURR_BATTLE_ENEMY_SHIP_MAX_HITPOINTS);
      HashMap<FleetMemberAPI, FleetMemberAPI> childParentShips = (HashMap<FleetMemberAPI, FleetMemberAPI>) pd.get(U.CURR_BATTLE_CHILD_PARENT_SHIPS);
      HashMap<FleetMemberAPI, Float> shipTimestamps = (HashMap<FleetMemberAPI, Float>) pd.get(U.CURR_BATTLE_SHIP_TIMES);
      BattleRecordFighterCount[] fighterCounts = (BattleRecordFighterCount[]) pd.get(U.CURR_BATTLE_FIGHTER_COUNTS);
      
      List<ShipAPI> ships = engine.getShips();
      for (ShipAPI ship : ships) {
        
        if(ship.getWing() != null) {
          if(ship.getOwner() == 100) {            
            // owner 100 = hulk; fighter shot down
            BattleRecordFighterCount f = fighterCounts[ship.getOriginalOwner()];
            f.logLost(ship.getWing().getSpec().getId());
            // log(String.format("KILLED: %s (%s) on side %d", ship.getId(), ship.getWing().getSpec().getId(), ship.getOriginalOwner()));
          } else {
            // not yet tracked - a newly launched fighter
            if(!ship.getCustomData().containsKey("TRACKED")) {
              ship.setCustomData("TRACKED", true);
            }
          }
        }
        
            
        FleetMemberAPI fm = ship.getFleetMember();
        if (fm == null) {
          continue;
        }
        
        switch(ship.getOwner()) {
          
          // player side
          case 0 -> {
              
              // for each newly deployed player ship, get its parent ship if any (fighters, modular ships, etc)
              ShipAPI parentShip = getParent(ship);
              
              // if not a child ship, increment time deployed
              if (ship == parentShip) {
                  FleetMemberAPI f = ship.getFleetMember();
                  if(!shipTimestamps.containsKey(f)) {
                      shipTimestamps.put(f, delta);
                  } else {
                      shipTimestamps.put(f, shipTimestamps.get(f) + delta);
                  }
              }
              
              // otherwise, store child-parent ship pair, for proper crediting of kills to parent ship
              if (childParentShips.containsKey(ship.getFleetMember())) {
                  continue;
              }
              childParentShips.put(ship.getFleetMember(), parentShip.getFleetMember());
          }
            
          // enemy side
          case 1 -> {
              // for each newly deployed enemy ship, store its current hull points
              if(!enemyShipMaxHps.containsKey(fm.getId())) {
                  enemyShipMaxHps.put(fm.getId(), ship.getHitpoints());
              }
          }
          
        }
        
      }
      
      delta--;
      
    }

  }

  private static ShipAPI getParent(ShipAPI ship) {

    if (ship == null) {
      return null;
    }

    ShipAPI parentShip = null;

    // seek recursively to handle exotic modships (fighter wing in ship submodule, etc.)
    if (ship.isFighter() && ship.getWing() != null) {
      parentShip = getParent(ship.getWing().getSourceShip());
    } else if (ship.isDrone()) {
      parentShip = getParent(ship.getDroneSource());
    } else if (ship.isStationModule()) {
      parentShip = getParent(ship.getParentStation());
    }

    return (parentShip == null ? ship : parentShip);

  }

  private static void log(String s) {
    Global.getLogger(BattleLogger.class).info(s);
  }

}
