package data.campaign.econ.conditions;

import com.fs.starfarer.api.impl.campaign.econ.BaseHazardCondition;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class JUNK_habTubes extends BaseHazardCondition {
    public static final float HAZARD_BONUS = -0.25f;

    @Override
    public void apply(String id) {
        super.apply(id);
        if (market != null) {
            market.getHazard().modifyFlat(id, HAZARD_BONUS, "Habitation tubes");
        }
    }

    @Override
    public void unapply(String id) {
        super.unapply(id);
        if (market != null) {
            market.getHazard().unmodify(id);
        }
    }

    @Override
    protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
        super.createTooltipAfterDescription(tooltip, expanded);
        tooltip.addPara("%s hazard rating", 10f, Misc.getHighlightColor(), "" + (int)(HAZARD_BONUS * 100f) + "%");
    }
}
