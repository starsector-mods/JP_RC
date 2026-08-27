package data.campaign.intel.misc;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import static com.fs.starfarer.api.util.Misc.ucFirst;
import data.scripts.campaign.fleets.JunkPiratesExplorerFleetAssignmentAI.JunkPiratesExplorerData;
import java.awt.Color;
import java.util.Set;

public final class JunkExplorerDecisionIntel extends BaseIntelPlugin {

    protected SectorEntityToken fromEntity;
    protected String fromName;
    protected String toName;
    protected String toSystemName;
    protected String fromFactionId;
    protected String toFactionId;
    protected int fleetPoints;
    protected String commanderName;
    protected String commanderRank;
    protected boolean commanderIsMale;
    protected String decision;

    transient protected FactionAPI junkFaction;
    transient protected FactionAPI origFaction;
    transient protected String fleetSizeDescriptor;
    transient protected String fleetType;

    public JunkExplorerDecisionIntel(JunkPiratesExplorerData data, String decision) {
        this.decision = decision;
        
        if (data == null || data.from == null || data.to == null || data.fleet == null || data.fleet.getFaction() == null) return;
        
        this.fromEntity = data.from.getPrimaryEntity();
        this.fromName = data.from.getName();
        this.toName = data.to.getName();
        this.toSystemName = data.to.getStarSystem().getName();
        this.fromFactionId = data.from.getFaction().getId();
        this.toFactionId = data.to.getFaction().getId();
        this.fleetPoints = data.fleet.getFleetPoints();
        
        if (data.fleet.getCommander() != null) {
            this.commanderName = data.fleet.getCommander().getNameString();
            this.commanderRank = data.fleet.getCommander().getRank();
            this.commanderIsMale = data.fleet.getCommander().isMale();
        } else {
            this.commanderName = "Unknown";
            this.commanderRank = "Captain";
            this.commanderIsMale = false;
        }

        initTransientData();
        
        boolean sameLoc = fromEntity != null && fromEntity.getContainingLocation() != null &&
                          fromEntity.getContainingLocation() == 
                              Global.getSector().getPlayerFleet().getContainingLocation() &&
                          !fromEntity.getContainingLocation().isHyperspace();
        
        boolean friends = data.fleet.getFaction().isAtWorst(Global.getSector().getPlayerFaction(), RepLevel.WELCOMING);
        if (!friends) return;
        
        float prob = 1.0f;
        if (!sameLoc) {
            prob -= 0.6f; // 40% chance if not in system
        }
        
        if (Math.random() > prob) {
            return;
        }
        
        float postingRange = 0f;
        setPostingRangeLY(postingRange, true);
        setPostingLocation(fromEntity);
        
        Global.getSector().getIntelManager().queueIntel(this, 15);
    }
    
    protected final void initTransientData() {
        junkFaction = Global.getSector().getFaction("junk_pirates");
        origFaction = Global.getSector().getFaction(fromFactionId);
        
        if (decision.equals("party")) {
            fleetType = "Explorers";
        } else if (decision.equals("troll")) {
            fleetType = "Trouble Makers";
        } else if (decision.equals("scavenge")) {
            fleetType = "Scavengers";
        }
        
        if (fleetPoints > 300) {
            fleetSizeDescriptor = "huge";
        } else if (fleetPoints > 200) {
            fleetSizeDescriptor = "very large";
        } else if (fleetPoints > 100) {
            fleetSizeDescriptor = "large";
        } else if (fleetPoints > 50) {
            fleetSizeDescriptor = "moderate";
        } else if (fleetPoints > 10) {
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
            info.addPara("Fleet size: " + ucFirst(fleetSizeDescriptor), initPad, tc);
            info.addPara("Target: " + toName, initPad, tc);
            initPad = 0f;
        }
        
        if (mode != IntelInfoPlugin.ListInfoMode.IN_DESC) {
            @SuppressWarnings("unused")
                        LabelAPI label = info.addPara(toSystemName, tc, initPad);
            initPad = 0f;
        }
        
        unindent(info);
    }
    
    protected String getWhat() {
        return commanderName;
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
        
        info.addImage(junkFaction.getLogo(), width, 128, opad);        
        
        String heOrShe = "She";
        String hisOrHer = "her";
        String whatWillSheDo = " has decided to cause as much trouble as possible within the ";
        
        if (commanderIsMale) {
            heOrShe = "He";
            hisOrHer = "his";
        }
        
        String fleetDescriptor = "fleet";
        
        if (decision.equals("party")) {
            whatWillSheDo = " has decided on a voyage of discovery; traveling to the ";
            fleetDescriptor = "group of friends";
        } else if (decision.equals("scavenge")) {
            whatWillSheDo = " has decided to go on a salvage expedition; traveling to the ";
            fleetDescriptor = "scavenger fleet";
        }

        LabelAPI label = info.addPara("Friends at " + fromName + 
                 " are excited to inform you that, having spent time in reflection, " + getWhat() +
                 ", the " + 
                 junkFaction.getPersonNamePrefix() + " " + commanderRank +
                 " is leaving orbit having decided on " +
                 hisOrHer + " preferred course of action.", opad, tc);
        
        label.setHighlight(fromName, junkFaction.getPersonNamePrefix());
        if (origFaction != null) {
            label.setHighlightColors(origFaction.getBaseUIColor(), junkFaction.getBaseUIColor());
        } else {
            label.setHighlightColors(junkFaction.getBaseUIColor(), junkFaction.getBaseUIColor());
        }
        
        String fleetSizeDescriptorModified = fleetSizeDescriptor;
        if (fleetSizeDescriptorModified.equals("moderate")) {
            fleetSizeDescriptorModified = "moderately sized";
        }
        @SuppressWarnings("unused")
                LabelAPI label2 = info.addPara(heOrShe + whatWillSheDo + toSystemName + ". " + heOrShe + " travels with a " +
                        fleetSizeDescriptorModified + " " + fleetDescriptor + ".", opad, tc);
        
        if (fleetSizeDescriptor.equals("tiny")) {
            @SuppressWarnings("unused")
                        LabelAPI label3 = info.addPara(heOrShe + " should know better, really.", opad, tc);
        } else if (fleetSizeDescriptor.equals("huge")) {
            @SuppressWarnings("unused")
                        LabelAPI label3 = info.addPara(heOrShe + " has often been accused of overdoing it.", opad, tc);
        }
        
        info.beginIconGroup();
        info.setIconSpacingMedium();
        info.addIconGroup(32, 1, opad);
    }
    
    @Override
    public String getIcon() {
        initTransientData();
        if (decision.equals("party")) {
            return Global.getSettings().getSpriteName("intel", "junk_pirates_party");
        } else if (decision.equals("scavenge")) {
            return Global.getSettings().getSpriteName("intel", "tradeFleet_other");
        }
        return Global.getSettings().getSpriteName("intel", "hostilities");
    }
    
    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        if ("party".equals(decision)) {
            tags.add(Tags.INTEL_STORY);
        } else if ("scavenge".equals(decision)) {
            tags.add(Tags.INTEL_EXPLORATION);
        } else {
            tags.add(Tags.INTEL_HOSTILITIES);
        }
        tags.add("junk_pirates");
        return tags;
    }
    
    public String getSortString() {
        return "Explorer Fleet";
    }
    
    public String getFleetTypeName() {
        String type = "Junk Pirates Explorers";
        if ("troll".equals(decision)) {
            type = "Junk Pirates Troublemakers";
        } else if ("scavenge".equals(decision)) {
            type = "Junk Pirates Scavengers";
        }
        return type;
    }
    
    public String getName() {
        return getFleetTypeName();
    }
    
    @Override
    public FactionAPI getFactionForUIColors() {
        initTransientData();
        if (toFactionId != null) {
            return Global.getSector().getFaction(toFactionId);
        }
        return null;
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