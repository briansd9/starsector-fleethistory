package fleethistory.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import com.thoughtworks.xstream.XStream;
import fleethistory.U;
import fleethistory.shipevents.ShipBattleRecordStats;
import fleethistory.tooltips.ButtonTooltip;
import fleethistory.tooltips.ShipKillCountTooltip;
import fleethistory.types.OfficerBattleEntry;
import fleethistory.types.OfficerLog;
import fleethistory.types.OfficerLogEntry;
import fleethistory.types.ShipInfo;
import java.awt.Color;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;

public class OfficerLogIntel extends BaseFleetHistoryIntelPlugin {

  private static final String OFFICER_LOG_PAGE_NUMBER_PREFIX = "OFFICER_LOG_PAGE_";
  private static final String CURRENT_OFFICER_LOG_ID = "CURRENT_OFFICER_LOG_ID";
  private static final String CURRENT_OFFICER_LOG_PAGE = "CURRENT_OFFICER_LOG_PAGE";
  private static final int MAX_EVENTS_PER_PAGE = 25;

  private final String officerId;
  private transient OfficerLog officerLog;

  public OfficerLogIntel(String officerId) {
    this.officerId = officerId;
  }

  public static void alias(XStream x) {
    x.aliasAttribute(OfficerLogIntel.class, "officerId", "c");
  }

  private OfficerLog getLog() {
    if (officerLog == null) {
      officerLog = U.getOfficerLogFor(this.officerId);
    }
    return officerLog;
  }

  @Override
  public void createIntelInfo(TooltipMakerAPI t, ListInfoMode mode) {

    try {

      OfficerLog o = this.getLog();
      boolean isActive = (o.getCurrentShipAssignment() != null);

      t.setParaFontColor(isActive ? Misc.getTextColor() : Misc.getGrayColor());

      t.addPara(
              o.getName(),
              isActive ? Misc.getBrightPlayerColor() : Misc.getDarkPlayerColor(),
              0
      );
      t.addPara(
              U.i18n("officer_level"),
              0,
              isActive ? Misc.getHighlightColor() : Misc.getDarkHighlightColor(),
              o.getLevel() + ""
      );

      if (o.getStats().battles > 0) {
        t.addPara(
                U.i18n(o.getStats().battles == 1 ? "battle_count" : "battles_count"),
                0,
                (isActive ? Misc.getBrightPlayerColor() : Misc.getDarkPlayerColor()),
                o.getStats().battles + ""
        );
        String killStats = String.format("%s, %s %s",
                U.i18n(o.getStats().kills == 1 ? "kill_count" : "kills_count"),
                U.i18n(o.getStats().assists == 1 ? "assist_count" : "assists_count"),
                U.i18n("total_fp_count")
        );
        t.addPara(
                killStats, 
                0,
                new Color[]{
                  (isActive ? Misc.getNegativeHighlightColor() : Misc.scaleColorOnly(Misc.getNegativeHighlightColor(), 0.5f)),
                  (isActive ? Misc.getHighlightColor() : Misc.getDarkHighlightColor()),
                  (isActive ? Misc.getBrightPlayerColor() : Misc.getDarkPlayerColor())
                },
                o.getStats().kills + "",
                o.getStats().assists + "",
                o.getStats().fleetPoints + ""
        );
      }

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  @Override
  public void createLargeDescription(CustomPanelAPI panel, float width, float height) {

    try {

      OfficerLog o = this.getLog();

      float bannerSize = height * 0.26f;
      float iconSize = 42f;
      float iconPadding = 8f;

      // officer headshot in top left
      TooltipMakerAPI officerImg = panel.createUIElement(bannerSize, bannerSize, false);
      officerImg.addImage(o.getSprite(), bannerSize, bannerSize, 0);
      panel.addUIElement(officerImg).inTL(25, 0);

      // officer info - name
      TooltipMakerAPI officerInfo = panel.createUIElement(width - bannerSize - 25, 15, false);
      officerInfo.setParaInsigniaLarge();
      officerInfo.addPara(o.getName(), getTitleColor(ListInfoMode.INTEL), 0);
      panel.addUIElement(officerInfo).rightOfTop(officerImg, 15);

      // officer info - level
      TooltipMakerAPI officerLevel = panel.createUIElement(bannerSize, 10, false);
      officerLevel.setParaFontVictor14();
      officerLevel.addPara(U.i18n("officer_level"), 0, Misc.getHighlightColor(), o.getLevel() + "");
      panel.addUIElement(officerLevel).belowLeft(officerInfo, 0);

      // skill icons
      UIComponentAPI iconRowStart = null;
      UIComponentAPI prevElem = null;

      // if no skill icons to draw, anchor on officer level element instead
      if (o.getSkills().isEmpty()) {
        iconRowStart = officerLevel;
      } else {
        for (String s : o.getSkills()) {

          String skillId = U.getCache().getCachedString(s);
          boolean isElite = false;
          if (skillId.startsWith("ELITE_")) {
            isElite = true;
            skillId = skillId.substring(6);
          }
          SkillSpecAPI spec = Global.getSettings().getSkillSpec(skillId);

          CustomPanelAPI iconPanel = panel.createCustomPanel(iconSize, iconSize, null);
          TooltipMakerAPI skillImg = iconPanel.createUIElement(iconSize, iconSize, false);
          skillImg.addImage(spec.getSpriteName(), iconSize, iconSize, 0);
          skillImg.addTooltipToPrevious(
                  new ButtonTooltip(spec.getName() + (isElite ? " (elite)" : "")),
                  TooltipMakerAPI.TooltipLocation.ABOVE
          );
          iconPanel.addUIElement(skillImg).inTL(0, 0);

          if (isElite) {
            TooltipMakerAPI eliteImg = iconPanel.createUIElement(16, 16, false);
            eliteImg.addImage("graphics/icons/insignia/16x_star_circle.png", 16, 16, 0);
            iconPanel.addUIElement(eliteImg).inTL(2, 2);
          }

          if (prevElem == null) {
            panel.addComponent(iconPanel).belowLeft(officerLevel, U.LINE_SPACING);
          } else {
            panel.addComponent(iconPanel).rightOfMid(prevElem, iconPadding);
          }
          if (iconRowStart == null) {
            iconRowStart = iconPanel;
          }
          prevElem = iconPanel;

        }

      }

      // officer info - service time, current ship assignment
      OfficerLog.OfficerBattleStats obs = o.getStats();
      TooltipMakerAPI officerStats = panel.createUIElement(width - bannerSize - 25, 0, false);
      if (o.getId().equals(Global.getSector().getPlayerFleet().getCommander().getId())) {
        officerStats.addPara(U.i18n("fleet_commander"), Misc.getBrightPlayerColor(), 0);
        String currentShip = o.getCurrentShipAssignment();
        if (currentShip != null) {
          officerStats.addPara(U.i18n("commanding_flagship"), 0, Misc.getBrightPlayerColor(), currentShip);
        }
      } else if (obs.firstTimestamp != 0) {
        String currentShip = o.getCurrentShipAssignment();
        if (currentShip != null) {
          officerStats.addPara(U.i18n("active_service_since"), 0, Misc.getBrightPlayerColor(), U.dateString(obs.firstTimestamp));
          officerStats.addPara(
                  U.i18n("commanding"),
                  0,
                  Misc.getBrightPlayerColor(),
                  currentShip
          );
        } else {
          officerStats.addPara(U.i18n("active_service_from_to"), 0, Misc.getBrightPlayerColor(), U.dateString(obs.firstTimestamp), U.dateString(obs.lastTimestamp));
          for(int i = o.getEntries().size() - 1; i >= 0; i--) {
            OfficerLogEntry ole = o.getEntries().get(i);
            if(ole instanceof OfficerBattleEntry) {
              OfficerBattleEntry obe = (OfficerBattleEntry)ole;
              ShipInfo si = U.getShipLogFor(obe.getShipId()).info;
              officerStats.addPara(
                      U.i18n("last_commanded"),
                      0,
                      Misc.getBrightPlayerColor(),
                      si.getShipName() + ", " + si.getHullSpec().getNameWithDesignationWithDashClass()
              );
              break;
            }
          }
        }
      }

      // officer info - battle stats
      if (obs.battles > 0) {
        if (obs.timesKilled > 0) {
          String battleStats = String.format(
                  "%s %s",
                  U.i18n(obs.battles == 1 ? "officer_battle_fought" : "officer_battles_fought"),
                  U.i18n(obs.timesKilled == 1 ? "officer_rescue" : "officer_rescues")
          );
          officerStats.addPara(
                  battleStats,
                  0,
                  Misc.getBrightPlayerColor(),
                  obs.battles + "", obs.timesKilled + ""
          );
        } else {
          officerStats.addPara(
                  U.i18n(obs.battles == 1 ? "officer_battle_fought" : "officer_battles_fought"),
                  0,
                  Misc.getBrightPlayerColor(), 
                  obs.battles + ""
          );
        }
        if (obs.kills > 0 && obs.assists > 0) {
          String killStats = String.format("%s, %s %s",
                  U.i18n(obs.kills == 1 ? "kill_count" : "kills_count"),
                  U.i18n(obs.assists == 1 ? "assist_count" : "assists_count"),
                  U.i18n("total_fp_count")
          );
          officerStats.addPara(killStats, 0,
                  new Color[]{Misc.getNegativeHighlightColor(), Misc.getHighlightColor(), Misc.getBrightPlayerColor()},
                  obs.kills + "", obs.assists + "", obs.fleetPoints + ""
          );
        } else if (obs.kills > 0) {
          String killStats = String.format("%s %s", U.i18n(obs.kills == 1 ? "kill_count" : "kills_count"), U.i18n("total_fp_count"));
          officerStats.addPara(killStats, 0, new Color[]{Misc.getNegativeHighlightColor(), Misc.getBrightPlayerColor()}, obs.kills + "", obs.fleetPoints + "");
        } else if (obs.assists > 0) {
          String killStats = String.format("%s %s", U.i18n(obs.assists == 1 ? "assist_count" : "assists_count"), U.i18n("total_fp_count"));
          officerStats.addPara(killStats, 0, new Color[]{Misc.getHighlightColor(), Misc.getBrightPlayerColor()}, obs.assists + "", obs.fleetPoints + "");
        }
      }
      panel.addUIElement(officerStats).belowLeft(iconRowStart, U.LINE_SPACING);

      float navButtonsPadding = 0;
      if (obs.kills > 0 || obs.assists > 0) {
        HashMap<String, Object> pd = U.getPersistentData();
        TooltipMakerAPI button1 = panel.createUIElement(100, 25, false);
        button1.addAreaCheckbox(
                U.i18n("kill_list"),
                U.KILL_LIST,
                Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(),
                U.KILL_LIST.equals(this.getViewMode()) ? Misc.getHighlightColor() : Misc.getBrightPlayerColor(),
                100,
                25,
                0
        );
        panel.addUIElement(button1).belowLeft(officerImg, 15);
        TooltipMakerAPI button2 = panel.createUIElement(100, 25, false);
        button2.addAreaCheckbox(
                U.i18n("battle_log"),
                U.BATTLE_LOG,
                Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(),
                U.BATTLE_LOG.equals(this.getViewMode()) ? Misc.getHighlightColor() : Misc.getBrightPlayerColor(),
                100,
                25,
                0
        );
        panel.addUIElement(button2).rightOfMid(button1, 5);
        navButtonsPadding = 205;
      }

      // add navigation buttons if needed
      if (o.getEntries().size() > MAX_EVENTS_PER_PAGE && U.BATTLE_LOG.equals(this.getViewMode())) {

        TooltipMakerAPI spacer = panel.createUIElement(5, 25, false);
        spacer.addPara("", 0);
        panel.addUIElement(spacer).belowLeft(officerImg, 15).setXAlignOffset(navButtonsPadding);

        TooltipMakerAPI prevBtn = spacer;
        int numPages = (int) Math.ceil(o.getEntries().size() / MAX_EVENTS_PER_PAGE) - (o.getEntries().size() % MAX_EVENTS_PER_PAGE == 0 ? 1 : 0);
        int currPage = getPageNumber();
        for (int i = 0; i <= numPages; i++) {
          TooltipMakerAPI pageBtn = panel.createUIElement(25, 25, false);
          pageBtn.addAreaCheckbox((i + 1) + "",
                  OFFICER_LOG_PAGE_NUMBER_PREFIX + i,
                  Misc.getBasePlayerColor(),
                  Misc.getDarkPlayerColor(),
                  (i == currPage ? Misc.getHighlightColor() : Misc.getBrightPlayerColor()),
                  25, 25, 0
          );
          panel.addUIElement(pageBtn).rightOfMid(prevBtn, 2);
          prevBtn = pageBtn;
        }

      }

      TooltipMakerAPI scroll = panel.createUIElement(width * 0.95f, height - bannerSize - 50, true);
      CustomPanelAPI container = null;

      if (U.BATTLE_LOG.equals(this.getViewMode())) {
        container = this.createEventLog(panel, width * 0.95f);
      } else if (U.KILL_LIST.equals(this.getViewMode())) {
        container = this.createKillList(panel, width * 0.95f);
      }

      scroll.addCustom(container, 5);
      panel.addUIElement(scroll).belowLeft(officerImg, 50);

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  private CustomPanelAPI createEventLog(CustomPanelAPI panel, float width) {

    CustomPanelAPI container = panel.createCustomPanel(width, 0, null);

    OfficerLog o = this.getLog();
    float calculatedHeight = 0;
    CustomPanelAPI prevEntry = null;
    float dateWidth = 100f;
    float entryPadding = 40f;

    int pageNumber = getPageNumber();
    for (int i = 0; i < MAX_EVENTS_PER_PAGE; i++) {

      int index = (o.getEntries().size() - 1) - (pageNumber * MAX_EVENTS_PER_PAGE) - i;
      if (index < 0 || index >= o.getEntries().size()) {
        break;
      }

      OfficerLogEntry e = o.getEntries().get(index);
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
      e.render(entryContent, width, 0);
      entryContainer.addComponent(entryContent).rightOfTop(entryDate, 0);
      entryContainer.getPosition().setSize(width, entryContent.getPosition().getHeight());

      if (prevEntry == null) {
        container.addComponent(entryContainer).inTL(0, 0);
      } else {
        container.addComponent(entryContainer).belowLeft(prevEntry, entryPadding);
      }
      prevEntry = entryContainer;

      calculatedHeight += entryPadding + entryContent.getPosition().getHeight();

    }

    container.getPosition().setSize(width, calculatedHeight);
    return container;

  }
  
  private CustomPanelAPI createKillList(CustomPanelAPI panel, float width) {

    OfficerLog o = this.getLog();

    final HashMap<String, Integer> kills = new HashMap<>();
    final HashMap<String, Integer> assists = new HashMap<>();
    final HashMap<String, Integer> fleetPoints = new HashMap<>();

    for (OfficerLogEntry e : o.getEntries()) {
      if (e instanceof OfficerBattleEntry) {
        OfficerBattleEntry br = (OfficerBattleEntry) e;
        for (ShipBattleRecordStats s : br.getBattleRecord().getStats()) {

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
      return content;

    } else {

      CustomPanelAPI content = panel.createCustomPanel(width, 0, null);
      TooltipMakerAPI killList = content.createUIElement(width, 0, false);
      killList.beginTable(
              Global.getSector().getPlayerFaction(), 20,
              U.i18n("ship_type"), width * 0.45f,
              U.i18n("kills"), width * 0.1f,
              U.i18n("assists"), width * 0.1f,
              U.i18n("total_fleet_points"), width * 0.2f
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
      content.addUIElement(killList);
      content.getPosition().setSize(width, killList.getPosition().getHeight());
      return content;

    }

  }

  @Override
  public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {

    String id = (String) buttonId;
    if (id.startsWith(OFFICER_LOG_PAGE_NUMBER_PREFIX)) {
      setPageNumber(Integer.parseInt(id.replace(OFFICER_LOG_PAGE_NUMBER_PREFIX, "")));
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

  public void setViewMode(String viewMode) {
    HashMap<String, Object> pd = U.getPersistentData();
    pd.put(CURRENT_OFFICER_LOG_ID, this.officerId);
    pd.put(U.LOG_VIEW_MODE_KEY, viewMode);
  }

  public String getViewMode() {

    OfficerLog o = this.getLog();
    if (o.getStats().kills == 0 && o.getStats().assists == 0) {
      return U.BATTLE_LOG;
    } else {
      return (String) U.getPersistentData().get(U.LOG_VIEW_MODE_KEY);
    }

  }

  public int getPageNumber() {
    HashMap<String, Object> pd = U.getPersistentData();
    if (!pd.containsKey(CURRENT_OFFICER_LOG_ID) || !pd.containsKey(CURRENT_OFFICER_LOG_PAGE) || !this.officerId.equals((String) pd.get(CURRENT_OFFICER_LOG_ID))) {
      pd.put(CURRENT_OFFICER_LOG_ID, this.officerId);
      pd.put(CURRENT_OFFICER_LOG_PAGE, 0);
      return 0;
    } else {
      return (int) pd.get(CURRENT_OFFICER_LOG_PAGE);
    }
  }

  public void setPageNumber(int pageNumber) {
    HashMap<String, Object> pd = U.getPersistentData();
    pd.put(CURRENT_OFFICER_LOG_ID, this.officerId);
    pd.put(CURRENT_OFFICER_LOG_PAGE, pageNumber);
  }

  @Override
  public String getIcon() {
    return this.getLog().getSprite();
  }

  @Override
  public String getSortString() {

    OfficerLog o = this.getLog();
    OfficerLog.OfficerBattleStats obs = o.getStats();

    // sort: active officers first, then sort by descending FP / kills / assists
    return (o.getCurrentShipAssignment() == null ? 1 : 0)
            + " " + (Integer.MAX_VALUE - obs.fleetPoints)
            + " " + (Integer.MAX_VALUE - obs.kills)
            + " " + (Integer.MAX_VALUE - obs.assists);

  }

  @Override
  public boolean isHidden() {

    HashMap<String, Object> pd = U.getPersistentData();
    if (!((String) pd.get(U.FLEET_HISTORY_VIEW_MODE)).equals(U.FLEET_HISTORY_VIEW_OFFICERS)) {
      return true;
    }

    OfficerLog o = this.getLog();
    if (pd.containsKey(U.FLEET_HISTORY_HIDE_INACTIVE) && o.getCurrentShipAssignment() == null) {
      return true;
    }

    return false;

  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof OfficerLogIntel && this.officerId.equals(((OfficerLogIntel) obj).officerId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.officerId);
  }

}
