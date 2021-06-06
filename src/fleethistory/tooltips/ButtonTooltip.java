/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fleethistory.tooltips;

import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

/**
 *
 * @author joshi
 */
public class ButtonTooltip implements TooltipMakerAPI.TooltipCreator {

  private final String msg;
  private final int width;

  public ButtonTooltip(String msg) {
    this.msg = msg;
    this.width = 0;
  }

  public ButtonTooltip(String msg, int width) {
    this.msg = msg;
    this.width = width;
  }
  
  @Override
  public boolean isTooltipExpandable(Object tooltipParam) {
    return false;
  }

  @Override
  public float getTooltipWidth(Object tooltipParam) {
    return (this.width > 0 ? this.width : this.msg.length() * 8);
  }

  @Override
  public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
    tooltip.setParaFontColor(Misc.getHighlightColor());
    tooltip.addPara(msg, 0);
  }

}
