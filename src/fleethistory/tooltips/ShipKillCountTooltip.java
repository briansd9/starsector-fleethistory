/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fleethistory.tooltips;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import fleethistory.U;

/**
 *
 * @author joshi
 */
public class ShipKillCountTooltip implements TooltipMakerAPI.TooltipCreator {

  private final ShipHullSpecAPI hull;
  private final String name;
  private final int kills;
  private final int assists;
  private final int fleetPoints;

  public ShipKillCountTooltip(String hullId, int kills, int assists, int fleetPoints) {
    this.hull = Global.getSettings().getHullSpec(hullId);
    this.name = getName(hull);
    this.kills = kills;
    this.assists = assists;
    this.fleetPoints = fleetPoints;
  }

  private String getName(ShipHullSpecAPI hull) {
    boolean isStation = hull.getHints().contains(ShipHullSpecAPI.ShipTypeHints.STATION);
    return isStation ? hull.getHullName() : hull.getNameWithDesignationWithDashClass();
  }

  @Override
  public boolean isTooltipExpandable(Object tooltipParam) {
    return false;
  }

  @Override
  public float getTooltipWidth(Object tooltipParam) {
    return 500; //Math.max(20, this.name.length()) * 8;    
  }

  @Override
  public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {

    tooltip.setParaSmallOrbitron();
    tooltip.addPara(
            this.name,
            Misc.getBrightPlayerColor(),
            0
    );
    tooltip.setParaFontOrbitron();
    tooltip.addPara(
            this.hull.getManufacturer(),
            Misc.getBrightPlayerColor(),
            0
    );
    tooltip.setParaFontDefault();
    tooltip.addSpacer(U.LINE_SPACING);

    if (this.kills > 0) {
      tooltip.addPara(U.i18n(this.kills == 1 ? "kill_count" : "kills_count"), 0, Misc.getNegativeHighlightColor(), this.kills + "");
    }
    if (this.assists > 0) {
      tooltip.addPara(U.i18n(this.kills == 1 ? "assist_count" : "assists_count"), 0, Misc.getHighlightColor(), this.assists + "");
    }
    tooltip.addPara(U.i18n("shipkillcount_tooltip_total_fp"), 0, Misc.getBrightPlayerColor(), this.fleetPoints + "");

    Description d = Global.getSettings().getDescription(this.hull.getDescriptionId(), Description.Type.SHIP);
    if (d != null && d.getText1FirstPara() != null && !d.getText1FirstPara().startsWith("No description")) {
      tooltip.addSpacer(U.LINE_SPACING);
      tooltip.addPara(d.getText1FirstPara(), 0);
    }

  }

}
