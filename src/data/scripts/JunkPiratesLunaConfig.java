package data.scripts;

import com.fs.starfarer.api.Global;
import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;
import data.scripts.campaign.procgen.themes.JunkPiratesAnarchistThemeGenerator;
import data.scripts.campaign.fleets.SyndicateAspFleetManager;
import data.scripts.campaign.fleets.SyndicateAspHitSquadFleetManager;
import data.scripts.campaign.fleets.JunkPiratesExplorerFleetManager;

public class JunkPiratesLunaConfig implements LunaSettingsListener {
    public static final String MOD_ID = "junk_pirates_release";

    public static void init() {
        try {
            if (!LunaSettings.hasSettingsListenerOfClass(JunkPiratesLunaConfig.class)) {
                LunaSettings.addSettingsListener(new JunkPiratesLunaConfig());
            }
        } catch (Throwable t) {
            Global.getLogger(JunkPiratesLunaConfig.class).error("Failed to register LunaSettingsListener", t);
        }
        loadSettings();
    }

    @Override
    public void settingsChanged(String modID) {
        if (MOD_ID.equals(modID)) {
            loadSettings();
            applyInGameSettings();
        }
    }

    public static void applyInGameSettings() {
        if (Global.getSector() == null) return;

        // Courier Fleets
        if (!JunkPiratesModPlugin.enableASPCourierFleets) {
            Global.getSector().removeScriptsOfClass(SyndicateAspFleetManager.class);
        } else if (!Global.getSector().hasScript(SyndicateAspFleetManager.class)) {
            Global.getSector().addScript(new SyndicateAspFleetManager());
        }

        // Hit Squads
        if (!JunkPiratesModPlugin.enableASPCourierFleets || !JunkPiratesModPlugin.enableASPHitSquads) {
            Global.getSector().removeScriptsOfClass(SyndicateAspHitSquadFleetManager.class);
        } else if (!Global.getSector().hasScript(SyndicateAspHitSquadFleetManager.class)) {
            Global.getSector().addScript(new SyndicateAspHitSquadFleetManager());
        }

        // Explorers
        if (!JunkPiratesModPlugin.enableJunkExplorers) {
            Global.getSector().removeScriptsOfClass(JunkPiratesExplorerFleetManager.class);
        } else if (!Global.getSector().hasScript(JunkPiratesExplorerFleetManager.class)) {
            Global.getSector().addScript(new JunkPiratesExplorerFleetManager());
        }
    }

    public static void loadSettings() {
        Boolean enableASP = LunaSettings.getBoolean(MOD_ID, "enableASP");
        if (enableASP != null) JunkPiratesModPlugin.enableASP = enableASP;

        Boolean enableASPCourierFleets = LunaSettings.getBoolean(MOD_ID, "enableASPCourierFleets");
        if (enableASPCourierFleets != null) JunkPiratesModPlugin.enableASPCourierFleets = enableASPCourierFleets;

        Boolean enableASPHitSquads = LunaSettings.getBoolean(MOD_ID, "enableASPHitSquads");
        if (enableASPHitSquads != null) JunkPiratesModPlugin.enableASPHitSquads = enableASPHitSquads;

        Boolean enablePACK = LunaSettings.getBoolean(MOD_ID, "enablePACK");
        if (enablePACK != null) JunkPiratesModPlugin.enablePACK = enablePACK;

        Boolean enableJunkPirates = LunaSettings.getBoolean(MOD_ID, "enableJunkPirates");
        if (enableJunkPirates != null) JunkPiratesModPlugin.enableJunkPirates = enableJunkPirates;

        Boolean enableJunkExplorers = LunaSettings.getBoolean(MOD_ID, "enableJunkExplorers");
        if (enableJunkExplorers != null) JunkPiratesModPlugin.enableJunkExplorers = enableJunkExplorers;

        Boolean enableJunkPiratesIntel = LunaSettings.getBoolean(MOD_ID, "enableJunkPiratesIntel");
        if (enableJunkPiratesIntel != null) JunkPiratesModPlugin.enableJunkPiratesIntel = enableJunkPiratesIntel;

        Float freqMod = LunaSettings.getFloat(MOD_ID, "junkPiratesFleetFrequencyModifier");
        if (freqMod != null) JunkPiratesModPlugin.junkPiratesFleetFrequencyModifier = freqMod;

        Float maxMod = LunaSettings.getFloat(MOD_ID, "junkPiratesMaxFleetModifier");
        if (maxMod != null) JunkPiratesModPlugin.junkPiratesMaxFleetModifier = maxMod;
    }

    public static void loadProcGenSettings(JunkPiratesAnarchistThemeGenerator generator) {
        Boolean enableProcGen = LunaSettings.getBoolean(MOD_ID, "enableProcGen");
        if (enableProcGen != null) generator.enableProcGen = enableProcGen;

        Boolean enableSpinerettes = LunaSettings.getBoolean(MOD_ID, "enableSpinerettes");
        if (enableSpinerettes != null) generator.enableSpinerettes = enableSpinerettes;

        Float skipProb = LunaSettings.getFloat(MOD_ID, "skipProbability");
        if (skipProb != null) generator.skipProbability = skipProb;

        Integer minAnarch = LunaSettings.getInt(MOD_ID, "minAnarchistConstellations");
        if (minAnarch != null) generator.minAnarchistConstellations = minAnarch;

        Integer maxAnarch = LunaSettings.getInt(MOD_ID, "maxAnarchistConstellations");
        if (maxAnarch != null) generator.maxAnarchistConstellations = maxAnarch;

        Integer softMax = LunaSettings.getInt(MOD_ID, "softMaxSpinerettes");
        if (softMax != null) generator.softMaxSpinerettes = softMax;
    }
}
