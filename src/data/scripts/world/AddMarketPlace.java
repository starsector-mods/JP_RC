package data.scripts.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Misc;
import java.util.ArrayList;

public class AddMarketPlace {

    public static MarketAPI addMarketplace(String factionID, SectorEntityToken primaryEntity, ArrayList<SectorEntityToken> connectedEntities, String name, 
                                    int size, ArrayList<String> marketConditions, ArrayList<String> Industries, ArrayList<String> submarkets, float tariff) {  
        EconomyAPI globalEconomy = Global.getSector().getEconomy();  
        String planetID = primaryEntity.getId();  
        String marketID = planetID;
              
        MarketAPI newMarket = Global.getFactory().createMarket(marketID, name, size);  
        newMarket.setFactionId(factionID);  
        newMarket.setPrimaryEntity(primaryEntity);  
        newMarket.setSurveyLevel(MarketAPI.SurveyLevel.FULL);
        newMarket.getTariff().modifyFlat("generator", tariff);  
        newMarket.setUseStockpilesForShortages(true);
              
        if (null != submarkets){  
            for (String market : submarkets){  
                newMarket.addSubmarket(market);  
            }  
        }  
        
        if (!marketConditions.contains("population_" + size)) {
            newMarket.addCondition("population_" + size);
        }
              
        for (String condition : marketConditions) {  
            newMarket.addCondition(condition);  
        }
        
        for (String industry : Industries) {
            newMarket.addIndustry(industry);
        }
              
        if (null != connectedEntities) {  
            for (SectorEntityToken entity : connectedEntities) {  
                if (entity != primaryEntity && !newMarket.getConnectedEntities().contains(entity)) {
                    newMarket.getConnectedEntities().add(entity);  
                }
            }  
        }  
            
        globalEconomy.addMarket(newMarket, true);  
        primaryEntity.setMarket(newMarket);
        primaryEntity.setFaction(factionID);
        newMarket.getMemoryWithoutUpdate().set("$core_noDeciv", true);
        Misc.setFullySurveyed(newMarket, null, false);
              
        if (null != connectedEntities) {  
            for (SectorEntityToken entity : connectedEntities) {  
                if (entity != primaryEntity) {
                    entity.setMarket(newMarket);
                    entity.setFaction(factionID);
                }
            }  
        }
            
        return newMarket;
    }
}
