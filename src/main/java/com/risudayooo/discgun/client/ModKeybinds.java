package com.risudayooo.discgun.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Custom key bindings (5章). Blink defaults to a free key; the design intends it
 * to be rebound onto the player's dash/sprint key. Parry sits on F (note: this
 * overlaps vanilla "Swap Item With Offhand" — unbind that, or it will also fire).
 */
public final class ModKeybinds {
	public static final String CATEGORY = "key.categories.discgun";

	public static KeyBinding blink;
	public static KeyBinding parry;
	public static KeyBinding reload;

	private ModKeybinds() {
	}

	public static void register() {
		blink = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.discgun.blink", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, CATEGORY));
		parry = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.discgun.parry", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F, CATEGORY));
		reload = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.discgun.reload", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY));
	}
}
