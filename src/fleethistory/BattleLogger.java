package fleethistory;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import java.util.HashMap;
import java.util.List;
import fleethistory.shipevents.ShipBattleRecord;
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
    
    pd.put(U.ENGAGEMENT_START_TIMESTAMP, System.currentTimeMillis());
    log("Starting battle at " + pd.get(U.ENGAGEMENT_START_TIMESTAMP));
    
    if (!pd.containsKey(U.CURR_BATTLE_TIMESTAMP)) {
      pd.put(U.CURR_BATTLE_TIMESTAMP, Global.getSector().getClock().getTimestamp());
      pd.put(U.CURR_BATTLE_ENEMY_SHIP_MAX_HITPOINTS, new HashMap<String, Float>());
      pd.put(U.CURR_BATTLE_CHILD_PARENT_SHIPS, new HashMap<FleetMemberAPI, FleetMemberAPI>());
      pd.put(U.CURR_BATTLE_SHIP_BATTLE_RECORDS, new HashMap<FleetMemberAPI, ShipBattleRecord>());
    }
  }

  @Override
  public void advance(float amount, List<InputEventAPI> events) {

    if (engine == null || engine.isPaused()) {
      return;
    }

    delta += amount;
    if (delta > 1.5) {
      
      HashMap<String, Object> pd = U.getPersistentData();
      HashMap<String, Float> enemyShipMaxHps = (HashMap<String, Float>) pd.get(U.CURR_BATTLE_ENEMY_SHIP_MAX_HITPOINTS);
      HashMap<FleetMemberAPI, FleetMemberAPI> childParentShips = (HashMap<FleetMemberAPI, FleetMemberAPI>) pd.get(U.CURR_BATTLE_CHILD_PARENT_SHIPS);
      
      List<ShipAPI> ships = engine.getShips();
      for (ShipAPI ship : ships) {
        
        FleetMemberAPI fm = ship.getFleetMember();
        if (fm == null) {
          continue;
        }
        
        switch(ship.getOwner()) {
          
          case 0: // player side
            
            // for each newly deployed player ship, get its parent ship if any (fighters, modular ships, etc) 
            ShipAPI parentShip = getParent(ship);
            if (ship == parentShip) {
              continue;
            }
            if (childParentShips.containsKey(ship.getFleetMember())) {
              continue;
            }
            // log(String.format("Adding new child ship pair %s -> %s", ship.getFleetMember().getId(), parentShip.getFleetMember().getId()));
            childParentShips.put(ship.getFleetMember(), parentShip.getFleetMember());
            
            break;
            
          case 1: // enemy side
            
            // for each newly deployed enemy ship, store its current hull points
            if(!enemyShipMaxHps.containsKey(fm.getId())) {
              enemyShipMaxHps.put(fm.getId(), ship.getHitpoints());
            }
            
            // TODO remove before release!
            String s = ship.getFleetMember().getShipName();
            if(s == null) continue;
            if(ship.getLocation().getY() > 9000 && !ship.getCustomData().containsKey(U.DELIMITER)) {
              ship.setCustomData(U.DELIMITER, 1);
              ship.getMutableStats().getAcceleration().modifyFlat("BOO!", 500);
              ship.getMutableStats().getDeceleration().modifyFlat("BOO!", 500);
              ship.getMutableStats().getMaxSpeed().modifyFlat("BOO!", 450);
            } else if(ship.getLocation().getY() < 9000 && ship.getCustomData().containsKey(U.DELIMITER)) {
              log(s + " has entered field, position now " + ship.getLocation().toString());
              ship.getMutableStats().getAcceleration().unmodify("BOO!");
              ship.getMutableStats().getDeceleration().unmodify("BOO!");
              ship.getMutableStats().getMaxSpeed().unmodify("BOO!");
              ship.getVelocity().set(0, 0);
              ship.removeCustomData(U.DELIMITER);
            }            
            if(ship.isRetreating() && !ship.getCustomData().containsKey("NORETREAT")) {
              log(s + " NO RETREAT!");
              ship.setCustomData("NORETREAT", 1);
              ship.setFixedLocation(new Vector2f(ship.getLocation().getX(), ship.getLocation().getY() - 9000));
            }
            // TODO remove before release!
            
            break;
            
          case 100: // hulks
            
            // TODO remove before release!
            log("Ship killed at position " + ship.getLocation().toString());
            engine.removeEntity(ship);
            break;
            
        }
        
      }
      delta = 0;
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
