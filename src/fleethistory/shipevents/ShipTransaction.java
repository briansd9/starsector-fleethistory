/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fleethistory.shipevents;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.thoughtworks.xstream.XStream;
import java.awt.Color;
import fleethistory.U;
import fleethistory.types.ShipEvent;

/**
 *
 * @author joshi
 */
public class ShipTransaction implements ShipEvent {
  
  public static final String ADDED = "a";
  public static final String REMOVED = "r";

  private final String location;
  private final String action;
  private final String onOrAt;
  private final String price;

  public ShipTransaction(String location, String action, String onOrAt, float price) {
    this.location = U.getCache().cacheString(location);
    this.action = action;
    this.onOrAt = onOrAt;
    this.price = U.encodeNum(Math.round(price));
  }

  public static void alias(XStream x) {
    x.aliasAttribute(ShipTransaction.class, "location", "l");
    x.aliasAttribute(ShipTransaction.class, "action", "a");
    x.aliasAttribute(ShipTransaction.class, "onOrAt", "o");
    x.aliasAttribute(ShipTransaction.class, "price", "p");
  }
  
  public String getLocation() {
    return U.getCache().getCachedString(this.location);
  }
  
  public int getPrice() {
    return (int)U.decodeNum(this.price);
  }

  @Override
  public void render(CustomPanelAPI panel, float width, float height) {
    
    TooltipMakerAPI t = panel.createUIElement(width, height, false);
    
    int p = this.getPrice();

    if (this.action.equals(ADDED) && p > 0) {
      t.addPara(
              "Bought %s %s for %s",
              U.LINE_SPACING,
              new Color[] { Misc.getTextColor(), Misc.getHighlightColor(), Misc.getHighlightColor() },
              new String[] { this.onOrAt, this.getLocation(), Misc.getDGSCredits(p) + "" }
      );
    } else if (this.action.equals(REMOVED) && p > 0) {
      t.addPara(
              "Sold %s %s for %s",
              U.LINE_SPACING,
              new Color[] { Misc.getTextColor(), Misc.getHighlightColor(), Misc.getHighlightColor() },
              new String[] { this.onOrAt, this.getLocation(), Misc.getDGSCredits(p) + "" }
      );
    } else if (this.action.equals(ADDED) && p == 0) {
      t.addPara(
              "Retrieved from %s",
              U.LINE_SPACING,
              Misc.getHighlightColor(),
              this.getLocation()
      );
    } else if (this.action.equals(REMOVED) && p == 0) {
      t.addPara(
              "Stored %s %s",
              U.LINE_SPACING,
              new Color[] { Misc.getTextColor(), Misc.getHighlightColor() },
              new String[] { this.onOrAt, this.getLocation() }
      );
    }
    
    panel.addUIElement(t);
    panel.getPosition().setSize(width, t.getPosition().getHeight());

  }

}
