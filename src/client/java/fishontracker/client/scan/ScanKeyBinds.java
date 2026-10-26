package fishontracker.client.scan;

import net.minecraft.client.KeyMapping;

/**
 * Global key binds for the scan session and their category.
 *
 * <p>Split out of the config because mod initializers have to register the binds before
 * any config screen exists, while the config screen only links to them.</p>
 */
public final class ScanKeyBinds {

    public static KeyMapping scanToggle;

    private ScanKeyBinds() {
    }

    public static boolean isBound() {
        return scanToggle != null;
    }
}
