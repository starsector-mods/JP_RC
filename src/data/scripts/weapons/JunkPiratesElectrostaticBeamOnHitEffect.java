package data.scripts.weapons;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import static data.scripts.plugins.JunkPiratesElectroChaffAndJunkjetPlugin.JP_DEBUG;
import data.scripts.plugins.JunkPiratesElectroChaffAndJunkjetPlugin;
import java.util.List;
import org.lazywizard.lazylib.combat.CombatUtils;
import static data.scripts.plugins.JunkPiratesElectroChaffAndJunkjetPlugin.EMP_ARC_RANGE;
import static data.scripts.plugins.JunkPiratesElectroChaffAndJunkjetPlugin.log;
import org.lwjgl.util.vector.Vector2f;

public class JunkPiratesElectrostaticBeamOnHitEffect implements BeamEffectPlugin {

    // amount to reduce burst length by for calculating crit interval, and increase it by for calculating crit chance
    private static final float TIME_MOD = 0.05f;

    private IntervalUtil fireInterval = null;

    private boolean wasZero = true;

    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        CombatEntityAPI target = beam.getDamageTarget();

        // initialize crit stuff
        float burstLength = beam.getWeapon().getSpec().getDerivedStats().getBurstFireDuration();
        float intervalLength = burstLength - TIME_MOD;
        float critChance = burstLength + TIME_MOD;

        // initialize fire interval for THIS SPECIFIC BEAM/WEAPON
        if (fireInterval == null) {
            fireInterval = new IntervalUtil(intervalLength, intervalLength);
            if (JP_DEBUG) {
                log.info(String.format("initialized fireInterval of [%s] seconds with [%s] critChance for [%s]", intervalLength, critChance, beam.getWeapon()));
            }
        }

        // don't trigger on hits to things that aren't ships, or hits to shield
        if (target instanceof ShipAPI && beam.getBrightness() >= 1f) {
            ShipAPI targetShip = (ShipAPI) target;
            boolean shieldHit = targetShip.getShield() != null && targetShip.getShield().isWithinArc(beam.getTo());

            if (!shieldHit) {
                // needed because when the ship is in fast-time, dpsDuration will not be reset every frame as it should be
                float dur = beam.getDamage().getDpsDuration();
                if (!wasZero) {
                    dur = 0;
                }
                wasZero = beam.getDamage().getDpsDuration() <= 0;
                fireInterval.advance(dur);

                // trigger every <interval> seconds
                // don't trigger if we're hitting a hulk (don't wanna waste chaff)
                if (fireInterval.intervalElapsed() && !targetShip.isHulk() && Math.random() < critChance) {
                    // spawn own pathetic arc
                    Vector2f dir = Vector2f.sub(beam.getTo(), beam.getFrom(), new Vector2f());
                    if (dir.lengthSquared() > 0) {
                        dir.normalise();
                    }
                    dir.scale(50f);
                    Vector2f point = Vector2f.sub(beam.getTo(), dir, new Vector2f());
                    float emp = beam.getDamage().getFluxComponent() * 2f;
                    float dam = beam.getDamage().getDamage() * 0.5f;
                    engine.spawnEmpArc(
                            beam.getSource(), point, beam.getDamageTarget(), beam.getDamageTarget(),
                            DamageType.ENERGY,
                            dam, // damage
                            emp, // emp 
                            69420f, // max range 
                            "tachyon_lance_emp_impact",
                            beam.getWidth() * 2f,
                            beam.getFringeColor(),
                            beam.getCoreColor());

                    // consume previously existing chaff particles for bonus arcs
                    if (JP_DEBUG) {
                        log.info("electrobeam hit nonhulk ship, triggering chaff");
                    }
                    List<MissileAPI> missiles = CombatUtils.getMissilesWithinRange(target.getLocation(), target.getCollisionRadius() + EMP_ARC_RANGE);
                    for (MissileAPI missile : missiles) {
                        if (JunkPiratesElectroChaffAndJunkjetPlugin.getChaff().contains(missile) && !JunkPiratesElectroChaffAndJunkjetPlugin.getChaffSpent().contains(missile)) {
                            JunkPiratesElectroChaffAndJunkjetPlugin.expendChaff(missile, target, false);
                        }
                    }
                }
            }
        }
    }
}
