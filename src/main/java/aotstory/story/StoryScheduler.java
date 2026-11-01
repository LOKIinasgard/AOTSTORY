package aotstory.story;

import aotstory.AotStory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Мини-планировщик "сделать через N тиков" (20 тиков = 1 секунда).
 * Нужен для кино-эффектов (титул, пауза, потом титан) и чтобы не спавнить NPC в момент входа в мир,
 * когда игрок и чанки ещё не до конца загружены.
 */
public final class StoryScheduler {
    private StoryScheduler() {}

    private static final class Task {
        int ticksLeft;
        final Runnable action;

        Task(int ticksLeft, Runnable action) {
            this.ticksLeft = ticksLeft;
            this.action = action;
        }
    }

    private static final List<Task> TASKS = new ArrayList<>();

    public static void later(int ticks, Runnable action) {
        TASKS.add(new Task(ticks, action));
    }

    /** Вызывается каждый серверный тик. */
    public static void tick() {
        if (TASKS.isEmpty()) return;
        List<Task> due = new ArrayList<>();
        for (Iterator<Task> it = TASKS.iterator(); it.hasNext(); ) {
            Task task = it.next();
            if (--task.ticksLeft <= 0) {
                due.add(task);
                it.remove();
            }
        }
        for (Task task : due) {
            try {
                task.action.run();
            } catch (Exception e) {
                AotStory.LOGGER.error("Ошибка в отложенной задаче сюжета", e);
            }
        }
    }

    /** При остановке мира: задачи не должны пережить сервер. */
    public static void clear() {
        TASKS.clear();
    }
}
