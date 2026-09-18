import org.bukkit.Bukkit
import org.bukkit.FluidCollisionMode
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.persistence.PersistentDataType

final String TELEPORT_STICK_MARKER = "teleport_stick"
final double MAX_TELEPORT_DISTANCE = 192.0D
final NamespacedKey itemKey = new NamespacedKey(plugin, "teleport_stick")
final NamespacedKey recipeKey = new NamespacedKey(plugin, "teleport_stick")
final Map returnLocations = Collections.synchronizedMap(new HashMap())
final Map lastUseAt = Collections.synchronizedMap(new HashMap())

def makeTeleportStick = {
  def stack = items.create("stick_blue")
  def meta = stack.itemMeta
  meta.displayName(net.kyori.adventure.text.Component.text("Teleport Stick"))
  meta.lore([
    net.kyori.adventure.text.Component.text("Right-click to blink to your crosshair"),
    net.kyori.adventure.text.Component.text("Sneak-right-click to blink back")
  ])
  meta.persistentDataContainer.set(itemKey, PersistentDataType.STRING, TELEPORT_STICK_MARKER)
  stack.itemMeta = meta
  stack
}

def isTeleportStick = { stack ->
  if (stack == null || !stack.hasItemMeta()) {
    return false
  }
  TELEPORT_STICK_MARKER == stack.itemMeta.persistentDataContainer.get(itemKey, PersistentDataType.STRING)
}

Bukkit.removeRecipe(recipeKey)
def recipe = new ShapedRecipe(recipeKey, makeTeleportStick())
recipe.shape(" E ", " S ", " A ")
recipe.setIngredient('E' as char, Material.ENDER_PEARL)
recipe.setIngredient('S' as char, Material.STICK)
recipe.setIngredient('A' as char, Material.AMETHYST_SHARD)
Bukkit.addRecipe(recipe)

commands.register("teleportstick", "Give yourself a Teleport Stick") { sender, args ->
  if (!sender.respondsTo("getInventory")) {
    sender.sendMessage("Players only")
    return true
  }
  sender.inventory.addItem(makeTeleportStick())
  sender.sendMessage("Added Teleport Stick")
  true
}

events.on(PlayerInteractEvent, EventPriority.NORMAL, false) { event ->
  if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) {
    return
  }
  def player = event.player
  if (!isTeleportStick(event.item) &&
      !isTeleportStick(player.inventory.itemInMainHand) &&
      !isTeleportStick(player.inventory.itemInOffHand)) {
    return
  }

  long now = System.currentTimeMillis()
  def lastUse = lastUseAt.get(player.uniqueId)
  if (lastUse != null && now - (lastUse as long) < 100L) {
    return
  }
  lastUseAt.put(player.uniqueId, now)

  event.cancelled = true
  event.setUseInteractedBlock(Event.Result.DENY)
  event.setUseItemInHand(Event.Result.DENY)
  def from = player.location.clone()
  def destination = null

  if (player.sneaking) {
    def remembered = returnLocations.get(player.uniqueId)
    if (remembered == null) {
      player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5F, 0.8F)
      return
    }
    destination = remembered.clone()
    destination.yaw = player.location.yaw
    destination.pitch = player.location.pitch
  }

  def eye = player.eyeLocation
  def direction = eye.direction.normalize()

  if (destination == null) {
    def result = player.world.rayTrace(
      eye,
      direction,
      MAX_TELEPORT_DISTANCE,
      FluidCollisionMode.NEVER,
      true,
      0.35D,
      { entity -> entity.uniqueId != player.uniqueId } as java.util.function.Predicate
    )

    def hit = null
    if (result?.hitEntity != null) {
      hit = result.hitEntity.location.toVector()
    } else if (result?.hitPosition != null) {
      hit = result.hitPosition
      def face = result.hitBlockFace
      if (face != null) {
        hit = hit.clone().add(face.direction.clone().multiply(0.6D))
      }
    } else if (direction.y > 0.05D) {
      double ceilingY = player.world.maxHeight - 1.0D
      double distanceToCeiling = (ceilingY - eye.y) / direction.y
      if (distanceToCeiling >= 0.0D) {
        hit = eye.toVector().add(direction.clone().multiply(distanceToCeiling))
      }
    }

    if (hit == null) {
      player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5F, 0.8F)
      return
    }

    destination = hit.toLocation(player.world, player.location.yaw, player.location.pitch)
  }

  player.world.spawnParticle(Particle.PORTAL, from.clone().add(0, 1.0D, 0), 36, 0.35D, 0.8D, 0.35D, 0.08D)
  returnLocations.put(player.uniqueId, from)
  player.teleport(destination)
  player.fallDistance = 0
  player.world.spawnParticle(Particle.PORTAL, destination.clone().add(0, 1.0D, 0), 36, 0.35D, 0.8D, 0.35D, 0.08D)
  player.world.playSound(from, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8F, 1.4F)
  player.world.playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8F, 1.7F)
  player.swingMainHand()
}
