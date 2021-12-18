package fleethistory.types;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import fleethistory.U;

public abstract class OfficerLogEntry {

  protected String timestamp;

  public long getTimestamp() {
    return U.decodeNum(this.timestamp);
  }

  public abstract String getCompressedString();  
  public abstract void render(CustomPanelAPI t, float width, float height);

}
