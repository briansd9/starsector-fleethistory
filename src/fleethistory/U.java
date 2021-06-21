/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fleethistory;

import fleethistory.types.ShipLog;
import fleethistory.types.ShipLogEntry;
import fleethistory.types.ShipInfo;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import org.apache.log4j.Logger;
import fleethistory.intel.BattleRecordIntel;
import fleethistory.intel.OfficerLogIntel;
import fleethistory.intel.ShipLogIntel;
import fleethistory.types.BattleRecord;
import fleethistory.types.OfficerLog;
import fleethistory.types.ShipEvent;
import fleethistory.types.ShipLogEntry.EventType;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 *
 * @author joshi
 */
public class U {

  public static final int LINE_SPACING = 5;
  private static final DecimalFormat d = new DecimalFormat("#.##");
  private static final Logger log = Global.getLogger(U.class);

  public static final String DATA_KEY = "FLEET_HISTORY_PLUGIN_DATA_KEY";
  public static final String STRING_CACHE = "STRING_CACHE";
  public static final String BATTLE_COUNT = "BATTLE_COUNT";

  public static final String SHIP_LOGS_KEY = "SHIP_LOGS";
  public static final String OFFICER_LOGS_KEY = "OFFICER_LOGS";
  public static final String BATTLE_RECORDS_KEY = "BATTLE_RECORDS";

  public static final String LAST_COMBAT = "LAST_COMBAT";

  public static final String CURR_BATTLE_ENEMY_SHIP_MAX_HITPOINTS = "CURR_BATTLE_ENEMY_SHIP_MAX_HITPOINTS";
  public static final String CURR_BATTLE_CHILD_PARENT_SHIPS = "CURR_BATTLE_CHILD_PARENT_SHIPS";
  public static final String CURR_BATTLE_TIMESTAMP = "CURR_BATTLE_TIMESTAMP";
  public static final String CURR_BATTLE_SHIP_BATTLE_RECORDS = "CURR_BATTLE_SHIP_BATTLE_RECORDS";
  public static final String CURR_BATTLE_RECORD_KEY = "CURR_BATTLE_RECORD_KEY";

  public static final String FLEET_HISTORY_VIEW_MODE = "FLEET_HISTORY_VIEW_MODE";
  public static final String FLEET_HISTORY_VIEW_SHIPS = "FLEET_HISTORY_VIEW_SHIPS";
  public static final String FLEET_HISTORY_VIEW_OFFICERS = "FLEET_HISTORY_VIEW_OFFICERS";
  public static final String FLEET_HISTORY_VIEW_BATTLES = "FLEET_HISTORY_VIEW_BATTLES";
  public static final String FLEET_HISTORY_CONFIG = "FLEET_HISTORY_CONFIG";

  public static final String FLEET_HISTORY_BATTLE_SIZE = "FLEET_HISTORY_BATTLE_SIZE";
  public static final String FLEET_HISTORY_BATTLE_AGE = "FLEET_HISTORY_BATTLE_AGE";
  public static final String FLEET_HISTORY_SHIP_BATTLE_COUNT = "FLEET_HISTORY_SHIP_BATTLE_COUNT";
  public static final String FLEET_HISTORY_SHIP_FP_SCORE = "FLEET_HISTORY_SHIP_FP_SCORE";
  public static final String FLEET_HISTORY_KILL_DISPLAY = "FLEET_HISTORY_KILL_DISPLAY";
  public static final String KILL_DISPLAY_ICONS = U.i18n("kill_count_icons");
  public static final String KILL_DISPLAY_TABLE = U.i18n("kill_count_table");
  public static final String KILL_DISPLAY_NONE = U.i18n("kill_count_none");
  public static final String FLEET_HISTORY_HIDE_COMMANDERS = "FLEET_HISTORY_HIDE_COMMANDERS";
  public static final String FLEET_HISTORY_HIDE_DEPLOYED = "FLEET_HISTORY_HIDE_DEPLOYED";
  public static final String FLEET_HISTORY_HIDE_INACTIVE = "FLEET_HISTORY_HIDE_INACTIVE";
  public static final String FLEET_HISTORY_CLEAR_ALL = "FLEET_HISTORY_CLEAR_ALL";

  public static final String BATTLE_LOG = "BATTLE_LOG";
  public static final String KILL_LIST = "KILL_LIST";
  public static final String LOG_VIEW_MODE_KEY = "LOG_VIEW_MODE_KEY";

  public static final String MANUAL_BATTLE_INDICATOR = "MANUAL_BATTLE_INDICATOR";

  public static final String DELIMITER = ";#~";

  // don't use these characters:
  // ;#~| delimiters
  // <>&' require escaping in XML, no space saved
  private static final String DIGITS
          = "0123456789"
          + "abcdefghijklmnopqrstuvwxyz"
          + "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
          + "!@$%^*()"
          + "_=+[]{}:,./?`";
  private static final int BASE = DIGITS.length();

  private static transient Properties strings = null;

  public static String format(float num) {
    return d.format(num);
  }

  public static HashMap<String, Object> getPersistentData() {
    if (!Global.getSector().getPersistentData().containsKey(DATA_KEY)) {
      Global.getSector().getPersistentData().put(DATA_KEY, new HashMap<String, Object>());
    }
    return (HashMap<String, Object>) Global.getSector().getPersistentData().get(DATA_KEY);
  }

//  // use for: captain portraits, hull ids
//  // key = integer key; value = full string
  public static StringCache getCache() {
    HashMap<String, Object> data = getPersistentData();
    if (!data.containsKey(STRING_CACHE)) {
      data.put(STRING_CACHE, new StringCache());
    }
    return (StringCache) data.get(STRING_CACHE);
  }

  public static HashMap<String, OfficerLog> getOfficerLogs() {
    HashMap<String, Object> data = getPersistentData();
    if (!data.containsKey(OFFICER_LOGS_KEY)) {
      data.put(OFFICER_LOGS_KEY, new HashMap<String, OfficerLog>());
    }
    return (HashMap<String, OfficerLog>) data.get(OFFICER_LOGS_KEY);
  }

  public static OfficerLog getOfficerLogFor(PersonAPI officer) {
    HashMap<String, OfficerLog> officerLogs = getOfficerLogs();
    String officerId = officer.getId();
    if (!officerLogs.containsKey(officerId)) {
      log.info("Adding new officer log for " + officer.getNameString());
      officerLogs.put(officerId, new OfficerLog(officer));
    }
    return getOfficerLogs().get(officerId);
  }

  public static OfficerLog getOfficerLogFor(String officerId) {
    return getOfficerLogs().get(officerId);
  }

  public static void addOfficerBattleEntry(PersonAPI officer, String shipId, long timestamp, String battleRecordId) {
    OfficerLog o = getOfficerLogFor(officer);
    Logger.getLogger(OfficerLog.class).info(
            "Adding new officer log entry for " + officer.getNameString() + ": " + shipId + ", " + timestamp + ", " + battleRecordId
    );
    o.addBattleEntry(shipId, timestamp, battleRecordId);
    updateOfficerLogIntel(officer);
  }

  public static void updateOfficerLogIntel(PersonAPI officer) {

    String id = officer.getId();
    IntelManagerAPI manager = Global.getSector().getIntelManager();
    OfficerLogIntel i = new OfficerLogIntel(id);
    if (!manager.hasIntel(i)) {
      log.info("Adding new OfficerLogIntel for id " + id);
      manager.addIntel(i);
    }

  }
  public static HashMap<String, BattleRecord> getBattleRecords() {
    HashMap<String, Object> data = getPersistentData();
    if (!data.containsKey(BATTLE_RECORDS_KEY)) {
      data.put(BATTLE_RECORDS_KEY, new HashMap<String, BattleRecord>());
    }
    return (HashMap<String, BattleRecord>) data.get(BATTLE_RECORDS_KEY);
  }

  public static HashMap<String, ShipLog> getShipLogs() {
    HashMap<String, Object> data = getPersistentData();
    if (!data.containsKey(SHIP_LOGS_KEY)) {
      data.put(SHIP_LOGS_KEY, new HashMap<String, ShipLog>());
    }
    return (HashMap<String, ShipLog>) data.get(SHIP_LOGS_KEY);
  }

  public static ShipLog getShipLogFor(FleetMemberAPI ship) {
    HashMap<String, ShipLog> shipLogs = getShipLogs();
    if (!shipLogs.containsKey(ship.getId())) {
      shipLogs.put(ship.getId(), new ShipLog(ship));
    }
    return getShipLogFor(ship.getId());
  }

  public static ShipLog getShipLogFor(String id) {
    return getShipLogs().get(id);
  }

  public static void addShipEvent(FleetMemberAPI ship, long timestamp, String type, ShipEvent event) {
    addShipEvent(ship, timestamp, type, event, true);
  }

  public static void addShipEvent(FleetMemberAPI ship, long timestamp, String type, ShipEvent event, boolean update) {

    ShipLog shipLog = getShipLogFor(ship);

    if (shipLog.events.size() > 0) {
      ShipLogEntry lastEvent = shipLog.events.get(shipLog.events.size() - 1);
      if (lastEvent.getTimestamp() == timestamp && lastEvent.type.equals(type) && !lastEvent.type.equals(ShipLogEntry.EventType.COMBAT)) {
        log.info(String.format("Duplicate event added for %s: %s, %s", shipLog.info.toString(), timestamp, type));
        return;
      }
    }

    shipLog.events.add(new ShipLogEntry(timestamp, type, event));
    if (update) {
      updateShipLogIntel(ship);
    }

  }

  public static String dateString(long timestamp) {
    CampaignClockAPI c = Global.getSector().getClock().createClock(timestamp);
    return c.getShortDate();
  }

  public static void addBattleRecord(String key, BattleRecord br) {
    if (!U.getBattleRecords().containsKey(key)) {
      U.getBattleRecords().put(key, br);
    } else {
      log.info("BattleRecord [" + key + "] already in list, ignoring");
    }
  }

  public static void updateBattleRecordIntel(String battleRecordId) {
    IntelManagerAPI manager = Global.getSector().getIntelManager();
    BattleRecordIntel i = new BattleRecordIntel(battleRecordId);
    if (!manager.hasIntel(i)) {
      manager.addIntel(i);
    } else {
      log.info("battleRecordIntel with key " + battleRecordId + " already exists, not adding");
    }
  }

  public static void updateShipLogIntel(FleetMemberAPI ship) {

    ShipLog shipLog = getShipLogFor(ship);
    shipLog.info = new ShipInfo(ship);

    if (shipLog.events.isEmpty()) {
      return;
    }

    String id = ship.getId();
    IntelManagerAPI manager = Global.getSector().getIntelManager();
    ShipLogIntel i = new ShipLogIntel(id);
    if (!manager.hasIntel(i)) {
      log.info("Adding new ShipLogIntel for id " + id);
      manager.addIntel(i);
    }

  }

  public static boolean deployedInPreviousBattle(String id) {
    if (!U.getPersistentData().containsKey(LAST_COMBAT) || U.getShipLogFor(id) == null) {
      return false;
    }
    long previousBattleTimestamp = (Long) U.getPersistentData().get(LAST_COMBAT);
    List<ShipLogEntry> shipEvents = U.getShipLogFor(id).events;
    return (shipEvents.size() > 0 && shipEvents.get(shipEvents.size() - 1).getTimestamp() == previousBattleTimestamp);
  }

  public static float hullSizeScalar(HullSize hullSize) {
    switch (hullSize) {
      case DEFAULT:
        return 1f;
      case FIGHTER:
        return .25f;
      case FRIGATE:
        return .49f;
      case DESTROYER:
        return .64f;
      case CRUISER:
        return .81f;
      case CAPITAL_SHIP:
        return 1f;
    }
    return 1f;
  }

  public static String getBattleRecordKey() {

    HashMap<String, Object> pd = U.getPersistentData();
    if (!pd.containsKey(BATTLE_COUNT)) {
      pd.put(BATTLE_COUNT, 0);
    }
    int nextKey = (int) pd.get(BATTLE_COUNT) + 1;
    pd.put(BATTLE_COUNT, nextKey);

    return U.encodeNum(nextKey);

  }

  public static boolean isStation(ShipHullSpecAPI hull) {
    return hull.getHints().contains(ShipHullSpecAPI.ShipTypeHints.STATION);
  }

  // little-endian
  public static String encodeNum(long n) {
    boolean isNegative = (n < 0);
    if (isNegative) {
      n = -n;
    }
    StringBuilder s = new StringBuilder();
    do {
      int digit = (int) (n % BASE);
      s.append(DIGITS.charAt(digit));
      n -= digit;
      n /= BASE;
    } while (n > 0);
    if (isNegative) {
      s.append("-");
    }
    return s.toString();
  }

  public static long decodeNum(String s) {

    if(s.equals("")) {
      return 0;
    }

    long value = 0;
    long placeValue = 1;
    for (int i = 0; i < s.length(); i++) {
      char digit = s.charAt(i);
      if (digit == '-') {
        value = -value;
      } else {
        value += DIGITS.indexOf(digit) * placeValue;
        placeValue *= BASE;
      }
    }
    return value;

  }

  public static void clearTempBattleData() {
    U.getPersistentData().remove(U.CURR_BATTLE_TIMESTAMP);
    U.getPersistentData().remove(U.CURR_BATTLE_ENEMY_SHIP_MAX_HITPOINTS);
    U.getPersistentData().remove(U.CURR_BATTLE_CHILD_PARENT_SHIPS);
    U.getPersistentData().remove(U.CURR_BATTLE_SHIP_BATTLE_RECORDS);
    U.getPersistentData().remove(U.CURR_BATTLE_RECORD_KEY);
  }
  
  public static String i18n(String key) {
    try {
      return Global.getSettings().getString("fleethistory", key);
    } catch(Exception e) {
      log.error("Error getting string with key [" + key + "]: " + e.getMessage());
      return "Error getting string with key [" + key + "]";
    }
  }

}
