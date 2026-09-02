package data.scripts.weapons;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import java.awt.Color;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class PackAffenpinscherBeamEffect implements BeamEffectPlugin {

    private static final String TUMBLE_TAG = "pack_affenpinscher_tumbled";
    private static final String HP_TAG = "pack_affenpinscher_hp";
    private static final Color SPARK_COLOR = new Color(255, 175, 90, 255);
    private static final Color JITTER_COLOR = new Color(255, 120, 50, 200);
    private static final float TUMBLE_CHANCE = 0.50f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        if (engine == null || engine.isPaused() || beam == null) {
            return;
        }

        CombatEntityAPI target = beam.getDamageTarget();
        if (target == null) {
            return;
        }

        // Target is a Missile
        if (target instanceof MissileAPI) {
            MissileAPI missile = (MissileAPI) target;

            if (missile.getCustomData() != null) {
                // Initial roll for this missile
                if (!missile.getCustomData().containsKey(TUMBLE_TAG)) {
                    if (Math.random() < TUMBLE_CHANCE) {
                        missile.setCustomData(TUMBLE_TAG, Boolean.TRUE);
                        missile.setCustomData(HP_TAG, missile.getHitpoints());

                        // 1. Permanently flame out the missile engine and break contrail
                        missile.flameOut();
                        missile.interruptContrail();

                        // 2. Set owner to neutral (100) so the Affenpinscher and all PD immediately drop target lock
                        missile.setOwner(100);

                        // 3. Tumble spin (300 - 600 deg/sec)
                        float spinSign = (Math.random() < 0.5f) ? -1.0f : 1.0f;
                        float spinRate = spinSign * (300f + (float) (Math.random() * 300f));

                        // 4. Replace guidance AI with tumbling AI
                        missile.setMissileAI(new TumblingMissileAI(missile, spinRate));
                        missile.setAngularVelocity(spinRate);

                        // 5. Apply strong lateral deflection velocity kick
                        Vector2f beamDir = VectorUtils.getDirectionalVector(beam.getFrom(), missile.getLocation());
                        Vector2f lateralKick = new Vector2f(-beamDir.y * spinSign, beamDir.x * spinSign);
                        float kickSpeed = 130f + (float) (Math.random() * 100f);
                        lateralKick.scale(kickSpeed);
                        Vector2f.add(missile.getVelocity(), lateralKick, missile.getVelocity());

                        // 6. Visual feedback
                        missile.setJitter(beam.getWeapon(), JITTER_COLOR, 0.5f, 4, 6f);
                        engine.addHitParticle(
                                missile.getLocation(),
                                missile.getVelocity(),
                                35f + (float) Math.random() * 15f,
                                1.2f,
                                0.25f,
                                SPARK_COLOR
                        );
                    } else {
                        // Roll failed: regular beam damage applies (no tumble)
                        missile.setCustomData(TUMBLE_TAG, Boolean.FALSE);
                    }
                }

                // If this missile was tumbled, ensure 0 damage is taken so it tumbles away intact
                if (Boolean.TRUE.equals(missile.getCustomData().get(TUMBLE_TAG))) {
                    Object savedHpObj = missile.getCustomData().get(HP_TAG);
                    if (savedHpObj instanceof Float) {
                        float savedHp = (Float) savedHpObj;
                        missile.setHitpoints(savedHp);
                    }
                }
            }
        }
    }

    /**
     * Replaces the missile's guidance system with an unguided tumbling drift.
     */
    public static class TumblingMissileAI implements MissileAIPlugin {
        private final MissileAPI missile;
        private final float spinRate;

        public TumblingMissileAI(MissileAPI missile, float spinRate) {
            this.missile = missile;
            this.spinRate = spinRate;
        }

        @Override
        public void advance(float amount) {
            if (missile == null || missile.isExpired()) {
                return;
            }
            missile.setAngularVelocity(spinRate);
        }
    }
}
