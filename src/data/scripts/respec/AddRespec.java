package data.scripts.respec;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorGeneratorPlugin;
import com.fs.starfarer.api.campaign.StarSystemAPI;

public class AddRespec implements SectorGeneratorPlugin
{
    @Override
    public void generate(SectorAPI sector)
    {
        StarSystemAPI system = (StarSystemAPI) Global.getSector().getStarSystems().get(0);
        system.addSpawnPoint(new RespecPlugin());
    }
}
