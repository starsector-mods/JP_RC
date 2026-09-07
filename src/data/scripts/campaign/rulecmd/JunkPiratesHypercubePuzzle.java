package data.scripts.campaign.rulecmd;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

public class JunkPiratesHypercubePuzzle extends BaseCommandPlugin {

    private static final String KEY_SOUND = "$hypercube_sound";
    private static final String KEY_LIGHT = "$hypercube_light";
    private static final String KEY_TENSION = "$hypercube_tension";
    private static final String KEY_INIT = "$hypercube_puzzle_init";

    // Initial starting state: Total energy = 6 (Sound=1, Light=3, Tension=2)
    private static final int INIT_SOUND = 1;
    private static final int INIT_LIGHT = 3;
    private static final int INIT_TENSION = 2;

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) { Global.getLogger(JunkPiratesHypercubePuzzle.class).info("Dialog is null!"); return false; }
        MemoryAPI mem = memoryMap.get(MemKeys.LOCAL);
        if (mem == null) { Global.getLogger(JunkPiratesHypercubePuzzle.class).info("MemoryAPI LOCAL is null!"); return false; }

        String action = params != null && !params.isEmpty() ? params.get(0).getString(memoryMap) : "init"; Global.getLogger(JunkPiratesHypercubePuzzle.class).info("Hypercube action: " + action);
        TextPanelAPI text = dialog.getTextPanel();

        // Initialize state if not present
        if (!mem.contains(KEY_INIT)) {
            resetState(mem);
        }

        int sound = mem.getInt(KEY_SOUND);
        int light = mem.getInt(KEY_LIGHT);
        int tension = mem.getInt(KEY_TENSION);

        if ("init".equals(action)) {
            data.campaign.intel.misc.HypercubeDiscoveryIntel.addIntelIfNeeded(text, "discovery");
            data.campaign.intel.misc.HypercubeDiscoveryIntel.markFound(text);
            printTelemetry(dialog, text, sound, light, tension);
            return true;
        }

        if ("ring".equals(action)) {
            // Sound -> Light
            if (sound <= 0) {
                triggerOverload(ruleId, dialog, memoryMap, mem, "Acoustic Cavitation! You attempt to siphon sound from an already silent chamber. The mechanism violently backfires!");
                return true;
            }
            if (light >= 3) {
                triggerOverload(ruleId, dialog, memoryMap, mem, "Luminescent Fracture! The light prism cannot contain any more energy. Searing radiation erupts through the casing!");
                return true;
            }
            sound--;
            light++;
            mem.set(KEY_SOUND, sound);
            mem.set(KEY_LIGHT, light);
            Global.getSoundPlayer().playUISound("ui_char_spent_story_point", 1f, 1f);
            text.addPara("You rotate the outer brass ring. The mechanical dampers shift, absorbing acoustic resonance and focusing it directly into the luminescence prism.");
            printTelemetry(dialog, text, sound, light, tension);
            return true;
        }

        if ("core".equals(action)) {
            // Light -> Tension
            if (light <= 0) {
                triggerOverload(ruleId, dialog, memoryMap, mem, "Photonic Depletion! There is no light left in the matrix to compress. The obsidian core seizes and snaps back!");
                return true;
            }
            if (tension >= 3) {
                triggerOverload(ruleId, dialog, memoryMap, mem, "Gravitational Shear Critical! The spatial lattice cannot withstand any more stress. Micro-wormholes violently rip across your fleet!");
                return true;
            }
            light--;
            tension++;
            mem.set(KEY_LIGHT, light);
            mem.set(KEY_TENSION, tension);
            Global.getSoundPlayer().playUISound("ui_char_spent_story_point", 1f, 1f);
            text.addPara("You depress the sunken obsidian core. The internal prisms collapse inward, extinguishing light and transferring the compressed energy into spatial tension.");
            printTelemetry(dialog, text, sound, light, tension);
            return true;
        }

        if ("facets".equals(action)) {
            // Tension -> Sound
            if (tension <= 0) {
                triggerOverload(ruleId, dialog, memoryMap, mem, "Spatial Slack! There is zero gravitational tension remaining to release. The sliding facets grind dry against the adamantine frame!");
                return true;
            }
            if (sound >= 3) {
                triggerOverload(ruleId, dialog, memoryMap, mem, "Harmonic Overload! The acoustic resonance has spiked past safe tolerances. A deafening sonic shockwave tears through the bulkheads!");
                return true;
            }
            tension--;
            sound++;
            mem.set(KEY_TENSION, tension);
            mem.set(KEY_SOUND, sound);
            Global.getSoundPlayer().playUISound("ui_char_spent_story_point", 1f, 1f);
            text.addPara("You slide the lateral facets along their tracks. The gravitational torque unwinds, venting spatial stress as a deep, resonant acoustic hum.");
            printTelemetry(dialog, text, sound, light, tension);
            return true;
        }

        if ("pin".equals(action)) {
            // Discharge all channels
            if (sound <= 0 || light <= 0 || tension <= 0) {
                triggerOverload(ruleId, dialog, memoryMap, mem, "Asymmetrical Discharge! The grounding pin requires balanced conduction across all three channels. Discharging through a dry channel causes a catastrophic short-circuit!");
                return true;
            }
            sound--;
            light--;
            tension--;
            mem.set(KEY_SOUND, sound);
            mem.set(KEY_LIGHT, light);
            mem.set(KEY_TENSION, tension);

            if (sound == 0 && light == 0 && tension == 0) {
                // WIN!
                SectorEntityToken target = dialog.getInteractionTarget();
                if (target != null) {
                    if (target.getMemoryWithoutUpdate().getBoolean("$hypercube_completed")) return false; // Anti-glitch
                    target.getMemoryWithoutUpdate().set("$hypercube_completed", true);
                }
                
                Global.getSoundPlayer().playUISound("ui_char_spent_story_point", 1f, 1.2f);
                CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
                if (playerFleet != null && playerFleet.getCargo() != null) {
                    playerFleet.getCargo().addCommodity("alpha_core", 1);
                    playerFleet.getCargo().addCommodity("rare_ore", 150);
                    playerFleet.getCargo().addCommodity("volatiles", 100);
                }
                
                if (Global.getSector().getPlayerPerson() != null && Global.getSector().getPlayerPerson().getStats() != null) {
                    Global.getSector().getPlayerPerson().getStats().addXP(50000, dialog.getTextPanel());
                }

                text.addPara("You strike the central grounding pin. A profound, shuddering shockwave ripples outward through the vacuum.", Misc.getPositiveHighlightColor());
                text.addPara("The ticking dies. The smoldering luminescence extinguishes into pure void. The gravitational torque vanishes as local space settles into glass-like tranquility.");
                text.addPara("In total, breathless silence and absolute darkness, the monolithic hexahedron slowly unfolds. Facets slide aside with impossible precision, unveiling an ancient Domain-era containment capsule nestled at the core.");
                text.addPara("Recovered 1 Alpha Core, 150 Rare Ore, 100 Volatiles, and 50,000 XP.", Misc.getPositiveHighlightColor());

                boolean alreadyKnewYork = data.campaign.intel.misc.YorkDiscoveryIntel.hasIntel();
                // Award York Discovery Intel
                data.campaign.intel.misc.YorkDiscoveryIntel.addIntelIfNeeded(dialog.getTextPanel(), "hypercube");
                
                if (alreadyKnewYork) {
                    text.addPara("Nestled alongside the Alpha Core lies an encrypted Early-Domain Astrometric Datacore. You run its navigation telemetry against the spacer's survey charts, confirming the precise coordinates for the 'York' system deep in the southern fringe.", Misc.getHighlightColor());
                } else {
                    text.addPara("Nestled alongside the Alpha Core lies an encrypted Early-Domain Astrometric Datacore. Translating its navigation telemetry reveals the precise coordinates of a forgotten frontier system designated 'York' deep in the southern fringe.", Misc.getHighlightColor());
                }
                text.addPara("Telemetry archives indicate this Alpha Core was specifically engineered to be slotted into the neural cradle of 'Lincoln Cathedral'—the grand orbital sanctuary station overlooking the garden world of Lincoln. In accordance with Domain sanctuary safety protocols, once slotted into the Cathedral, the core will permanently fuse to the station and cannot be unplugged.", Misc.getTextColor());

                dialog.getOptionPanel().clearOptions();
                dialog.getOptionPanel().addOption("Salvage the contents and withdraw", "hypercube_leave");
                return true;
            }

            Global.getSoundPlayer().playUISound("ui_char_spent_story_point", 1f, 0.9f);
            text.addPara("You strike the central grounding pin. A harmonic pulse discharges evenly into the vacuum, dissipating energy from all three channels simultaneously.");
            printTelemetry(dialog, text, sound, light, tension);
            return true;
        }

        return false;
    }

    private void resetState(MemoryAPI mem) {
        mem.set(KEY_INIT, true);
        mem.set(KEY_SOUND, INIT_SOUND);
        mem.set(KEY_LIGHT, INIT_LIGHT);
        mem.set(KEY_TENSION, INIT_TENSION);
    }

    private void triggerOverload(String ruleId, InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap, MemoryAPI mem, String reason) {
        Global.getSoundPlayer().playUISound("hit_heavy", 1f, 0.8f);
        TextPanelAPI text = dialog.getTextPanel();
        text.addPara(reason, Misc.getNegativeHighlightColor());

        // Apply dynamic cargo & crew loss
        List<Misc.Token> lossParams = new ArrayList<Misc.Token>();
        lossParams.add(new Misc.Token("1", Misc.TokenType.LITERAL));
        new JunkPiratesHypercubeLoss().execute(ruleId, dialog, lossParams, memoryMap);

        text.addPara("The Hypercube violently snaps back to its initial chaotic equilibrium!", Misc.getNegativeHighlightColor());
        resetState(mem);
        printTelemetry(dialog, text, INIT_SOUND, INIT_LIGHT, INIT_TENSION);
    }

    private void printTelemetry(InteractionDialogAPI dialog, TextPanelAPI text, int sound, int light, int tension) {
        text.setFontSmallInsignia();
        text.addPara("--- ANOMALY SENSOR TELEMETRY ---", Misc.getGrayColor());

        String soundStr;
        Color soundCol = Misc.getTextColor();
        switch (sound) {
            case 0: soundStr = "0 / 3 - SILENT (Vacuum Void - STABILIZED)"; soundCol = Misc.getPositiveHighlightColor(); break;
            case 1: soundStr = "1 / 3 - TICKING (Clockwork Cadence)"; break;
            case 2: soundStr = "2 / 3 - CHIMING (Cathedral Resonance)"; break;
            default: soundStr = "3 / 3 - SHRIEKING (Harmonic Strain - CRITICAL)"; soundCol = Misc.getNegativeHighlightColor(); break;
        }
        text.addPara("Acoustic Resonance: " + soundStr, soundCol);

        String lightStr;
        Color lightCol = Misc.getTextColor();
        switch (light) {
            case 0: lightStr = "0 / 3 - PITCH (Absolute Silhouette - STABILIZED)"; lightCol = Misc.getPositiveHighlightColor(); break;
            case 1: lightStr = "1 / 3 - AMBER (Smoldering Filigree)"; break;
            case 2: lightStr = "2 / 3 - CYAN (Geometric Radiation)"; break;
            default: lightStr = "3 / 3 - ULTRAVIOLET (Prismatic Bleed - CRITICAL)"; lightCol = Misc.getNegativeHighlightColor(); break;
        }
        text.addPara("Luminescence: " + lightStr, lightCol);

        String tensionStr;
        Color tensionCol = Misc.getTextColor();
        switch (tension) {
            case 0: tensionStr = "0 / 3 - STABLE (Zero Drift - STABILIZED)"; tensionCol = Misc.getPositiveHighlightColor(); break;
            case 1: tensionStr = "1 / 3 - TREMOR (Micro-Vibration)"; break;
            case 2: tensionStr = "2 / 3 - TORQUE (Gravitational Shear)"; break;
            default: tensionStr = "3 / 3 - CRITICAL (Dimensional Tearing - CRITICAL)"; tensionCol = Misc.getNegativeHighlightColor(); break;
        }
        text.addPara("Spatial Tension: " + tensionStr, tensionCol);
                dialog.getOptionPanel().clearOptions();
        dialog.getOptionPanel().addOption("Rotate the Outer Brass Ring (Sound -> Light)", "hypercube_act_ring");
        dialog.getOptionPanel().addOption("Depress the Sunken Obsidian Core (Light -> Tension)", "hypercube_act_core");
        dialog.getOptionPanel().addOption("Slide the Lateral Facets (Tension -> Sound)", "hypercube_act_facets");
        dialog.getOptionPanel().addOption("Strike the Central Grounding Pin (Discharge All)", "hypercube_act_pin");
        dialog.getOptionPanel().addOption("Review Salvaged Audio Log", "hypercube_clue");
        dialog.getOptionPanel().addOption("Withdraw from Anomaly", "hypercube_leave");
        text.setFontInsignia();

    }
}
