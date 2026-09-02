package data.campaign.econ.conditions;

import com.fs.starfarer.api.impl.campaign.econ.BaseHazardCondition;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class JUNK_habTubes_active extends BaseHazardCondition {
    public static final float HAZARD_MALUS = 0.25f;
    public static final int STABILITY_PENALTY = 1;

    @Override
    public void apply(String id) {
        super.apply(id);
        if (market != null) {
            market.getHazard().modifyFlat(id, HAZARD_MALUS, "Active automata expansion");
            market.getStability().modifyFlat(id, -STABILITY_PENALTY, "Automata cloud interference");
        }
    }

    @Override
    public void unapply(String id) {
        super.unapply(id);
        if (market != null) {
            market.getHazard().unmodify(id);
            market.getStability().unmodify(id);
        }
    }

    @Override
    protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
        super.createTooltipAfterDescription(tooltip, expanded);
        tooltip.addPara("%s hazard rating", 10f, Misc.getHighlightColor(), "+" + (int)(HAZARD_MALUS * 100f) + "%");
        tooltip.addPara("%s stability", 10f, Misc.getHighlightColor(), "-" + STABILITY_PENALTY);
    }
}
