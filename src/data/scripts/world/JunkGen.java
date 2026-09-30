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
import data.scripts.world.systems.York;
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
    new York().generate(sector);
    if (enableASP) {
        new Ursulo().generate(sector);
    }


    
    initFactionRelationships(sector);
    
    

    }
    
    private static ArrayList<String> makeArrayList(JSONArray list) {
            
            ArrayList<String> newlist = new ArrayList<>();
            if (list == null) return newlist;
            
            try {
                for (int i=0; i<list.length(); i++)
                {
                        newlist.add(list.getString(i));
                }
                return newlist;
                        
            } catch (Exception ex) { }
            
            return newlist;
    }
    
    private static void setRel(FactionAPI f1, FactionAPI f2, RepLevel level) {
        if (f1 != null && f2 != null && f1.getId() != null && f2.getId() != null) {
            f1.setRelationship(f2.getId(), level);
        }
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
        if (pack != null) {
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
        }

        // JUNK PIRATES relations
        if (junk != null) {
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
        }
        
        // ASP Relations
        if (asp != null) {
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
        }

        
        // PLAYER STARTING relations to MOD Factions
        if (player != null) {
            if (junk != null) player.setRelationship(junk.getId(), 0);
            if (pack != null) player.setRelationship(pack.getId(), 0);
            if (asp != null) player.setRelationship(asp.getId(), 0);
        }
        
        // Vanilla and this mod
        setRel(junk, hegemony, RepLevel.HOSTILE);
        setRel(junk, path, RepLevel.HOSTILE);
        setRel(junk, church, RepLevel.HOSTILE);
        setRel(junk, pirates, RepLevel.FAVORABLE);
        setRel(junk, diktat, RepLevel.HOSTILE);
        setRel(junk, tritachyon, RepLevel.HOSTILE);
        setRel(junk, independent, RepLevel.NEUTRAL);
        setRel(junk, asp, RepLevel.HOSTILE);
        setRel(junk, pack, RepLevel.FAVORABLE);
        setRel(junk, league, RepLevel.NEUTRAL); // Common borders; League remain more open than Heg
        setRel(junk, remnants, RepLevel.HOSTILE); // Common borders; League remain more open than Heg
        
        // make all the NRK fellas hostile, generally, to other people. Except big boy ANARCHISTS. All NRK friendly to PACK, and low tier boys tolerate JP
        List<FactionAPI> factionList = new ArrayList<>(sector.getAllFactions());
        if (junkboys != null) factionList.remove(junkboys);
        if (technicians != null) factionList.remove(technicians);
        if (hounds != null) factionList.remove(hounds);
        for (FactionAPI faction : factionList) {
            if (faction == null) continue;
            if (junkboys != null) junkboys.setRelationship(faction.getId(), RepLevel.HOSTILE);
            if (technicians != null) technicians.setRelationship(faction.getId(), RepLevel.HOSTILE);
            if (hounds != null) hounds.setRelationship(faction.getId(), RepLevel.HOSTILE);
        }
        if (junkboys != null) {
            junkboys.setRelationship("player", RepLevel.NEUTRAL);
            if (junk != null) junkboys.setRelationship(junk.getId(), RepLevel.NEUTRAL);
            if (pack != null) junkboys.setRelationship(pack.getId(), RepLevel.FRIENDLY);
        }
        if (technicians != null) {
            technicians.setRelationship("player", RepLevel.HOSTILE);
            if (pack != null) technicians.setRelationship(pack.getId(), RepLevel.FRIENDLY);
        }
        if (hounds != null) {
            hounds.setRelationship("player", RepLevel.NEUTRAL);
            if (pack != null) hounds.setRelationship(pack.getId(), RepLevel.FRIENDLY);
        }
        
        setRel(tritachyon, junk, RepLevel.HOSTILE); // We are not a tech mine
        setRel(hegemony, junk, RepLevel.HOSTILE); // Villainous scum
        setRel(pirates, junk, RepLevel.FAVORABLE); // He he these fellas are funny
        setRel(kol, junk, RepLevel.HOSTILE); // IN LUDDS NAME BEGONE
        setRel(league, junk, RepLevel.NEUTRAL); // Kinda misunderstood
        setRel(independent, junk, RepLevel.NEUTRAL); // Just like us; if more chaotic
        setRel(church, junk, RepLevel.NEUTRAL);
        setRel(path, junk, RepLevel.HOSTILE); // tech lovin heathens
        setRel(diktat, junk, RepLevel.HOSTILE); // Get outta our sector and hands off my synchrotron core
        setRel(remnants, junk, RepLevel.HOSTILE); // Get outta our sector and hands off my synchrotron core
        

        setRel(pack, hegemony, RepLevel.SUSPICIOUS);
        setRel(pack, pirates, RepLevel.NEUTRAL);
        setRel(pack, church, RepLevel.NEUTRAL);
        setRel(pack, path, RepLevel.HOSTILE);
        setRel(pack, diktat, RepLevel.SUSPICIOUS);
        setRel(pack, tritachyon, RepLevel.SUSPICIOUS);
        setRel(pack, independent, RepLevel.FAVORABLE);
        setRel(pack, junk, RepLevel.FAVORABLE);
        setRel(pack, asp, RepLevel.NEUTRAL);
        setRel(pack, league, RepLevel.WELCOMING);
        setRel(pack, remnants, RepLevel.HOSTILE);
        
        setRel(tritachyon, pack, RepLevel.SUSPICIOUS);
        setRel(hegemony, pack, RepLevel.NEUTRAL);
        setRel(pirates, pack, RepLevel.HOSTILE);
        setRel(remnants, pack, RepLevel.HOSTILE);
        setRel(kol, pack, RepLevel.NEUTRAL);
        setRel(league, pack, RepLevel.WELCOMING);
        setRel(diktat, pack, RepLevel.SUSPICIOUS);
        setRel(independent, pack, RepLevel.FAVORABLE);
        setRel(church, pack, RepLevel.SUSPICIOUS);
        setRel(path, pack, RepLevel.SUSPICIOUS);

        setRel(asp, hegemony, RepLevel.FAVORABLE);
        setRel(asp, pirates, RepLevel.HOSTILE);
        setRel(asp, church, RepLevel.FAVORABLE);
        setRel(asp, path, RepLevel.HOSTILE);
        setRel(asp, diktat, RepLevel.FAVORABLE);
        setRel(asp, tritachyon, RepLevel.WELCOMING);
        setRel(asp, independent, RepLevel.WELCOMING);
        setRel(asp, pack, RepLevel.NEUTRAL);
        setRel(asp, league, RepLevel.HOSTILE); // it just aint working. They want to cut us off
        setRel(asp, junk, RepLevel.HOSTILE);
        setRel(asp, remnants, RepLevel.HOSTILE);

        setRel(pirates, asp, RepLevel.HOSTILE);
        setRel(tritachyon, asp, RepLevel.WELCOMING);
        setRel(hegemony, asp, RepLevel.FAVORABLE);
        setRel(kol, asp, RepLevel.NEUTRAL);
        setRel(diktat, asp, RepLevel.NEUTRAL);
        setRel(league, asp, RepLevel.HOSTILE);
        setRel(church, asp, RepLevel.FAVORABLE);
        setRel(path, asp, RepLevel.HOSTILE);
        setRel(remnants, asp, RepLevel.HOSTILE);
        setRel(independent, asp, RepLevel.WELCOMING);
    }
}
