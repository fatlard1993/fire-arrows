package justfatlard.fire_arrows;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * An arrow that carries something, fired as the game's own arrow with its charge marked on it.
 *
 * <p>The game's arrow, so every client draws it with nothing new to know and every bow, crossbow and
 * dispenser fires it; the charge is a tag, read where it lands (see {@link ArrowHits}). A fire arrow
 * flies burning, which is all the game needs to set what it hits alight and to light the TNT it
 * strikes: the Flame enchantment's arrows already do both.
 */
public class CarryingArrowItem extends ArrowItem {
	/** As long as a Flame arrow burns. */
	private static final float BURNING_SECONDS = 100;

	private final Charge charge;

	public CarryingArrowItem(Charge charge, Properties properties) {
		super(properties);
		this.charge = charge;
	}

	public Charge charge() {
		return charge;
	}

	@Override
	public AbstractArrow createArrow(Level level, ItemStack itemStack, LivingEntity owner, @Nullable ItemStack firedFromWeapon) {
		return charged(new Arrow(level, owner, itemStack.copyWithCount(1), firedFromWeapon));
	}

	@Override
	public Projectile asProjectile(Level level, Position position, ItemStack itemStack, Direction direction) {
		Arrow arrow = new Arrow(level, position.x(), position.y(), position.z(), itemStack.copyWithCount(1), null);
		arrow.pickup = AbstractArrow.Pickup.ALLOWED;
		return charged(arrow);
	}

	private Arrow charged(Arrow arrow) {
		arrow.addTag(charge.tag());
		if (charge == Charge.FIRE) arrow.igniteForSeconds(BURNING_SECONDS);
		return arrow;
	}
}
