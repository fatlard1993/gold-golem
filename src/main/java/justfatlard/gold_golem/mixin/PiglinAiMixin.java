package justfatlard.gold_golem.mixin;

import justfatlard.gold_golem.GoldGolem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A player standing near a gold golem counts, to a piglin, as a player wearing gold. */
@Mixin(PiglinAi.class)
abstract class PiglinAiMixin {

	@Inject(method = "isWearingSafeArmor", at = @At("RETURN"), cancellable = true)
	private static void goldGolem$keptCalm(final LivingEntity entity, final CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ() && GoldGolem.isKeepingPeace(entity)) cir.setReturnValue(true);
	}
}
