# ModHost Java Bridge (LeviLauncher Android)

Runs Java mods inside Minecraft Bedrock on Android through a native LeviLauncher mod.
This is **not** Fabric/NeoForge: mods target the small API in `java/api` (`ModHostMod`, `Events`, `ModLogger`).
Java Edition mods must be ported to it.

    C++ mod (template) -> JavaBridge -> DexClassLoader -> com.modhost.api.Bootstrap -> your ModHostMod

## How the build works
CI checks out the LeviLauncher template next to this repo, runs `scripts/overlay.py` (adds `src/bridge`,
hooks it into `MyMod.cpp` and `CMakeLists.txt`, ships `modhost-api.dex.jar` inside the `.levipack`),
then runs the template's `scripts/package.ps1 -Abi arm64-v8a`. Nothing to merge by hand.
Minecraft version filter is the template's default `1.26.*`.

## Writing a mod
Implement `ModHostMod`, build with javac + d8 (see `scripts/build-java.sh`), and put
`yourmod.jar` + `yourmod.json` (`{"id": "...", "main": "pkg.Class"}`) into `<dataDir>/mods`.
Jars must be dexed with d8; Android can't load plain class-file jars.

## Features
- Events: tick (20/s), app.resumed/app.paused, custom events, native `fire()` (docs/EVENTS.md)
- In-game "Mods" button: switch mods on/off, see why a mod can't run, and open a gear editor per mod:
  status, settings, and a text editor for its config files (docs/CONFIG.md)
- Fabric-style and NeoForge-style APIs: `ModInitializer`, `FabricLoader`, `Event`, `ClientTickEvents`,
  `@Mod` + `IEventBus`, slf4j (docs/API_COMPAT.md)
- Bundled libraries so common mods load: slf4j, log4j2 (shims), Gson (downloaded at build)
- `Copy report` button for sharing status and log without screenshots
- `convert-mods` workflow prepares ordinary jars (docs/COMPAT.md)

## Status
- Done: JVM bridge, API, events, menu + gear editor, Fabric/NeoForge-style layer, converter, CI.
- Not done: native game-event hooks (need Bedrock signatures per version), Lua loader.
- Untested on a device or inside the real template; expect small fixes.
