package aotstory.npc;

import aotstory.AotStory;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModEntities {
    private ModEntities() {}

    public static final EntityType<StoryNpcEntity> STORY_NPC = Registry.register(
            Registries.ENTITY_TYPE,
            AotStory.id("story_npc"),
            EntityType.Builder.create(StoryNpcEntity::new, SpawnGroup.MISC)
                    .dimensions(0.6f, 1.8f)
                    .build("story_npc"));

    public static void register() {
        FabricDefaultAttributeRegistry.register(STORY_NPC, StoryNpcEntity.createNpcAttributes());
    }
}
