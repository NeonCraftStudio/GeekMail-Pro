package me.geek.mail.common.menu.action

import me.geek.mail.api.data.SqlManage.getData
import me.geek.mail.api.mail.MailState
import me.geek.mail.api.mail.MailSub
import me.geek.mail.common.menu.MenuBase
import me.geek.mail.common.menu.sub.MenuData
import me.geek.mail.common.menu.sub.IconType.*
import me.geek.mail.modules.catcher.Chat
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import taboolib.platform.util.sendLang

/**
 * 作者: 老廖
 * 时间: 2023/1/21
 *
 **/
class MailMenu(
    override val player: Player,
    override val menuData: MenuData
): MenuBase() {

    private val ioc: MutableMap<Int, MailSub> = mutableMapOf()
    override fun build(): MenuBase {
        val data = player.getData()
        var item = this.inventory.contents
        if (data.mailData.isNotEmpty()) {
            var mailSize = data.mailData.size
            while (mailSize > 0) {
                menuData.layout.forEachIndexed { index, value ->
                    if (value != ' ') {
                        menuData.icon[value]?.let {  icon ->
                            if (icon.iconType == TEXT) {
                                if (mailSize > 0) {
                                    val i = data.mailData[data.mailData.size - mailSize].getIcon(icon)
                                    ioc[index] = data.mailData[data.mailData.size - mailSize]
                                    item[index] = i
                                    mailSize--
                                }
                            }
                        }
                    }
                }
                this.contents.add(item)
                item = this.inventory.contents
            }
            sound("BLOCK_NOTE_BLOCK_HARP",1f, 1f)
            this.openMenu()
        }
        return this
    }
    private var view: Boolean = false
    private var cd: Long = 0

    override fun onClick(event: InventoryClickEvent) {
        event.isCancelled = true
        if (view) {
            this.inventory.contents = contents[page]
            view = false
            return
        }
        if (cd < System.currentTimeMillis()) cd = System.currentTimeMillis() + 200 else return

        menuData.layout[event.rawSlot].let {
            menuData.icon[it]?.let { icon ->
                when (icon.iconType) {
                    TEXT -> {
                        val mail = ioc[event.rawSlot] ?: error(" 邮件索引错误")
                        if (event.isRightClick) {
                            if (mail.state == MailState.Acquired) {
                                val itemStacks = this.contents[page]
                                itemStacks[event.rawSlot] = ItemStack(Material.AIR)

                                ioc.remove(event.rawSlot) // 未成功删除标记， 可能无法正常删除

                                this.inventory.contents = itemStacks
                                this.contents[page] = itemStacks
                                player.sendLang("玩家-删除邮件-成功")
                            } else player.sendLang("玩家-删除邮件-失败")
                        }
                        // 领取邮件附件
                        if (event.isLeftClick && !event.isShiftClick) {
                            if (mail.state == MailState.NotObtained) {
                                mail.state = MailState.Acquired
                                mail.getTime = System.currentTimeMillis()
                                val item = event.currentItem!!
                                val itemMeta = item.itemMeta
                                if (itemMeta != null && itemMeta.lore != null) {
                                    itemMeta.lore = mail.parseMailInfo(itemMeta.lore!!)
                                    itemMeta.removeEnchant(Enchantment.DAMAGE_ALL)
                                    itemMeta.removeItemFlags(ItemFlag.HIDE_ENCHANTS)
                                }
                                event.currentItem = item
                                this.contents[page] = event.inventory.contents

                                if (mail.itemStacks != null && mail.itemStacks!!.isNotEmpty()) {
                                    if (!mail.giveAppendix()) return
                                } else mail.giveAppendix()

                                player.sendLang("玩家-领取附件-成功", mail.appendixInfo)
                            } else if (mail.state == MailState.Acquired) player.sendLang("玩家-领取附件-失败")
                            return
                        }
                        if (event.click == ClickType.MIDDLE && mail.mailType.contains("物品")) {
                            view = true
                            this.inventory.contents = mail.itemStacks!!
                            return
                        }
                        return
                    }

                    DELETE -> {
                        if (player.getData().mailData.size >= 1) {
                            player.closeInventory()
                            player.getData().mailData.removeIf { mail -> mail.state == MailState.Acquired }
                            sound("BLOCK_SOUL_SAND_STEP",0.7f, 1f)
                        } else sound("BLOCK_NOTE_BLOCK_DIDGERIDOO",0.7f, 1f)
                        return
                    }
                    GET_ALL -> {
                        if (player.getData().mailData.size >= 1) {
                            player.closeInventory()
                            player.getData().mailData.forEach { mail ->
                                if (mail.state == MailState.NotObtained) {
                                    if (mail.giveAppendix()) {
                                        mail.state = MailState.Acquired
                                        mail.getTime = System.currentTimeMillis()
                                    }
                                }
                            }
                        }
                    }

                    BIND -> {
                        if (player.getData().mail.isEmpty()) {
                            Chat(player).start()
                            player.closeInventory()
                        }
                        return
                    }

                    LAST_PAGE -> {
                        if (this.page != 0) {
                            sound("BLOCK_SCAFFOLDING_BREAK",1f, 1f)
                            // 上跳页面
                            this.page -= 1
                            this.inventory.contents = this.contents[page]
                        } else sound("BLOCK_NOTE_BLOCK_DIDGERIDOO",1f, 1f)
                        return
                    }
                    NEXT_PAGE -> {
                        if (this.contents.size > this.page + 1 ) {
                            sound("BLOCK_SCAFFOLDING_BREAK",1f, 1f)
                            // 下跳页面
                            this.page += 1
                            this.inventory.contents = this.contents[page]
                        } else sound("BLOCK_NOTE_BLOCK_DIDGERIDOO",1f, 1f)
                        return
                    }
                    else -> return
                }
            }
        }
    }

    override fun onClose(event: InventoryCloseEvent) {

    }
}