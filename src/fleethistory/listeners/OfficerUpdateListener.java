package fleethistory.listeners;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import fleethistory.U;

public class OfficerUpdateListener implements EveryFrameScript {

  private float elapsedTime = 0;
  
  // update all active officers' stats once per in-game day
  @Override
  public void advance(float amount) {
    elapsedTime += amount;
    if (Global.getSector().getClock().convertToDays(elapsedTime) > 1) {
      for (FleetMemberAPI f : Global.getSector().getPlayerFleet().getMembersWithFightersCopy()) {
        PersonAPI captain = f.getCaptain();
        if (!captain.isDefault()) {
          U.getOfficerLogFor(captain).update(captain);
        }
      }
      elapsedTime = 0;
    }
  }

  @Override
  public boolean isDone() {
    return false;
  }

  @Override
  public boolean runWhilePaused() {
    return false;
  }

}
