package aotstory.dialogue;

import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Один диалог (прочитан из JSON). Это чистые данные: сюжет правится в data/aotstory/dialogues,
 * без перекомпиляции мода.
 *
 * @param npc       id NPC, у которого этот диалог (hannes, eren...)
 * @param priority  из подходящих диалогов NPC выбирается с наибольшим priority
 * @param requires  флаги, которые должны быть уже установлены, чтобы диалог стал доступен
 * @param doneFlag  флаг, который ставится по завершении диалога (null = ничего не ставит).
 *                  Пока флаг не стоит — диалог доступен; как только поставился — больше не предлагается.
 * @param start     id первого узла
 * @param nodes     узлы диалога
 */
public record Dialogue(Identifier id, String npc, int priority, Set<String> requires, String doneFlag,
                       String start, Map<String, Node> nodes) {

    /** Узел: реплика + варианты ответа. setFlags применяются при завершении диалога. */
    public record Node(String speaker, String text, List<String> setFlags, List<Choice> choices) {}

    /** next == null -> этот ответ завершает диалог. */
    public record Choice(String text, String next) {}
}
