package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.loading.IndustrySpecAPI;

public class JP_IndEvoIntegration {

    public static final String ACADEMY_ID = "IndEvo_Academy";
    public static final String PORT_ID = "IndEvo_PrivatePort";

    public static boolean isIndEvoEnabled() {
        return Global.getSettings().getModManager().isModEnabled("IndEvo");
    }

    private static boolean hasIndustrySpec(String id) {
        for (IndustrySpecAPI spec : Global.getSettings().getAllIndustrySpecs()) {
            if (spec.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    public static void addAcademy(SectorEntityToken entity) {
        if (isIndEvoEnabled() && entity != null && entity.getMarket() != null) {
            if (hasIndustrySpec(ACADEMY_ID)) {
                entity.getMarket().addIndustry(ACADEMY_ID);
            }
        }
    }

    public static void addPort(SectorEntityToken entity) {
        if (isIndEvoEnabled() && entity != null && entity.getMarket() != null) {
            if (hasIndustrySpec(PORT_ID)) {
                entity.getMarket().addIndustry(PORT_ID);
            }
        }
    }
}
