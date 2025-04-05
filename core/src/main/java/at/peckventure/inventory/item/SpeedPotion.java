package at.peckventure.inventory.item;

import at.peckventure.entities.Player;
import at.peckventure.status.EffectRegistry;
import at.peckventure.status.SpeedBoostEffect;
import com.badlogic.gdx.graphics.Texture;

import java.util.Objects;

public class SpeedPotion extends Item
{
    public final int MAX_STACK_SIZE = 1;
    public SpeedPotion(String id, String name, Texture texture)
    {
        super(id, name, texture);
    }

    @Override
    public void onUse(Player player)
    {
        player.addEffect(Objects.requireNonNull(EffectRegistry.createEffect("speed_boost", 3, 10)));
    }
}
