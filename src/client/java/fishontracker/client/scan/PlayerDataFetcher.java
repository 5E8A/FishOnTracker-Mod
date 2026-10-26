package fishontracker.client.scan;

import net.minecraft.client.Minecraft;

public class PlayerDataFetcher {

    public static String getUsername() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            return client.player.getGameProfile().name();
        }
        return "Unknown";
    }

    public static int getPlayerLevel() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            return client.player.experienceLevel;
        }
        return 0;
    }
}