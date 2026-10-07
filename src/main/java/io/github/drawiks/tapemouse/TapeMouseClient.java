package io.github.drawiks.tapemouse;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.TreeMap;

/**
 * Presses a keybinding on a tick delay. {@code delay 0} holds it down, anything higher taps it.
 */
public class TapeMouseClient implements ClientModInitializer {
	static final String MOD_ID = "tapemouse-fabric";

	private static KeyMapping active;
	private static int delay;
	private static int counter;

	@Override
	public void onInitializeClient() {
		ClientTickEvents.START_CLIENT_TICK.register(TapeMouseClient::tick);
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, ctx) -> registerCommand(dispatcher));
		HudElementRegistry.addLast(
				Identifier.fromNamespaceAndPath(MOD_ID, "status"),
				TapeMouseClient::renderStatus);
	}

	// Ticks since the last press; reset on every press.
	static boolean due(int counter, int delay) {
		return delay == 0 || counter + 1 >= delay;
	}

	private static void tick(Minecraft mc) {
		if (active == null) return;
		if (mc.gui.screen() != null || active.isUnbound()) {
			active.setDown(false);
			return;
		}

		if (delay == 0) {
			press();
			return;
		}

		active.setDown(false);
		if (due(counter, delay)) {
			counter = 0;
			press();
		} else {
			counter++;
		}
	}

	// setDown holds the key; click() is what discrete actions actually read, via consumeClick.
	private static void press() {
		active.setDown(true);
		InputConstants.Key key = KeyMappingHelper.getBoundKeyOf(active);
		KeyMapping.click(key);
	}

	private static void setActive(KeyMapping keyBinding, int newDelay) {
		if (active != null) active.setDown(false);
		active = keyBinding;
		delay = newDelay;
		counter = 0;
	}

	// attack, not key.attack
	private static String name(KeyMapping km) {
		return km.getName().replaceFirst("^key\\.", "");
	}

	private static Map<String, KeyMapping> keybinds() {
		Map<String, KeyMapping> map = new TreeMap<>();
		for (KeyMapping km : Minecraft.getInstance().options.keyMappings) {
			map.put(name(km), km);
		}
		return map;
	}

	static void registerCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(ClientCommands.literal("tapemouse")
				.executes(c -> {
					help(c.getSource());
					return 1;
				})
				.then(ClientCommands.literal("off").executes(c -> {
					setActive(null, 0);
					c.getSource().sendFeedback(Component.translatable("tapemouse.command.off")
							.withStyle(ChatFormatting.YELLOW));
					return 1;
				}))
				.then(ClientCommands.literal("list").executes(c -> {
					c.getSource().sendFeedback(Component.translatable("tapemouse.command.keybindlist")
							.withStyle(ChatFormatting.AQUA));
					keybinds().forEach((key, km) -> c.getSource().sendFeedback(Component.literal(key)
							.withStyle(ChatFormatting.GRAY)
							.append(" ")
							.append(Component.translatable("key." + key).withStyle(ChatFormatting.WHITE))
							.append(" [").append(km.getTranslatedKeyMessage()).append("]")
							.append(" (").append(km.getCategory().label()).append(")")
							.withStyle(ChatFormatting.DARK_GRAY)));
					return 1;
				}))
				.then(ClientCommands.literal("set")
						.then(ClientCommands.argument("key", StringArgumentType.word())
								.suggests((c, builder) -> {
									keybinds().keySet().forEach(builder::suggest);
									return builder.buildFuture();
								})
								.then(ClientCommands.argument("delay", IntegerArgumentType.integer(0))
										.executes(c -> {
											KeyMapping km = keybinds().get(StringArgumentType.getString(c, "key"));
											if (km == null) {
												c.getSource().sendError(Component.translatable("tapemouse.command.unknownkey",
														StringArgumentType.getString(c, "key")));
												return 0;
											}
											int ticks = IntegerArgumentType.getInteger(c, "delay");
											setActive(km, ticks);
											c.getSource().sendFeedback(Component.translatable("tapemouse.command.set",
													name(km), km.getTranslatedKeyMessage(), ticks));
											return 1;
										})))));
	}

	private static void help(FabricClientCommandSource source) {
		source.sendFeedback(Component.literal("TapeMouse Fabric").withStyle(ChatFormatting.AQUA));
		source.sendFeedback(Component.literal("/tapemouse off").withStyle(ChatFormatting.WHITE)
				.append(" - stop pressing").withStyle(ChatFormatting.GRAY));
		source.sendFeedback(Component.literal("/tapemouse list").withStyle(ChatFormatting.WHITE)
				.append(" - list keybinding names").withStyle(ChatFormatting.GRAY));
		source.sendFeedback(Component.literal("/tapemouse set <key> <delay>").withStyle(ChatFormatting.WHITE)
				.append(" - press every N ticks, 0 holds it down").withStyle(ChatFormatting.GRAY));
		source.sendFeedback(Component.literal("Currently: ")
				.append(active == null
						? Component.literal("off").withStyle(ChatFormatting.RED)
						: Component.literal(name(active) + " " + modeLabel())
								.withStyle(ChatFormatting.GREEN)));
	}

	private static String modeLabel() {
		return delay == 0 ? "[HOLD]" : "[CLICK every " + delay + " ticks]";
	}

	private static void renderStatus(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (active == null || !mc.debugEntries.isOverlayVisible()) return;

		int y = graphics.guiHeight() - 22;
		graphics.text(mc.font, Component.literal("TapeMouse: ").append(name(active)).append(" ").append(modeLabel()),
				2, y, 0xFFFF55);
		graphics.text(mc.font, delay == 0
				? Component.literal("Delay: hold")
				: Component.literal("Delay: " + counter + " / " + delay),
				2, y + 10, 0xAAAAAA);
	}
}