package fleethistory.tooltips;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import fleethistory.U;
import java.util.ArrayList;
import fleethistory.shipevents.ShipBattleRecord;
import fleethistory.types.BattleRecordShipInfo;
import fleethistory.types.ShipBattleResult;
import fleethistory.types.ShipLog;
import fleethistory.types.ShipLogEntry;

public class ShipTooltip implements TooltipMakerAPI.TooltipCreator {

  private final BattleRecordShipInfo info;
  private final long battleTimestamp;

  public ShipTooltip(BattleRecordShipInfo info, long timestamp) {
    this.info = info;
    this.battleTimestamp = timestamp;
  }

  @Override
  public boolean isTooltipExpandable(Object tooltipParam) {
    return false;
  }

  @Override
  public float getTooltipWidth(Object tooltipParam) {
    ShipHullSpecAPI hull = info.getHullSpec();
    String hullName = U.isStation(hull) ? hull.getHullName() : hull.getNameWithDesignationWithDashClass();
    // 30 = max length of kill count string
    return Math.max(30, Math.max((info.getName() == null ? 0 : info.getName().length()), hullName.length())) * 8;
  }

  @Override
  public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
    ShipHullSpecAPI hull = info.getHullSpec();
    boolean isStation = U.isStation(hull);
    if (info.getName() != null) {
      tooltip.addPara(info.getName(), Misc.getHighlightColor(), 0);
    }
    tooltip.addPara(isStation ? hull.getHullName() : hull.getNameWithDesignationWithDashClass(), 0);

    ShipLog s = U.getShipLogFor(info.getId());
    if (s != null) {
      ShipLogEntry logEntry = null;
      for (ShipLogEntry e : s.events) {
        if (e.getTimestamp() == this.battleTimestamp && ShipLogEntry.EventType.COMBAT.equals(e.type)) {
          logEntry = e;
          break;
        }
      }
      if (logEntry != null) {
        ShipBattleRecord sbr = (ShipBattleRecord) logEntry.event;
        if (sbr.getFleetPoints() > 0) {
          
          String str = "";
          ArrayList<String> params = new ArrayList<>();

          int kills = sbr.getKills();
          int assists = sbr.getAssists();
          if(kills > 0 && assists > 0) {
            str = String.format(
                    "%s, %s %s",
                    U.i18n(kills == 1 ? "kill_count" : "kills_count"),
                    U.i18n(assists == 1 ? "assist_count" : "assists_count"),
                    U.i18n("fp_count")
            );
            params.add(kills + "");
            params.add(assists + "");
          } else if (kills > 0) {
            str = String.format(
                    "%s %s",
                    U.i18n(kills == 1 ? "kill_count" : "kills_count"),
                    U.i18n("fp_count")
            );
            params.add(kills + "");
          } else if (assists > 0) {
            str = String.format(
                    "%s %s",
                    U.i18n(assists == 1 ? "assist_count" : "assists_count"),
                    U.i18n("fp_count")
            );
            params.add(assists + "");
          }
          params.add(sbr.getFleetPoints() + "");

          tooltip.addPara(str, 0, Misc.getHighlightColor(), params.toArray(new String[params.size()]));
        }
      }
    }

    if (ShipBattleResult.isLost(info.status)) {
      String status = U.i18n(info.status.equals(ShipBattleResult.DESTROYED) ? "outcome_destroyed" : "outcome_disabled");
      tooltip.addPara(status, Misc.getNegativeHighlightColor(), 0);
    }

  }

}
