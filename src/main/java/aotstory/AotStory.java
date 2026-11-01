package aotstory;

import aotstory.command.StoryCommands;
import aotstory.dialogue.DialogueLoader;
import aotstory.dialogue.DialogueManager;
import aotstory.net.ModNetworking;
import aotstory.npc.ModEntities;
import aotstory.story.StoryEvents;
import aotstory.story.StoryManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Точка входа (сервер + клиент в одиночной игре). Здесь только регистрация, логики нет. */
public class AotStory implements ModInitializer {
    public static final String MOD_ID = "aotstory";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModEntities.register();          // тип сущности NPC + атрибуты
        ModNetworking.registerCommon();  // пакеты диалога
        DialogueLoader.register();       // диалоги из data/aotstory/dialogues/*.json
        StoryEvents.registerAll();       // привязка событий к флагам
        StoryCommands.register();        // /aotstory ... (для отладки)

        // Первый вход в мир = старт сюжета (ставим NPC и "замораживаем" мир)
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                StoryManager.onPlayerJoin(server, handler.getPlayer()));
        // Сессия диалога не должна пережить игрока
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                DialogueManager.drop(handler.getPlayer().getUuid()));
    }
}
