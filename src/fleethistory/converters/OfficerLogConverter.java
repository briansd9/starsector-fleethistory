/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fleethistory.converters;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import fleethistory.U;
import fleethistory.types.OfficerBattleEntry;
import fleethistory.types.OfficerLog;
import fleethistory.types.OfficerSkillEntry;
import org.apache.log4j.Logger;

/**
 *
 * @author joshi
 */
public class OfficerLogConverter implements Converter {

  @Override
  public boolean canConvert(Class c) {
    return c.equals(OfficerLog.class);
  }

  @Override
  public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
    OfficerLog c = (OfficerLog) value;
    writer.addAttribute("d", c.getCompressedString());
    writer.addAttribute("e", c.getCompressedEntries());
  }

  @Override
  public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {

    //Logger.getLogger(this.getClass()).info(reader.getAttribute("d"));
    String[] data = reader.getAttribute("d").split("\\|");
    OfficerLog o = new OfficerLog(data[0], data[1], data[2], Integer.parseInt(data[3]));
    for (int i = 4; i < data.length; i++) {
      o.getSkills().add(data[i]);
    }

    String[] entries = reader.getAttribute("e").split(U.DELIMITER);
    for (String str : entries) {
      if (str.length() > 0) {
        if (str.startsWith("B|")) {
          o.getEntries().add(new OfficerBattleEntry(str));
        } else if (str.startsWith("S|")) {
          o.getEntries().add(new OfficerSkillEntry(str));
        } else {
          throw new IllegalArgumentException(str);
        }
      }
    }

    return o;

  }

}
