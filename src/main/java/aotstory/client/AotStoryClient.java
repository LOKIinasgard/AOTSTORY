package aotstory.client;

import aotstory.net.DialogueEndPayload;
import aotstory.net.DialogueNodePayload;
import aotstory.npc.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.MinecraftClient;

/** Клиентская часть: отрисовка NPC и окно диалога. */
public class AotStoryClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.STORY_NPC, StoryNpcRenderer::new);

        ClientPlayNetworking.registerGlobalReceiver(DialogueNodePayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    MinecraftClient mc = context.client();
                    if (mc.currentScreen instanceof DialogueScreen screen) {
                        screen.update(payload);   // следующий узел в том же окне
                    } else {
                        mc.setScreen(new DialogueScreen(payload));
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(DialogueEndPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    MinecraftClient mc = context.client();
                    if (mc.currentScreen instanceof DialogueScreen) {
                        mc.setScreen(null);
                    }
                }));
    }
}
