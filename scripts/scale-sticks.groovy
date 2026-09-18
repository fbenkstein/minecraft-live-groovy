import org.bukkit.Bukkit
import org.bukkit.FluidCollisionMode
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.persistence.PersistentDataType

final String SCALE_STICK_MARKER = "scale_stick"
final String SHRINK_MODE = "shrink"
final String GROW_MODE = "grow"
final double MAX_SCALE_DISTANCE = 192.0D
final double MIN_SCALE = 0.0625D
final double MAX_SCALE = 16.0D
final NamespacedKey itemKey = new NamespacedKey(plugin, "scale_stick")
final NamespacedKey shrinkRecipeKey = new NamespacedKey(plugin, "shrink_stick")
final NamespacedKey growRecipeKey = new NamespacedKey(plugin, "grow_stick")
final Map lastUseAt = Collections.synchronizedMap(new HashMap())

def makeScaleStick = { String mode, String itemId, String name, String lore ->
  def stack = items.create(itemId)
  def meta = stack.itemMeta
  meta.displayName(net.kyori.adventure.text.Component.text(name))
  meta.lore([
    net.kyori.adventure.text.Component.text(lore)
  ])
  meta.persistentDataContainer.set(itemKey, PersistentDataType.STRING, mode)
  stack.itemMeta = meta
  stack
}

def makeShrinkStick = {
  makeScaleStick(SHRINK_MODE, "stick_cyan", "Shrink Stick", "Right-click an entity to scale it by 0.9")
}

def makeGrowStick = {
  makeScaleStick(GROW_MODE, "stick_yellow", "Grow Stick", "Right-click an entity to scale it by 1.1")
}

def scaleMode = { stack ->
  if (stack == null || !stack.hasItemMeta()) {
    return null
  }
  def mode = stack.itemMeta.persistentDataContainer.get(itemKey, PersistentDataType.STRING)
  (mode == SHRINK_MODE || mode == GROW_MODE) ? mode : null
}

Bukkit.removeRecipe(shrinkRecipeKey)
def shrinkRecipe = new ShapedRecipe(shrinkRecipeKey, makeShrinkStick())
shrinkRecipe.shape(" L ", " S ", " A ")
shrinkRecipe.setIngredient('L' as char, Material.SLIME_BALL)
shrinkRecipe.setIngredient('S' as char, Material.STICK)
shrinkRecipe.setIngredient('A' as char, Material.AMETHYST_SHARD)
Bukkit.addRecipe(shrinkRecipe)

Bukkit.removeRecipe(growRecipeKey)
def growRecipe = new ShapedRecipe(growRecipeKey, makeGrowStick())
growRecipe.shape(" B ", " S ", " A ")
growRecipe.setIngredient('B' as char, Material.BONE_MEAL)
growRecipe.setIngredient('S' as char, Material.STICK)
growRecipe.setIngredient('A' as char, Material.AMETHYST_SHARD)
Bukkit.addRecipe(growRecipe)

commands.register("shrinkstick", "Give yourself a Shrink Stick") { sender, args ->
  if (!sender.respondsTo("getInventory")) {
    sender.sendMessage("Players only")
    return true
  }
  sender.inventory.addItem(makeShrinkStick())
  sender.sendMessage("Added Shrink Stick")
  true
}

commands.register("growstick", "Give yourself a Grow Stick") { sender, args ->
  if (!sender.respondsTo("getInventory")) {
    sender.sendMessage("Players only")
    return true
  }
  sender.inventory.addItem(makeGrowStick())
  sender.sendMessage("Added Grow Stick")
  true
}

commands.register("scalesticks", "Give yourself both Scale Sticks") { sender, args ->
  if (!sender.respondsTo("getInventory")) {
    sender.sendMessage("Players only")
    return true
  }
  sender.inventory.addItem(makeShrinkStick())
  sender.inventory.addItem(makeGrowStick())
  sender.sendMessage("Added Scale Sticks")
  true
}

events.on(PlayerInteractEvent, EventPriority.NORMAL, false) { event ->
  if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) {
    return
  }
  def player = event.player
  def mode = scaleMode(event.item)
  if (mode == null) mode = scaleMode(player.inventory.itemInMainHand)
  if (mode == null) mode = scaleMode(player.inventory.itemInOffHand)
  if (mode == null) {
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
    MAX_SCALE_DISTANCE,
    FluidCollisionMode.NEVER,
    true,
    0.45D,
    { entity -> entity.uniqueId != player.uniqueId } as java.util.function.Predicate
  )

  def target = result?.hitEntity
  if (target == null || !target.respondsTo("getAttribute")) {
    player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5F, 0.8F)
    return
  }

  def scaleAttribute = target.getAttribute(Attribute.SCALE)
  if (scaleAttribute == null) {
    player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5F, 0.8F)
    return
  }

  double multiplier = mode == SHRINK_MODE ? 0.9D : 1.1D
  double nextScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scaleAttribute.baseValue * multiplier))
  scaleAttribute.setBaseValue(nextScale)

  target.world.spawnParticle(Particle.PORTAL, target.location.clone().add(0, 0.8D, 0), 22, 0.35D, 0.55D, 0.35D, 0.05D)
  player.world.playSound(target.location, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7F, mode == SHRINK_MODE ? 1.6F : 0.8F)
  player.swingMainHand()
}
