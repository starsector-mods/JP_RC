/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Drops;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.FleetAdvanceScript;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageEntity;
import com.fs.starfarer.api.util.Misc;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author paul
 */
public class JunkPiratesLostTechDefenderInteraction extends BaseCommandPlugin {

    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, final Map<String, MemoryAPI> memoryMap) {
            if (dialog == null) return false;

            final SectorEntityToken entity = dialog.getInteractionTarget();
            final MemoryAPI memory = getEntityMemory(memoryMap);

            final CampaignFleetAPI defenders = memory.getFleet("$defenderFleet");
            if (defenders == null) return false;

            dialog.setInteractionTarget(defenders);

            final FleetInteractionDialogPluginImpl.FIDConfig config = new FleetInteractionDialogPluginImpl.FIDConfig();
            config.leaveAlwaysAvailable = true;
            config.showCommLinkOption = false;
            config.showEngageText = false;
            config.showFleetAttitude = false;
            config.showTransponderStatus = false;
            config.showWarningDialogWhenNotHostile = false;
            config.alwaysAttackVsAttack = true;
            config.impactsAllyReputation = true;
            config.impactsEnemyReputation = false;
            config.pullInAllies = false;
            config.pullInEnemies = false;
            config.pullInStations = false;
            config.lootCredits = false;

            config.firstTimeEngageOptionText = "Engage the strange automata";
            config.afterFirstTimeEngageOptionText = "Re-engage the arachnoid megastructures";
            config.noSalvageLeaveOptionText = "Continue";

            config.dismissOnLeave = false;
            config.printXPToDialog = true;

            long seed = memory.getLong(MemFlags.SALVAGE_SEED);
            config.salvageRandom = Misc.getRandom(seed, 75);


            final FleetInteractionDialogPluginImpl plugin = new FleetInteractionDialogPluginImpl(config);

            final InteractionDialogPlugin originalPlugin = dialog.getPlugin();
            config.delegate = new FleetInteractionDialogPluginImpl.BaseFIDDelegate() {
                    @Override
                    public void notifyLeave(InteractionDialogAPI dialog) {
                            // nothing in there we care about keeping; clearing to reduce savefile size
                            defenders.getMemoryWithoutUpdate().clear();
                            // there's a "standing down" assignment given after a battle is finished that we don't care about
                            defenders.clearAssignments();
                            defenders.deflate();

                            dialog.setPlugin(originalPlugin);
                            dialog.setInteractionTarget(entity);

                            //Global.getSector().getCampaignUI().clearMessages();

                            if (plugin.getContext() instanceof FleetEncounterContext) {
                                    FleetEncounterContext context = (FleetEncounterContext) plugin.getContext();
                                    if (context.didPlayerWinEncounterOutright()) {

//                                            JunkPiratesLostTechSalvageGen.SDMParams p = new JunkPiratesLostTechSalvageGen.SDMParams();
//                                            p.entity = entity;
//                                            p.factionId = defenders.getFaction().getId();
//
//                                            JunkPiratesLostTechSalvageGen.SalvageDefenderModificationPlugin plugin = Global.getSector().getGenericPlugins().pickPlugin(
//                                                                                            JunkPiratesLostTechSalvageGen.SalvageDefenderModificationPlugin.class, p);
//                                            if (plugin != null) {
//                                                    plugin.reportDefeated(p, entity, defenders);
//                                            }

                                            memory.unset("$hasDefenders");
                                            memory.unset("$defenderFleet");
                                            memory.set("$defenderFleetDefeated", true, 60f); // Expire after 60 days
                                            if (entity.getOrbit() != null && entity.getOrbit().getFocus() != null) {
                                                MarketAPI her_market = entity.getOrbit().getFocus().getMarket();
                                                if (her_market != null) {
                                                    if (her_market.hasCondition("JUNK_habTubes_active")) {
                                                        her_market.removeCondition("JUNK_habTubes_active");
                                                        her_market.addCondition("JUNK_habTubes");
                                                    } else if (!her_market.hasCondition("JUNK_habTubes")) {
                                                        her_market.addCondition("JUNK_habTubes");
                                                    }
                                                    her_market.reapplyConditions();
                                                }
                                            }
                                            entity.removeScriptsOfClass(FleetAdvanceScript.class);
                                            FireBest.fire(null, dialog, memoryMap, "BeatDefendersContinue");
                                    } else {
                                            boolean persistDefenders = false;
                                            if (context.isEngagedInHostilities()) {
                                                    persistDefenders |= !Misc.getSnapshotMembersLost(defenders).isEmpty();
                                                    for (FleetMemberAPI member : defenders.getFleetData().getMembersListCopy()) {
                                                            if (member.getStatus().needsRepairs()) {
                                                                    persistDefenders = true;
                                                                    break;
                                                            }
                                                    }
                                            }

                                            if (persistDefenders) {
                                                    if (!entity.hasScriptOfClass(FleetAdvanceScript.class)) {
                                                            defenders.setDoNotAdvanceAI(true);
                                                            defenders.setContainingLocation(entity.getContainingLocation());
                                                            defenders.setLocation(entity.getLocation().x, entity.getLocation().y);
                                                            entity.addScript(new FleetAdvanceScript(defenders));
                                                    }
                                                    memory.expire("$defenderFleet", 10); // defenders may have gotten damaged; persist them for a bit
                                            } else {
                                                    defenders.despawn();
                                                    memory.unset("$defenderFleet");
                                            }
                                            dialog.dismiss();
                                    }
                            } else {
                                    dialog.dismiss();
                            }
                    }
                    @Override
                    public void battleContextCreated(InteractionDialogAPI dialog, BattleCreationContext bcc) {
                            bcc.aiRetreatAllowed = false;
                            bcc.objectivesAllowed = false;
                            bcc.enemyDeployAll = true;
                    }
                    @Override
                    public void postPlayerSalvageGeneration(InteractionDialogAPI dialog, FleetEncounterContext context, CargoAPI salvage) {
                            FleetEncounterContextPlugin.DataForEncounterSide winner = context.getWinnerData();
                            FleetEncounterContextPlugin.DataForEncounterSide loser = context.getLoserData();

                            if (winner == null || loser == null) return;

                            float playerContribMult = context.computePlayerContribFraction();

                            List<SalvageEntityGenDataSpec.DropData> dropRandom = new ArrayList<SalvageEntityGenDataSpec.DropData>();
                            List<SalvageEntityGenDataSpec.DropData> dropValue = new ArrayList<SalvageEntityGenDataSpec.DropData>();

                            float valueMultFleet = Global.getSector().getPlayerFleet().getStats().getDynamic().getValue(Stats.BATTLE_SALVAGE_MULT_FLEET);
                            float valueModShips = context.getSalvageValueModPlayerShips();

                            for (FleetEncounterContextPlugin.FleetMemberData data : winner.getEnemyCasualties()) {
                                    if (config.salvageRandom.nextFloat() < playerContribMult) {
                                            SalvageEntityGenDataSpec.DropData drop = new SalvageEntityGenDataSpec.DropData();
                                            drop.chances = 1;
                                            drop.value = -1;
                                            switch (data.getMember().getHullSpec().getHullSize()) {
                                            case CAPITAL_SHIP:
                                                    drop.group = Drops.AI_CORES3;
                                                    drop.chances = 2;
                                                    break;
                                            case CRUISER:
                                                    drop.group = Drops.AI_CORES3;
                                                    break;
                                            case DESTROYER:
                                                    drop.group = Drops.AI_CORES2;
                                                    break;
                                            case FIGHTER:
                                            case FRIGATE:
                                                    drop.group = Drops.AI_CORES1;
                                                    break;
                                            default: break;
                                            }
                                            if (drop.group != null) {
                                                    dropRandom.add(drop);
                                            }
                                    }
                            }
                            float fuelMult = Global.getSector().getPlayerFleet().getStats().getDynamic().getValue(Stats.FUEL_SALVAGE_VALUE_MULT_FLEET);
                            //float fuel = salvage.getFuel();
                            //salvage.addFuel((int) Math.round(fuel * fuelMult));

                             // Track the number of defeated Spinerettes globally to scale down rewards on second+ encounters
                             MemoryAPI globalMemory = Global.getSector().getCharacterData().getMemoryWithoutUpdate();
                             int defeatedCount = 0;
                             if (globalMemory.contains("$spineretteDefeatedCount")) {
                                 defeatedCount = globalMemory.getInt("$spineretteDefeatedCount");
                             }
                             globalMemory.set("$spineretteDefeatedCount", defeatedCount + 1);

                             float rewardMult = 1f;
                             if (defeatedCount == 1) {
                                 rewardMult = 0.5f; // 2nd encounter: 50% rewards
                             } else if (defeatedCount > 1) {
                                 rewardMult = 0.4f; // 3rd+ encounters: 40% rewards
                             }

                             // From second encounter onwards, disable special items (AI cores, etc.)
                             if (defeatedCount >= 1) {
                                 dropRandom.clear();
                                 // Add weapons instead sometimes
                                 if (config.salvageRandom.nextFloat() < 0.5f) {
                                     SalvageEntityGenDataSpec.DropData drop = new SalvageEntityGenDataSpec.DropData();
                                     drop.chances = 2;
                                     drop.group = "weapons1"; // Basic weapons
                                     dropRandom.add(drop);
                                 }
                             }

                             CargoAPI extra = SalvageEntity.generateSalvage(config.salvageRandom, (valueMultFleet + valueModShips) * rewardMult, rewardMult, 1f, fuelMult * rewardMult, dropValue, dropRandom);
                             for (CargoStackAPI stack : extra.getStacksCopy()) {
                                     if (stack.isFuelStack()) {
                                             stack.setSize((int)(stack.getSize() * fuelMult * rewardMult));
                                     }
                                     salvage.addFromStack(stack);
                             }

                    }

            };


            dialog.setPlugin(plugin);
            plugin.init(dialog);

            return true;
    }
}
