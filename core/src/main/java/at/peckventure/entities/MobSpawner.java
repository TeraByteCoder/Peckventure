package at.peckventure.entities;

import at.peckventure.entities.mob.Beetle;
import at.peckventure.entities.mob.Mob;
import at.peckventure.entities.mob.MobCapManager;
import at.peckventure.entities.mob.MobRegistration;
import at.peckventure.entities.mob.MobRegistry;
import at.peckventure.world.SpawnLocationSearcher;
import at.peckventure.world.block.Block;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class MobSpawner {
    private final World world;
    private Player player; // Single player mode
    private Set<? extends Player> players; // Multi-player mode with generic wildcard
    private final Random random = new Random();
    private float spawnTimer = 0f;
    private final float spawnInterval = 5f; // Alle 5 Sekunden
    private final MobCapManager mobCapManager;
    private final boolean isMultiPlayer;

    // Liste der Mob-Typen, die gespawnt werden können
    private final List<String> spawnableMobTypes = new ArrayList<>();

    // Konstruktor für Single-Player-Modus
    public MobSpawner(World world, Player player) {
        this.world = world;
        this.player = player;
        this.players = null;
        this.isMultiPlayer = false;
        this.mobCapManager = MobCapManager.getInstance();

        // Standard-Mob-Typen zum Spawnen hinzufügen
        spawnableMobTypes.add(MobRegistration.BEETLE_STRING_ID);

        // Standard-Caps für jeden Mob-Typ setzen
        mobCapManager.setMobCap(MobRegistration.BEETLE_STRING_ID, 5);
    }

    // Konstruktor für Multi-Player-Modus - akzeptiert jede Art von Set, solange die Elemente Player oder eine Unterklasse sind
    public MobSpawner(World world, Set<? extends Player> players) {
        this.world = world;
        this.player = null;
        this.players = players;
        this.isMultiPlayer = true;
        this.mobCapManager = MobCapManager.getInstance();

        // Standard-Mob-Typen zum Spawnen hinzufügen
        spawnableMobTypes.add(MobRegistration.BEETLE_STRING_ID);

        // Standard-Caps für jeden Mob-Typ setzen
        mobCapManager.setMobCap(MobRegistration.BEETLE_STRING_ID, 5);
    }

    /**
     * Setzt das Limit für einen bestimmten Mob-Typ.
     *
     * @param mobTypeId Die String-ID des Mob-Typs
     * @param cap Das maximale Limit
     */
    public void setMobCap(String mobTypeId, int cap) {
        mobCapManager.setMobCap(mobTypeId, cap);
    }

    /**
     * Fügt einen neuen Mob-Typ zur Liste der spawmbaren Mobs hinzu.
     *
     * @param mobTypeId Die String-ID des Mob-Typs
     * @param cap Das maximale Limit (optional)
     */
    public void addSpawnableMobType(String mobTypeId, int cap) {
        if (!spawnableMobTypes.contains(mobTypeId)) {
            spawnableMobTypes.add(mobTypeId);
            mobCapManager.setMobCap(mobTypeId, cap);
        }
    }

    public void update(float delta) {
        spawnTimer += delta;
        if (spawnTimer >= spawnInterval) {
            spawnTimer = 0f;
            trySpawnMob();
        }
    }

    /**
     * Aktualisiert die Spielerliste für den Multiplayer-Modus.
     * Hilfreich, wenn sich die Spielerliste dynamisch ändert.
     *
     * @param players Die neue Spielerliste
     */
    public void updatePlayerList(Set<? extends Player> players) {
        if (isMultiPlayer) {
            this.players = players;
        }
    }

    /**
     * Fügt einen Spieler zur Spielerliste im Multiplayer-Modus hinzu.
     *
     * @param player Der hinzuzufügende Spieler
     */
    public void addPlayer(Player player) {
        if (isMultiPlayer && player != null) {
            if (this.players == null) {
                // Da wir nicht direkt zur players-Set hinzufügen können (wegen des Wildcard-Typs),
                // müssen wir eine neue Set erstellen
                HashSet<Player> newPlayers = new HashSet<>();
                newPlayers.add(player);
                this.players = newPlayers;
            } else {
                // Wir müssen einen Cast zu einem mutibaren Set durchführen
                // HINWEIS: Dies ist nur sicher, wenn alle Aufrufe dieser Methode durch den Server erfolgen
                // und dieser die Kontrolle über das Set hat
                try {
                    @SuppressWarnings("unchecked")
                    Set<Player> mutablePlayers = (Set<Player>) this.players;
                    mutablePlayers.add(player);
                } catch (ClassCastException e) {
                    System.err.println("Fehler beim Hinzufügen eines Spielers: " + e.getMessage());
                    // Alternative Lösung: Erstelle ein neues Set mit den alten Spielern plus dem neuen
                    HashSet<Player> newPlayers = new HashSet<>();
                    for (Player p : this.players) {
                        newPlayers.add(p);
                    }
                    newPlayers.add(player);
                    this.players = newPlayers;
                }
            }
        }
    }

    /**
     * Entfernt einen Spieler aus der Spielerliste im Multiplayer-Modus.
     *
     * @param player Der zu entfernende Spieler
     */
    public void removePlayer(Player player) {
        if (isMultiPlayer && this.players != null) {
            try {
                @SuppressWarnings("unchecked")
                Set<Player> mutablePlayers = (Set<Player>) this.players;
                mutablePlayers.remove(player);
            } catch (ClassCastException e) {
                System.err.println("Fehler beim Entfernen eines Spielers: " + e.getMessage());
                // Alternative Lösung: Erstelle ein neues Set ohne den zu entfernenden Spieler
                HashSet<Player> newPlayers = new HashSet<>();
                for (Player p : this.players) {
                    if (!p.equals(player)) {
                        newPlayers.add(p);
                    }
                }
                this.players = newPlayers;
            }
        }
    }

    private void trySpawnMob() {
        // Check if we can spawn more mobs at all
        if (spawnableMobTypes.isEmpty()) {
            return;
        }

        // Wähle zufällig einen Mob-Typ zum Spawnen
        String mobTypeToSpawn = getRandomMobTypeToSpawn();
        if (mobTypeToSpawn == null) {
            // Kein Mob-Typ kann gespawnt werden (alle haben ihre Caps erreicht)
            return;
        }

        // Try to find a valid spawn location
        SpawnLocation location = findValidSpawnLocation();
        if (location != null) {
            // Convert block coordinates to world coordinates for mob creation
            float spawnWorldX = location.x * Block.BLOCK_SIZE;
            float spawnWorldY = location.y * Block.BLOCK_SIZE;

            System.out.println("Spawning " + mobTypeToSpawn + " at world coordinates: " + spawnWorldX + ", " + spawnWorldY);

            // Create the mob with world coordinates
            Mob mob = MobRegistry.createMob(mobTypeToSpawn, world, spawnWorldX, spawnWorldY);

            // Make sure the mob is initialized properly
            if (mob != null) {
                // You might need to do additional setup for the mob here
                System.out.println("Successfully spawned " + mobTypeToSpawn + " with ID: " + mob.hashCode());
                System.out.println("Current count: " + mobCapManager.getCurrentCount(mobTypeToSpawn) +
                    " / " + mobCapManager.getMobCap(mobTypeToSpawn));
            }
        } else {
            System.out.println("Could not find valid spawn location for " + mobTypeToSpawn);
        }
    }

    /**
     * Findet eine gültige Spawn-Position, die mindestens 20 Blöcke von allen Spielern entfernt ist.
     *
     * @return SpawnLocation-Objekt mit X- und Y-Koordinaten oder null, wenn keine gültige Position gefunden wurde
     */
    private SpawnLocation findValidSpawnLocation() {
        // Get all player positions
        List<PlayerPosition> playerPositions = new ArrayList<>();

        if (isMultiPlayer) {
            // Im Multiplayer-Modus: Alle Spieler berücksichtigen
            for (Player p : players) {
                if (p != null) {
                    int playerX = (int) p.getX() / Block.BLOCK_SIZE;
                    playerPositions.add(new PlayerPosition(playerX, p));
                }
            }
        } else {
            // Im Singleplayer-Modus: Nur den einen Spieler berücksichtigen
            int playerX = (int) player.getX() / Block.BLOCK_SIZE;
            playerPositions.add(new PlayerPosition(playerX, player));
        }

        if (playerPositions.isEmpty()) {
            return null; // Keine Spieler gefunden
        }

        // Versuche 10 mal, eine gültige Position zu finden
        for (int attempt = 0; attempt < 10; attempt++) {
            // Wähle zufällig einen Spieler als Referenzpunkt
            PlayerPosition referencePlayer = playerPositions.get(random.nextInt(playerPositions.size()));
            int referenceX = referencePlayer.blockX;

            // Generiere eine zufällige Distanz zwischen 20 und 40 Blöcken
            int distance = (int) randomRange(20, 40);

            // Zufällig entscheiden, ob links oder rechts vom Referenz-Spieler
            boolean spawnLeft = random.nextBoolean();
            int dx = spawnLeft ? -distance : distance;

            int candidateX = referenceX + dx;

            // Überprüfe, ob die Position mindestens 20 Blöcke von ALLEN Spielern entfernt ist
            boolean isFarEnoughFromAllPlayers = true;
            for (PlayerPosition p : playerPositions) {
                int distanceToPlayer = Math.abs(candidateX - p.blockX);
                if (distanceToPlayer < 20) {
                    isFarEnoughFromAllPlayers = false;
                    break;
                }
            }

            // Wenn die Position zu nah an einem Spieler ist, versuche erneut
            if (!isFarEnoughFromAllPlayers) {
                continue;
            }

            // Get valid Y position in block coordinates
            int candidateY = SpawnLocationSearcher.getValidY(candidateX);

            // If we found a valid spawn position
            if (candidateY != 300) { // 300 is the default return when no valid position is found
                return new SpawnLocation(candidateX, candidateY);
            }
        }

        // Keine gültige Position gefunden
        return null;
    }

    /**
     * Wählt zufällig einen Mob-Typ aus, der noch gespawnt werden kann.
     *
     * @return Die String-ID des zu spawnenden Mob-Typs oder null, wenn kein Mob gespawnt werden kann
     */
    private String getRandomMobTypeToSpawn() {
        // Liste der Mob-Typen, die noch gespawnt werden können
        List<String> availableMobTypes = new ArrayList<>();

        for (String mobType : spawnableMobTypes) {
            if (mobCapManager.canSpawnMore(mobType)) {
                availableMobTypes.add(mobType);
            }
        }

        if (availableMobTypes.isEmpty()) {
            return null;
        }

        // Zufällig einen Mob-Typ auswählen
        int index = random.nextInt(availableMobTypes.size());
        return availableMobTypes.get(index);
    }

    private float randomRange(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    /**
     * Hilfsklasse zur Speicherung von Spielerpositionen
     */
    private static class PlayerPosition {
        final int blockX;
        final Player player;

        PlayerPosition(int blockX, Player player) {
            this.blockX = blockX;
            this.player = player;
        }
    }

    /**
     * Hilfsklasse zur Speicherung von Spawn-Positionen
     */
    private static class SpawnLocation {
        final int x;
        final int y;

        SpawnLocation(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
