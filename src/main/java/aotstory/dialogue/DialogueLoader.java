package aotstory.dialogue;

import aotstory.AotStory;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.io.Reader;
import java.util.*;

/** Читает диалоги из data/aotstory/dialogues/*.json (перечитываются по /reload). */
public final class DialogueLoader implements SimpleSynchronousResourceReloadListener {
    private static final Map<Identifier, Dialogue> DIALOGUES = new HashMap<>();

    public static void register() {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new DialogueLoader());
    }

    @Override
    public Identifier getFabricId() {
        return AotStory.id("dialogues");
    }

    @Override
    public void reload(ResourceManager manager) {
        DIALOGUES.clear();
        Map<Identifier, Resource> found = manager.findResources("dialogues", id -> id.getPath().endsWith(".json"));
        for (Map.Entry<Identifier, Resource> entry : found.entrySet()) {
            try (Reader reader = entry.getValue().getReader()) {
                Dialogue dialogue = parse(entry.getKey(), JsonParser.parseReader(reader).getAsJsonObject());
                DIALOGUES.put(dialogue.id(), dialogue);
            } catch (Exception e) {
                AotStory.LOGGER.error("Не удалось прочитать диалог {}", entry.getKey(), e);
            }
        }
        AotStory.LOGGER.info("Загружено диалогов: {}", DIALOGUES.size());
    }

    private static Dialogue parse(Identifier id, JsonObject o) {
        Map<String, Dialogue.Node> nodes = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("nodes").entrySet()) {
            JsonObject n = e.getValue().getAsJsonObject();
            List<Dialogue.Choice> choices = new ArrayList<>();
            if (n.has("choices")) {
                for (JsonElement ce : n.getAsJsonArray("choices")) {
                    JsonObject c = ce.getAsJsonObject();
                    choices.add(new Dialogue.Choice(c.get("text").getAsString(),
                            c.has("next") ? c.get("next").getAsString() : null));
                }
            }
            nodes.put(e.getKey(), new Dialogue.Node(
                    n.has("speaker") ? n.get("speaker").getAsString() : "",
                    n.get("text").getAsString(),
                    strings(n, "set_flags"),
                    choices));
        }

        String start = o.get("start").getAsString();
        if (!nodes.containsKey(start)) {
            throw new IllegalArgumentException("нет стартового узла '" + start + "'");
        }
        for (Dialogue.Node node : nodes.values()) {
            for (Dialogue.Choice choice : node.choices()) {
                if (choice.next() != null && !nodes.containsKey(choice.next())) {
                    AotStory.LOGGER.warn("Диалог {}: ссылка на несуществующий узел '{}'", id, choice.next());
                }
            }
        }

        return new Dialogue(id,
                o.get("npc").getAsString(),
                o.has("priority") ? o.get("priority").getAsInt() : 0,
                new LinkedHashSet<>(strings(o, "requires")),
                o.has("done_flag") ? o.get("done_flag").getAsString() : null,
                start,
                nodes);
    }

    private static List<String> strings(JsonObject o, String key) {
        List<String> out = new ArrayList<>();
        if (o.has(key)) {
            for (JsonElement e : o.getAsJsonArray(key)) {
                out.add(e.getAsString());
            }
        }
        return out;
    }

    /**
     * Какой диалог NPC предложит сейчас: подходят те, у которых все requires уже стоят
     * и done_flag ещё нет; из них берётся с наибольшим priority.
     * Именно здесь "закрыта" дверь к следующим этапам: пока флага нет, диалога просто не существует.
     */
    public static Optional<Dialogue> pick(String npcId, Set<String> flags) {
        return DIALOGUES.values().stream()
                .filter(d -> d.npc().equals(npcId))
                .filter(d -> flags.containsAll(d.requires()))
                .filter(d -> d.doneFlag() == null || !flags.contains(d.doneFlag()))
                .max(Comparator.comparingInt(Dialogue::priority));
    }
}
