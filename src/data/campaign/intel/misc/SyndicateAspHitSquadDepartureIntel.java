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
        this.fromFactionId = data.from.getFaction().getId();
        this.toFactionId = data.to.getFaction().getId();
        this.fleetPoints = data.fleet.getFleetPoints();

        initTransientData();
        
        boolean sameLoc = fromEntity != null && fromEntity.getContainingLocation() != null &&
                          fromEntity.getContainingLocation() == 
                              Global.getSector().getPlayerFleet().getContainingLocation() &&
                          !fromEntity.getContainingLocation().isHyperspace();
        
        float prob = 0.5f; // approx 50% base chance
        
        if (sameLoc) {
            prob += 0.5f; //  approx 100% chance if player in system
        }
        
        float target = 5f; // we only want maybe 5 or 6 sector wide. If you are so heavily wanted it's fine to be diluted
        float numAlready = Global.getSector().getIntelManager().getIntelCount(SyndicateAspHitSquadDepartureIntel.class, true);
        
        if (numAlready > target) {
            prob -= 0.15f * (numAlready - target); // less chance more news, 15% per news item over
        }
        
        if (origFaction != null && origFaction.isHostileTo(Factions.PLAYER)) {
            prob -= 0.3f; // less likely to spill news at a hostile planet - 70% if in system, 20% if out system, 0% if lots of news & out of system
        }
        
        if (Math.random() > prob) {
            return;
        }
        
        float postingRange = 0f;
        setPostingRangeLY(postingRange, true);
        setPostingLocation(fromEntity);
        
        float postingTime = ((float) Math.random() * 6) + 4; 
        
        Global.getSector().getIntelManager().queueIntel(this, postingTime); // stick it there for about a week or so, fairly transient sort of news.
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
            info.addPara("Faction: " + aspFaction.getDisplayName(), initPad, tc,
                         aspFaction.getBaseUIColor(), aspFaction.getDisplayName());
            info.addPara("Fleet size: " + ucFirst(fleetSizeDescriptor), initPad, tc);
            initPad = 0f;
        }
        
        if (mode != IntelInfoPlugin.ListInfoMode.IN_DESC) {
            LabelAPI label = info.addPara("Hit Squad at " + fromName, tc, initPad);
            label.setHighlight(fromName);
            label.setHighlightColors(origFaction.getBaseUIColor());
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
        
        String fleetType = "Hit Fleet";
        
        LabelAPI label = info.addPara("You are getting a lot of comms traffic from " + fromName + 
                 " with your ident attached to it.", opad, tc);
        @SuppressWarnings("unused")
                LabelAPI label2 = info.addPara("It appears that " + aspFaction.getPersonNamePrefixAOrAn() + " " + 
                 aspFaction.getPersonNamePrefix() + " " + fleetType +
                 " was seen in orbit, seeking not much else but your whereabouts.", opad, tc);
        
        label.setHighlight(fromName, aspFaction.getPersonNamePrefix());
        label.setHighlightColors(origFaction.getBaseUIColor(), aspFaction.getBaseUIColor());
        
        String fleetSizeDescriptorModified = fleetSizeDescriptor;
        if (fleetSizeDescriptorModified.equals("moderate")) {
            fleetSizeDescriptorModified = "moderately sized";
        }
        
        LabelAPI label3;
        if (fleetSizeDescriptor.equals("tiny") || fleetSizeDescriptor.equals("small")) {
            label3 = info.addPara("However, rumours suggest the fleet is " + fleetSizeDescriptor + ".", opad);
        } else {
            label3 = info.addPara("The information available points to the fleet being " + fleetSizeDescriptorModified + ".", opad);
        }
        label3.setHighlight(fleetSizeDescriptor);
        label3.setHighlightColors(aspFaction.getBaseUIColor());
        
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
        return fromEntity;
    }

    protected Object readResolve() {
        initTransientData();
        return this;
    }
}