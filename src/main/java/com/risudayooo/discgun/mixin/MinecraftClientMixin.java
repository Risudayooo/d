package com.risudayooo.discgun.mixin;

import com.risudayooo.discgun.item.GunCDPlayerItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Suppresses vanilla attack/use while a gun is held (5章: "Attack/Use を
 * オーバーライド"). The actual fire/action is driven from {@link
 * com.risudayooo.discgun.client.DiscGunClient}, so here we just cancel vanilla so
 * the player doesn't break blocks / use items while shooting.
 */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
	@Shadow
	public ClientPlayerEntity player;

	private boolean holdingGun() {
		return player != null && player.getMainHandStack().getItem() instanceof GunCDPlayerItem;
	}

	@Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
	private void discgun$cancelAttack(CallbackInfoReturnable<Boolean> cir) {
		if (holdingGun()) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
	private void discgun$cancelUse(CallbackInfo ci) {
		if (holdingGun()) {
			ci.cancel();
		}
	}
}
