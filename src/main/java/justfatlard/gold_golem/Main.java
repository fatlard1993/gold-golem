package justfatlard.gold_golem;

import justfatlard.pandorical.api.PandoricalApi;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A golem built of gold that keeps the people around it alive, and the piglins around it calm.
 */
public class Main implements ModInitializer {
	public static final String MOD_ID = "gold-golem-justfatlard";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		GoldGolems.register();
		// The skin, the egg's picture and the names.
		PandoricalApi.content().registerModAssets(MOD_ID);

		LOGGER.info("[{}] Gold golems", MOD_ID);
	}
}
