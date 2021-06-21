/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fleethistory.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.thoughtworks.xstream.XStream;
import fleethistory.U;
import fleethistory.tooltips.CaptainTooltip;
import fleethistory.tooltips.ShipCountTooltip;
import fleethistory.tooltips.ShipTooltip;
import fleethistory.types.BattleRecord;
import fleethistory.types.BattleRecordExtraInfo;
import fleethistory.types.BattleRecordFleetInfo;
import fleethistory.types.BattleRecordPersonInfo;
import fleethistory.types.BattleRecordShipCount;
import fleethistory.types.BattleRecordShipInfo;
import fleethistory.types.BattleRecordSideCount;
import fleethistory.types.BattleRecordSideInfo;
import fleethistory.types.ShipBattleResult;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import org.apache.log4j.Logger;

/**
 *
 * @author joshi
 */
public class BattleRecordIntel extends BaseFleetHistoryIntelPlugin {

  private static final Logger log = Global.getLogger(BattleRecordIntel.class);
  private static final float SECTION_SPACING = 20f;
  private static final float IMG_SIZE = 50f;
  private static final float ICON_SPACING = 12f;
  private static final float EXPLO_SCALE_FACTOR = 0.75f;

  public String battleRecordId;

  public BattleRecordIntel(String battleRecordId) {
    this.battleRecordId = battleRecordId;
  }

  public static void alias(XStream x) {
    x.aliasAttribute(BattleRecordIntel.class, "battleRecordId", "b");
  }

  private BattleRecord getBattleRecord() {
    return ((HashMap<String, BattleRecord>) U.getBattleRecords()).get(this.battleRecordId);
  }

  @Override
  public String getSortString() {
    return getBattleRecord().getTimestamp() + "";
  }

  @Override
  public String getIcon() {
    return Global.getSector().getFaction(getBattleRecord().getEnemyFactionId()).getCrest();
  }

  @Override
  public void createLargeDescription(CustomPanelAPI cp, float width, float height) {

    try {

      HashMap<String, Object> pd = U.getPersistentData();

      float BOX_WIDTH = width * 0.93f;
      float calculatedHeight = 0f;

      CustomPanelAPI panel = cp.createCustomPanel(width, calculatedHeight, null);

      TooltipMakerAPI topInfo = createTopInfo(panel, BOX_WIDTH);
      calculatedHeight += topInfo.getPosition().getHeight();
      panel.addUIElement(topInfo).inTL(0, 0);

      TooltipMakerAPI sidesSection = createSidesSection(panel, BOX_WIDTH);
      float sidesSectionOffset = SECTION_SPACING + sidesSection.getPosition().getHeight();
      calculatedHeight += sidesSectionOffset;
      panel.addUIElement(sidesSection).belowMid(topInfo, 15);

      TooltipMakerAPI lossesSection = createLossesSection(panel, BOX_WIDTH);
       // height of this section hardcoded - always contains 4 lines of text
      float lossesSectionOffset = 100; 
      calculatedHeight += lossesSectionOffset;
      panel.addUIElement(lossesSection).belowMid(sidesSection, sidesSectionOffset);

      TooltipMakerAPI officersSection = createOfficersSection(panel, BOX_WIDTH);
      float officersSectionOffset = SECTION_SPACING + officersSection.getPosition().getHeight();
      calculatedHeight += officersSectionOffset;
      panel.addUIElement(officersSection).belowMid(lossesSection, lossesSectionOffset);

      TooltipMakerAPI strengthsSection = createStrengthsSection(panel, BOX_WIDTH);
      float strengthsSectionOffset = SECTION_SPACING + strengthsSection.getPosition().getHeight();
      calculatedHeight += strengthsSectionOffset;
      panel.addUIElement(strengthsSection).belowMid(officersSection, officersSectionOffset);
      
      panel.getPosition().setSize(width, calculatedHeight + 150);

      TooltipMakerAPI container = cp.createUIElement(width * 0.978f, height, true);
      container.addCustom(panel, 0);
      container.addSpacer(50);
      cp.addUIElement(container).inTL(0, 0);
      

    } catch (Exception e) {
      e.printStackTrace();
    }

  }
  
  private TooltipMakerAPI createTopInfo(CustomPanelAPI panel, float width) {

    BattleRecord br = getBattleRecord();

    float calculatedHeight = 75f;
    if (br.extraInfo != null) {
      calculatedHeight += br.extraInfo.size() * 20;
    }

    TooltipMakerAPI outerContainer = panel.createUIElement(width, calculatedHeight, false);
    CustomPanelAPI container = panel.createCustomPanel(width, calculatedHeight, null);

    float labelWidth = width * 0.15f;

    TooltipMakerAPI dateLabel = container.createUIElement(labelWidth, 0, false);
    dateLabel.addPara(U.i18n("battle_date"), 0);
    container.addUIElement(dateLabel).inTL(0, U.LINE_SPACING);
    TooltipMakerAPI dateText = container.createUIElement(width - labelWidth, 0, false);
    dateText.setParaFontColor(Misc.getHighlightColor());
    dateText.addPara(Global.getSector().getClock().createClock(br.getTimestamp()).getDateString(), 0);
    container.addUIElement(dateText).rightOfMid(dateLabel, 0);

    TooltipMakerAPI locLabel = container.createUIElement(labelWidth, 0, false);
    locLabel.addPara(U.i18n("battle_location"), 0);
    container.addUIElement(locLabel).belowMid(dateLabel, U.LINE_SPACING);
    TooltipMakerAPI locText = container.createUIElement(width - labelWidth, 0, false);
    locText.setParaFontColor(Misc.getHighlightColor());
    locText.addPara(br.getLocation(), 0);
    container.addUIElement(locText).rightOfMid(locLabel, 0);

    TooltipMakerAPI resLabel = container.createUIElement(labelWidth, 0, false);
    resLabel.addPara(U.i18n("battle_result"), 0);
    container.addUIElement(resLabel).belowMid(locLabel, U.LINE_SPACING);
    TooltipMakerAPI resText = container.createUIElement(width - labelWidth, 0, false);
    resText.setParaFontColor(Misc.getHighlightColor());
    resText.addPara(getOutcomeString(), 0);
    if (br.extraInfo != null) {
      for (BattleRecordExtraInfo info : br.extraInfo) {
        switch (info.key) {
          case BattleRecordExtraInfo.BOUNTY_COMPLETED:
            resText.setParaFontColor(Misc.getTextColor());
            resText.addPara(U.i18n("bounty_claimed"), U.LINE_SPACING, Misc.getHighlightColor(), info.data);
            break;
        }
      }
    }

    container.addUIElement(resText).rightOfTop(resLabel, 0);
    outerContainer.addComponent(container);
    return outerContainer;

  }

  private String getOutcomeString() {

    BattleRecord br = getBattleRecord();
    float playerLost = br.playerSide.getLostCount().fp;
    float playerDeployed = br.playerSide.getDeployedCount().fp;
    float playerLossPct = playerLost / playerDeployed;
    float enemyLost = br.enemySide.getLostCount().fp;
    float enemyDeployed = br.enemySide.getDeployedCount().fp;
    float enemyLossPct = enemyLost / enemyDeployed;

    if (br.playerWon) {
      if (enemyLost >= 5 * playerDeployed) {
        return U.i18n("victory_heroic");
      }
      if (playerLost == 0 && enemyLossPct >= 0.75) {
        return U.i18n("victory_overwhelming");
      }
      if (playerLossPct >= 0.75 && enemyLost < playerLost) {
        return U.i18n("victory_pyrrhic");
      }
      // both sides' deployed forces and losses within 20%
      if (Math.max(playerDeployed, enemyDeployed) / Math.max(1, Math.min(playerDeployed, enemyDeployed)) <= 1.2
              && Math.max(playerLost, enemyLost) / Math.max(1, Math.min(playerLost, enemyLost)) <= 1.2) {
        return U.i18n("victory_close");
      }
      return U.i18n("victory");
    } else {
      if (enemyLost >= 5 * playerDeployed) {
        return U.i18n("defeat_heroic");
      }
      if (enemyLost == 0 && playerLossPct >= 0.75) {
        return U.i18n("defeat_utter");
      }
      if (playerLossPct >= 0.75 && enemyLost < playerLost) {
        return U.i18n("defeat_crushing");
      }
      if (Math.max(playerDeployed, enemyDeployed) / Math.max(1, Math.min(playerDeployed, enemyDeployed)) <= 1.2
              && Math.max(playerLost, enemyLost) / Math.max(1, Math.min(playerLost, enemyLost)) <= 1.2) {
        return U.i18n("defeat_close");
      }
      return U.i18n("defeat");
    }

  }

  private TooltipMakerAPI createLossesSection(CustomPanelAPI panel, float width) {

    BattleRecord br = getBattleRecord();

    TooltipMakerAPI outerContainer = panel.createUIElement(width, 0, false);
    CustomPanelAPI container = panel.createCustomPanel(width, 0, null);

    TooltipMakerAPI header = container.createUIElement(width, 0, false);
    header.addSectionHeading(U.i18n("casualties_header"), Alignment.MID, 0);
    container.addUIElement(header).inTL(0, 0);

    TooltipMakerAPI playerLossSection = createLossSection(container, width, br.playerSide);
    container.addUIElement(playerLossSection).belowLeft(header, 0);

    TooltipMakerAPI enemyLossSection = createLossSection(container, width, br.enemySide);
    container.addUIElement(enemyLossSection).belowRight(header, 0);

    outerContainer.addComponent(container);
    return outerContainer;

  }

  private TooltipMakerAPI createLossSection(CustomPanelAPI container, float width, BattleRecordSideInfo side) {

    BattleRecord br = getBattleRecord();

    TooltipMakerAPI t = container.createUIElement(width / 2f, 0, false);
    BattleRecordSideCount deployed = side.getDeployedCount();
    BattleRecordSideCount lost = side.getLostCount();
    if (lost.fp == 0) {
      t.addPara(U.i18n("no_ships_lost"), Misc.getPositiveHighlightColor(), U.LINE_SPACING);
      t.addPara(
              U.i18n(deployed.ships != 1 ? "ships_deployed" : "ship_deployed"),
              U.LINE_SPACING,
              Misc.getHighlightColor(),
              new String[]{
                deployed.ships + "",
                deployed.fp + ""
              }
      );
    } else if (lost.fp == deployed.fp) {
      t.setParaFontColor(Misc.getNegativeHighlightColor());
      t.addPara(U.i18n("all_ships_lost"), U.LINE_SPACING);
      t.setParaFontColor(Misc.getTextColor());
      t.addPara(
              U.i18n(deployed.ships != 1 ? "ships_lost" : "ship_lost"),
              U.LINE_SPACING,
              Misc.getHighlightColor(),
              new String[]{
                lost.ships + "",
                lost.fp + "",}
      );
    } else {
      t.addPara(
              U.i18n("some_ships_lost"),
              U.LINE_SPACING,
              Misc.getNegativeHighlightColor(),
              U.format(100f * lost.fp / deployed.fp) + "%"
      );
      t.addPara(
              U.i18n("ships_lost_fraction"),
              U.LINE_SPACING,
              Misc.getHighlightColor(),
              new String[]{
                lost.ships + "",
                deployed.ships + "",
                lost.fp + "",
                deployed.fp + "",}
      );
    }

    // player officers are always recovered after battle???
    if (lost.officers > 0 && side.equals(br.enemySide)) {
      t.addPara(
              U.i18n(lost.officers == 1 ? "officer_lost" : "officers_lost"),
              U.LINE_SPACING,
              Misc.getHighlightColor(),
              lost.officers + ""
      );
    }

    return t;
  }

  private TooltipMakerAPI createSidesSection(CustomPanelAPI panel, float width) {

    BattleRecord br = getBattleRecord();
    TooltipMakerAPI outerContainer = panel.createUIElement(width, 0, false);
    CustomPanelAPI container = panel.createCustomPanel(width, 0, null);

    TooltipMakerAPI header = container.createUIElement(width, 0, false);
    header.addSectionHeading(U.i18n("fleets_header"), Alignment.MID, 0);
    container.addUIElement(header).inTL(0, 0);

    TooltipMakerAPI playerSideSection = createSideSection(container, width, br.playerSide.fleets);
    float calculatedHeight = playerSideSection.getPosition().getHeight();
    container.addUIElement(playerSideSection).belowLeft(header, 0);

    TooltipMakerAPI enemySideSection = createSideSection(container, width, br.enemySide.fleets);
    calculatedHeight = Math.max(calculatedHeight, enemySideSection.getPosition().getHeight());
    container.addUIElement(enemySideSection).belowRight(header, 0);

    outerContainer.addComponent(container);
    outerContainer.getPosition().setSize(width, 20 + calculatedHeight);
    
    return outerContainer;

  }

  private TooltipMakerAPI createSideSection(CustomPanelAPI container, float width, Collection<BattleRecordFleetInfo> fleets) {
    
    TooltipMakerAPI side = container.createUIElement(width / 2f, 0, false);
    
    HashMap<String, ArrayList<String>> sides = new HashMap<>();
    for(BattleRecordFleetInfo fleet: fleets) {
      String factionId = fleet.getFactionId();
      if(!sides.containsKey(factionId)) {
        sides.put(factionId, new ArrayList<String>());
      }
      sides.get(factionId).add(fleet.getFleetName());
    }
    
    float calculatedHeight = 0f;
    
    for(String factionId : sides.keySet()) {
      
      CustomPanelAPI innerContainer = container.createCustomPanel(width / 2f, 0, null);
      
      TooltipMakerAPI factionImg = innerContainer.createUIElement(IMG_SIZE, IMG_SIZE, false);
      FactionAPI f = Global.getSector().getFaction(factionId);
      factionImg.addImage(f.getCrest(), IMG_SIZE, IMG_SIZE, 0);
      innerContainer.addUIElement(factionImg).inTL(10, 10 + calculatedHeight);
      
      ArrayList<String> fleetArr = sides.get(factionId);
      Collections.sort(fleetArr);
      
      TooltipMakerAPI fleetList = innerContainer.createUIElement(width/2f - IMG_SIZE, 0, false);
      for(int i = 0; i < fleetArr.size(); i++) {
        String fleetName = fleetArr.get(i);
        fleetList.addPara(fleetName.replace("pirate", "Pirate"), f.getBrightUIColor(), (i == 0 ? 0 : U.LINE_SPACING));
      }
      innerContainer.addUIElement(fleetList).rightOfTop(factionImg, 15);
      
      side.addComponent(innerContainer);
      calculatedHeight += 10 + Math.max(factionImg.getPosition().getHeight(), fleetList.getPosition().getHeight());
      
    }
    
    side.getPosition().setSize(width/2f, calculatedHeight);
    return side;
    
  }

  private TooltipMakerAPI createOfficersSection(CustomPanelAPI panel, float width) {

    BattleRecord br = getBattleRecord();
    TooltipMakerAPI outerContainer = panel.createUIElement(width, 0, false);

    if (U.getPersistentData().containsKey(U.FLEET_HISTORY_HIDE_COMMANDERS) || (br.playerSide.officers.isEmpty() && br.enemySide.officers.isEmpty())) {
      return outerContainer;
    }

    CustomPanelAPI container = panel.createCustomPanel(width, 0, null);

    TooltipMakerAPI header = container.createUIElement(width, 0, false);
    header.addSectionHeading(U.i18n("commanders_header"), Alignment.MID, 0);
    container.addUIElement(header).inTL(0, 0);

    TooltipMakerAPI playerOfficerSection = createOfficerSection(container, width, br.playerSide.officers);
    float calculatedHeight = playerOfficerSection.getPosition().getHeight();
    container.addUIElement(playerOfficerSection).belowLeft(header, 0);

    TooltipMakerAPI enemyOfficerSection = createOfficerSection(container, width, br.enemySide.officers);
    calculatedHeight = Math.max(calculatedHeight, enemyOfficerSection.getPosition().getHeight());
    container.addUIElement(enemyOfficerSection).belowRight(header, 0);

    outerContainer.addComponent(container);
    outerContainer.getPosition().setSize(width, 20 + calculatedHeight);
    
    return outerContainer;

  }

  private TooltipMakerAPI createOfficerSection(CustomPanelAPI container, float width, Collection<BattleRecordPersonInfo> officers) {
    TooltipMakerAPI officerList = container.createUIElement(width / 2f, 0, false);
    if (officers.size() <= 4) {
      boolean first = true;
      for (BattleRecordPersonInfo officer : officers) {
        if (officer.getName().length() == 0) {
          continue;
        }
        TooltipMakerAPI t = officerList.beginImageWithText(officer.getSpriteId(), IMG_SIZE);
        t.addPara(officer.getName(), 0);
        if(first) {
          officerList.addImageWithText(U.LINE_SPACING);
          first = false;
        } else {
          officerList.addImageWithText(ICON_SPACING);
        }
        if (officer.getShipName() != null) {
          officerList.addTooltipToPrevious(new CaptainTooltip(officer), TooltipMakerAPI.TooltipLocation.ABOVE);
        }
      }
      officerList.getPosition().setSize(width / 2f, officers.size() * (IMG_SIZE + ICON_SPACING));
    } else {
      CustomPanelAPI innerContainer = container.createCustomPanel(width / 2f, 0, null);
      int currCounter = 0;
      int positionIncrement = (int) (IMG_SIZE + ICON_SPACING);
      int elementsPerRow = (int) (width / 2f * 0.9f / positionIncrement);
      for (final BattleRecordPersonInfo officer : officers) {
        TooltipMakerAPI officerImg = innerContainer.createUIElement(IMG_SIZE, IMG_SIZE, false);
        officerImg.addImage(officer.getSpriteId(), IMG_SIZE, IMG_SIZE, 0);
        officerImg.addTooltipToPrevious(new CaptainTooltip(officer, true), TooltipMakerAPI.TooltipLocation.ABOVE);
        int xPos = currCounter % elementsPerRow;
        int yPos = currCounter / elementsPerRow;
        innerContainer.addUIElement(officerImg).inTL(xPos * positionIncrement, 10 + yPos * positionIncrement);
        currCounter++;
      }
      officerList.addComponent(innerContainer);
      officerList.getPosition().setSize(width / 2f, (float)Math.ceil(officers.size() * 1.0f / elementsPerRow) * (IMG_SIZE + ICON_SPACING) );
    }
    return officerList;
  }

  private TooltipMakerAPI createStrengthsSection(CustomPanelAPI panel, float width) {

    BattleRecord br = getBattleRecord();

    TooltipMakerAPI outerContainer = panel.createUIElement(width, 0, false);
    if (U.getPersistentData().containsKey(U.FLEET_HISTORY_HIDE_DEPLOYED)) {
      return outerContainer;
    }

    CustomPanelAPI container = panel.createCustomPanel(width, 0, null);

    TooltipMakerAPI header = container.createUIElement(width, 0, false);
    header.addSectionHeading(U.i18n("strengths_header"), Alignment.MID, 0);
    container.addUIElement(header).inTL(0, 0);

    TooltipMakerAPI playerStrengthSection = createStrengthSection(container, width, br.playerSide);
    float calculatedHeight = playerStrengthSection.getPosition().getHeight();
    container.addUIElement(playerStrengthSection).belowLeft(header, 0);

    TooltipMakerAPI enemyStrengthSection = createStrengthSection(container, width, br.enemySide);
    calculatedHeight = Math.max(calculatedHeight, enemyStrengthSection.getPosition().getHeight());
    container.addUIElement(enemyStrengthSection).belowRight(header, 0);

    outerContainer.addComponent(container);
    outerContainer.getPosition().setSize(width, 20 + calculatedHeight);
    
    return outerContainer;
  }

  private TooltipMakerAPI createStrengthSection(CustomPanelAPI container, float width, BattleRecordSideInfo side) {
    
    int SHIPS_PER_ROW = (int) ((width / 2 * 0.9f) / (IMG_SIZE + ICON_SPACING));
    float calculatedHeight = (float) Math.ceil(side.shipCounts.size() / SHIPS_PER_ROW) * (IMG_SIZE + ICON_SPACING);
    TooltipMakerAPI outerContainer = container.createUIElement(width / 2f, calculatedHeight, false);
    CustomPanelAPI shipList = container.createCustomPanel(width / 2f, calculatedHeight, null);
    int index = 0;
    for (BattleRecordShipInfo s : side.ships) {
      float x = (index % SHIPS_PER_ROW) * (IMG_SIZE + ICON_SPACING);
      float y = 10 + (index / SHIPS_PER_ROW) * (IMG_SIZE + ICON_SPACING);
      TooltipMakerAPI shipImg = createShipIcon(shipList, s);
      shipList.addUIElement(shipImg).inTL(x, y);
      index++;
    }
    // after player ships section, start allied ships section on new row
    while(index % SHIPS_PER_ROW != 0) {
      index++;
    }
    for (BattleRecordShipCount c : side.shipCounts) {
      float x = (index % SHIPS_PER_ROW) * (IMG_SIZE + ICON_SPACING);
      float y = 10 + (index / SHIPS_PER_ROW) * (IMG_SIZE + ICON_SPACING);
      TooltipMakerAPI shipImg = createShipCountIcon(shipList, c);
      shipList.addUIElement(shipImg).inTL(x, y);
      index++;
    }

    outerContainer.addComponent(shipList);
    outerContainer.getPosition().setSize(width / 2, calculatedHeight);
    return outerContainer;
  }

  private TooltipMakerAPI createShipIcon(CustomPanelAPI container, BattleRecordShipInfo s) {

    BattleRecord br = getBattleRecord();

    ShipHullSpecAPI hull = s.getHullSpec();
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

    TooltipMakerAPI outerContainer = container.createUIElement(IMG_SIZE, IMG_SIZE, false);
    CustomPanelAPI innerContainer = container.createCustomPanel(IMG_SIZE, IMG_SIZE, null);

    TooltipMakerAPI shipImg = innerContainer.createUIElement(scaledWidth, scaledHeight, false);
    shipImg.addImage(hull.getSpriteName(), scaledWidth, scaledHeight, 0);
    shipImg.addTooltipToPrevious(new ShipTooltip(s, br.getTimestamp()), TooltipMakerAPI.TooltipLocation.BELOW);
    innerContainer.addUIElement(shipImg).inMid();

    TooltipMakerAPI playerShipIndicator = innerContainer.createUIElement(16, 16, false);
    playerShipIndicator.addImage(Global.getSector().getPlayerFaction().getCrest(), 16, 16, 0);
    innerContainer.addUIElement(playerShipIndicator).inTL(0, 0);    
    if (ShipBattleResult.isLost(s.status)) {
      TooltipMakerAPI explo = innerContainer.createUIElement(IMG_SIZE * EXPLO_SCALE_FACTOR, IMG_SIZE * EXPLO_SCALE_FACTOR, false);
      explo.addImage(Global.getSettings().getSpriteName("fh", "explosion"), IMG_SIZE * EXPLO_SCALE_FACTOR, IMG_SIZE * EXPLO_SCALE_FACTOR, 0);
      innerContainer.addUIElement(explo).inMid();
    }
    outerContainer.addComponent(innerContainer);
    return outerContainer;

  }

  private TooltipMakerAPI createShipCountIcon(CustomPanelAPI container, BattleRecordShipCount c) {

    ShipHullSpecAPI hull = c.getHullSpec();
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

    TooltipMakerAPI outerContainer = container.createUIElement(IMG_SIZE, IMG_SIZE, false);
    CustomPanelAPI innerContainer = container.createCustomPanel(IMG_SIZE, IMG_SIZE, null);

    TooltipMakerAPI tooltipHolder = innerContainer.createUIElement(IMG_SIZE, IMG_SIZE, false);
    CustomPanelAPI innerTooltipHolder = innerContainer.createCustomPanel(IMG_SIZE, IMG_SIZE, null);
    tooltipHolder.addCustom(innerTooltipHolder, 0);
    tooltipHolder.addTooltipToPrevious(new ShipCountTooltip(c), TooltipMakerAPI.TooltipLocation.BELOW);
    innerContainer.addUIElement(tooltipHolder).inTL(0, 0);

    TooltipMakerAPI shipImg = innerContainer.createUIElement(scaledWidth, scaledHeight, false);
    shipImg.addImage(hull.getSpriteName(), scaledWidth, scaledHeight, 0);
    innerContainer.addUIElement(shipImg).inMid();

    int lost = c.getLost();
    int total = c.getTotal();

    if (!U.isStation(c.getHullSpec())) {
      TooltipMakerAPI caption = innerContainer.createUIElement(IMG_SIZE, IMG_SIZE, false);
      if (total == lost) {
        caption.addPara(total + "", Misc.getNegativeHighlightColor(), 0);
      } else if (lost == 0) {
        caption.addPara(total + "", Misc.getHighlightColor(), 0);
      } else {
        caption.addPara(
                "%s / %s",
                0,
                new Color[]{Misc.getNegativeHighlightColor(), Misc.getHighlightColor()},
                lost + "",
                total + ""
        );
      }
      innerContainer.addUIElement(caption).inBL(0, 0);
    }

    if (lost == total) {
      TooltipMakerAPI explo = innerContainer.createUIElement(IMG_SIZE * EXPLO_SCALE_FACTOR, IMG_SIZE * EXPLO_SCALE_FACTOR, false);
      explo.addImage(Global.getSettings().getSpriteName("fh", "explosion"), IMG_SIZE * EXPLO_SCALE_FACTOR, IMG_SIZE * EXPLO_SCALE_FACTOR, 0);
      innerContainer.addUIElement(explo).inMid();
    }

    outerContainer.addComponent(innerContainer);
    return outerContainer;

  }

  @Override
  public void createIntelInfo(TooltipMakerAPI t, ListInfoMode mode) {
    BattleRecord br = getBattleRecord();
    t.addPara(
            U.i18n("battle_versus"),
            0,
            new Color[]{
              Misc.getHighlightColor(),
              Global.getSector().getFaction(br.getEnemyFactionId()).getBaseUIColor()
            },
            new String[]{
              getOutcomeString(),
              Global.getSector().getFaction(br.getEnemyFactionId()).getDisplayName()
            }
    );
    t.addPara(Global.getSector().getClock().createClock(br.getTimestamp()).getShortDate() + " - " + br.getLocation(), 0);
  }

  @Override
  public boolean isDone() {
    return !U.getBattleRecords().containsKey(this.battleRecordId);
  }

  @Override
  public boolean isHidden() {

    if (!((String) U.getPersistentData().get(U.FLEET_HISTORY_VIEW_MODE)).equals(U.FLEET_HISTORY_VIEW_BATTLES)) {
      return true;
    }

    BattleRecord br = getBattleRecord();

    if ((int) U.getPersistentData().get(U.FLEET_HISTORY_BATTLE_SIZE) > 0) {
      int minBattleSize = (int) U.getPersistentData().get(U.FLEET_HISTORY_BATTLE_SIZE);
      if (br.playerSide.getDeployedCount().fp + br.enemySide.getDeployedCount().fp < minBattleSize) {
        return true;
      }
    }

    if ((int) U.getPersistentData().get(U.FLEET_HISTORY_BATTLE_AGE) > 0) {
      int maxBattleAge = (int) U.getPersistentData().get(U.FLEET_HISTORY_BATTLE_AGE);
      if (Global.getSector().getClock().getElapsedDaysSince(br.getTimestamp()) >= maxBattleAge) {
        return true;
      }
    }

    return false;

  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof BattleRecordIntel && this.battleRecordId.equals(((BattleRecordIntel) obj).battleRecordId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.battleRecordId);
  }

}
