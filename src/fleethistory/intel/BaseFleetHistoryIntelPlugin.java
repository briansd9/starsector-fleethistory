/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fleethistory.intel;

import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import fleethistory.U;
import java.util.Set;

/**
 *
 * @author joshi
 */
public class BaseFleetHistoryIntelPlugin extends BaseIntelPlugin {

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
