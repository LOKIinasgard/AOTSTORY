package aotstory.net;

import aotstory.AotStory;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.List;

/** Сервер -> клиент: показать узел диалога (реплика + тексты кнопок). */
public record DialogueNodePayload(String speaker, String text, List<String> choices) implements CustomPayload {
    public static final CustomPayload.Id<DialogueNodePayload> ID = new CustomPayload.Id<>(AotStory.id("dialogue_node"));

    public static final PacketCodec<RegistryByteBuf, DialogueNodePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, DialogueNodePayload::speaker,
            PacketCodecs.STRING, DialogueNodePayload::text,
            PacketCodecs.STRING.collect(PacketCodecs.toList()), DialogueNodePayload::choices,
            DialogueNodePayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
