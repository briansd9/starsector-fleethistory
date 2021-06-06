/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fleethistory.intel;

import fleethistory.U;
import fleethistory.types.ShipLogEntry;
import fleethistory.types.ShipLog;
import fleethistory.shipevents.ShipBattleRecord;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.thoughtworks.xstream.XStream;
import java.awt.Color;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.log4j.Logger;
import fleethistory.shipevents.ShipBattleRecordStats;
import fleethistory.tooltips.ShipKillCountTooltip;
import fleethistory.types.OfficerLog;
import fleethistory.types.ShipEvent;
import java.util.Collection;

/**
 *
 * @author joshi
 */
public class ShipLogIntel extends BaseFleetHistoryIntelPlugin {

  private static final Logger log = Global.getLogger(ShipLogIntel.class);

  private static final String SHIP_LOG_PAGE_NUMBER_PREFIX = "SHIP_LOG_PAGE_";
  private static final String CURRENT_SHIP_LOG_ID = "CURRENT_SHIP_LOG_ID";
  private static final String CURRENT_SHIP_LOG_PAGE = "CURRENT_SHIP_LOG_PAGE";
  private static final int MAX_EVENTS_PER_PAGE = 25;

  private final String fleetMemberId;

  public ShipLogIntel(String id) {
    this.fleetMemberId = id;
  }

  public static void alias(XStream x) {
    x.aliasAttribute(ShipLogIntel.class, "fleetMemberId", "f");
  }

  @Override
  public String getSortString() {

    ShipLog s = U.getShipLogFor(this.fleetMemberId);

    // highest sort priority: ships that fought in the previous battle
    int deployedInPreviousBattle = U.deployedInPreviousBattle(this.fleetMemberId) ? 0 : 1;
    int prevBattleFP = 0;
    // for these ships, sort by fleetpoints descending
    if (deployedInPreviousBattle == 0 && s.getLastBattleRecord() != null) {
      prevBattleFP = s.getLastBattleRecord().getFleetPoints();
    }

    // then ships currently in fleet
    int currentMember = 1;
    List<FleetMemberAPI> playerFleet = Global.getSector().getPlayerFleet().getMembersWithFightersCopy();
    for (FleetMemberAPI f : playerFleet) {
      if (this.fleetMemberId.equals(f.getId())) {
        currentMember = 0;
        break;
      }
    }

    // then sort by total FP, kills, assists
    return deployedInPreviousBattle
            + " " + (Integer.MAX_VALUE - prevBattleFP)
            + " " + currentMember
            + " " + (Integer.MAX_VALUE - s.getFleetPointScore())
            + " " + (Integer.MAX_VALUE - s.getKills())
            + " " + (Integer.MAX_VALUE - s.getAssists());

  }

  @Override
  public String getIcon() {
    return U.getShipLogFor(this.fleetMemberId).info.getHullSpec().getSpriteName();
  }

  public int getPageNumber() {
    HashMap<String, Object> pd = U.getPersistentData();
    if (!pd.containsKey(CURRENT_SHIP_LOG_ID) || !pd.containsKey(CURRENT_SHIP_LOG_PAGE) || !this.fleetMemberId.equals((String) pd.get(CURRENT_SHIP_LOG_ID))) {
      pd.put(CURRENT_SHIP_LOG_ID, this.fleetMemberId);
      pd.put(CURRENT_SHIP_LOG_PAGE, 0);
      return 0;
    } else {
      return (int) pd.get(CURRENT_SHIP_LOG_PAGE);
    }
  }

  public void setPageNumber(int pageNumber) {
    HashMap<String, Object> pd = U.getPersistentData();
    pd.put(CURRENT_SHIP_LOG_ID, this.fleetMemberId);
    pd.put(CURRENT_SHIP_LOG_PAGE, pageNumber);
  }

  public String getViewMode() {

    ShipLog s = U.getShipLogFor(this.fleetMemberId);
    if (s.getKills() == 0 && s.getAssists() == 0) {
      // if ship has no kills, just show event log
      return U.BATTLE_LOG;
    } else {
      return (String) U.getPersistentData().get(U.LOG_VIEW_MODE_KEY);
    }

  }

  public void setViewMode(String viewMode) {
    HashMap<String, Object> pd = U.getPersistentData();
    pd.put(CURRENT_SHIP_LOG_ID, this.fleetMemberId);
    pd.put(U.LOG_VIEW_MODE_KEY, viewMode);
  }

  @Override
  public void createLargeDescription(CustomPanelAPI panel, float width, float height) {

    ShipLog shipLog = U.getShipLogFor(this.fleetMemberId);

    float infoBannerHeight = height * 0.25f;
    float topPadding = 40;
    float navButtonsPadding = 0;
    String viewMode = getViewMode();

    try {

      // top banner - ship info and total kill count
      // return the TooltipMakerAPI object so we can place other elements relative to it
      TooltipMakerAPI infoBanner = createInfoBanner(panel, width, infoBannerHeight);

      // buttons irrelevant for non-combat ships - only event log is shown
      if (shipLog.getKills() > 0 || shipLog.getAssists() > 0) {
        HashMap<String, Object> pd = U.getPersistentData();
        TooltipMakerAPI button1 = panel.createUIElement(100, 25, false);
        button1.addAreaCheckbox("Kill List", U.KILL_LIST,
                Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(),
                U.KILL_LIST.equals((String) pd.get(U.LOG_VIEW_MODE_KEY)) ? Misc.getHighlightColor() : Misc.getBrightPlayerColor(),
                100,
                25,
                0
        );
        panel.addUIElement(button1).belowLeft(infoBanner, 10);
        TooltipMakerAPI button2 = panel.createUIElement(100, 20, false);
        button2.addAreaCheckbox("Battle Log", U.BATTLE_LOG,
                Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(),
                U.BATTLE_LOG.equals((String) pd.get(U.LOG_VIEW_MODE_KEY)) ? Misc.getHighlightColor() : Misc.getBrightPlayerColor(),
                100,
                25,
                0
        );
        panel.addUIElement(button2).rightOfMid(button1, 5);
        topPadding += 10;
        navButtonsPadding += 203;
      } else {
        topPadding = 15;
      }

      // log entries list
      if (viewMode.equals(U.BATTLE_LOG)) {
        if (shipLog.events.size() > MAX_EVENTS_PER_PAGE) {
          createNavButtons(infoBanner, panel, navButtonsPadding, width, height);
        }
        TooltipMakerAPI entryList = createEntryList(panel, width * 0.95f, height - infoBannerHeight - topPadding);
        panel.addUIElement(entryList).belowLeft(infoBanner, topPadding);
      } else if (viewMode.equals(U.KILL_LIST)) {
        TooltipMakerAPI killList = createKillList(panel, width * 0.95f, height - infoBannerHeight - topPadding);
        panel.addUIElement(killList).belowLeft(infoBanner, topPadding);
      }

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  public TooltipMakerAPI createKillList(CustomPanelAPI panel, float width, float height) {

    ShipLog shipLog = U.getShipLogFor(this.fleetMemberId);

    final HashMap<String, Integer> kills = new HashMap<>();
    final HashMap<String, Integer> assists = new HashMap<>();
    final HashMap<String, Integer> fleetPoints = new HashMap<>();

    for (ShipLogEntry e : shipLog.events) {
      if (e.type.equals(ShipLogEntry.EventType.COMBAT)) {
        ShipBattleRecord br = (ShipBattleRecord) e.event;
        for (ShipBattleRecordStats s : br.getStats()) {

          String hullId = s.getHullId();

          int shipKills = s.getKills();
          if (shipKills > 0) {
            if (!kills.containsKey(hullId)) {
              kills.put(hullId, 0);
            }
            kills.put(hullId, kills.get(hullId) + shipKills);
          }

          int shipAssists = s.getAssists();
          if (shipAssists > 0) {
            if (!assists.containsKey(hullId)) {
              assists.put(hullId, 0);
            }
            assists.put(hullId, assists.get(hullId) + shipAssists);
          }

          int shipFP = s.getFleetPoints();
          if (!fleetPoints.containsKey(hullId)) {
            fleetPoints.put(hullId, 0);
          }
          fleetPoints.put(hullId, fleetPoints.get(hullId) + shipFP);

        }
      }
    }

    String[] keys = fleetPoints.keySet().toArray(new String[fleetPoints.size()]);
    for (String key : keys) {
      if (!kills.containsKey(key)) {
        kills.put(key, 0);
      }
      if (!assists.containsKey(key)) {
        assists.put(key, 0);
      }
    }

    Arrays.sort(keys, new Comparator<String>() {
      @Override
      public int compare(String s1, String s2) {
        int f1 = fleetPoints.get(s1), f2 = fleetPoints.get(s2);
        if (f1 != f2) {
          return f2 - f1;
        }
        int k1 = kills.get(s1), k2 = kills.get(s2);
        if (k1 != k2) {
          return k2 - k1;
        }
        int a1 = assists.get(s1), a2 = assists.get(s2);
        if (a1 != a2) {
          return a2 - a1;
        }
        return s2.compareTo(s1);
      }
    });

    TooltipMakerAPI killList = panel.createUIElement(width, height, true);

    HashMap<String, Object> pd = U.getPersistentData();
    if (U.KILL_DISPLAY_ICONS.equals(pd.get(U.FLEET_HISTORY_KILL_DISPLAY))) {

      float IMG_SIZE = 80f;
      float SPACING = 20f;

      int index = 0;
      int elementsPerRow = (int) Math.floor(width / (IMG_SIZE + SPACING));

      float calculatedHeight = (IMG_SIZE + SPACING) * (float) Math.ceil(keys.length / elementsPerRow);

      CustomPanelAPI content = panel.createCustomPanel(width, calculatedHeight, null);

      for (String key : keys) {

        ShipHullSpecAPI hull = Global.getSettings().getHullSpec(key);
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
        CustomPanelAPI innerContainer = content.createCustomPanel(IMG_SIZE, IMG_SIZE, null);

        TooltipMakerAPI shipImg = innerContainer.createUIElement(scaledWidth, scaledHeight, false);
        shipImg.addImage(hull.getSpriteName(), scaledWidth, scaledHeight, 0);
        innerContainer.addUIElement(shipImg).inMid();

        int fp = fleetPoints.get(key);
        int k = kills.get(key);
        int a = assists.get(key);

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

        outerContainer.addTooltipToPrevious(new ShipKillCountTooltip(key, k, a, fp),
                TooltipMakerAPI.TooltipLocation.BELOW
        );

        int x = (int) ((IMG_SIZE + SPACING) * (index % elementsPerRow));
        int y = (int) ((IMG_SIZE + SPACING) * (index / elementsPerRow) + 5);

        content.addUIElement(outerContainer).inTL(x, y);
        index++;

      }
      killList.addCustom(content, 0);

    } else {

      killList.beginTable(
              Global.getSector().getPlayerFaction(), 20,
              "Ship Type", width * 0.45f,
              "Kills", width * 0.1f,
              "Assists", width * 0.1f,
              "Total fleet points", width * 0.2f
      );
      for (String key : keys) {
        ShipHullSpecAPI hull = Global.getSettings().getHullSpec(key);
        String hullName = U.isStation(hull) ? hull.getHullName() : hull.getNameWithDesignationWithDashClass();
        killList.addRow(
                Alignment.LMID, Misc.getTextColor(), hullName,
                Alignment.MID, Misc.getTextColor(), kills.get(key) + "",
                Alignment.MID, Misc.getTextColor(), assists.get(key) + "",
                Alignment.MID, Misc.getTextColor(), fleetPoints.get(key) + ""
        );
      }
      killList.addTable("", 0, 0);
      killList.addSpacer(15);

    }

    return killList;

  }

  public TooltipMakerAPI createInfoBanner(CustomPanelAPI panel, float width, float height) {

    ShipLog shipLog = U.getShipLogFor(this.fleetMemberId);

    TooltipMakerAPI infoBanner = panel.createUIElement(width - 150, height, false);
    CustomPanelAPI content = panel.createCustomPanel(width - 150, height, null);

    CustomPanelAPI imageContainer = content.createCustomPanel(height, height, null);
    SpriteAPI sprite = Global.getSettings().getSprite(shipLog.info.getHullSpec().getSpriteName());
    float scaledWidth = sprite.getWidth();
    float scaledHeight = sprite.getHeight();
    if (scaledWidth >= scaledHeight && scaledWidth >= height) {
      scaledHeight = height * (scaledHeight / scaledWidth);
      scaledWidth = height;
    } else if (scaledHeight > scaledWidth && scaledHeight > height) {
      scaledWidth = height * (scaledWidth / scaledHeight);
      scaledHeight = height;
    }
    float scaleFactor = U.hullSizeScalar(shipLog.info.getHullSpec().getHullSize());
    scaledWidth *= scaleFactor;
    scaledHeight *= scaleFactor;
    TooltipMakerAPI image = imageContainer.createUIElement(scaledWidth, scaledHeight, false);
    image.addImage(shipLog.info.getHullSpec().getSpriteName(), scaledWidth, scaledHeight, 0);
    imageContainer.addUIElement(image).inMid();
    content.addComponent(imageContainer).inTL(0, 0);

    TooltipMakerAPI text = content.createUIElement(width - 150, 0, false);
    text.addSpacer(5);
    text.setParaInsigniaLarge();
    text.addPara(shipLog.info.getShipName(), getTitleColor(ListInfoMode.INTEL), 0);
    text.setParaFontDefault();
    text.addPara(shipLog.info.getHullSpec().getNameWithDesignationWithDashClass(), 0);
    
    String shipFullName = shipLog.info.getShipName() + ", " + shipLog.info.getHullSpec().getNameWithDesignationWithDashClass();
    for(OfficerLog ol : U.getOfficerLogs().values()) {
      if(shipFullName.equals(ol.getCurrentShipAssignment())) {
        String officerString = "Commanding officer: %s";
        if(Global.getSector().getPlayerPerson().getId().equals(ol.getId())) {
          officerString = "Flagship of %s's fleet";
        }
        text.addPara(officerString, 0, Misc.getBrightPlayerColor(), ol.getName());
        break;
      }
    }

    int combats = shipLog.getCombats();
    if (combats > 0) {
      int recovered = shipLog.getRecovered();
      if (recovered > 0) {
        text.addPara(
                "Deployed in %s battle" + (combats > 1 ? "s" : "") + " (recovered %s time" + (recovered > 1 ? "s" : "") + ")",
                0,
                Misc.getBrightPlayerColor(),
                combats + "",
                recovered + ""
        );
      } else {
        text.addPara("Deployed in %s battle" + (combats > 1 ? "s" : ""), 0, Misc.getBrightPlayerColor(), combats + "");
      }
      int kills = shipLog.getKills();
      int assists = shipLog.getAssists();
      int points = shipLog.getFleetPointScore();
      if (kills > 0 && assists > 0) {
        text.addPara(
                "%s kill" + (kills > 1 ? "s" : "") + " and %s assist" + (assists > 1 ? "s" : "") + " (total %s fleet point" + (points > 1 ? "s" : "") + ")",
                0,
                new Color[]{
                  Misc.getNegativeHighlightColor(),
                  Misc.getHighlightColor(),
                  Misc.getBrightPlayerColor()
                },
                kills + "",
                assists + "",
                points + ""
        );
      } else if (kills > 0) {
        text.addPara(
                "%s kill" + (kills > 1 ? "s" : "") + " (total %s fleet point" + (points > 1 ? "s" : "") + ")",
                0,
                new Color[]{
                  Misc.getNegativeHighlightColor(),
                  Misc.getBrightPlayerColor()
                },
                kills + "",
                points + ""
        );
      } else if (assists > 0) {
        text.addPara(
                "%s assist" + (assists > 1 ? "s" : "") + " (total %s fleet point" + (points > 1 ? "s" : "") + ")",
                0,
                new Color[]{
                  Misc.getHighlightColor(),
                  Misc.getBrightPlayerColor()
                },
                assists + "",
                points + ""
        );
      }
    }
    content.addUIElement(text).rightOfTop(imageContainer, 25);


    /*    
    TooltipMakerAPI text = infoBanner.beginImageWithText(shipLog.info.spriteName, height);
    infoBanner.addImageWithText(0);

     */
    infoBanner.addComponent(content);
    panel.addUIElement(infoBanner).inTL(25, 0);
    return infoBanner;

  }

  public void createNavButtons(TooltipMakerAPI infoBanner, CustomPanelAPI panel, float xOffset, float width, float height) {

    ShipLog shipLog = U.getShipLogFor(this.fleetMemberId);

    TooltipMakerAPI spacer = panel.createUIElement(5, 25, false);
    spacer.addPara("", 0);
    panel.addUIElement(spacer).belowLeft(infoBanner, 9).setXAlignOffset(xOffset);

    TooltipMakerAPI prevElem = spacer;

    int numPages = (shipLog.events.size() / MAX_EVENTS_PER_PAGE) - (shipLog.events.size() % MAX_EVENTS_PER_PAGE == 0 ? 1 : 0);
    int currPage = getPageNumber();

    for (int i = 0; i <= numPages; i++) {
      TooltipMakerAPI pageBtn = panel.createUIElement(25, 25, false);
      pageBtn.addAreaCheckbox((i + 1) + "",
              SHIP_LOG_PAGE_NUMBER_PREFIX + i,
              Misc.getBasePlayerColor(),
              Misc.getDarkPlayerColor(),
              (i == currPage ? Misc.getHighlightColor() : Misc.getBrightPlayerColor()),
              25, 25, 3
      );
      panel.addUIElement(pageBtn).rightOfMid(prevElem, 2);
      prevElem = pageBtn;
    }

  }

  public TooltipMakerAPI createEntryList(CustomPanelAPI panel, float width, float height) {

    ShipLog shipLog = U.getShipLogFor(this.fleetMemberId);

    TooltipMakerAPI entryList = panel.createUIElement(width, height, true);
    CustomPanelAPI container = panel.createCustomPanel(width, 0, null);

    TooltipMakerAPI content = container.createUIElement(width, 0, false);

    int pageNumber = getPageNumber();
    float dateWidth = 100f;

    for (int i = 0; i < MAX_EVENTS_PER_PAGE; i++) {
      
      int index = (shipLog.events.size() - 1) - (pageNumber * MAX_EVENTS_PER_PAGE) - i;
      if (index < 0 || index >= shipLog.events.size()) {
        break;
      }

      ShipLogEntry e = shipLog.events.get(index);
      CustomPanelAPI entryContainer = container.createCustomPanel(width, 0, null);

      TooltipMakerAPI entryDate = entryContainer.createUIElement(dateWidth, 0, false);
      entryDate.setParaFontOrbitron();
      entryDate.addPara(
              U.dateString(e.getTimestamp()),
              Misc.getBasePlayerColor(),
              U.LINE_SPACING
      );
      entryContainer.addUIElement(entryDate).inTL(0, 0);

      CustomPanelAPI entryContent = entryContainer.createCustomPanel(width - dateWidth, 0, null);
      ShipEvent se = (ShipEvent) e.event;
      se.render(entryContent, width, 0);
      entryContainer.addComponent(entryContent).rightOfTop(entryDate, 0);
      entryContainer.getPosition().setSize(width, entryContent.getPosition().getHeight());

      content.addCustom(entryContainer, 0);
      content.addSpacer(40);
      
    }

    container.addUIElement(content).inTL(0, 0);
    container.getPosition().setSize(width, content.getPosition().getHeight());

    entryList.addCustom(content, 0);
    return entryList;

  }

  @Override
  public Set<String> getIntelTags(SectorMapAPI map) {
    Set<String> tags = super.getIntelTags(map);
    tags.add("Fleet Action History");
    return tags;
  }

  @Override
  public void createIntelInfo(TooltipMakerAPI t, ListInfoMode mode) {

    ShipLog shipLog = U.getShipLogFor(this.fleetMemberId);
    boolean isCurrentFleetMember = shipLog.isCurrentFleetMember();

    t.addPara(shipLog.info.getShipName(), isCurrentFleetMember ? Misc.getBrightPlayerColor() : Misc.getDarkPlayerColor(), 0);
    t.addPara(shipLog.info.getHullSpec().getNameWithDesignationWithDashClass(), isCurrentFleetMember ? Misc.getTextColor() : Misc.getGrayColor(), 0);

    Color highlightColor = isCurrentFleetMember ? Misc.getHighlightColor() : Misc.getDarkHighlightColor();
    t.setParaFontColor(isCurrentFleetMember ? Misc.getTextColor() : Misc.getGrayColor());

    int combats = shipLog.getCombats();
    int kills = shipLog.getKills();
    int assists = shipLog.getAssists();
    int fp = shipLog.getFleetPointScore();

    if (combats > 0) {
      t.addPara(
              "%s battle" + (combats != 1 ? "s" : ""),
              0,
              (isCurrentFleetMember ? Misc.getBrightPlayerColor() : Misc.getDarkPlayerColor()),
              combats + ""
      );
      t.addPara(
              "%s kill" + (kills != 1 ? "s" : "") + ", %s assist" + (assists != 1 ? "s" : "") + ", %s FP",
              0,
              new Color[]{
                (isCurrentFleetMember ? Misc.getNegativeHighlightColor() : Misc.scaleColorOnly(Misc.getNegativeHighlightColor(), 0.5f)),
                (isCurrentFleetMember ? Misc.getHighlightColor() : Misc.getDarkHighlightColor()),
                (isCurrentFleetMember ? Misc.getBrightPlayerColor() : Misc.getDarkPlayerColor())
              },
              kills + "",
              assists + "",
              fp + ""
      );
    }

    if (U.deployedInPreviousBattle(this.fleetMemberId)) {
      ShipBattleRecord sbr = shipLog.getLastBattleRecord();
      if (sbr != null) {
        t.addPara(
                "Previous battle: %s / %s / %s",
                0,
                new Color[]{
                  (isCurrentFleetMember ? Misc.getNegativeHighlightColor() : Misc.scaleColorOnly(Misc.getNegativeHighlightColor(), 0.5f)),
                  (isCurrentFleetMember ? Misc.getHighlightColor() : Misc.getDarkHighlightColor()),
                  (isCurrentFleetMember ? Misc.getBrightPlayerColor() : Misc.getDarkPlayerColor())
                },
                sbr.getKills() + "",
                sbr.getAssists() + "",
                sbr.getFleetPoints() + ""
        );
      }
    }

  }

  @Override
  public boolean isHidden() {

    HashMap<String, Object> pd = U.getPersistentData();
    if (!((String) pd.get(U.FLEET_HISTORY_VIEW_MODE)).equals(U.FLEET_HISTORY_VIEW_SHIPS)) {
      return true;
    }

    ShipLog s = U.getShipLogFor(this.fleetMemberId);
    if (pd.containsKey(U.FLEET_HISTORY_HIDE_INACTIVE) && !s.isCurrentFleetMember()) {
      return true;
    }
    if ((int) U.getPersistentData().get(U.FLEET_HISTORY_SHIP_BATTLE_COUNT) > 0) {
      int minBattleCount = (int) U.getPersistentData().get(U.FLEET_HISTORY_SHIP_BATTLE_COUNT);
      if (s.getCombats() < minBattleCount) {
        return true;
      }
    }
    if ((int) U.getPersistentData().get(U.FLEET_HISTORY_SHIP_FP_SCORE) > 0) {
      int minScore = (int) U.getPersistentData().get(U.FLEET_HISTORY_SHIP_FP_SCORE);
      if (s.getFleetPointScore() < minScore) {
        return true;
      }
    }

    return false;
  }

  @Override
  public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
    
    String id = (String) buttonId;
    if (id.startsWith(SHIP_LOG_PAGE_NUMBER_PREFIX)) {
      setPageNumber(Integer.parseInt(id.replace(SHIP_LOG_PAGE_NUMBER_PREFIX, "")));
      if (ui != null) {
        ui.updateUIForItem(this);
      }
      return;
    }

    switch (id) {
      case U.BATTLE_LOG:
      case U.KILL_LIST:
        setViewMode(id);
        ui.updateUIForItem(this);
        break;
    }
  }

  @Override
  public boolean shouldRemoveIntel() {
    return !U.getShipLogs().containsKey(fleetMemberId) || U.getShipLogFor(fleetMemberId).events.isEmpty();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof ShipLogIntel && this.fleetMemberId.equals(((ShipLogIntel) obj).fleetMemberId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.fleetMemberId);
  }

}
