package justfatlard.gold_golem;

import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jspecify.annotations.Nullable;

/**
 * Walks to whoever is hurt nearby, close enough for the next pulse to reach them. Only while it has
 * gilt to spend: an empty golem has nothing to bring.
 */
final class TendWoundedGoal extends Goal {
	/** Inside the pulse's reach with room to spare, so a patient who shuffles is still covered. */
	private static final double CLOSE_ENOUGH = GoldGolem.MEND_RADIUS - 2.0;

	private final GoldGolem golem;
	private final double speed;
	private @Nullable LivingEntity patient;
	private int repath;

	TendWoundedGoal(final GoldGolem golem, final double speed) {
		this.golem = golem;
		this.speed = speed;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (!this.golem.hasGilt() || this.golem.getRandom().nextInt(10) != 0) return false;
		this.patient = this.golem.woundedAllies(GoldGolem.SEEK_RADIUS).stream()
			.filter(ally -> ally.distanceToSqr(this.golem) > CLOSE_ENOUGH * CLOSE_ENOUGH)
			.min(Comparator.comparingDouble(ally -> ally.distanceToSqr(this.golem)))
			.orElse(null);
		return this.patient != null;
	}

	@Override
	public boolean canContinueToUse() {
		return this.patient != null && this.patient.isAlive() && this.golem.hasGilt()
			&& this.patient.getHealth() < this.patient.getMaxHealth()
			&& this.patient.distanceToSqr(this.golem) > CLOSE_ENOUGH * CLOSE_ENOUGH
			&& this.patient.distanceToSqr(this.golem) < GoldGolem.SEEK_RADIUS * GoldGolem.SEEK_RADIUS * 1.5;
	}

	@Override
	public void start() {
		this.repath = 0;
	}

	@Override
	public void tick() {
		this.golem.getLookControl().setLookAt(this.patient, 10.0F, this.golem.getMaxHeadXRot());
		if (--this.repath <= 0) {
			this.repath = 10;
			this.golem.getNavigation().moveTo(this.patient, this.speed);
		}
	}

	@Override
	public void stop() {
		this.patient = null;
		this.golem.getNavigation().stop();
	}
}
