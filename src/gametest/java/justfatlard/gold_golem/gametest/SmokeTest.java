package justfatlard.gold_golem.gametest;

import justfatlard.pandorical.gametest.Smoke;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/**
 * This mod loads, its mixins bind, and a world comes up with it installed.
 *
 * <p>Nothing is asserted beyond getting there; {@link GoldGolemBehaviour} holds the golem to the
 * readme.
 */
public final class SmokeTest implements FabricClientGameTest {

	@Override
	public void runTest(ClientGameTestContext context) {
		Smoke.run(context, "gold-golem-justfatlard");
	}
}
