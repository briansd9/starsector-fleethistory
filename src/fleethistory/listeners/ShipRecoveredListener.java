/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fleethistory.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.listeners.ShipRecoveryListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import java.util.List;
import fleethistory.U;
import fleethistory.shipevents.ShipRecovery;
import fleethistory.types.ShipLogEntry;

/**
 *
 * @author joshi
 */
public class ShipRecoveredListener implements ShipRecoveryListener {
  
  @Override
  public void reportShipsRecovered(List<FleetMemberAPI> ships, InteractionDialogAPI dialog) {
    long timestamp = Global.getSector().getClock().getTimestamp();
    for(FleetMemberAPI ship : ships) {
      LocationAPI loc = Global.getSector().getPlayerFleet().getContainingLocation();
      U.addShipEvent(ship, timestamp, ShipLogEntry.EventType.RECOVERED, new ShipRecovery(loc.getName()));
    }    
  }
  
}
