package fishontracker.client;
import fishontracker.client.scan.ScannerManager;
import fishontracker.client.scan.ScanKeyBinds;
import fishontracker.client.scan.ScanKeyHandler;

import com.mojang.blaze3d.platform.InputConstants;
import fishontracker.client.config.TrackerConfig;
import fishontracker.client.resource.ConfigurablePackSource;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class FishontrackerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        TrackerConfig.load();
        registerKeyBinds();
        ScanKeyHandler.register();
        ConfigurablePackSource.register();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("fot")
                .then(ClientCommands.literal("scan")
                    .then(ClientCommands.literal("start")
                        .executes(context -> {
                            ScannerManager.startSession();
                            return 1;
                        }))
                    .then(ClientCommands.literal("stop")
                        .executes(context -> {
                            ScannerManager.stopSession();
                            return 1;
                        }))
                )
            );
        });
    }

    private static void registerKeyBinds() {
        KeyMapping.Category category = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("fishontracker", "keybinds"));

        ScanKeyBinds.scanToggle = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.fishontracker.scan_toggle",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            category));
    }
}
