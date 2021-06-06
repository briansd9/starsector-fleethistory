/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fleethistory.tooltips;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import fleethistory.U;
import fleethistory.types.BattleRecordShipCount;
import java.awt.Color;

/**
 *
 * @author joshi
 */
public class ShipCountTooltip implements TooltipMakerAPI.TooltipCreator {

  private final BattleRecordShipCount count;

  public ShipCountTooltip(BattleRecordShipCount count) {
    this.count = count;
  }

  @Override
  public boolean isTooltipExpandable(Object tooltipParam) {
    return false;
  }

  @Override
  public float getTooltipWidth(Object tooltipParam) {
    ShipHullSpecAPI hull = count.getHullSpec();
    boolean isStation = hull.getHints().contains(ShipHullSpecAPI.ShipTypeHints.STATION);
    return Math.max(25, isStation ? hull.getHullName().length() : hull.getNameWithDesignationWithDashClass().length()) * 8;
  }

  @Override
  public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
    
    ShipHullSpecAPI hull = count.getHullSpec();
    boolean isStation = U.isStation(hull);
    tooltip.addPara(isStation ? hull.getHullName() : hull.getNameWithDesignationWithDashClass(), Misc.getHighlightColor(), 0);

    int total = count.getTotal();
    int lost = count.getLost();

    if (lost == 0) {
      if(!isStation) {
        tooltip.addPara("%s deployed", 0, Misc.getHighlightColor(), total + "");
      }
      tooltip.addPara("%s total FP", 0, Misc.getHighlightColor(), count.getFP() + "");
    } else if (total == lost) {
      if(!isStation) {
        tooltip.addPara("%s deployed, %s", 0, Misc.getNegativeHighlightColor(), total + "", "no survivors");
      } else {
        tooltip.addPara("Destroyed", Misc.getNegativeHighlightColor(), 0);
      }
      tooltip.addPara("%s total FP", 0, Misc.getNegativeHighlightColor(), count.getFP() + "");
    } else {
      Color[] colors = {Misc.getNegativeHighlightColor(), Misc.getHighlightColor()};
      tooltip.addPara(
              "%s lost of %s deployed",
              0,
              colors,
              lost + "",
              total + ""
      );
      tooltip.addPara(
              "%s / %s total FP",
              0,
              colors,
              count.getLostFP() + "",
              count.getFP() + ""
      );
    }

  }

}
