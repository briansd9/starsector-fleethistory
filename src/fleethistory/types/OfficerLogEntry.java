/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fleethistory.types;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import fleethistory.U;

/**
 *
 * @author joshi
 */
public abstract class OfficerLogEntry {

  protected String timestamp;

  public long getTimestamp() {
    return U.decodeNum(this.timestamp);
  }

  public abstract String getCompressedString();  
  public abstract void render(CustomPanelAPI t, float width, float height);

}
