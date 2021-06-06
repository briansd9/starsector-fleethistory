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
import org.apache.log4j.Logger;
import fleethistory.U;
import fleethistory.types.ShipEvent;

/**
 *
 * @author joshi
 */
public class ShipRecovery implements ShipEvent {

  private final String location;

  public ShipRecovery(String location) {
    this.location = U.getCache().cacheString(location);
  }
  
  public static void alias(XStream x) {
    x.aliasAttribute(ShipRecovery.class, "location", "l");
  }
  
  public String getLocation() {
    return U.getCache().getCachedString(this.location);
  }

  @Override
  public void render(CustomPanelAPI panel, float width, float height) {
    TooltipMakerAPI t = panel.createUIElement(width, height, false);
    t.addPara("Recovered in %s", U.LINE_SPACING, Misc.getHighlightColor(), this.getLocation());
    panel.addUIElement(t);
    panel.getPosition().setSize(width, t.getPosition().getHeight());
  }
  
}
