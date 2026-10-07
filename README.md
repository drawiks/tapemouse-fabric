# TapeMouse Fabric

Client-side keybinding macro for Fabric on Minecraft 26.3.

Repeatedly presses any vanilla keybinding on a fixed tick delay, so you can keep farming, boating
or walking while the window is unfocused or you are doing something else in it.

Commands only. No GUI, no config screen.

## Requirements

- Minecraft 26.3
- Fabric Loader 0.19.5+
- Fabric API 0.162.0+26.3

## Usage

```
/tapemouse off                     stop
/tapemouse list                    list keybinding names
/tapemouse set <key> <delay>       press <key> every <delay> ticks, 0 holds it down
```

The key name is the bare name, e.g. `attack`, `use`, `forward`, `jump` — not `key.attack`.
Tab completion offers every valid name.

### Delay

`delay` is in ticks, 20 ticks = 1 second.

- `0` — **hold**: the key stays down continuously. This is what you want for
  `attack`, `use` and movement keys.
- `20` — **click**: the key taps every second.

While any screen is open (main menu, inventory, pause, chat) the macro is paused and the key is
released, so nothing gets stuck down when you alt-tab back in.

### Status

While the F3 debug screen is open, the mod prints its state in the bottom-left corner:

```
TapeMouse: attack [HOLD]
Delay: hold
```

## Notes

This is an automation aid for singleplayer and for servers that allow it. Server-side anti-AFK
detection is unaffected by anything a client mod does — this is detectable by design. Do not use
it where it is not allowed.

## Credits

The idea of emulating a keybinding on a delay comes from **TapeMouse** by
[Dries007](https://github.com/dries007/TapeMouse) (original, BSD-3-Clause) and the
[TapeMouse](https://modrinth.com/mod/tapemouse) Fabric port (MIT). This is an independent
implementation for Minecraft 26.3; no code was copied from either.

## License

MIT. See [LICENSE](LICENSE).