package aotstory.net;

import aotstory.AotStory;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Сервер -> клиент: закрыть окно диалога. */
public record DialogueEndPayload() implements CustomPayload {
    public static final CustomPayload.Id<DialogueEndPayload> ID = new CustomPayload.Id<>(AotStory.id("dialogue_end"));

    public static final PacketCodec<RegistryByteBuf, DialogueEndPayload> CODEC = PacketCodec.unit(new DialogueEndPayload());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
