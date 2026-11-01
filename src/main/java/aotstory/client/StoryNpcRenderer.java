package aotstory.client;

import aotstory.AotStory;
import aotstory.npc.StoryNpcEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * NPC выглядит как игрок. Скин: если есть файл
 * assets/aotstory/textures/entity/npc/<npcId>.png (64x64, обычная "широкая" модель) — берётся он,
 * иначе стандартный Стив. Так для Ханнеса, Эрена и др. достаточно положить png.
 */
public class StoryNpcRenderer extends MobEntityRenderer<StoryNpcEntity, PlayerEntityModel<StoryNpcEntity>> {
    private static final Identifier DEFAULT = Identifier.ofVanilla("textures/entity/player/wide/steve.png");
    private static final Map<String, Identifier> CACHE = new HashMap<>();

    public StoryNpcRenderer(EntityRendererFactory.Context context) {
        super(context, new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public Identifier getTexture(StoryNpcEntity entity) {
        return CACHE.computeIfAbsent(entity.getNpcId(), npcId -> {
            Identifier custom = AotStory.id("textures/entity/npc/" + npcId + ".png");
            return MinecraftClient.getInstance().getResourceManager().getResource(custom).isPresent() ? custom : DEFAULT;
        });
    }
}
