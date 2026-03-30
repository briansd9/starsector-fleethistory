package fleethistory.types;

import com.fs.starfarer.api.Global;
import fleethistory.U;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 *
 * @author joshi
 */
public class BattleRecordFighterCount {
  
  private transient Map<String, Integer> count;
  private transient String compressedString;
  
  public BattleRecordFighterCount() {
    this.count = new HashMap<>(); 
  }
  
  public BattleRecordFighterCount(String str) {
    this.compressedString = str;
  }
  
  public String getCompressedString() {
    if(this.count == null || this.count.isEmpty()) {
        return "";
    }
    StringBuilder s = new StringBuilder();
    for(String wingID : count.keySet()) {
      if(s.length() > 0) {
        s.append("|");
      }
      String cachedWingID = U.getCache().cacheString(wingID);
      String lost = U.encodeNum(count.get(wingID));
      s.append(String.format("%s|%s", cachedWingID, lost));
    }
    return s.toString();
  }

  public void logLost(String wingID) {
    if(!count.containsKey(wingID)) {
      Logger.getLogger(this.getClass()).info("Adding new record for " + wingID);
      count.put(wingID, 0);
    }
    count.put(wingID, count.get(wingID) + 1);
  }
  
  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    for(String key : this.getCount().keySet()) {
      String wingName = Global.getSettings().getFighterWingSpec(key).getWingName();
      s.append(String.format("%s: %d, ", wingName, count.get(key)));
    }
    return s.toString();
  }
  
  public Map<String, Integer> getCount() {    
    if(this.count == null && this.compressedString != null) {
      this.count = new HashMap<>();
      String[] tempStr = this.compressedString.split("\\|");
      for(int x = 0; x < tempStr.length; x+= 2) {
        // Logger.getLogger(this.getClass()).info("Got from file: " + tempStr[x] + " -> " + tempStr[x+1]);
        String wingID = U.getCache().getCachedString(tempStr[x]);
        int lost = (int) U.decodeNum(tempStr[x+1]);
        // Logger.getLogger(this.getClass()).info(String.format("Loading %s: %d lost", wingID, lost));
        this.count.put(wingID, lost);
      }
    }
    return this.count;    
  }
  
  public List<Map.Entry<String, Integer>> getSortedCount() {
      List <Map.Entry<String, Integer>> c = new ArrayList<>(this.getCount().entrySet());
      c.sort((Map.Entry<String, Integer> e1, Map.Entry<String, Integer> e2) -> {
          return e2.getValue() - e1.getValue();
      });
      return c;
  }
  
  
}
