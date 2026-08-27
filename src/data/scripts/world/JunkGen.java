package data.scripts.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorGeneratorPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import static data.scripts.JunkPiratesModPlugin.enableASP;
import static data.scripts.JunkPiratesModPlugin.enableJunkPirates;
import static data.scripts.JunkPiratesModPlugin.enablePACK;

import data.scripts.world.systems.Brehinni;
import data.scripts.world.systems.Canis;
//import data.scripts.world.systems.York;
import data.scripts.world.systems.Ursulo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JunkGen implements SectorGeneratorPlugin{
      
    public void generate(SectorAPI sector) { 

    SharedData.getData().getPersonBountyEventData().addParticipatingFaction("pack");  
    SharedData.getData().getPersonBountyEventData().addParticipatingFaction("syndicate_asp");
 
    
//            System.out.println("enableProcGen: " + enableProcGen);
//            System.out.println("minAnarchistConstellations: " + minAnarchistConstellations);
            System.out.println("enablePACK: " + enablePACK);
            System.out.println("enableJunkPirates: " + enableJunkPirates);    
    
    if (enableJunkPirates) {
        new Brehinni().generate(sector);
    }
    if (enablePACK) {
        new Canis().generate(sector);
    }
    //new York().generate(sector);
    if (enableASP) {
        new Ursulo().generate(sector);
    }


    
    initFactionRelationships(sector);
    
    

    }
    
    private static ArrayList<String> makeArrayList(JSONArray list) {
            
            ArrayList<String> newlist = new ArrayList<>();
            
            try {
                for (int i=0; i<list.length(); i++)
                {
                        newlist.add(list.getString(i));
                }
                        return newlist;
                        
                        } catch (Exception ex) { }
            
            return null;
    }
    
    public static void initFactionRelationships(SectorAPI sector) {
        
        // Load the faction relationships from the JSON
        
                JSONArray VengefulPack = new JSONArray();
                JSONArray HostilePack = new JSONArray();
                JSONArray InhospitablePack = new JSONArray();
                JSONArray SuspiciousPack = new JSONArray();
                JSONArray NeutralPack = new JSONArray();
                JSONArray FavorablePack = new JSONArray();
                JSONArray WelcomingPack = new JSONArray();
                JSONArray FriendlyPack = new JSONArray();
                JSONArray CooperativePack = new JSONArray();

                JSONArray VengefulASP = new JSONArray();
                JSONArray HostileASP = new JSONArray();
                JSONArray InhospitableASP = new JSONArray();
                JSONArray SuspiciousASP = new JSONArray();
                JSONArray NeutralASP = new JSONArray();
                JSONArray FavorableASP = new JSONArray();
                JSONArray WelcomingASP = new JSONArray();
                JSONArray FriendlyASP = new JSONArray();
                JSONArray CooperativeASP = new JSONArray();

                JSONArray VengefulJunk = new JSONArray();
                JSONArray HostileJunk = new JSONArray();
                JSONArray InhospitableJunk = new JSONArray();
                JSONArray SuspiciousJunk = new JSONArray();
                JSONArray NeutralJunk = new JSONArray();
                JSONArray FavorableJunk = new JSONArray();
                JSONArray WelcomingJunk = new JSONArray();
                JSONArray FriendlyJunk = new JSONArray();
                JSONArray CooperativeJunk = new JSONArray();
        
            try {
                JSONObject factionrels = Global.getSettings().loadJSON("data/config/jpConfig/junk_pirates_Relations.json");

                VengefulPack = factionrels.getJSONArray("VengefulPack");
                HostilePack = factionrels.getJSONArray("HostilePack");
                InhospitablePack = factionrels.getJSONArray("InhospitablePack");
                SuspiciousPack = factionrels.getJSONArray("SuspiciousPack");
                NeutralPack = factionrels.getJSONArray("NeutralPack");
                FavorablePack = factionrels.getJSONArray("FavorablePack");
                WelcomingPack = factionrels.getJSONArray("WelcomingPack");
                FriendlyPack = factionrels.getJSONArray("FriendlyPack");
                CooperativePack = factionrels.getJSONArray("CooperativePack");

                VengefulASP = factionrels.getJSONArray("VengefulASP");
                HostileASP = factionrels.getJSONArray("HostileASP");
                InhospitableASP = factionrels.getJSONArray("InhospitableASP");
                SuspiciousASP = factionrels.getJSONArray("SuspiciousASP");
                NeutralASP = factionrels.getJSONArray("NeutralASP");
                FavorableASP = factionrels.getJSONArray("FavorableASP");
                WelcomingASP = factionrels.getJSONArray("WelcomingASP");
                FriendlyASP = factionrels.getJSONArray("FriendlyASP");
                CooperativeASP = factionrels.getJSONArray("CooperativeASP");

                VengefulJunk = factionrels.getJSONArray("VengefulJunk");
                HostileJunk = factionrels.getJSONArray("HostileJunk");
                InhospitableJunk = factionrels.getJSONArray("InhospitableJunk");
                SuspiciousJunk = factionrels.getJSONArray("SuspiciousJunk");
                NeutralJunk = factionrels.getJSONArray("NeutralJunk");
                FavorableJunk = factionrels.getJSONArray("FavorableJunk");
                WelcomingJunk = factionrels.getJSONArray("WelcomingJunk");
                FriendlyJunk = factionrels.getJSONArray("FriendlyJunk");
                CooperativeJunk = factionrels.getJSONArray("CooperativeJunk");

                } catch (IOException | JSONException ex) {
                    System.out.println("JP Faction Init Exception " + ex);
                }
            
            
        // Vanilla Factions and this mod

        FactionAPI hegemony = sector.getFaction(Factions.HEGEMONY);
        FactionAPI tritachyon = sector.getFaction(Factions.TRITACHYON);
        FactionAPI pirates = sector.getFaction(Factions.PIRATES);
        FactionAPI independent = sector.getFaction(Factions.INDEPENDENT);
        FactionAPI kol = sector.getFaction(Factions.KOL);
        FactionAPI church = sector.getFaction(Factions.LUDDIC_CHURCH);
        FactionAPI path = sector.getFaction(Factions.LUDDIC_PATH);
        FactionAPI player = sector.getFaction(Factions.PLAYER);
        FactionAPI diktat = sector.getFaction(Factions.DIKTAT);
        FactionAPI junk = sector.getFaction("junk_pirates");
        FactionAPI pack = sector.getFaction("pack");
        FactionAPI asp = sector.getFaction("syndicate_asp");
        FactionAPI junkboys = sector.getFaction("junk_pirates_junkboys");
        FactionAPI technicians = sector.getFaction("junk_pirates_technicians");
        FactionAPI hounds = sector.getFaction("junk_pirates_hounds");
        FactionAPI league = sector.getFaction(Factions.PERSEAN);
        FactionAPI remnants = sector.getFaction(Factions.REMNANTS);


        // set up relations - PACK to others
        
        for (Object faction : makeArrayList(VengefulPack)) {
            if (Global.getSector().getFaction((String) faction) != null) {  
                pack.setRelationship((String) faction, RepLevel.VENGEFUL);
            }
        }

        for (Object faction : makeArrayList(HostilePack)) {
            if (Global.getSector().getFaction((String) faction) != null) {  
                pack.setRelationship((String) faction, RepLevel.HOSTILE);
            }
        }

        for (Object faction : makeArrayList(InhospitablePack)) {
            if (Global.getSector().getFaction((String) faction) != null) {  
                pack.setRelationship((String) faction, RepLevel.INHOSPITABLE);
            }
        }

        for (Object faction : makeArrayList(SuspiciousPack)) {
            if (Global.getSector().getFaction((String) faction) != null) {  
                pack.setRelationship((String) faction, RepLevel.SUSPICIOUS);
            }
        }

        for (Object faction : makeArrayList(NeutralPack)) {
            if (Global.getSector().getFaction((String) faction) != null) {  
                pack.setRelationship((String) faction, RepLevel.NEUTRAL);
            }
        }

        for (Object faction : makeArrayList(FavorablePack)) {
            if (Global.getSector().getFaction((String) faction) != null) {  
                pack.setRelationship((String) faction, RepLevel.FAVORABLE);
            }
        }

        for (Object faction : makeArrayList(FriendlyPack)) {
            if (Global.getSector().getFaction((String) faction) != null) {  
                pack.setRelationship((String) faction, RepLevel.WELCOMING);
            }
        }

        for (Object faction : makeArrayList(WelcomingPack)) {
            if (Global.getSector().getFaction((String) faction) != null) {  
                pack.setRelationship((String) faction, RepLevel.FRIENDLY);
            }
        }

        for (Object faction : makeArrayList(CooperativePack)) {
            if (Global.getSector().getFaction((String) faction) != null) {  
                pack.setRelationship((String) faction, RepLevel.COOPERATIVE);
            }
        }

        // JUNK PIRATES relations
        
        for (Object faction : makeArrayList(VengefulJunk)) {
            if (Global.getSector().getFaction((String) faction) != null) {  
                junk.setRelationship((String) faction, RepLevel.VENGEFUL);
            }
        }

        for (Object faction : makeArrayList(HostileJunk)) {
            if (Global.getSector().getFaction((String) faction) != null) {  
                junk.setRelationship((String) faction, RepLevel.HOSTILE);
            }
        }

        for (Object faction : makeArrayList(InhospitableJunk)) {
            if (Global.getSector().getFaction((String) faction) != null) {  
                junk.setRelationship((String) faction, RepLevel.INHOSPITABLE);
            }
        }

        for (Object faction : makeArrayList(SuspiciousJunk)) {
            if (Global.getSector().getFaction((String) faction) != null) {  
                junk.setRelationship((String) faction, RepLevel.SUSPICIOUS);
            }
        }

        for (Object faction : makeArrayList(NeutralJunk)) {
            if (Global.getSector().getFaction((String) faction) != null) {  
                junk.setRelationship((String) faction, RepLevel.NEUTRAL);
            }
        }

        for (Object faction : makeArrayList(FavorableJunk)) {
            if (Global.getSector().getFaction((String) faction) != null) {  
                junk.setRelationship((String) faction, RepLevel.FAVORABLE);
            }
        }

        for (Object faction : makeArrayList(WelcomingJunk)) {
            if (Global.getSector().getFaction((String) faction) != null) {  
                junk.setRelationship((String) faction, RepLevel.WELCOMING);
            }
        }

        for (Object faction : makeArrayList(FriendlyJunk)) {
            if (Global.getSector().getFaction((String) faction) != null) {  
                junk.setRelationship((String) faction, RepLevel.FRIENDLY);
            }
        }

        for (Object faction : makeArrayList(CooperativeJunk)) {
            if (Global.getSector().getFaction((String) faction) != null) {  
                junk.setRelationship((String) faction, RepLevel.COOPERATIVE);
            }
        }
        
        // ASP Relations
        
        for (Object faction : makeArrayList(VengefulASP)) {
            if (Global.getSector().getFaction((String) faction) != null) {  
                asp.setRelationship((String) faction, RepLevel.VENGEFUL);
            }
        }

        for (Object faction : makeArrayList(HostileASP)) {
            if (Global.getSector().getFaction((String) faction) != null) {  
                asp.setRelationship((String) faction, RepLevel.HOSTILE);
            }
        }

        for (Object faction : makeArrayList(InhospitableASP)) {
            if (Global.getSector().getFaction((String) faction) != null) {  
                asp.setRelationship((String) faction, RepLevel.INHOSPITABLE);
            }
        }

        for (Object faction : makeArrayList(SuspiciousASP)) {
            if (Global.getSector().getFaction((String) faction) != null) {  
                asp.setRelationship((String) faction, RepLevel.SUSPICIOUS);
            }
        }

        for (Object faction : makeArrayList(NeutralASP)) {
            if (Global.getSector().getFaction((String) faction) != null) {  
                asp.setRelationship((String) faction, RepLevel.NEUTRAL);
            }
        }

        for (Object faction : makeArrayList(FavorableASP)) {
            if (Global.getSector().getFaction((String) faction) != null) {  
                asp.setRelationship((String) faction, RepLevel.FAVORABLE);
            }
        }

        for (Object faction : makeArrayList(WelcomingASP)) {
            if (Global.getSector().getFaction((String) faction) != null) {  
                asp.setRelationship((String) faction, RepLevel.WELCOMING);
            }
        }

        for (Object faction : makeArrayList(FriendlyASP)) {
            if (Global.getSector().getFaction((String) faction) != null) {  
                asp.setRelationship((String) faction, RepLevel.FRIENDLY);
            }
        }

        for (Object faction : makeArrayList(CooperativeASP)) {
            if (Global.getSector().getFaction((String) faction) != null) {  
                asp.setRelationship((String) faction, RepLevel.COOPERATIVE);
            }
        }


        
        // PLAYER STARTING relations to MOD Factions
        
        player.setRelationship(junk.getId(), 0);
        player.setRelationship(pack.getId(), 0);
        player.setRelationship(asp.getId(), 0);
        
        // Vanilla and this mod
        junk.setRelationship(hegemony.getId(), RepLevel.HOSTILE);
        junk.setRelationship(path.getId(), RepLevel.HOSTILE);
        junk.setRelationship(church.getId(), RepLevel.HOSTILE);
        junk.setRelationship(pirates.getId(), RepLevel.FAVORABLE);
        junk.setRelationship(diktat.getId(), RepLevel.HOSTILE);
        junk.setRelationship(tritachyon.getId(), RepLevel.HOSTILE);
        junk.setRelationship(independent.getId(), RepLevel.NEUTRAL);
        junk.setRelationship(asp.getId(), RepLevel.HOSTILE);
        junk.setRelationship(pack.getId(), RepLevel.FAVORABLE);
        junk.setRelationship(league.getId(), RepLevel.NEUTRAL); // Common borders; League remain more open than Heg
        junk.setRelationship(remnants.getId(), RepLevel.HOSTILE); // Common borders; League remain more open than Heg
        
        // make all the NRK fellas hostile, generally, to other people. Except big boy ANARCHISTS. All NRK friendly to PACK, and low tier boys tolerate JP
        List<FactionAPI> factionList = sector.getAllFactions();
        factionList.remove(junkboys);
        factionList.remove(technicians);
        factionList.remove(hounds);
        for (FactionAPI faction : factionList) {
            junkboys.setRelationship(faction.getId(), RepLevel.HOSTILE);
            technicians.setRelationship(faction.getId(), RepLevel.HOSTILE);
            hounds.setRelationship(faction.getId(), RepLevel.HOSTILE);
        }
        junkboys.setRelationship("player", RepLevel.NEUTRAL);
        junkboys.setRelationship(junk.getId(), RepLevel.NEUTRAL);
        junkboys.setRelationship(pack.getId(), RepLevel.FRIENDLY);
        technicians.setRelationship("player", RepLevel.HOSTILE);
        technicians.setRelationship(pack.getId(), RepLevel.FRIENDLY);
        hounds.setRelationship("player", RepLevel.NEUTRAL);
        hounds.setRelationship(pack.getId(), RepLevel.FRIENDLY);
        
        tritachyon.setRelationship(junk.getId(), RepLevel.HOSTILE); // We are not a tech mine
        hegemony.setRelationship(junk.getId(), RepLevel.HOSTILE); // Villainous scum
        pirates.setRelationship(junk.getId(), RepLevel.FAVORABLE); // He he these fellas are funny
        kol.setRelationship(junk.getId(), RepLevel.HOSTILE); // IN LUDDS NAME BEGONE
        league.setRelationship(junk.getId(), RepLevel.NEUTRAL); // Kinda misunderstood
        independent.setRelationship(junk.getId(), RepLevel.NEUTRAL); // Just like us; if more chaotic
        church.setRelationship(junk.getId(), RepLevel.NEUTRAL);
        path.setRelationship(junk.getId(), RepLevel.HOSTILE); // tech lovin heathens
        diktat.setRelationship(junk.getId(), RepLevel.HOSTILE); // Get outta our sector and hands off my synchrotron core
        remnants.setRelationship(junk.getId(), RepLevel.HOSTILE); // Get outta our sector and hands off my synchrotron core
        

        pack.setRelationship(hegemony.getId(), RepLevel.SUSPICIOUS);
        pack.setRelationship(pirates.getId(), RepLevel.NEUTRAL);
        pack.setRelationship(church.getId(), RepLevel.NEUTRAL);
        pack.setRelationship(path.getId(), RepLevel.HOSTILE);
        pack.setRelationship(diktat.getId(), RepLevel.SUSPICIOUS);
        pack.setRelationship(tritachyon.getId(), RepLevel.SUSPICIOUS);
        pack.setRelationship(independent.getId(), RepLevel.FAVORABLE);
        pack.setRelationship(junk.getId(), RepLevel.FAVORABLE);
        pack.setRelationship(asp.getId(), RepLevel.NEUTRAL);
        pack.setRelationship(league.getId(), RepLevel.WELCOMING);
        pack.setRelationship(remnants.getId(), RepLevel.HOSTILE);
        
        tritachyon.setRelationship(pack.getId(), RepLevel.SUSPICIOUS);
        hegemony.setRelationship(pack.getId(), RepLevel.NEUTRAL);
        pirates.setRelationship(pack.getId(), RepLevel.HOSTILE);
        remnants.setRelationship(pack.getId(), RepLevel.HOSTILE);
        kol.setRelationship(pack.getId(), RepLevel.NEUTRAL);
        league.setRelationship(pack.getId(), RepLevel.WELCOMING);
        diktat.setRelationship(pack.getId(), RepLevel.SUSPICIOUS);
        independent.setRelationship(pack.getId(), RepLevel.FAVORABLE);
        church.setRelationship(pack.getId(), RepLevel.SUSPICIOUS);
        path.setRelationship(pack.getId(), RepLevel.SUSPICIOUS);

        asp.setRelationship(hegemony.getId(), RepLevel.FAVORABLE);
        asp.setRelationship(pirates.getId(), RepLevel.HOSTILE);
        asp.setRelationship(church.getId(), RepLevel.FAVORABLE);
        asp.setRelationship(path.getId(), RepLevel.HOSTILE);
        asp.setRelationship(diktat.getId(), RepLevel.FAVORABLE);
        asp.setRelationship(tritachyon.getId(), RepLevel.WELCOMING);
        asp.setRelationship(independent.getId(), RepLevel.WELCOMING);
        asp.setRelationship(pack.getId(), RepLevel.NEUTRAL);
        asp.setRelationship(league.getId(), RepLevel.HOSTILE); // it just aint working. They want to cut us off
        asp.setRelationship(junk.getId(), RepLevel.HOSTILE);
        asp.setRelationship(remnants.getId(), RepLevel.HOSTILE);

	pirates.setRelationship(asp.getId(), RepLevel.HOSTILE);
        tritachyon.setRelationship(asp.getId(), RepLevel.WELCOMING);
        hegemony.setRelationship(asp.getId(), RepLevel.FAVORABLE);
        kol.setRelationship(asp.getId(), RepLevel.NEUTRAL);
        diktat.setRelationship(asp.getId(), RepLevel.NEUTRAL);
	league.setRelationship(asp.getId(), RepLevel.HOSTILE);
	church.setRelationship(asp.getId(), RepLevel.FAVORABLE);
	path.setRelationship(asp.getId(), RepLevel.HOSTILE);
	remnants.setRelationship(asp.getId(), RepLevel.HOSTILE);
	independent.setRelationship(asp.getId(), RepLevel.WELCOMING);
    }
}
