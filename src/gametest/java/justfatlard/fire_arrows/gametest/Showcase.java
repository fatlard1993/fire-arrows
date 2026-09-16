package justfatlard.fire_arrows.gametest;

import justfatlard.fire_arrows.Charge;
import justfatlard.fire_arrows.Main;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The pictures for the readme and the mod page: the three arrows framed, a torch arrow lighting a
 * wall at night, and the hole an explosive one takes out of it.
 *
 * <p>Not an assertion in sight. This builds the scene, stands the player where it reads, and saves
 * the frames; whether the arrows work is {@link ArrowsLand}'s job. Run it under xvfb-run and the
 * frames land in build/run/clientGameTest/screenshots.
 */
public final class Showcase implements FabricClientGameTest {

	/** Big enough to read on a mod page, and the shape every other screenshot in the suite is. */
	private static final int WIDTH = 1920;
	private static final int HEIGHT = 1080;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			TestServerConnection connection = world.getConnection();
			connection.waitForChunksRender();

			// Nothing between the camera and the scene: no hand, no hotbar, no hearts. F1 through
			// the game's own binding rather than a field, which has moved before.
			context.getInput().pressKey(options -> options.keyToggleGui);
			context.runOnClient(client -> client.options.gamma().set(1.0));
			server.runCommand("gamerule doDaylightCycle false");
			server.runCommand("gamerule doWeatherCycle false");
			server.runCommand("gamemode spectator @a");

			BlockPos origin = server.computeOnServer(s -> connection.getServerPlayer().blockPosition());

			framed(context, server, connection, origin);
			torchAtNight(context, server, connection, origin);
			theHole(context, server, connection, origin);
		}
	}

	/** The three of them hung on a wall, in the order they are made. */
	private void framed(ClientGameTestContext context, TestServerContext server,
			TestServerConnection connection, BlockPos origin) {
		BlockPos wall = origin.north(6);
		server.runCommand("time set noon");
		server.runOnServer(s -> {
			ServerLevel level = s.overworld();
			for (int x = -3; x <= 3; x++) {
				for (int y = 0; y <= 3; y++) {
					level.setBlockAndUpdate(wall.offset(x, y, 0), Blocks.STONE_BRICKS.defaultBlockState());
				}
			}
			int x = -2;
			for (Charge charge : Charge.values()) {
				ItemFrame frame = new ItemFrame(level, wall.offset(x, 2, 1), Direction.SOUTH);
				frame.setItem(new ItemStack(Main.ITEMS.get(charge)), false);
				frame.setInvisible(true);
				level.addFreshEntity(frame);
				x += 2;
			}
		});
		stand(server, wall.getX() + 0.5, wall.getY() + 0.9, wall.getZ() + 3.4, 180, 0);
		context.waitTicks(20);
		shoot(context, "arrows");
	}

	/** What a torch arrow is for: a wall lit from where you stand, in the dark. */
	private void torchAtNight(ClientGameTestContext context, TestServerContext server,
			TestServerConnection connection, BlockPos origin) {
		BlockPos wall = origin.east(20);
		server.runCommand("time set midnight");
		server.runOnServer(s -> {
			ServerLevel level = s.overworld();
			for (int z = -3; z <= 3; z++) {
				for (int y = 0; y <= 5; y++) {
					level.setBlockAndUpdate(wall.offset(0, y, z), Blocks.DEEPSLATE.defaultBlockState());
				}
			}
			fire(level, Charge.TORCH, Vec3.atCenterOf(wall.west(7).above(2)), Vec3.atCenterOf(wall.above(2)));
		});
		stand(server, wall.getX() - 3.6, wall.getY() + 0.9, wall.getZ() + 1.8, 253, 0);
		context.waitTicks(40);
		shoot(context, "torch-arrow");
	}

	/** And what an explosive one is for: a way through, made from where you stand. */
	private void theHole(ClientGameTestContext context, TestServerContext server,
			TestServerConnection connection, BlockPos origin) {
		BlockPos wall = origin.south(20);
		server.runCommand("time set noon");
		server.runOnServer(s -> {
			ServerLevel level = s.overworld();
			for (int x = -4; x <= 4; x++) {
				for (int y = 0; y <= 6; y++) {
					level.setBlockAndUpdate(wall.offset(x, y, 0), Blocks.STONE_BRICKS.defaultBlockState());
				}
			}
		});
		stand(server, wall.getX() + 0.5, wall.getY() + 1.4, wall.getZ() - 7.5, 0, 0);
		context.waitTicks(20);
		server.runOnServer(s -> fire(s.overworld(), Charge.EXPLOSIVE,
			Vec3.atCenterOf(wall.south(6).above(3)), Vec3.atCenterOf(wall.above(3))));
		// Two frames, because they say different things: the blast as it goes, and the way through
		// it left once the smoke is gone.
		context.waitTicks(11);
		shoot(context, "explosive-arrow");
		context.waitTicks(60);
		shoot(context, "explosive-arrow-hole");
	}

	/**
	 * Put the camera here, looking this way.
	 *
	 * <p>The y is where the feet go, so what the camera looks at is 1.62 higher: aim at a thing's
	 * height minus an eye. Every frame came out looking over the top of the scene until this was.
	 *
	 * <p>Through /tp rather than {@code snapTo}: a server-side move of a player is a teleport the
	 * client has to confirm, and an unconfirmed one is undone a moment later by the position the
	 * client keeps sending. Every frame came back as untouched flat ground until this went through
	 * the command.
	 */
	private void stand(TestServerContext server, double x, double y, double z, int yaw, int pitch) {
		server.runCommand("tp @a %.2f %.2f %.2f %d %d".formatted(x, y, z, yaw, pitch));
	}

	private void shoot(ClientGameTestContext context, String name) {
		context.takeScreenshot(TestScreenshotOptions.of(name)
			.withSize(WIDTH, HEIGHT)
			.disableCounterPrefix());
	}

	/** As {@link ArrowsLand} fires them: straight and quick, so the shot is where it is aimed. */
	private static void fire(ServerLevel level, Charge charge, Vec3 from, Vec3 at) {
		ItemStack stack = new ItemStack(Main.ITEMS.get(charge));
		var arrow = (AbstractArrow) Main.ITEMS.get(charge).asProjectile(level, from, stack, Direction.NORTH);
		Vec3 path = at.subtract(from);
		arrow.shoot(path.x, path.y, path.z, 3.0F, 0.0F);
		arrow.setNoGravity(true);
		level.addFreshEntity(arrow);
	}
}
