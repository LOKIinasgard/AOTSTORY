package aotstory.story;

import aotstory.AotStory;
import aotstory.npc.Npcs;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameRules;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

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
     * Первый вход в мир = старт сюжета. Игрок появляется, рядом уже стоит стартовый NPC,
     * а мир "заморожен": нет смены дня/ночи, спавна мобов и погоды — до тех пор, пока не пройден диалог.
     */
    public static void onPlayerJoin(MinecraftServer server, ServerPlayerEntity player) {
        StoryState state = state(server);
        if (state.has(Flags.WORLD_STARTED)) {
            return; // мир уже начат раньше
        }
        state.add(Flags.WORLD_STARTED); // без событий: это служебный флаг
        lockWorld(server, true);
        Npcs.spawnInFront(player, "hannes", 3);
        player.sendMessage(Text.literal("Подойди к Ханнесу и поговори с ним (ПКМ).").formatted(Formatting.GRAY), false);
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
