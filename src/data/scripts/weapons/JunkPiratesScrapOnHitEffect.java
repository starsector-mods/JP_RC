package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import static data.scripts.plugins.JunkPiratesElectroChaffAndJunkjetPlugin.JP_DEBUG;
import data.scripts.plugins.JunkPiratesElectroChaffAndJunkjetPlugin;
import static data.scripts.plugins.JunkPiratesElectroChaffAndJunkjetPlugin.log;
import java.awt.Color;
import org.lwjgl.util.vector.Vector2f;

public class JunkPiratesScrapOnHitEffect implements OnHitEffectPlugin {

    private static final Color EXPLOSION_COLOR = new Color(185, 255, 200, 255); // RGBA value
    private static final float EXPLOSION_SIZE = 35f; // in pixels, i think
    private static final float EXPLOSION_DURATION = 0.10f; // in seconds

    private static final String SOUND_ID = "hit_glancing_energy"; // needs to be a sound ID declared in sounds.json
    private static final float SOUND_PITCH = 1.3f;
    private static final float SOUND_VOL = 0.5f;

    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {

        WeaponAPI weapon = projectile.getWeapon();

        if (target == null) {
            if (JP_DEBUG) {
                log.info("scrapgun hit null target, returning");
            }
            return;
        }

        // spawn the chaff particles
        int chaff = JunkPiratesElectroChaffAndJunkjetPlugin.spawnChaff(projectile.getProjectileSpecId(), weapon, point, target, shieldHit);

        // check if we spawned any chaff
        if (chaff > 0) {

            // if we did, spawn an explosion
            Global.getCombatEngine().spawnExplosion(point, new Vector2f(0f, 0f), EXPLOSION_COLOR, EXPLOSION_SIZE, EXPLOSION_DURATION);

            // play explosion sound
            Global.getSoundPlayer().playSound(SOUND_ID, SOUND_PITCH, SOUND_VOL, point, new Vector2f(0f, 0f));
        }
    }
}
