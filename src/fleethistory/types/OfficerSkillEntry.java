package fleethistory.types;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import fleethistory.U;
import java.util.ArrayList;
import java.util.List;

public class OfficerSkillEntry extends OfficerLogEntry {

  private final String level;
  private final List<String> skills;

  public OfficerSkillEntry(long timestamp, int level, List<String> skills) {
    this.timestamp = U.encodeNum(timestamp);
    this.level = U.encodeNum(level);
    this.skills = new ArrayList<>();
    this.skills.addAll(skills);
  }

  public OfficerSkillEntry(String compressedString) {
    String[] str = compressedString.split("\\|", -1);
    // str[0] is entry type identifier "S|"
    this.timestamp = str[1];
    this.level = str[2];
    this.skills = new ArrayList<>();
    for (int i = 3; i < str.length; i++) {
      this.skills.add(str[i]);
    }
  }

  @Override
  public String getCompressedString() {
    StringBuilder sb = new StringBuilder("S|");
    sb.append(this.timestamp).append("|").append(this.level);
    for (String s : this.skills) {
      sb.append("|").append(s);
    }
    return sb.toString();
  }

  public int getLevel() {
    return (int) U.decodeNum(this.level);
  }

  public List<String> getSkills() {
    return this.skills;
  }

  @Override
  public void render(CustomPanelAPI panel, float width, float height) {

    float iconSize = 42f;
    float iconPadding = 8f;
    float calculatedHeight = 0f;
    TooltipMakerAPI info = panel.createUIElement(width * 0.75f, 0, false);
    List<String> skillNameArray = new ArrayList<>();

    if (this.getLevel() > 0) {
      info.addPara(U.i18n("officer_level"), 0, Misc.getHighlightColor(), this.getLevel() + "");
    }
    if (!this.getSkills().isEmpty()) {
      StringBuilder skillFmtString = new StringBuilder();
      for (String str : this.getSkills()) {
        String skillId = U.getCache().getCachedString(str);
        boolean isElite = skillId.startsWith("ELITE_");
        SkillSpecAPI skill = Global.getSettings().getSkillSpec(skillId.replace("ELITE_", ""));
        if (skill != null) {
          skillFmtString.append(skillFmtString.length() > 0 ? ", " : "").append("%s");
          skillNameArray.add(skill.getName() + (isElite ? U.i18n("elite_skill_suffix") : ""));
        }
      }

      if (!skillNameArray.isEmpty()) {
        if (this.getLevel() > 0) {
          info.addSpacer(U.LINE_SPACING);
        }
        skillNameArray.add(0, skillNameArray.size() + "");
        info.addPara(
                U.i18n(skillNameArray.size() == 2 ? "officer_skill_gained" : "officer_skills_gained") + ": " + skillFmtString.toString(),
                0,
                Misc.getHighlightColor(),
                skillNameArray.toArray(new String[skillNameArray.size()])
        );
      }
    }
    panel.addUIElement(info).inTL(0, U.LINE_SPACING);
    calculatedHeight += info.getPosition().getHeight();

    if (!skillNameArray.isEmpty()) {
      
      CustomPanelAPI iconRow = panel.createCustomPanel(width, iconSize, null);
      
      for (int i = 0; i < this.getSkills().size(); i++) {
        
        String skillId = U.getCache().getCachedString(this.getSkills().get(i));
        boolean isElite = skillId.startsWith("ELITE_");
        SkillSpecAPI spec = Global.getSettings().getSkillSpec(skillId.replace("ELITE_", ""));

        CustomPanelAPI iconPanel = iconRow.createCustomPanel(iconSize, iconSize, null);
        TooltipMakerAPI skillImg = iconPanel.createUIElement(iconSize, iconSize, false);
        skillImg.addImage(spec.getSpriteName(), iconSize, iconSize, 0);
        if(i == this.getSkills().size() - 1) {
          skillImg.addSpacer(25);
        }
        iconPanel.addUIElement(skillImg).inTL(0, 0);

        if (isElite) {
          TooltipMakerAPI eliteImg = iconPanel.createUIElement(16, 16, false);
          eliteImg.addImage("graphics/icons/insignia/16x_star_circle.png", 16, 16, 0);
          iconPanel.addUIElement(eliteImg).inTL(2, 2);
        }
        
        iconRow.addComponent(iconPanel).inTL(0, 0).setXAlignOffset(i * (iconSize + iconPadding));
        
      }
      
      panel.addComponent(iconRow).belowLeft(info, U.LINE_SPACING);
      calculatedHeight += iconRow.getPosition().getHeight();
      
    }


    panel.getPosition().setSize(width, calculatedHeight + 20);

  }

}
