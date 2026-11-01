package aotstory.dialogue;

import aotstory.net.DialogueEndPayload;
import aotstory.net.DialogueNodePayload;
import aotstory.npc.Npcs;
import aotstory.npc.StoryNpcEntity;
import aotstory.story.StoryManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.*;

/**
 * Ход диалога на СЕРВЕРЕ. Клиент только рисует текущий узел и присылает номер выбранного ответа.
 * Сервер всё проверяет сам (есть ли сессия, рядом ли NPC, существует ли такой ответ),
 * поэтому модифицированный клиент не может "перепрыгнуть" сюжет.
 */
public final class DialogueManager {
    private DialogueManager() {}

    private static final class Session {
        final Dialogue dialogue;
        final UUID npc;
        String nodeId;
        /** Флаги копятся и ставятся только когда диалог завершён целиком. */
        final List<String> pendingFlags = new ArrayList<>();

        Session(Dialogue dialogue, UUID npc) {
            this.dialogue = dialogue;
            this.npc = npc;
        }
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final double MAX_DISTANCE_SQ = 12 * 12;

    public static void start(ServerPlayerEntity player, StoryNpcEntity npc) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        Optional<Dialogue> picked = DialogueLoader.pick(npc.getNpcId(), StoryManager.flags(server));
        if (picked.isEmpty()) {
            player.sendMessage(Text.literal("[" + Npcs.displayName(npc.getNpcId()) + "] ..."), false);
            return;
        }
        Session session = new Session(picked.get(), npc.getUuid());
        SESSIONS.put(player.getUuid(), session);
        enter(player, session, session.dialogue.start());
    }

    /** Игрок выбрал ответ с номером index. */
    public static void onChoice(ServerPlayerEntity player, int index) {
        Session session = SESSIONS.get(player.getUuid());
        if (session == null) return;

        Entity npc = player.getServerWorld().getEntity(session.npc);
        if (npc == null || npc.squaredDistanceTo(player) > MAX_DISTANCE_SQ) {
            drop(player.getUuid()); // отошёл от NPC: диалог прерван, флаги НЕ ставятся
            ServerPlayNetworkingSend.end(player);
            return;
        }

        Dialogue.Node node = session.dialogue.nodes().get(session.nodeId);
        if (node.choices().isEmpty()) {
            finish(player, session);
            return;
        }
        if (index < 0 || index >= node.choices().size()) return;

        String next = node.choices().get(index).next();
        if (next == null || !session.dialogue.nodes().containsKey(next)) {
            finish(player, session);
        } else {
            enter(player, session, next);
        }
    }

    public static void drop(UUID playerId) {
        SESSIONS.remove(playerId);
    }

    public static void clear() {
        SESSIONS.clear();
    }

    private static void enter(ServerPlayerEntity player, Session session, String nodeId) {
        session.nodeId = nodeId;
        Dialogue.Node node = session.dialogue.nodes().get(nodeId);
        session.pendingFlags.addAll(node.setFlags());

        List<String> labels = new ArrayList<>();
        for (Dialogue.Choice c : node.choices()) labels.add(c.text());
        if (labels.isEmpty()) labels.add("Закрыть");
        ServerPlayNetworking.send(player, new DialogueNodePayload(node.speaker(), node.text(), labels));
    }

    /**
     * Диалог пройден до конца. Сначала закрываем окно, и только потом ставим флаги:
     * именно здесь сюжет двигается дальше (StoryManager.complete -> StoryEvents).
     */
    private static void finish(ServerPlayerEntity player, Session session) {
        SESSIONS.remove(player.getUuid());
        ServerPlayNetworkingSend.end(player);

        MinecraftServer server = player.getServer();
        if (server == null) return;
        for (String flag : session.pendingFlags) {
            StoryManager.complete(server, flag, player);
        }
        if (session.dialogue.doneFlag() != null) {
            StoryManager.complete(server, session.dialogue.doneFlag(), player);
        }
    }

    /** Маленький помощник, чтобы не тянуть импорт отправки в каждый метод. */
    private static final class ServerPlayNetworkingSend {
        static void end(ServerPlayerEntity player) {
            ServerPlayNetworking.send(player, new DialogueEndPayload());
        }
    }
}
