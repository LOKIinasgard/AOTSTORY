package aotstory.story;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.PersistentState;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Сохраняемый прогресс сюжета (хранится на СЕРВЕРЕ, в папке мира: data/aotstory_state.dat).
 *
 * Решение: прогресс общий на весь мир, а не на игрока — это один сюжет на сервер, и так
 * проще блокировать глобальные вещи (время суток, спавн). Если понадобится личный прогресс
 * в кооперативе, сюда добавится Map<UUID, Set<String>>.
 */
public class StoryState extends PersistentState {
    public static final String KEY = "aotstory_state";

    public static final PersistentState.Type<StoryState> TYPE =
            new PersistentState.Type<>(StoryState::new, StoryState::fromNbt, null);

    private final Set<String> flags = new LinkedHashSet<>();

    public static StoryState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        StoryState state = new StoryState();
        NbtList list = nbt.getList("flags", NbtElement.STRING_TYPE);
        for (int i = 0; i < list.size(); i++) {
            state.flags.add(list.getString(i));
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        NbtList list = new NbtList();
        for (String flag : flags) {
            list.add(NbtString.of(flag));
        }
        nbt.put("flags", list);
        return nbt;
    }

    public boolean has(String flag) {
        return flags.contains(flag);
    }

    /** @return true, если флага раньше не было */
    boolean add(String flag) {
        boolean added = flags.add(flag);
        if (added) markDirty();
        return added;
    }

    boolean remove(String flag) {
        boolean removed = flags.remove(flag);
        if (removed) markDirty();
        return removed;
    }

    void clear() {
        flags.clear();
        markDirty();
    }

    /** Копия, чтобы снаружи нельзя было изменить флаги в обход StoryManager. */
    public Set<String> all() {
        return new LinkedHashSet<>(flags);
    }
}
