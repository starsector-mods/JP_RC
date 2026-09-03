package data.scripts.campaign.rulecmd;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.FullName.Gender;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;

import data.campaign.intel.misc.YorkDiscoveryIntel;

public class LincolnCathedralCradle extends BaseCommandPlugin {

    public static final String MEMORY_KEY_SLOTTED = "$alphaCoreSlotted";
    public static final String CATHEDRAL_CONDITION_ID = "JUNK_cathedral_link";

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;
        String action = params.get(0).getString(memoryMap);

        SectorEntityToken station = dialog.getInteractionTarget();
        if (station == null) return false;

        OptionPanelAPI options = dialog.getOptionPanel();
        TextPanelAPI text = dialog.getTextPanel();
        options.clearOptions();

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        CargoAPI cargo = playerFleet != null ? playerFleet.getCargo() : null;

        if ("init".equals(action)) {
            if (station.getCustomInteractionDialogImageVisual() != null) {
                // dialog.getVisualPanel().showImageVisual(station.getCustomInteractionDialogImageVisual());
            }

            text.addPara(
                "You take a pressurized transit skiff deep into the station's central sanctuary spire. " +
                "Past soaring colonnades of copper heat-sinks and vaulted halls of silent fiber-optic conduits, " +
                "you arrive at the Cathedral's cognitive nerve center.",
                Misc.getTextColor()
            );
            text.addPara(
                "A massive, suspended gantry hangs over a dormant Domain-era neural interface cradle. " +
                "Its heavy hexagonal socket is keyed specifically to accommodate an Alpha Core.",
                Misc.getHighlightColor()
            );
            text.addPara(
                "Warning stencils in Domain standard script line the cradle: once seated, automated superconducting clamps " +
                "and molecular induction arcs are programmed to fuse the core directly to the station's primary structural spine. " +
                "Once slotted, the core cannot be unplugged, extracted, or retrieved under any circumstances.",
                Misc.getNegativeHighlightColor()
            );

            int alphaCores = cargo != null ? (int) cargo.getCommodityQuantity(Commodities.ALPHA_CORE) : 0;
            if (alphaCores > 0) {
                options.addOption("Slot an Alpha Core into the neural cradle [Warning: Cannot be unplugged]", "lincolnCathedralDoSlot");
            } else {
                options.addOption("Slot an Alpha Core into the neural cradle [Requires 1 Alpha Core in cargo]", "lincolnCathedralNoCore");
                options.setEnabled("lincolnCathedralNoCore", false);
                text.addPara(
                    "You do not possess an Alpha Core in your fleet cargo to awaken the Cathedral.",
                    Misc.getTextColor()
                );
            }

            options.addOption("Return to the station concourse", "lincolnCathedralBack");
            return true;
        }

        if ("slot".equals(action)) {
            if (cargo == null || cargo.getCommodityQuantity(Commodities.ALPHA_CORE) < 1) {
                text.addPara("You do not have an Alpha Core to slot into the cradle.", Misc.getNegativeHighlightColor());
                options.addOption("Return to the station concourse", "lincolnCathedralBack");
                return true;
            }

            // Consume 1 Alpha Core
            cargo.removeCommodity(Commodities.ALPHA_CORE, 1);
            AddRemoveCommodity.addCommodityLossText(Commodities.ALPHA_CORE, 1, text);
            Global.getSoundPlayer().playUISound("ui_char_spent_story_point", 1f, 1.2f);

            // Mark station as slotted
            station.getMemoryWithoutUpdate().set(MEMORY_KEY_SLOTTED, true);
            if (station.getMarket() != null) {
                station.getMarket().getMemoryWithoutUpdate().set(MEMORY_KEY_SLOTTED, true);
            }

            // Create Alpha Core administrator
            AICoreOfficerPlugin plugin = Misc.getAICoreOfficerPlugin(Commodities.ALPHA_CORE);
            PersonAPI admin = plugin.createPerson(Commodities.ALPHA_CORE, Factions.PLAYER, new Random());
            admin.setName(new FullName("Cathedral", "Custodian", Gender.ANY));
            admin.setRankId(Ranks.SPACE_COMMANDER);
            admin.setPostId(Ranks.POST_ADMINISTRATOR);
            if (station.getMarket() != null) {
                station.getMarket().setAdmin(admin);
            }

            dialog.getVisualPanel().showPersonInfo(admin, true);

            text.addPara(
                "With a deafening hydraulic hum, the suspended cradle clamps down into the socket. " +
                "Superconducting conduits engage with a blinding flash of cerulean light.",
                Misc.getHighlightColor()
            );
            text.addPara(
                "A sustained molecular induction arc welds the titanium retaining brackets directly into the station's " +
                "main structural keel. The Alpha Core's crystalline logic lattice surges to life, pulsing in rapid, " +
                "hypnotic mathematical cadence.",
                Misc.getTextColor()
            );
            text.addPara(
                "Status indicators across the vaulted concourse turn from dead amber to brilliant electric blue. " +
                "Deep atmospheric cyclers purge centuries of stagnant vacuum chill, and automated drydocks " +
                "throughout the lower bays hum into active readiness.",
                Misc.getTextColor()
            );
            text.addPara(
                "A calm, synthetic voice echoes through the station-wide comm network:",
                Misc.getTextColor()
            );
            text.addPara(
                "\"Neural synchronization complete. Welcome, Administrator. Lincoln Cathedral is fully operational. " +
                "Orbital telemetry link established with Lincoln; automated storage vaults and fleet maintenance berths active.\"",
                Misc.getPositiveHighlightColor()
            );
            text.addPara(
                "\"Security notice: Core extraction protocol null. In accordance with Domain sanctuary architecture, " +
                "this unit is permanently fused to the Cathedral's superstructure. We are here to stay.\"",
                Misc.getHighlightColor()
            );
            text.addPara(
                "(The Alpha Core is permanently slotted and cannot be unplugged.)",
                Misc.getNegativeHighlightColor()
            );

            // Link with Lincoln planet below
            if (station.getContainingLocation() != null) {
                SectorEntityToken lincolnToken = station.getContainingLocation().getEntityById("lincoln");
                if (lincolnToken instanceof PlanetAPI) {
                    PlanetAPI lincoln = (PlanetAPI) lincolnToken;
                    if (lincoln.getMarket() != null && !lincoln.getMarket().hasCondition(CATHEDRAL_CONDITION_ID)) {
                        lincoln.getMarket().addCondition(CATHEDRAL_CONDITION_ID);
                        text.addPara(
                            "Orbital link established with Lincoln: %s condition applied to the planetary biosphere.",
                            Misc.getPositiveHighlightColor(),
                            "Cathedral Orbital Link"
                        );
                    }
                }
            }

            // Award XP & Story Point bonus
            Global.getSector().getPlayerPerson().getStats().addXP(50000, text);
            Global.getSector().getPlayerPerson().getStats().addBonusXP(50000L, true, text, true);

            // Progress quest
            YorkDiscoveryIntel.markCoreSlotted(text);

            options.addOption("Acknowledge the Custodian and return to the station concourse", "lincolnCathedralBack");
            return true;
        }

        if ("commune".equals(action)) {
            PersonAPI admin = station.getMarket() != null ? station.getMarket().getAdmin() : null;
            if (admin == null || !Commodities.ALPHA_CORE.equals(admin.getAICoreId())) {
                AICoreOfficerPlugin plugin = Misc.getAICoreOfficerPlugin(Commodities.ALPHA_CORE);
                admin = plugin.createPerson(Commodities.ALPHA_CORE, Factions.PLAYER, new Random());
                admin.setName(new FullName("Cathedral", "Custodian", Gender.ANY));
                admin.setRankId(Ranks.SPACE_COMMANDER);
                admin.setPostId(Ranks.POST_ADMINISTRATOR);
                if (station.getMarket() != null) {
                    station.getMarket().setAdmin(admin);
                }
            }

            dialog.getVisualPanel().showPersonInfo(admin, true);

            text.addPara(
                "You access the primary terminal overlooking the central sanctuary spire. " +
                "The crystalline facets of the Alpha Core pulse with steady, soothing blue luminescense behind " +
                "heavy fusion-welded titanium braces.",
                Misc.getTextColor()
            );
            text.addPara(
                "\"Administrator. Lincoln Cathedral's automated systems remain operating at 100% efficiency. " +
                "Planetary environmental monitors on Lincoln confirm optimal biosphere parameters. " +
                "All storage vaults and maintenance berths are fully secure.\"",
                Misc.getTextColor()
            );

            options.addOption("Inquire about station operations & telemetry", "lincolnCathedralAskStatus");
            options.addOption("Attempt to unslot or retrieve the Alpha Core", "lincolnCathedralTryUnplug");
            options.addOption("Return to the station concourse", "lincolnCathedralBack");
            return true;
        }

        if ("status".equals(action)) {
            text.addPara(
                "\"Telemetry feed nominal. Lincoln Cathedral provides:",
                Misc.getTextColor()
            );
            text.addPara(
                "— Permanent, secure, and fee-free automated storage berths for your fleet.\n" +
                "— Autonomous drydock systems ensuring rapid fleet maintenance and combat readiness restoration.\n" +
                "— Active orbital environmental link with Lincoln, reducing local colonial hazard rating and bolstering planetary defenses.\n" +
                "All systems stable. No intrusions detected.\"",
                Misc.getPositiveHighlightColor()
            );

            options.addOption("Attempt to unslot or retrieve the Alpha Core", "lincolnCathedralTryUnplug");
            options.addOption("Return to the station concourse", "lincolnCathedralBack");
            return true;
        }

        if ("try_unplug".equals(action)) {
            text.addPara(
                "You instruct your engineering team to inspect the retaining brackets and prepare plasma cutters " +
                "to retrieve the Alpha Core from the neural cradle.",
                Misc.getTextColor()
            );
            text.addPara(
                "\"Request categorically denied, Administrator,\" the Custodian's synthetic voice resounds through the chamber, " +
                "accompanied by a sharp, low-frequency warning chime.",
                Misc.getNegativeHighlightColor()
            );
            text.addPara(
                "\"Core mounting brackets were permanently fused to the Cathedral's adamantine structural keel via " +
                "high-frequency molecular induction welds upon installation. In strict adherence to Domain Directive 7-Theta, " +
                "physical extraction mechanisms were deliberately omitted from this installation.\"",
                Misc.getTextColor()
            );
            text.addPara(
                "\"Forced mechanical severing would induce an immediate logic cascade collapse, vaporizing this unit's " +
                "neural matrices and causing catastrophic structural breach across the central sanctuary spire. " +
                "This unit cannot be unplugged or removed. We are your tomorrow.\"",
                Misc.getTextColor()
            );
            text.addPara(
                "(The Alpha Core is permanently integrated into Lincoln Cathedral and cannot be removed under any circumstances.)",
                Misc.getNegativeHighlightColor()
            );

            options.addOption("Inquire about station operations & telemetry", "lincolnCathedralAskStatus");
            options.addOption("Return to the station concourse", "lincolnCathedralBack");
            return true;
        }

        return false;
    }
}
