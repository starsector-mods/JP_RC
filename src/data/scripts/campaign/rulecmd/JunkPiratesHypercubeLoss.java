package data.scripts.campaign.rulecmd;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

public class JunkPiratesHypercubeLoss extends BaseCommandPlugin {

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;
        
        int severity = 1;
        if (params != null && !params.isEmpty()) {
            severity = params.get(0).getInt(memoryMap);
        }
        
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        Random rand = new Random();

        List<String> pool = new ArrayList<String>();
        pool.add("crew");
        pool.add("supplies");
        pool.add("fuel");
        pool.add("heavy_machinery");
        pool.add("marines");
        pool.add("metals");
        pool.add("rare_metals");
        pool.add("volatiles");
        pool.add("organics");
        pool.add("luxury_goods");
        pool.add("drugs");

        int numTypes = 1 + rand.nextInt(2 + severity); 
        List<String> toLose = new ArrayList<String>();
        
        while (toLose.size() < numTypes && !pool.isEmpty()) {
            String pick = pool.remove(rand.nextInt(pool.size()));
            if (playerFleet.getCargo().getCommodityQuantity(pick) > 0) {
                toLose.add(pick);
            }
        }

        if (toLose.isEmpty()) {
            dialog.getTextPanel().addPara("The anomaly hungers, but your holds are entirely empty.", Misc.getGrayColor());
            return true;
        }

        for (String commodityId : toLose) {
            float current = playerFleet.getCargo().getCommodityQuantity(commodityId);
            float lossFraction = 0.02f + (rand.nextFloat() * 0.05f * severity); // 2-7% sev 1, 2-12% sev 2
            int loss = (int) Math.ceil(current * lossFraction);
            
            int flatLoss = (rand.nextInt(10) + 5) * severity;
            if (loss < flatLoss) {
                loss = flatLoss;
            }
            if (loss > current) {
                loss = (int) current;
            }
            if (loss < 1) loss = 1;

            playerFleet.getCargo().removeCommodity(commodityId, loss);
            String name = Global.getSettings().getCommoditySpec(commodityId).getName();
            
            dialog.getTextPanel().addPara("Lost %s " + name + " to the anomaly.", Misc.getNegativeHighlightColor(), "" + loss);
        }

        return true;
    }
}
