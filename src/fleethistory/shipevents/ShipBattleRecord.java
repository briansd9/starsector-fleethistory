package fleethistory.shipevents;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.thoughtworks.xstream.XStream;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import org.apache.log4j.Logger;
import fleethistory.U;
import fleethistory.tooltips.ShipKillCountTooltip;
import fleethistory.types.BattleRecord;
import fleethistory.types.ShipBattleResult;
import fleethistory.types.ShipEvent;
import fleethistory.types.ShipInfo;
import java.util.Collections;

public class ShipBattleRecord implements ShipEvent {

  public static final int ROW_HEIGHT = 25;

  public String battleRecordId;
  public int totalDamageDealt = 0;
  public float health = 0;
  public String result;
  public boolean recovered = false;

  // temporary storage - need this to handle accumulated damage over multiple-engagement battles
  private transient List<ShipInfo> tempShipsKilledList;
  private transient List<ShipBattleRecordStats> stats;
  // stored here
  public String statsCompressedString;

  public ShipBattleRecord(String battleRecordId, String result) {
    this.battleRecordId = battleRecordId;
    this.result = result;
    this.tempShipsKilledList = new ArrayList<>();
  }

  public static void alias(XStream x) {

    x.aliasAttribute(ShipBattleRecord.class, "battleRecordId", "b");
    x.aliasAttribute(ShipBattleRecord.class, "statsCompressedString", "s");
    x.aliasAttribute(ShipBattleRecord.class, "totalDamageDealt", "d");
    x.aliasAttribute(ShipBattleRecord.class, "health", "h");
    x.aliasAttribute(ShipBattleRecord.class, "result", "o");
    x.aliasAttribute(ShipBattleRecord.class, "recovered", "r");

  }

  public void addShip(ShipInfo s) {
    this.tempShipsKilledList.add(s);
  }

  public void finalizeStats() {

    if (this.tempShipsKilledList == null) {
      Logger.getLogger(ShipBattleRecord.class).info("finalizing null ships killed list - returning");
    } else {

      HashMap<String, ShipBattleRecordStats> tempStats = new HashMap<>();
      for (ShipInfo ship : this.tempShipsKilledList) {
        String hullId = ship.getHullId();
        if (!tempStats.containsKey(hullId)) {
          tempStats.put(hullId, new ShipBattleRecordStats(hullId));
        }
        ShipBattleRecordStats s = tempStats.get(hullId);

        s.fleetPoints += Math.round(ship.getFP() * ship.damagePct);
        if (ship.damagePct >= 0.8) {
          s.kills++;
        } else if (ship.damagePct >= 0.2) {
          s.assists++;
        }
      }
      this.stats = new ArrayList(tempStats.values());

      StringBuilder sb = new StringBuilder();
      for (ShipBattleRecordStats s : this.stats) {
        if (sb.length() > 0) {
          sb.append(U.DELIMITER);
        }
        sb.append(s.getCompressedString());
      }
      this.statsCompressedString = sb.toString();

      this.tempShipsKilledList = null;

    }
  }

  public List<ShipBattleRecordStats> getStats() {
    if (this.stats == null) {
      this.stats = new ArrayList<>();
      if (this.statsCompressedString.length() > 0) {
        String[] tokens = this.statsCompressedString.split(U.DELIMITER);
        for (String tok : tokens) {
          this.stats.add(new ShipBattleRecordStats(tok, null));
        }
      }
    }
    return this.stats;
  }

  public int getFleetPoints() {
    int count = 0;
    for (ShipBattleRecordStats s : getStats()) {
      count += s.getFleetPoints();
    }
    return count;
  }

  public int getKills() {
    int count = 0;
    for (ShipBattleRecordStats s : getStats()) {
      count += s.getKills();
    }
    return count;
  }

  public int getAssists() {
    int count = 0;
    for (ShipBattleRecordStats s : getStats()) {
      count += s.getAssists();
    }
    return count;
  }

  public String getOutcomeString() {
    String str = "";
    if (result.equals(ShipBattleResult.DEPLOYED) || result.equals(ShipBattleResult.RETREATED)) {
      if (health == 1) {
        str += "Completely unscathed";
      } else {
        if (health < 0.33) {
          str += "Heavily";
        } else if (health < 0.75) {
          str += "Moderately";
        } else if (health < 1) {
          str += "Lightly";
        }
        str += " damaged (%s)";
      }
    } else {
      if (recovered) {
        str += (result.equals(ShipBattleResult.DESTROYED) ? "Destroyed" : "Disabled");
      } else {
        str += "Lost";
      }
    }
    str += " in battle against %s %s %s";
    if (result.equals(ShipBattleResult.RETREATED)) {
      str += ", retreated";
    } else if (recovered) {
      str += ", recovered";
    }
    return str;
  }

  @Override
  public void render(CustomPanelAPI panel, float width, float height) {
    

    TooltipMakerAPI t = panel.createUIElement(width, height, false);
    renderOutcomeString(t);
    renderBattleStats(t);
    panel.addUIElement(t).inTL(0, 0);
    
    if (!this.getStats().isEmpty()) {
      CustomPanelAPI killTablePanel = panel.createCustomPanel(width, height, null);
      renderKillTable(killTablePanel, width, height);
      panel.addComponent(killTablePanel).belowLeft(t, 0);
      panel.getPosition().setSize(width, panel.getPosition().getHeight() + killTablePanel.getPosition().getHeight() + 25);
    }

  }

  public void renderOutcomeString(TooltipMakerAPI t) {

    BattleRecord br = U.getBattleRecords().get(this.battleRecordId);

    ArrayList<Color> colors = new ArrayList<>();
    colors.add(Global.getSector().getFaction(br.getEnemyFactionId()).getBaseUIColor());
    colors.add(Misc.getTextColor());
    colors.add(Misc.getHighlightColor());
    ArrayList<String> strings = new ArrayList<>();
    strings.add(br.getEnemyFleetDisplayName());
    strings.add(br.inOrAt);
    strings.add(br.getLocation());
    if (health < 1 && (this.result.equals(ShipBattleResult.DEPLOYED) || this.result.equals(ShipBattleResult.RETREATED))) {
      Color c = null;
      if (health < 0.33) {
        c = Misc.getNegativeHighlightColor();
      } else if (health < 0.75) {
        c = Misc.getHighlightColor();
      } else if (health < 1) {
        c = Misc.getPositiveHighlightColor();
      }
      colors.add(0, c);
      strings.add(0, U.format((1 - health) * 100f) + "%");
    }

    t.addPara(
            this.getOutcomeString(),
            U.LINE_SPACING,
            colors.toArray(new Color[colors.size()]),
            strings.toArray(new String[strings.size()])
    );

  }

  public void renderBattleStats(TooltipMakerAPI t) {

    int killCount = this.getKills();
    int assistCount = this.getAssists();
    int fleetPointCount = this.getFleetPoints();

    if (killCount == 0 && assistCount == 0) {
      return;
    }

    if (killCount > 0 && assistCount > 0) {
      t.addPara(
              "%s kill" + (killCount > 1 ? "s" : "") + ", %s assist" + (assistCount > 1 ? "s" : "") + ", %s fleet point" + (fleetPointCount > 1 ? "s" : ""),
              U.LINE_SPACING,
              new Color[]{Misc.getNegativeHighlightColor(), Misc.getHighlightColor(), Misc.getBrightPlayerColor()},
              killCount + "",
              assistCount + "",
              fleetPointCount + ""
      );
    } else if (killCount > 0) {
      t.addPara(
              "%s kill" + (killCount > 1 ? "s" : "") + ", %s fleet point" + (fleetPointCount > 1 ? "s" : ""),
              U.LINE_SPACING,
              new Color[]{Misc.getNegativeHighlightColor(), Misc.getBrightPlayerColor()},
              killCount + "",
              fleetPointCount + ""
      );
    } else {
      t.addPara(
              "%s assist" + (assistCount > 1 ? "s" : "") + ", %s fleet point" + (fleetPointCount > 1 ? "s" : ""),
              U.LINE_SPACING,
              new Color[]{Misc.getHighlightColor(), Misc.getBrightPlayerColor()},
              assistCount + "",
              fleetPointCount + ""
      );
    }
  }

  public void renderKillTable(CustomPanelAPI panel, float width, float height) {

    HashMap<String, Object> pd = U.getPersistentData();
    String killCountDisplay = (String) pd.get(U.FLEET_HISTORY_KILL_DISPLAY);
    if (killCountDisplay.equals(U.KILL_DISPLAY_NONE)) {
      return;
    }
    
    if (this.getKills() == 0 && this.getAssists() == 0) {
      return;
    }    
    
    List<ShipBattleRecordStats> statsList = this.getStats();
    Collections.sort(statsList, new Comparator<ShipBattleRecordStats>() {
      @Override
      public int compare(ShipBattleRecordStats k1, ShipBattleRecordStats k2) {
        if (k2.getFleetPoints() != k1.getFleetPoints()) {
          return k2.getFleetPoints() - k1.getFleetPoints();
        }
        if (k2.getKills() != k1.getKills()) {
          return k2.getKills() - k1.getKills();
        }
        if (k2.getAssists() != k1.getAssists()) {
          return k2.getAssists() - k1.getAssists();
        }
        return 0;
      }
    });
    
    if(killCountDisplay.equals(U.KILL_DISPLAY_TABLE)) {
      
      TooltipMakerAPI t = panel.createUIElement(width, height, false);
      
      float tableWidth = width * 0.8f;
      t.beginTable(
              Global.getSector().getPlayerFaction(), 20,
              "Ship Type", tableWidth * 0.5f,
              "Kills", tableWidth * 0.1f,
              "Assists", tableWidth * 0.1f,
              "Fleet Points", tableWidth * 0.15f
      );
      for (ShipBattleRecordStats statsRow : statsList) {
        ShipHullSpecAPI hull = Global.getSettings().getHullSpec(statsRow.getHullId());
        boolean isStation = U.isStation(hull);
        String shipType = isStation ? hull.getHullName() : hull.getNameWithDesignationWithDashClass();
        t.addRow(Alignment.LMID, Misc.getTextColor(), shipType,
                Alignment.MID, Misc.getTextColor(), statsRow.getKills() + "",
                Alignment.MID, Misc.getTextColor(), statsRow.getAssists() + "",
                Alignment.MID, Misc.getTextColor(), statsRow.getFleetPoints() + ""
        );
      }
      t.addTable("", 0, 2 * U.LINE_SPACING);
      
      panel.addUIElement(t);
      panel.getPosition().setSize(width, panel.getPosition().getHeight() + t.getPosition().getHeight());
      
    } else {
      
      float IMG_SIZE = 50f;
      float SPACING = 20f;

      int index = 0;
      int elementsPerRow = (int) Math.floor(width * 0.8f / (IMG_SIZE + SPACING));

      float calculatedHeight = (IMG_SIZE + SPACING) * (float) Math.ceil(statsList.size() * 1.0f / elementsPerRow);

      CustomPanelAPI content = panel.createCustomPanel(width * 0.8f, calculatedHeight, null);

      for (ShipBattleRecordStats sbrs : statsList) {
        
        String hullId = sbrs.getHullId();
        ShipHullSpecAPI hull = Global.getSettings().getHullSpec(hullId);
        SpriteAPI sprite = Global.getSettings().getSprite(hull.getSpriteName());
        float scaledWidth = sprite.getWidth();
        float scaledHeight = sprite.getHeight();
        if (scaledWidth >= scaledHeight && scaledWidth >= IMG_SIZE) {
          scaledHeight = IMG_SIZE * (scaledHeight / scaledWidth);
          scaledWidth = IMG_SIZE;
        } else if (scaledHeight > scaledWidth && scaledHeight > IMG_SIZE) {
          scaledWidth = IMG_SIZE * (scaledWidth / scaledHeight);
          scaledHeight = IMG_SIZE;
        }
        float scaleFactor = U.hullSizeScalar(hull.getHullSize());
        scaledWidth *= scaleFactor;
        scaledHeight *= scaleFactor;

        TooltipMakerAPI outerContainer = content.createUIElement(IMG_SIZE, IMG_SIZE, false);
        outerContainer.addSpacer(15);
        CustomPanelAPI innerContainer = content.createCustomPanel(IMG_SIZE, IMG_SIZE, null);

        TooltipMakerAPI shipImg = innerContainer.createUIElement(scaledWidth, scaledHeight, false);
        shipImg.addImage(hull.getSpriteName(), scaledWidth, scaledHeight, 0);
        innerContainer.addUIElement(shipImg).inMid();

        int fp = sbrs.getFleetPoints();
        int k = sbrs.getKills();
        int a = sbrs.getAssists();

        TooltipMakerAPI count = innerContainer.createUIElement(IMG_SIZE, IMG_SIZE, false);
        if (fp > 0) {
          count.addPara(fp + "", Misc.getBrightPlayerColor(), 0);
        }
        if (k > 0) {
          count.addPara(k + "", Misc.getNegativeHighlightColor(), 0);
        }
        if (a > 0) {
          count.addPara(a + "", Misc.getHighlightColor(), 0);
        }
        innerContainer.addUIElement(count).inTL(0, 0);
        outerContainer.addCustom(innerContainer, 0);

        outerContainer.addTooltipToPrevious(new ShipKillCountTooltip(hullId, k, a, fp),
                TooltipMakerAPI.TooltipLocation.BELOW
        );

        int x = (int) ((IMG_SIZE + SPACING) * (index % elementsPerRow));
        int y = (int) ((IMG_SIZE + SPACING) * (index / elementsPerRow));

        content.addUIElement(outerContainer).inTL(x, y);
        index++;

      }
      panel.addComponent(content).inTL(0, 0);
      panel.getPosition().setSize(width, content.getPosition().getHeight());
      
    }

  }

}
