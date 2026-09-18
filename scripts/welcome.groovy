import org.bukkit.event.player.PlayerJoinEvent

// Loaded with /groovy reload. Edit this file and reload without restarting Paper.
events.on(PlayerJoinEvent) { event ->
  event.player.sendMessage("Welcome from LiveGroovy")
}
