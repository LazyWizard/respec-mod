package data.scripts.respec;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import java.awt.Color;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class RespecPlugin extends BaseModPlugin implements EveryFrameScript
{
    private static final String RESPEC_ITEM_PREFIX = "respec_";
    private static final float RESPEC_ITEM_COST_PER_XP = .2f;
    private static final int MAX_LEVEL_SUPPORTED = 60;
    private static final Set APTITUDE_IDS = new HashSet(), SKILL_IDS = new HashSet();

    static
    {
        // Aptitudes
        APTITUDE_IDS.add("combat");
        APTITUDE_IDS.add("leadership");
        APTITUDE_IDS.add("technology");
        APTITUDE_IDS.add("industry");

        // Combat skills
        SKILL_IDS.add("missile_specialization");
        SKILL_IDS.add("ordnance_expert");
        SKILL_IDS.add("damage_control");
        SKILL_IDS.add("target_analysis");
        SKILL_IDS.add("evasive_action");
        SKILL_IDS.add("helmsmanship");
        SKILL_IDS.add("flux_modulation");

        // Leadership skills
        SKILL_IDS.add("advanced_tactics");
        SKILL_IDS.add("command_experience");
        SKILL_IDS.add("fleet_logistics");

        // Technology skills
        SKILL_IDS.add("gunnery_implants");
        SKILL_IDS.add("applied_physics");
        SKILL_IDS.add("flux_dynamics");
        SKILL_IDS.add("computer_systems");
        SKILL_IDS.add("construction");
        SKILL_IDS.add("mechanical_engineering");
        SKILL_IDS.add("field_repairs");
        SKILL_IDS.add("navigation");
    }

    private float getRespecCost(MutableCharacterStatsAPI character)
    {
        int level = (int) Math.max(1f,
                Math.min(MAX_LEVEL_SUPPORTED, character.getLevel()));
        data.scripts.plugins.LevelupPluginImpl tmp =
                new data.scripts.plugins.LevelupPluginImpl();
        return tmp.getXPForLevel(level);
    }

    private void respecPlayer()
    {
        int tmp;
        String currId;
        MutableCharacterStatsAPI player = Global.getSector().getPlayerFleet().getCommanderStats();

        // Remove aptitudes
        for (Iterator aptitudes = APTITUDE_IDS.iterator(); aptitudes.hasNext();)
        {
            currId = (String) aptitudes.next();
            tmp = Math.round(player.getAptitudeLevel(currId));
            player.setAptitudeLevel(currId, 0f);
            player.addAptitudePoints(tmp);
        }

        // Remove skills
        for (Iterator skills = SKILL_IDS.iterator(); skills.hasNext();)
        {
            currId = (String) skills.next();
            tmp = Math.round(player.getSkillLevel(currId));
            player.setSkillLevel(currId, 0f);
            player.addSkillPoints(tmp);
        }

        Global.getSector().addMessage("Respec complete.", Color.GREEN);
    }

    @Override
    public void onApplicationLoad() throws Exception
    {
        // TODO: Dynamically generate aptitude/skill list
    }

    @Override
    public void onGameLoad()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public static void main(String[] args)
    {
        data.scripts.plugins.LevelupPluginImpl tmp =
                new data.scripts.plugins.LevelupPluginImpl();

        // resources.csv
        System.out.println("name,id,cargo space,base value,stack size,icon,order");
        for (int x = 1; x <= MAX_LEVEL_SUPPORTED; x++)
        {
            System.out.println("\"ThinkTank (level " + x
                    + (x == MAX_LEVEL_SUPPORTED ? "+" : "") + ")\"," + RESPEC_ITEM_PREFIX
                    + x + ",0," + (long) (tmp.getXPForLevel(x) * RESPEC_ITEM_COST_PER_XP)
                    + ",1,\"graphics/icons/cargo/thinktank_white.png\",9999");
        }

        // descriptions.csv
        System.out.println("\n\n\nid,type,text1,text2,text3,notes");
        for (int x = 1; x <= MAX_LEVEL_SUPPORTED; x++)
        {
            System.out.println(RESPEC_ITEM_PREFIX + x
                    + ",RESOURCE,\"The Neuroventure ThinkTank will rearrange"
                    + " neural connections and allows for shifting character"
                    + " abilities from one specialization to another.\",,,");
        }
    }

    @Override
    public boolean isDone()
    {
        return false;
    }

    @Override
    public boolean runWhilePaused()
    {
        return true;
    }

    @Override
    public void advance(float amount)
    {
        // TODO: write key handler
    }
}
