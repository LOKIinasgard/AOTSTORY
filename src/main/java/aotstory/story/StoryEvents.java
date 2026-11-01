package aotstory.story;

import aotstory.AotStory;
import aotstory.npc.Npcs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Реестр "флаг -> события". Событие не вызывается откуда-то ещё: оно живёт здесь и
 * запускается только когда StoryManager ставит соответствующий флаг.
 *
 * Цепочка сюжета читается прямо в registerAll():
 *   диалог с Ханнесом (флаг hannes_intro_done) -> Событие 1 -> доступен Эрен -> диалог с Эреном -> ...
 */
public final class StoryEvents {
    private StoryEvents() {}

    /** Титаны берутся из Danny's AOT по id сущности (мод id: dannys-aot). Если мода нет — событие не падает. */
    private static final Identifier COLOSSAL_TITAN = Identifier.of("dannys-aot", "colossal_titan");
    /** На каком расстоянии перед игроком появляется Колоссальный (блоков). */
    private static final int COLOSSAL_DISTANCE = 80;

    @FunctionalInterface
    public interface Handler {
        void run(MinecraftServer server, @Nullable ServerPlayerEntity cause);
    }

    private static final Map<String, List<Handler>> HANDLERS = new HashMap<>();

    public static void on(String flag, Handler handler) {
        HANDLERS.computeIfAbsent(flag, k -> new ArrayList<>()).add(handler);
    }

    static void fire(MinecraftServer server, String flag, @Nullable ServerPlayerEntity cause) {
        for (Handler handler : HANDLERS.getOrDefault(flag, List.of())) {
            handler.run(server, cause);
        }
    }

    /** Сюда по мере развития сюжета добавляются новые звенья цепочки. */
    public static void registerAll() {
        on(Flags.HANNES_INTRO_DONE, StoryEvents::eventWallBreach);
        // on(Flags.EREN_TALK_DONE, StoryEvents::event2...);   // следующее звено
    }

    /**
     * СОБЫТИЕ 1: пролог закончился. Размораживаем мир, показываем титул, ставим Эрена рядом
     * с игроком, а через 3 секунды на горизонте появляется Колоссальный титан.
     */
    private static void eventWallBreach(MinecraftServer server, @Nullable ServerPlayerEntity cause) {
        StoryManager.lockWorld(server, false);

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 70, 20));
            player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("Стена Мария")));
            player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("Год 845")));
            player.getServerWorld().playSound(null, player.getBlockPos(),
                    SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.AMBIENT, 4.0f, 0.5f);
        }

        if (cause != null) {
            // NPC_B становится доступным: появляется рядом с игроком, вызвавшим событие
            Npcs.spawnInFront(cause, "eren", 3);

            // Пауза для драматизма, потом титан. Игрока ищем заново: за 3 секунды он мог выйти.
            UUID playerId = cause.getUuid();
            StoryScheduler.later(60, () -> {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
                if (player != null) {
                    spawnColossal(player);
                }
            });
        }

        // Флаг "событие отыграло" ставим в конце: на него опирается диалог Эрена (requires)
        StoryManager.complete(server, Flags.EVENT_WALL_BREACH, cause);
    }

    /** Колоссальный титан из DAOT на поверхности перед игроком, на расстоянии COLOSSAL_DISTANCE. */
    private static void spawnColossal(ServerPlayerEntity player) {
        Optional<EntityType<?>> type = Registries.ENTITY_TYPE.getOrEmpty(COLOSSAL_TITAN);
        if (type.isEmpty()) {
            AotStory.LOGGER.warn("[story] в реестре нет {} — Danny's AOT не установлен?", COLOSSAL_TITAN);
            player.sendMessage(Text.literal("(Danny's AOT не найден — Колоссальный титан не появился)"), false);
            return;
        }
        ServerWorld world = player.getServerWorld();
        Direction facing = player.getHorizontalFacing();
        BlockPos column = player.getBlockPos().offset(facing, COLOSSAL_DISTANCE);
        BlockPos top = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, column);

        Entity titan = type.get().spawn(world, top, SpawnReason.EVENT);
        if (titan == null) {
            AotStory.LOGGER.warn("[story] Колоссальный титан не создался на {}", top);
            return;
        }
        world.playSound(null, top, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 8.0f, 0.4f);
        AotStory.LOGGER.info("[story] Колоссальный титан появился на {}", top.toShortString());
    }
}
