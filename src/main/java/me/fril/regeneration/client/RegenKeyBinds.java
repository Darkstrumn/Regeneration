package me.fril.regeneration.client;

import me.fril.regeneration.RegenerationMod;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

/**
 * Created by Sub
 * on 17/09/2018.
 */
public class RegenKeyBinds {
	
	public static KeyBinding REGEN_NOW;
	
	public static void init() {
		REGEN_NOW = new KeyBinding("regeneration.keybinds.regenerate", Keyboard.KEY_I, RegenerationMod.NAME);
		ClientRegistry.registerKeyBinding(REGEN_NOW);
	}
	
}
