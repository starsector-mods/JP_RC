package data.campaign.econ.conditions;

import com.fs.starfarer.api.impl.campaign.econ.BaseHazardCondition;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class LincolnCathedralLink extends BaseHazardCondition {
    public static final float HAZARD_BONUS = -0.15f;
    public static final int STABILITY_BONUS = 1;
    public static final float FLEET_SIZE_BONUS = 0.15f;

    @Override
    public void apply(String id) {
        super.apply(id);
        if (market != null) {
            market.getHazard().modifyFlat(id, HAZARD_BONUS, "Lincoln Cathedral orbital nexus");
            market.getStability().modifyFlat(id, STABILITY_BONUS, "Cathedral Alpha Core coordination");
            market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyFlat(id, FLEET_SIZE_BONUS, "Cathedral orbital telemetry");
        }
    }

    @Override
    public void unapply(String id) {
        super.unapply(id);
        if (market != null) {
            market.getHazard().unmodify(id);
            market.getStability().unmodify(id);
            market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodify(id);
        }
    }

    @Override
    protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
        super.createTooltipAfterDescription(tooltip, expanded);
        tooltip.addPara("%s hazard rating", 10f, Misc.getHighlightColor(), "" + (int)(HAZARD_BONUS * 100f) + "%");
        tooltip.addPara("+%s stability", 3f, Misc.getPositiveHighlightColor(), "" + STABILITY_BONUS);
        tooltip.addPara("+%s colonial fleet size", 3f, Misc.getPositiveHighlightColor(), "" + (int)(FLEET_SIZE_BONUS * 100f) + "%");
    }
}
