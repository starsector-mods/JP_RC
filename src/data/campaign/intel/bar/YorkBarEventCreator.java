package data.campaign.intel.bar;

import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarEvent;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventCreator;
import data.campaign.intel.misc.YorkDiscoveryIntel;

public class YorkBarEventCreator extends BaseBarEventCreator {

    @Override
    public PortsideBarEvent createBarEvent() {
        return new YorkBarEvent();
    }

    @Override
    public float getBarEventFrequencyWeight() {
        // High frequency if the player hasn't discovered York yet
        if (YorkDiscoveryIntel.hasIntel()) {
            return 0f;
        }
        return 15f;
    }

    @Override
    public float getBarEventActiveDuration() {
        return 60f;
    }

    @Override
    public float getBarEventTimeoutDuration() {
        return 15f;
    }

    @Override
    public boolean isPriority() {
        return false;
    }
}
