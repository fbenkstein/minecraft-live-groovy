# LiveGroovy

Hot-reloadable Groovy scripts for the local Paper server.

> Most of this project, plugin and scripts alike, was written with AI assistance (Claude Code) during ad-hoc live sessions on the server. Review before reuse.

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

See [AGENTS.md](AGENTS.md) for Adventure/classloader import and typing constraints to know about when writing scripts.
