package fleethistory.types;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI.SkillLevelAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import fleethistory.StringCache;
import fleethistory.U;
import fleethistory.shipevents.ShipBattleRecord;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

public class OfficerLog {

  private static Logger log = Logger.getLogger(OfficerLog.class);

  private final String id;
  private String name;
  private final String sprite;
  private int level;
  private List<String> skills;

  private transient OfficerBattleStats officerBattleStats;
  private transient List<OfficerLogEntry> entries;

  public OfficerLog(PersonAPI p) {

    StringCache sc = U.getCache();

    this.id = sc.cacheString(p.getId());

    String n =  p.getNameString() + (p.isAICore() ? "@" + Integer.toHexString(p.hashCode()) : "");
    this.name = sc.cacheString(n);

    this.sprite = sc.cacheString(p.getPortraitSprite());

    MutableCharacterStatsAPI stats = p.getStats();

    this.level = stats.getLevel();
    log.info("Setting initial level for " + n + " to " + this.level);

    this.setSkills(stats);

  }

  public OfficerLog(String id, String name, String sprite, int level) {
    this.id = id;
    this.name = name;
    this.sprite = sprite;
    this.level = level;
    this.skills = new ArrayList<>();
  }

  public String getId() {
    return U.getCache().getCachedString(this.id);
  }

  public String getName() {
    return U.getCache().getCachedString(this.name);
  }

  public String getSprite() {
    return U.getCache().getCachedString(this.sprite);
  }

  public int getLevel() {
    return this.level;
  }

  public List<String> getSkills() {
    return this.skills;
  }

  public final void setSkills(MutableCharacterStatsAPI stats) {
    this.skills = new ArrayList<>();
    for (SkillLevelAPI slapi : stats.getSkillsCopy()) {
      SkillSpecAPI ssapi = slapi.getSkill();
      if (!ssapi.isAptitudeEffect() && ssapi.isCombatOfficerSkill() && slapi.getLevel() > 0) {
        String skillId = (slapi.getLevel() == 2 ? "ELITE_" : "") + ssapi.getId();
        log.info("Adding skill: " + skillId);
        this.skills.add(U.getCache().cacheString(skillId));
      }
    }
  }

  public List<OfficerLogEntry> getEntries() {
    if (this.entries == null) {
      this.entries = new ArrayList<>();
    }
    return this.entries;
  }

  public void update(PersonAPI p) {
    
    if(!p.isAICore()) {
      String newName = p.getNameString();
      if (newName != null && !newName.equals(this.getName())) {
        Logger.getLogger(this.getClass()).info(String.format("Officer name changed: %s -> %s", this.getName(), newName));
        this.name = U.getCache().cacheString(newName);
      }
    }

    MutableCharacterStatsAPI stats = p.getStats();
    int newLevel = (stats.getLevel() > this.level ? stats.getLevel() : 0);
    List<String> skillDiffs = getSkillDiffs(stats.getSkillsCopy());

    if (newLevel > 0) {
      Logger.getLogger(this.getClass()).info(String.format("%s level up: %d -> %d", this.getName(), this.level, newLevel));
      this.level = newLevel;
    }
    if (!skillDiffs.isEmpty()) {
      this.setSkills(stats);
    }

    this.updateOfficerBattleStats();

    if (newLevel > 0 || !skillDiffs.isEmpty()) {
      this.getEntries().add(
              new OfficerSkillEntry(
                      Global.getSector().getClock().getTimestamp(),
                      newLevel,
                      skillDiffs
              )
      );
    }

  }

  public List<String> getSkillDiffs(List<SkillLevelAPI> skills) {
    List<String> newSkills = new ArrayList<>();
    for (SkillLevelAPI slapi : skills) {
      SkillSpecAPI ssapi = slapi.getSkill();
      if (!ssapi.isAptitudeEffect() && ssapi.isCombatOfficerSkill() && slapi.getLevel() > 0) {
        String skillId = (slapi.getLevel() == 2 ? "ELITE_" : "") + ssapi.getId();
        if (!this.skills.contains(U.getCache().cacheString(skillId))) {
          Logger.getLogger(this.getClass()).info(String.format("%s new skill found: %s", this.getName(), skillId));
          newSkills.add(U.getCache().cacheString(skillId));
        }
      }
    }
    return newSkills;
  }

  public void addBattleEntry(String shipId, long timestamp, String battleRecordId) {
    Logger.getLogger(OfficerLog.class).info("Adding new captain log entry " + shipId + ", " + timestamp + ", " + battleRecordId);
    this.getEntries().add(new OfficerBattleEntry(shipId, timestamp, battleRecordId));
  }

  public String getCompressedString() {
    StringBuilder sb = new StringBuilder(String.format("%s|%s|%s|%d", this.id, this.name, this.sprite, this.level));
    for (String skill : skills) {
      // skill is already compressed, no need to cache string
      sb.append("|").append(skill);
    }
    return sb.toString();
  }

  public String getCompressedEntries() {
    StringBuilder sb = new StringBuilder();
    for (OfficerLogEntry e : this.getEntries()) {
      if (sb.length() > 0) {
        sb.append(U.DELIMITER);
      }
      sb.append(e.getCompressedString());
    }
    return sb.toString();
  }

  public OfficerBattleStats getStats() {
    if (this.officerBattleStats == null) {
      this.updateOfficerBattleStats();
    }
    return this.officerBattleStats;
  }

  public void updateOfficerBattleStats() {

    this.officerBattleStats = new OfficerBattleStats();
    ArrayList<String> shipsCommanded = new ArrayList<>();

    for (OfficerLogEntry ole : this.getEntries()) {

      if (ole instanceof OfficerBattleEntry) {

        OfficerBattleEntry obe = (OfficerBattleEntry) ole;
        if (this.officerBattleStats.firstTimestamp == 0) {
          this.officerBattleStats.firstTimestamp = obe.getTimestamp();
        }
        this.officerBattleStats.lastTimestamp = obe.getTimestamp();

        String shipId = obe.getShipId();
        if (!shipsCommanded.contains(shipId)) {
          this.officerBattleStats.ships++;
          shipsCommanded.add(shipId);
        }

        ShipBattleRecord sbr = obe.getBattleRecord();
        this.officerBattleStats.battles++;
        this.officerBattleStats.timesKilled += (sbr.recovered ? 1 : 0);
        this.officerBattleStats.kills += sbr.getKills();
        this.officerBattleStats.assists += sbr.getAssists();
        this.officerBattleStats.fleetPoints += sbr.getFleetPoints();

      }

    }

  }

  public String getCurrentShipAssignment() {
    for (FleetMemberAPI f : Global.getSector().getPlayerFleet().getMembersWithFightersCopy()) {
      PersonAPI captain = f.getCaptain();
      if (!captain.isDefault() && this.getId().equals(captain.getId())) {
        return f.getShipName() + ", " + f.getHullSpec().getNameWithDesignationWithDashClass();
      }
    }
    return null;
  }

  public class OfficerBattleStats {

    public long firstTimestamp;
    public long lastTimestamp;
    public int battles;
    public int ships;
    public int kills;
    public int assists;
    public int fleetPoints;
    public int timesKilled;
    
  }

}
