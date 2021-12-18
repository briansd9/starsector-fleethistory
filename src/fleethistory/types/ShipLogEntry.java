package fleethistory.types;

import com.thoughtworks.xstream.XStream;
import fleethistory.U;

public class ShipLogEntry {
  
  private String timestamp;
  public String type;
  public ShipEvent event;
  
  public ShipLogEntry(long timestamp, String type, ShipEvent event) {
    this.setTimestamp(timestamp);
    this.type = type;
    this.event = event;
  }
  
  public final void setTimestamp(long timestamp) {
    this.timestamp = U.encodeNum(timestamp);
  }
  public final long getTimestamp() {
    return U.decodeNum(timestamp);
  }
  
  public static void alias(XStream x) {
    x.aliasAttribute(ShipLogEntry.class, "timestamp", "t");
    x.aliasAttribute(ShipLogEntry.class, "type", "y");
    x.aliasAttribute(ShipLogEntry.class, "event", "e");
  }
  
  public class EventType {
    public static final String COMBAT = "c";
    public static final String RECOVERED = "r";
    public static final String TRANSACTION = "t";
  }
  
}
