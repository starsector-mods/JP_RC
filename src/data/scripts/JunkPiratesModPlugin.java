package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SectorThemeGenerator;
import data.scripts.campaign.fleets.JunkPiratesExplorerFleetManager;
import data.scripts.campaign.fleets.SyndicateAspFleetManager;
import data.scripts.campaign.fleets.SyndicateAspHitSquadFleetManager;
import data.scripts.campaign.procgen.themes.JunkPiratesAnarchistThemeGenerator;
// Import every entry from your mod's data/world/generators.csv
import data.scripts.world.JunkGen;
//import data.scripts.omnifac.AddOmniFac;

import org.dark.shaders.util.ShaderLib;
import org.dark.shaders.light.LightData;
import org.dark.shaders.util.TextureData;
import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;

public class JunkPiratesModPlugin extends BaseModPlugin
{

    public static final boolean isExerelin;
    
    public static boolean enableASP = true;
    public static boolean enableASPCourierFleets = true;
    public static boolean enableASPHitSquads = true;
    public static boolean enablePACK = true;
    public static boolean enablePACKDiplomats = true;
    public static boolean enableJunkPirates = true;
    public static boolean enableJunkExplorers = true;
    
    public static boolean enableJunkPiratesIntel = true;
    
    public static float junkPiratesFleetFrequencyModifier = 1.0f;
    public static float junkPiratesMaxFleetModifier = 1.0f;
    
//    
//    public static int minAnarchistConstellations;
//    public static int maxAnarchistConstellations;
//
//    public static int softMaxSpinerettes;
//
//    public static boolean enableProcGen;
//    public static boolean enableSpinerettes;
    
    static
    {
        isExerelin = Global.getSettings().getModManager().isModEnabled("nexerelin");
        
        // Force LunaLib to initialize its settings map early on the main thread (Thread-3).
        // This prevents a known race condition where the background script compiler (Thread-6)
        // triggers LunaSettingsLoader concurrently while Thread-3 is evaluating rules.csv,
        // which can cause dependent mods (like IndEvo) to crash with a NullPointerException.
        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            try {
                lunalib.lunaSettings.LunaSettings.getBoolean("junk_pirates_release", "enableASP");
            } catch (Throwable t) {
                // Ignore
            }
        }
    }
    
    
    private static void getProcGenSettings() {
    try {
        JSONObject settings = Global.getSettings().loadJSON("mendoncaModSettings.json");

            enableASP = settings.getBoolean("enableASP");
            enableASPCourierFleets = settings.getBoolean("enableASPCourierFleets");
            enableASPHitSquads = settings.getBoolean("enableASPHitSquads");
            enablePACK = settings.getBoolean("enablePACK");
            enablePACKDiplomats = settings.getBoolean("enablePACKDiplomats");
            enableJunkPirates = settings.getBoolean("enableJunkPirates");
            enableJunkExplorers = settings.getBoolean("enableJunkExplorers");
            
            enableJunkPiratesIntel = settings.getBoolean("enableJunkPiratesIntel");
            
            junkPiratesFleetFrequencyModifier = (float) settings.getDouble("junkPiratesFleetFrequencyModifier");
            junkPiratesMaxFleetModifier = (float) settings.getDouble("junkPiratesMaxFleetModifier");
  
        } catch (Exception ex) {
            System.out.println("JP Config Exception " + ex);
        }
        
        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            try {
                JunkPiratesLunaConfig.init();
            } catch (Throwable t) {
                Global.getLogger(JunkPiratesModPlugin.class).error("Failed to load LunaSettings for Junk Pirates", t);
            }
        }
    }
    
    private static void initJunkPirates() {
        new JunkGen().generate(Global.getSector());
    }
    private static void applyNoDecivFlags() {
        if (Global.getSector() == null || Global.getSector().getEconomy() == null) return;
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (market.getFactionId() != null) {
                String faction = market.getFactionId();
                if (faction.startsWith("junk_pirates") || faction.equals("pack") || faction.startsWith("syndicate_asp")) {
                    market.getMemoryWithoutUpdate().set("$core_noDeciv", true);
                }
            }
        }
    }
    
    @Override
    public void onNewGameAfterEconomyLoad() {
        // if (isExerelin)
        // {
        //    // JP_NexIntegration.onNewGameAfterEconomyLoad();
        // }
        
        applyNoDecivFlags();
        
        if (enableASPCourierFleets) {
            Global.getSector().addScript(new SyndicateAspFleetManager());
        }
        
        if (enableASPCourierFleets && enableASPHitSquads) {
            Global.getSector().getMemoryWithoutUpdate().set("$playerIsSyndicateAspWanted", false);
            Global.getSector().addScript(new SyndicateAspHitSquadFleetManager());
        }
        
        if (enableJunkExplorers) {
            Global.getSector().addScript(new JunkPiratesExplorerFleetManager());
        }
        
        Global.getSector().addScript(new data.scripts.campaign.SpineretteRespawnManager());
                         
    }
    @Override
    public void onGameLoad(boolean newGame) {
        getProcGenSettings();
        applyNoDecivFlags();
        
        if (!Global.getSector().hasScript(data.scripts.campaign.SpineretteRespawnManager.class)) {
            Global.getSector().addScript(new data.scripts.campaign.SpineretteRespawnManager());
        }
        if (!enableASPCourierFleets) {
            Global.getSector().removeScriptsOfClass(SyndicateAspFleetManager.class);
        } else if (!Global.getSector().hasScript(SyndicateAspFleetManager.class)) {
            Global.getSector().addScript(new SyndicateAspFleetManager());
        }
        if (!enableASPCourierFleets || !enableASPHitSquads) {
            Global.getSector().removeScriptsOfClass(SyndicateAspHitSquadFleetManager.class);
        } else if (!Global.getSector().hasScript(SyndicateAspHitSquadFleetManager.class)) {
            Global.getSector().addScript(new SyndicateAspHitSquadFleetManager());
        }
        if (!enableJunkExplorers) {
            Global.getSector().removeScriptsOfClass(JunkPiratesExplorerFleetManager.class);
        } else if (!Global.getSector().hasScript(JunkPiratesExplorerFleetManager.class)) {
            Global.getSector().addScript(new JunkPiratesExplorerFleetManager());
        }

        retrofitHypercube();
        retrofitYork();

        if (com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager.getInstance() != null) {
            if (!com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager.getInstance().hasEventCreator(data.campaign.intel.bar.YorkBarEventCreator.class)) {
                com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager.getInstance().addEventCreator(new data.campaign.intel.bar.YorkBarEventCreator());
            }
        }
    }

    private static void retrofitYork() {
        if (Global.getSector() == null) return;
        com.fs.starfarer.api.campaign.StarSystemAPI york = Global.getSector().getStarSystem("York");
        if (york == null) {
            new data.scripts.world.systems.York().generate(Global.getSector());
        } else {
            data.scripts.world.systems.York.spawnRemnants(york);
        }
    }

    private static void retrofitHypercube() {
        if (Global.getSector() == null) return;
        boolean found = false;
        for (com.fs.starfarer.api.campaign.StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (system == null) continue;
            for (com.fs.starfarer.api.campaign.SectorEntityToken entity : system.getCustomEntities()) {
                if (entity == null) continue;
                if ("junk_pirates_hypercube".equals(entity.getCustomEntityType())) {
                    found = true;
                    if (!entity.hasTag("has_interaction_dialog")) {
                        entity.addTag("has_interaction_dialog");
                    }
                    if (!entity.hasTag("non_expiring")) {
                        entity.addTag("non_expiring");
                    }
                }
            }
        }
        if (!found) {
            com.fs.starfarer.api.campaign.StarSystemAPI brehinni = Global.getSector().getStarSystem("Brehinni");
            if (brehinni != null) {
                com.fs.starfarer.api.campaign.SectorEntityToken swanage = brehinni.getEntityById("swanage");
                if (swanage != null) {
                    com.fs.starfarer.api.campaign.SectorEntityToken hypercube = brehinni.addCustomEntity("hypercube", "Hypercube", "junk_pirates_hypercube", "junk_pirates");
                    hypercube.setCircularOrbitPointingDown(swanage, 270, 1000, 45);
                    hypercube.addTag("has_interaction_dialog");
                    hypercube.addTag("non_expiring");
                }
            }
        }
    }
    @Override
    public void onNewGame() {
        if (isExerelin && !JP_NexIntegration.isCorvusMode())
            {
                return;
            }
        initJunkPirates();
    }
    
//    @Override
//    public void onNewGame()
//    {
//        // Calling a separate method avoids duplicate code with onEnabled()
//        initJunkPirates();
//    }

    @Override  
    public void onApplicationLoad()
    {  
        if (SectorThemeGenerator.generators != null) {
            boolean hasGenerator = false;
            for (com.fs.starfarer.api.impl.campaign.procgen.themes.ThemeGenerator tg : SectorThemeGenerator.generators) {
                if (tg instanceof JunkPiratesAnarchistThemeGenerator) {
                    hasGenerator = true;
                    break;
                }
            }
            if (!hasGenerator) {
                SectorThemeGenerator.generators.add(new JunkPiratesAnarchistThemeGenerator());
            }
        }
        
        getProcGenSettings();
        
        ShaderLib.init();  
        LightData.readLightDataCSV("data/lights/junk_pirates_light_data.csv");  
        TextureData.readTextureDataCSV("data/lights/junk_pirates_texture_data.csv");  
    }
}