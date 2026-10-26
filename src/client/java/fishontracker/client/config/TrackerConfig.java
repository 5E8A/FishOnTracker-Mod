package fishontracker.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import fishontracker.Fishontracker;
import fishontracker.client.resource.ConfigurablePackSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;

public class TrackerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // config/fishontracker/config.json
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("fishontracker");
    private static final File FILE = CONFIG_DIR.resolve("config.json").toFile();

    private static final String FISHONMC_EXTRAS = "fishonmcextras";
    private static final String SODIUM = "sodium";

    public static TrackerConfig INSTANCE = new TrackerConfig();

    public boolean forceCapeFix = true;
    public boolean hideSodiumWarning = true;
    public boolean coloredAmuletStars = true;
    public boolean includeProfileData = true;
    public boolean leavesFix = true;
    public boolean shortRankIcons = false;
    public boolean tooltipFix = true;

    public static void load() {
        if (FILE.exists()) {
            try (FileReader reader = new FileReader(FILE)) {
                INSTANCE = GSON.fromJson(reader, TrackerConfig.class);
                if (INSTANCE == null) {
                    INSTANCE = new TrackerConfig();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            save();
        }
    }

    public static void save() {
        try {
            File dir = FILE.getParentFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            try (FileWriter writer = new FileWriter(FILE)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            ConfigurablePackSource.apply(client);
        }
    }

    /** Folder inside the mod resources that holds the option description screenshots. */
    private static final String DESCRIPTION_IMAGE_DIR = "textures/gui/config/";

    /** Description without a screenshot. */
    private static OptionDescription description(String text) {
        return OptionDescription.of(Component.literal(text));
    }

    /**
     * Description with a screenshot drawn above the text by YACL. YACL scales the image to the
     * width of the description pane, so the given size only defines its aspect ratio and should
     * match the pixel size of the texture to avoid stretching.
     */
    private static OptionDescription description(String text, String imageFile, int width, int height) {
        return OptionDescription.createBuilder()
            .text(Component.literal(text))
            .image(Fishontracker.id(DESCRIPTION_IMAGE_DIR + imageFile), width, height)
            .build();
    }

    public static Screen createScreen(Screen parent) {
        boolean extrasLoaded = FabricLoader.getInstance().isModLoaded(FISHONMC_EXTRAS);
        boolean sodiumLoaded = FabricLoader.getInstance().isModLoaded(SODIUM);

        return YetAnotherConfigLib.createBuilder()
            .title(Component.literal("FishOnTracker Config"))
            .category(generalCategory())
            .category(otherCategory(extrasLoaded, sodiumLoaded))
            .save(TrackerConfig::save)
            .build()
            .generateScreen(parent);
    }

    private static ConfigCategory generalCategory() {
        return ConfigCategory.createBuilder()
            .name(Component.literal("General"))
            .option(Option.<Boolean>createBuilder()
                .name(Component.literal("Include Profile Data"))
                .description(description(
                    "Includes your username, level and cash balance in the exported JSON. "
                        + "Turn this off if you only want to share the item list."))
                .binding(
                    true,
                    () -> INSTANCE.includeProfileData,
                    val -> INSTANCE.includeProfileData = val
                )
                .controller(BooleanControllerBuilder::create)
                .build())
            .build();
    }

    private static ConfigCategory otherCategory(boolean extrasLoaded, boolean sodiumLoaded) {
        return ConfigCategory.createBuilder()
            .name(Component.literal("Other"))
            .group(OptionGroup.createBuilder()
                .name(Component.literal("FishOnMC Extras"))
                .option(Option.<Boolean>createBuilder()
                    .name(Component.literal("Cape Offset Fix"))
                    .description(description(
                        extrasLoaded
                            ? "[FishOnMC Extras] Removes the gap between the cape and the player's back when toggling off armor rendering."
                            : "Unavailable - FishOnMC Extras is not installed.",
                        "cape_fix.png", 800, 400))
                    .binding(
                        true,
                        () -> INSTANCE.forceCapeFix,
                        val -> INSTANCE.forceCapeFix = val
                    )
                    .available(extrasLoaded)
                    .controller(BooleanControllerBuilder::create)
                    .build())
                .option(Option.<Boolean>createBuilder()
                    .name(Component.literal("Colored Pet Amulet Stars"))
                    .description(description(
                        extrasLoaded
                            ? "[FishOnMC Extras] Draws a colour matched star for the amulet equipped on a pet instead of the default yellow one."
                            : "Unavailable - FishOnMC Extras is not installed.",
                        "amulet_stars.png", 273, 60))
                    .binding(
                        true,
                        () -> INSTANCE.coloredAmuletStars,
                        val -> INSTANCE.coloredAmuletStars = val
                    )
                    .available(extrasLoaded)
                    .controller(BooleanControllerBuilder::create)
                    .build())
                .build())
            .group(OptionGroup.createBuilder()
                .name(Component.literal("Sodium"))
                .option(Option.<Boolean>createBuilder()
                    .name(Component.literal("Hide Incompatible Pack Warning"))
                    .description(description(
                        sodiumLoaded
                            ? "[Sodium] Hides the visual Sodium warning on screen while keeping details in the log files."
                            : "Unavailable - Sodium is not installed.",
                        "sodium_warning.png", 810, 99))
                    .binding(
                        true,
                        () -> INSTANCE.hideSodiumWarning,
                        val -> INSTANCE.hideSodiumWarning = val
                    )
                    .available(sodiumLoaded)
                    .controller(BooleanControllerBuilder::create)
                    .build())
                .option(Option.<Boolean>createBuilder()
                    .name(Component.literal("Leaves Fix"))
                    .description(description(
                        sodiumLoaded
                            ? "[Sodium] Removes broken leaves when using Sodium."
                            : "Unavailable - Sodium is not installed, vanilla leaves render correctly anyway.",
                        "leaves_fix.png", 585, 255))
                    .binding(
                        true,
                        () -> INSTANCE.leavesFix,
                        val -> INSTANCE.leavesFix = val
                    )
                    .available(sodiumLoaded)
                    .controller(BooleanControllerBuilder::create)
                    .build())
                .build())
            .group(OptionGroup.createBuilder()
                .name(Component.literal("Misc"))
                .option(Option.<Boolean>createBuilder()
                    .name(Component.literal("Short Rank Icons"))
                    .description(description(
                        "Smaller icons for ranks."))
                    .binding(
                        false,
                        () -> INSTANCE.shortRankIcons,
                        val -> INSTANCE.shortRankIcons = val
                    )
                    .controller(BooleanControllerBuilder::create)
                    .build())
                .option(Option.<Boolean>createBuilder()
                    .name(Component.literal("Tooltip Fix"))
                    .description(description(
                        "Extends the fancy item tooltip frame to match the text width.",
                        "tooltip_fix.png", 1351, 636))
                    .binding(
                        true,
                        () -> INSTANCE.tooltipFix,
                        val -> INSTANCE.tooltipFix = val
                    )
                    .controller(BooleanControllerBuilder::create)
                    .build())
                .build())
            .build();
    }
}
