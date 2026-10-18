package justfatlard.gold_golem.gametest;

import justfatlard.gold_golem.GoldGolem;
import justfatlard.gold_golem.GoldGolems;
import justfatlard.pandorical.api.PandoricalApi;
import justfatlard.pandorical.gametest.Pictures;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * A gold golem beside an iron one, from the front and from the side, into
 * {@code build/run/clientGameTest/screenshots/}: the way to see a change to the body. And the
 * client is holding it to the body this mod ships, not the iron golem's.
 */
public final class GoldGolemPictured implements FabricClientGameTest {

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			TestServerConnection connection = world.getConnection();
			connection.waitForChunksRender();
			server.waitFor(s -> PandoricalApi.isContentReady(connection.getServerPlayer()));
			Pictures.stage(context, server, Pictures.MORNING);

			int[] gold = new int[1];
			Vec3 centre = server.computeOnServer(s -> {
				ServerLevel level = s.overworld();
				BlockPos ground = Pictures.ground(level, connection.getServerPlayer().getBlockX(),
					connection.getServerPlayer().getBlockZ());
				Pictures.clearAround(level, ground, 6, 6);
				Vec3 at = Vec3.atBottomCenterOf(ground);
				gold[0] = pose(level, GoldGolems.GOLD_GOLEM.create(level, EntitySpawnReason.COMMAND), at).getId();
				pose(level, EntityTypes.IRON_GOLEM.create(level, EntitySpawnReason.COMMAND), at.add(2.6, 0, 0));
				return at;
			});
			context.waitTicks(20);

			// Drawn from this mod's body, built into the iron golem's model class: a body that class
			// refused would leave the golem drawn as a plain iron golem, and say so only in the log.
			String model = context.computeOnClient(client -> {
				Entity golem = client.level.getEntity(gold[0]);
				if (golem == null) return "no gold golem on the client";
				return drawnModel(client.getEntityRenderDispatcher().getRenderer(golem).createRenderState(golem, 1F));
			});
			if (!model.equals(GoldGolems.MODEL.toString())) {
				throw new AssertionError("the client draws the gold golem with " + model);
			}

			Pictures.look(server, centre.add(1.3, 1.6, -6.5), centre.add(1.3, 1.1, 0));
			Pictures.settle(context, connection);
			System.out.println("[gold-golem test] picture " + Pictures.shoot(context, connection, "gold-golem-front").toAbsolutePath());

			Pictures.look(server, centre.add(-4.6, 2.2, -3.4), centre.add(0, 1.0, 0));
			Pictures.settle(context, connection);
			System.out.println("[gold-golem test] picture " + Pictures.shoot(context, connection, "gold-golem-quarter").toAbsolutePath());
		}
	}

	/**
	 * What Pandorical's client has dressed a render state in: the model's id, or why not. Read by
	 * reflection because the test kit can only pose an entity with a body of its own
	 * ({@code Models.posedBody}), not one standing on a vanilla base, and the client's own classes
	 * are not on the tests' compile path.
	 */
	private static String drawnModel(Object state) {
		try {
			Class<?> models = Class.forName("justfatlard.pandorical.client.entitymodel.ClientEntityModels");
			Object drawn = models.getMethod("drawn", Object.class).invoke(null, state);
			if (drawn == null) return "nothing of its own";
			Object id = drawn.getClass().getMethod("id").invoke(drawn);
			Object built = drawn.getClass().getMethod("model").invoke(drawn);
			return built == null ? id + ", which its renderer would not build" : String.valueOf(id);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("could not read what Pandorical draws the golem with", e);
		}
	}

	/** Still, facing -z, toward the camera. */
	private static Mob pose(ServerLevel level, Mob mob, Vec3 at) {
		mob.snapTo(at.x, at.y, at.z, 180F, 0F);
		mob.setYHeadRot(180F);
		mob.setNoAi(true);
		mob.setPersistenceRequired();
		if (mob instanceof GoldGolem golem) golem.setPlayerCreated(true);
		level.addFreshEntity(mob);
		return mob;
	}
}
