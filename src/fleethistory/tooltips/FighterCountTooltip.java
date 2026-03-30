package fleethistory.tooltips;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import fleethistory.U;

public class FighterCountTooltip implements TooltipMakerAPI.TooltipCreator {

  private final FighterWingSpecAPI spec;
  private final ShipHullSpecAPI hull;
  private final int lost;

  public FighterCountTooltip(FighterWingSpecAPI spec, ShipHullSpecAPI hull, int lost) {
    this.spec = spec;
    this.hull = hull;
    this.lost = lost;
  }

  @Override
  public boolean isTooltipExpandable(Object tooltipParam) {
    return false;
  }

  @Override
  public float getTooltipWidth(Object tooltipParam) {
    return Math.max(25, hull.getNameWithDesignationWithDashClass().length()) * 8;
  }

  @Override
  public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
    tooltip.addPara(hull.getHullName()+ " " + spec.getVariant().getDisplayName(), 0);
    tooltip.addPara(U.i18n("shipcount_tooltip_lost"), 0, Misc.getNegativeHighlightColor(), lost + "");
  }

}
