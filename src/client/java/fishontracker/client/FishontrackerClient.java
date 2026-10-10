package fishontracker.client;

import fishontracker.client.config.TrackerConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;

public class FishontrackerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        TrackerConfig.load();
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
}