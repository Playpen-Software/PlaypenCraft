package com.squeakyllama.playpenChestShops;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class ShopManager {
    // NOTE: Lines are 0-indexed, meaning 0 is the first line.
    public static int typeIndex = 0;
    public static int actionInitialIndex = 1;
    public static int priceInitialIndex = 2;
    public static int currencyInitialIndex = 3;
    public static int actionIndex = 0;
    public static int priceIndex = 1;
    public static int currencyIndex = 2;
    public static int ownerIndex = 3;

    private static final PlaypenChestShops plugin = PlaypenChestShops.getPlugin(PlaypenChestShops.class);

    public static final NamespacedKey ShopOwnerKey = new NamespacedKey(plugin, "shop_owner");
    public static final NamespacedKey ShopStateKey = new NamespacedKey(plugin, "shop_is_active");
    public static final NamespacedKey ChestPosXKey = new NamespacedKey(plugin, "shop_chest_x");
    public static final NamespacedKey ChestPosYKey = new NamespacedKey(plugin, "shop_chest_y");
    public static final NamespacedKey ChestPosZKey = new NamespacedKey(plugin, "shop_chest_z");
    public static final NamespacedKey currencyKey = new NamespacedKey(plugin, "shop_currency");
    public static final NamespacedKey priceKey = new NamespacedKey(plugin, "shop_price");

    public static void createShop(SignChangeEvent event, Block chest) {
        Material chestType = chest.getType();
        if (chestType != Material.CHEST) return;

        List<Component> lines = event.lines();
        UUID shopOwnerUUID = event.getPlayer().getUniqueId();

        if (event.getBlock().getState() instanceof Sign sign) {
            PersistentDataContainer pdc = sign.getPersistentDataContainer();
            Player shopOwner = Bukkit.getPlayer(shopOwnerUUID);

            assert shopOwner != null;

            Component priceLine = lines.get(priceInitialIndex);
            Component currencyLine = lines.get(currencyInitialIndex);

            int price = getItemPrice(priceLine, shopOwner);
            if (price <= 0) {
                shopOwner.sendRichMessage("<red>Please enter a valid number above 0 for the price of your item. If this issue persists, contact moderators.</red>");
                return;
            }

            pdc.set(priceKey, PersistentDataType.INTEGER, price);

            String sanitizedCurrencyId = PlainTextComponentSerializer
                    .plainText()
                    .serialize(currencyLine)
                    .trim()
                    .toUpperCase(Locale.ROOT);

            if (!isValidMaterial(sanitizedCurrencyId)) {
                shopOwner.sendRichMessage("<red>Please enter a valid item ID (eg. DIAMOND_BLOCK) for the currency. If this issue persists, contact moderators.</red>");
                return;
            }

            pdc.set(currencyKey, PersistentDataType.STRING, sanitizedCurrencyId);

            pdc.set(ChestPosXKey, PersistentDataType.INTEGER, chest.getX());
            pdc.set(ChestPosYKey, PersistentDataType.INTEGER, chest.getY());
            pdc.set(ChestPosZKey, PersistentDataType.INTEGER, chest.getZ());

            pdc.set(ShopStateKey, PersistentDataType.BOOLEAN, false);

            pdc.set(ShopOwnerKey, PersistentDataType.STRING, shopOwnerUUID.toString());
            sign.update(true);

            shopOwner.sendRichMessage("<green>Your shop is ready for activation. To activate, interact with the sign (right click or tap) while holding the block or item you want to sell in it</green>");
        }
    }

    public static void activateShop(Sign sign, ItemStack product) {

        formatSign(sign);
    }

    private static void formatSign(Sign sign) {
        UUID shopOwnerUUID = event.getPlayer().getUniqueId();

        Map<String, String> actionFormatMap = new HashMap<>();
        actionFormatMap.put("selling", "[SELLING]");
        actionFormatMap.put("sell", "[SELLING]");
        actionFormatMap.put("[selling]", "[SELLING]");
        actionFormatMap.put("[sell]", "[SELLING]");

        Player shopOwner = Bukkit.getPlayer(shopOwnerUUID);
        assert shopOwner != null;

        Component actionLine = event.line(actionInitialIndex);
        Component priceLine = event.line(priceInitialIndex);
        Component currencyLine = event.line(currencyInitialIndex);

        if (actionLine == null) actionLine = Component.empty();
        if (priceLine == null) priceLine = Component.empty();
        if (currencyLine == null) currencyLine = Component.empty();

        String actionText = PlainTextComponentSerializer.plainText()
                .serialize(actionLine)
                .trim()
                .toLowerCase(Locale.ROOT);

        if (!actionFormatMap.containsKey(actionText)) {
            shopOwner.sendRichMessage("<red>Please enter a valid action: sell</red>");
            return;
        }

        String replacementValue = actionFormatMap.get(actionText);
        TextReplacementConfig replacementConfig = TextReplacementConfig.builder()
                .matchLiteral(actionText)
                .replacement(replacementValue)
                .build();

        actionLine = actionLine.replaceText(replacementConfig)
                .color(NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, TextDecoration.State.TRUE);

        String sanitizedCurrencyId = PlainTextComponentSerializer
                .plainText()
                .serialize(currencyLine)
                .trim()
                .toUpperCase(Locale.ROOT);

        if (!isValidMaterial(sanitizedCurrencyId)) {
            shopOwner.sendRichMessage("<red>Please enter a valid item ID (eg. DIAMOND_BLOCK) for the currency</red>");
            return;
        }

        currencyLine = MiniMessage.miniMessage().deserialize(sanitizedCurrencyId)
                .color(NamedTextColor.AQUA);

        Component ownerUsername = MiniMessage.miniMessage().deserialize(shopOwner.getName());

        event.line(actionIndex, actionLine);
        event.line(priceIndex, priceLine);
        event.line(currencyIndex, currencyLine);
        event.line(ownerIndex, ownerUsername);

        if (event.getBlock().getState() instanceof Sign sign) {
            SignSide frontSide = sign.getSide(Side.FRONT);

            frontSide.line(actionIndex, actionLine);
            frontSide.line(priceIndex, priceLine);
            frontSide.line(currencyIndex, currencyLine);
            frontSide.line(ownerIndex, ownerUsername);

            sign.update(true);
        }
    }

    private static int getItemPrice(Component line, Player shopOwner) {
        String priceLineText = PlainTextComponentSerializer.plainText().serialize(line);

        try {
            return Integer.parseInt(priceLineText.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static boolean isValidMaterial(String material) {
        return Material.matchMaterial(material) != null;
    }
}
