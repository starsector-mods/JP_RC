package data.scripts.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import java.util.List;

/**
 * Handles periodic checking and respawning of Spinerette boss entities.
 * Once a Spinerette is defeated, the interaction code marks it with a defeated flag.
 * After a set duration, this script clears the defeat flag and restores the active hazard condition.
 */
public class SpineretteRespawnManager implements EveryFrameScript {

    private IntervalUtil tracker; // Check every 5 to 10 game days

    public IntervalUtil getTracker() {
        if (tracker == null) {
            tracker = new IntervalUtil(5f, 10f);
        }
        return tracker;
    }

    protected Object readResolve() {
        getTracker();
        return this;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
        if (Global.getSector() == null || Global.getSector().getClock() == null) return;
        float days = Global.getSector().getClock().convertToDays(amount);
        getTracker().advance(days);
        if (!getTracker().intervalElapsed()) {
            return;
        }

        // Loop through all star systems to find spinerette entities
        List<StarSystemAPI> systems = Global.getSector().getStarSystems();
        if (systems == null) return;
        for (StarSystemAPI system : systems) {
            if (system == null) continue;
            List<SectorEntityToken> entities = system.getEntitiesWithTag("junk_pirates_spinerette_active");
            if (entities == null) continue;
            for (SectorEntityToken entity : entities) {
                if (entity == null) continue;
                MemoryAPI mem = entity.getMemoryWithoutUpdate();
                if (mem == null) continue;
                if (mem.getBoolean("$defenderFleetDefeated")) {
                    mem.set("$spineretteWasDefeated", true);
                } else if (mem.getBoolean("$spineretteWasDefeated")) {
                    // 60 days have passed and $defenderFleetDefeated has expired
                    mem.unset("$spineretteWasDefeated");
                    mem.unset("$hasDefenders");
                    mem.unset("$defenderFleet");
                    mem.unset("$salvaged");

                    // Re-apply hazard tubes condition to the market if one exists
                    if (entity.getOrbit() != null && entity.getOrbit().getFocus() != null) {
                        MarketAPI focusMarket = entity.getOrbit().getFocus().getMarket();
                        if (focusMarket != null && focusMarket.hasCondition("JUNK_habTubes")) {
                            focusMarket.removeCondition("JUNK_habTubes");
                            focusMarket.addCondition("JUNK_habTubes_active");
                            focusMarket.reapplyConditions();
                        }
                    }

                    // Display a system message warning the player that automated structures have rebooted
                    if (Global.getSector().getCampaignUI() != null) {
                        Global.getSector().getCampaignUI().addMessage(
                            "Sensors indicate that the Automata Cloud surrounding the Spinerette structure in " + system.getNameWithLowercaseType() + " has completed its repair cycle.",
                            Global.getSettings().getColor("yellowTextColor")
                        );
                    }
                }
            }
        }
    }
}
