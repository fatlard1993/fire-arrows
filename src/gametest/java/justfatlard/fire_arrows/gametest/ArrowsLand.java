package justfatlard.fire_arrows.gametest;

import justfatlard.fire_arrows.Charge;
import justfatlard.fire_arrows.Main;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Each arrow, fired at what it is for, does it. */
public final class ArrowsLand implements FabricClientGameTest {

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			TestServerConnection connection = world.getConnection();
			connection.waitForChunksRender();
			server.runCommand("gamemode survival @a");
			BlockPos base = server.computeOnServer(s -> connection.getServerPlayer().blockPosition().above(30));

			// Every one of them goes in a bow and a crossbow, and has its recipe.
			server.runOnServer(s -> {
				for (Charge charge : Charge.values()) {
					ItemStack stack = new ItemStack(Main.ITEMS.get(charge));
					check(stack.is(ItemTags.ARROWS), charge + " is not in #arrows, so no bow will fire it");
					check(s.getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(
						net.minecraft.core.registries.Registries.RECIPE,
						net.minecraft.resources.Identifier.fromNamespaceAndPath(Main.MOD_ID, charge.itemName()))).isPresent(),
						charge + " has no recipe");
				}
			});

			// A torch arrow into a wall: a torch on the wall, a plain arrow to pick up.
			BlockPos wall = base.east(6);
			server.runOnServer(s -> {
				ServerLevel level = s.overworld();
				level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
				fire(level, Charge.TORCH, Vec3.atCenterOf(base), Vec3.atCenterOf(wall));
			});
			context.waitTicks(30);
			server.runOnServer(s -> {
				ServerLevel level = s.overworld();
				check(level.getBlockState(wall.west()).is(Blocks.WALL_TORCH),
					"no torch where the torch arrow hit: " + level.getBlockState(wall.west()));
				var stuck = level.getEntitiesOfClass(AbstractArrow.class, new AABB(wall).inflate(2));
				check(!stuck.isEmpty() && stuck.get(0).getPickupItemStackOrigin().is(Items.ARROW),
					"the torch arrow left is not a plain arrow: " + stuck);
			});

			// A fire arrow into TNT: it is lit.
			BlockPos tnt = base.south(8);
			server.runOnServer(s -> {
				ServerLevel level = s.overworld();
				level.setBlockAndUpdate(tnt.below(), Blocks.STONE.defaultBlockState());
				level.setBlockAndUpdate(tnt, Blocks.TNT.defaultBlockState());
				fire(level, Charge.FIRE, Vec3.atCenterOf(tnt.north(6)), Vec3.atCenterOf(tnt));
			});
			context.waitTicks(20);
			server.runOnServer(s -> check(!s.overworld().getEntitiesOfClass(PrimedTnt.class, new AABB(tnt).inflate(3)).isEmpty(),
				"the fire arrow did not light the TNT: " + s.overworld().getBlockState(tnt)));
			context.waitTicks(90);

			// A fire arrow into a cow: it is alight.
			BlockPos pen = base.west(10);
			int cow = server.computeOnServer(s -> {
				ServerLevel level = s.overworld();
				for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
					level.setBlockAndUpdate(pen.offset(x, -1, z), Blocks.STONE.defaultBlockState());
				}
				Mob mob = EntityTypes.COW.create(level, EntitySpawnReason.COMMAND);
				mob.snapTo(pen.getX() + 0.5, pen.getY(), pen.getZ() + 0.5);
				mob.setNoAi(true);
				level.addFreshEntity(mob);
				fire(level, Charge.FIRE, Vec3.atCenterOf(pen.east(6)), mob.position().add(0, 0.7, 0));
				return mob.getId();
			});
			context.waitTicks(20);
			server.runOnServer(s -> check(s.overworld().getEntity(cow).isOnFire(), "the fire arrow did not set the cow alight"));

			// An explosive arrow into a wall: a hole in it.
			BlockPos target = base.north(14);
			server.runOnServer(s -> {
				ServerLevel level = s.overworld();
				for (int y = -2; y <= 2; y++) for (int x = -2; x <= 2; x++) {
					level.setBlockAndUpdate(target.offset(x, y, 0), Blocks.STONE.defaultBlockState());
				}
				fire(level, Charge.EXPLOSIVE, Vec3.atCenterOf(target.south(8)), Vec3.atCenterOf(target));
			});
			context.waitTicks(30);
			server.runOnServer(s -> {
				ServerLevel level = s.overworld();
				int gone = 0;
				for (int y = -2; y <= 2; y++) for (int x = -2; x <= 2; x++) {
					if (level.getBlockState(target.offset(x, y, 0)).isAir()) gone++;
				}
				// A bite out of the wall, not the whole of it: the charge is under TNT's.
				check(gone >= 3, "the explosive arrow blew only " + gone + " blocks out of the wall");
				check(gone <= 16, "the explosive arrow blew " + gone + " blocks out of a 25 block wall");
				check(level.getEntitiesOfClass(AbstractArrow.class, new AABB(target).inflate(4)).isEmpty(),
					"an explosive arrow survived its own explosion");
			});

			check(context.computeOnClient(client -> BuiltInRegistries.ITEM.containsKey(
				net.minecraft.resources.Identifier.fromNamespaceAndPath(Main.MOD_ID, "explosive_arrow"))),
				"the client does not know the arrows");
		}
	}

	/** An arrow from {@code from} at {@code at}, as a dispenser would fire one: quick and straight. */
	private static void fire(ServerLevel level, Charge charge, Vec3 from, Vec3 at) {
		ItemStack stack = new ItemStack(Main.ITEMS.get(charge));
		var arrow = (AbstractArrow) Main.ITEMS.get(charge).asProjectile(level, from, stack,
			net.minecraft.core.Direction.NORTH);
		Vec3 path = at.subtract(from);
		arrow.shoot(path.x, path.y, path.z, 3.0F, 0.0F);
		arrow.setNoGravity(true);
		level.addFreshEntity(arrow);
	}

	private static void check(boolean holds, String otherwise) {
		if (!holds) throw new AssertionError(otherwise);
	}
}
