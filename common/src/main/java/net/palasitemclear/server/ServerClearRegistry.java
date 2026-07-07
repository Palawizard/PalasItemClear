package net.palasitemclear.server;

/**
 * Binds the active {@link ServerClearController} for the running dedicated server.
 */
public final class ServerClearRegistry {
    private static volatile ServerClearController controller;

    private ServerClearRegistry() {
    }

    public static void bind(ServerClearController value) {
        controller = value;
    }

    public static void unbind() {
        controller = null;
    }

    public static ServerClearController get() {
        return controller;
    }
}
