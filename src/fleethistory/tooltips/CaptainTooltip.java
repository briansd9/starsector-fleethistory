/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fleethistory.tooltips;

import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import fleethistory.U;
import java.awt.Color;
import fleethistory.types.BattleRecordPersonInfo;
import fleethistory.types.ShipBattleResult;

/**
 *
 * @author joshi
 */
public class CaptainTooltip implements TooltipMakerAPI.TooltipCreator {
  
  private final BattleRecordPersonInfo info;
  private final boolean showNameInTooltip;
  
  public CaptainTooltip(BattleRecordPersonInfo info) {
    this.info = info;
    this.showNameInTooltip = false;
  }
  
  public CaptainTooltip(BattleRecordPersonInfo info, boolean showNameInTooltip) {
    this.info = info;
    this.showNameInTooltip = showNameInTooltip;
  }

  @Override
  public boolean isTooltipExpandable(Object tooltipParam) {
    return false;
  }

  @Override
  public float getTooltipWidth(Object tooltipParam) {
    return 450;
  }

  @Override
  public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
    
    if(this.showNameInTooltip) {
      tooltip.setParaFontColor(Misc.getHighlightColor());
      tooltip.addPara(info.getName(), 0);
    }
    
    if(info.getShipName() != null) {
      String shipStatus = "";
      if(info.shipStatus != null) {
        switch(info.shipStatus) {
          case ShipBattleResult.DESTROYED:
            shipStatus = U.i18n("captain_tooltip_destroyed");
            break;
          case ShipBattleResult.DISABLED:
            shipStatus = U.i18n("captain_tooltip_disabled");
            break;
        }
      }
      tooltip.addPara(
        "%s %s %s",
        0,
        new Color[] {
          Misc.getTextColor(),
          Misc.getHighlightColor(),
          Misc.getNegativeHighlightColor()
        },
        new String[] {
          U.i18n(info.isFleetCommander ? "captain_tooltip_commanding_flagship" : "captain_tooltip_commanding"),
          info.getFullShipName(),
          shipStatus
        }
      );
    }
  }

}
