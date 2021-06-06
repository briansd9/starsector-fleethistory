/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fleethistory;

import com.thoughtworks.xstream.XStream;
import java.util.HashMap;
import org.apache.log4j.Logger;

/**
 *
 * @author joshi
 */
public class StringCache {

  private final StringBuilder data;
  private transient int currKey;
  public transient HashMap<String, String> stringToKey;
  public transient HashMap<String, String> keyToString;

  public StringCache() {
    data = new StringBuilder();
  }

  public static void alias(XStream x) {
    x.aliasAttribute(StringCache.class, "data", "d");
  }

  public void init() {
    stringToKey = new HashMap<>();
    keyToString = new HashMap<>();
    if (data.length() > 0) {
      String[] d = data.toString().split(U.DELIMITER);
      for (int i = 0; i < d.length; i += 2) {
        String str = d[i];
        String key = d[i + 1];
        keyToString.put(key, str);
        stringToKey.put(str, key);
      }
    }
    currKey = stringToKey.size();
  }

  public String cacheString(String str) {
    if (keyToString == null) {
      init();
    }
    if (stringToKey.containsKey(str)) {
      return stringToKey.get(str);
    } else {
      String key = generateKey();
      Logger.getLogger(StringCache.class).info(String.format("Caching string [%s] with key [%s]", str, key));
      data.append(str).append(U.DELIMITER).append(key).append(U.DELIMITER);
      stringToKey.put(str, key);
      keyToString.put(key, str);
      return key;
    }
  }

  public String getCachedString(String key) {
    if (keyToString == null) {
      init();
    }
    return (keyToString.containsKey(key) ? keyToString.get(key) : null);
  }

  public String generateKey() {
    currKey++;
    String key = U.encodeNum(currKey);
    while(keyToString.containsKey(key)) {
      Logger.getLogger(StringCache.class).info(String.format("Cache already contains key [%s] for number [%d]", key, currKey));
      currKey++;
      key = U.encodeNum(currKey);
    }
    return key;
  }

}
