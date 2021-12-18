package fleethistory.types;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import fleethistory.U;
import fleethistory.shipevents.ShipBattleRecord;

public class OfficerBattleEntry extends OfficerLogEntry {

  private final String shipId;
  public final String battleRecordId;

  private transient ShipBattleRecord shipBattleRecord;

  public OfficerBattleEntry(String shipId, long timestamp, String battleRecordId) {
    this.shipId = U.getCache().cacheString(shipId);
    this.timestamp = U.encodeNum(timestamp);
    // battleRecordId already compressed, no need to cache
    this.battleRecordId = battleRecordId;
  }

  public OfficerBattleEntry(String compressedString) {
    String[] s = compressedString.split("\\|");
    // s[0] is entry type identifier "B|"
    this.shipId = s[1];
    this.timestamp = s[2];
    this.battleRecordId = s[3];
  }

  public ShipBattleRecord getBattleRecord() {

    if (this.shipBattleRecord != null) {
      return this.shipBattleRecord;
    }

    ShipLog s = U.getShipLogFor(this.getShipId());

    for (ShipLogEntry sle : s.events) {
      if (sle.getTimestamp() == this.getTimestamp() && sle.event instanceof ShipBattleRecord) {
        ShipBattleRecord sbr = (ShipBattleRecord) sle.event;
        if (sbr.battleRecordId.equals(this.battleRecordId)) {
          this.shipBattleRecord = sbr;
          return sbr;
        }
      }
    }

    throw new IllegalStateException(
            String.format(
                    "No battle record found for officer battle entry [%s] [%s] [%s]",
                    shipId,
                    timestamp,
                    battleRecordId
            )
    );

  }

  @Override
  public String getCompressedString() {
    return String.format("B|%s|%s|%s", this.shipId, this.timestamp, this.battleRecordId);
  }

  public String getShipId() {
    return U.getCache().getCachedString(this.shipId);
  }

  @Override
  public void render(CustomPanelAPI panel, float width, float height) {

    float imageSize = 60f;

    ShipBattleRecord sbr = this.getBattleRecord();
    ShipInfo si = U.getShipLogFor(this.getShipId()).info;

//    TooltipMakerAPI shipImg = panel.createUIElement(imageSize, imageSize, false);
//    shipImg.addImage(si.getHullSpec().getSpriteName(), imageSize, imageSize, 0);
//    panel.addUIElement(shipImg).inTL(0, 0);
    TooltipMakerAPI shipInfo = panel.createUIElement(width - imageSize, 0, false);
    shipInfo.addPara(
            U.i18n("officer_command_string"),
            0,
            Misc.getBrightPlayerColor(),
            si.getShipName(),
            si.getHullSpec().getNameWithDesignationWithDashClass()
    );
    sbr.renderOutcomeString(shipInfo);
    sbr.renderBattleStats(shipInfo);
    //panel.addUIElement(shipInfo).rightOfTop(shipImg, 15);
    panel.addUIElement(shipInfo).inTL(0, U.LINE_SPACING);

    CustomPanelAPI killTable = panel.createCustomPanel(width, 0, null);
    sbr.renderKillTable(killTable, width, height);
    panel.addComponent(killTable).belowLeft(shipInfo, 0);

    panel.getPosition().setSize(
            width,
            killTable.getPosition().getHeight() + shipInfo.getPosition().getHeight()
    );

  }

}
