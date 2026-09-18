import org.bukkit.FluidCollisionMode
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.util.Vector

final double DEFAULT_CLONE_DISTANCE = 96.0D
final double MAX_CLONE_DISTANCE = 192.0D
final int DEFAULT_CLONE_COUNT = 1
final int MAX_CLONE_COUNT = 16

def parseIntArg = { args, int index, int fallback ->
  if (args.length <= index) {
    return fallback
  }
  try {
    return Integer.parseInt(args[index] as String)
  } catch (NumberFormatException ignored) {
    return fallback
  }
}

def parseDoubleArg = { args, int index, double fallback ->
  if (args.length <= index) {
    return fallback
  }
  try {
    return Double.parseDouble(args[index] as String)
  } catch (NumberFormatException ignored) {
    return fallback
  }
}

commands.register(
  "cloneentity",
  "Clone the entity in your crosshair",
  "/cloneentity [count] [distance]",
  ["entityclone", "eclone"],
) { sender, args ->
  if (!(sender instanceof Player)) {
    sender.sendMessage("Players only")
    return true
  }

  int count = Math.max(1, Math.min(MAX_CLONE_COUNT, parseIntArg(args, 0, DEFAULT_CLONE_COUNT)))
  double distance = Math.max(1.0D, Math.min(MAX_CLONE_DISTANCE, parseDoubleArg(args, 1, DEFAULT_CLONE_DISTANCE)))

  def player = sender
  def eye = player.eyeLocation
  def direction = eye.direction.normalize()
  def result = player.world.rayTrace(
    eye,
    direction,
    distance,
    FluidCollisionMode.NEVER,
    true,
    0.45D,
    { entity -> entity.uniqueId != player.uniqueId } as java.util.function.Predicate
  )

  def target = result?.hitEntity
  if (target == null) {
    player.sendMessage("No entity in sight")
    player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5F, 0.8F)
    return true
  }
  if (target instanceof Player) {
    player.sendMessage("Players cannot be cloned")
    player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5F, 0.8F)
    return true
  }

  def baseLocation = target.location.clone()
  def side = new Vector(-direction.z, 0.0D, direction.x)
  if (side.lengthSquared() < 0.0001D) {
    side = new Vector(1.0D, 0.0D, 0.0D)
  } else {
    side.normalize()
  }

  int cloned = 0
  for (int i = 0; i < count; i++) {
    double row = Math.floor(i / 5.0D)
    double column = (i % 5) - Math.min(count - 1, 4) / 2.0D
    def offset = direction.clone().multiply(1.5D + row * 1.25D).add(side.clone().multiply(column * 1.25D))
    def cloneLocation = baseLocation.clone().add(offset)
    cloneLocation.yaw = baseLocation.yaw
    cloneLocation.pitch = baseLocation.pitch

    try {
      def clone = target.copy(cloneLocation)
      if (clone != null) {
        clone.velocity = target.velocity.clone()
        cloned++
      }
    } catch (Throwable e) {
      plugin.logger.warning("Unable to clone entity " + target.type + ": " + e.message)
      player.sendMessage("That entity could not be cloned")
      player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5F, 0.8F)
      return true
    }
  }

  if (cloned > 0) {
    target.world.spawnParticle(Particle.PORTAL, baseLocation.clone().add(0, target.height * 0.5D, 0), 36, 0.4D, 0.6D, 0.4D, 0.08D)
    player.world.playSound(baseLocation, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8F, 1.6F)
    player.sendMessage("Cloned " + cloned + " " + target.type.key.key)
    player.swingMainHand()
  }
  true
}
