package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;

public class JP_IndEvoIntegration {

    public static boolean isIndEvoEnabled() {
        return Global.getSettings().getModManager().isModEnabled("IndEvo");
    }

    public static void addAcademy(SectorEntityToken entity) {
        if (isIndEvoEnabled()) {
            entity.getMarket().addIndustry("IndEvo_academy");
        }
    }

    public static void addPort(SectorEntityToken entity) {
        if (isIndEvoEnabled()) {
            entity.getMarket().addIndustry("IndEvo_port");
        }
    }
}
