/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fleethistory.types;

import fleethistory.U;

/**
 *
 * @author joshi
 */
public class BattleRecordFleetInfo {

  private String fleetName;
  private String factionId;

  public BattleRecordFleetInfo(String fleetName, String factionId) {
    this.setFleetName(fleetName);
    this.setFactionId(factionId);
  }
  
  public BattleRecordFleetInfo(String compressedString) {
    String[] str = compressedString.split("\\|");
    this.fleetName = str[0];
    this.factionId = str[1];
  }  
  public String getCompressedString() {
    return String.format(
            "%s|%s", 
            this.fleetName, 
            this.factionId
    );
  }  
  
  public final void setFleetName(String str) {
    this.fleetName = U.getCache().cacheString(str);
  }
  public String getFleetName() {
    return U.getCache().getCachedString(this.fleetName);
  }
  
  public final void setFactionId(String str) {
    this.factionId = U.getCache().cacheString(str);
  }
  public String getFactionId() {
    return U.getCache().getCachedString(this.factionId);
  }
  
}
