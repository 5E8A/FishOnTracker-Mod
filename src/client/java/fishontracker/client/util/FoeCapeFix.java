package fishontracker.client.util;

import net.fabricmc.loader.api.FabricLoader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class FoeCapeFix {

    public static boolean isArmorHidden() {
        if (!FabricLoader.getInstance().isModLoaded("fishonmcextras")) {
            return false;
        }

        try {
            Class<?> connHandlerClass = Class.forName("dannypx.foe.handler.logic.ConnectionHandler");
            Method instanceMethod = connHandlerClass.getMethod("instance");
            Object connHandler = instanceMethod.invoke(null);
            Method isOnServerMethod = connHandlerClass.getMethod("isOnServer");
            boolean isOnServer = (boolean) isOnServerMethod.invoke(connHandler);

            if (!isOnServer) {
                return false;
            }

            Class<?> configsClass = Class.forName("dannypx.foe.config.Configs");
            Field rendererConfigField = configsClass.getField("rendererConfig");
            Object rendererConfig = rendererConfigField.get(null);

            Field hideArmorField = rendererConfig.getClass().getField("hideArmor");
            Object hideArmorSupplier = hideArmorField.get(rendererConfig);

            Method getMethod = hideArmorSupplier.getClass().getMethod("get");
            return Boolean.TRUE.equals(getMethod.invoke(hideArmorSupplier));
        } catch (Throwable ignored) {
            return false;
        }
    }
}