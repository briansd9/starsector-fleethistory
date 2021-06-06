/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fleethistory.types;

/**
 *
 * @author joshi
 */
public class ShipBattleResult {
  
  // never mind readability, go for max compression - this is kind of smelly
  public static final String DEPLOYED = "A";
  public static final String DESTROYED = "B";
  public static final String DISABLED = "C";
  public static final String RETREATED = "D";
  
  public static final boolean isLost(String status) {
    return (DESTROYED.equals(status) || DISABLED.equals(status));
  }
  
};
