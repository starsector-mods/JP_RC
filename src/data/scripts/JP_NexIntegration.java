package data.scripts;

import exerelin.campaign.SectorManager;

public class JP_NexIntegration {

    /**
     * Safely checks if Nexerelin is in Corvus mode.
     * This class should only be accessed if Nexerelin is enabled.
     */
    public static boolean isCorvusMode() {
        return SectorManager.getManager().isCorvusMode();
    }
}
