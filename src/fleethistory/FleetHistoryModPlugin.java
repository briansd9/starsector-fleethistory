/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fleethistory;

import fleethistory.intel.FleetSummaryIntel;
import fleethistory.listeners.ShipRecoveredListener;
import fleethistory.listeners.BattleListener;
import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.thoughtworks.xstream.XStream;
import fleethistory.converters.BattleRecordSideInfoConverter;
import fleethistory.converters.OfficerLogConverter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.log4j.Logger;
import fleethistory.intel.BattleRecordIntel;
import fleethistory.intel.OfficerLogIntel;
import fleethistory.intel.ShipLogIntel;
import fleethistory.listeners.OfficerUpdateListener;
import fleethistory.listeners.ShipBoughtOrSoldListener;
import fleethistory.shipevents.ShipBattleRecord;
import fleethistory.shipevents.ShipRecovery;
import fleethistory.shipevents.ShipTransaction;
import fleethistory.types.BattleRecord;
import fleethistory.types.BattleRecordSideInfo;
import fleethistory.types.BattleRecordExtraInfo;
import fleethistory.types.BattleRecordSideCount;
import fleethistory.types.OfficerLog;
import fleethistory.types.ShipInfo;
import fleethistory.types.ShipLog;
import fleethistory.types.ShipLogEntry;

/**
 *
 * @author joshi
 */
public class FleetHistoryModPlugin extends BaseModPlugin {

  private static final Logger log = Global.getLogger(FleetHistoryModPlugin.class);
  private static final Class[] fleetHistoryIntelClasses = {
    BattleRecordIntel.class,
    FleetSummaryIntel.class,
    ShipLogIntel.class,
    OfficerLogIntel.class
  };

  private static BattleListener battleListener;
  private static ShipBoughtOrSoldListener shipBoughtOrSoldListener;
  private static OfficerUpdateListener officerUpdateListener;
  private static ShipRecoveredListener shipRecoveredListener;

  @Override
  public void onGameLoad(boolean newGame) {

    log.info("Started fleet history plugin ongameload!");

    battleListener = new BattleListener();
    shipBoughtOrSoldListener = new ShipBoughtOrSoldListener();
    officerUpdateListener = new OfficerUpdateListener();
    shipRecoveredListener = new ShipRecoveredListener();

    Global.getSector().addTransientListener(battleListener);
    Global.getSector().addTransientListener(shipBoughtOrSoldListener);
    Global.getSector().getListenerManager().addListener(shipRecoveredListener, true);
    Global.getSector().addTransientScript(officerUpdateListener);

    IntelManagerAPI manager = Global.getSector().getIntelManager();
    if (!manager.hasIntelOfClass(FleetSummaryIntel.class)) {
      manager.addIntel(new FleetSummaryIntel());
    }

    // set some default display options      
    HashMap<String, Object> pd = U.getPersistentData();
    if (!pd.containsKey(U.FLEET_HISTORY_VIEW_MODE)) {
      pd.put(U.FLEET_HISTORY_VIEW_MODE, U.FLEET_HISTORY_VIEW_SHIPS);
    }
    if (!pd.containsKey(U.LOG_VIEW_MODE_KEY)) {
      pd.put(U.LOG_VIEW_MODE_KEY, U.BATTLE_LOG);
    }
    if (!pd.containsKey(U.FLEET_HISTORY_BATTLE_SIZE)) {
      pd.put(U.FLEET_HISTORY_BATTLE_SIZE, 0);
    }
    if (!pd.containsKey(U.FLEET_HISTORY_BATTLE_AGE)) {
      pd.put(U.FLEET_HISTORY_BATTLE_AGE, 0);
    }
    if (!pd.containsKey(U.FLEET_HISTORY_SHIP_BATTLE_COUNT)) {
      pd.put(U.FLEET_HISTORY_SHIP_BATTLE_COUNT, 0);
    }
    if (!pd.containsKey(U.FLEET_HISTORY_SHIP_FP_SCORE)) {
      pd.put(U.FLEET_HISTORY_SHIP_FP_SCORE, 0);
    }
    if (!pd.containsKey(U.FLEET_HISTORY_KILL_DISPLAY)) {
      pd.put(U.FLEET_HISTORY_KILL_DISPLAY, U.KILL_DISPLAY_ICONS);
    }

  }
  
  @Override
  public void beforeGameSave() {
    // make sure no leftover data gets written to the save file in the first place! >:\
    U.clearTempBattleData();
  }

  @Override
  public void configureXStream(XStream x) {

    x.alias("FHmain", FleetSummaryIntel.class);

    x.alias("FHscm", StringCache.class);
    StringCache.alias(x);

    x.alias("FH0", BattleRecordIntel.class);
    BattleRecordIntel.alias(x);

    x.alias("FH1", ShipLog.class);
    ShipLog.alias(x);

    x.alias("FH2", ShipLogEntry.class);
    ShipLogEntry.alias(x);

    x.alias("FH3", ShipBattleRecord.class);
    ShipBattleRecord.alias(x);

    x.alias("FH4", ShipLogIntel.class);
    ShipLogIntel.alias(x);

    x.alias("FH5", BattleRecord.class);
    BattleRecord.alias(x);

    x.alias("FH6", ShipInfo.class);
    ShipInfo.alias(x);

    x.alias("FH7", ShipTransaction.class);
    ShipTransaction.alias(x);

    x.alias("FH8", ShipRecovery.class);
    ShipRecovery.alias(x);

    x.alias("FH9", BattleRecordExtraInfo.class);
    BattleRecordExtraInfo.alias(x);

    x.alias("FHa", BattleRecordSideInfo.class);
    x.registerConverter(new BattleRecordSideInfoConverter());

    x.alias("FHb", BattleRecordSideCount.class);
    BattleRecordSideCount.alias(x);

    x.alias("FHc", OfficerLog.class);
    x.registerConverter(new OfficerLogConverter());

    x.alias("FHd", OfficerLogIntel.class);
    OfficerLogIntel.alias(x);

  }

  public static void clearAllData() {

    log.info("Clearing all fleet history data");

    Global.getSector().removeListener(battleListener);
    Global.getSector().removeListener(shipBoughtOrSoldListener);
    Global.getSector().getListenerManager().removeListener(shipRecoveredListener);
    Global.getSector().removeTransientScript(officerUpdateListener);

    IntelManagerAPI manager = Global.getSector().getIntelManager();
    List<IntelInfoPlugin> intelToRemove = new ArrayList<>();
    for (Class intelClass : fleetHistoryIntelClasses) {
      log.info("Getting all intel items of class " + intelClass.getName());
      for (IntelInfoPlugin i : manager.getIntel(intelClass)) {
        intelToRemove.add(i);
      }
    }
    log.info("Removing all fleet history intel items from intel manager");
    for (IntelInfoPlugin i : intelToRemove) {
      manager.removeIntel(i);
    }

    log.info("Removing all saved data from sector persistent data");
    Global.getSector().getPersistentData().remove(U.DATA_KEY);

    log.info("Done clearing fleet history data");

  }

}
