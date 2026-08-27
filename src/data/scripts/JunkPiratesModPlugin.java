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
    
    public static boolean enableASP;
    public static boolean enableASPCourierFleets;
    public static boolean enableASPHitSquads;
    public static boolean enablePACK;
    public static boolean enablePACKDiplomats;
    public static boolean enableJunkPirates;
    public static boolean enableJunkExplorers;
    
    public static boolean enableJunkPiratesIntel;
    
    public static float junkPiratesFleetFrequencyModifier;
    public static float junkPiratesMaxFleetModifier;
    
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
  
        } catch (IOException | JSONException ex) {
            System.out.println("JP Config Exception " + ex);
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
        applyNoDecivFlags();
        
        if (!Global.getSector().hasScript(data.scripts.campaign.SpineretteRespawnManager.class)) {
            Global.getSector().addScript(new data.scripts.campaign.SpineretteRespawnManager());
        }
        if (enableASPCourierFleets && !Global.getSector().hasScript(SyndicateAspFleetManager.class)) {
            Global.getSector().addScript(new SyndicateAspFleetManager());
        }
        if (enableASPCourierFleets && enableASPHitSquads && !Global.getSector().hasScript(SyndicateAspHitSquadFleetManager.class)) {
            Global.getSector().addScript(new SyndicateAspHitSquadFleetManager());
        }
        if (enableJunkExplorers && !Global.getSector().hasScript(JunkPiratesExplorerFleetManager.class)) {
            Global.getSector().addScript(new JunkPiratesExplorerFleetManager());
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
    
        SectorThemeGenerator.generators.add(1, new JunkPiratesAnarchistThemeGenerator());
        
        getProcGenSettings();
        
        ShaderLib.init();  
        LightData.readLightDataCSV("data/lights/junk_pirates_light_data.csv");  
        TextureData.readTextureDataCSV("data/lights/junk_pirates_texture_data.csv");  
    }
}