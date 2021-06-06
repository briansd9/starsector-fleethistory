/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fleethistory.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import java.util.Objects;
import java.util.Set;
import fleethistory.types.ShipLog;
import fleethistory.U;
import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import fleethistory.FleetHistoryModPlugin;
import fleethistory.tooltips.ButtonTooltip;
import fleethistory.types.BattleRecord;
import fleethistory.types.BattleRecordSideCount;
import fleethistory.types.OfficerLog;

/**
 *
 * @author joshi
 */
public class FleetSummaryIntel extends BaseFleetHistoryIntelPlugin {

  private static final String FLEET_HISTORY_SORT_MODE = "FLEET_HISTORY_SORT_MODE";
  private static final String FLEET_HISTORY_PREFIX = "FH_";
  private static final String SHIP_NAME = "Ship Name";
  private static final String BATTLES = "Battles";
  private static final String KILLS = "Kills";
  private static final String ASSISTS = "Assists";
  private static final String FLEET_POINTS = "Total Fleet Points";

  private static final String BATTLE_HISTORY_SORT_MODE = "BATTLE_HISTORY_SORT_MODE";
  private static final String BATTLE_HISTORY_PREFIX = "BH_";
  private static final String FACTION = "Faction";
  // private static final String BATTLES = "Battles";
  private static final String WON = "Won";
  private static final String LOST = "Lost";
  private static final String TOTAL_BATTLES = "Total";
  private static final String FRIGATES = "Frigates";
  private static final String DESTROYERS = "Destroyers";
  private static final String CRUISERS = "Cruisers";
  private static final String CAPITAL_SHIPS = "Capitals";
  private static final String STATIONS = "Stations";
  // space at the end is important - for differentiating from TOTAL_BATTLES (this stinks)
  private static final String TOTAL_SHIPS = "Total ";
  private static final String TOTAL_FP = "Total FP";

  private static final String FACTION_SHIP_COUNT_MODE = "FACTION_SHIP_COUNT_MODE";
  private static final String FACTION_SHIPS_DESTROYED = "Faction Ships Destroyed";
  private static final String FACTION_SHIPS_LOST_TO = "Ships Lost to Faction";

  private static final String OFFICER_HISTORY_SORT_MODE = "OFFICER_HISTORY_SORT_MODE";
  private static final String OFFICER_HISTORY_PREFIX = "OH_";
  private static final String LEVEL = "Level";
  private static final String NAME = "Name";
//  duplicated from fleet history sort mode
//  private static final String BATTLES = "Battles";
//  private static final String KILLS = "Kills";
//  private static final String ASSISTS = "Assists";
//  private static final String FLEET_POINTS = "Total Fleet Points";

  private static final Color BUTTON_TEXT_COLOR = Misc.getBrightPlayerColor();
  private static final Color BUTTON_BG_COLOR = Misc.getBrightPlayerColor();
  private static final Color BUTTON_BORDER_COLOR = Misc.getDarkPlayerColor();
  private static final Color BUTTON_HIGHLIGHT_COLOR = Misc.getHighlightColor();
  private static final float TABLE_HEADER_HEIGHT = 25f;

  @Override
  public void createLargeDescription(CustomPanelAPI panel, float width, float height) {

    HashMap<String, Object> pd = U.getPersistentData();

    try {

      float topBannerHeight = 25;

      String viewMode = (String) pd.get(U.FLEET_HISTORY_VIEW_MODE);

      TooltipMakerAPI shipViewBtn = panel.createUIElement(150, 25, false);
      shipViewBtn.addAreaCheckbox(
              "Ships", U.FLEET_HISTORY_VIEW_SHIPS,
              Misc.getBasePlayerColor(),
              Misc.getDarkPlayerColor(),
              (viewMode.equals(U.FLEET_HISTORY_VIEW_SHIPS) && !pd.containsKey(U.FLEET_HISTORY_CONFIG) ? Misc.getHighlightColor() : Misc.getBrightPlayerColor()),
              150, 25, 0
      );
      panel.addUIElement(shipViewBtn).inTL(0, 0);

      TooltipMakerAPI captainViewBtn = panel.createUIElement(150, 25, false);
      captainViewBtn.addAreaCheckbox(
              "Officers", U.FLEET_HISTORY_VIEW_OFFICERS,
              Misc.getBasePlayerColor(),
              Misc.getDarkPlayerColor(),
              (viewMode.equals(U.FLEET_HISTORY_VIEW_OFFICERS) && !pd.containsKey(U.FLEET_HISTORY_CONFIG) ? Misc.getHighlightColor() : Misc.getBrightPlayerColor()),
              150, 25, 0
      );
      panel.addUIElement(captainViewBtn).rightOfMid(shipViewBtn, 10);

      TooltipMakerAPI battleViewBtn = panel.createUIElement(150, 25, false);
      battleViewBtn.addAreaCheckbox(
              "Battles", U.FLEET_HISTORY_VIEW_BATTLES,
              Misc.getBasePlayerColor(),
              Misc.getDarkPlayerColor(),
              (viewMode.equals(U.FLEET_HISTORY_VIEW_BATTLES) && !pd.containsKey(U.FLEET_HISTORY_CONFIG) ? Misc.getHighlightColor() : Misc.getBrightPlayerColor()),
              150, 25, 0
      );
      panel.addUIElement(battleViewBtn).rightOfMid(captainViewBtn, 10);

      TooltipMakerAPI configBtn = panel.createUIElement(150, 25, false);
      configBtn.addAreaCheckbox(
              "Settings", U.FLEET_HISTORY_CONFIG,
              Misc.getBasePlayerColor(),
              Misc.getDarkPlayerColor(),
              (pd.containsKey(U.FLEET_HISTORY_CONFIG) ? Misc.getHighlightColor() : Misc.getBrightPlayerColor()),
              150, 25, 0
      );
      panel.addUIElement(configBtn).rightOfMid(battleViewBtn, 10);

      TooltipMakerAPI spacer = panel.createUIElement(width, 1, false);
      spacer.addButton("", "", Color.decode("#222222"), Color.decode("#222222"), width * 0.94f, 1, 0);
      panel.addUIElement(spacer).belowLeft(shipViewBtn, 15);

      TooltipMakerAPI container = null;
      CustomPanelAPI content = null;
      if (pd.containsKey(U.FLEET_HISTORY_CONFIG)) {
        TooltipMakerAPI configSection = createConfigSection(panel, width * 0.956f, height - topBannerHeight - 45);
        panel.addUIElement(configSection).belowMid(spacer, 15);
      } else {

        // use full screen width for battles summary - lots of table columns
        float componentWidth = width * 0.978f;
        float componentHeight = height - topBannerHeight - 45;

        container = panel.createUIElement(componentWidth, componentHeight, true);

        switch (viewMode) {
          case U.FLEET_HISTORY_VIEW_SHIPS:
            content = createShipSummary(panel, componentWidth, componentHeight);
            break;
          case U.FLEET_HISTORY_VIEW_BATTLES:
            content = createBattleSummary(panel, componentWidth, componentHeight);
            break;
          case U.FLEET_HISTORY_VIEW_OFFICERS:
            content = createOfficerSummary(panel, componentWidth, componentHeight);
            break;
        }

        container.addCustom(content, 0);
        panel.addUIElement(container).belowLeft(spacer, 15).setXAlignOffset(0);

      }
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  private TooltipMakerAPI createTableHeader(CustomPanelAPI panel, String prefix, String label, boolean isSortMode, float width, float height) {
    TooltipMakerAPI header = panel.createUIElement(width, height, false);
    header.addAreaCheckbox(
            label,
            prefix + label,
            BUTTON_BG_COLOR,
            BUTTON_BORDER_COLOR,
            isSortMode ? BUTTON_HIGHLIGHT_COLOR : BUTTON_TEXT_COLOR,
            width,
            height,
            0
    );
    return header;
  }

  private TooltipMakerAPI createTableHeader(CustomPanelAPI panel, String prefix, String label, boolean isSortMode, float width) {
    return createTableHeader(panel, prefix, label, isSortMode, width, TABLE_HEADER_HEIGHT);
  }

  private CustomPanelAPI createShipSummary(CustomPanelAPI panel, float width, float height) {

    String sortMode = FLEET_POINTS;
    int reverseMode = 1;
    if (U.getPersistentData().containsKey(FLEET_HISTORY_SORT_MODE)) {
      sortMode = (String) U.getPersistentData().get(FLEET_HISTORY_SORT_MODE);
      if (sortMode.endsWith("_REVERSE")) {
        sortMode = sortMode.replaceAll("_REVERSE", "");
        reverseMode = -1;
      }
    }
    final String SORT_MODE = sortMode;
    final int REVERSE_MODE = reverseMode;

    ShipLog[] shipLogs = U.getShipLogs().values().toArray(new ShipLog[U.getShipLogs().size()]);
    Arrays.sort(shipLogs, new Comparator<ShipLog>() {
      @Override
      public int compare(ShipLog s1, ShipLog s2) {
        switch (SORT_MODE) {
          case SHIP_NAME:
            return REVERSE_MODE * s1.info.getShipName().compareTo(s2.info.getShipName());
          case BATTLES:
            return REVERSE_MODE * (s2.getCombats() - s1.getCombats());
          case KILLS:
            return REVERSE_MODE * (s2.getKills() - s1.getKills());
          case ASSISTS:
            return REVERSE_MODE * (s2.getAssists() - s1.getAssists());
          case FLEET_POINTS:
          default:
            return REVERSE_MODE * (s2.getFleetPointScore() - s1.getFleetPointScore());
        }
      }
    });

    float IMG_SIZE = 30f;
    float ROW_PADDING = 5f;
    CustomPanelAPI t = panel.createCustomPanel(width, (IMG_SIZE + ROW_PADDING) * (shipLogs.length + 1), null);
    CustomPanelAPI previousRow = null;

    CustomPanelAPI header = t.createCustomPanel(width, TABLE_HEADER_HEIGHT, null);

    TooltipMakerAPI labelHeader = createTableHeader(header, FLEET_HISTORY_PREFIX, SHIP_NAME, SORT_MODE.equals(SHIP_NAME), IMG_SIZE + width * 0.4f);
    header.addUIElement(labelHeader).inTL(0, 0);

    TooltipMakerAPI combatsHeader = createTableHeader(header, FLEET_HISTORY_PREFIX, BATTLES, SORT_MODE.equals(BATTLES), width * 0.1f);
    header.addUIElement(combatsHeader).rightOfMid(labelHeader, 0);

    TooltipMakerAPI killsHeader = createTableHeader(header, FLEET_HISTORY_PREFIX, KILLS, SORT_MODE.equals(KILLS), width * 0.1f);
    header.addUIElement(killsHeader).rightOfMid(combatsHeader, 0);

    TooltipMakerAPI assistsHeader = createTableHeader(header, FLEET_HISTORY_PREFIX, ASSISTS, SORT_MODE.equals(ASSISTS), width * 0.1f);
    header.addUIElement(assistsHeader).rightOfMid(killsHeader, 0);

    TooltipMakerAPI fpHeader = createTableHeader(header, FLEET_HISTORY_PREFIX, FLEET_POINTS, SORT_MODE.equals(FLEET_POINTS), width * 0.2f);
    header.addUIElement(fpHeader).rightOfMid(assistsHeader, 0);

    t.addComponent(header).inTL(0, 0);
    previousRow = header;

    for (ShipLog s : shipLogs) {

      HashMap<String, Object> pd = U.getPersistentData();

      boolean isCurrentFleetMember = s.isCurrentFleetMember();
      if (pd.containsKey(U.FLEET_HISTORY_HIDE_INACTIVE) && !s.isCurrentFleetMember()) {
        continue;
      }

      Color highlightColor = isCurrentFleetMember ? Misc.getBrightPlayerColor() : Misc.getDarkPlayerColor();
      Color defaultColor = isCurrentFleetMember ? Misc.getTextColor() : Misc.getGrayColor();

      CustomPanelAPI row = t.createCustomPanel(width, IMG_SIZE, null);

      TooltipMakerAPI icon = row.createUIElement(IMG_SIZE, IMG_SIZE, false);
      float scaleFactor = U.hullSizeScalar(s.info.getHullSpec().getHullSize());
      icon.addImage(s.info.getHullSpec().getSpriteName(), IMG_SIZE * scaleFactor, IMG_SIZE * scaleFactor, (IMG_SIZE / 2) * (1 - scaleFactor));
      row.addUIElement(icon).inTL((IMG_SIZE / 2) * (1 - scaleFactor), 0);

      TooltipMakerAPI label = row.createUIElement(width * 0.4f, IMG_SIZE, false);
      label.addPara(s.info.getShipName(), highlightColor, 0);
      label.addPara(s.info.getHullSpec().getNameWithDesignationWithDashClass(), defaultColor, 0);
      row.addUIElement(label).rightOfMid(icon, (IMG_SIZE / 2) * scaleFactor);

      TooltipMakerAPI combats = row.createUIElement(width * 0.1f, IMG_SIZE, false);
      combats.addPara(s.getCombats() + "", defaultColor, 0);
      row.addUIElement(combats).rightOfMid(label, 0);

      TooltipMakerAPI kills = row.createUIElement(width * 0.1f, IMG_SIZE, false);
      kills.addPara(s.getKills() + "", defaultColor, 0);
      row.addUIElement(kills).rightOfMid(combats, 0);

      TooltipMakerAPI assists = row.createUIElement(width * 0.1f, IMG_SIZE, false);
      assists.addPara(s.getAssists() + "", defaultColor, 0);
      row.addUIElement(assists).rightOfMid(kills, 0);

      TooltipMakerAPI fleetPoints = row.createUIElement(width * 0.25f, IMG_SIZE, false);
      fleetPoints.addPara(s.getFleetPointScore() + "", defaultColor, 0);
      row.addUIElement(fleetPoints).rightOfMid(assists, 0);

      t.addComponent(row).belowMid(previousRow, ROW_PADDING);
      previousRow = row;

    }

    return t;

  }

  private CustomPanelAPI createBattleSummary(CustomPanelAPI panel, float width, float height) {

    String sortMode = TOTAL_FP;
    int reverseMode = 1;
    if (U.getPersistentData().containsKey(BATTLE_HISTORY_SORT_MODE)) {
      sortMode = (String) U.getPersistentData().get(BATTLE_HISTORY_SORT_MODE);
      if (sortMode.endsWith("_REVERSE")) {
        sortMode = sortMode.replaceAll("_REVERSE", "");
        reverseMode = -1;
      }
    }
    final String SORT_MODE = sortMode;
    final int REVERSE_MODE = reverseMode;

    Collection<BattleRecord> battleRecords = U.getBattleRecords().values();
    HashMap<String, FactionBattleHistory> factionHistory = new HashMap<>();
    
    String factionShipCountMode = FACTION_SHIPS_DESTROYED;
    if (U.getPersistentData().containsKey(FACTION_SHIP_COUNT_MODE)) {
      factionShipCountMode = (String) U.getPersistentData().get(FACTION_SHIP_COUNT_MODE);
    }

    for (BattleRecord br : battleRecords) {

      if (!factionHistory.containsKey(br.getEnemyFactionId())) {
        factionHistory.put(br.getEnemyFactionId(), new FactionBattleHistory(br.getEnemyFactionId()));
      }
      FactionBattleHistory fh = factionHistory.get(br.getEnemyFactionId());

      fh.battles++;

      if (br.playerWon) {
        fh.battlesWon++;
      } else {
        fh.battlesLost++;
      }

      BattleRecordSideCount bsc = (factionShipCountMode.equals(FACTION_SHIPS_DESTROYED) ? br.enemySide.getLostCount() : br.playerSide.getLostCount());
      fh.officers += bsc.officers;
      fh.totalShips += bsc.ships;
      fh.totalFleetPoints += bsc.fp;
      fh.frigates += bsc.frigates;
      fh.destroyers += bsc.destroyers;
      fh.cruisers += bsc.cruisers;
      fh.capitalShips += bsc.capitalShips;
      fh.stations += bsc.stations;

    }

    FactionBattleHistory[] factionArr = factionHistory.values().toArray(new FactionBattleHistory[factionHistory.size()]);
    Arrays.sort(factionArr, new Comparator<FactionBattleHistory>() {
      @Override
      public int compare(FactionBattleHistory f1, FactionBattleHistory f2) {
        int retval = 0;
        switch (SORT_MODE) {
          case TOTAL_FP:
            retval = REVERSE_MODE * (f2.totalFleetPoints - f1.totalFleetPoints);
            if (retval != 0) {
              return retval;
            }
          case TOTAL_SHIPS:
            retval = REVERSE_MODE * (f2.totalShips - f1.totalShips);
            if (retval != 0) {
              return retval;
            }
          case TOTAL_BATTLES:
            retval = REVERSE_MODE * (f2.battles - f1.battles);
            if (retval != 0) {
              return retval;
            }
          case STATIONS:
            retval = REVERSE_MODE * (f2.stations - f1.stations);
            if (retval != 0) {
              return retval;
            }
          case CAPITAL_SHIPS:
            retval = REVERSE_MODE * (f2.capitalShips - f1.capitalShips);
            if (retval != 0) {
              return retval;
            }
          case DESTROYERS:
            retval = REVERSE_MODE * (f2.destroyers - f1.destroyers);
            if (retval != 0) {
              return retval;
            }
          case CRUISERS:
            retval = REVERSE_MODE * (f2.cruisers - f1.cruisers);
            if (retval != 0) {
              return retval;
            }
          case FRIGATES:
            retval = REVERSE_MODE * (f2.frigates - f1.frigates);
            if (retval != 0) {
              return retval;
            }
          case WON:
            retval = REVERSE_MODE * (f2.battlesWon - f1.battlesWon);
            if (retval != 0) {
              return retval;
            }
          case LOST:
            retval = REVERSE_MODE * (f2.battlesLost - f1.battlesLost);
            if (retval != 0) {
              return retval;
            }
          case FACTION:
          default:
            String s1 = Global.getSector().getFaction(f1.factionId).getDisplayNameLong();
            String s2 = Global.getSector().getFaction(f2.factionId).getDisplayNameLong();
            return REVERSE_MODE * s1.compareToIgnoreCase(s2);
        }
      }
    });

    float IMG_SIZE = 30f;
    float ROW_PADDING = 8f;
    CustomPanelAPI t = panel.createCustomPanel(width, (IMG_SIZE + ROW_PADDING) * (factionArr.length + 1), null);
    CustomPanelAPI previousRow = null;

    CustomPanelAPI table = t.createCustomPanel(width, TABLE_HEADER_HEIGHT, null);

    float labelWidth = IMG_SIZE + width * 0.28f;
    float wonWidth = width * 0.045f;
    float lostWidth = width * 0.045f;
    float totalBattlesWidth = width * 0.055f;
    float frigatesWidth = width * 0.07f;
    float destroyersWidth = width * 0.085f;
    float cruisersWidth = width * 0.065f;
    float capitalShipsWidth = width * 0.07f;
    float stationsWidth = width * 0.07f;
    float totalShipsWidth = width * 0.065f;
    float totalFleetPointsWidth = width * 0.08f;
    
    // not sure why 11 is required to make the full width match up - rounding error? 1px offset per column?
    float fullWidth = 11 + labelWidth + wonWidth + lostWidth + totalBattlesWidth + frigatesWidth + destroyersWidth + cruisersWidth + capitalShipsWidth
            + stationsWidth + totalBattlesWidth + totalFleetPointsWidth;

    TooltipMakerAPI labelHeader = createTableHeader(table, BATTLE_HISTORY_PREFIX, FACTION, SORT_MODE.equals(FACTION), labelWidth, TABLE_HEADER_HEIGHT * 2);
    // add y-padding of TABLE_HEADER_HEIGHT to make space for top-level headers (battles / ships)
    table.addUIElement(labelHeader).inTL(0, 0);

    TooltipMakerAPI wonHeader = createTableHeader(table, BATTLE_HISTORY_PREFIX, WON, SORT_MODE.equals(WON), wonWidth);
    table.addUIElement(wonHeader).rightOfBottom(labelHeader, 0);

    TooltipMakerAPI lostHeader = createTableHeader(table, BATTLE_HISTORY_PREFIX, LOST, SORT_MODE.equals(LOST), lostWidth);
    table.addUIElement(lostHeader).rightOfMid(wonHeader, 0);

    TooltipMakerAPI totalBattlesHeader = createTableHeader(table, BATTLE_HISTORY_PREFIX, TOTAL_BATTLES, SORT_MODE.equals(TOTAL_BATTLES), totalBattlesWidth);
    table.addUIElement(totalBattlesHeader).rightOfMid(lostHeader, 0);

    TooltipMakerAPI battlesTopHeader = createTableHeader(table, "", BATTLES, false, wonWidth + lostWidth + totalBattlesWidth);
    table.addUIElement(battlesTopHeader).aboveLeft(wonHeader, 0);

    TooltipMakerAPI frigatesHeader = createTableHeader(table, BATTLE_HISTORY_PREFIX, FRIGATES, SORT_MODE.equals(FRIGATES), frigatesWidth);
    table.addUIElement(frigatesHeader).rightOfMid(totalBattlesHeader, 0);

    TooltipMakerAPI destroyersHeader = createTableHeader(table, BATTLE_HISTORY_PREFIX, DESTROYERS, SORT_MODE.equals(DESTROYERS), destroyersWidth);
    table.addUIElement(destroyersHeader).rightOfMid(frigatesHeader, 0);

    TooltipMakerAPI cruisersHeader = createTableHeader(table, BATTLE_HISTORY_PREFIX, CRUISERS, SORT_MODE.equals(CRUISERS), cruisersWidth);
    table.addUIElement(cruisersHeader).rightOfMid(destroyersHeader, 0);

    TooltipMakerAPI capitalShipsHeader = createTableHeader(table, BATTLE_HISTORY_PREFIX, CAPITAL_SHIPS, SORT_MODE.equals(CAPITAL_SHIPS), capitalShipsWidth);
    table.addUIElement(capitalShipsHeader).rightOfMid(cruisersHeader, 0);

    TooltipMakerAPI stationsHeader = createTableHeader(table, BATTLE_HISTORY_PREFIX, STATIONS, SORT_MODE.equals(STATIONS), stationsWidth);
    table.addUIElement(stationsHeader).rightOfMid(capitalShipsHeader, 0);

    TooltipMakerAPI totalShipsHeader = createTableHeader(table, BATTLE_HISTORY_PREFIX, TOTAL_SHIPS, SORT_MODE.equals(TOTAL_SHIPS), totalShipsWidth);
    table.addUIElement(totalShipsHeader).rightOfMid(stationsHeader, 0);

    TooltipMakerAPI totalFleetPointsHeader = createTableHeader(table, BATTLE_HISTORY_PREFIX, TOTAL_FP, SORT_MODE.equals(TOTAL_FP), totalFleetPointsWidth);
    table.addUIElement(totalFleetPointsHeader).rightOfBottom(totalShipsHeader, 0);
    
    TooltipMakerAPI shipsTopHeader = createTableHeader(
            table, "", factionShipCountMode, false, 
            frigatesWidth + destroyersWidth + cruisersWidth + capitalShipsWidth + stationsWidth + totalShipsWidth + totalFleetPointsWidth
    );
    table.addUIElement(shipsTopHeader).aboveLeft(frigatesHeader, 0);    
    TooltipMakerAPI shipsToggleIcon = table.createUIElement(12, 16, false);
    shipsToggleIcon.addImage(Global.getSettings().getSpriteName("test", "toggle"), 16, 16, 0);
    table.addUIElement(shipsToggleIcon).rightOfMid(
            shipsTopHeader, 
            factionShipCountMode.equals(FACTION_SHIPS_DESTROYED) ? (width * -0.166f) : (width * -0.178f)
    );

    for (int i = 0; i < factionArr.length; i++) {

      FactionBattleHistory f = factionArr[i];
      FactionAPI faction = Global.getSector().getFaction(f.factionId);
      float yOffset = 4 + (IMG_SIZE + ROW_PADDING) * i;
      float textOffset = 7;
      
      TooltipMakerAPI highlighter = table.createUIElement(fullWidth, IMG_SIZE, false);
      highlighter.addAreaCheckbox("", "", new Color(255, 255, 255, 128), Color.BLACK, Color.BLACK, fullWidth, IMG_SIZE + 4, 0);
      table.addUIElement(highlighter).belowLeft(labelHeader, yOffset - 2);

      TooltipMakerAPI icon = table.createUIElement(IMG_SIZE, IMG_SIZE, false);
      icon.addImage(faction.getCrest(), IMG_SIZE, IMG_SIZE, 0);
      table.addUIElement(icon).belowLeft(labelHeader, yOffset);

      TooltipMakerAPI labelCell = table.createUIElement(labelWidth - IMG_SIZE, IMG_SIZE / 2, false);
      labelCell.addPara(faction.getDisplayNameLong(), faction.getBrightUIColor(), 0);
      table.addUIElement(labelCell).rightOfMid(icon, IMG_SIZE / 2);

      TooltipMakerAPI wonCell = table.createUIElement(wonWidth, IMG_SIZE, false);
      wonCell.addPara("  " + f.battlesWon, 0);
      table.addUIElement(wonCell).belowMid(wonHeader, yOffset + textOffset);

      TooltipMakerAPI lostCell = table.createUIElement(lostWidth, IMG_SIZE, false);
      lostCell.addPara("  " + f.battlesLost, 0);
      table.addUIElement(lostCell).belowMid(lostHeader, yOffset + textOffset);

      TooltipMakerAPI totalBattlesCell = table.createUIElement(totalBattlesWidth, IMG_SIZE, false);
      totalBattlesCell.addPara("  " + f.battles, 0);
      table.addUIElement(totalBattlesCell).belowMid(totalBattlesHeader, yOffset + textOffset);

      TooltipMakerAPI frigatesCell = table.createUIElement(frigatesWidth, IMG_SIZE, false);
      frigatesCell.addPara("  " + f.frigates, 0);
      table.addUIElement(frigatesCell).belowMid(frigatesHeader, yOffset + textOffset);

      TooltipMakerAPI destroyersCell = table.createUIElement(destroyersWidth, IMG_SIZE, false);
      destroyersCell.addPara("  " + f.destroyers, 0);
      table.addUIElement(destroyersCell).belowMid(destroyersHeader, yOffset + textOffset);

      TooltipMakerAPI cruisersCell = table.createUIElement(cruisersWidth, IMG_SIZE, false);
      cruisersCell.addPara("  " + f.cruisers, 0);
      table.addUIElement(cruisersCell).belowMid(cruisersHeader, yOffset + textOffset);

      TooltipMakerAPI capitalShipsCell = table.createUIElement(capitalShipsWidth, IMG_SIZE, false);
      capitalShipsCell.addPara("  " + f.capitalShips, 0);
      table.addUIElement(capitalShipsCell).belowMid(capitalShipsHeader, yOffset + textOffset);

      TooltipMakerAPI stationsCell = table.createUIElement(stationsWidth, IMG_SIZE, false);
      stationsCell.addPara("  " + f.stations, 0);
      table.addUIElement(stationsCell).belowMid(stationsHeader, yOffset + textOffset);

      TooltipMakerAPI totalShipsCell = table.createUIElement(totalShipsWidth, IMG_SIZE, false);
      totalShipsCell.addPara(" " + f.totalShips, 0);
      table.addUIElement(totalShipsCell).belowMid(totalShipsHeader, yOffset + textOffset);

      TooltipMakerAPI totalFleetPointsCell = table.createUIElement(totalFleetPointsWidth, IMG_SIZE, false);
      totalFleetPointsCell.addPara("  " + f.totalFleetPoints, 0);
      table.addUIElement(totalFleetPointsCell).belowMid(totalFleetPointsHeader, yOffset + textOffset);

    }

    t.addComponent(table).inTL(0, 0);
    return t;

  }

  private CustomPanelAPI createOfficerSummary(CustomPanelAPI panel, float width, float height) {

    String sortMode = FLEET_POINTS;
    int reverseMode = 1;
    if (U.getPersistentData().containsKey(OFFICER_HISTORY_SORT_MODE)) {
      sortMode = (String) U.getPersistentData().get(OFFICER_HISTORY_SORT_MODE);
      if (sortMode.endsWith("_REVERSE")) {
        sortMode = sortMode.replaceAll("_REVERSE", "");
        reverseMode = -1;
      }
    }
    final String SORT_MODE = sortMode;
    final int REVERSE_MODE = reverseMode;

    OfficerLog[] officerLogs = U.getOfficerLogs().values().toArray(new OfficerLog[U.getOfficerLogs().size()]);
    Arrays.sort(officerLogs, new Comparator<OfficerLog>() {
      @Override
      public int compare(OfficerLog o1, OfficerLog o2) {
        OfficerLog.OfficerBattleStats s1 = o1.getStats();
        OfficerLog.OfficerBattleStats s2 = o2.getStats();
        switch (SORT_MODE) {
          case NAME:
            return REVERSE_MODE * o1.getName().compareTo(o2.getName());
          case BATTLES:
            return REVERSE_MODE * (s2.battles - s1.battles);
          case KILLS:
            return REVERSE_MODE * (s2.kills - s1.kills);
          case ASSISTS:
            return REVERSE_MODE * (s2.assists - s1.assists);
          case FLEET_POINTS:
          default:
            return REVERSE_MODE * (s2.fleetPoints - s1.fleetPoints);
        }
      }
    });

    float IMG_SIZE = 30f;
    float ROW_PADDING = 5f;
    CustomPanelAPI t = panel.createCustomPanel(width, (IMG_SIZE + ROW_PADDING) * (officerLogs.length + 1), null);
    CustomPanelAPI previousRow = null;

    CustomPanelAPI header = t.createCustomPanel(width, TABLE_HEADER_HEIGHT, null);

    float labelWidth = IMG_SIZE + width * 0.3f;
    float levelWidth = width * 0.1f;
    float combatsWidth = width * 0.1f;
    float killsWidth = width * 0.1f;
    float assistsWidth = width * 0.1f;
    float fpWidth = width * 0.2f;

    TooltipMakerAPI labelHeader = createTableHeader(header, OFFICER_HISTORY_PREFIX, NAME, SORT_MODE.equals(NAME), labelWidth);
    header.addUIElement(labelHeader).inTL(0, 0);

    TooltipMakerAPI levelHeader = createTableHeader(header, OFFICER_HISTORY_PREFIX, LEVEL, SORT_MODE.equals(LEVEL), levelWidth);
    header.addUIElement(levelHeader).rightOfMid(labelHeader, 0);

    TooltipMakerAPI combatsHeader = createTableHeader(header, OFFICER_HISTORY_PREFIX, BATTLES, SORT_MODE.equals(BATTLES), combatsWidth);
    header.addUIElement(combatsHeader).rightOfMid(levelHeader, 0);

    TooltipMakerAPI killsHeader = createTableHeader(header, OFFICER_HISTORY_PREFIX, KILLS, SORT_MODE.equals(KILLS), killsWidth);
    header.addUIElement(killsHeader).rightOfMid(combatsHeader, 0);

    TooltipMakerAPI assistsHeader = createTableHeader(header, OFFICER_HISTORY_PREFIX, ASSISTS, SORT_MODE.equals(ASSISTS), assistsWidth);
    header.addUIElement(assistsHeader).rightOfMid(killsHeader, 0);

    TooltipMakerAPI fpHeader = createTableHeader(header, OFFICER_HISTORY_PREFIX, FLEET_POINTS, SORT_MODE.equals(FLEET_POINTS), fpWidth);
    header.addUIElement(fpHeader).rightOfMid(assistsHeader, 0);

    t.addComponent(header).inTL(0, 0);
    previousRow = header;

    for (OfficerLog o : officerLogs) {

      HashMap<String, Object> pd = U.getPersistentData();

      boolean isActive = (o.getCurrentShipAssignment() != null);
      if (pd.containsKey(U.FLEET_HISTORY_HIDE_INACTIVE) && !isActive) {
        continue;
      }

      Color defaultColor = isActive ? Misc.getTextColor() : Misc.getGrayColor();

      CustomPanelAPI row = t.createCustomPanel(width, IMG_SIZE, null);

      TooltipMakerAPI icon = row.createUIElement(IMG_SIZE, IMG_SIZE, false);
      icon.addImage(o.getSprite(), IMG_SIZE, IMG_SIZE, 0);
      row.addUIElement(icon).inTL(0, 0);

      TooltipMakerAPI label = row.createUIElement(labelWidth - IMG_SIZE, IMG_SIZE, false);
      label.addPara(o.getName(), defaultColor, 0);
      row.addUIElement(label).rightOfMid(icon, IMG_SIZE / 2);

      TooltipMakerAPI level = row.createUIElement(levelWidth, IMG_SIZE, false);
      level.addPara(o.getLevel() + "", defaultColor, 0);
      row.addUIElement(level).rightOfMid(label, 0);

      OfficerLog.OfficerBattleStats s = o.getStats();

      TooltipMakerAPI combats = row.createUIElement(combatsWidth, IMG_SIZE, false);
      combats.addPara(s.battles + "", defaultColor, 0);
      row.addUIElement(combats).rightOfMid(level, 0);

      TooltipMakerAPI kills = row.createUIElement(killsWidth, IMG_SIZE, false);
      kills.addPara(s.kills + "", defaultColor, 0);
      row.addUIElement(kills).rightOfMid(combats, 0);

      TooltipMakerAPI assists = row.createUIElement(assistsWidth, IMG_SIZE, false);
      assists.addPara(s.assists + "", defaultColor, 0);
      row.addUIElement(assists).rightOfMid(kills, 0);

      TooltipMakerAPI fleetPoints = row.createUIElement(fpWidth, IMG_SIZE, false);
      fleetPoints.addPara(s.fleetPoints + "", defaultColor, 0);
      row.addUIElement(fleetPoints).rightOfMid(assists, 0);

      t.addComponent(row).belowMid(previousRow, ROW_PADDING);
      previousRow = row;

    }

    return t;

  }

  private TooltipMakerAPI createConfigSection(CustomPanelAPI panel, float width, float height) {

    HashMap<String, Object> pd = U.getPersistentData();

    TooltipMakerAPI t = panel.createUIElement(width, height, true);
    CustomPanelAPI container = panel.createCustomPanel(width, 600, null);

    TooltipMakerAPI battleFiltersHeader = container.createUIElement(250, 25, false);
    battleFiltersHeader.setParaInsigniaVeryLarge();
    battleFiltersHeader.setParaFontColor(Misc.getBasePlayerColor());
    battleFiltersHeader.addPara("Battle Settings", 3);
    container.addUIElement(battleFiltersHeader).inTL(0, 0);

    TooltipMakerAPI battleSizeLabel = container.createUIElement(150, 25, false);
    battleSizeLabel.setParaSmallInsignia();
    battleSizeLabel.setParaFontColor(Misc.getBasePlayerColor());
    battleSizeLabel.addPara("Battle size", 3);
    container.addUIElement(battleSizeLabel).belowLeft(battleFiltersHeader, 10).setXAlignOffset(25);

    TooltipMakerAPI prevComponent = battleSizeLabel;
    ButtonAPI btn = null;

    int[] battleSizes = {0, 50, 100, 200, 400, 800, 1600, 3200};
    int selectedBattleSize = (int) pd.get(U.FLEET_HISTORY_BATTLE_SIZE);
    for (int size : battleSizes) {
      boolean checked = (selectedBattleSize == size);
      TooltipMakerAPI battleSizeBtn = container.createUIElement(50, 25, false);
      btn = battleSizeBtn.addAreaCheckbox(
              size + "",
              U.FLEET_HISTORY_BATTLE_SIZE + size,
              Misc.getBasePlayerColor(),
              Misc.getDarkPlayerColor(),
              checked ? Misc.getHighlightColor() : Misc.getBrightPlayerColor(),
              50, 25, 0
      );
      btn.setChecked(checked);
      container.addUIElement(battleSizeBtn).rightOfMid(prevComponent, 2);
      prevComponent = battleSizeBtn;
    }
    TooltipMakerAPI battleSizeDesc = container.createUIElement(width, 0, false);
    battleSizeDesc.addPara("Only display battles with at least this many total fleet points deployed.", 0);
    container.addUIElement(battleSizeDesc).belowLeft(battleSizeLabel, 5);

    TooltipMakerAPI battleAgeLabel = container.createUIElement(150, 25, false);
    battleAgeLabel.setParaSmallInsignia();
    battleAgeLabel.setParaFontColor(Misc.getBasePlayerColor());
    battleAgeLabel.addPara("Days ago", 3);
    container.addUIElement(battleAgeLabel).belowLeft(battleSizeDesc, 15);
    prevComponent = battleAgeLabel;

    int[] battleAges = {0, 7, 14, 30, 90, 180, 365, 730};
    int selectedBattleAge = (int) pd.get(U.FLEET_HISTORY_BATTLE_AGE);
    for (int age : battleAges) {
      boolean checked = (selectedBattleAge == age);
      TooltipMakerAPI battleAgeBtn = container.createUIElement(50, 25, false);
      btn = battleAgeBtn.addAreaCheckbox(
              age + "",
              U.FLEET_HISTORY_BATTLE_AGE + age,
              Misc.getBasePlayerColor(),
              Misc.getDarkPlayerColor(),
              checked ? Misc.getHighlightColor() : Misc.getBrightPlayerColor(),
              50, 25, 0
      );
      btn.setChecked(checked);
      container.addUIElement(battleAgeBtn).rightOfMid(prevComponent, 2);
      prevComponent = battleAgeBtn;
    }
    TooltipMakerAPI battleAgeDesc = container.createUIElement(width, 0, false);
    battleAgeDesc.addPara("Only display battles within the selected time period (select 0 to show all).", 0);
    container.addUIElement(battleAgeDesc).belowLeft(battleAgeLabel, 5);

    TooltipMakerAPI hideCommandersCheckbox = container.createUIElement(25, 25, false);
    btn = hideCommandersCheckbox.addAreaCheckbox(
            " ", U.FLEET_HISTORY_HIDE_COMMANDERS,
            Misc.getBasePlayerColor(),
            Misc.getDarkPlayerColor(),
            Misc.getBrightPlayerColor(),
            15, 15, 5
    );
    btn.setChecked(pd.containsKey(U.FLEET_HISTORY_HIDE_COMMANDERS));
    container.addUIElement(hideCommandersCheckbox).belowLeft(battleAgeDesc, 15);
    TooltipMakerAPI hideCommandersDesc = container.createUIElement(width, 25, false);
    hideCommandersDesc.addPara("Hide commanders section", 5);
    container.addUIElement(hideCommandersDesc).rightOfMid(hideCommandersCheckbox, 5);

    TooltipMakerAPI hideDeployedCheckbox = container.createUIElement(25, 25, false);
    btn = hideDeployedCheckbox.addAreaCheckbox(
            " ", U.FLEET_HISTORY_HIDE_DEPLOYED,
            Misc.getBasePlayerColor(),
            Misc.getDarkPlayerColor(),
            Misc.getBrightPlayerColor(),
            15, 15, 5
    );
    btn.setChecked(pd.containsKey(U.FLEET_HISTORY_HIDE_DEPLOYED));
    container.addUIElement(hideDeployedCheckbox).belowLeft(hideCommandersCheckbox, 0);
    TooltipMakerAPI hideDeployedDesc = container.createUIElement(width, 25, false);
    hideDeployedDesc.addPara("Hide deployed forces section", 5);
    container.addUIElement(hideDeployedDesc).rightOfMid(hideDeployedCheckbox, 5);

    TooltipMakerAPI spacer = container.createUIElement(width, 1, false);
    spacer.addButton("", "", Color.decode("#222222"), Color.decode("#222222"), width * 0.94f, 1, 0);
    container.addUIElement(spacer).belowLeft(hideDeployedDesc, 20).setXAlignOffset(-65);

    TooltipMakerAPI shipFiltersHeader = container.createUIElement(450, 25, false);
    shipFiltersHeader.setParaInsigniaVeryLarge();
    shipFiltersHeader.setParaFontColor(Misc.getBasePlayerColor());
    shipFiltersHeader.addPara("Ship and Officer Settings", 3);
    container.addUIElement(shipFiltersHeader).belowLeft(spacer, 15).setXAlignOffset(15);

    TooltipMakerAPI shipBattleCountLabel = container.createUIElement(150, 25, false);
    shipBattleCountLabel.setParaSmallInsignia();
    shipBattleCountLabel.setParaFontColor(Misc.getBasePlayerColor());
    shipBattleCountLabel.addPara("Battles fought", 3);
    container.addUIElement(shipBattleCountLabel).belowLeft(shipFiltersHeader, 15).setXAlignOffset(20);
    prevComponent = shipBattleCountLabel;

    int[] shipBattleCounts = {0, 1, 5, 10, 25, 50, 100, 200};
    int selectedBattleCount = (int) pd.get(U.FLEET_HISTORY_SHIP_BATTLE_COUNT);
    for (int count : shipBattleCounts) {
      boolean checked = (selectedBattleCount == count);
      TooltipMakerAPI shipBattleCountBtn = container.createUIElement(50, 25, false);
      btn = shipBattleCountBtn.addAreaCheckbox(
              count + "",
              U.FLEET_HISTORY_SHIP_BATTLE_COUNT + count,
              Misc.getBasePlayerColor(),
              Misc.getDarkPlayerColor(),
              checked ? Misc.getHighlightColor() : Misc.getBrightPlayerColor(),
              50, 25, 0
      );
      btn.setChecked(checked);
      container.addUIElement(shipBattleCountBtn).rightOfMid(prevComponent, 2);
      prevComponent = shipBattleCountBtn;
    }
    TooltipMakerAPI shipBattleCountDesc = container.createUIElement(width, 0, false);
    shipBattleCountDesc.addPara("Only display ships / officers that have been deployed in at least this many battles.", 0);
    container.addUIElement(shipBattleCountDesc).belowLeft(shipBattleCountLabel, 5);

    TooltipMakerAPI shipScoreLabel = container.createUIElement(150, 25, false);
    shipScoreLabel.setParaSmallInsignia();
    shipScoreLabel.setParaFontColor(Misc.getBasePlayerColor());
    shipScoreLabel.addPara("Fleet point score", 3);
    container.addUIElement(shipScoreLabel).belowLeft(shipBattleCountDesc, 15);
    prevComponent = shipScoreLabel;

    int[] shipScores = {0, 1, 10, 50, 100, 200, 500, 1000};
    int selectedShipScore = (int) pd.get(U.FLEET_HISTORY_SHIP_FP_SCORE);
    for (int score : shipScores) {
      boolean checked = (selectedShipScore == score);
      TooltipMakerAPI shipScoreBtn = container.createUIElement(50, 25, false);
      btn = shipScoreBtn.addAreaCheckbox(
              score + "",
              U.FLEET_HISTORY_SHIP_FP_SCORE + score,
              Misc.getBasePlayerColor(),
              Misc.getDarkPlayerColor(),
              checked ? Misc.getHighlightColor() : Misc.getBrightPlayerColor(),
              50, 25, 0
      );
      btn.setChecked(checked);
      container.addUIElement(shipScoreBtn).rightOfMid(prevComponent, 2);
      prevComponent = shipScoreBtn;
    }
    TooltipMakerAPI shipScoreDesc = container.createUIElement(width, 0, false);
    shipScoreDesc.addPara("Only display ships / officers that have killed at least this many FP of enemy ships.", 0);
    container.addUIElement(shipScoreDesc).belowLeft(shipScoreLabel, 5);

    TooltipMakerAPI killDisplayLabel = container.createUIElement(150, 25, false);
    killDisplayLabel.setParaSmallInsignia();
    killDisplayLabel.setParaFontColor(Misc.getBasePlayerColor());
    killDisplayLabel.addPara("Kill count display", 3);
    container.addUIElement(killDisplayLabel).belowLeft(shipScoreDesc, 15);
    prevComponent = killDisplayLabel;

    String[] killDisplays = {U.KILL_DISPLAY_ICONS, U.KILL_DISPLAY_TABLE, U.KILL_DISPLAY_NONE};
    String selectedKillDisplay = (String) pd.get(U.FLEET_HISTORY_KILL_DISPLAY);
    for (String display : killDisplays) {
      boolean checked = (selectedKillDisplay.equals(display));
      TooltipMakerAPI killDisplayBtn = container.createUIElement(102, 25, false);
      btn = killDisplayBtn.addAreaCheckbox(
              display + "",
              display,
              Misc.getBasePlayerColor(),
              Misc.getDarkPlayerColor(),
              checked ? Misc.getHighlightColor() : Misc.getBrightPlayerColor(),
              102, 25, 0
      );
      btn.setChecked(checked);
      container.addUIElement(killDisplayBtn).rightOfMid(prevComponent, 2);
      prevComponent = killDisplayBtn;
    }
    TooltipMakerAPI killDisplayDesc = container.createUIElement(width, 0, false);
    killDisplayDesc.addPara("Select how to display detailed kill counts in battle logs.", 0);
    container.addUIElement(killDisplayDesc).belowLeft(killDisplayLabel, 5);

    TooltipMakerAPI hideInactiveCheckbox = container.createUIElement(25, 25, false);
    btn = hideInactiveCheckbox.addAreaCheckbox(
            " ", U.FLEET_HISTORY_HIDE_INACTIVE,
            Misc.getBasePlayerColor(),
            Misc.getDarkPlayerColor(),
            Misc.getBrightPlayerColor(),
            15, 15, 5
    );
    btn.setChecked(pd.containsKey(U.FLEET_HISTORY_HIDE_INACTIVE));
    hideInactiveCheckbox.addTooltipToPrevious(
            new ButtonTooltip("If checked, ships and officers not currently in your fleet (in storage / unassigned / lost in battle) will be hidden.", 350),
            TooltipMakerAPI.TooltipLocation.BELOW
    );
    container.addUIElement(hideInactiveCheckbox).belowLeft(killDisplayDesc, 15);
    TooltipMakerAPI hideInactiveDesc = container.createUIElement(width, 25, false);
    hideInactiveDesc.addPara("Hide inactive", 5);
    container.addUIElement(hideInactiveDesc).rightOfMid(hideInactiveCheckbox, 5);

    spacer = container.createUIElement(width, 1, false);
    spacer.addButton("", "", Color.decode("#222222"), Color.decode("#222222"), width * 0.94f, 1, 0);
    container.addUIElement(spacer).belowLeft(hideInactiveDesc, 25).setXAlignOffset(-70);

    TooltipMakerAPI clearDataBtn = container.createUIElement(150, 25, false);
    clearDataBtn.addAreaCheckbox("Clear all data", U.FLEET_HISTORY_CLEAR_ALL, Misc.getNegativeHighlightColor(), Color.decode("#442200"), Misc.getNegativeHighlightColor(), 150, 25, 0);
    container.addUIElement(clearDataBtn).belowLeft(spacer, 25).setXAlignOffset(15);
    TooltipMakerAPI clearDataInfo = container.createUIElement(width * 0.75f, 0, false);
    clearDataInfo.addPara("Deletes all stored fleet history data, and prevents any further data from being logged for this play session. This allows the mod to be safely disabled.", 0);
    container.addUIElement(clearDataInfo).rightOfMid(clearDataBtn, 15);

    t.addCustom(container, 0);
    return t;

  }

  @Override
  public boolean doesButtonHaveConfirmDialog(Object buttonId) {
    return U.FLEET_HISTORY_CLEAR_ALL.equals(buttonId);
  }

  @Override
  public void createConfirmationPrompt(Object buttonId, TooltipMakerAPI prompt) {

    if (!U.FLEET_HISTORY_CLEAR_ALL.equals(buttonId)) {
      return;
    }

    prompt.addPara("All fleet history will be removed. %s", 0, Misc.getHighlightColor(), "This cannot be undone!");
    prompt.addPara("", 0);
    prompt.addPara("Do you want to proceed?", 0);

  }

  @Override
  public void buttonPressConfirmed(Object id, IntelUIAPI ui) {

    if (!(id instanceof String)) {
      return;
    }

    String buttonId = (String) id;

    if (buttonId.equals(U.FLEET_HISTORY_CLEAR_ALL)) {
      FleetHistoryModPlugin.clearAllData();
      ui.recreateIntelUI();
      return;
    }

    HashMap<String, Object> pd = U.getPersistentData();

    if (buttonId.startsWith(U.FLEET_HISTORY_BATTLE_SIZE)) {
      
      int selected = Integer.parseInt(buttonId.replace(U.FLEET_HISTORY_BATTLE_SIZE, ""));
      pd.put(U.FLEET_HISTORY_BATTLE_SIZE, selected);
      pd.put(U.FLEET_HISTORY_VIEW_MODE, U.FLEET_HISTORY_VIEW_BATTLES);
      
    } else if (buttonId.startsWith(U.FLEET_HISTORY_BATTLE_AGE)) {
      
      int selected = Integer.parseInt(buttonId.replace(U.FLEET_HISTORY_BATTLE_AGE, ""));
      pd.put(U.FLEET_HISTORY_BATTLE_AGE, selected);
      pd.put(U.FLEET_HISTORY_VIEW_MODE, U.FLEET_HISTORY_VIEW_BATTLES);
      
    } else if (buttonId.startsWith(U.FLEET_HISTORY_SHIP_BATTLE_COUNT)) {
      
      int selected = Integer.parseInt(buttonId.replace(U.FLEET_HISTORY_SHIP_BATTLE_COUNT, ""));
      pd.put(U.FLEET_HISTORY_SHIP_BATTLE_COUNT, selected);
      pd.put(U.FLEET_HISTORY_VIEW_MODE, U.FLEET_HISTORY_VIEW_SHIPS);
      
    } else if (buttonId.startsWith(U.FLEET_HISTORY_SHIP_FP_SCORE)) {
      
      int selected = Integer.parseInt(buttonId.replace(U.FLEET_HISTORY_SHIP_FP_SCORE, ""));
      pd.put(U.FLEET_HISTORY_SHIP_FP_SCORE, selected);
      pd.put(U.FLEET_HISTORY_VIEW_MODE, U.FLEET_HISTORY_VIEW_SHIPS);
      
    } else if (buttonId.startsWith(FLEET_HISTORY_PREFIX)) {
      
      String sortKey = buttonId.replace(FLEET_HISTORY_PREFIX, "");
      if (U.getPersistentData().containsKey(FLEET_HISTORY_SORT_MODE)) {
        String currSortKey = (String) U.getPersistentData().get(FLEET_HISTORY_SORT_MODE);
        if (currSortKey.equals(sortKey)) {
          sortKey += "_REVERSE";
        }
      }
      U.getPersistentData().put(FLEET_HISTORY_SORT_MODE, sortKey);

    } else if (buttonId.startsWith(BATTLE_HISTORY_PREFIX)) {
      
      String sortKey = buttonId.replace(BATTLE_HISTORY_PREFIX, "");
      if (U.getPersistentData().containsKey(BATTLE_HISTORY_SORT_MODE)) {
        String currSortKey = (String) U.getPersistentData().get(BATTLE_HISTORY_SORT_MODE);
        if (currSortKey.equals(sortKey)) {
          sortKey += "_REVERSE";
        }
      }
      U.getPersistentData().put(BATTLE_HISTORY_SORT_MODE, sortKey);
      
    } else if (buttonId.startsWith(OFFICER_HISTORY_PREFIX)) {
      
      String sortKey = buttonId.replace(OFFICER_HISTORY_PREFIX, "");
      if (U.getPersistentData().containsKey(OFFICER_HISTORY_SORT_MODE)) {
        String currSortKey = (String) U.getPersistentData().get(OFFICER_HISTORY_SORT_MODE);
        if (currSortKey.equals(sortKey)) {
          sortKey += "_REVERSE";
        }
      }
      U.getPersistentData().put(OFFICER_HISTORY_SORT_MODE, sortKey);

    } else {    
      
      switch (buttonId) {
        case U.FLEET_HISTORY_VIEW_SHIPS:
        case U.FLEET_HISTORY_VIEW_BATTLES:
        case U.FLEET_HISTORY_VIEW_OFFICERS:
          pd.put(U.FLEET_HISTORY_VIEW_MODE, buttonId);
          pd.remove(U.FLEET_HISTORY_CONFIG);
          break;
        case U.FLEET_HISTORY_CONFIG:
          pd.put(U.FLEET_HISTORY_CONFIG, 1);
          break;
        case U.FLEET_HISTORY_HIDE_COMMANDERS:
        case U.FLEET_HISTORY_HIDE_DEPLOYED:
          if (!pd.containsKey(buttonId)) {
            pd.put(buttonId, true);
          } else {
            pd.remove(buttonId);
          }
          break;
        case U.FLEET_HISTORY_HIDE_INACTIVE:
          if (!pd.containsKey(buttonId)) {
            pd.put(buttonId, true);
          } else {
            pd.remove(buttonId);
          }
          pd.put(U.FLEET_HISTORY_VIEW_MODE, U.FLEET_HISTORY_VIEW_SHIPS);
          break;
        case U.KILL_DISPLAY_ICONS:
        case U.KILL_DISPLAY_TABLE:
        case U.KILL_DISPLAY_NONE:
          pd.put(U.FLEET_HISTORY_KILL_DISPLAY, buttonId);
          break;
        case FACTION_SHIPS_LOST_TO:
          pd.put(FACTION_SHIP_COUNT_MODE, FACTION_SHIPS_DESTROYED);
          break;
        case FACTION_SHIPS_DESTROYED:
          pd.put(FACTION_SHIP_COUNT_MODE, FACTION_SHIPS_LOST_TO);
          break;
        case BATTLES:
          // do nothing - just recreate intel UI so header button doesn't toggle
          break;
      }
      
    }

    if (ui != null) {
      ui.recreateIntelUI();
      ui.updateUIForItem(this);
    }

  }

  @Override
  public String getIcon() {
    String flag = Global.getSector().getPlayerFaction().getCrest();
    if (flag == null) {
      return Global.getSector().getPlayerPerson().getPortraitSprite();
    } else {
      return flag;
    }

  }

  @Override
  public void createIntelInfo(TooltipMakerAPI t, ListInfoMode mode) {
    t.setParaInsigniaLarge();
    t.addPara("Fleet Action History", Global.getSector().getPlayerFaction().getBrightUIColor(), 0);
  }

  @Override
  public Set<String> getIntelTags(SectorMapAPI map) {
    Set<String> tags = super.getIntelTags(map);
    tags.add("Fleet Action History");
    return tags;
  }

  @Override
  public String getSortString() {
    // always display first
    return "-0";
  }

  @Override
  public boolean isHidden() {
    return (U.getShipLogs().isEmpty() && U.getBattleRecords().isEmpty());
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof FleetSummaryIntel;
  }

  @Override
  public int hashCode() {
    return Objects.hash("TestFleetSummaryIntel");
  }

  private class FactionBattleHistory implements Comparable<FactionBattleHistory> {

    String factionId;
    int battles = 0;
    int battlesWon = 0;
    int battlesLost = 0;
    int officers = 0;
    int totalShips = 0;
    int totalFleetPoints = 0;
    int frigates = 0;
    int destroyers = 0;
    int cruisers = 0;
    int capitalShips = 0;
    int stations = 0;

    FactionBattleHistory(String id) {
      this.factionId = id;
    }

    @Override
    public int compareTo(FactionBattleHistory f) {
      return f.totalFleetPoints - this.totalFleetPoints;
    }

  }

}
