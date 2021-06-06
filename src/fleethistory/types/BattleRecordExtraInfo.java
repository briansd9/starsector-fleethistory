package fleethistory.types;

import com.thoughtworks.xstream.XStream;

public class BattleRecordExtraInfo {

  public static final String BOUNTY_COMPLETED = "b";

  public String key;
  public String data;

  public BattleRecordExtraInfo(String key, String data) {
    this.key = key;
    this.data = data;
  }

  public static void alias(XStream x) {
    x.aliasAttribute(BattleRecordExtraInfo.class, "key", "k");
    x.aliasAttribute(BattleRecordExtraInfo.class, "data", "d");
  }
  
  public static void createBountyExtraInfo() {
    
  }

}
