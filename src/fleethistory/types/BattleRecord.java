/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fleethistory.types;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.intel.PersonBountyIntel;
import com.thoughtworks.xstream.XStream;
import fleethistory.U;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author joshi
 */
public class BattleRecord {

  public String id;
  private String timestamp;
  private String location;
  private String enemyFactionId;
  private String enemyFleetName;
  public BattleRecordSideInfo playerSide;
  public BattleRecordSideInfo enemySide;
  public String inOrAt = U.i18n("in");  // in <star system>; at <station>
  public boolean playerWon = false;
  public List<BattleRecordExtraInfo> extraInfo;

  public BattleRecord(String id, long timestamp, String location, String enemyFactionId, String enemyFleetName, boolean playerWon) {
    this.id = id;
    this.setTimestamp(timestamp);
    this.setLocation(location);
    this.setEnemyFactionId(enemyFactionId);
    this.setEnemyFleetName(enemyFleetName);
    this.playerSide = new BattleRecordSideInfo();
    this.enemySide = new BattleRecordSideInfo();
    this.playerWon = playerWon;
  }

  public static void alias(XStream x) {
    String[] aliases = {
      "id", "a",
      "timestamp", "t",
      "location", "l",
      "enemyFactionId", "e",
      "enemyFleetName", "n",
      "playerSide", "p",
      "enemySide", "m",
      "inOrAt", "i",
      "playerWon", "w",
      "extraInfo", "x"
    };
    for (int i = 0; i < aliases.length; i += 2) {
      x.aliasAttribute(BattleRecord.class, aliases[i], aliases[i + 1]);
    }
  }
  
  public final void setTimestamp(long timestamp) {
    this.timestamp = U.encodeNum(timestamp);
  }
  public final long getTimestamp() {
    return U.decodeNum(timestamp);
  }

  public final void setLocation(String str) {
    this.location = U.getCache().cacheString(str);
  }

  public String getLocation() {
    return U.getCache().getCachedString(this.location);
  }

  public final void setEnemyFactionId(String str) {
    this.enemyFactionId = U.getCache().cacheString(str);
  }

  public String getEnemyFactionId() {
    return U.getCache().getCachedString(this.enemyFactionId);
  }

  public final void setEnemyFleetName(String str) {
    this.enemyFleetName = U.getCache().cacheString(str);
  }

  public String getEnemyFleetName() {
    return U.getCache().getCachedString(this.enemyFleetName);
  }

  public String getEnemyFleetDisplayName() {
    if (this.getEnemyFleetName() == null || this.getEnemyFleetName().equals("null")) {
      return Global.getSector().getFaction(this.getEnemyFactionId()).getDisplayName();
    } else {
      return this.getEnemyFleetName();
    }
  }

  public void setPlayerFleetInfo(List<CampaignFleetAPI> fleets) {
    setFleetInfo(this.playerSide, fleets);
  }

  public void setEnemyFleetInfo(List<CampaignFleetAPI> fleets) {
    setFleetInfo(this.enemySide, fleets);
  }

  public void setFleetInfo(BattleRecordSideInfo side, List<CampaignFleetAPI> fleets) {

    for (CampaignFleetAPI c : fleets) {

      if (side.tempFleets.containsKey(c.getId())) {
        continue;
      }

      // default for enemy fleets: fleet name with faction prefix
      String fleetName = c.getNameWithFactionKeepCase();
      if (c.isPlayerFleet()) {
        if (c.getFaction().getDisplayName().equals("player")) {
          // if no player-founded faction yet, just use "Playername's Fleet"
          fleetName = String.format(U.i18n("player_fleet_name"), Global.getSector().getPlayerPerson().getNameString());
        } else {
          fleetName = c.getFullName();
        }
      }
      side.tempFleets.put(c.getId(), new BattleRecordFleetInfo(fleetName, c.getFaction().getId()));

    }

  }

  public void setPlayerFleetStrength(EngagementResultForFleetAPI result) {
    setFleetStrength(this.playerSide, result);
    setCaptains(this.playerSide, result);
  }

  public void setEnemyFleetStrength(EngagementResultForFleetAPI result) {
    setFleetStrength(this.enemySide, result);
    setCaptains(this.enemySide, result);
  }

  public void setCaptains(BattleRecordSideInfo side, EngagementResultForFleetAPI result) {
    
    HashMap<String, Integer> coreCounts = new HashMap<>();
    HashMap<String, String> coreSprites = new HashMap<>();

    for (DeployedFleetMemberAPI df : result.getAllEverDeployedCopy()) {
      if(df.getMember() == null) {
        continue;
      }
      PersonAPI p = df.getMember().getCaptain();
      if (p.isAICore()) {
        String coreName = p.getNameString();
        if (!coreCounts.containsKey(coreName)) {
          coreCounts.put(coreName, 1);
          coreSprites.put(coreName, p.getPortraitSprite());
        } else {
          coreCounts.put(coreName, 1 + coreCounts.get(coreName));
        }
      } else if (!p.isDefault()) {

        if (side.tempOfficers.containsKey(p.getId())) {
          BattleRecordPersonInfo existingPersonInfo = side.tempOfficers.get(p.getId());
          // update existing person info, if any
          if (result.getDestroyed().contains(df.getMember())) {
            existingPersonInfo.shipStatus = ShipBattleResult.DESTROYED;
          } else if (result.getDisabled().contains(df.getMember())) {
            existingPersonInfo.shipStatus = ShipBattleResult.DISABLED;
          }
        } else {
          BattleRecordPersonInfo info = new BattleRecordPersonInfo(p.getNameString(), p.getPortraitSprite());
          info.setShip(df.getMember());
          if (result.getDestroyed().contains(df.getMember())) {
            info.shipStatus = ShipBattleResult.DESTROYED;
          } else if (result.getDisabled().contains(df.getMember())) {
            info.shipStatus = ShipBattleResult.DISABLED;
          }
          if (df.getMember().isFlagship()) {
            info.isFleetCommander = true;
          }
          side.tempOfficers.put(p.getId(), info);
        }

      }
    }

    if (!coreCounts.isEmpty()) {
      for (String key : coreCounts.keySet()) {
        // don't differentiate AI cores in officers section
        BattleRecordPersonInfo core = new BattleRecordPersonInfo(coreCounts.get(key) + "x " + key, coreSprites.get(key));
        side.tempOfficers.put(key, core);
      }
    }

  }

  public void setFleetStrength(BattleRecordSideInfo side, EngagementResultForFleetAPI result) {
    List<FleetMemberAPI> playerFleet = Global.getSector().getPlayerFleet().getMembersWithFightersCopy();
    for (FleetMemberAPI f : result.getDeployed()) {
      boolean isPlayerShip = playerFleet.contains(f);
      side.putShip(f, isPlayerShip, ShipBattleResult.DEPLOYED);
    }
    for (FleetMemberAPI f : result.getDestroyed()) {
      boolean isPlayerShip = playerFleet.contains(f);
      side.putShip(f, isPlayerShip, ShipBattleResult.DESTROYED);
    }
    for (FleetMemberAPI f : result.getDisabled()) {
      boolean isPlayerShip = playerFleet.contains(f);
      side.putShip(f, isPlayerShip, ShipBattleResult.DISABLED);
    }
    for (FleetMemberAPI f : result.getRetreated()) {
      boolean isPlayerShip = playerFleet.contains(f);
      side.putShip(f, isPlayerShip, ShipBattleResult.RETREATED);
    }
  }

  public void checkPersonBounty() {

    ArrayList<String> completedBountyTargets = new ArrayList<>();
    for (IntelInfoPlugin i : Global.getSector().getIntelManager().getIntel(PersonBountyIntel.class)) {
      String bountyString = ((PersonBountyIntel) i).getName();
      if (bountyString.startsWith("Bounty Completed - ")) {
        completedBountyTargets.add(bountyString.replace("Bounty Completed - ", ""));
      }
    }

    for (BattleRecordPersonInfo p : this.enemySide.tempOfficers.values()) {
      if (ShipBattleResult.isLost(p.shipStatus)) {
        if (completedBountyTargets.contains(p.getName())) {
          addExtraInfo(BattleRecordExtraInfo.BOUNTY_COMPLETED, p.getName());
        }
      }
    }
  }

  public void addExtraInfo(String key, String data) {
    if (this.extraInfo == null) {
      this.extraInfo = new ArrayList<>();
    }
    this.extraInfo.add(new BattleRecordExtraInfo(key, data));
  }

  public void finalizeStats() {
    this.playerSide.finalizeStats();
    this.enemySide.finalizeStats();
  }

}
