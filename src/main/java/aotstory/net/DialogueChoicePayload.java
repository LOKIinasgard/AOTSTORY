package aotstory.net;

import aotstory.AotStory;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Клиент -> сервер: игрок нажал кнопку с номером index. */
public record DialogueChoicePayload(int index) implements CustomPayload {
    public static final CustomPayload.Id<DialogueChoicePayload> ID = new CustomPayload.Id<>(AotStory.id("dialogue_choice"));

    public static final PacketCodec<RegistryByteBuf, DialogueChoicePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, DialogueChoicePayload::index,
            DialogueChoicePayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
