package at.peckventure.rpc;

import net.arikia.dev.drpc.DiscordEventHandlers;
import net.arikia.dev.drpc.DiscordRPC;
import net.arikia.dev.drpc.DiscordRichPresence;

public class DiscordPresence {

    private static final String CLIENT_ID = "1358089026581565570"; // Deine Application-ID von Discord
    private static long startTimestamp;

    public static void start() {
        DiscordEventHandlers handlers = new DiscordEventHandlers.Builder().build();
        DiscordRPC.discordInitialize(CLIENT_ID, handlers, true);
        startTimestamp = System.currentTimeMillis();

        // Zeige standardmäßig das Hauptmenü an
        updatePresence("🧭 Im Hauptmenü", "mainmenu", "Peckventure");
    }

    public static void updateToIngame(String mapName) {
        updatePresence("⚔️ In einem Abenteuer - Karte: " + mapName, "ingame", "Abenteuer läuft");
    }

    public static void updateToLoading() {
        updatePresence("⏳ Lade neue Welt...", "loading", "Bitte warten...");
    }

    public static void updateToPaused() {
        updatePresence("⏸️ Spiel pausiert", "pause", "Zeit zum Durchatmen");
    }

    private static void updatePresence(String details, String imageKey, String imageText) {
        DiscordRichPresence presence = new DiscordRichPresence.Builder(details)
                .setBigImage(imageKey, imageText) // <- Bildname & Text darunter
                .setStartTimestamps(startTimestamp)
                .build();

        DiscordRPC.discordUpdatePresence(presence);
    }

    public static void runCallbacks() {
        DiscordRPC.discordRunCallbacks();
    }

    public static void stop() {
        DiscordRPC.discordShutdown();
    }
}
