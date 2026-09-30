package data.scripts.campaign.rulecmd;

import java.util.List;
import java.util.Map;
import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc.Token;

import data.campaign.intel.misc.HypercubeDiscoveryIntel;
import data.campaign.intel.misc.YorkDiscoveryIntel;

public class JP_YorkBar_Cmd extends BaseCommandPlugin {

    private static final String[] FEMALE_PORTRAITS = {
        "graphics/portraits/junk_pirates_portrait_f_1.png",
        "graphics/portraits/junk_pirates_portrait_f_2.png",
        "graphics/portraits/junk_pirates_portrait_f_3.png",
        "graphics/portraits/junk_pirates_portrait_f_4.png",
        "graphics/portraits/junk_pirates_portrait_f_5.png"
    };

    private static final String[] MALE_PORTRAITS = {
        "graphics/portraits/junk_pirates_portrait_m_1.png",
        "graphics/portraits/junk_pirates_portrait_m_2.png",
        "graphics/portraits/junk_pirates_portrait_m_3.png",
        "graphics/portraits/junk_pirates_portrait_m_4.png",
        "graphics/portraits/junk_pirates_portrait_m_5.png"
    };

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
        if (params.isEmpty()) return false;
        String action = params.get(0).getString(memoryMap);

        if ("isValid".equals(action)) {
            if (dialog.getInteractionTarget() == null || dialog.getInteractionTarget().getMarket() == null) return false;
            MarketAPI market = dialog.getInteractionTarget().getMarket();
            if (!"ear_burns".equals(market.getId())) return false;
            if (Global.getSector().getMemoryWithoutUpdate().getBoolean("$global.jp_quest_fully_complete")) return false;
            return true;
        }
        
        if ("showPerson".equals(action)) {
            // Generate person if not already in memory
            PersonAPI person = (PersonAPI) memoryMap.get(com.fs.starfarer.api.campaign.rules.MemKeys.GLOBAL).get("$global.jp_yorkBar_person");
            if (person == null) {
                person = Global.getFactory().createPerson();
                person.setFaction(Factions.INDEPENDENT);
                Random random = new Random();
                boolean isFemale = random.nextBoolean();
                person.setGender(isFemale ? FullName.Gender.FEMALE : FullName.Gender.MALE);
                person.getName().setFirst(isFemale ? "Aris" : "Vane");
                person.getName().setLast(isFemale ? "Thorne" : "Kael");
                
                String[] pool = isFemale ? FEMALE_PORTRAITS : MALE_PORTRAITS;
                person.setPortraitSprite(pool[random.nextInt(pool.length)]);
                person.setRankId(Ranks.SPACE_SAILOR);
                person.setPostId(Ranks.POST_SPACER);
                
                memoryMap.get(com.fs.starfarer.api.campaign.rules.MemKeys.GLOBAL).set("$global.jp_yorkBar_person", person);
            }
            dialog.getVisualPanel().showPersonInfo(person, true);
            return true;
        }
        
        if ("hasCredits".equals(action)) {
            int amount = (int) params.get(1).getFloat(memoryMap);
            return Global.getSector().getPlayerFleet().getCargo().getCredits().get() >= amount;
        }
        
        if ("spendCredits".equals(action)) {
            int amount = (int) params.get(1).getFloat(memoryMap);
            Global.getSector().getPlayerFleet().getCargo().getCredits().subtract(amount);
            AddRemoveCommodity.addCreditsLossText(amount, dialog.getTextPanel());
            Global.getSector().getMemoryWithoutUpdate().set("$global.jp_yorkBar_mealBought", true);
            return true;
        }
        
        if ("hasSP".equals(action)) {
            int amount = (int) params.get(1).getFloat(memoryMap);
            return Global.getSector().getPlayerPerson().getStats().getStoryPoints() >= amount;
        }
        
        if ("spendSP_Cathedral".equals(action)) {
            Global.getSector().getPlayerPerson().getStats().spendStoryPoints(3, true, dialog.getTextPanel(), false, 1f, "Vision for Tomorrow: Acquired coordinates to the lost York sanctuary");
            Global.getSoundPlayer().playUISound("ui_char_spent_story_point", 1f, 1.2f);
            YorkDiscoveryIntel.addIntelIfNeeded(dialog.getTextPanel(), "bar");
            return true;
        }
        
        if ("spendSP_Hypercube".equals(action)) {
            Global.getSector().getPlayerPerson().getStats().spendStoryPoints(3, true, dialog.getTextPanel(), false, 1f, "The Hypercube Enigma: Acquired coordinates to the anomaly");
            Global.getSoundPlayer().playUISound("ui_char_spent_story_point", 1f, 1.2f);
            HypercubeDiscoveryIntel.addIntelIfNeeded(dialog.getTextPanel(), "bar");
            return true;
        }
        
        if ("hasYork".equals(action)) {
            return YorkDiscoveryIntel.hasIntel();
        }
        
        if ("hasHypercube".equals(action)) {
            return HypercubeDiscoveryIntel.hasIntel();
        }
        
        if ("isHypercubeSolved".equals(action)) {
            return Global.getSector().getMemoryWithoutUpdate().getBoolean("$global.jp_hypercube_solved_global");
        }
        
        if ("isQuestComplete".equals(action)) {
            return Global.getSector().getMemoryWithoutUpdate().getBoolean("$global.jp_york_quest_complete_global");
        }
        
        if ("removeOption".equals(action)) {
            String optId = params.get(1).getString(memoryMap);
            dialog.getOptionPanel().removeOption(optId);
            return true;
        }

        if ("completeQuest".equals(action)) {
            Global.getSector().getMemoryWithoutUpdate().set("$global.jp_quest_fully_complete", true);
            Global.getSector().getPlayerFleet().getCargo().addCommodity("omega_core", 1);
            AddRemoveCommodity.addCommodityGainText("omega_core", 1, dialog.getTextPanel());
            return true;
        }

        return false;
    }
}
