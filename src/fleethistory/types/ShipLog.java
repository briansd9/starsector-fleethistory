/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fleethistory.types;

import com.fs.starfarer.api.Global;
import fleethistory.shipevents.ShipBattleRecord;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.thoughtworks.xstream.XStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author joshi
 */
public class ShipLog implements Comparable<ShipLog> {

  public String id;
  public ShipInfo info;
  public List<ShipLogEntry> events;

  public ShipLog(FleetMemberAPI ship) {
    this.id = ship.getId();
    this.info = new ShipInfo(ship);
    this.events = new ArrayList<>();
  }
  
  public static void alias(XStream x) {
    x.aliasAttribute(ShipLog.class, "id", "i");
    x.aliasAttribute(ShipLog.class, "info", "n");
    x.aliasAttribute(ShipLog.class, "events", "e");
  }
  
  public long getTimestamp() {
    return events.get(0).getTimestamp();
  }

  public int getKills() {
    int count = 0;
    for (ShipLogEntry e : events) {
      if (e.type.equals(ShipLogEntry.EventType.COMBAT)) {
        ShipBattleRecord br = (ShipBattleRecord) e.event;
        count += br.getKills();
      }
    }
    return count;
  }

  public int getAssists() {
    int count = 0;
    for (ShipLogEntry e : events) {
      if (e.type.equals(ShipLogEntry.EventType.COMBAT)) {
        ShipBattleRecord br = (ShipBattleRecord) e.event;
        count += br.getAssists();
      }
    }
    return count;
  }

  public int getCombats() {
    int count = 0;
    for (ShipLogEntry e : events) {
      if (e.type.equals(ShipLogEntry.EventType.COMBAT)) {
        count++;
      }
    }
    return count;
  }

  public int getFleetPointScore() {
    int count = 0;
    for (ShipLogEntry e : events) {
      if (e.type.equals(ShipLogEntry.EventType.COMBAT)) {
        ShipBattleRecord br = (ShipBattleRecord) e.event;
        count += br.getFleetPoints();
      }
    }
    return count;
  }
  
  public int getRecovered() {
    int count = 0;
    for (ShipLogEntry e : events) {
      if (e.type.equals(ShipLogEntry.EventType.COMBAT) && ((ShipBattleRecord)e.event).recovered ) {
        count++;
      }
    }
    return count;
  }

  public ShipBattleRecord getLastBattleRecord() {
    for (int i = events.size() - 1; i >= 0; i--) {
      ShipLogEntry e = events.get(i);
      if (e.type.equals(ShipLogEntry.EventType.COMBAT)) {
        return (ShipBattleRecord) e.event;
      }
    }
    return null;
  }

  public boolean isCurrentFleetMember() {
    for (FleetMemberAPI f : Global.getSector().getPlayerFleet().getMembersWithFightersCopy()) {
      if (this.id.equals(f.getId())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int compareTo(ShipLog other) {
    return Long.valueOf(this.getTimestamp() - other.getTimestamp()).intValue();
  }

}
