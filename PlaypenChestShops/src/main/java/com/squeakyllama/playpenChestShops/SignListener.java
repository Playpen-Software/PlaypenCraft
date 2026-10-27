package com.squeakyllama.playpenChestShops;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;

import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class SignListener implements Listener {
    JavaPlugin plugin;

    SignListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Block signBlock = event.getBlock();
        Material signMaterial = signBlock.getType();

        // Return if not a wall sign
        if (!Tag.WALL_SIGNS.isTagged(signMaterial)) return;

        // Get the block data to determine what the sign is attached to
        BlockData blockData = signBlock.getBlockData();
        if (!(blockData instanceof WallSign)) return;

        WallSign wallSign = (WallSign) blockData;
        Block attachedBlock = signBlock.getRelative(wallSign.getFacing().getOppositeFace());

        // Return if the block the sign was placed against is not a chest
        if (attachedBlock.getType() != Material.CHEST) return;

        List<Component> lines = event.lines();

        String lineZeroText = PlainTextComponentSerializer.plainText()
                .serialize(lines.get(ShopManager.typeIndex))
                .trim()
                .toLowerCase(Locale.ROOT);

        // Ignore signs that aren't shops
        if (!lineZeroText.equals("chestshop")) return;

        ShopManager.createShop(event, attachedBlock);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        // Ensure a block was actually clicked
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        // Check if the clicked block's state is a Sign
        if (clickedBlock.getState() instanceof Sign sign) {
            PersistentDataContainer pdc = sign.getPersistentDataContainer();

            // Return if the sign doesn't have the required data to be a shop.
            if (!pdc.has(ShopManager.ShopOwnerKey, PersistentDataType.STRING)) return;

            // Cancel the event so the sign editing GUI never opens for registered shops
            event.setCancelled(true);

            Player player = event.getPlayer();
            String ownerUUIDString = pdc.get(ShopManager.ShopOwnerKey, PersistentDataType.STRING);
            boolean shopIsActive = Boolean.TRUE.equals(pdc.get(ShopManager.ShopStateKey, PersistentDataType.BOOLEAN));
            if (shopIsActive) return;

            if (ownerUUIDString != null) {
                try {
                    UUID ownerUUID = UUID.fromString(ownerUUIDString);
                    UUID playerUUID = player.getUniqueId();

                    if (!playerUUID.equals(ownerUUID)) {
                        player.sendRichMessage("<red>You cannot activate this shop, because you are not the owner</red>");
                    }
                    return;
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID stored in shop sign: " + ownerUUIDString);
                    player.sendRichMessage("<red>An error has occurred trying to activate this shop. Try again. Contact your server's admin if this problem persists");
                    return;
                }
            }

            // Check if the player's main hand is empty
            ItemStack mainHandItem = event.getItem();
            if (mainHandItem == null || mainHandItem.getType() == Material.AIR) {
                player.sendRichMessage("<red>To activate this shop, you must be holding the item you wish to sell</red>");
                return;
            }

            ShopManager.activateShop(sign, mainHandItem);
        }
    }
}