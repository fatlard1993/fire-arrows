package justfatlard.fire_arrows;

import java.util.Locale;

/** What an arrow carries, which decides what happens where it lands. */
public enum Charge {
	/** Sets a torch on the face it strikes. */
	TORCH,
	/** Flies burning: sets what it hits alight, and lights TNT, candles and campfires it strikes. */
	FIRE,
	/** Goes off like TNT at whatever it strikes. */
	EXPLOSIVE;

	/** How an arrow in flight is marked with its charge: a tag, which is saved with the arrow. */
	public String tag() {
		return Main.MOD_ID + ":" + name().toLowerCase(Locale.ROOT);
	}

	public String itemName() {
		return name().toLowerCase(Locale.ROOT) + "_arrow";
	}
}
