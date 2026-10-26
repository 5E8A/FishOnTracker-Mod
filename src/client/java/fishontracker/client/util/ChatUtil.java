package fishontracker.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Sends FishOnTracker-prefixed messages to the player chat and the action bar.
 * All methods are null-safe (no-ops when the player/GUI is not available).
 */
public final class ChatUtil {

    private static final String PREFIX = "§2§l[FOT] §8» ";

    private ChatUtil() {
    }

    /** Sends a message to the player chat. */
    public static void chat(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal(PREFIX + message));
        }
    }

    /** Shows a message on the action bar (overlay). */
    public static void overlay(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.gui != null) {
            client.gui.setOverlayMessage(Component.literal(PREFIX + message), false);
        }
    }
}
