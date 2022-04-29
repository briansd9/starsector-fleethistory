package fleethistory.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CombatDamageData;
import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import fleethistory.types.BattleRecord;
import fleethistory.shipevents.ShipBattleRecord;
import fleethistory.types.ShipInfo;
import fleethistory.types.ShipLog;
import fleethistory.U;
import fleethistory.types.ShipBattleResult;
import fleethistory.types.ShipLogEntry;

public class BattleListener extends BaseCampaignEventListener {

  private static final Logger log = Global.getLogger(BattleListener.class);

  public BattleListener() {
    super(false);
    //log.info("BattleListener instantiated");
  }

  @Override
  public void reportPlayerEngagement(EngagementResultAPI result) {
    HashMap<String, Float> enemyShipMaxHps = (HashMap<String, Float>) U.getPersistentData().get(U.CURR_BATTLE_ENEMY_SHIP_MAX_HITPOINTS);
    if (enemyShipMaxHps == null || enemyShipMaxHps.isEmpty() || !U.getPersistentData().containsKey(U.MANUAL_BATTLE_INDICATOR)) {
      log.info("autoresolved - will not create battle records");
      return;
    }
    if(result.getLastCombatDamageData() == null) {
      log.info("no combat damage data - will not create battle records");
      return;
    };
    logCombatResults(result);
    U.getPersistentData().remove(U.MANUAL_BATTLE_INDICATOR);
  }

  private void logCombatResults(EngagementResultAPI result) {
    if (!U.getPersistentData().containsKey(U.CURR_BATTLE_SHIP_BATTLE_RECORDS)) {
      log.info("Ship battle records not found in persistent data! Returning");
      return;
    }
    logPlayerFleetOutcome(result);
    logPlayerKills(result);
  }

  private void logPlayerFleetOutcome(EngagementResultAPI result) {

    EngagementResultForFleetAPI playerFleet, enemyFleet;
    if (result.didPlayerWin()) {
      playerFleet = result.getWinnerResult();
      enemyFleet = result.getLoserResult();
    } else {
      playerFleet = result.getLoserResult();
      enemyFleet = result.getWinnerResult();
    }

    BattleRecord br;
    String battleRecordKey;

    // use existing battle record, if found
    if (U.getPersistentData().containsKey(U.CURR_BATTLE_RECORD_KEY)) {

      battleRecordKey = (String) U.getPersistentData().get(U.CURR_BATTLE_RECORD_KEY);
      br = U.getBattleRecords().get(battleRecordKey);

    } else {

      // unique key for battle - should stay consistent between engagements in a battle
      battleRecordKey = U.getBattleRecordKey();
      log.info("Storing current battle record key: " + battleRecordKey);
      U.getPersistentData().put(U.CURR_BATTLE_RECORD_KEY, battleRecordKey);

      String enemyFactionId = enemyFleet.getFleet().getFaction().getId();
      String enemyFleetName = enemyFleet.getFleet().getNameWithFactionKeepCase();
      String battleLocation = result.getBattle().getPlayerCombined().getContainingLocation().getNameWithTypeIfNebula();

      // check if fight took place at an orbital station    
      boolean battleWasAtStation = false;
      for (FleetMemberAPI f : playerFleet.getFleet().getMembersWithFightersCopy()) {
        if (f.isStation()) {
          if(f.getShipName() != null) {
            battleLocation = f.getShipName();
          }
          battleWasAtStation = true;
          enemyFleetName = null;
        }
      }
      for (FleetMemberAPI f : enemyFleet.getFleet().getMembersWithFightersCopy()) {
        if (f.isStation()) {
          if(f.getShipName() != null) {
            battleLocation = f.getShipName();
          }
          battleWasAtStation = true;
          enemyFleetName = null;
        }
      }

      // in-game date (not guaranteed to be unique - multiple battles for a single timestamp are possible)
      long timestamp = Global.getSector().getClock().getTimestamp();

      br = new BattleRecord(battleRecordKey, timestamp, battleLocation, enemyFactionId, enemyFleetName, result.didPlayerWin());
      // TODO to handle battles with multiple engagements: if battlerecord with same timestamp, location, coords already exists, merge
      if (battleWasAtStation) {
        br.inOrAt = U.i18n("at");
      }

    }

    br.setPlayerFleetInfo(result.getBattle().getPlayerSide());
    br.setEnemyFleetInfo(result.getBattle().getNonPlayerSide());
    br.setPlayerFleetStrength(playerFleet);
    br.setEnemyFleetStrength(enemyFleet);
    U.addBattleRecord(battleRecordKey, br);

    // record outcome for each ship deployed
    for (FleetMemberAPI ship : playerFleet.getDeployed()) {
      putShipBattleRecord(br, ship, ShipBattleResult.DEPLOYED);
    }
    for (FleetMemberAPI ship : playerFleet.getDestroyed()) {
      putShipBattleRecord(br, ship, ShipBattleResult.DESTROYED);
    }
    for (FleetMemberAPI ship : playerFleet.getDisabled()) {
      putShipBattleRecord(br, ship, ShipBattleResult.DISABLED);
    }
    for (FleetMemberAPI ship : playerFleet.getRetreated()) {
      putShipBattleRecord(br, ship, ShipBattleResult.RETREATED);
    }

  }

  private void putShipBattleRecord(BattleRecord br, FleetMemberAPI ship, String result) {

    HashMap<FleetMemberAPI, ShipBattleRecord> shipBattleRecords = (HashMap<FleetMemberAPI, ShipBattleRecord>) U.getPersistentData().get(U.CURR_BATTLE_SHIP_BATTLE_RECORDS);

    if (shipBattleRecords.containsKey(ship)) {
      // if ship was already deployed in this battle, just update outcome and health values
      ShipBattleRecord existingRecord = (ShipBattleRecord) shipBattleRecords.get(ship);
      existingRecord.result = result;
      if (result.equals(ShipBattleResult.DEPLOYED) || result.equals(ShipBattleResult.RETREATED)) {
        // 2022-04-29 try to detect what's breaking getRepairednessFraction
        try {
          existingRecord.health = Float.parseFloat(U.format(ship.getRepairTracker().computeRepairednessFraction()));
        } catch(NumberFormatException e) {
          existingRecord.health = 1000;
        }
      }
    } else {
      ShipBattleRecord sbr = new ShipBattleRecord(br.id, result);
      if (result.equals(ShipBattleResult.DEPLOYED) || result.equals(ShipBattleResult.RETREATED)) {
        // 2022-04-29 try to detect what's breaking getRepairednessFraction
        try {
          sbr.health = Float.parseFloat(U.format(ship.getRepairTracker().computeRepairednessFraction()));
        } catch(NumberFormatException e) {
          sbr.health = 1000;
        }
      }
      shipBattleRecords.put(ship, sbr);
    }
  }

  private void logPlayerKills(EngagementResultAPI result) {

    HashMap<String, Float> enemyShipMaxHps = (HashMap<String, Float>) U.getPersistentData().get(U.CURR_BATTLE_ENEMY_SHIP_MAX_HITPOINTS);
    HashMap<FleetMemberAPI, FleetMemberAPI> childParentShips = (HashMap<FleetMemberAPI, FleetMemberAPI>) U.getPersistentData().get(U.CURR_BATTLE_CHILD_PARENT_SHIPS);
//    log.info("enemy ship max hps:");
//    log.info(enemyShipMaxHps.toString());
//    log.info("child parent ships:");
//    log.info(childParentShips.toString());

    EngagementResultForFleetAPI playerFleet, enemyFleet;
    if (result.didPlayerWin()) {
      playerFleet = result.getWinnerResult();
      enemyFleet = result.getLoserResult();
    } else {
      playerFleet = result.getLoserResult();
      enemyFleet = result.getWinnerResult();
    }

    HashMap<String, FleetMemberAPI> playerShipsByFleetMemberId = new HashMap<>();
    for (DeployedFleetMemberAPI d : playerFleet.getAllEverDeployedCopy()) {
      // only count ships actually owned by player
      if (!d.isAlly()) {
        playerShipsByFleetMemberId.put(d.getMember().getId(), d.getMember());
      }
    }

    Map<FleetMemberAPI, CombatDamageData.DealtByFleetMember> damageData = result.getLastCombatDamageData().getDealt();
    List<FleetMemberAPI> enemiesDisabled = enemyFleet.getDisabled();
    List<FleetMemberAPI> enemiesDestroyed = enemyFleet.getDestroyed();

    HashMap<FleetMemberAPI, HashMap<FleetMemberAPI, Float>> damageDealtByShip = new HashMap<>();

    for (FleetMemberAPI f : damageData.keySet()) {

      if (f.getOwner() == 0 && !f.isAlly()) {

        //log.info("Logging data for " + f.getShipName() + " " + f.getHullSpec().getNameWithDesignationWithDashClass());

        // handle fighter kills - credited to carrier
        FleetMemberAPI creditedShip = (childParentShips.containsKey(f) ? (FleetMemberAPI) childParentShips.get(f) : f);
        //log.info("Credited ship is " + creditedShip.getShipName() + " " + creditedShip.getHullSpec().getNameWithDesignationWithDashClass());

        if (!damageDealtByShip.containsKey(creditedShip)) {
          damageDealtByShip.put(creditedShip, new HashMap<FleetMemberAPI, Float>());
        }
        HashMap<FleetMemberAPI, Float> damageDealtToEnemyShips = damageDealtByShip.get(creditedShip);

        Map<FleetMemberAPI, CombatDamageData.DamageToFleetMember> damage = ((CombatDamageData.DealtByFleetMember) damageData.get(f)).getDamage();
        for (FleetMemberAPI target : damage.keySet()) {
          //log.info("Found a target: " + target.getShipName() + " " + target.getHullSpec().getNameWithDesignationWithDashClass());
          
          // ignore targets that weren't killed
          if (!enemiesDisabled.contains(target) && !enemiesDestroyed.contains(target)) {
            continue;
          }
          // ignore ships with no hp record (fighters, etc.)
          if (!enemyShipMaxHps.containsKey(target.getId())) {
            continue;
          }

          if (!damageDealtToEnemyShips.containsKey(target)) {
            damageDealtToEnemyShips.put(target, 0f);
          }
          float currentDamage = damageDealtToEnemyShips.get(target);
          float dmg = ((CombatDamageData.DamageToFleetMember) damage.get(target)).hullDamage;
          damageDealtToEnemyShips.put(target, currentDamage + dmg);

        }

      }

    }

    HashMap<FleetMemberAPI, ShipBattleRecord> shipBattleRecords = (HashMap<FleetMemberAPI, ShipBattleRecord>) U.getPersistentData().get(U.CURR_BATTLE_SHIP_BATTLE_RECORDS);

    for (FleetMemberAPI playerShip : damageDealtByShip.keySet()) {

      //log.info("Counting kills for " + playerShip.getShipName() + " " + playerShip.getHullSpec().getNameWithDesignationWithDashClass());

      ShipBattleRecord sbr = (ShipBattleRecord) shipBattleRecords.get(playerShip);
      if (sbr == null) {
        continue;
      }

      HashMap<FleetMemberAPI, Float> enemyShipsDamaged = (HashMap<FleetMemberAPI, Float>) damageDealtByShip.get(playerShip);

      for (FleetMemberAPI target : enemyShipsDamaged.keySet()) {

        float dmg = enemyShipsDamaged.get(target);
        sbr.totalDamageDealt += Math.round(dmg);

        float maxHP = enemyShipMaxHps.get(target.getId());
//        log.info(
//                String.format(
//                        "%s took %s of %s damage (%s)",
//                        target.getShipName() + " " + target.getHullSpec().getNameWithDesignationWithDashClass(),
//                        dmg,
//                        maxHP,
//                        U.format(dmg * 100 / maxHP)
//                )
//        );

        // 80% damage = kill; 20% damage = assist
        if (dmg / maxHP > 0.8) {
          //log.info("KILLED!");
          sbr.addShip(new ShipInfo(target, dmg, maxHP));
        } else if (dmg / maxHP > 0.2) {
          //log.info("assisted");
          sbr.addShip(new ShipInfo(target, dmg, maxHP));
        }
      }

    }

  }

  @Override
  public void reportBattleFinished(CampaignFleetAPI primaryWinner, BattleAPI battle) {
    
    if(!battle.isPlayerInvolved()) {
      U.clearTempBattleData();
      return;
    }

    // store timestamp of last completed battle
    long timestamp = Global.getSector().getClock().getTimestamp();
    U.getPersistentData().put(U.LAST_COMBAT, timestamp);

    // store ship battle records permanently
    HashMap<FleetMemberAPI, ShipBattleRecord> shipBattleRecords = (HashMap<FleetMemberAPI, ShipBattleRecord>) U.getPersistentData().get(U.CURR_BATTLE_SHIP_BATTLE_RECORDS);
    
    // null check because reportBattleFinished is sometimes called more than once???
    if (shipBattleRecords != null) {
      for (FleetMemberAPI f : shipBattleRecords.keySet()) {
        
        if (f.isAlly()) {
          continue;
        }

        ShipBattleRecord sbr = (ShipBattleRecord) shipBattleRecords.get(f);

        // check if recovered from previous battle
        if (ShipBattleResult.isLost(sbr.result)) {
          ShipLog shipLog = U.getShipLogFor(f);
          if (!shipLog.events.isEmpty()) {
            ShipLogEntry lastEntry = shipLog.events.get(shipLog.events.size() - 1);
            // if yes, delete previous "recovered" entry and set status for this entry instead
            if (lastEntry.type == ShipLogEntry.EventType.RECOVERED && lastEntry.getTimestamp() == timestamp) {
              sbr.recovered = true;
              shipLog.events.remove(lastEntry);
            }
          }
        }

        sbr.finalizeStats();
        U.addShipEvent(f, timestamp, ShipLogEntry.EventType.COMBAT, sbr);
        
        try {
        
          PersonAPI captain = f.getCaptain();
          if(!captain.isDefault()) {
            U.addOfficerBattleEntry(captain, f.getId(), timestamp, sbr.battleRecordId);
            U.getOfficerLogFor(captain).update(captain);
          }
          
        } catch(Exception e) {
          e.printStackTrace();
        }
        
      }
    }

    String lastBattleKey = (String) U.getPersistentData().get(U.CURR_BATTLE_RECORD_KEY);
    log.info("Last battle record key is: " + lastBattleKey);
    BattleRecord finishedBattle = U.getBattleRecords().get(lastBattleKey);
    if (finishedBattle != null) {
      log.info("Finalizing stats for " + lastBattleKey);
      finishedBattle.checkPersonBounty();
      finishedBattle.finalizeStats();
      U.updateBattleRecordIntel(lastBattleKey);
    } else {
      log.error("!!!!!!!!!! No battle record found for " + lastBattleKey);
    }

    // clean up all temp data used in this battle
    U.clearTempBattleData();
    
  }

}
