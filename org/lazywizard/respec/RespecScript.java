package org.lazywizard.respec;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;

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
        CampaignClockAPI clock = Global.getSector().getClock();
        if (clock.getHour() != lastCheck)
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
