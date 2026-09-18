import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.entity.EntityType
import org.bukkit.entity.Pig
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Vector

final String PIG_STICK_MARKER = "pig_stick"
final double LAUNCHED_PIG_SCALE = 0.6D
final NamespacedKey itemKey = new NamespacedKey(plugin, "pig_stick")
final NamespacedKey recipeKey = new NamespacedKey(plugin, "pig_stick")
final Set launchedPigs = Collections.synchronizedSet(new HashSet())

def makePigStick = {
  def stack = items.create("stick_pink")
  def meta = stack.itemMeta
  meta.displayName(net.kyori.adventure.text.Component.text("Pig Stick"))
  meta.lore([
    net.kyori.adventure.text.Component.text("Right-click to launch a flaming pig")
  ])
  meta.persistentDataContainer.set(itemKey, PersistentDataType.STRING, PIG_STICK_MARKER)
  stack.itemMeta = meta
  stack
}

def isPigStick = { stack ->
  if (stack == null || !stack.hasItemMeta()) {
    return false
  }
  PIG_STICK_MARKER == stack.itemMeta.persistentDataContainer.get(itemKey, PersistentDataType.STRING)
}

Bukkit.removeRecipe(recipeKey)
def recipe = new ShapedRecipe(recipeKey, makePigStick())
recipe.shape(" P ", " S ", " B ")
recipe.setIngredient('P' as char, Material.PORKCHOP)
recipe.setIngredient('S' as char, Material.STICK)
recipe.setIngredient('B' as char, Material.BLAZE_POWDER)
Bukkit.addRecipe(recipe)

commands.register("pigstick", "Give yourself a Pig Stick") { sender, args ->
  if (!sender.respondsTo("getInventory")) {
    sender.sendMessage("Players only")
    return true
  }
  sender.inventory.addItem(makePigStick())
  sender.sendMessage("Added Pig Stick")
  true
}

events.on(PlayerInteractEvent, EventPriority.NORMAL, false) { event ->
  if (event.hand != EquipmentSlot.HAND) {
    return
  }
  if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) {
    return
  }
  if (!isPigStick(event.item)) {
    return
  }

  event.cancelled = true
  def player = event.player
  def direction = player.eyeLocation.direction.normalize()
  def spawnLocation = player.eyeLocation.add(direction.clone().multiply(1.4))
  def pig = player.world.spawnEntity(spawnLocation, EntityType.PIG)

  pig.customName(net.kyori.adventure.text.Component.text("Flaming Pig"))
  pig.customNameVisible = true
  pig.fireTicks = 20 * 12
  pig.getAttribute(Attribute.SCALE)?.setBaseValue(LAUNCHED_PIG_SCALE)
  pig.health = Math.min(pig.health, 6.0D)
  pig.velocity = direction.clone().multiply(2.2D).add(new Vector(0, 0.25D, 0))
  pig.persistentDataContainer.set(itemKey, PersistentDataType.STRING, PIG_STICK_MARKER)
  launchedPigs.add(pig.uniqueId)

  player.world.playSound(player.location, Sound.ENTITY_PIG_AMBIENT, 1.0F, 1.4F)
  player.swingMainHand()
}

events.on(EntityDeathEvent) { event ->
  def entity = event.entity
  if (!(entity instanceof Pig)) {
    return
  }
  boolean tracked = launchedPigs.remove(entity.uniqueId)
  if (!tracked && PIG_STICK_MARKER != entity.persistentDataContainer.get(itemKey, PersistentDataType.STRING)) {
    return
  }
  def deathLocation = entity.location.clone()
  entity.world.spawnParticle(Particle.FLAME, deathLocation, 30, 0.4D, 0.4D, 0.4D, 0.05D)
  scheduler.later(5L) {
    deathLocation.world.createExplosion(deathLocation, 3.0F, true, false)
  }
}

scheduler.repeat(1L, 2L) {
  launchedPigs.removeIf { id ->
    def entity = Bukkit.getEntity(id)
    if (entity == null || entity.dead) {
      return true
    }
    entity.fireTicks = Math.max(entity.fireTicks, 20)
    entity.world.spawnParticle(Particle.SMOKE, entity.location.add(0, 0.35D, 0), 2, 0.15D, 0.15D, 0.15D, 0.01D)
    false
  }
}
