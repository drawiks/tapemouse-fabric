package io.github.drawiks.tapemouse;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.util.List;

/** Delay counter and command tree checks. Run with {@code ./gradlew selfCheck}. */
public final class TapeMouseSelfCheck {
	public static void main(String[] args) {
		holdFiresEveryTick();
		clickFiresExactlyOncePerDelay();
		counterCyclesThroughEveryValue();
		commandTreeIsShapedCorrectly();
		System.out.println("TapeMouse self-check OK");
	}

	private static void holdFiresEveryTick() {
		for (int i = 0; i < 100; i++) {
			assert TapeMouseClient.due(i, 0) : "delay 0 must fire on every tick, not " + i;
		}
	}

	private static void clickFiresExactlyOncePerDelay() {
		for (int delay = 1; delay <= 40; delay++) {
			int fired = countFires(20 * delay + delay, delay);
			assert fired == 21 : "delay " + delay + " fired " + fired + " times, expected 21";
		}
	}

	private static void counterCyclesThroughEveryValue() {
		for (int delay = 1; delay <= 20; delay++) {
			List<Integer> firedAt = new java.util.ArrayList<>();
			int counter = 0;
			for (int tick = 0; tick < delay * 3; tick++) {
				if (TapeMouseClient.due(counter, delay)) {
					firedAt.add(tick);
					counter = 0;
				} else {
					counter++;
				}
				assert counter < delay : "counter escaped its range for delay " + delay;
			}
			for (int i = 1; i < firedAt.size(); i++) {
				assert firedAt.get(i) - firedAt.get(i - 1) == delay
						: "delay " + delay + " fired at irregular intervals: " + firedAt;
			}
		}
	}

	private static void commandTreeIsShapedCorrectly() {
		CommandDispatcher<FabricClientCommandSource> dispatcher = new CommandDispatcher<>();
		TapeMouseClient.registerCommand(dispatcher);

		CommandNode<FabricClientCommandSource> root = dispatcher.getRoot().getChild("tapemouse");
		assert root != null : "/tapemouse was not registered";
		assert root.getCommand() != null : "/tapemouse must print help when run bare";

		assert child(root, "off") != null : "/tapemouse off missing";
		assert child(root, "list") != null : "/tapemouse list missing";

		CommandNode<FabricClientCommandSource> set = child(root, "set");
		assert set != null : "/tapemouse set missing";
		assert set.getCommand() == null : "/tapemouse set requires <key> <delay>, not a bare command";

		CommandNode<FabricClientCommandSource> key = child(set, "key");
		assert key != null : "<key> argument missing";
		assert key.getCommand() == null : "<key> must require <delay>";

		CommandNode<FabricClientCommandSource> delay = child(key, "delay");
		assert delay != null : "<delay> argument missing";
		assert delay.getCommand() != null : "/tapemouse set <key> <delay> must be runnable";
	}

	private static CommandNode<FabricClientCommandSource> child(CommandNode<FabricClientCommandSource> parent, String name) {
		return parent.getChildren().stream()
				.filter(n -> n.getName().equals(name))
				.findFirst()
				.orElse(null);
	}

	private static int countFires(int ticks, int delay) {
		int counter = 0;
		int fired = 0;
		for (int tick = 0; tick < ticks; tick++) {
			if (TapeMouseClient.due(counter, delay)) {
				counter = 0;
				fired++;
			} else {
				counter++;
			}
		}
		return fired;
	}
}