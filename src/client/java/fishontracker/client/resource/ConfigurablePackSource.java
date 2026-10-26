package fishontracker.client.resource;

import fishontracker.client.config.TrackerConfig;
import fishontracker.client.tooltip.TooltipFixContext;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.resources.IoSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Ships the mod's optional overrides (leaves fix, short rank icons) as built-in resource packs
 * whose content is gated by their config toggle.
 *
 * <p>Unlike a normal built-in pack the packs are never written to {@code options.resourcePacks}.
 * They are registered as required packs, so the repository always keeps them selected, and the
 * only thing a toggle does is decide whether the pack serves any resources at all. Turning a
 * toggle on or off therefore only needs a resource reload, which makes the packs work from the
 * first launch, from the main menu, and without touching the vanilla pack screen.</p>
 *
 * <p>The pack files are read straight out of the mod jar through {@link ModContainer#findPath},
 * so nothing is ever written to the config directory.</p>
 */
public final class ConfigurablePackSource implements RepositorySource {

    private static final Logger LOGGER = LoggerFactory.getLogger("fishontracker");

    private static final String NAMESPACE = "fishontracker";

    /** Repository source instance used by the pack repository hook. */
    public static final ConfigurablePackSource INSTANCE = new ConfigurablePackSource();

    /** Packs owned by the mod, keyed by their repository id. */
    private static final Map<String, Entry> PACKS = new LinkedHashMap<>();

    /** Toggle state the packs were last served with, or null before the first pack load. */
    private static boolean[] appliedState;

    private ConfigurablePackSource() {
    }

    /**
     * Registers the mod's packs. Called during client initialisation; the actual registration
     * into the pack repository happens via {@code BuiltInPackSourceMixin}.
     */
    public static void register() {
        add("leaves_fix", "Leaves Fix", () -> TrackerConfig.INSTANCE.leavesFix);
        add("short_rank_icons", "Short Rank Icons", () -> TrackerConfig.INSTANCE.shortRankIcons);
        cleanupLegacyExtraction();
    }

    /** Whether the pack is one of ours and therefore hidden from the vanilla pack screen. */
    public static boolean isOwnPack(Pack pack) {
        return pack != null && PACKS.containsKey(pack.getId());
    }

    /**
     * Re-applies the toggles, but only when they actually changed since the packs were last
     * served. Other config options never need a resource reload, so saving the config alone
     * is a no-op here.
     */
    public static void apply(Minecraft client) {
        if (client == null || client.options == null || client.getResourcePackRepository() == null) return;

        boolean[] current = currentState();
        if (appliedState != null && Arrays.equals(current, appliedState)) return;

        // The reload re-runs loadPacks(), which records the new state as applied.
        client.reloadResourcePacks();
    }

    /** Snapshot of the pack toggles in {@link #PACKS} order. */
    private static boolean[] currentState() {
        boolean[] state = new boolean[PACKS.size()];
        int index = 0;
        for (Entry entry : PACKS.values()) {
            state[index++] = entry.enabled().getAsBoolean();
        }
        return state;
    }

    @Override
    public void loadPacks(Consumer<Pack> consumer) {
        // A reload swaps the font and the tooltip assets, so any width carried over from a
        // half-finished frame is no longer meaningful.
        TooltipFixContext.reset();

        // The packs are about to serve their resources with the current toggles, so remember
        // that as the applied state. Everything is registered even when off; the gate only
        // decides whether any resources are handed out.
        appliedState = currentState();

        ModContainer container = FabricLoader.getInstance().getModContainer("fishontracker").orElse(null);

        for (Map.Entry<String, Entry> entry : PACKS.entrySet()) {
            Path root = container == null
                ? null
                : container.findPath("resourcepacks/" + entry.getValue().path()).orElse(null);

            if (root == null) {
                LOGGER.warn("Missing bundled pack resourcepacks/{}", entry.getValue().path());
                continue;
            }

            PackLocationInfo location = new PackLocationInfo(
                entry.getKey(),
                Component.literal("FishOnTracker - " + entry.getValue().displayName()),
                PackSource.BUILT_IN,
                Optional.<KnownPack>empty());

            PackResources resources = new GatedPackResources(location, root, entry.getValue().enabled());
            Pack pack = Pack.readMetaAndCreate(
                location,
                new FixedResources(resources),
                PackType.CLIENT_RESOURCES,
                new PackSelectionConfig(true, Pack.Position.TOP, false));

            if (pack == null) {
                LOGGER.warn("Could not create built-in resource pack {}", entry.getKey());
                continue;
            }

            consumer.accept(pack);
        }
    }

    private static void add(String path, String displayName, BooleanSupplier enabled) {
        PACKS.put(NAMESPACE + ":" + path, new Entry(displayName, enabled, path));
    }

    /**
     * Removes files written by older mod versions: the extracted {@code resources} folder,
     * which used to hold the packs before they were served straight from the jar, and the
     * {@code amulet_stars.json} mapping, which is now fixed in code.
     */
    private static void cleanupLegacyExtraction() {
        Path configRoot = FabricLoader.getInstance().getConfigDir().resolve("fishontracker");

        Path legacyResources = configRoot.resolve("resources");
        if (Files.exists(legacyResources)) {
            try (Stream<Path> stream = Files.walk(legacyResources)) {
                List<Path> paths = stream.sorted((a, b) -> b.getNameCount() - a.getNameCount()).toList();
                for (Path path : paths) {
                    Files.deleteIfExists(path);
                }
                LOGGER.info("Removed legacy extracted packs at {}", legacyResources);
            } catch (IOException e) {
                LOGGER.warn("Could not clean up legacy extracted packs at {}", legacyResources, e);
            }
        }

        Path legacyAmuletStars = configRoot.resolve("amulet_stars.json");
        if (Files.exists(legacyAmuletStars)) {
            try {
                Files.deleteIfExists(legacyAmuletStars);
                LOGGER.info("Removed legacy amulet star mapping at {}", legacyAmuletStars);
            } catch (IOException e) {
                LOGGER.warn("Could not clean up legacy amulet star mapping at {}", legacyAmuletStars, e);
            }
        }
    }

    private static final class Entry {
        private final String displayName;
        private final BooleanSupplier enabled;
        private final String path;

        private Entry(String displayName, BooleanSupplier enabled, String path) {
            this.displayName = displayName;
            this.enabled = enabled;
            this.path = path;
        }

        private String displayName() {
            return displayName;
        }

        private BooleanSupplier enabled() {
            return enabled;
        }

        private String path() {
            return path;
        }
    }

    private record FixedResources(PackResources pack) implements Pack.ResourcesSupplier {
        @Override
        public PackResources openPrimary(PackLocationInfo location) {
            return pack;
        }

        @Override
        public PackResources openFull(PackLocationInfo location, Pack.Metadata metadata) {
            return pack;
        }
    }

    /**
     * Serves the bundled pack resources, but only while its config toggle is on. When the
     * toggle is off every lookup reports "nothing here", which makes the always selected pack
     * behave exactly as if it was disabled.
     */
    private static final class GatedPackResources extends PathPackResources {

        private final BooleanSupplier enabled;

        private GatedPackResources(PackLocationInfo location, Path root, BooleanSupplier enabled) {
            super(location, root);
            this.enabled = enabled;
        }

        @Override
        public IoSupplier<InputStream> getRootResource(String... elements) {
            // pack.mcmeta must always be readable so the pack can be registered even while
            // disabled; only the actual resources are gated by the toggle.
            if (elements.length == 1 && "pack.mcmeta".equals(elements[0])) {
                return super.getRootResource(elements);
            }
            return enabled.getAsBoolean() ? super.getRootResource(elements) : null;
        }

        @Override
        public IoSupplier<InputStream> getResource(PackType type, net.minecraft.resources.Identifier id) {
            return enabled.getAsBoolean() ? super.getResource(type, id) : null;
        }

        @Override
        public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
            if (enabled.getAsBoolean()) {
                super.listResources(type, namespace, path, output);
            }
        }

        @Override
        public Set<String> getNamespaces(PackType type) {
            return enabled.getAsBoolean() ? super.getNamespaces(type) : Set.of();
        }
    }
}
