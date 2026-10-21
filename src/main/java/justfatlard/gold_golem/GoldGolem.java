package justfatlard.gold_golem;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A golem that mends rather than fights. Every few seconds it gives the hurt around it a burst of
 * regeneration, paid for out of its gilt, which comes back slowly by itself and quickly with gold
 * fed to it; and piglins near one leave you be, as if you were wearing gold yourself.
 *
 * <p>It runs the iron golem's code, so Pandorical clients can stand an iron golem in for it, wearing
 * a body of its own, and vanilla clients are shown a plain iron golem. It adds no synced data: the gilt it has to spend is
 * the server's alone.
 */
public class GoldGolem extends IronGolem {
	public static final int MAX_GILT = 64;
	/** What a newly built golem has to spend: a few minutes of work before it needs feeding. */
	public static final int STARTING_GILT = 16;
	/** One gilt back every this many ticks, by itself, up to the most it holds. */
	public static final int GILT_REGEN_TICKS = 600;
	public static final int GILT_PER_NUGGET = 1;
	public static final int GILT_PER_GOLDEN_CARROT = 8;
	/** An iron ingot gives an iron golem a quarter of its health back; a gold ingot does the same here. */
	private static final float INGOT_REPAIR = 10.0F;

	/** How near the hurt must be to be mended by a pulse. */
	public static final double MEND_RADIUS = 6.0;
	/** How far it will walk to reach someone hurt. */
	static final double SEEK_RADIUS = 16.0;
	/** How far it wanders from where it was built, or from where its lead was taken off. */
	private static final int HOME_RADIUS = 12;
	/** How near a golem must be for piglins to leave a player alone. */
	public static final double PEACE_RADIUS = 16.0;

	public static final int PULSE_INTERVAL = 100;
	/** A little longer than the interval, so someone standing by it never sees the effect lapse. */
	private static final int REGENERATION_TICKS = 120;
	private static final int REGENERATION_AMPLIFIER = 1;
	private static final int IDLE_RECHECK = 20;

	/** Drawn at the iron golem's height and shrunk to this, so what is seen and what is hit agree. */
	public static final double SCALE = 0.7;

	private int gilt = STARTING_GILT;
	private int pulseCooldown;
	private int regenCountdown = GILT_REGEN_TICKS;

	public GoldGolem(final EntityType<? extends GoldGolem> type, final Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 40.0)
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
			.add(Attributes.ATTACK_DAMAGE, 5.0)
			.add(Attributes.STEP_HEIGHT, 1.0)
			.add(Attributes.SCALE, SCALE);
	}

	// None of the iron golem's patrolling, village defence or flower giving: it hits back only at
	// what hits it, and never at a player, because it is always player made.
	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
		this.goalSelector.addGoal(2, new TendWoundedGoal(this, 1.0));
		this.goalSelector.addGoal(3, new MoveTowardsRestrictionGoal(this, 0.6));
		this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.6));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(
		final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, final @Nullable SpawnGroupData groupData
	) {
		this.setPlayerCreated(true);
		this.settle();
		return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
	}

	/** Makes wherever it stands now the patch it keeps to. */
	public void settle() {
		this.setHomeTo(this.blockPosition(), HOME_RADIUS);
	}

	@Override
	public void onLeashRemoved() {
		super.onLeashRemoved();
		if (this.getLeashData() == null) this.settle();
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (!(this.level() instanceof ServerLevel level)) return;
		if (--this.pulseCooldown <= 0) this.pulseCooldown = this.pulse(level) ? PULSE_INTERVAL : IDLE_RECHECK;
		if (--this.regenCountdown <= 0) {
			this.regenCountdown = GILT_REGEN_TICKS;
			this.setGilt(this.gilt + 1);
		}
	}

	/** Mends everyone hurt within reach, for one gilt. False when there was no one, or nothing to pay with. */
	boolean pulse(final ServerLevel level) {
		if (this.gilt <= 0) return false;
		List<LivingEntity> hurt = this.woundedAllies(MEND_RADIUS);
		if (hurt.isEmpty()) return false;

		this.gilt--;
		for (LivingEntity ally : hurt) {
			ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGENERATION_TICKS, REGENERATION_AMPLIFIER), this);
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, ally.getX(), ally.getY() + ally.getBbHeight() * 0.6, ally.getZ(),
				6, ally.getBbWidth() * 0.4, ally.getBbHeight() * 0.3, ally.getBbWidth() * 0.4, 0.0);
		}
		level.sendParticles(ParticleTypes.WAX_ON, this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(),
			16, 0.8, 0.8, 0.8, 0.5);
		// The bell on its head: a note block over gold plays the bell.
		this.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 1.5F, 0.8F + this.random.nextFloat() * 0.4F);
		return true;
	}

	List<LivingEntity> woundedAllies(final double radius) {
		return this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius),
			entity -> entity != this && entity.isAlive() && entity.getHealth() < entity.getMaxHealth()
				&& entity != this.getTarget() && isAlly(entity));
	}

	/** Players, villagers, golems and anything someone has tamed. Never a monster, tame or not. */
	static boolean isAlly(final LivingEntity entity) {
		if (entity instanceof Enemy) return false;
		if (entity instanceof Player player) return !player.isSpectator();
		if (entity instanceof AbstractVillager || entity instanceof AbstractGolem) return true;
		return entity instanceof OwnableEntity owned && owned.getOwnerReference() != null;
	}

	public boolean hasGilt() {
		return this.gilt > 0;
	}

	public int getGilt() {
		return this.gilt;
	}

	public void setGilt(final int gilt) {
		this.gilt = Math.clamp(gilt, 0, MAX_GILT);
	}

	@Override
	protected InteractionResult mobInteract(final Player player, final InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (held.is(Items.GOLD_INGOT)) return this.repair(player, held);
		int worth = held.is(Items.GOLD_NUGGET) ? GILT_PER_NUGGET : held.is(Items.GOLDEN_CARROT) ? GILT_PER_GOLDEN_CARROT : 0;
		if (worth > 0) return this.feed(player, held, worth);
		if (held.isEmpty() && hand == InteractionHand.MAIN_HAND) {
			if (!this.level().isClientSide()) player.sendOverlayMessage(this.giltReading());
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	private InteractionResult repair(final Player player, final ItemStack ingot) {
		float before = this.getHealth();
		this.heal(INGOT_REPAIR);
		if (this.getHealth() == before) return InteractionResult.PASS;
		this.playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.0F, 1.3F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
		ingot.consume(1, player);
		return InteractionResult.SUCCESS;
	}

	private InteractionResult feed(final Player player, final ItemStack food, final int worth) {
		if (this.gilt >= MAX_GILT) {
			if (!this.level().isClientSide()) player.sendOverlayMessage(this.giltReading());
			return InteractionResult.PASS;
		}
		this.setGilt(this.gilt + worth);
		this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.4F);
		if (this.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.WAX_ON, this.getX(), this.getY() + this.getBbHeight() * 0.7, this.getZ(),
				8, 0.5, 0.5, 0.5, 0.3);
			player.sendOverlayMessage(this.giltReading());
		}
		food.consume(1, player);
		return InteractionResult.SUCCESS;
	}

	public Component giltReading() {
		return Component.translatableWithFallback("entity.gold-golem-justfatlard.gold_golem.gilt",
			"Gilt: %s of %s", this.gilt, MAX_GILT);
	}

	@Override
	protected void addAdditionalSaveData(final ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Gilt", this.gilt);
	}

	@Override
	protected void readAdditionalSaveData(final ValueInput input) {
		super.readAdditionalSaveData(input);
		this.setGilt(input.getIntOr("Gilt", STARTING_GILT));
	}

	// Gold rings where iron clangs.
	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 1.35F;
	}

	@Override
	protected Component getTypeName() {
		return Component.translatableWithFallback(this.getType().getDescriptionId(), "Gold Golem");
	}

	/** Whether a gold golem stands near enough to {@code entity} to keep the piglins around it calm. */
	public static boolean isKeepingPeace(final LivingEntity entity) {
		return !entity.level().getEntitiesOfClass(GoldGolem.class, entity.getBoundingBox().inflate(PEACE_RADIUS),
			LivingEntity::isAlive).isEmpty();
	}
}
