commands.register("redsword", "Give a red iron sword") { sender, args ->
  if (!sender.respondsTo("getInventory")) {
    sender.sendMessage("Players only")
    return true
  }

  def sword = items.create("iron_sword_red")
  def meta = sword.itemMeta
  meta.displayName(net.kyori.adventure.text.Component.text("Red Iron Sword"))
  sword.itemMeta = meta
  sender.inventory.addItem(sword)
  sender.sendMessage("Added iron_sword_red")
  true
}
