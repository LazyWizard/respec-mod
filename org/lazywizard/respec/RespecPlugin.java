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
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Level;

public class RespecPlugin extends BaseModPlugin implements EveryFrameScript
{
    private static final String RESPEC_ENABLED_FLAG = "lw_respec_enabled";
    private static final String RESPEC_ITEM_PREFIX = "respec_";
    private static final float RESPEC_ITEM_COST_PER_XP = .2f;
    private static final int MAX_LEVEL_SUPPORTED = 60;
    private static final Set<String> APTITUDE_IDS = new HashSet<String>(8);
    private static final Set<String> SKILL_IDS = new HashSet<String>(32);
    private static final float DAYS_BETWEEN_CHECKS = .5f;
    private long lastCheck = Long.MIN_VALUE;

    private int getLevel()
    {
        MutableCharacterStatsAPI player = Global.getSector().getPlayerFleet().getCommanderStats();
        return Math.max(1, Math.min(MAX_LEVEL_SUPPORTED, player.getLevel()));
    }

    private void respecPlayer()
    {
        int tmp;
        MutableCharacterStatsAPI player = Global.getSector().getPlayerFleet().getCommanderStats();

        Global.getLogger(RespecPlugin.class).log(Level.INFO,
                "Performing respec...");

        // Remove aptitudes
        for (String currId : APTITUDE_IDS)
        {
            tmp = Math.round(player.getAptitudeLevel(currId));
            if (tmp > 0)
            {
                Global.getLogger(RespecPlugin.class).log(Level.DEBUG,
                        "Removing " + tmp + " aptitude points from " + currId);
                player.setAptitudeLevel(currId, 0f);
                player.addAptitudePoints(tmp);
            }
        }

        // Remove skills
        for (String currId : SKILL_IDS)
        {
            tmp = Math.round(player.getSkillLevel(currId));
            if (tmp > 0)
            {
                Global.getLogger(RespecPlugin.class).log(Level.DEBUG,
                        "Removing " + tmp + " skill points from " + currId);
                player.setSkillLevel(currId, 0f);
                player.addSkillPoints(tmp);
            }
        }

        Global.getLogger(RespecPlugin.class).log(Level.INFO,
                "Respec complete.");
        Global.getSector().getCampaignUI().addMessage("Respec complete.", Color.GREEN);
    }

    private boolean checkPlayer()
    {
        CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
        String id;
        boolean shouldRespec = false;

        // Check for and remove all respec packages
        for (CargoStackAPI stack : cargo.getStacksCopy())
        {
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

    private void updateInventories(boolean addRespec)
    {
        Global.getLogger(RespecPlugin.class).log(Level.DEBUG,
                "Checking station inventories...");

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
                        /*Global.getLogger(RespecPlugin.class).log(Level.DEBUG,
                         "Removing " + stack.getSize() + " items from "
                         + station.getFullName() + ".");*/
                        station.getCargo().removeItems(stack.getType(),
                                stack.getData(), stack.getSize());
                    }
                }

                // Add the proper item for the player's level (no free respecs)
                if (addRespec && !station.getCargo().isFreeTransfer())
                {
                    /*Global.getLogger(RespecPlugin.class).log(Level.DEBUG,
                     "Adding item to " + station.getFullName() + ".");*/
                    station.getCargo().addItems(CargoItemType.RESOURCES, respecPackage, 1f);
                }
            }
        }

        Global.getLogger(RespecPlugin.class).log(Level.DEBUG,
                "Checked station inventories.");
    }

    private void doChecks()
    {
        checkPlayer();
        updateInventories(true);
        lastCheck = Global.getSector().getClock().getTimestamp();
    }

    @Override
    public void advance(float amount)
    {
        CampaignClockAPI clock = Global.getSector().getClock();
        if (clock.getElapsedDaysSince(lastCheck) >= DAYS_BETWEEN_CHECKS)
        {
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
        //Global.getLogger(RespecPlugin.class).setLevel(Level.INFO);

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

        /*String id;
         Global.getLogger(RespecPlugin.class).log(Level.DEBUG,
         "Loading aptitudes...");
         JSONArray aptitudeData = Global.getSettings()
         .getMergedSpreadhsheetDataForMod("id",
         "data/characters/skills/aptitude_data.csv", "lw_respec");
         for (int x = 0; x < aptitudeData.length(); x++)
         {
         id = aptitudeData.getJSONObject(x).getString("id");
         Global.getLogger(RespecPlugin.class).log(Level.DEBUG,
         "Found aptitude \"" + id + "\"");
         APTITUDE_IDS.add(id);
         }

         Global.getLogger(RespecPlugin.class).log(Level.DEBUG,
         "Aptitudes loaded.");
         Global.getLogger(RespecPlugin.class).log(Level.DEBUG,
         "Loading skills...");

         JSONArray skillData = Global.getSettings()
         .getMergedSpreadhsheetDataForMod("id",
         "data/characters/skills/skill_data.csv", "lw_respec");
         for (int x = 0; x < skillData.length(); x++)
         {
         id = skillData.getJSONObject(x).getString("id");
         Global.getLogger(RespecPlugin.class).log(Level.DEBUG,
         "Found skill \"" + id + "\"");
         SKILL_IDS.add(id);
         }

         Global.getLogger(RespecPlugin.class).log(Level.DEBUG,
         "Loaded skills.");*/
    }

    @Override
    public void onEnabled(boolean wasEnabledBefore)
    {
        Map data = Global.getSector().getPersistentData();
        if (!data.containsKey(RESPEC_ENABLED_FLAG))
        {
            data.put(RESPEC_ENABLED_FLAG, true);
            Global.getSector().addScript(this);
            doChecks();
        }
    }

    public static void main(String[] args)
    {
        data.scripts.plugins.LevelupPluginImpl tmp =
                new data.scripts.plugins.LevelupPluginImpl();
        String icon;
        float progress;

        // data/campaign/resources.csv
        System.out.println("name,id,cargo space,base value,stack size,icon,order");
        for (int x = 1; x <= MAX_LEVEL_SUPPORTED; x++)
        {
            progress = x / (float) MAX_LEVEL_SUPPORTED;

            // Inventory icon changes for higher-level respecs
            if (x == MAX_LEVEL_SUPPORTED)
            {
                icon = "graphics/icons/cargo/thinktank_black.png";
            }
            else if (progress >= .66f)
            {
                icon = "graphics/icons/cargo/thinktank_beige.png";
            }
            else if (progress >= .33f)
            {
                icon = "graphics/icons/cargo/thinktank_silver.png";
            }
            else
            {
                icon = "graphics/icons/cargo/thinktank_white.png";
            }

            System.out.println("\"ThinkTank (level " + x
                    + (x == MAX_LEVEL_SUPPORTED ? "+" : "") + ")\"," + RESPEC_ITEM_PREFIX
                    + x + ",0," + (long) (tmp.getXPForLevel(x) * RESPEC_ITEM_COST_PER_XP)
                    + ",1,\"" + icon + "\",9999");
        }

        // data/strings/descriptions.csv
        System.out.println("\n\n\nid,type,text1,text2,text3,notes");
        for (int x = 1; x <= MAX_LEVEL_SUPPORTED; x++)
        {
            System.out.println(RESPEC_ITEM_PREFIX + x
                    + ",RESOURCE,\"The Neuroventure ThinkTank can rearrange"
                    + " neural connections and allows for shifting character"
                    + " abilities from one specialization to another.\",,,");
        }
    }
}
