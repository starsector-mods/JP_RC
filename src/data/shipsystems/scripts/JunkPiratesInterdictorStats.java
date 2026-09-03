package data.shipsystems.scripts;

import java.awt.Color;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.FindShipFilter;

public class JunkPiratesInterdictorStats extends BaseShipSystemScript {
	public static final Object SHIP_KEY = new Object();
	public static final Object TARGET_KEY = new Object();
	
	public static final float RANGE = 1000f;
	public static final float DEBUFF_DURATION = 3.0f;
	public static final Color EFFECT_COLOR = new Color(100, 165, 255, 120);
	
	public static class TargetData {
		public ShipAPI target;
		public boolean triggered = false;
		public TargetData(ShipAPI target) {
			this.target = target;
		}
	}
	
	public static float getFluxForHullSize(HullSize size) {
		switch (size) {
			case FIGHTER:
				return 200f;
			case FRIGATE:
				return 450f;
			case DESTROYER:
				return 750f;
			case CRUISER:
				return 1300f;
			case CAPITAL_SHIP:
				return 1900f;
			default:
				return 400f;
		}
	}
	
	@Override
	public void apply(MutableShipStatsAPI stats, final String id, State state, float effectLevel) {
		ShipAPI ship = null;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
		} else {
			return;
		}
		
		final String targetDataKey = ship.getId() + "_interdictor_target_data";
		
		Object targetDataObj = Global.getCombatEngine().getCustomData().get(targetDataKey); 
		if (state == State.IN && targetDataObj == null) {
			ShipAPI target = findTarget(ship);
			Global.getCombatEngine().getCustomData().put(targetDataKey, new TargetData(target));
		} else if (state == State.IDLE && targetDataObj != null) {
			Global.getCombatEngine().getCustomData().remove(targetDataKey);
		}
		if (targetDataObj == null || ((TargetData) targetDataObj).target == null) return;
		
		final TargetData targetData = (TargetData) targetDataObj;
		final ShipAPI mainTarget = targetData.target;
		final ShipAPI sourceShip = ship;
		
		// Ensure activation effects trigger EXACTLY ONCE per pulse
		if (effectLevel >= 1f && !targetData.triggered) {
			targetData.triggered = true;
			
			// Flux Transfer Mechanic: Siphon 40% of current flux from the source and dump it onto the target
			float currentTotalFlux = sourceShip.getFluxTracker().getCurrFlux();
			float fluxToTransfer = currentTotalFlux * 0.40f;
			sourceShip.getFluxTracker().setCurrFlux(currentTotalFlux - fluxToTransfer);
			sourceShip.getFluxTracker().setHardFlux(sourceShip.getFluxTracker().getHardFlux() * 0.60f);

			Color color = getEffectColor(mainTarget);
			color = Misc.setAlpha(color, 255);

			if (mainTarget.getFluxTracker().showFloaty() || 
					ship == Global.getCombatEngine().getPlayerShip() ||
					mainTarget == Global.getCombatEngine().getPlayerShip()) {
				mainTarget.getFluxTracker().showOverloadFloatyIfNeeded("Ship systems interdicted", color, 4f, true);
			}

			// 1. Initial complete engine flameout (triggered ONCE on hit)
			ShipEngineControllerAPI initialEc = mainTarget.getEngineController();
			if (initialEc != null) {
				for (ShipEngineAPI engine : initialEc.getShipEngines()) {
					engine.disable(false);
				}
				initialEc.forceFlameout(false);
				initialEc.computeEffectiveStats(mainTarget == Global.getCombatEngine().getPlayerShip());
			}

			// 2. Add base hull-size flux PLUS the transferred flux to the enemy
			float baseFlux = getFluxForHullSize(mainTarget.getHullSize());
			
			float softFluxToAdd = currentTotalFlux * 0.20f;
			float hardFluxToAdd = currentTotalFlux * 0.20f;
			
			// Add soft flux (base + 20% transferred)
			mainTarget.getFluxTracker().increaseFlux(baseFlux + softFluxToAdd, false);
			// Add hard flux (20% transferred)
			mainTarget.getFluxTracker().increaseFlux(hardFluxToAdd, true);

			// 3. Disable & Lock Out Target Ship System for the 4.0s duration (does not disable phase cloak or shields)
			if (mainTarget.getSystem() != null) {
				ShipSystemAPI targetSys = mainTarget.getSystem();
				if (targetSys.isActive() || targetSys.isOn()) {
					targetSys.deactivate();
				}
				targetSys.forceState(SystemState.COOLDOWN, DEBUFF_DURATION);
				targetSys.setCooldownRemaining(Math.max(targetSys.getCooldownRemaining(), DEBUFF_DURATION));
			}

			// 4. Register EveryFrame plugin for the 4.0s debuff duration, cleanly recovering afterwards
			final String debuffId = "jp_interdictor_" + sourceShip.getId() + "_" + System.nanoTime();
			Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
				private float elapsed = 0f;
				
				@Override
				public void advance(float amount, List<InputEventAPI> events) {
					if (Global.getCombatEngine().isPaused()) return;
					elapsed += amount;
					
					// 4.0-second recovery: unmodify stats and REIGNITE ENGINES
					if (elapsed >= DEBUFF_DURATION || mainTarget == null || !mainTarget.isAlive()) {
						if (mainTarget != null) {
							if (mainTarget.getMutableStats() != null) {
								mainTarget.getMutableStats().getMaxSpeed().unmodify(debuffId);
								mainTarget.getMutableStats().getMaxTurnRate().unmodify(debuffId);
								mainTarget.getMutableStats().getTurnAcceleration().unmodify(debuffId);
								mainTarget.getMutableStats().getDeceleration().unmodify(debuffId);
								mainTarget.getMutableStats().getAcceleration().unmodify(debuffId);
							}
							// Explicitly repair and reignite all engine thrusters
							ShipEngineControllerAPI ec = mainTarget.getEngineController();
							if (ec != null) {
								for (ShipEngineAPI engine : ec.getShipEngines()) {
									engine.repair();
								}
								ec.computeEffectiveStats(mainTarget == Global.getCombatEngine().getPlayerShip());
							}
							
							// Explicitly repair PD weapons
							for (com.fs.starfarer.api.combat.WeaponAPI w : mainTarget.getAllWeapons()) {
								if (w.hasAIHint(com.fs.starfarer.api.combat.WeaponAPI.AIHints.PD) || w.hasAIHint(com.fs.starfarer.api.combat.WeaponAPI.AIHints.PD_ALSO)) {
									if (w.isDisabled()) {
										w.repair();
									}
								}
							}
						}
						
						for (ShipAPI friendly : Global.getCombatEngine().getShips()) {
							if (friendly != null && friendly.isAlive() && friendly.isDrone() && friendly.getOwner() == sourceShip.getOwner()) {
								if (friendly.getMutableStats() != null) {
									friendly.getMutableStats().getMaxSpeed().unmodify(debuffId);
									friendly.getMutableStats().getEnergyWeaponDamageMult().unmodify(debuffId);
									friendly.getMutableStats().getBallisticWeaponDamageMult().unmodify(debuffId);
								}
							}
						}
						
						if (mainTarget != null) {
							Color color = getEffectColor(mainTarget);
							color = Misc.setAlpha(color, 255);
							mainTarget.getFluxTracker().showOverloadFloatyIfNeeded("Ship systems restored", color, 4f, true);
						}
						
						Global.getCombatEngine().removePlugin(this);
						return;
					}
					
					// Agility suppression during 4.0s
					mainTarget.getMutableStats().getMaxSpeed().modifyMult(debuffId, 0.50f);
					mainTarget.getMutableStats().getMaxTurnRate().modifyMult(debuffId, 0.40f);
					mainTarget.getMutableStats().getTurnAcceleration().modifyMult(debuffId, 0.40f);
					mainTarget.getMutableStats().getDeceleration().modifyMult(debuffId, 0.50f);
					mainTarget.getMutableStats().getAcceleration().modifyMult(debuffId, 0.50f);
					
					// Lock target ship system during the 4.0s duration
					if (mainTarget.getSystem() != null && mainTarget.getSystem().getCooldownRemaining() < (DEBUFF_DURATION - elapsed)) {
						mainTarget.getSystem().setCooldownRemaining(DEBUFF_DURATION - elapsed);
					}
					
					// Phase Drone Frenzy during 3.0s
					for (ShipAPI friendly : Global.getCombatEngine().getShips()) {
						if (friendly != null && friendly.isAlive() && friendly.isDrone() && friendly.getOwner() == sourceShip.getOwner()) {
							if (friendly.getWing() != null && sourceShip.equals(friendly.getWing().getSourceShip())) {
								friendly.getMutableStats().getMaxSpeed().modifyMult(debuffId, 1.30f);
								friendly.getMutableStats().getEnergyWeaponDamageMult().modifyMult(debuffId, 1.15f);
								friendly.getMutableStats().getBallisticWeaponDamageMult().modifyMult(debuffId, 1.15f);
							}
						}
					}
					
					// Disable target PD weapons for 3.0s
					for (com.fs.starfarer.api.combat.WeaponAPI w : mainTarget.getAllWeapons()) {
						if (w.hasAIHint(com.fs.starfarer.api.combat.WeaponAPI.AIHints.PD) || w.hasAIHint(com.fs.starfarer.api.combat.WeaponAPI.AIHints.PD_ALSO)) {
							if (!w.isDisabled()) {
								w.disable(false);
							}
						}
					}

					// Maintain HUD debuff status for player ship with vanilla interdictor icon
					if (mainTarget == Global.getCombatEngine().getPlayerShip()) {
						Global.getCombatEngine().maintainStatusForPlayerShip(
							TARGET_KEY,
							"graphics/icons/hullsys/interdictor_array.png",
							"Ship systems interdicted",
							"engines and navigation disrupted",
							true
						);
					}
				}
			});
		}

		if (effectLevel > 0) {
			float jitterLevel = effectLevel;
			float maxRangeBonus = 20f + mainTarget.getCollisionRadius() * 0.25f;
			float jitterRangeBonus = jitterLevel * maxRangeBonus;
			if (state == State.OUT) {
				jitterRangeBonus = maxRangeBonus + (1f - jitterLevel) * maxRangeBonus;
			}
			mainTarget.setJitter(this,
					getEffectColor(mainTarget),
					jitterLevel, 6, 0f, jitterRangeBonus);

			ship.setJitter(this,
					getEffectColor(mainTarget),
					jitterLevel, 6, 0f, jitterRangeBonus);
		}

		if (ship == Global.getCombatEngine().getPlayerShip() && ship.getSystem() != null) {
			Global.getCombatEngine().maintainStatusForPlayerShip(
				SHIP_KEY,
				ship.getSystem().getSpecAPI().getIconSpriteName(),
				ship.getSystem().getDisplayName(),
				"interdiction field active",
				false
			);
		}
	}

	protected Color getEffectColor(ShipAPI ship) {
		if (ship.getEngineController().getShipEngines().isEmpty()) {
			return EFFECT_COLOR;
		}
		return Misc.setAlpha(ship.getEngineController().getShipEngines().get(0).getEngineColor(), EFFECT_COLOR.getAlpha());
	}
	
	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
	}
	
	protected ShipAPI findTarget(final ShipAPI ship) {
		FindShipFilter filter = new FindShipFilter() {
			public boolean matches(ShipAPI targetShip) {
				return targetShip != null && targetShip.isAlive() && !targetShip.getEngineController().isFlamedOut() && targetShip.getOwner() != ship.getOwner();
			}
		};
		
		float range = getMaxRange(ship);
		boolean player = ship == Global.getCombatEngine().getPlayerShip();
		ShipAPI target = ship.getShipTarget();
		if (target != null) {
			float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
			float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
			if (dist > range + radSum || !filter.matches(target)) {
				target = null;
			}
		}
		
		if (target == null) {
			if (player) {
				target = Misc.findClosestShipEnemyOf(ship, ship.getMouseTarget(), HullSize.FIGHTER, range, true, filter);
			} else {
				Object test = ship.getAIFlags().getCustom(AIFlags.MANEUVER_TARGET);
				if (test instanceof ShipAPI) {
					ShipAPI candidate = (ShipAPI) test;
					float dist = Misc.getDistance(ship.getLocation(), candidate.getLocation());
					float radSum = ship.getCollisionRadius() + candidate.getCollisionRadius();
					if (dist <= range + radSum && filter.matches(candidate)) {
						target = candidate;
					}
				}
			}
			if (target == null) {
				target = Misc.findClosestShipEnemyOf(ship, ship.getLocation(), HullSize.FIGHTER, range, true, filter);
			}
		}
		
		return target;
	}
	
	protected float getMaxRange(ShipAPI ship) {
		return RANGE;
	}

	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("interdiction field active: flux siphoned", false);
		} else if (index == 1) {
			return new StatusData("phase drone frenzy (+30% speed, +15% dmg)", false);
		}
		return null;
	}

	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.isOutOfAmmo()) return null;
		if (system.getState() != SystemState.IDLE) return null;
		
		ShipAPI target = findTarget(ship);
		if (target != null && target != ship) {
			return "READY";
		}
		if (target == null && ship.getShipTarget() != null) {
			return "OUT OF RANGE";
		}
		return "NO TARGET";
	}

	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		if (system.isActive()) return true;
		ShipAPI target = findTarget(ship);
		return target != null && target != ship;
	}
}
