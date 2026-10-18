package justfatlard.gold_golem.gametest;

import java.util.List;
import java.util.UUID;
import justfatlard.gold_golem.GoldGolem;
import justfatlard.gold_golem.GoldGolems;
import justfatlard.pandorical.entitykind.EntityKinds;
import justfatlard.pandorical.gametest.Smoke;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * What the readme promises, on a stone floor in the sky: a T of gold blocks under a pumpkin
 * builds a gold golem and an iron T still builds an iron one; the golem mends a hurt villager for
 * one gilt, eats nuggets and carrots, is repaired by ingots, and gets gilt back by itself; a player beside one counts to a
 * piglin as wearing gold; and it is drawn as an iron golem by a Pandorical client and sent to a
 * vanilla one as an iron golem.
 */
public final class GoldGolemBehaviour implements FabricClientGameTest {

	private static final int FLOOR = 24;

	@Override
	public void runTest(ClientGameTestContext context) {
		Smoke.run(context, "gold-golem-justfatlard", session -> {
			BlockPos[] floor = new BlockPos[1];
			UUID[] golemId = new UUID[1];
			UUID[] villagerId = new UUID[1];

			// Built the way an iron golem is, out of gold, and the iron golem still built out of iron.
			session.onServer(server -> {
				var source = server.createCommandSourceStack().withSuppressedOutput();
				server.getCommands().performPrefixedCommand(source, "gamerule spawn_mobs false");
				server.getCommands().performPrefixedCommand(source, "gamemode survival @a");
				ServerPlayer player = session.player();
				ServerLevel level = (ServerLevel) player.level();
				floor[0] = player.blockPosition().above(40);
				lay(level, floor[0]);
				player.teleportTo(floor[0].getX() + 2.5, floor[0].getY() + 1, floor[0].getZ() + 2.5);

				BlockPos gold = floor[0].offset(FLOOR / 2, 1, FLOOR / 2);
				build(level, gold, Blocks.GOLD_BLOCK);
				List<GoldGolem> built = level.getEntitiesOfClass(GoldGolem.class, around(gold));
				check(built.size() == 1, built.size() + " gold golems from one T of gold");
				check(level.getBlockState(gold).isAir() && level.getBlockState(gold.above(2)).isAir(),
					"the blocks a gold golem was built from were left standing");
				GoldGolem golem = built.getFirst();
				check(golem.isPlayerCreated(), "a built gold golem is not player made");
				check(golem.getGilt() == GoldGolem.STARTING_GILT, "a built gold golem has " + golem.getGilt() + " gilt");
				check(golem.hasHome() && golem.getHomePosition().closerThan(gold, 2),
					"a built gold golem keeps to no patch near where it was built");
				check("Gold Golem".equals(golem.getName().getString()), "a gold golem is called " + golem.getName().getString());
				check(Math.abs(golem.getScale() - GoldGolem.SCALE) < 0.001F, "a gold golem is scale " + golem.getScale());
				golemId[0] = golem.getUUID();

				BlockPos iron = floor[0].offset(3, 1, FLOOR - 4);
				build(level, iron, Blocks.IRON_BLOCK);
				List<IronGolem> ironBuilt = level.getEntitiesOfClass(IronGolem.class, around(iron), each -> !(each instanceof GoldGolem));
				check(ironBuilt.size() == 1, ironBuilt.size() + " iron golems from one T of iron");
				ironBuilt.forEach(Entity::discard);

				// Hurt within reach, with its feet nailed down so it cannot wander out of it.
				Villager villager = EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
				villager.snapTo(golem.getX() + 2, golem.getY(), golem.getZ());
				villager.setNoAi(true);
				level.addFreshEntity(villager);
				villager.setHealth(4F);
				villagerId[0] = villager.getUUID();
			});

			// Mended, for one gilt.
			session.waitTicks(60);
			session.onServer(server -> {
				ServerLevel level = (ServerLevel) session.player().level();
				GoldGolem golem = (GoldGolem) level.getEntity(golemId[0]);
				Villager villager = (Villager) level.getEntity(villagerId[0]);
				check(villager.hasEffect(MobEffects.REGENERATION), "a hurt villager beside a gold golem was not mended");
				check(golem.getGilt() == GoldGolem.STARTING_GILT - 1,
					"one mend cost " + (GoldGolem.STARTING_GILT - golem.getGilt()) + " gilt");

				// An empty golem mends nobody.
				villager.removeAllEffects();
				villager.setHealth(4F);
				golem.setGilt(0);
			});
			session.waitTicks(GoldGolem.PULSE_INTERVAL + 20);
			session.onServer(server -> {
				ServerPlayer player = session.player();
				ServerLevel level = (ServerLevel) player.level();
				GoldGolem golem = (GoldGolem) level.getEntity(golemId[0]);
				Villager villager = (Villager) level.getEntity(villagerId[0]);
				check(!villager.hasEffect(MobEffects.REGENERATION), "a gold golem with no gilt mended a villager");

				// Fed and repaired by hand.
				feed(golem, player, Items.GOLD_NUGGET);
				check(golem.getGilt() == GoldGolem.GILT_PER_NUGGET, "a nugget gave " + golem.getGilt() + " gilt");
				feed(golem, player, Items.GOLDEN_CARROT);
				check(golem.getGilt() == GoldGolem.GILT_PER_NUGGET + GoldGolem.GILT_PER_GOLDEN_CARROT,
					"a golden carrot gave " + (golem.getGilt() - GoldGolem.GILT_PER_NUGGET) + " gilt");
				golem.setHealth(10F);
				feed(golem, player, Items.GOLD_INGOT);
				check(golem.getHealth() > 10F, "a gold ingot did not repair a gold golem");
				check(golem.getGilt() == GoldGolem.GILT_PER_NUGGET + GoldGolem.GILT_PER_GOLDEN_CARROT,
					"a gold ingot was taken as gilt");

				// Piglins take a player beside one for a player in gold, and one far from it for one who is not.
				check(PiglinAi.isWearingSafeArmor(player), "a player beside a gold golem is a target to piglins");
				Vec3 back = player.position();
				player.teleportTo(back.x + GoldGolem.PEACE_RADIUS + 16, back.y, back.z);
				check(!PiglinAi.isWearingSafeArmor(player), "a player far from any gold golem is safe from piglins");
				player.teleportTo(back.x, back.y, back.z);

				// A vanilla client is shown an iron golem in its place; a Pandorical client is shown the gold golem.
				ClientboundAddEntityPacket add = new ClientboundAddEntityPacket(golem.getId(), golem.getUUID(),
					golem.getX(), golem.getY(), golem.getZ(), 0F, 0F, golem.getType(), 0, Vec3.ZERO, 0);
				EntityKinds.understood(player.getUUID(), false);
				try {
					check(EntityKinds.mayShow(golem, player), "a vanilla client was refused a gold golem");
					check(EntityKinds.forPlayer(add, player).getType() == EntityTypes.IRON_GOLEM,
						"a vanilla client was sent " + EntityKinds.forPlayer(add, player).getType());
				} finally {
					EntityKinds.understood(player.getUUID(), true);
				}
				check(EntityKinds.forPlayer(add, player).getType() == GoldGolems.GOLD_GOLEM,
					"a Pandorical client was sent " + EntityKinds.forPlayer(add, player).getType());

				// Left with nobody to mend and nothing to spend, for the time it takes gilt to come back.
				villager.setHealth(villager.getMaxHealth());
				golem.setGilt(0);
			});

			// Its gilt comes back by itself.
			session.waitTicks(GoldGolem.GILT_REGEN_TICKS + 20);
			session.onServer(server -> {
				GoldGolem golem = (GoldGolem) ((ServerLevel) session.player().level()).getEntity(golemId[0]);
				check(golem.getGilt() == 1, "a gold golem regained " + golem.getGilt() + " gilt in " + GoldGolem.GILT_REGEN_TICKS + " ticks");
			});

			// Drawn by the iron golem's renderer, under the gold golem's own type.
			session.waitTicks(10);
			String drawn = session.context().computeOnClient(client -> {
				for (Entity each : client.level.entitiesForRendering()) {
					if (!each.getUUID().equals(golemId[0])) continue;
					var renderer = client.getEntityRenderDispatcher().getRenderer(each);
					return (renderer == null ? "nothing" : renderer.getClass().getSimpleName()) + " for "
						+ BuiltInRegistries.ENTITY_TYPE.getKey(each.getType());
				}
				return "no gold golem on the client";
			});
			check(drawn.equals("IronGolemRenderer for gold-golem-justfatlard:gold_golem"), "the client drew " + drawn);
		});
	}

	private static void feed(GoldGolem golem, ServerPlayer player, net.minecraft.world.item.Item item) {
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
		golem.interact(player, InteractionHand.MAIN_HAND, golem.position());
		check(player.getMainHandItem().isEmpty(), "feeding a gold golem " + item + " did not use it up");
	}

	/** The iron golem's T: two blocks of body over one of legs, arms out, and the pumpkin last. */
	private static void build(ServerLevel level, BlockPos feet, net.minecraft.world.level.block.Block body) {
		level.setBlockAndUpdate(feet, body.defaultBlockState());
		level.setBlockAndUpdate(feet.above(), body.defaultBlockState());
		level.setBlockAndUpdate(feet.above().east(), body.defaultBlockState());
		level.setBlockAndUpdate(feet.above().west(), body.defaultBlockState());
		level.setBlockAndUpdate(feet.above(2), Blocks.CARVED_PUMPKIN.defaultBlockState());
	}

	private static void lay(ServerLevel level, BlockPos corner) {
		for (int x = 0; x < FLOOR; x++) for (int z = 0; z < FLOOR; z++) {
			level.setBlockAndUpdate(corner.offset(x, 0, z), Blocks.STONE.defaultBlockState());
			for (int y = 1; y <= 4; y++) level.setBlockAndUpdate(corner.offset(x, y, z), Blocks.AIR.defaultBlockState());
		}
	}

	private static AABB around(BlockPos pos) {
		return new AABB(pos).inflate(3);
	}

	private static void check(boolean condition, String failure) {
		if (!condition) throw new AssertionError(failure);
	}
}
