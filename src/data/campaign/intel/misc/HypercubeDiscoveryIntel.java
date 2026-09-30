package data.campaign.intel.misc;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import java.awt.Color;
import java.util.Set;

public class HypercubeDiscoveryIntel extends BaseIntelPlugin {
    
    public static boolean hasIntel() {
        if (Global.getSector().getIntelManager().hasIntelOfClass(HypercubeDiscoveryIntel.class)) return true;
        StarSystemAPI sys = Global.getSector().getStarSystem("Breh'Inni");
        if (sys != null) {
            SectorEntityToken hc = sys.getEntityById("hypercube");
            if (hc != null && hc.getMemoryWithoutUpdate().getBoolean("$hypercube_completed")) {
                return true;
            }
        }
        return false;
    }
    
    public static void addIntelIfNeeded(com.fs.starfarer.api.campaign.TextPanelAPI text, String source) {
        if (!hasIntel()) {
            HypercubeDiscoveryIntel intel = new HypercubeDiscoveryIntel(source);
            Global.getSector().getIntelManager().addIntel(intel, false, text);
        }
    }
    
    public static void markFound(com.fs.starfarer.api.campaign.TextPanelAPI text) {
        if (!hasIntel()) return;
        for (com.fs.starfarer.api.campaign.comm.IntelInfoPlugin plugin : Global.getSector().getIntelManager().getIntel(HypercubeDiscoveryIntel.class)) {
            HypercubeDiscoveryIntel intel = (HypercubeDiscoveryIntel) plugin;
            if (!intel.isCompleted()) {
                intel.complete();
                intel.sendUpdateIfPlayerHasIntel(intel, text);
            }
        }
    }

    protected String source;
    protected boolean completed = false;

    public HypercubeDiscoveryIntel(String source) {
        this.source = source != null ? source : "datacore";
    }

    public void complete() {
        completed = true;
        endAfterDelay();
    }
    
    public boolean isCompleted() {
        return completed;
    }

    @Override
    protected void advanceImpl(float amount) {
        if (completed) return;
        StarSystemAPI sys = Global.getSector().getStarSystem("Breh'Inni");
        if (sys != null) {
            SectorEntityToken hc = sys.getEntityById("hypercube");
            if (hc != null && hc.getMemoryWithoutUpdate().getBoolean("$hypercube_completed")) {
                complete();
                sendUpdateIfPlayerHasIntel(this, false);
            }
        }
    }
    
    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        Color c = getTitleColor(mode);
        info.setParaSmallInsignia();
        info.addPara(getName(), c, 0f);
        info.setParaFontDefault();

        bullet(info);
        Color tc = getBulletColorForMode(mode);
        Color h = Misc.getHighlightColor();

        if (!completed) {
            info.addPara("Anomalous geometric object located in the deep outer fringes of the Brehinni system", tc, 3f);
            info.addPara("Requires active scanning or interaction", h, 3f);
        } else {
            info.addPara("Hypercube puzzle solved", Misc.getPositiveHighlightColor(), 3f);
        }
        unindent(info);
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        Color h = Misc.getHighlightColor();
        float opad = 10f;

        if (!completed) {
            info.addPara(
                "You acquired deep-space sensor logs from a spacer at a portside bar. The telemetry points to a massive, perfectly geometric anomaly floating in the absolute dark on the outer fringes of the %s system.",
                opad, h, "Brehinni"
            );
            info.addPara(
                "The object has no recognizable energy signature, but it exhibits strange spatial distortions. You must find it in the outer dark and investigate.",
                opad, Misc.getTextColor()
            );
        } else {
            info.addPara(
                "You successfully tracked down the Hypercube in the Brehinni system and engaged with its bizarre, reality-bending puzzle mechanics.",
                opad, Misc.getPositiveHighlightColor()
            );
        }
    }

    @Override
    public String getIcon() {
        return Global.getSettings().getSpriteName("intel", "discovered_entity");
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add(Tags.INTEL_EXPLORATION);
        tags.add(Tags.INTEL_STORY);
        if (!isCompleted() && !isEnded() && !isEnding()) {
            tags.add(Tags.INTEL_ACCEPTED);
        }
        return tags;
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        StarSystemAPI sys = Global.getSector().getStarSystem("Breh'Inni");
        if (sys != null) {
            SectorEntityToken hc = sys.getEntityById("hypercube");
            if (hc != null) return hc;
            if (sys.getCenter() != null) return sys.getCenter();
        }
        return null;
    }

    @Override
    public String getName() {
        if (completed) {
            return "The Hypercube Enigma - Completed";
        }
        return "The Hypercube Enigma";
    }

    @Override
    public String getSortString() {
        return "Hypercube Enigma";
    }

    @Override
    public IntelSortTier getSortTier() {
        return IntelSortTier.TIER_2;
    }
}
