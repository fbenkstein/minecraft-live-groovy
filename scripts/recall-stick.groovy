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

final String RECALL_STICK_MARKER = "recall_stick"
final double MAX_RECALL_DISTANCE = 192.0D
final NamespacedKey itemKey = new NamespacedKey(plugin, "recall_stick")
final NamespacedKey recipeKey = new NamespacedKey(plugin, "recall_stick")
final Map lastUseAt = Collections.synchronizedMap(new HashMap())

def makeRecallStick = {
  def stack = items.create("stick_green")
  def meta = stack.itemMeta
  meta.displayName(net.kyori.adventure.text.Component.text("Recall Stick"))
  meta.lore([
    net.kyori.adventure.text.Component.text("Right-click an entity to pull it to you")
  ])
  meta.persistentDataContainer.set(itemKey, PersistentDataType.STRING, RECALL_STICK_MARKER)
  stack.itemMeta = meta
  stack
}

def isRecallStick = { stack ->
  if (stack == null || !stack.hasItemMeta()) {
    return false
  }
  RECALL_STICK_MARKER == stack.itemMeta.persistentDataContainer.get(itemKey, PersistentDataType.STRING)
}

Bukkit.removeRecipe(recipeKey)
def recipe = new ShapedRecipe(recipeKey, makeRecallStick())
recipe.shape(" L ", " S ", " E ")
recipe.setIngredient('L' as char, Material.LEAD)
recipe.setIngredient('S' as char, Material.STICK)
recipe.setIngredient('E' as char, Material.ENDER_PEARL)
Bukkit.addRecipe(recipe)

commands.register("recallstick", "Give yourself a Recall Stick") { sender, args ->
  if (!sender.respondsTo("getInventory")) {
    sender.sendMessage("Players only")
    return true
  }
  sender.inventory.addItem(makeRecallStick())
  sender.sendMessage("Added Recall Stick")
  true
}

events.on(PlayerInteractEvent, EventPriority.NORMAL, false) { event ->
  if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) {
    return
  }
  def player = event.player
  if (!isRecallStick(event.item) &&
      !isRecallStick(player.inventory.itemInMainHand) &&
      !isRecallStick(player.inventory.itemInOffHand)) {
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

  def eye = player.eyeLocation
  def direction = eye.direction.normalize()
  def result = player.world.rayTrace(
    eye,
    direction,
    MAX_RECALL_DISTANCE,
    FluidCollisionMode.NEVER,
    true,
    0.45D,
    { entity -> entity.uniqueId != player.uniqueId } as java.util.function.Predicate
  )

  def target = result?.hitEntity
  if (target == null) {
    player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5F, 0.8F)
    return
  }

  def from = target.location.clone()
  def destination = player.location.clone().add(player.location.direction.normalize().multiply(1.4D))
  destination.y = player.location.y
  destination.yaw = target.location.yaw
  destination.pitch = target.location.pitch

  target.world.spawnParticle(Particle.PORTAL, from.clone().add(0, 0.8D, 0), 28, 0.35D, 0.55D, 0.35D, 0.08D)
  target.teleport(destination)
  target.fallDistance = 0
  player.world.spawnParticle(Particle.PORTAL, destination.clone().add(0, 0.8D, 0), 28, 0.35D, 0.55D, 0.35D, 0.08D)
  player.world.playSound(player.location, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8F, 1.2F)
  player.swingMainHand()
}
