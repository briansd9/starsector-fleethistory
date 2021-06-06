/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fleethistory.types;

import com.thoughtworks.xstream.XStream;

/**
 *
 * @author joshi
 */
public class BattleRecordSideCount {

  public int fp = 0;
  public int ships = 0;
  public int officers = 0;
  public int crew = 0;
  public int frigates = 0;
  public int destroyers = 0;
  public int cruisers = 0;
  public int capitalShips = 0;
  public int stations = 0;

  public static void alias(XStream x) {
    String[] aliases = {
      "fp", "f",
      "ships", "s",
      "officers", "o",
      "crew", "c",
      "frigates", "r",
      "destroyers", "d",
      "cruisers", "u",
      "capitalShips", "a",
      "stations", "t"
    };
    for (int i = 0; i < aliases.length; i += 2) {
      x.aliasAttribute(BattleRecordSideCount.class, aliases[i], aliases[i + 1]);
    }
  }

}
