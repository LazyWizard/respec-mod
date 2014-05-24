package org.lazywizard.respec;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;

public class RespecScript implements EveryFrameScript
{
    private int lastCheck = 25;

    private void doChecks()
    {
        Respec.checkPlayer();
        Respec.updateInventories(true);
        lastCheck = Global.getSector().getClock().getHour();
    }

    @Override
    public void advance(float amount)
    {
        if (Global.getSector().getClock().getHour() != lastCheck)
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
}
