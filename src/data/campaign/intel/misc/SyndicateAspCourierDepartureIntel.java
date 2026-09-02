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
        this.fromFactionId = data.from.getFactionId();
        this.toFactionId = data.to.getFactionId();
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
                          Global.getSector().getPlayerFleet() != null &&
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
        
        Color custColor = customerFaction != null ? customerFaction.getBaseUIColor() : Misc.getTextColor();
        String custName = customerFaction != null ? customerFaction.getDisplayName() : "Contractor";
        Color origColor = origFaction != null ? origFaction.getBaseUIColor() : Misc.getTextColor();
        
        if (mode != IntelInfoPlugin.ListInfoMode.IN_DESC) {
            info.addPara("Client: %s", initPad, tc, custColor, custName);
            LabelAPI label = info.addPara("Route: " + fromName + " to " + toName, tc, 0f);
            label.setHighlight(fromName, toName);
            label.setHighlightColors(origColor, custColor);
            info.addPara("Cargo manifest: %s", 0f, tc, Misc.getHighlightColor(), Misc.ucFirst(getWhat()));
            initPad = 0f;
        }
        
        unindent(info);
    }
    
    protected String getWhat() {
        String what = "high-value smuggled goods and black market technology";
        if (cargoType.equals("prisoner")) {
            what = "dangerous high-profile prisoners";
        } else if (cargoType.equals("money")) {
            what = "encrypted bearer credit chips";
        } else if (cargoType.equals("vip")) {
            what = "corporate and underworld VIPs";
        } else if (cargoType.equals("items")) {
            what = "specialized equipment and heavy machinery";
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
        
        String logo = (aspFaction != null) ? aspFaction.getLogo() : null;
        if (logo != null) {
            info.addImage(logo, width, 128, opad);
        }
        
        String fleetType = "Courier Fleet";
        if (prisoner) {
            fleetType = "Prisoner Transport Escort";
        } else if (vip) {
            fleetType = "Executive VIP Escort";
        } else if (money) {
            fleetType = "High-Security Specie Convoy";
        }
        
        String prefixAOrAn = (aspFaction != null) ? aspFaction.getPersonNamePrefixAOrAn() : "a";
        String prefix = (aspFaction != null) ? aspFaction.getPersonNamePrefix() : "Syndicate";
        Color aspColor = (aspFaction != null) ? aspFaction.getBaseUIColor() : Misc.getBasePlayerColor();
        Color origColor = (origFaction != null) ? origFaction.getBaseUIColor() : Misc.getTextColor();
        Color custColor = (customerFaction != null) ? customerFaction.getBaseUIColor() : Misc.getTextColor();
        String custName = (customerFaction != null) ? customerFaction.getDisplayName() : "Unknown clients";
        
        LabelAPI label = info.addPara("Underworld contacts " + fromOnOrAt + " " + fromName + 
                 " report that " + 
                 prefixAOrAn + " " + 
                 prefix + " " + fleetType + " has departed orbit around " + 
                 fromName + ".",
                 opad, tc);
        
        label.setHighlight(fromName, prefix + " " + fleetType);
        label.setHighlightColors(origColor, aspColor);
        
        String what = getWhat();
        
        LabelAPI label2 = info.addPara("The syndicate group was contracted by " + custName + 
                " officials to discreetly transport " + what + " to " + toName + ".", opad, tc);
        
        label2.setHighlight(custName, what, toName);
        label2.setHighlightColors(custColor, Misc.getHighlightColor(), custColor);
        
        info.beginIconGroup();
        info.setIconSpacingMedium();
        info.addIconGroup(32, 1, opad);
    }
    
    @Override
    public String getIcon() {
        initTransientData();
        return (aspFaction != null) ? aspFaction.getCrest() : "graphics/icons/intel/courier.png";
    }
    
    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add(Tags.INTEL_ACCEPTED);
        tags.add(Tags.INTEL_FLEET_LOG);
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
        if (sameLocation || toEntity == null || fromEntity == null) {
            return null;
        }
        
        SectorEntityToken entityFrom = fromEntity;
        if (map != null) {
            SectorEntityToken iconEntity = map.getIntelIconEntity(this);
            if (iconEntity != null) {
                entityFrom = iconEntity;
            }
        }
        
        if (entityFrom == null) {
            return null;
        }
        
        List<IntelInfoPlugin.ArrowData> result = new ArrayList<IntelInfoPlugin.ArrowData>();
        IntelInfoPlugin.ArrowData arrow = new IntelInfoPlugin.ArrowData(entityFrom, toEntity);
        arrow.color = getFactionForUIColors().getBaseUIColor();
        result.add(arrow);
        
        return result;
    }
}