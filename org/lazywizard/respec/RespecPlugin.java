package org.lazywizard.respec;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import java.awt.Color;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.apache.log4j.Priority;

public class RespecPlugin extends BaseModPlugin implements EveryFrameScript
{
    private static final String RESPEC_ITEM_PREFIX = "respec_";
    private static final float RESPEC_ITEM_COST_PER_XP = .2f;
    private static final int MAX_LEVEL_SUPPORTED = 60;
    private static final Set<String> APTITUDE_IDS = new HashSet<String>();
    private static final Set<String> SKILL_IDS = new HashSet<String>();
    private static final float DAYS_BETWEEN_CHECKS = .5f;
    private long lastCheck = Long.MIN_VALUE;

    public RespecPlugin()
    {
        lastCheck = Global.getSector().getClock().getTimestamp();
    }

    private int getLevel()
    {
        MutableCharacterStatsAPI player = Global.getSector().getPlayerFleet().getCommanderStats();
        float totalSkill = player.getSkillPoints() - 4; // Compensate for level 1 bonus

        for (Iterator<String> skills = SKILL_IDS.iterator(); skills.hasNext();)
        {
            totalSkill += player.getSkillLevel(skills.next());
        }

        return (int) Math.max(1f, Math.min(MAX_LEVEL_SUPPORTED, totalSkill / 2));
    }

    private void respecPlayer()
    {
        int tmp;
        MutableCharacterStatsAPI player = Global.getSector().getPlayerFleet().getCommanderStats();

        // Remove aptitudes
        for (String currId : APTITUDE_IDS)
        {
            tmp = Math.round(player.getAptitudeLevel(currId));
            player.setAptitudeLevel(currId, 0f);
            player.addAptitudePoints(tmp);
        }

        // Remove skills
        for (String currId : SKILL_IDS)
        {
            tmp = Math.round(player.getSkillLevel(currId));
            player.setSkillLevel(currId, 0f);
            player.addSkillPoints(tmp);
        }

        Global.getSector().getCampaignUI().addMessage("Respec complete.", Color.GREEN);
    }

    private boolean checkPlayer()
    {
        CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
        CargoStackAPI stack;
        String id;
        boolean shouldRespec = false;

        // Check for and remove all respec packages
        for (Iterator iter = cargo.getStacksCopy().iterator(); iter.hasNext();)
        {
            stack = (CargoStackAPI) iter.next();
            if (stack.isNull())
            {
                continue;
            }

            id = (String) stack.getData();
            if (id.startsWith(RESPEC_ITEM_PREFIX))
            {
                shouldRespec = true;
                cargo.removeItems(stack.getType(), stack.getData(), stack.getSize());
            }
        }

        // A respec package was found
        if (shouldRespec)
        {
            respecPlayer();
            return true;
        }

        return false;
    }

    private void updateInventories(boolean removeOnly)
    {
        String id, respecPackage = RESPEC_ITEM_PREFIX + getLevel();

        for (StarSystemAPI system : Global.getSector().getStarSystems())
        {
            for (SectorEntityToken station : system.getOrbitalStations())
            {
                // Remove all existing respec items
                for (CargoStackAPI stack : station.getCargo().getStacksCopy())
                {
                    if (stack.isNull())
                    {
                        continue;
                    }

                    id = (String) stack.getData();
                    if (id.startsWith(RESPEC_ITEM_PREFIX))
                    {
                        station.getCargo().removeItems(stack.getType(),
                                stack.getData(), stack.getSize());
                    }
                }

                // Add the proper item for the player's level (no free respecs)
                if (!removeOnly && !station.getCargo().isFreeTransfer())
                {
                    station.getCargo().addItems(CargoItemType.RESOURCES, respecPackage, 1f);
                }
            }
        }
    }

    private void doChecks()
    {
        checkPlayer();
        updateInventories(false);
    }

    @Override
    public void advance(float amount)
    {
        CampaignClockAPI clock = Global.getSector().getClock();
        if (clock.getElapsedDaysSince(lastCheck) >= DAYS_BETWEEN_CHECKS)
        {
            lastCheck = clock.getTimestamp();
            doChecks();
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
        return false;
    }

    @Override
    public void onApplicationLoad() throws Exception
    {
        Global.getLogger(RespecPlugin.class).log(Priority.DEBUG,
                "Loading aptitudes...");

        // Aptitudes
        APTITUDE_IDS.add("combat");
        APTITUDE_IDS.add("leadership");
        APTITUDE_IDS.add("technology");
        APTITUDE_IDS.add("industry");

        Global.getLogger(RespecPlugin.class).log(Priority.DEBUG,
                "Aptitudes loaded.");
        Global.getLogger(RespecPlugin.class).log(Priority.DEBUG,
                "Loading skills...");

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

        Global.getLogger(RespecPlugin.class).log(Priority.DEBUG,
                "Loaded skills.");
    }

    @Override
    public void onGameLoad()
    {
        updateInventories(false);
    }

    @Override
    public void beforeGameSave()
    {
        updateInventories(true);
    }

    @Override
    public void afterGameSave()
    {
        updateInventories(false);
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
}
