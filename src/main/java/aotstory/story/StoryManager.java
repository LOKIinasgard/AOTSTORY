package aotstory.story;

import aotstory.AotStory;
import aotstory.npc.Npcs;
import aotstory.npc.StoryNpcEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameRules;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

/**
 * ЕДИНЫЙ ШЛЮЗ прогресса сюжета.
 *
 * Ключевая идея архитектуры: ничто в моде не запускает события само по таймеру или по тикам.
 * Событие стартует ТОЛЬКО как реакция на установку флага (StoryEvents), а флаг ставится ТОЛЬКО
 * здесь, и только в ответ на завершённый диалог (DialogueManager). Поэтому "пока не поговоришь —
 * ничего не происходит" выполняется по построению, а не россыпью проверок по коду.
 */
public final class StoryManager {
    private StoryManager() {}

    /** Прогресс лежит в Overworld, а не в "текущем" мире: иначе в других измерениях (в т.ч. мире DAOT) прогресс был бы свой. */
    public static StoryState state(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(StoryState.TYPE, StoryState.KEY);
    }

    public static boolean has(MinecraftServer server, String flag) {
        return state(server).has(flag);
    }

    public static Set<String> flags(MinecraftServer server) {
        return state(server).all();
    }

    /**
     * Отметить, что что-то произошло. Идемпотентно: повторный вызов с тем же флагом ничего не делает,
     * поэтому событие не может сработать дважды.
     *
     * @param cause игрок, чьё действие вызвало флаг (для событий "рядом с игроком"), может быть null
     * @return true, если флаг поставлен впервые
     */
    public static boolean complete(MinecraftServer server, String flag, @Nullable ServerPlayerEntity cause) {
        if (!state(server).add(flag)) {
            return false;
        }
        AotStory.LOGGER.info("[story] флаг установлен: {}", flag);
        StoryEvents.fire(server, flag, cause);
        return true;
    }

    /** Только для отладочных команд. */
    public static boolean clearFlag(MinecraftServer server, String flag) {
        return state(server).remove(flag);
    }

    /** Только для отладочных команд: полный сброс сюжета. */
    public static void reset(MinecraftServer server) {
        state(server).clear();
        lockWorld(server, false);
    }

    /**
     * Вход игрока в мир. Если сюжет ещё не начат — через пару секунд (когда игрок и чанки
     * точно загружены) ставим стартового NPC. Флаг "мир начат" ставится ТОЛЬКО после того,
     * как NPC реально появился; если не вышло — повторяем попытку.
     */
    public static void onPlayerJoin(MinecraftServer server, ServerPlayerEntity player) {
        if (state(server).has(Flags.WORLD_STARTED)) {
            return;
        }
        scheduleStart(server, player.getUuid(), 1);
    }

    private static void scheduleStart(MinecraftServer server, UUID playerId, int attempt) {
        StoryScheduler.later(attempt == 1 ? 40 : 100, () -> {
            if (state(server).has(Flags.WORLD_STARTED)) return;
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            if (player == null) return; // игрок вышел: попробуем при следующем входе
            if (!startWorld(server, player) && attempt < 5) {
                scheduleStart(server, playerId, attempt + 1);
            }
        });
    }

    /**
     * Старт сюжета: ставим Ханнеса перед игроком и "замораживаем" мир (нет смены дня/ночи,
     * спавна мобов и погоды) до конца первого диалога. Вызывается и командой /aotstory start.
     *
     * @return true, если Ханнес появился
     */
    public static boolean startWorld(MinecraftServer server, ServerPlayerEntity player) {
        StoryNpcEntity hannes = Npcs.spawnInFront(player, "hannes", 3);
        if (hannes == null) {
            AotStory.LOGGER.warn("[story] не удалось поставить Ханнеса, повторим позже");
            return false;
        }
        state(server).add(Flags.WORLD_STARTED); // без событий: служебный флаг
        lockWorld(server, true);
        player.sendMessage(Text.literal("Ханнес ждёт тебя (" + hannes.getBlockPos().toShortString()
                + "). Подойди и нажми ПКМ.").formatted(Formatting.GRAY), false);
        return true;
    }

    /**
     * Блокировка/разблокировка "мировых" событий. Пока сюжет не начался, мир стоит на месте.
     * Осторожно: правила меняются глобально — если у тебя до этого было выключено, например,
     * doWeatherCycle, после разблокировки оно станет true.
     */
    public static void lockWorld(MinecraftServer server, boolean locked) {
        GameRules rules = server.getGameRules();
        rules.get(GameRules.DO_DAYLIGHT_CYCLE).set(!locked, server);
        rules.get(GameRules.DO_MOB_SPAWNING).set(!locked, server);
        rules.get(GameRules.DO_WEATHER_CYCLE).set(!locked, server);
    }
}
