package fishontracker.client.scan;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * Turns presses of the scan key bind into scan start/stop toggles.
 *
 * <p>Key binds cannot call {@link ScannerManager} directly — the tick event runs on the
 * client thread and drains every queued press, so held keys do not start and immediately
 * stop a session.</p>
 */
public final class ScanKeyHandler {

    private ScanKeyHandler() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ScanKeyBinds.isBound()) return;

            boolean toggleRequested = false;
            while (ScanKeyBinds.scanToggle.consumeClick()) {
                toggleRequested = true;
            }

            if (toggleRequested) {
                toggle();
            }
        });
    }

    /** Starts a session when idle, finishes and exports it when one is running. */
    public static void toggle() {
        if (ScannerManager.isScanning()) {
            ScannerManager.stopSession();
        } else {
            ScannerManager.startSession();
        }
    }
}
