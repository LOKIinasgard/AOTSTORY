package aotstory.story;

/**
 * Имена всех сюжетных флагов в одном месте.
 * Правило: флаг = "что-то уже произошло". Флаги только добавляются (через StoryManager.complete),
 * поэтому сюжет не может "откатиться" случайно, а любое событие можно повесить на конкретный флаг.
 */
public final class Flags {
    private Flags() {}

    /** Мир уже инициализирован (стартовый NPC поставлен, мир заморожен). */
    public static final String WORLD_STARTED = "world_started";

    // --- Пролог ---
    /** Игрок дослушал первый диалог с Ханнесом -> запускается Событие 1. */
    public static final String HANNES_INTRO_DONE = "hannes_intro_done";
    /** Событие 1 отыграло -> становится доступен Эрен. */
    public static final String EVENT_WALL_BREACH = "event_wall_breach";
    /** Диалог с Эреном пройден -> здесь позже повесим Событие 2. */
    public static final String EREN_TALK_DONE = "eren_talk_done";
}
