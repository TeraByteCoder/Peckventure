package at.peckventure.entities.mob;

import at.peckventure.Globals;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the maximum number of mobs that can exist simultaneously in the world.
 * Provides functionality to set caps for different mob types and check if more mobs can be spawned.
 */
public class MobCapManager {
    // Default cap for all mob types if not specified
    private static final int DEFAULT_CAP = 10;

    // Maps mob type IDs to their maximum allowed count
    private final Map<String, Integer> mobCaps = new HashMap<>();

    // Singleton instance
    private static MobCapManager instance;

    private MobCapManager() {
        // Private constructor for singleton
    }

    /**
     * Gets the singleton instance of MobCapManager.
     */
    public static synchronized MobCapManager getInstance() {
        if (instance == null) {
            instance = new MobCapManager();
        }
        return instance;
    }

    /**
     * Sets the maximum number of mobs of a specific type that can exist at once.
     *
     * @param mobTypeId The string ID of the mob type
     * @param maxCount The maximum allowed count
     */
    public void setMobCap(String mobTypeId, int maxCount) {
        mobCaps.put(mobTypeId, maxCount);
    }

    /**
     * Gets the current cap for a specific mob type.
     *
     * @param mobTypeId The string ID of the mob type
     * @return The maximum allowed count for the mob type
     */
    public int getMobCap(String mobTypeId) {
        return mobCaps.getOrDefault(mobTypeId, DEFAULT_CAP);
    }

    /**
     * Counts the current number of mobs of a specific type.
     *
     * @param mobTypeId The string ID of the mob type
     * @return The current count of the mob type
     */
    public int getCurrentCount(String mobTypeId) {
        int count = 0;

        for (Mob mob : Globals.mobs.values()) {
            int mobId = MobRegistry.getMobId(mob);
            String mobStringId = MobRegistry.getMobStringId(mobId);

            if (mobStringId != null && mobStringId.equals(mobTypeId)) {
                count++;
            }
        }

        return count;
    }

    /**
     * Checks if more mobs of a specific type can be spawned.
     *
     * @param mobTypeId The string ID of the mob type
     * @return True if more mobs can be spawned, false otherwise
     */
    public boolean canSpawnMore(String mobTypeId) {
        int currentCount = getCurrentCount(mobTypeId);
        int maxCount = getMobCap(mobTypeId);

        return currentCount < maxCount;
    }

    /**
     * Gets the total number of all mob types currently in the world.
     *
     * @return The total number of mobs
     */
    public int getTotalMobCount() {
        return Globals.mobs.size();
    }

    /**
     * Sets a global cap for all mob types.
     *
     * @param cap The maximum number of mobs allowed for each type
     */
    public void setGlobalCap(int cap) {
        // Since MobRegistry.getAllRegisteredMobTypes() might not exist in your code,
        // we'll update all currently set mob caps
        for (String mobTypeId : mobCaps.keySet()) {
            setMobCap(mobTypeId, cap);
        }
    }

    /**
     * Gets all mob types that have caps set.
     *
     * @return Array of mob type string IDs
     */
    public String[] getMobTypesWithCaps() {
        return mobCaps.keySet().toArray(new String[0]);
    }
}
