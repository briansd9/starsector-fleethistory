/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fleethistory.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import org.apache.log4j.Logger;
import fleethistory.U;
import fleethistory.shipevents.ShipTransaction;
import fleethistory.types.ShipLogEntry;

/**
 *
 * @author joshi
 */
public class ShipBoughtOrSoldListener extends BaseCampaignEventListener {

  public ShipBoughtOrSoldListener() {
    super(false);
  }
  
  @Override
  public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {
    long timestamp = Global.getSector().getClock().getTimestamp();
    String location = transaction.getMarket().getName();
    String onOrAt = transaction.getMarket().getOnOrAt();
    for(PlayerMarketTransaction.ShipSaleInfo info : transaction.getShipsBought()) {
      ShipTransaction st = new ShipTransaction(location, ShipTransaction.ADDED, onOrAt, info.getPrice());
      U.addShipEvent(info.getMember(), timestamp, ShipLogEntry.EventType.TRANSACTION, st);
    }
    for(PlayerMarketTransaction.ShipSaleInfo info : transaction.getShipsSold()) {
      ShipTransaction st = new ShipTransaction(location, ShipTransaction.REMOVED, onOrAt, info.getPrice());
      U.addShipEvent(info.getMember(), timestamp, ShipLogEntry.EventType.TRANSACTION, st);
    }
  }

}