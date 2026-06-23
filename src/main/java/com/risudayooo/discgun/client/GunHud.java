package com.risudayooo.discgun.client;

import com.risudayooo.discgun.disc.DiscType;
import com.risudayooo.discgun.item.GunCDPlayerItem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Bottom-right HUD shown while holding a gun: loaded disc + ammo (or RELOADING).
 */
public final class GunHud {
	private GunHud() {
	}

	public static void register() {
		HudRenderCallback.EVENT.register((context, tickCounter) -> render(context));
	}

	private static void render(DrawContext context) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player == null || mc.options.hudHidden) {
			return;
		}
		ItemStack gun = mc.player.getMainHandStack();
		if (!(gun.getItem() instanceof GunCDPlayerItem)) {
			return;
		}

		DiscType disc = GunCDPlayerItem.getDisc(gun);
		int mag = disc.ability().magazineSize();
		int ammo = GunCDPlayerItem.getAmmo(gun);
		boolean reloading = DiscGunClient.isReloading(mc.player.getWorld().getTime());

		TextRenderer tr = mc.textRenderer;
		Text discName = Text.translatable(disc.translationKey()).formatted(disc.color());
		Text ammoText = reloading
				? Text.translatable("hud.discgun.reloading").formatted(Formatting.YELLOW)
				: Text.literal(ammo + " / " + mag).formatted(ammo == 0 ? Formatting.RED : Formatting.WHITE);

		int right = context.getScaledWindowWidth() - 12;
		int y = context.getScaledWindowHeight() - 46;
		context.drawTextWithShadow(tr, discName, right - tr.getWidth(discName), y, 0xFFFFFF);
		context.drawTextWithShadow(tr, ammoText, right - tr.getWidth(ammoText), y + 12, 0xFFFFFF);
	}
}
