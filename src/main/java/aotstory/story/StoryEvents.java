package aotstory.story;

import aotstory.npc.Npcs;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Реестр "флаг -> события". Событие не вызывается откуда-то ещё: оно живёт здесь и
 * запускается только когда StoryManager ставит соответствующий флаг.
 *
 * Цепочка сюжета читается прямо в registerAll():
 *   диалог с Ханнесом (флаг hannes_intro_done) -> Событие 1 -> доступен Эрен -> диалог с Эреном -> ...
 */
public final class StoryEvents {
    private StoryEvents() {}

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
     * СОБЫТИЕ 1 (заглушка): пролог закончился. Размораживаем мир, показываем титул,
     * ставим Эрена рядом с игроком. Появление Колоссального титана и пр. добавим следующим шагом.
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

        // NPC_B становится доступным: появляется рядом с игроком, вызвавшим событие
        if (cause != null) {
            Npcs.spawnInFront(cause, "eren", 3);
        }

        // Флаг "событие отыграло" ставим в конце: на него опирается диалог Эрена (requires)
        StoryManager.complete(server, Flags.EVENT_WALL_BREACH, cause);
    }
}
