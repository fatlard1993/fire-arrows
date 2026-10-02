package justfatlard.fire_arrows;

import justfatlard.fire_arrows.mixin.AbstractArrowAccessor;
import justfatlard.pandorical.api.PandoricalApi;
import justfatlard.pandorical.api.Trust;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * What a charged arrow does where it lands. Called from {@code AbstractArrowHitMixin}, before the
 * game handles the hit; true when the hit is spent and the game should not go on with it.
 */
public final class ArrowHits {
	private ArrowHits() {}

	/**
	 * Under TNT's four, over a creeper's three: an arrow's worth of the block it carries. Stone
	 * gives way somewhere between the two - at three the hit block alone goes - so this is the
	 * smallest charge that still takes a bite out of a wall.
	 */
	private static final float EXPLOSION_POWER = 3.5F;

	private static FireArrowsConfig config;

	static void configure(FireArrowsConfig loaded) {
		config = loaded;
	}

	public static Charge chargeOf(AbstractArrow arrow) {
		for (Charge charge : Charge.values()) {
			if (arrow.entityTags().contains(charge.tag())) return charge;
		}
		return null;
	}

	/** An arrow striking a block. */
	public static boolean hitBlock(AbstractArrow arrow, BlockHitResult hit) {
		if (!(arrow.level() instanceof ServerLevel level)) return false;
		Charge charge = chargeOf(arrow);
		if (charge == null) return false;

		switch (charge) {
			case TORCH -> {
				placeTorch(level, arrow, hit);
				spend(arrow, charge);
				return false;
			}
			case FIRE -> {
				if (arrow.isOnFire()) kindle(level, arrow, hit);
				spend(arrow, charge);
				return false;
			}
			case EXPLOSIVE -> {
				// Not trusted with explosives: it lands as the plain arrow it is underneath.
				if (!trusted(arrow, Trust.EXPLOSIVES)) {
					spend(arrow, charge);
					return false;
				}
				explode(level, arrow, hit.getLocation());
				return true;
			}
		}
		return false;
	}

	/** An arrow striking an entity. */
	public static boolean hitEntity(AbstractArrow arrow) {
		if (!(arrow.level() instanceof ServerLevel level)) return false;
		if (chargeOf(arrow) != Charge.EXPLOSIVE || !trusted(arrow, Trust.EXPLOSIVES)) return false;
		explode(level, arrow, arrow.position());
		return true;
	}

	private static void explode(ServerLevel level, AbstractArrow arrow, Vec3 at) {
		arrow.discard();
		level.explode(arrow, at.x, at.y, at.z, EXPLOSION_POWER,
			config != null && !config.explosiveBreaksBlocks ? Level.ExplosionInteraction.NONE : Level.ExplosionInteraction.TNT);
	}

	/** The charge is used: what is left to pick up is a plain arrow. */
	private static void spend(AbstractArrow arrow, Charge charge) {
		arrow.removeTag(charge.tag());
		((AbstractArrowAccessor) arrow).fireArrows$setPickupItemStack(new ItemStack(Items.ARROW));
	}

	/**
	 * Fire on the face it struck, as flint and steel would set it. A burning arrow lights TNT,
	 * candles and campfires by itself, so those are left to the game.
	 */
	private static void kindle(ServerLevel level, AbstractArrow arrow, BlockHitResult hit) {
		BlockState struck = level.getBlockState(hit.getBlockPos());
		if (CampfireBlock.canLight(struck) || CandleBlock.canLight(struck) || CandleCakeBlock.canLight(struck)) return;

		BlockPos at = hit.getBlockPos().relative(hit.getDirection());
		if (!trusted(arrow, Trust.FIRE) || !mayPlace(level, arrow, at) || !BaseFireBlock.canBePlacedAt(level, at, arrow.getDirection())) return;
		level.setBlockAndUpdate(at, BaseFireBlock.getState(level, at));
		level.playSound(null, at, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
	}

	/** Whether an op has trusted the one who fired it with this; a dispenser answers to nobody. */
	private static boolean trusted(AbstractArrow arrow, Trust what) {
		return !(arrow.getOwner() instanceof ServerPlayer player) || PandoricalApi.trust().may(player, what);
	}

	/** Where the one who fired it could have set a block by hand; a dispenser may set one anywhere. */
	private static boolean mayPlace(ServerLevel level, AbstractArrow arrow, BlockPos at) {
		return !(arrow.getOwner() instanceof ServerPlayer player)
			|| (player.mayBuild() && level.mayInteract(player, at));
	}

	/**
	 * On the face it struck: standing on a top, fixed to a side. Where a torch will not go - a
	 * ceiling, water, somebody's protected ground - the torch drops where it would have stood.
	 */
	private static void placeTorch(ServerLevel level, AbstractArrow arrow, BlockHitResult hit) {
		Direction face = hit.getDirection();
		BlockPos at = hit.getBlockPos().relative(face);
		BlockState torch = face == Direction.UP ? Blocks.TORCH.defaultBlockState()
			: face == Direction.DOWN ? null
			: Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, face);

		boolean allowed = mayPlace(level, arrow, at);
		BlockState there = level.getBlockState(at);
		if (torch != null && allowed && there.canBeReplaced() && there.getFluidState().isEmpty()
				&& torch.canSurvive(level, at)) {
			level.setBlockAndUpdate(at, torch);
			level.playSound(null, at, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
			return;
		}

		ItemEntity dropped = new ItemEntity(level, at.getX() + 0.5, at.getY() + 0.25, at.getZ() + 0.5,
			new ItemStack(Items.TORCH));
		dropped.setDefaultPickUpDelay();
		level.addFreshEntity(dropped);
	}
}
