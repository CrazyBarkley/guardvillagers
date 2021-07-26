package tallestegg.guardvillagers.entities;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GolemRandomStrollInVillageGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveBackToVillageGoal;
import net.minecraft.world.entity.ai.goal.MoveThroughVillageGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Illusioner;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MilkBucketItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.loot.IntRange;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.fmllegacy.network.PacketDistributor;
import tallestegg.guardvillagers.GuardItems;
import tallestegg.guardvillagers.GuardLootTables;
import tallestegg.guardvillagers.GuardPacketHandler;
import tallestegg.guardvillagers.configuration.GuardConfig;
import tallestegg.guardvillagers.entities.ai.goals.ArmorerRepairGuardArmorGoal;
import tallestegg.guardvillagers.entities.ai.goals.FollowShieldGuards;
import tallestegg.guardvillagers.entities.ai.goals.GuardEatFoodGoal;
import tallestegg.guardvillagers.entities.ai.goals.GuardRunToEatGoal;
import tallestegg.guardvillagers.entities.ai.goals.GuardSetRunningToEatGoal;
import tallestegg.guardvillagers.entities.ai.goals.HeroHurtByTargetGoal;
import tallestegg.guardvillagers.entities.ai.goals.HeroHurtTargetGoal;
import tallestegg.guardvillagers.entities.ai.goals.KickGoal;
import tallestegg.guardvillagers.entities.ai.goals.RaiseShieldGoal;
import tallestegg.guardvillagers.entities.ai.goals.RangedBowAttackPassiveGoal;
import tallestegg.guardvillagers.entities.ai.goals.RangedCrossbowAttackPassiveGoal;
import tallestegg.guardvillagers.entities.ai.goals.RunToClericGoal;
import tallestegg.guardvillagers.entities.ai.goals.WalkBackToCheckPointGoal;
import tallestegg.guardvillagers.networking.GuardOpenInventoryPacket;

public class Guard extends PathfinderMob implements CrossbowAttackMob, RangedAttackMob, NeutralMob, ContainerListener {
    private static final UUID MODIFIER_UUID = UUID.fromString("5CD17E52-A79A-43D3-A529-90FDE04B181E");
    private static final AttributeModifier USE_ITEM_SPEED_PENALTY = new AttributeModifier(MODIFIER_UUID, "Use item speed penalty", -0.25D, AttributeModifier.Operation.ADDITION);
    private static final EntityDataAccessor<Optional<BlockPos>> GUARD_POS = SynchedEntityData.defineId(Guard.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Boolean> PATROLLING = SynchedEntityData.defineId(Guard.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> GUARD_VARIANT = SynchedEntityData.defineId(Guard.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RUNNING_TO_EAT = SynchedEntityData.defineId(Guard.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_CHARGING_STATE = SynchedEntityData.defineId(Guard.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> EATING = SynchedEntityData.defineId(Guard.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> KICKING = SynchedEntityData.defineId(Guard.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FOLLOWING = SynchedEntityData.defineId(Guard.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Optional<UUID>> OWNER_UNIQUE_ID = SynchedEntityData.defineId(Guard.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final Map<Pose, EntityDimensions> SIZE_BY_POSE = ImmutableMap.<Pose, EntityDimensions>builder().put(Pose.STANDING, EntityDimensions.scalable(0.6F, 1.95F)).put(Pose.SLEEPING, SLEEPING_DIMENSIONS).put(Pose.FALL_FLYING, EntityDimensions.scalable(0.6F, 0.6F)).put(Pose.SWIMMING, EntityDimensions.scalable(0.6F, 0.6F))
            .put(Pose.SPIN_ATTACK, EntityDimensions.scalable(0.6F, 0.6F)).put(Pose.CROUCHING, EntityDimensions.scalable(0.6F, 1.75F)).put(Pose.DYING, EntityDimensions.fixed(0.2F, 0.2F)).build();
    public SimpleContainer guardInventory = new SimpleContainer(6);
    public int kickTicks;
    public int shieldCoolDown;
    public int kickCoolDown;
    public boolean interacting;
    private int remainingPersistentAngerTime;
    private static final UniformInt angerTime = TimeUtil.rangeOfSeconds(20, 39);
    private UUID persistentAngerTarget;
    private static final Map<EquipmentSlot, ResourceLocation> EQUIPMENT_SLOT_ITEMS = Util.make(Maps.newHashMap(), (slotItems) -> {
        slotItems.put(EquipmentSlot.MAINHAND, GuardLootTables.GUARD_MAIN_HAND);
        slotItems.put(EquipmentSlot.OFFHAND, GuardLootTables.GUARD_OFF_HAND);
        slotItems.put(EquipmentSlot.HEAD, GuardLootTables.GUARD_HELMET);
        slotItems.put(EquipmentSlot.CHEST, GuardLootTables.GUARD_CHEST);
        slotItems.put(EquipmentSlot.LEGS, GuardLootTables.GUARD_LEGGINGS);
        slotItems.put(EquipmentSlot.FEET, GuardLootTables.GUARD_FEET);
    });

    public Guard(EntityType<? extends Guard> type, Level world) {
        super(type, world);
        this.guardInventory.addListener(this);
        this.itemHandler = net.minecraftforge.common.util.LazyOptional.of(() -> new net.minecraftforge.items.wrapper.InvWrapper(this.guardInventory));
        this.setPersistenceRequired();
        if (GuardConfig.GuardsOpenDoors)
            ((GroundPathNavigation) this.getNavigation()).setCanOpenDoors(true);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor worldIn, DifficultyInstance difficultyIn, MobSpawnType reason, @Nullable SpawnGroupData spawnDataIn, @Nullable CompoundTag dataTag) {
        this.setPersistenceRequired();
        int type = Guard.getRandomTypeForBiome(level, this.blockPosition());
        if (spawnDataIn instanceof Guard.GuardData) {
            type = ((Guard.GuardData) spawnDataIn).variantData;
            spawnDataIn = new Guard.GuardData(type);
        }
        this.setGuardVariant(type);
        this.populateDefaultEquipmentSlots(difficultyIn);
        return super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    @Override
    protected void doPush(Entity entityIn) {
        if (entityIn instanceof PathfinderMob) {
            PathfinderMob living = (PathfinderMob) entityIn;
            boolean attackTargets = living.getTarget() instanceof Villager || living.getTarget() instanceof IronGolem || living.getTarget() instanceof Guard;
            if (attackTargets)
                this.setTarget(living);
        }
        super.doPush(entityIn);
    }

    @Nullable
    public void setPatrolPos(BlockPos position) {
        this.entityData.set(GUARD_POS, Optional.ofNullable(position));
    }

    @Nullable
    public BlockPos getPatrolPos() {
        return this.entityData.get(GUARD_POS).orElse((BlockPos) null);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        if (this.isBlocking()) {
            return SoundEvents.SHIELD_BLOCK;
        } else {
            return SoundEvents.VILLAGER_HURT;
        }
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    public static int slotToInventoryIndex(EquipmentSlot slot) {
        switch (slot) {
        case CHEST:
            return 1;
        case FEET:
            return 3;
        case HEAD:
            return 0;
        case LEGS:
            return 2;
        default:
            break;
        }
        return 0;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHitIn) {
        for (int i = 0; i < this.guardInventory.getContainerSize(); ++i) {
            ItemStack itemstack = this.guardInventory.getItem(i);
            if (!itemstack.isEmpty() && !EnchantmentHelper.hasVanishingCurse(itemstack)) {
                this.spawnAtLocation(itemstack);
            }
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        UUID uuid = compound.hasUUID("Owner") ? compound.getUUID("Owner") : null;
        if (uuid != null) {
            try {
                this.setOwnerId(uuid);
            } catch (Throwable throwable) {
                this.setOwnerId(null);
            }
        }
        this.setGuardVariant(compound.getInt("Type"));
        this.kickTicks = compound.getInt("KickTicks");
        this.setFollowing(compound.getBoolean("Following"));
        this.interacting = compound.getBoolean("Interacting");
        this.setEating(compound.getBoolean("Eating"));
        this.setPatrolling(compound.getBoolean("Patrolling"));
        this.setRunningToEat(compound.getBoolean("RunningToEat"));
        this.shieldCoolDown = compound.getInt("KickCooldown");
        this.kickCoolDown = compound.getInt("ShieldCooldown");
        if (compound.contains("PatrolPosX")) {
            int x = compound.getInt("PatrolPosX");
            int y = compound.getInt("PatrolPosY");
            int z = compound.getInt("PatrolPosZ");
            this.entityData.set(GUARD_POS, Optional.ofNullable(new BlockPos(x, y, z)));
        }
        ListTag listnbt = compound.getList("Inventory", 10);
        for (int i = 0; i < listnbt.size(); ++i) {
            CompoundTag compoundnbt = listnbt.getCompound(i);
            int j = compoundnbt.getByte("Slot") & 255;
            this.guardInventory.setItem(j, ItemStack.of(compoundnbt));
        }
        if (compound.contains("ArmorItems", 9)) {
            ListTag armorItems = compound.getList("ArmorItems", 10);
            for (int i = 0; i < this.armorItems.size(); ++i) {
                int index = Guard.slotToInventoryIndex(Mob.getEquipmentSlotForItem(ItemStack.of(armorItems.getCompound(i))));
                this.guardInventory.setItem(index, ItemStack.of(armorItems.getCompound(i)));
            }
        }
        if (compound.contains("HandItems", 9)) {
            ListTag handItems = compound.getList("HandItems", 10);
            for (int i = 0; i < this.handItems.size(); ++i) {
                int handSlot = i == 0 ? 5 : 4;
                this.guardInventory.setItem(handSlot, ItemStack.of(handItems.getCompound(i)));
            }
        }
        if (!level.isClientSide)
            this.readPersistentAngerSaveData((ServerLevel) this.level, compound);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("Type", this.getGuardVariant());
        compound.putInt("KickTicks", this.kickTicks);
        compound.putInt("ShieldCooldown", this.shieldCoolDown);
        compound.putInt("KickCooldown", this.kickCoolDown);
        compound.putBoolean("Following", this.isFollowing());
        compound.putBoolean("Interacting", this.interacting);
        compound.putBoolean("Eating", this.isEating());
        compound.putBoolean("Patrolling", this.isPatrolling());
        compound.putBoolean("RunningToEat", this.isRunningToEat());
        if (this.getOwnerId() != null) {
            compound.putUUID("Owner", this.getOwnerId());
        }
        ListTag listnbt = new ListTag();
        for (int i = 0; i < this.guardInventory.getContainerSize(); ++i) {
            ItemStack itemstack = this.guardInventory.getItem(i);
            if (!itemstack.isEmpty()) {
                CompoundTag compoundnbt = new CompoundTag();
                compoundnbt.putByte("Slot", (byte) i);
                itemstack.save(compoundnbt);
                listnbt.add(compoundnbt);
            }
        }
        compound.put("Inventory", listnbt);
        if (this.getPatrolPos() != null) {
            compound.putInt("PatrolPosX", this.getPatrolPos().getX());
            compound.putInt("PatrolPosY", this.getPatrolPos().getY());
            compound.putInt("PatrolPosZ", this.getPatrolPos().getZ());
        }
        this.addPersistentAngerSaveData(compound);
    }

    @Nullable
    public LivingEntity getOwner() {
        try {
            UUID uuid = this.getOwnerId();
            return (uuid == null || uuid != null && this.level.getPlayerByUUID(uuid) != null && !this.level.getPlayerByUUID(uuid).hasEffect(MobEffects.HERO_OF_THE_VILLAGE)) ? null : this.level.getPlayerByUUID(uuid);
        } catch (IllegalArgumentException illegalargumentexception) {
            return null;
        }
    }

    public boolean isOwner(LivingEntity entityIn) {
        return entityIn == this.getOwner();
    }

    @Nullable
    public UUID getOwnerId() {
        return this.entityData.get(OWNER_UNIQUE_ID).orElse(null);
    }

    public void setOwnerId(@Nullable UUID p_184754_1_) {
        this.entityData.set(OWNER_UNIQUE_ID, Optional.ofNullable(p_184754_1_));
    }

    @Override
    public boolean doHurtTarget(Entity entityIn) {
        if (this.isKicking()) {
            ((LivingEntity) entityIn).knockback(1.0F, Mth.sin(this.getYRot() * ((float) Math.PI / 180F)), (-Mth.cos(this.getYRot() * ((float) Math.PI / 180F))));
            this.kickTicks = 10;
            this.level.broadcastEntityEvent(this, (byte) 4);
            this.lookAt(entityIn, 90.0F, 90.0F);
        }
        ItemStack hand = this.getMainHandItem();
        hand.hurtAndBreak(1, this, (entity) -> entity.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return super.doHurtTarget(entityIn);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) {
            this.kickTicks = 10;
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public boolean isImmobile() {
        return this.interacting || super.isImmobile();
    }

    @Override
    protected void completeUsingItem() {
        super.completeUsingItem();
        if (this.getOffhandItem().getItem() instanceof PotionItem && !(this.getOffhandItem().getItem() instanceof SplashPotionItem))
            this.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.GLASS_BOTTLE));
        if (this.getOffhandItem().getItem() instanceof MilkBucketItem)
            this.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.BUCKET));
    }

    @Override
    public ItemStack eat(Level world, ItemStack stack) {
        if (stack.isEdible()) {
            this.heal(stack.getItem().getFoodProperties().getNutrition());
        }
        super.eat(world, stack);
        world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5F, world.random.nextFloat() * 0.1F + 0.9F);
        this.setEating(false);
        return stack;
    }

    @Override
    public void aiStep() {
        if (this.kickTicks > 0) {
            --this.kickTicks;
        }
        if (this.kickCoolDown > 0) {
            --this.kickCoolDown;
        }
        if (this.shieldCoolDown > 0) {
            --this.shieldCoolDown;
        }
        if (this.getHealth() < this.getMaxHealth() && this.tickCount % 200 == 0) {
            this.heal(GuardConfig.amountOfHealthRegenerated);
        }
        if (!this.level.isClientSide)
            this.updatePersistentAnger((ServerLevel) this.level, true);
        this.updateSwingTime();
        super.aiStep();
    }

    @Override
    public EntityDimensions getDimensions(Pose poseIn) {
        return SIZE_BY_POSE.getOrDefault(poseIn, EntityDimensions.scalable(0.6F, 1.95F));
    }

    @Override
    public float getStandingEyeHeight(Pose poseIn, EntityDimensions sizeIn) {
        if (poseIn == Pose.CROUCHING) {
            return 1.40F;
        }
        return super.getStandingEyeHeight(poseIn, sizeIn);
    }

    @Override
    protected void blockUsingShield(LivingEntity entityIn) {
        super.blockUsingShield(entityIn);
        if (entityIn.getMainHandItem().canDisableShield(this.useItem, this, entityIn))
            this.disableShield(true);
    }

    @Override
    protected void hurtCurrentlyUsedShield(float damage) {
        if (this.useItem.isShield(this)) {
            if (damage >= 3.0F) {
                int i = 1 + Mth.floor(damage);
                InteractionHand hand = this.getUsedItemHand();
                this.useItem.hurtAndBreak(i, this, (entity) -> entity.broadcastBreakEvent(hand));
                if (this.useItem.isEmpty()) {
                    if (hand == InteractionHand.MAIN_HAND) {
                        this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                    } else {
                        this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                    }
                    this.useItem = ItemStack.EMPTY;
                    this.playSound(SoundEvents.SHIELD_BREAK, 0.8F, 0.8F + this.level.random.nextFloat() * 0.4F);
                }
            }
        }
    }

    @Override
    public void startUsingItem(InteractionHand hand) {
        ItemStack itemstack = this.getItemInHand(hand);
        if (itemstack.isShield(this)) {
            AttributeInstance modifiableattributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
            modifiableattributeinstance.removeModifier(USE_ITEM_SPEED_PENALTY);
            modifiableattributeinstance.addTransientModifier(USE_ITEM_SPEED_PENALTY);
        }
        super.startUsingItem(hand);
    }

    @Override
    public void stopUsingItem() {
        if (this.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(USE_ITEM_SPEED_PENALTY))
            this.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(USE_ITEM_SPEED_PENALTY);
        super.stopUsingItem();
    }

    public void disableShield(boolean increase) {
        float chance = 0.25F + (float) EnchantmentHelper.getBlockEfficiency(this) * 0.05F;
        if (increase)
            chance += 0.75;
        if (this.random.nextFloat() < chance) {
            this.shieldCoolDown = 100;
            this.stopUsingItem();
            this.level.broadcastEntityEvent(this, (byte) 30);
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(GUARD_VARIANT, 0);
        this.entityData.define(DATA_CHARGING_STATE, false);
        this.entityData.define(KICKING, false);
        this.entityData.define(OWNER_UNIQUE_ID, Optional.empty());
        this.entityData.define(EATING, false);
        this.entityData.define(FOLLOWING, false);
        this.entityData.define(GUARD_POS, Optional.empty());
        this.entityData.define(PATROLLING, false);
        this.entityData.define(RUNNING_TO_EAT, false);
    }

    public boolean isCharging() {
        return this.entityData.get(DATA_CHARGING_STATE);
    }

    public void setChargingCrossbow(boolean charging) {
        this.entityData.set(DATA_CHARGING_STATE, charging);
    }

    public boolean isKicking() {
        return this.entityData.get(KICKING);
    }

    public void setKicking(boolean kicking) {
        this.entityData.set(KICKING, kicking);
    }

    @Override
    protected void populateDefaultEquipmentSlots(DifficultyInstance difficulty) {
        for (EquipmentSlot equipmentslottype : EquipmentSlot.values()) {
            for (ItemStack stack : this.getItemsFromLootTable(equipmentslottype)) {
                this.setItemSlot(equipmentslottype, stack);
            }
        }
        this.handDropChances[EquipmentSlot.MAINHAND.getIndex()] = 100.0F;
        this.handDropChances[EquipmentSlot.OFFHAND.getIndex()] = 100.0F;
    }

    public List<ItemStack> getItemsFromLootTable(EquipmentSlot slot) {
        if (EQUIPMENT_SLOT_ITEMS.containsKey(slot)) {
            LootTable loot = this.level.getServer().getLootTables().get(EQUIPMENT_SLOT_ITEMS.get(slot));
            LootContext.Builder lootcontext$builder = (new LootContext.Builder((ServerLevel) this.level)).withParameter(LootContextParams.THIS_ENTITY, this).withRandom(this.getRandom());
            return loot.getRandomItems(lootcontext$builder.create(GuardLootTables.SLOT));
        }
        return null;
    }

    public int getGuardVariant() {
        return this.entityData.get(GUARD_VARIANT);
    }

    public void setGuardVariant(int typeId) {
        this.entityData.set(GUARD_VARIANT, typeId);
    }

    // Credit : the abnormals people for discovering this
    public ItemStack getPickedResult(HitResult target) {
        return new ItemStack(GuardItems.GUARD_SPAWN_EGG.get());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(0, new KickGoal(this));
        this.goalSelector.addGoal(0, new GuardEatFoodGoal(this));
        this.goalSelector.addGoal(0, new RaiseShieldGoal(this));
        this.goalSelector.addGoal(1, new GuardRunToEatGoal(this));
        this.goalSelector.addGoal(1, new GuardSetRunningToEatGoal(this, 1.0D));
        this.goalSelector.addGoal(2, new RangedCrossbowAttackPassiveGoal<>(this, 1.0D, 8.0F));
        this.goalSelector.addGoal(2, new RangedBowAttackPassiveGoal<>(this, 0.5D, 20, 15.0F));
        this.goalSelector.addGoal(2, new Guard.GuardMeleeGoal(this, 0.8D, true));
        this.goalSelector.addGoal(3, new Guard.FollowHeroGoal(this));
        if (GuardConfig.GuardsRunFromPolarBears)
            this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, PolarBear.class, 12.0F, 1.0D, 1.2D));
        this.goalSelector.addGoal(3, new MoveBackToVillageGoal(this, 0.5D, false));
        this.goalSelector.addGoal(3, new GolemRandomStrollInVillageGoal(this, 0.5D));
        this.goalSelector.addGoal(3, new MoveThroughVillageGoal(this, 0.5D, false, 4, () -> false));
        if (GuardConfig.GuardsOpenDoors)
            this.goalSelector.addGoal(3, new OpenDoorGoal(this, true) {
                @Override
                public void start() {
                    this.mob.swing(InteractionHand.MAIN_HAND);
                    super.start();
                }
            });
        if (GuardConfig.GuardFormation)
            this.goalSelector.addGoal(5, new FollowShieldGuards(this)); // phalanx
        if (GuardConfig.ClericHealing)
            this.goalSelector.addGoal(6, new RunToClericGoal(this));
        if (GuardConfig.armorerRepairGuardArmor)
            this.goalSelector.addGoal(6, new ArmorerRepairGuardArmorGoal(this));
        this.goalSelector.addGoal(4, new WalkBackToCheckPointGoal(this, 0.5D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, AbstractVillager.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomStrollGoal(this, 0.5D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.targetSelector.addGoal(5, new Guard.DefendVillageGuardGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Ravager.class, true));
        this.targetSelector.addGoal(2, (new HurtByTargetGoal(this, Guard.class, IronGolem.class)).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Witch.class, true));
        this.targetSelector.addGoal(3, new HeroHurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new HeroHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractIllager.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Raider.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Illusioner.class, true));
        if (GuardConfig.AttackAllMobs) {
            this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 5, true, true, (mob) -> {
                return mob instanceof Enemy && !GuardConfig.MobBlackList.contains(mob.getEncodeId());
            }));
        }
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Zombie.class, true));
        this.targetSelector.addGoal(4, new ResetUniversalAngerTargetGoal<>(this, false));
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        this.shieldCoolDown = 8;
        if (this.getMainHandItem().getItem() instanceof CrossbowItem)
            this.performCrossbowAttack(this, 6.0F);
        if (this.getMainHandItem().getItem() instanceof BowItem) {
            ItemStack itemstack = this.getProjectile(this.getItemInHand(GuardItems.getHandWith(this, item -> item instanceof BowItem)));
            ItemStack hand = this.getMainHandItem();
            hand.hurtAndBreak(1, this, (entity) -> entity.broadcastBreakEvent(EquipmentSlot.MAINHAND));
            AbstractArrow abstractarrowentity = ProjectileUtil.getMobArrow(this, itemstack, distanceFactor);
            abstractarrowentity = ((net.minecraft.world.item.BowItem) this.getMainHandItem().getItem()).customArrow(abstractarrowentity);

            int powerLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, itemstack);
            if (powerLevel > 0)
                abstractarrowentity.setBaseDamage(abstractarrowentity.getBaseDamage() + (double) powerLevel * 0.5D + 0.5D);
            int punchLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, itemstack);
            if (punchLevel > 0)
                abstractarrowentity.setKnockback(punchLevel);
            if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, itemstack) > 0)
                abstractarrowentity.setSecondsOnFire(100);
            double d0 = target.getX() - this.getX();
            double d1 = target.getY(0.3333333333333333D) - abstractarrowentity.getY();
            double d2 = target.getZ() - this.getZ();
            double d3 = Mth.sqrt((float) (d0 * d0 + d2 * d2));
            abstractarrowentity.shoot(d0, d1 + d3 * (double) 0.2F, d2, 1.6F, (float) (14 - this.level.getDifficulty().getId() * 4));
            this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
            this.level.addFreshEntity(abstractarrowentity);
        }
    }

    @Override
    public void setItemSlot(EquipmentSlot slotIn, ItemStack stack) {
        super.setItemSlot(slotIn, stack);
        switch (slotIn) {
        case CHEST:
            if (this.guardInventory.getItem(1).isEmpty())
                this.guardInventory.setItem(1, this.armorItems.get(slotIn.getIndex()));
            break;
        case FEET:
            if (this.guardInventory.getItem(3).isEmpty())
                this.guardInventory.setItem(3, this.armorItems.get(slotIn.getIndex()));
            break;
        case HEAD:
            if (this.guardInventory.getItem(0).isEmpty())
                this.guardInventory.setItem(0, this.armorItems.get(slotIn.getIndex()));
            break;
        case LEGS:
            if (this.guardInventory.getItem(2).isEmpty())
                this.guardInventory.setItem(2, this.armorItems.get(slotIn.getIndex()));
            break;
        case MAINHAND:
            if (this.guardInventory.getItem(5).isEmpty())
                this.guardInventory.setItem(5, this.handItems.get(slotIn.getIndex()));
            break;
        case OFFHAND:
            if (this.guardInventory.getItem(4).isEmpty())
                this.guardInventory.setItem(4, this.handItems.get(slotIn.getIndex()));
            break;
        }
    }

    @Override
    public ItemStack getProjectile(ItemStack shootable) {
        if (shootable.getItem() instanceof ProjectileWeaponItem) {
            Predicate<ItemStack> predicate = ((ProjectileWeaponItem) shootable.getItem()).getSupportedHeldProjectiles();
            ItemStack itemstack = ProjectileWeaponItem.getHeldProjectile(this, predicate);
            return itemstack.isEmpty() ? new ItemStack(Items.ARROW) : itemstack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    public int getKickTicks() {
        return this.kickTicks;
    }

    public boolean isFollowing() {
        return this.entityData.get(FOLLOWING);
    }

    public void setFollowing(boolean following) {
        this.entityData.set(FOLLOWING, following);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !GuardConfig.MobBlackList.contains(target.getEncodeId()) && !target.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) && !this.isOwner(target) && !(target instanceof Villager) && super.canAttack(target);
    }

    /**
     * Credit - SmellyModder for Biome Specific Textures
     */
    public static int getRandomTypeForBiome(LevelAccessor world, BlockPos pos) {
        Biome biome = world.getBiome(pos);
        switch (biome.getBiomeCategory()) {
        case DESERT:
        case MESA:
            return 1;
        case ICY:
            return 6;
        case JUNGLE:
            return 4;
        case SAVANNA:
            return 2;
        case SWAMP:
            return 3;
        case TAIGA:
            return 5;
        case NONE:
        case PLAINS:
        default:
            return 0;
        }
    }

    @Override
    public void rideTick() {
        super.rideTick();
        if (this.getVehicle() instanceof PathfinderMob) {
            PathfinderMob creatureentity = (PathfinderMob) this.getVehicle();
            this.yBodyRot = creatureentity.yBodyRot;
        }
    }

    @Override
    public void onCrossbowAttackPerformed() {
        this.noActionTime = 0;
    }

    @Override
    public void shootCrossbowProjectile(LivingEntity arg0, ItemStack arg1, Projectile arg2, float arg3) {
        this.shootCrossbowProjectile(this, arg0, arg2, arg3, 1.6F);
    }

    @Override
    protected void blockedByShield(LivingEntity entityIn) {
        if (this.isKicking()) {
            this.setKicking(false);
        }
        super.blockedByShield(this);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        boolean configValues = !GuardConfig.giveGuardStuffHOTV || !GuardConfig.setGuardPatrolHotv || player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) && GuardConfig.giveGuardStuffHOTV || player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) && GuardConfig.setGuardPatrolHotv
                || player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) && GuardConfig.giveGuardStuffHOTV && GuardConfig.setGuardPatrolHotv;
        boolean inventoryRequirements = !player.isSecondaryUseActive() && this.onGround;
        if (configValues && inventoryRequirements) {
            if (this.getTarget() != player && this.isEffectiveAi()) {
                if (player instanceof ServerPlayer) {
                    this.openGui((ServerPlayer) player);
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.CONSUME;
        }
        return super.mobInteract(player, hand);
    }

   /* @Override
    public boolean setSlot(int inventorySlot, ItemStack itemStackIn) {
        int i = inventorySlot - 400;
        if (i >= 0 && i < 2 && i < this.guardInventory.getContainerSize()) {
            if (i == 0) {
                return false;
            } else if (itemStackIn.getItem() instanceof ArmorItem) {
                this.guardInventory.setItem(i, itemStackIn);
                return true;
            } else {
                return false;
            }
        } else {
            int j = inventorySlot - 500 + 2;
            if (j >= 2 && j < this.guardInventory.getContainerSize()) {
                this.guardInventory.setItem(j, itemStackIn);
                return true;
            } else {
                return false;
            }
        }
    }*/

    public static String getNameByType(int id) {
        switch (id) {
        case 0:
            return "plains";
        case 1:
            return "desert";
        case 2:
            return "savanna";
        case 3:
            return "swamp";
        case 4:
            return "jungle";
        case 5:
            return "taiga";
        case 6:
            return "snow";
        }
        return "";
    }

    @Override
    public void containerChanged(Container invBasic) {
    }

    @Override
    protected void hurtArmor(DamageSource damageSource, float damage) {
        if (damage >= 0.0F) {
            damage = damage / 4.0F;
            if (damage < 1.0F) {
                damage = 1.0F;
            }
            for (int i = 0; i < this.guardInventory.getContainerSize(); ++i) {
                ItemStack itemstack = this.guardInventory.getItem(i);
                if ((!damageSource.isFire() || !itemstack.getItem().isFireResistant()) && itemstack.getItem() instanceof ArmorItem) {
                    int j = i;
                    itemstack.hurtAndBreak((int) damage, this, (p_214023_1_) -> {
                        p_214023_1_.broadcastBreakEvent(EquipmentSlot.byTypeAndIndex(EquipmentSlot.Type.ARMOR, j));
                    });
                }
            }
        }
    }

    @Override
    public void thunderHit(ServerLevel p_241841_1_, LightningBolt p_241841_2_) {
        if (p_241841_1_.getDifficulty() != Difficulty.PEACEFUL) {
            Witch witchentity = EntityType.WITCH.create(p_241841_1_);
            if (witchentity == null)
                return;
            witchentity.copyPosition(this);
            witchentity.finalizeSpawn(p_241841_1_, p_241841_1_.getCurrentDifficultyAt(witchentity.blockPosition()), MobSpawnType.CONVERSION, null, null);
            witchentity.setNoAi(this.isNoAi());
            witchentity.setCustomName(this.getCustomName());
            witchentity.setCustomNameVisible(this.isCustomNameVisible());
            witchentity.setPersistenceRequired();
            p_241841_1_.addFreshEntityWithPassengers(witchentity);
            this.remove(true);
        } else {
            super.thunderHit(p_241841_1_, p_241841_2_);
        }
    }

    @Override
    public UUID getPersistentAngerTarget() {
        return this.persistentAngerTarget;
    }

    @Override
    public int getRemainingPersistentAngerTime() {
        return this.remainingPersistentAngerTime;
    }

    @Override
    public void setPersistentAngerTarget(UUID arg0) {
        this.persistentAngerTarget = arg0;
    }

    @Override
    public void setRemainingPersistentAngerTime(int arg0) {
        this.remainingPersistentAngerTime = arg0;
    }

    @Override
    public void startPersistentAngerTimer() {
        this.setRemainingPersistentAngerTime(angerTime.sample(random));
    }

    public void openGui(ServerPlayer player) {
        if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }
        this.interacting = true;
        player.nextContainerCounter();
        GuardPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new GuardOpenInventoryPacket(player.containerCounter, this.guardInventory.getContainerSize(), this.getId()));
        player.containerMenu = new GuardContainer(player.containerCounter, player.getInventory(), this.guardInventory, this);
        player.initMenu(player.containerMenu);
        MinecraftForge.EVENT_BUS.post(new PlayerContainerEvent.Open(player, player.containerMenu));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, GuardConfig.GuardHealth).add(Attributes.MOVEMENT_SPEED, GuardConfig.GuardSpeed).add(Attributes.ATTACK_DAMAGE, 1.0D).add(Attributes.FOLLOW_RANGE,
                GuardConfig.GuardFollowRange);
    }

    private net.minecraftforge.common.util.LazyOptional<?> itemHandler;

    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable net.minecraft.core.Direction facing) {
        if (this.isAlive() && capability == net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && itemHandler != null)
            return itemHandler.cast();
        return super.getCapability(capability, facing);
    }

    public boolean isEating() {
        return this.entityData.get(EATING);
    }

    public void setEating(boolean eating) {
        this.entityData.set(EATING, eating);
    }

    public boolean isPatrolling() {
        return this.entityData.get(PATROLLING);
    }

    public void setPatrolling(boolean patrolling) {
        this.entityData.set(PATROLLING, patrolling);
    }

    public boolean isRunningToEat() {
        return this.entityData.get(RUNNING_TO_EAT);
    }

    public void setRunningToEat(boolean running) {
        this.entityData.set(RUNNING_TO_EAT, running);
    }

    public static class GuardData implements SpawnGroupData {
        public final int variantData;

        public GuardData(int type) {
            this.variantData = type;
        }
    }

    public static class DefendVillageGuardGoal extends TargetGoal {
        private final Guard guard;
        private LivingEntity villageAggressorTarget;

        public DefendVillageGuardGoal(Guard guardIn) {
            super(guardIn, false, true);
            this.guard = guardIn;
            this.setFlags(EnumSet.of(Goal.Flag.TARGET, Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            AABB axisalignedbb = this.guard.getBoundingBox().inflate(10.0D, 8.0D, 10.0D);
            List<Villager> list = guard.level.getEntitiesOfClass(Villager.class, axisalignedbb);
            List<Player> list1 = guard.level.getEntitiesOfClass(Player.class, axisalignedbb);
            for (LivingEntity livingentity : list) {
                Villager villagerentity = (Villager) livingentity;
                for (Player playerentity : list1) {
                    int i = villagerentity.getPlayerReputation(playerentity);
                    if (i <= -100) {
                        this.villageAggressorTarget = playerentity;
                    }
                }
            }
            return villageAggressorTarget != null && !villageAggressorTarget.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) && !this.villageAggressorTarget.isSpectator() && !((Player) this.villageAggressorTarget).isCreative();
        }

        @Override
        public void start() {
            this.guard.setTarget(this.villageAggressorTarget);
            super.start();
        }
    }

    public static class FollowHeroGoal extends Goal {
        public final Guard guard;

        public FollowHeroGoal(Guard mob) {
            guard = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public void start() {
            super.start();
            if (guard.getOwner() != null) {
                guard.getNavigation().moveTo(guard.getOwner(), 0.9D);
            }
        }

        @Override
        public void tick() {
            if (guard.getOwner() != null) {
                guard.getNavigation().moveTo(guard.getOwner(), 0.9D);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return guard.isFollowing() && this.canUse();
        }

        @Override
        public boolean canUse() {
            List<Player> list = this.guard.level.getEntitiesOfClass(Player.class, this.guard.getBoundingBox().inflate(10.0D));
            if (!list.isEmpty()) {
                for (LivingEntity mob : list) {
                    Player player = (Player) mob;
                    if (!player.isInvisible() && player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE)) {
                        guard.setOwnerId(player.getUUID());
                        return guard.isFollowing();
                    }
                }
            }
            return false;
        }

        @Override
        public void stop() {
            this.guard.getNavigation().stop();
            if (guard.getOwner() != null && !guard.getOwner().hasEffect(MobEffects.HERO_OF_THE_VILLAGE)) {
                guard.setOwnerId(null);
                guard.setFollowing(false);
            }
        }
    }

    public class GuardMeleeGoal extends MeleeAttackGoal {
        public final Guard guard;

        public GuardMeleeGoal(Guard guard, double speedIn, boolean useLongMemory) {
            super(guard, speedIn, useLongMemory);
            this.guard = guard;
        }

        @Override
        public boolean canUse() {
            return !(this.guard.getMainHandItem().getItem() instanceof CrossbowItem) && this.guard.getTarget() != null && !this.guard.isEating() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return super.canContinueToUse() && this.guard.getTarget() != null && !(this.guard.getMainHandItem().getItem() instanceof CrossbowItem);
        }

        @Override
        public void tick() {
            LivingEntity target = guard.getTarget();
            if (target != null) {
                if (target.distanceTo(guard) <= 3.0D && !guard.isBlocking()) {
                    guard.getMoveControl().strafe(-2.0F, 0.0F);
                    guard.lookAt(target, 30.0F, 30.0F);
                }
                if (path != null && target.distanceTo(guard) <= 2.0D)
                    guard.getNavigation().stop();
                super.tick();
            }
        }

        @Override
        protected double getAttackReachSqr(LivingEntity attackTarget) {
            return super.getAttackReachSqr(attackTarget) * 3.55D;
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity enemy, double distToEnemySqr) {
            double d0 = this.getAttackReachSqr(enemy);
            if (distToEnemySqr <= d0 && this.ticksUntilNextAttack <= 0) {
                this.resetAttackCooldown();
                this.guard.stopUsingItem();
                if (guard.shieldCoolDown == 0)
                    this.guard.shieldCoolDown = 8;
                this.guard.swing(InteractionHand.MAIN_HAND);
                this.guard.doHurtTarget(enemy);
            }
        }
    }
}