package data.hullmods;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;


public class CCSyndicateAsp extends BaseHullMod{
	private static final float FLUX_MULT = 1.05f; // The ASP Syndicate are good at looking after their vessels. Slightly more efficient operation.
	private static final float CR_BONUS = 5f;
		
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getMaxCombatReadiness().modifyFlat(id, CR_BONUS * 0.01f);
		stats.getFluxCapacity().modifyMult(id, FLUX_MULT);
		stats.getFluxDissipation().modifyMult(id, FLUX_MULT);}
        
        public String getDescriptionParam(int index, HullSize hullSize) {
            
                float fluxMult = (FLUX_MULT - 1f) * (100f);
                
		if (index == 0) return "" + (int) CR_BONUS + "%";
		if (index == 1) return "" + (int) fluxMult + "%";
		//if (index == 1) return "" + (int) STRENGTH_DECREASE;
		return null;
	}
}