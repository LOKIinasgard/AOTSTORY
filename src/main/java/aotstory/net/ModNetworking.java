package aotstory.net;

import aotstory.dialogue.DialogueManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ModNetworking {
    private ModNetworking() {}

    /** Общая регистрация (нужна и на сервере, и на клиенте). */
    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(DialogueNodePayload.ID, DialogueNodePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DialogueEndPayload.ID, DialogueEndPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DialogueChoicePayload.ID, DialogueChoicePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(DialogueChoicePayload.ID, (payload, context) ->
                context.server().execute(() -> DialogueManager.onChoice(context.player(), payload.index())));
    }
}
