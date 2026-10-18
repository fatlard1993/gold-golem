package justfatlard.gold_golem;

import java.util.function.Predicate;
import justfatlard.pandorical.api.EntityRegistration;
import justfatlard.pandorical.api.ItemRegistration;
import justfatlard.pandorical.api.PandoricalApi;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;

/**
 * The gold golem as the game knows it: an entity type, its spawn egg, how Pandorical draws it, and
 * the iron golem's T of blocks, in gold, that builds one.
 *
 * <p>Pandorical clients run the iron golem's class in its place and draw it from a body of its
 * own, which the iron golem's model class animates by part name. Vanilla clients are shown an iron
 * golem. It is an iron golem's size, and smaller only through its scale attribute, which every
 * client reads.
 */
public final class GoldGolems {
	private GoldGolems() {}

	private static final Identifier ID = Identifier.fromNamespaceAndPath(Main.MOD_ID, "gold_golem");

	public static final EntityType<GoldGolem> GOLD_GOLEM = FabricEntityType.Builder.createMob(GoldGolem::new,
			MobCategory.MISC, mob -> mob.defaultAttributes(GoldGolem::createAttributes))
		.sized(EntityTypes.IRON_GOLEM.getWidth(), EntityTypes.IRON_GOLEM.getHeight())
		.eyeHeight(EntityTypes.IRON_GOLEM.getDimensions().eyeHeight())
		.clientTrackingRange(EntityTypes.IRON_GOLEM.clientTrackingRange())
		.build(ResourceKey.create(Registries.ENTITY_TYPE, ID));

	private static final Identifier EGG_ID = Identifier.fromNamespaceAndPath(Main.MOD_ID, "gold_golem_spawn_egg");

	public static final Item SPAWN_EGG = new SpawnEggItem(new Item.Properties()
		.setId(ResourceKey.create(Registries.ITEM, EGG_ID))
		.spawnEgg(GOLD_GOLEM));

	/** Its own body and skin, from {@code generate_textures.py}, moved by the iron golem's animation. */
	public static final Identifier MODEL = Identifier.fromNamespaceAndPath(Main.MOD_ID, "gold_golem");
	public static final Identifier SKIN = Identifier.fromNamespaceAndPath(Main.MOD_ID, "textures/entity/gold_golem.png");

	private static final Predicate<BlockState> PUMPKINS = state -> state.is(Blocks.CARVED_PUMPKIN) || state.is(Blocks.JACK_O_LANTERN);

	private static final BlockPattern BASE = BlockPatternBuilder.start()
		.aisle("~ ~", "###", "~#~")
		.where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.GOLD_BLOCK)))
		.where('~', BlockInWorld.hasState(BlockBehaviour.BlockStateBase::isAir))
		.build();

	private static final BlockPattern FULL = BlockPatternBuilder.start()
		.aisle("~^~", "###", "~#~")
		.where('^', BlockInWorld.hasState(PUMPKINS))
		.where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.GOLD_BLOCK)))
		.where('~', BlockInWorld.hasState(BlockBehaviour.BlockStateBase::isAir))
		.build();

	public static void register() {
		Registry.register(BuiltInRegistries.ENTITY_TYPE, ID, GOLD_GOLEM);
		Registry.register(BuiltInRegistries.ITEM, EGG_ID, SPAWN_EGG);

		PandoricalApi.content().registerEntity(GOLD_GOLEM, new EntityRegistration()
			.base(EntityTypes.IRON_GOLEM)
			.model(MODEL)
			.texture(SKIN)
			.vanillaFallback());
		PandoricalApi.content().registerItem(EGG_ID.toString(), new ItemRegistration());

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)
			.register(output -> output.accept(SPAWN_EGG));
	}

	/** Whether a pumpkin set on {@code top} would finish a gold golem; what a dispenser asks before placing one. */
	public static boolean canBuild(final LevelReader level, final BlockPos top) {
		return BASE.find(level, top) != null;
	}

	/** Builds a gold golem out of the blocks around {@code top}, the way the game builds an iron one. */
	public static boolean tryBuild(final Level level, final BlockPos top) {
		if (!(level instanceof ServerLevel server)) return false;
		BlockPattern.BlockPatternMatch match = FULL.find(level, top);
		if (match == null) return false;
		GoldGolem golem = GOLD_GOLEM.create(level, EntitySpawnReason.TRIGGERED);
		if (golem == null) return false;

		CarvedPumpkinBlock.clearPatternBlocks(level, match);
		BlockPos feet = match.getBlock(1, 2, 0).getPos();
		golem.snapTo(feet.getX() + 0.5, feet.getY() + 0.05, feet.getZ() + 0.5, 0.0F, 0.0F);
		golem.setPlayerCreated(true);
		golem.settle();
		server.addFreshEntity(golem);
		for (ServerPlayer player : server.getEntitiesOfClass(ServerPlayer.class, golem.getBoundingBox().inflate(5.0))) {
			CriteriaTriggers.SUMMONED_ENTITY.trigger(player, golem);
		}
		CarvedPumpkinBlock.updatePatternBlocks(level, match);
		return true;
	}
}
