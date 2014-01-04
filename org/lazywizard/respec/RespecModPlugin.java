package org.lazywizard.respec;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import org.apache.log4j.Level;

public class RespecModPlugin extends BaseModPlugin
{
    @Override
    public void onApplicationLoad() throws Exception
    {
        Global.getLogger(Respec.class).setLevel(Level.INFO);
        Respec.loadCSVData();
    }

    @Override
    public void onGameLoad()
    {
        Global.getSector().addScript(new RespecScript());
    }

    @Override
    public void beforeGameSave()
    {
        Respec.checkPlayer();
        Respec.updateInventories(false);
        Global.getSector().removeScriptsOfClass(RespecScript.class);
    }

    @Override
    public void afterGameSave()
    {
        Global.getSector().addScript(new RespecScript());
    }
}
