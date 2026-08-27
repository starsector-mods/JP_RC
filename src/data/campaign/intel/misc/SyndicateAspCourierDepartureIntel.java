package data.campaign.intel.misc;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.fleets.SyndicateAspCourierFleetAssignmentAI.SyndicateAspCourierRouteData;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class SyndicateAspCourierDepartureIntel extends BaseIntelPlugin {

    protected SectorEntityToken fromEntity;
    protected SectorEntityToken toEntity;
    protected String fromName;
    protected String toName;
    protected String fromFactionId;
    protected String toFactionId;
    protected String fromOnOrAt;
    protected String cargoType;
    protected float fleetSize;
    protected boolean sameLocation;
    protected String cargoListString;

    transient protected boolean money;
    transient protected boolean prisoner;
    transient protected boolean items;
    transient protected boolean vip;
    transient protected FactionAPI customerFaction;
    transient protected FactionAPI origFaction;
    transient protected FactionAPI aspFaction;

    public SyndicateAspCourierDepartureIntel(SyndicateAspCourierRouteData data) {
        if (data == null || data.from == null || data.fleet == null || data.cargotype == null || data.to == null) return;
        
        this.fromEntity = data.from.getPrimaryEntity();
        this.toEntity = data.to.getPrimaryEntity();
        this.fromName = data.from.getName();
        this.toName = data.to.getName();
        this.fromFactionId = data.from.getFaction().getId();
        this.toFactionId = data.to.getFaction().getId();
        this.fromOnOrAt = data.from.getOnOrAt();
        this.cargoType = data.cargotype;
        this.fleetSize = data.size;
        this.cargoListString = SyndicateAspCourierRouteData.getCargoList(data.cargoDeliver);
        
        this.sameLocation = fromEntity != null && toEntity != null && fromEntity.getContainingLocation() == toEntity.getContainingLocation() && !fromEntity.getContainingLocation().isHyperspace();

        initTransientData();
        
        float prob = 0.1f;
        if (prisoner) prob += 0.3f;
        if (money) prob += 0.1f;
        
        boolean sameLoc = fromEntity != null && fromEntity.getContainingLocation() != null &&
                          fromEntity.getContainingLocation() == 
                              Global.getSector().getPlayerFleet().getContainingLocation() &&
                          !fromEntity.getContainingLocation().isHyperspace();
        if (sameLoc) prob = 1f;
        
        float target = Global.getSettings().getFloat("targetNumTradeFleetNotifications"); // may as well align with vanilla
        float numAlready = Global.getSector().getIntelManager().getIntelCount(SyndicateAspCourierDepartureIntel.class, true);
        
        float probMult = Misc.getProbabilityMult(target, numAlready, 0.5f);
        if (probMult > 1) probMult = 1; // just making it less likely if there's a bunch of these already
        
        prob *= probMult;
        
        if (Math.random() > prob) {
            return;
        }
        
        float postingRange = 0f;
        if (prisoner) {
            postingRange = Math.max(3f, postingRange); // bigger stink
        }
        setPostingRangeLY(postingRange, true);
        
        setPostingLocation(fromEntity);
        
        Global.getSector().getIntelManager().queueIntel(this, 20);
    }
    
    protected final void initTransientData() {
        items = false;
        money = false;
        prisoner = false;
        vip = false;
        
        if (cargoType == null) cargoType = "items";
        
        if (cargoType.equals("items")) {
            items = true;
        } else if (cargoType.equals("prisoner")) {
            prisoner = true;
        } else if (cargoType.equals("money")) {
            money = true;
        } else if (cargoType.equals("vip")) {
            vip = true;
        }

        customerFaction = Global.getSector().getFaction(toFactionId);
        origFaction = Global.getSector().getFaction(fromFactionId);
        aspFaction = Global.getSector().getFaction("syndicate_asp");
    }
    
    protected void addBulletPoints(TooltipMakerAPI info, IntelInfoPlugin.ListInfoMode mode) {
        float initPad = (mode == IntelInfoPlugin.ListInfoMode.IN_DESC) ? 10f : 3f;
        Color tc = getBulletColorForMode(mode);
        
        bullet(info);
        
        if (mode != IntelInfoPlugin.ListInfoMode.IN_DESC) {
            info.addPara("Working for: " + customerFaction.getDisplayName(), initPad, tc,
                         customerFaction.getBaseUIColor(), customerFaction.getDisplayName());
            initPad = 0f;
        }
        
        if (mode != IntelInfoPlugin.ListInfoMode.IN_DESC) {
            LabelAPI label = info.addPara("From " + fromName + " to " + toName, tc, initPad);
            label.setHighlight(fromName, toName);
            label.setHighlightColors(origFaction.getBaseUIColor(), customerFaction.getBaseUIColor());
            initPad = 0f;
        }
        
        unindent(info);
    }
    
    protected String getWhat() {
        String what = "high-value smuggled goods and black market technology";
        if (cargoType.equals("prisoner")) {
            what = "dangerous prisoners";
        } else if (cargoType.equals("money")) {
            what = "valuable items";
        } else if (cargoType.equals("vip")) {
            what = "important people";
        } else if (cargoType.equals("items")) {
            if (cargoListString != null && !cargoListString.isEmpty() && !cargoListString.equals("nothing")) {
                what = cargoListString;
            }
        }
        return what;
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
        
        String fleetType = "Courier Fleet";
        
        LabelAPI label = info.addPara("Your contacts " + fromOnOrAt + " " + fromName + 
                 " let you know that " + 
                 aspFaction.getPersonNamePrefixAOrAn() + " " + 
                 aspFaction.getPersonNamePrefix() + " " + fleetType + " was seen in orbit around " + 
                 fromName + ".",
                 opad, tc, 
                 aspFaction.getBaseUIColor(),
                 aspFaction.getPersonNamePrefix());
        
        label.setHighlight(fromName, aspFaction.getPersonNamePrefix(), toName);
        label.setHighlightColors(origFaction.getBaseUIColor(), aspFaction.getBaseUIColor(), customerFaction.getBaseUIColor());
        
        String what = getWhat();
        
        LabelAPI label2 = info.addPara("Information is limited, but the courier group were seen in negotiations with " + customerFaction.getDisplayName() + 
                " officials and were rumoured to be discussing the shipping of " + what + " to " + toName + ".", opad);
        
        label2.setHighlight(customerFaction.getDisplayName(), toName);
        label2.setHighlightColors(customerFaction.getBaseUIColor(), customerFaction.getBaseUIColor());
        
        info.beginIconGroup();
        info.setIconSpacingMedium();
        info.addIconGroup(32, 1, opad);
    }
    
    @Override
    public String getIcon() {
        initTransientData();
        return Global.getSettings().getSpriteName("intel", "tradeFleet_other");
    }
    
    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add(Tags.INTEL_FLEET_DEPARTURES);
        tags.add("syndicate_asp");
        if (toFactionId != null) {
            tags.add(toFactionId);
        }
        return tags;
    }
    
    public String getSortString() {
        return "Courier Fleet";
    }
    
    public String getFleetTypeName() {
        String fleetType = "Courier Fleet";
        if (prisoner) {
            fleetType = "Armed Guard";
        }
        return fleetType;
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
    
    public List<IntelInfoPlugin.ArrowData> getArrowData(SectorMapAPI map) {
        List<IntelInfoPlugin.ArrowData> result = new ArrayList<IntelInfoPlugin.ArrowData>();
        
        if (sameLocation) {
            return null;
        }
        
        SectorEntityToken entityFrom = fromEntity;
        if (map != null) {
            SectorEntityToken iconEntity = map.getIntelIconEntity(this);
            if (iconEntity != null) {
                entityFrom = iconEntity;
            }
        }
        
        IntelInfoPlugin.ArrowData arrow = new IntelInfoPlugin.ArrowData(entityFrom, toEntity);
        arrow.color = getFactionForUIColors().getBaseUIColor();
        result.add(arrow);
        
        return result;
    }
}