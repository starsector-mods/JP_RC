package data.campaign.intel.bar;

import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName.Gender;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventWithPerson;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.util.Misc;

import data.campaign.intel.misc.YorkDiscoveryIntel;

public class YorkBarEvent extends BaseBarEventWithPerson {

    public static enum OptionId {
        INIT,
        BUY_CREDITS,
        CONVINCE_STORY,
        LEAVE
    }

    public static final int PRICE = 15000;

    private static final String[] FEMALE_PORTRAITS = {
        "graphics/portraits/junk_pirates_portrait_f_1.png",
        "graphics/portraits/junk_pirates_portrait_f_2.png",
        "graphics/portraits/junk_pirates_portrait_f_3.png",
        "graphics/portraits/junk_pirates_portrait_f_4.png",
        "graphics/portraits/junk_pirates_portrait_f_5.png",
        "graphics/portraits/junk_pirates_portrait_f_6.png",
        "graphics/portraits/pack_portrait_f_1.png",
        "graphics/portraits/pack_portrait_f_2.png"
    };

    private static final String[] MALE_PORTRAITS = {
        "graphics/portraits/junk_pirates_portrait_m_1.png",
        "graphics/portraits/junk_pirates_portrait_m_2.png",
        "graphics/portraits/junk_pirates_portrait_m_3.png",
        "graphics/portraits/junk_pirates_portrait_m_4.png",
        "graphics/portraits/junk_pirates_portrait_m_5.png",
        "graphics/portraits/junk_pirates_portrait_m_6.png",
        "graphics/portraits/junk_pirates_portrait_m_7.png",
        "graphics/portraits/junk_pirates_portrait_m_8.png",
        "graphics/portraits/junk_pirates_portrait_m_9.png",
        "graphics/portraits/junk_pirates_portrait_m_10.png",
        "graphics/portraits/pack_portrait_m_1.png",
        "graphics/portraits/pack_portrait_m_2.png"
    };

    public YorkBarEvent() {
        super();
    }

    @Override
    protected PersonAPI createPerson() {
        PersonAPI p = Global.getSector().getFaction(getPersonFaction()).createRandomPerson(getPersonGender(), random);
        String[] pool = (p.getGender() == Gender.FEMALE) ? FEMALE_PORTRAITS : MALE_PORTRAITS;
        if (pool.length > 0) {
            p.setPortraitSprite(pool[random.nextInt(pool.length)]);
        }
        p.setRankId(getPersonRank());
        p.setPostId(getPersonPost());
        return p;
    }

    @Override
    public boolean shouldShowAtMarket(MarketAPI market) {
        if (!super.shouldShowAtMarket(market)) return false;
        if (YorkDiscoveryIntel.hasIntel()) return false;

        // Can appear in Breh'Inni, Canis, Ursulo, or any independent/pirate spaceport
        String factionId = market.getFactionId();
        if ("junk_pirates".equals(factionId) || "pack".equals(factionId) || "syndicate_asp".equals(factionId)
                || Factions.INDEPENDENT.equals(factionId) || Factions.PIRATES.equals(factionId)) {
            return true;
        }

        return false;
    }

    @Override
    protected String getPersonFaction() {
        return Factions.INDEPENDENT;
    }

    @Override
    protected String getPersonRank() {
        return Ranks.CITIZEN;
    }

    @Override
    protected String getPersonPost() {
        return Ranks.POST_SPACER;
    }

    @Override
    protected Gender getPersonGender() {
        return Gender.ANY;
    }

    @Override
    public void addPromptAndOption(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        super.addPromptAndOption(dialog, memoryMap);
        regen(dialog.getInteractionTarget().getMarket());

        TextPanelAPI text = dialog.getTextPanel();
        text.addPara("A weathered spacer sitting alone in a corner booth is hunched over a battered TriPad, reviewing faint navigational charts and deep-space survey pings.");

        dialog.getOptionPanel().addOption("Approach the spacer and inquire about " + getHisOrHer() + " survey charts", this);
    }

    @Override
    public void init(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        super.init(dialog, memoryMap);
        done = false;
        dialog.getVisualPanel().showPersonInfo(person, true);
        optionSelected(null, OptionId.INIT);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (!(optionData instanceof OptionId)) return;
        OptionId option = (OptionId) optionData;

        OptionPanelAPI options = dialog.getOptionPanel();
        TextPanelAPI text = dialog.getTextPanel();
        options.clearOptions();

        CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();

        switch (option) {
            case INIT:
                text.addPara(
                    "The spacer looks up from the display, glancing around the crowded room before leaning forward across the table.",
                    Misc.getTextColor()
                );
                text.addPara(
                    "\"You have the bearing of someone who looks beyond the Core, captain. Most in this bar only care about the next supply run or the price of fuel on the black market. But me? I trace the ghosts.\"",
                    Misc.getTextColor()
                );
                text.addPara(
                    "\"My family flew deep-scan recon on an exploratory hauler long before the AI Wars. Far beyond the southern edge of the Sector, past where the beacons die, their sensors locked onto a clean, uncatalogued yellow star. They swore on their dying breaths that it harbored an untouched Terran garden world, rich companion mining moons, and an ancient Domain cathedral station silently holding orbit.\"",
                    Misc.getTextColor()
                );
                text.addPara(
                    "\"I never had the fleet or resources to settle it, but I kept the raw telemetry cartridge. I'm done chasing legends through deep hyperspace, but with the Sector burning, maybe you can build something out there.\"",
                    Misc.getTextColor()
                );
                text.addPara(
                    "\"Word of caution, though: our old sensor logs caught terrifying, non-human neutrino signatures pulsing in that gravity well—heavy automated drone battlegroups patrolling in silent geometric lockstep. If you go out there looking for a new home, bring enough guns to fight a war.\"",
                    Misc.getTextColor()
                );
                text.addPara(
                    "\"And one more thing: the logs say the Cathedral's cognitive nerve center is completely dormant. It takes an Alpha Core to awaken the station's automated systems and unlock the sanctuary. If you bring one out there and slot it into the cradle, Domain protocols will fuse it permanently into the bulkheads—you won't be able to unplug it. But it'll watch over your people forever.\"",
                    Misc.getTextColor()
                );

                if (cargo.getCredits().get() >= PRICE) {
                    options.addOption("Purchase the encrypted nav-cartridge (" + Misc.getDGSCredits(PRICE) + " credits)", OptionId.BUY_CREDITS);
                } else {
                    options.addOption("You cannot afford the nav-cartridge (" + Misc.getDGSCredits(PRICE) + " credits)", OptionId.BUY_CREDITS);
                    options.setEnabled(OptionId.BUY_CREDITS, false);
                }

                int sp = Global.getSector().getPlayerPerson().getStats().getStoryPoints();
                if (sp >= 3) {
                    options.addOption("Confront " + getHimOrHer() + " with your vision of a free haven beyond the Core [3 Story Points]", OptionId.CONVINCE_STORY);
                } else {
                    options.addOption("Confront " + getHimOrHer() + " with your vision of a free haven beyond the Core [Requires 3 Story Points]", OptionId.CONVINCE_STORY);
                    options.setEnabled(OptionId.CONVINCE_STORY, false);
                }

                options.addOption("Politely decline and step away", OptionId.LEAVE);
                break;

            case BUY_CREDITS:
                cargo.getCredits().subtract(PRICE);
                AddRemoveCommodity.addCreditsLossText(PRICE, text);
                Global.getSoundPlayer().playUISound("ui_char_spent_story_point", 1f, 1f);

                text.addPara(
                    "The spacer nods with a mixture of relief and solemn reverence, sliding a heavy, lead-jacketed nav-cartridge across the table.",
                    Misc.getTextColor()
                );
                text.addPara(
                    "\"May the void treat you well, captain. Keep that cartridge shielded—the Core World polities would burn a system to own what's on that chip.\"",
                    Misc.getHighlightColor()
                );

                BarEventManager.getInstance().notifyWasInteractedWith(this);
                YorkDiscoveryIntel.addIntelIfNeeded(text, "bar");
                done = true;
                options.addOption("Leave the table", OptionId.LEAVE);
                break;

            case CONVINCE_STORY:
                Global.getSector().getPlayerPerson().getStats().spendStoryPoints(3, true, text, false, 100f, "Vision for Tomorrow: Acquired coordinates to the lost York sanctuary");
                Global.getSoundPlayer().playUISound("ui_char_spent_story_point", 1f, 1.2f);

                text.addPara(
                    "You outline your plans for the frontier—not another corporate tech-mine or bloody warlord fief, but a genuine sanctuary where spacers and working crews can breathe clean air and build a future free from Hegemony tithes or Syndicate debts.",
                    Misc.getTextColor()
                );
                text.addPara(
                    "The spacer stares at you in silence for a long moment, then lets out a soft laugh. With a firm, solemn nod, " + getHeOrShe() + " pushes the nav-cartridge into your hands.",
                    Misc.getTextColor()
                );
                text.addPara(
                    "\"Take it, captain. No charge. If someone is finally going to wake the Cathedral and plant crops under that yellow star, I want it to be someone with fire in their chest. Go make it count.\"",
                    Misc.getPositiveHighlightColor()
                );

                BarEventManager.getInstance().notifyWasInteractedWith(this);
                YorkDiscoveryIntel.addIntelIfNeeded(text, "bar");
                done = true;
                options.addOption("Thank the spacer and step away", OptionId.LEAVE);
                break;

            case LEAVE:
            default:
                noContinue = true;
                done = true;
                break;
        }
    }
}
