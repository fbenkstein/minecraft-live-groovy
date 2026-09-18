commands.register("palettekit", "Give a set of retextured palette items") { sender, args ->
  if (!sender.respondsTo("getInventory")) {
    sender.sendMessage("Players only")
    return true
  }

  def give = { id, name ->
    def stack = items.create(id)
    def meta = stack.itemMeta
    meta.displayName(net.kyori.adventure.text.Component.text(name))
    stack.itemMeta = meta
    sender.inventory.addItem(stack)
  }

  give("iron_sword_red", "Red Iron Sword")
  give("diamond_pickaxe_purple", "Purple Diamond Pickaxe")
  give("golden_axe_blue", "Blue Golden Axe")
  give("netherite_shovel_green", "Green Netherite Shovel")
  give("bow_cyan", "Cyan Bow")
  give("book_pink", "Pink Book")
  give("apple_black", "Black Apple")

  sender.sendMessage("Added palette kit")
  true
}
