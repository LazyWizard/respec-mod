package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import data.scripts.respec.RespecPlugin;
import java.util.List;

public class CheckAddRespec implements EveryFrameCombatPlugin
{
    @Override
    public void advance(float amount, List events)
    {
    }

    @Override
    public void init(CombatEngineAPI engine)
    {
        if (engine.isInCampaign())
        {
            List systems = Global.getSector().getStarSystems();
            if (systems.isEmpty())
            {
                throw new RuntimeException("No starsystems found!");
            }

            StarSystemAPI system = (StarSystemAPI) systems.get(0);
            if (RespecPlugin.getLastSystem() != system)
            {
                system.addSpawnPoint(new RespecPlugin(system));
            }
        }
    }
}