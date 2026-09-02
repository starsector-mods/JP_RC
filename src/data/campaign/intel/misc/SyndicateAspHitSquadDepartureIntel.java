package data.campaign.intel.misc;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import static com.fs.starfarer.api.util.Misc.ucFirst;
import data.scripts.campaign.fleets.SyndicateAspHitSquadFleetAssignmentAI.SyndicateAspHitSquadData;
import java.awt.Color;
import java.util.Set;

public final class SyndicateAspHitSquadDepartureIntel extends BaseIntelPlugin {

    protected SectorEntityToken fromEntity;
    protected SectorEntityToken toEntity;
    protected String fromName;
    protected String fromFactionId;
    protected String toFactionId;
    protected int fleetPoints;

    transient protected FactionAPI aspFaction;
    transient protected FactionAPI origFaction;
    transient protected FactionAPI customerFaction;
    transient protected String fleetSizeDescriptor;

    public SyndicateAspHitSquadDepartureIntel(SyndicateAspHitSquadData data) {
        if (data == null || data.from == null || data.to == null || data.fleet == null) return;
        
        this.fromEntity = data.from.getPrimaryEntity();
        this.toEntity = data.to.getPrimaryEntity();
        this.fromName = data.from.getName();
        this.fromFactionId = data.from.getFactionId();
        this.toFactionId = data.to.getFactionId();
        this.fleetPoints = data.fleet.getFleetPoints();

        initTransientData();
        
        boolean sameLoc = fromEntity != null && fromEntity.getContainingLocation() != null &&
                          Global.getSector().getPlayerFleet() != null &&
                          fromEntity.getContainingLocation() == 
                              Global.getSector().getPlayerFleet().getContainingLocation() &&
                          !fromEntity.getContainingLocation().isHyperspace();
        
        float prob = 0.5f; // approx 50% base chance
        
        if (sameLoc) {
            prob += 0.5f; //  approx 100% chance if player in system
        }
        
        if (origFaction != null && origFaction.isHostileTo(Factions.PLAYER)) {
            prob -= 0.3f; // less likely to spill news at a hostile planet - 70% if in system, 20% if out system, 0% if lots of news & out of system
        }
        
        if (Math.random() > prob) {
            return;
        }
        
        this.hitSquad = data.fleet;
        Global.getSector().getIntelManager().addIntel(this);
    }
    
    protected com.fs.starfarer.api.campaign.CampaignFleetAPI hitSquad;

    @Override
    public void advance(float amount) {
        super.advance(amount);
        if (hitSquad == null || !hitSquad.isAlive()) {
            endAfterDelay();
        }
    }
    
    protected final void initTransientData() {
        aspFaction = Global.getSector().getFaction("syndicate_asp");
        origFaction = Global.getSector().getFaction(fromFactionId);
        customerFaction = Global.getSector().getFaction(toFactionId);
        
        if (fleetPoints > 300) {
            fleetSizeDescriptor = "huge";
        } else if (fleetPoints > 200) {
            fleetSizeDescriptor = "very large";
        } else if (fleetPoints > 100) {
            fleetSizeDescriptor = "large";
        } else if (fleetPoints > 50) {
            fleetSizeDescriptor = "moderate";
        } else if (fleetPoints > 15) {
            fleetSizeDescriptor = "small";
        } else {
            fleetSizeDescriptor = "tiny";
        }
    }
    
    protected void addBulletPoints(TooltipMakerAPI info, IntelInfoPlugin.ListInfoMode mode) {
        float initPad = (mode == IntelInfoPlugin.ListInfoMode.IN_DESC) ? 10f : 3f;
        Color tc = getBulletColorForMode(mode);
        
        bullet(info);
        
        if (mode != IntelInfoPlugin.ListInfoMode.IN_DESC) {
            info.addPara("Enforcing faction: %s", initPad, tc, aspFaction.getBaseUIColor(), aspFaction.getDisplayName());
            info.addPara("Origin: %s", 0f, tc, origFaction != null ? origFaction.getBaseUIColor() : Misc.getHighlightColor(), fromName);
            info.addPara("Fleet size: %s", 0f, tc, Misc.getHighlightColor(), ucFirst(fleetSizeDescriptor));
            initPad = 0f;
        }
        
        unindent(info);
    }
    
    protected String getWhat() {
        return Global.getSector().getPlayerPerson().getNameString();
    }
    
    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        Color c = getTitleColor(mode);
        initTransientData();
        info.addPara(getName(), c, 0f);
        addBulletPoints(info, mode);
    }
    
    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        initTransientData();
        Color tc = Misc.getTextColor();
        float opad = 10f;
        
        info.addImage(aspFaction.getLogo(), width, 128, opad);
        
        LabelAPI label = info.addPara("Urgent encrypted comms traffic intercepted from " + fromName + 
                 " indicates that your fleet's transponder profile has been flagged by syndicate enforcers.", opad, tc);
        label.setHighlight(fromName);
        label.setHighlightColors(origFaction != null ? origFaction.getBaseUIColor() : Misc.getHighlightColor());
        
        LabelAPI label2 = info.addPara("A specialized " + aspFaction.getPersonNamePrefix() + " Hit Squad was sighted departing " +
                 fromName + ", actively tracking your movements across the sector.", opad, tc);
        label2.setHighlight(aspFaction.getPersonNamePrefix() + " Hit Squad", fromName);
        label2.setHighlightColors(aspFaction.getBaseUIColor(), origFaction != null ? origFaction.getBaseUIColor() : Misc.getHighlightColor());
        
        String fleetSizeDescriptorModified = fleetSizeDescriptor;
        if (fleetSizeDescriptorModified.equals("moderate")) {
            fleetSizeDescriptorModified = "moderately sized";
        }
        
        LabelAPI label3;
        if (fleetSizeDescriptor.equals("tiny") || fleetSizeDescriptor.equals("small")) {
            label3 = info.addPara("Reconnaissance reports suggest the hunting group is relatively " + fleetSizeDescriptor + ", likely prioritizing speed and ambush tactics.", opad, tc);
        } else {
            label3 = info.addPara("Intelligence confirms the hunting detachment is " + fleetSizeDescriptorModified + ", heavily armed and prepared for heavy engagement.", opad, tc);
        }
        label3.setHighlight(fleetSizeDescriptorModified.equals("moderately sized") ? "moderately sized" : fleetSizeDescriptor);
        label3.setHighlightColors(Misc.getNegativeHighlightColor());
        
        info.beginIconGroup();
        info.setIconSpacingMedium();
        info.addIconGroup(32, 1, opad);
    }
    
    @Override
    public String getIcon() {
        initTransientData();
        return Global.getSettings().getSpriteName("intel", "hostilities");
    }
    
    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add(Tags.INTEL_MILITARY);
        tags.add("syndicate_asp");
        return tags;
    }
    
    public String getSortString() {
        return "Hit Fleet";
    }
    
    public String getFleetTypeName() {
        return "Hit Fleet";
    }
    
    public String getName() {
        return getFleetTypeName();
    }
    
    @Override
    public FactionAPI getFactionForUIColors() {
        initTransientData();
        return customerFaction;
    }

    public String getSmallDescriptionTitle() {
        return getName();
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        if (hitSquad != null && hitSquad.isAlive()) return hitSquad;
        return fromEntity;
    }
    
    @Override
    public java.util.List<IntelInfoPlugin.ArrowData> getArrowData(SectorMapAPI map) {
        java.util.List<IntelInfoPlugin.ArrowData> arrows = new java.util.ArrayList<IntelInfoPlugin.ArrowData>();
        com.fs.starfarer.api.campaign.CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (hitSquad != null && hitSquad.isAlive() && player != null) {
            IntelInfoPlugin.ArrowData arrow = new IntelInfoPlugin.ArrowData(hitSquad, player);
            arrow.color = new java.awt.Color(240, 70, 50, 200);
            arrow.width = 15f;
            arrow.alphaMult = 0.85f;
            arrows.add(arrow);
        }
        return arrows;
    }

    protected Object readResolve() {
        initTransientData();
        return this;
    }
}