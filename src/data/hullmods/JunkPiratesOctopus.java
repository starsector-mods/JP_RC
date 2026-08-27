/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import java.util.HashSet;
import java.util.Set;
/**
 *
 * @author paul
 */
public class JunkPiratesOctopus extends BaseHullMod {

        private static final Set<String> BLOCKED_HULLMODS = new HashSet<>(7);
        
        static {
            BLOCKED_HULLMODS.add("frontshield");
            BLOCKED_HULLMODS.add("frontemitter");
            BLOCKED_HULLMODS.add("adaptiveshields");
            BLOCKED_HULLMODS.add("advancedshieldemitter");
            BLOCKED_HULLMODS.add("hardenedshieldemitter");
            BLOCKED_HULLMODS.add("stabilizedshieldemitter");
            BLOCKED_HULLMODS.add("extendedshieldemitter");
        }

        @Override
        public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
            for (String tmp : BLOCKED_HULLMODS) {
                if (ship.getVariant().getHullMods().contains(tmp)) {
                    ship.getVariant().removeMod(tmp);
                }
            }
        }
}
