package at.peckventure.status;

import at.peckventure.entities.Player;

public interface StatusEffect {
    int level = 1;
    void apply(Player player);      // Was passiert, wenn aktiviert
    void remove(Player player);     // Was passiert, wenn entfernt
    boolean isExpired();            // Nur für Effekte relevant
    void update(float deltaTime);   // Effektdauer herunterzählen
}
