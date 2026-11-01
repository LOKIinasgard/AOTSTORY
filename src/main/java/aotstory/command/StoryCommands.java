package aotstory.command;

import aotstory.npc.Npcs;
import aotstory.story.StoryManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;

/**
 * Отладочные команды (только для операторов). Нужны, чтобы тестировать сюжет, не проходя его заново.
 *   /aotstory flags            - список флагов
 *   /aotstory set <флаг>       - поставить флаг (событие на него сработает!)
 *   /aotstory clear <флаг>     - снять флаг
 *   /aotstory reset            - сбросить весь прогресс (после этого перезайди в мир)
 *   /aotstory npc <id>         - поставить NPC перед собой
 */
public final class StoryCommands {
    private StoryCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("aotstory")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("flags").executes(ctx -> {
                            var flags = StoryManager.flags(ctx.getSource().getServer());
                            ctx.getSource().sendFeedback(() -> Text.literal("Флаги: " + (flags.isEmpty() ? "(нет)" : String.join(", ", flags))), false);
                            return flags.size();
                        }))
                        .then(CommandManager.literal("set").then(CommandManager.argument("flag", StringArgumentType.word()).executes(ctx -> {
                            String flag = StringArgumentType.getString(ctx, "flag");
                            boolean added = StoryManager.complete(ctx.getSource().getServer(), flag, ctx.getSource().getPlayer());
                            ctx.getSource().sendFeedback(() -> Text.literal(added ? "Флаг поставлен: " + flag : "Флаг уже стоял: " + flag), false);
                            return 1;
                        })))
                        .then(CommandManager.literal("clear").then(CommandManager.argument("flag", StringArgumentType.word()).executes(ctx -> {
                            String flag = StringArgumentType.getString(ctx, "flag");
                            boolean removed = StoryManager.clearFlag(ctx.getSource().getServer(), flag);
                            ctx.getSource().sendFeedback(() -> Text.literal(removed ? "Флаг снят: " + flag : "Такого флага не было: " + flag), false);
                            return 1;
                        })))
                        .then(CommandManager.literal("reset").executes(ctx -> {
                            StoryManager.reset(ctx.getSource().getServer());
                            ctx.getSource().sendFeedback(() -> Text.literal("Сюжет сброшен. Перезайди в мир, чтобы начать заново."), true);
                            return 1;
                        }))
                        .then(CommandManager.literal("npc").then(CommandManager.argument("id", StringArgumentType.word()).executes(ctx -> {
                            String id = StringArgumentType.getString(ctx, "id");
                            Npcs.spawnInFront(ctx.getSource().getPlayerOrThrow(), id, 2);
                            return 1;
                        })))));
    }
}
