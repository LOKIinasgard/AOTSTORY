package aotstory.npc;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.Map;

/** Список персонажей (id -> отображаемое имя) и спавн NPC. Новые персонажи добавляются в NAMES. */
public final class Npcs {
    private Npcs() {}

    private static final Map<String, String> NAMES = Map.of(
            "hannes", "Ханнес",
            "eren", "Эрен Йегер"
    );

    public static String displayName(String npcId) {
        return NAMES.getOrDefault(npcId, npcId);
    }

    public static StoryNpcEntity spawn(ServerWorld world, String npcId, Vec3d pos, float yaw) {
        StoryNpcEntity npc = new StoryNpcEntity(ModEntities.STORY_NPC, world);
        npc.setNpcId(npcId);
        npc.setCustomName(Text.literal(displayName(npcId)));
        npc.setCustomNameVisible(true);
        npc.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, 0.0f);
        world.spawnEntity(npc);
        return npc;
    }

    /** Ставит NPC на поверхность перед игроком (лицом к нему). */
    public static StoryNpcEntity spawnInFront(ServerPlayerEntity player, String npcId, int distance) {
        ServerWorld world = player.getServerWorld();
        Direction facing = player.getHorizontalFacing();
        BlockPos column = player.getBlockPos().offset(facing, distance);
        BlockPos top = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, column);
        return spawn(world, npcId, Vec3d.ofBottomCenter(top), facing.getOpposite().asRotation());
    }
}
