package justfatlard.gold_golem;

import justfatlard.pandorical.api.PandoricalApi;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A golem built of gold that keeps the people around it alive, and the piglins around it calm.
 */
public class Main implements ModInitializer {
	public static final String MOD_ID = "gold-golem-justfatlard";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final String BLOCK_TIP = "block-tip";

	@Override
	public void onInitialize() {
		GoldGolems.register();
		// The skin, the egg's picture and the names.
		PandoricalApi.content().registerModAssets(MOD_ID);

		// Isolated in its own class and only reached from here: it names block-tip's types directly,
		// so it must not load without that mod.
		if (FabricLoader.getInstance().isModLoaded(BLOCK_TIP)) {
			justfatlard.gold_golem.integration.GoldGolemTips.register();
		}

		LOGGER.info("[{}] Gold golems", MOD_ID);
	}
}
