package justfatlard.fire_arrows;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** {@code config/fire-arrows.json}, written with its defaults on first run. */
public final class FireArrowsConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(Main.MOD_ID);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("fire-arrows.json");

	/** Off, an explosive arrow still hurts what it hits and leaves the ground as it was. */
	public boolean explosiveBreaksBlocks = true;

	public static FireArrowsConfig load() {
		FireArrowsConfig config = new FireArrowsConfig();
		if (Files.exists(PATH)) {
			try (Reader reader = Files.newBufferedReader(PATH)) {
				FireArrowsConfig read = GSON.fromJson(reader, FireArrowsConfig.class);
				if (read != null) config = read;
			} catch (IOException | RuntimeException e) {
				LOGGER.error("Failed to read {}, using defaults", PATH, e);
			}
		}
		config.save();
		return config;
	}

	public void setExplosiveBreaksBlocks(boolean on) {
		explosiveBreaksBlocks = on;
		save();
	}

	private void save() {
		try {
			Files.createDirectories(PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(PATH)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			LOGGER.error("Failed to write {}", PATH, e);
		}
	}
}
