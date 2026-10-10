package fishontracker.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import net.fabricmc.loader.api.FabricLoader;
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

    public static TrackerConfig INSTANCE = new TrackerConfig();

    // Pola konfiguracyjne:
    public boolean forceCapeFix = true;
    public boolean hideSodiumWarning = true;

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
    }

public static Screen createScreen(Screen parent) {
        return YetAnotherConfigLib.createBuilder()
            .title(Component.literal("FishOnTracker Config"))
            .category(ConfigCategory.createBuilder()
                .name(Component.literal("General"))
                .option(Option.<Boolean>createBuilder()
                    .name(Component.literal("Fix Cape Offset"))
                    .description(OptionDescription.of(Component.literal("Removes the gap between the cape and the player's back when fishonmcextras toggles armor rendering.")))
                    .binding(
                        true,
                        () -> INSTANCE.forceCapeFix,
                        val -> INSTANCE.forceCapeFix = val
                    )
                    .controller(BooleanControllerBuilder::create)
                    .build())
                .option(Option.<Boolean>createBuilder()
                    .name(Component.literal("Hide Incompatible Packs Warning"))
                    .description(OptionDescription.of(Component.literal("Hides the visual Sodium warning on screen while keeping details in the log files.")))
                    .binding(
                        true,
                        () -> INSTANCE.hideSodiumWarning,
                        val -> INSTANCE.hideSodiumWarning = val
                    )
                    .controller(BooleanControllerBuilder::create)
                    .build())
                .build())
            .save(TrackerConfig::save)
            .build()
            .generateScreen(parent);
    }
}