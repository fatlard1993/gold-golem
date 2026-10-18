package justfatlard.gold_golem.mixin;

import justfatlard.gold_golem.GoldGolems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CarvedPumpkinBlock.class)
abstract class CarvedPumpkinBlockMixin {

	@Inject(method = "trySpawnGolem", at = @At("HEAD"), cancellable = true)
	private void goldGolem$build(final Level level, final BlockPos topPos, final CallbackInfo ci) {
		if (GoldGolems.tryBuild(level, topPos)) ci.cancel();
	}

	@Inject(method = "canSpawnGolem", at = @At("RETURN"), cancellable = true)
	private void goldGolem$canBuild(final LevelReader level, final BlockPos topPos, final CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ() && GoldGolems.canBuild(level, topPos)) cir.setReturnValue(true);
	}
}
