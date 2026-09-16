package justfatlard.fire_arrows;

import justfatlard.pandorical.api.ItemRegistration;
import justfatlard.pandorical.api.PandoricalApi;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;

public class Main implements ModInitializer {
	public static final String MOD_ID = "fire-arrows-justfatlard";
	private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Map<Charge, CarryingArrowItem> ITEMS = new EnumMap<>(Charge.class);

	@Override
	public void onInitialize() {
		for (Charge charge : Charge.values()) {
			Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, charge.itemName());
			CarryingArrowItem item = new CarryingArrowItem(charge,
				new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)));
			Registry.register(BuiltInRegistries.ITEM, id, item);
			ITEMS.put(charge, item);

			PandoricalApi.content().registerItem(id.toString(), new ItemRegistration()
				.model(MOD_ID + ":item/" + charge.itemName())
				.maxStackSize(64));
		}
		PandoricalApi.content().registerModAssets(MOD_ID);

		// Beside the arrows the game already has, in the order they escalate.
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output ->
			output.insertAfter(Items.SPECTRAL_ARROW, ITEMS.get(Charge.TORCH), ITEMS.get(Charge.FIRE),
				ITEMS.get(Charge.EXPLOSIVE)));

		FireArrowsConfig config = FireArrowsConfig.load();
		ArrowHits.configure(config);
		PandoricalApi.settings().serverGroup(MOD_ID, "Fire Arrows")
			.toggle("explosiveBreaksBlocks", "Explosive arrows break blocks", true)
			.describe("Off, they still hurt what they hit; the ground is left as it was")
			.backedBy(player -> config.explosiveBreaksBlocks, (player, on) -> config.setExplosiveBreaksBlocks(on));

		LOGGER.info("Loaded fire-arrows");
	}
}
