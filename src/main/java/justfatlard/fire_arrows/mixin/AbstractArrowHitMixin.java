package justfatlard.fire_arrows.mixin;

import justfatlard.fire_arrows.ArrowHits;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Where a charged arrow lands; see {@link ArrowHits}. */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowHitMixin {

	@Inject(method = "onHitBlock", at = @At("HEAD"), cancellable = true)
	private void fireArrows$hitBlock(BlockHitResult hit, CallbackInfo ci) {
		if (ArrowHits.hitBlock((AbstractArrow) (Object) this, hit)) ci.cancel();
	}

	@Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
	private void fireArrows$hitEntity(EntityHitResult hit, CallbackInfo ci) {
		if (ArrowHits.hitEntity((AbstractArrow) (Object) this)) ci.cancel();
	}
}
