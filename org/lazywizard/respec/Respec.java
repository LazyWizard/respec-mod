package org.lazywizard.respec;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import java.awt.Color;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.apache.log4j.Level;
import org.json.JSONArray;
import org.json.JSONException;

class Respec
{
    private static final String RESPEC_ITEM_PREFIX = "respec_";
    private static final float RESPEC_ITEM_COST_PER_XP = .2f;
    private static final int MAX_LEVEL_SUPPORTED = 60;
    private static final Set<String> APTITUDE_IDS = new HashSet<String>();
    private static final Set<String> SKILL_IDS = new HashSet<String>();

    static void loadCSVData() throws JSONException, IOException
    {
        int numAptitudes = 0, numSkills = 0;

        Global.getLogger(Respec.class).log(Level.DEBUG,
                "Loading aptitudes...");
        JSONArray aptitudeData = Global.getSettings()
                .loadCSV("data/characters/skills/aptitude_data.csv");
        for (int x = 0; x < aptitudeData.length(); x++)
        {
            String id = aptitudeData.getJSONObject(x).getString("id");
            if (id.isEmpty())
            {
                Global.getLogger(Respec.class).log(Level.DEBUG,
                        "Ignoring empty CSV row");
            }
            else
            {
                Global.getLogger(Respec.class).log(Level.DEBUG,
                        "Found aptitude \"" + id + "\"");
                APTITUDE_IDS.add(id);
                numAptitudes++;
            }
        }

        Global.getLogger(Respec.class).log(Level.DEBUG,
                "Loading skills...");
        JSONArray skillData = Global.getSettings()
                .loadCSV("data/characters/skills/skill_data.csv");
        for (int x = 0; x < skillData.length(); x++)
        {
            String id = skillData.getJSONObject(x).getString("id");
            if (id.isEmpty())
            {
                Global.getLogger(Respec.class).log(Level.DEBUG,
                        "Ignoring empty CSV row");
            }
            else
            {
                Global.getLogger(Respec.class).log(Level.DEBUG,
                        "Found skill \"" + id + "\"");
                SKILL_IDS.add(id);
                numSkills++;
            }
        }

        Global.getLogger(Respec.class).log(Level.INFO,
                "Loaded " + numAptitudes + " aptitudes and " + numSkills + " skills.");
    }

    private static int getLevel()
    {
        MutableCharacterStatsAPI player = Global.getSector().getPlayerFleet().getCommanderStats();
        return Math.max(1, Math.min(MAX_LEVEL_SUPPORTED, player.getLevel()));
    }

    private static void respecPlayer()
    {
        int tmp;
        MutableCharacterStatsAPI player = Global.getSector().getPlayerFleet().getCommanderStats();

        Global.getLogger(Respec.class).log(Level.INFO,
                "Performing respec...");

        // Remove aptitudes
        for (String currId : APTITUDE_IDS)
        {
            tmp = Math.round(player.getAptitudeLevel(currId));
            if (tmp > 0)
            {
                Global.getLogger(Respec.class).log(Level.DEBUG,
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
                Global.getLogger(Respec.class).log(Level.DEBUG,
                        "Removing " + tmp + " skill points from " + currId);
                player.setSkillLevel(currId, 0f);
                player.addSkillPoints(tmp);
            }
        }

        Global.getLogger(Respec.class).log(Level.INFO,
                "Respec complete.");
        Global.getSector().getCampaignUI().addMessage("Respec complete.", Color.GREEN);
    }

    static boolean checkPlayer()
    {
        CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
        String id;
        boolean shouldRespec = false;

        // Check for and remove all respec packages
        for (CargoStackAPI stack : cargo.getStacksCopy())
        {
            if (stack.isNull() || !stack.isResourceStack())
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

    static void updateInventories(boolean addRespec)
    {
        Global.getLogger(Respec.class).log(Level.DEBUG,
                "Checking station inventories...");

        String id, respecPackage = RESPEC_ITEM_PREFIX + getLevel();
        for (StarSystemAPI system : Global.getSector().getStarSystems())
        {
            for (SectorEntityToken station : system.getOrbitalStations())
            {
                // Remove all existing respec items
                CargoAPI cargo = station.getCargo();
                for (CargoStackAPI stack : cargo.getStacksCopy())
                {
                    if (stack.isNull() || !stack.isResourceStack())
                    {
                        continue;
                    }

                    id = (String) stack.getData();
                    if (id.startsWith(RESPEC_ITEM_PREFIX))
                    {
                        Global.getLogger(Respec.class).log(Level.DEBUG,
                                "Removing " + stack.getSize() + " items from "
                                + station.getFullName() + ".");
                        cargo.removeItems(stack.getType(),
                                stack.getData(), stack.getSize());
                    }
                }

                // Add the proper item for the player's level (no free respecs)
                if (addRespec && !station.isFreeTransfer()
                        && !station.getFaction().isNeutralFaction())
                {
                    Global.getLogger(Respec.class).log(Level.DEBUG,
                            "Adding item to " + station.getFullName() + ".");
                    cargo.addItems(CargoItemType.RESOURCES, respecPackage, 1f);
                }
            }
        }

        Global.getLogger(Respec.class).log(Level.DEBUG,
                "Checked station inventories.");
    }

    public static void main(String[] args)
    {
        data.scripts.plugins.LevelupPluginImpl tmp
                = new data.scripts.plugins.LevelupPluginImpl();
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

    private Respec()
    {
    }
}
