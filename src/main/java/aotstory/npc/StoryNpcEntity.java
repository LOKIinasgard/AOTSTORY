package aotstory.npc;

import aotstory.dialogue.DialogueManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * Сюжетный NPC. Одна сущность на всех персонажей: кто это, определяет строка npcId
 * ("hannes", "eren"...), а реплики берутся из data/aotstory/dialogues по этому id.
 * Решение: свой класс сущности вместо жителя — у жителя своя торговля и поведение, которые мешают.
 */
public class StoryNpcEntity extends PathAwareEntity {
    private static final TrackedData<String> NPC_ID =
            DataTracker.registerData(StoryNpcEntity.class, TrackedDataHandlerRegistry.STRING);

    public StoryNpcEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        this.setPersistent(); // не деспавнится
    }

    public static DefaultAttributeContainer.Builder createNpcAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void initGoals() {
        // Просто смотрит на игроков рядом. Никакого блуждания и боя.
        this.goalSelector.add(1, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(2, new LookAroundGoal(this));
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(NPC_ID, "");
    }

    public String getNpcId() {
        return this.dataTracker.get(NPC_ID);
    }

    public void setNpcId(String id) {
        this.dataTracker.set(NPC_ID, id);
    }

    /** Сюжетного NPC нельзя убить: иначе игрок сломает цепочку сюжета. */
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    /** ПКМ по NPC = начать диалог. Логика только на сервере. */
    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
        }
        if (!this.getWorld().isClient && player instanceof ServerPlayerEntity serverPlayer) {
            DialogueManager.start(serverPlayer, this);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("NpcId", getNpcId());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        setNpcId(nbt.getString("NpcId"));
    }
}
