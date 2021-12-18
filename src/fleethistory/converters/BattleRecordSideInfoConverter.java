package fleethistory.converters;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import fleethistory.U;
import fleethistory.types.BattleRecordFleetInfo;
import fleethistory.types.BattleRecordPersonInfo;
import fleethistory.types.BattleRecordShipCount;
import fleethistory.types.BattleRecordShipInfo;
import fleethistory.types.BattleRecordSideInfo;
import java.util.ArrayList;
import org.apache.log4j.Logger;

public class BattleRecordSideInfoConverter implements Converter {

  @Override
  public boolean canConvert(Class c) {
    return c.equals(BattleRecordSideInfo.class);
  }

  @Override
  public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {

    StringBuilder s;
    BattleRecordSideInfo i = (BattleRecordSideInfo) value;
    
    if(!i.finalized) {
      Logger.getLogger(this.getClass()).info("BATTLE RECORD NOT FINALIZED, finalizing");
      i.finalizeStats();
    }

    s = new StringBuilder();
    for (BattleRecordFleetInfo fleet : i.fleets) {
      if (s.length() > 0) {
        s.append(U.DELIMITER);
      }
      s.append(fleet.getCompressedString());
    }
    writer.addAttribute("f", s.toString());

    s = new StringBuilder();
    for (BattleRecordPersonInfo officer : i.officers) {
      if (s.length() > 0) {
        s.append(U.DELIMITER);
      }
      s.append(officer.getCompressedString());
    }
    writer.addAttribute("o", s.toString());

    s = new StringBuilder();
    for (BattleRecordShipInfo ship : i.ships) {
      if (s.length() > 0) {
        s.append(U.DELIMITER);
      }
      s.append(ship.getCompressedString());
    }
    writer.addAttribute("s", s.toString());

    s = new StringBuilder();
    for (BattleRecordShipCount count : i.shipCounts) {
      if (s.length() > 0) {
        s.append(U.DELIMITER);
      }
      s.append(count.getCompressedString());
    }
    writer.addAttribute("c", s.toString());

  }

  @Override
  public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {

    // create new object, clean up unnecessary temp storage immediately
    BattleRecordSideInfo i = new BattleRecordSideInfo();
    i.tempFleets = null;
    i.tempOfficers = null;
    i.tempShips = null;
    i.finalized = true;

    String[] tempStr;

    i.fleets = new ArrayList<>();
    if (reader.getAttribute("f") != null && reader.getAttribute("f").length() > 0) {
      tempStr = reader.getAttribute("f").split(U.DELIMITER);
      for (String str : tempStr) {
//        Logger.getLogger(this.getClass()).info(String.format("Fleet: [%s]", str));
        i.fleets.add(new BattleRecordFleetInfo(str));
      }
    }

    i.officers = new ArrayList<>();
    if (reader.getAttribute("o") != null && reader.getAttribute("o").length() > 0) {
      tempStr = reader.getAttribute("o").split(U.DELIMITER);
      for (String str : tempStr) {
//        Logger.getLogger(this.getClass()).info(String.format("Officer: [%s]", str));
        i.officers.add(new BattleRecordPersonInfo(str));
      }
    }

    i.ships = new ArrayList<>();
    if (reader.getAttribute("s") != null && reader.getAttribute("s").length() > 0) {
      tempStr = reader.getAttribute("s").split(U.DELIMITER);
      for (String str : tempStr) {
//        Logger.getLogger(this.getClass()).info(String.format("Ship: [%s]", str));
        i.ships.add(new BattleRecordShipInfo(str));
      }
    }

    i.shipCounts = new ArrayList<>();
    if (reader.getAttribute("c") != null && reader.getAttribute("c").length() > 0) {
      tempStr = reader.getAttribute("c").split(U.DELIMITER);
      for (String str : tempStr) {
//        Logger.getLogger(this.getClass()).info(String.format("shipcount: [%s]", str));
        i.shipCounts.add(new BattleRecordShipCount(str, null));
      }
    }

    return i;

  }

}
