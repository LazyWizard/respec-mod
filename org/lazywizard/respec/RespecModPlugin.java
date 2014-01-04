package org.lazywizard.respec;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import org.apache.log4j.Level;

public class RespecModPlugin extends BaseModPlugin
{
    @Override
    public void onApplicationLoad() throws Exception
    {
        //Global.getLogger(RespecScript.class).setLevel(Level.INFO);
        RespecScript.reloadData();
    }

    @Override
    public void onEnabled(boolean wasEnabledBefore)
    {
        if (!wasEnabledBefore)
        {
            Global.getSector().addScript(new RespecScript());
        }
    }

    @Override
    public void onGameLoad()
    {
        Global.getSector().addScript(new RespecScript());
    }

    @Override
    public void beforeGameSave()
    {
        Global.getSector().removeScriptsOfClass(RespecScript.class);
    }

    @Override
    public void afterGameSave()
    {
        Global.getSector().addScript(new RespecScript());
    }
}
