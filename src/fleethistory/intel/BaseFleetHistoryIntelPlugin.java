package fleethistory.intel;

import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import fleethistory.U;
import java.util.Set;
import java.util.Stack;

public class BaseFleetHistoryIntelPlugin extends BaseIntelPlugin {
  
  static Stack<BaseFleetHistoryIntelPlugin> browseHistory = new Stack<>();
  static BaseFleetHistoryIntelPlugin referer = null;
  static BaseFleetHistoryIntelPlugin intelToForceDisplay = null;
  
  @Override
  public void createLargeDescription(CustomPanelAPI cp, float width, float height) {
    
    if(!browseHistory.isEmpty() && referer != null) {
      
        TooltipMakerAPI t = cp.createUIElement(35, 25, false);
        t.addAreaCheckbox(
                "<<", 
                "<<",
                Misc.getBrightPlayerColor(),
                Misc.getDarkPlayerColor(),
                Misc.getHighlightColor(),
                35,
                25,
                0
        );
        cp.addUIElement(t).inTL(-14, 0);
    }
    
    if(referer == null) {
      browseHistory.clear();
    }
    referer = null;
    intelToForceDisplay = null;
    
  }
  
  public void navigateTo(IntelUIAPI ui, BaseFleetHistoryIntelPlugin p) {
    navigateTo(ui, p, false);
  }

  public void navigateTo(IntelUIAPI ui, BaseFleetHistoryIntelPlugin p, boolean isBack) {
    
    if(p instanceof ShipLogIntel) {
      U.getPersistentData().put(U.FLEET_HISTORY_VIEW_MODE, U.FLEET_HISTORY_VIEW_SHIPS);
    } else if(p instanceof BattleRecordIntel) {
      U.getPersistentData().put(U.FLEET_HISTORY_VIEW_MODE, U.FLEET_HISTORY_VIEW_BATTLES);
    } else if(p instanceof OfficerLogIntel) {
      U.getPersistentData().put(U.FLEET_HISTORY_VIEW_MODE, U.FLEET_HISTORY_VIEW_OFFICERS);
    }
    if(!isBack) {
      BaseFleetHistoryIntelPlugin.browseHistory.push(this);
    }
    BaseFleetHistoryIntelPlugin.referer = this;
    BaseFleetHistoryIntelPlugin.intelToForceDisplay = p;
    ui.recreateIntelUI();
    ui.selectItem(p);
  }
  
  @Override
  public void advance(float amount) {
    super.advance(amount);
  }  

  @Override
  public boolean isNew() {
    return false;
  }

  @Override
  public boolean hasLargeDescription() {
    return true;
  }

  @Override
  public boolean autoAddCampaignMessage() {
    return false;
  }

  @Override
  public boolean hasSmallDescription() {
    return false;
  }

  @Override
  public boolean hasImportantButton() {
    return false;
  }

  @Override
  public boolean shouldRemoveIntel() {
    return false;
  }
  
  @Override
  public IntelSortTier getSortTier() {
    return IntelSortTier.TIER_3;
  }  

  @Override
  public Set<String> getIntelTags(SectorMapAPI map) {
    Set<String> tags = super.getIntelTags(map);
    tags.add(U.i18n("intel_category_tag"));
    return tags;
  }

}
