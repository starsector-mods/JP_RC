package data.campaign.intel.misc;

import java.awt.Color;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class YorkDiscoveryIntel extends BaseIntelPlugin {

    public static enum YorkQuestStage {
        EXPLORE_YORK,
        SLOT_ALPHA_CORE,
        SANCTUARY_AWAKENED
    }

    public static boolean hasIntel() {
        if (Global.getSector().getIntelManager().hasIntelOfClass(YorkDiscoveryIntel.class)) return true;
        // Check if organic discovery already completed it
        com.fs.starfarer.api.campaign.StarSystemAPI york = Global.getSector().getStarSystem("York");
        if (york != null) {
            com.fs.starfarer.api.campaign.SectorEntityToken station = york.getEntityById("lincoln_cathedral");
            if (station != null && station.getMemoryWithoutUpdate().getBoolean("$alphaCoreSlotted")) {
                return true;
            }
        }
        return false;
    }

    public static void addIntelIfNeeded(TextPanelAPI textPanel, String source) {
        if (hasIntel()) return;
        YorkDiscoveryIntel intel = new YorkDiscoveryIntel(source);
        Global.getSector().getIntelManager().addIntel(intel, false, textPanel);
    }

    public static void markCoreSlotted(TextPanelAPI textPanel) {
        YorkDiscoveryIntel intel = (YorkDiscoveryIntel) Global.getSector().getIntelManager().getFirstIntel(YorkDiscoveryIntel.class);
        if (intel != null) {
            intel.stage = YorkQuestStage.SANCTUARY_AWAKENED;
            intel.completed = true;
            if (textPanel != null) {
                intel.sendUpdateIfPlayerHasIntel(intel, textPanel);
            } else {
                intel.sendUpdateIfPlayerHasIntel(intel, false);
            }
            intel.endAfterDelay();
        }
    }

    protected String source;
    protected boolean completed = false;
    protected YorkQuestStage stage = YorkQuestStage.EXPLORE_YORK;

    public YorkDiscoveryIntel(String source) {
        this.source = source != null ? source : "datacore";
        Global.getSector().addScript(this);
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);
        if (completed || isEnded() || isEnding()) return;

        StarSystemAPI york = Global.getSector().getStarSystem("York");
        if (york != null && Global.getSector().getPlayerFleet() != null) {
            SectorEntityToken cathedral = york.getEntityById("lincoln_cathedral");
            if (cathedral != null && cathedral.getMemoryWithoutUpdate().getBoolean("$alphaCoreSlotted")) {
                stage = YorkQuestStage.SANCTUARY_AWAKENED;
                completed = true;
                sendUpdateIfPlayerHasIntel(this, false);
                endAfterDelay();
                return;
            }

            if (stage == YorkQuestStage.EXPLORE_YORK && Global.getSector().getPlayerFleet().getContainingLocation() == york) {
                stage = YorkQuestStage.SLOT_ALPHA_CORE;
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
        Color p = Misc.getPositiveHighlightColor();
        Color n = Misc.getNegativeHighlightColor();

        if (stage == YorkQuestStage.EXPLORE_YORK) {
            info.addPara("Uncharted system in the deep southern fringe", tc, 3f);
            info.addPara("Explore the lost sanctuary world of Lincoln", tc, 3f);
            info.addPara("Acquire an Alpha Core to awaken Lincoln Cathedral", h, 3f);
            info.addPara("Warning: High-energy automated drone signatures detected", n, 3f);
        } else if (stage == YorkQuestStage.SLOT_ALPHA_CORE) {
            info.addPara("Reached the York star system", p, 3f);
            info.addPara("Slot an Alpha Core into Lincoln Cathedral's neural cradle", h, 3f);
            info.addPara("Warning: Core cannot be unplugged once slotted", Misc.getStoryOptionColor(), 3f);
            info.addPara("Warning: Giant Remnant Ordos guard the orbital perimeter", n, 3f);
        } else {
            info.addPara("Reached the York star system", p, 3f);
            info.addPara("Lincoln Cathedral awakened by integrated Alpha Core", p, 3f);
            info.addPara("Alpha Core permanently fused as Cathedral Custodian", h, 3f);
            info.addPara("Sanctuary secured and ready for colonization", p, 3f);
        }
        unindent(info);
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        Color h = Misc.getHighlightColor();
        float opad = 10f;

        info.addImage(Global.getSettings().getSpriteName("illustrations", "lincoln_cathedral"), width, opad);

        if (stage == YorkQuestStage.EXPLORE_YORK) {
            info.addPara(
                "Your fleet has acquired decrypted Early-Domain astrometric nav-data detailing a forgotten " +
                "planetary sanctuary designated %s, located far beyond the Core Worlds in the deep southern void.",
                opad, h, "York"
            );
            info.addPara(
                "According to recovered survey archives, the system harbors an untouched %s garden world (%s) " +
                "and a colossal vaulted planetary installation known as %s. " +
                "The facility's primary cognitive architecture requires an %s to fully awaken.",
                opad, h, "Terran", "Lincoln", "Lincoln Cathedral", "Alpha Core"
            );
            info.addPara(
                "Caution: Sensor telemetry flags severe automated Remnant drone fleets guarding the system. " +
                "Reclaiming the sanctuary will require substantial combat strength or extreme cunning.",
                opad, Misc.getNegativeHighlightColor()
            );
        } else if (stage == YorkQuestStage.SLOT_ALPHA_CORE) {
            info.addPara(
                "Your fleet has successfully navigated the deep hyperspace void and entered the %s system. " +
                "Sensors confirm the pristine condition of %s and the silent spires of %s.",
                opad, h, "York", "Lincoln", "Lincoln Cathedral"
            );
            info.addPara(
                "To awaken the sanctuary, dock with Lincoln Cathedral and slot an %s into the central neural cradle.",
                opad, h, "Alpha Core"
            );
            info.addPara(
                "Important Note: In accordance with Domain sanctuary architecture, the neural cradle's molecular induction " +
                "clamps will permanently fuse the Alpha Core to the station's superstructure upon installation. " +
                "The core cannot be unplugged, removed, or retrieved once slotted.",
                opad, Misc.getStoryOptionColor()
            );
            info.addPara(
                "Caution: Multiple giant Remnant Ordos have been detected guarding Lincoln and key orbital focal points.",
                opad, Misc.getNegativeHighlightColor()
            );
        } else {
            info.addPara(
                "The %s has been permanently slotted into the central neural cradle of %s. " +
                "Superconducting clamps have fused the core directly to the facility's structural keel, establishing a permanent " +
                "neural-biosphere telemetry link with the jungles of %s outside.",
                opad, h, "Alpha Core", "Lincoln Cathedral", "Lincoln"
            );
            info.addPara(
                "The awakened Alpha Core now serves as the eternal, sentient custodian of the Cathedral—overseeing " +
                "subterranean storage berths, autonomous surface drydocks, and planetary ecological coordination.",
                opad, Misc.getPositiveHighlightColor()
            );
            info.addPara(
                "As engineered by Domain sanctuary protocols, the Alpha Core is permanently integrated and cannot be unplugged. " +
                "The vision for tomorrow is realized.",
                opad, h
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
        tags.add(Tags.INTEL_ACCEPTED);
        return tags;
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        StarSystemAPI york = Global.getSector().getStarSystem("York");
        if (york != null) {
            SectorEntityToken cathedral = york.getEntityById("lincoln_cathedral");
            if (stage == YorkQuestStage.SLOT_ALPHA_CORE && cathedral != null) return cathedral;
            SectorEntityToken lincoln = york.getEntityById("lincoln");
            if (lincoln != null) return lincoln;
            if (york.getCenter() != null) return york.getCenter();
        }
        return null;
    }

    @Override
    public String getName() {
        if (completed) {
            return "Vision for Tomorrow - Completed";
        }
        return "Vision for Tomorrow";
    }

    @Override
    public String getSortString() {
        return "Vision for Tomorrow";
    }

    @Override
    public IntelSortTier getSortTier() {
        return IntelSortTier.TIER_2;
    }
}
