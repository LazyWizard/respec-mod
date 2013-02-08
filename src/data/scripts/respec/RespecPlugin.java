package data.scripts.respec;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.SpawnPointPlugin;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import java.awt.Color;
import java.util.*;

public class RespecPlugin implements SpawnPointPlugin
{
    private static final String RESPEC_ITEM_PREFIX = "respec_";
    private static final float RESPEC_ITEM_COST_PER_XP = .2f;
    private static final int MAX_LEVEL_SUPPORTED = 60;
    private static final Set APTITUDE_IDS = new HashSet(), SKILL_IDS = new HashSet();
    private static final float DAYS_BETWEEN_CHECKS = .5f;
    private static StarSystemAPI lastSystem = null;
    private StarSystemAPI system;
    private long lastCheck = Long.MIN_VALUE;

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
        SKILL_IDS.add("coordinated_maneuvers");
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

        // Industry skills
    }

    public RespecPlugin(StarSystemAPI system)
    {
        RespecPlugin.lastSystem = this.system = system;
        lastCheck = Global.getSector().getClock().getTimestamp();
    }

    public Object readResolve()
    {
        RespecPlugin.lastSystem = this.system;
        return this;
    }

    public static StarSystemAPI getLastSystem()
    {
        return lastSystem;
    }

    private int getLevel()
    {
        MutableCharacterStatsAPI player = Global.getSector().getPlayerFleet().getCommanderStats();
        float totalSkill = player.getSkillPoints() - 4; // Compensate for level 1 bonus

        for (Iterator skills = SKILL_IDS.iterator(); skills.hasNext();)
        {
            totalSkill += player.getSkillLevel((String) skills.next());
        }

        return (int) Math.max(1f, Math.min(MAX_LEVEL_SUPPORTED, totalSkill / 2));
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

    private boolean checkPlayer()
    {
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        CargoStackAPI stack;
        String id;
        boolean shouldRespec = false;

        // Check for and remove all respec packages
        for (Iterator iter = player.getCargo().getStacksCopy().iterator(); iter.hasNext();)
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
                stack.getCargo().removeItems(stack.getType(), stack.getData(), stack.getSize());
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

    private void checkStationInventories()
    {
        StarSystemAPI system = (StarSystemAPI) Global.getSector().getStarSystems().get(0);
        SectorEntityToken station;
        CargoStackAPI stack;
        String id, respecPackage = RESPEC_ITEM_PREFIX + getLevel();

        for (Iterator stations = system.getOrbitalStations().iterator(); stations.hasNext();)
        {
            station = (SectorEntityToken) stations.next();

            // NO FREE RESPECS!
            if (station.getCargo().isFreeTransfer())
            {
                continue;
            }

            // Remove all existing respec items
            for (Iterator cargo = station.getCargo().getStacksCopy().iterator(); cargo.hasNext();)
            {
                stack = (CargoStackAPI) cargo.next();
                if (stack.isNull())
                {
                    continue;
                }

                id = (String) stack.getData();
                if (id.startsWith(RESPEC_ITEM_PREFIX))
                {
                    stack.getCargo().removeItems(stack.getType(), stack.getData(), stack.getSize());
                }
            }

            // Add the proper item for the player's level
            station.getCargo().addItems(CargoItemType.RESOURCES, respecPackage, 1f);
        }
    }

    private void doChecks()
    {
        checkPlayer();
        checkStationInventories();
    }

    @Override
    public void advance(SectorAPI sector, LocationAPI location)
    {
        if (sector.getClock().getElapsedDaysSince(lastCheck) >= DAYS_BETWEEN_CHECKS)
        {
            lastCheck = sector.getClock().getTimestamp();
            doChecks();
        }
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
                    + " neuronal connections and thus allows to shift character"
                    + " abilities from one specialization to another.\",,,");
        }
    }
}
