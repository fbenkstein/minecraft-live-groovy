# LiveGroovy

Hot-reloadable Groovy scripts for the local Paper server.

Build:

```bash
mvn package
```

Install after building:

```bash
cp target/LiveGroovy.jar ../data/plugins/LiveGroovy.jar
mkdir -p ../data/plugins/LiveGroovy/scripts
cp scripts/*.groovy ../data/plugins/LiveGroovy/scripts/
```

Server command:

```text
/groovy reload
/groovy list
/groovy run welcome.groovy
```

Scripts get these bindings:

```groovy
server
plugin
api
events
commands
scheduler
items
```

Example event:

```groovy
import org.bukkit.event.player.PlayerJoinEvent

events.on(PlayerJoinEvent) { event ->
  event.player.sendMessage("Welcome from Groovy")
}
```

Example command with a pre-created palette texture:

```groovy
commands.register("redsword") { sender, args ->
  sender.inventory.addItem(items.create("iron_sword_red"))
  true
}
```

## Import and typing constraints

Paper embeds Adventure (`net.kyori.adventure`) in its jar and loads it via a custom classloader at server startup. This classloader is **not** reachable by the plugin during Groovy's compile-time type resolution, only at runtime.

Two patterns trigger compile-time Adventure resolution and will fail:

**1. Explicit imports of Adventure classes**

```groovy
// BAD — "unable to resolve class" at compile time
import net.kyori.adventure.text.Component
meta.displayName(Component.text("Name"))

// GOOD — resolved at runtime through Paper's classloader
meta.displayName(net.kyori.adventure.text.Component.text("Name"))
```

**2. Explicitly-typed variables whose Bukkit type has Adventure in its method signatures**

Groovy statically resolves method signatures on typed variables. Many Bukkit entity and item types expose Adventure methods (e.g. `Entity.customName(Component)`, `ItemMeta.displayName(Component)`), which forces Adventure to be loaded at compile time — and fails.

```groovy
// BAD — Groovy resolves Pig.customName(Component) at compile time
Pig pig = player.world.spawnEntity(loc, EntityType.PIG) as Pig
pig.customName(net.kyori.adventure.text.Component.text("..."))

// GOOD — dynamic dispatch; method resolved at runtime
def pig = player.world.spawnEntity(loc, EntityType.PIG)
pig.customName(net.kyori.adventure.text.Component.text("..."))
```

**Rule of thumb:** use `def` for all entity and item variables. Only use explicit types for simple value types (e.g. `String`, `int`, `boolean`, `NamespacedKey`) that have no Adventure in their API surface.

You can still use `instanceof Pig` and similar checks — those only test type identity and do not trigger method-signature resolution.
