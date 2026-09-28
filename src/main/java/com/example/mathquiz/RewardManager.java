package com.example.mathquiz;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Handles the three reward types: golden apples, enchanted golden apple, mystery box. */
public class RewardManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final String[] TIERS = {"IRON", "DIAMOND", "NETHERITE"};
    private static final int[] TIER_WEIGHTS = {50, 40, 10};
    private static final String[] PIECES = {"HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS", "SWORD", "PICKAXE", "AXE"};

    private static final List<Enchantment> ENCHANTS = List.of(
            Enchantment.PROTECTION, Enchantment.UNBREAKING, Enchantment.MENDING,
            Enchantment.SHARPNESS, Enchantment.EFFICIENCY, Enchantment.FORTUNE,
            Enchantment.FIRE_PROTECTION, Enchantment.FEATHER_FALLING, Enchantment.LOOTING);

    private final FileConfiguration cfg;

    public RewardManager(FileConfiguration cfg) {
        this.cfg = cfg;
    }

    /** Picks a random reward, gives it to the player, returns a description for chat. */
    public Component giveRandomReward(Player p) {
        int apples = Math.max(0, cfg.getInt("rewards.golden-apples.weight", 50));
        int enchanted = Math.max(0, cfg.getInt("rewards.enchanted-golden-apple.weight", 20));
        int box = Math.max(0, cfg.getInt("rewards.mystery-box.weight", 30));
        int total = apples + enchanted + box;
        if (total <= 0) return MM.deserialize("<gray>nothing (all rewards disabled)");

        int roll = ThreadLocalRandom.current().nextInt(total);
        if (roll < apples) {
            int amt = cfg.getInt("rewards.golden-apples.amount", 10);
            give(p, new ItemStack(Material.GOLDEN_APPLE, amt));
            return MM.deserialize("<gold><bold>" + amt + "x Golden Apples");
        }
        roll -= apples;
        if (roll < enchanted) {
            int amt = cfg.getInt("rewards.enchanted-golden-apple.amount", 1);
            give(p, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, amt));
            return MM.deserialize("<light_purple><bold>" + amt + "x Enchanted Golden Apple");
        }
        int items = Math.max(1, cfg.getInt("rewards.mystery-box.items", 3));
        for (int i = 0; i < items; i++) give(p, randomGear());
        return MM.deserialize("<aqua><bold>Mystery Box <gray>(" + items + " pieces of gear)");
    }

    private ItemStack randomGear() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        int roll = rnd.nextInt(100), acc = 0;
        String tier = TIERS[0];
        for (int i = 0; i < TIERS.length; i++) {
            acc += TIER_WEIGHTS[i];
            if (roll < acc) { tier = TIERS[i]; break; }
        }
        String piece = PIECES[rnd.nextInt(PIECES.length)];
        ItemStack item = new ItemStack(Material.valueOf(tier + "_" + piece));

        List<Enchantment> pool = new ArrayList<>(ENCHANTS);
        Collections.shuffle(pool);
        int wanted = rnd.nextInt(2, 4); // 2-3 enchants
        int added = 0;
        for (Enchantment e : pool) {
            if (added >= wanted) break;
            if (!e.canEnchantItem(item)) continue;
            item.addEnchantment(e, rnd.nextInt(1, e.getMaxLevel() + 1));
            added++;
        }
        return item;
    }

    private void give(Player p, ItemStack item) {
        p.getInventory().addItem(item).values()
                .forEach(left -> p.getWorld().dropItemNaturally(p.getLocation(), left));
    }
}
