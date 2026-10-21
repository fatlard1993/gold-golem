package justfatlard.gold_golem.integration;

import justfatlard.block_tip.api.BlockTipApi;
import justfatlard.gold_golem.GoldGolem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * A gold golem's gilt, on the card. It is the server's alone, so looking is the only way to read it
 * short of walking up and clicking.
 *
 * <p>Compiled against block-tip's API and guarded at the call site by a mod-loaded check, so a
 * server without it never loads this class.
 */
public final class GoldGolemTips {
	private GoldGolemTips() {}

	public static void register() {
		BlockTipApi.describeEntity(GoldGolemTips::describe);
	}

	private static String describe(Entity entity, ServerPlayer player) {
		if (!(entity instanceof GoldGolem golem)) return null;
		return golem.giltReading().getString();
	}
}
