package fleethistory.shipevents;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.thoughtworks.xstream.XStream;
import fleethistory.U;
import fleethistory.types.ShipEvent;

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
    t.addPara(U.i18n("recovered_in"), U.LINE_SPACING, Misc.getHighlightColor(), this.getLocation());
    panel.addUIElement(t);
    panel.getPosition().setSize(width, t.getPosition().getHeight());
  }
  
}
