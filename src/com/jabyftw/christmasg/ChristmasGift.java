package com.jabyftw.christmasg;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Rafael
 */
public class ChristmasGift extends JavaPlugin implements CommandExecutor {

    CustomConfig configYML;
    FileConfiguration config;
    private int giftDelay, grinchChance;
    private RandomCollection<ItemStack> gifts = new RandomCollection();
    private Map<String, Map<Integer, ItemStack>> remaining = new HashMap();

    @Override
    public void onEnable() {
        configYML = new CustomConfig(this, "config");
        config = configYML.getCustomConfig();
        // CONFIGURATION
        config.options().header("Version 0.1");
        config.options().copyHeader(true);
        config.addDefault("config.repeatDelayInHour", 24);
        String[] gift = {"0.8;diamond;10", "0.2;diamond_sword;5"};
        config.addDefault("config.gifts", Arrays.asList(gift));
        config.addDefault("config.grinchPercent", 5);
        //config.addDefault("lang.", "&");
        config.addDefault("lang.grinchMessage", "&4Haha, Grinch got you! &cYou lost your gift. ;(");
        config.addDefault("lang.giftMessage", "&eCongratulations! &6You received a gift!");
        config.addDefault("lang.noPermission", "&cYou dont have permission!");
        config.addDefault("lang.onCooldown", "&cYou can only use this every %cooldown hour(s)");
        config.addDefault("lang.useCommandToGetItems", "&6There are items remaining! Use &e/christmas remaining &6to get them.");
        config.addDefault("lang.thereAreNoItemsRemaining", "&cThere are no items remaining!");
        config.addDefault("cooldown.test", System.currentTimeMillis());
        config.options().copyDefaults(true);
        saveConfig();
        reloadConfig();
        for (Map.Entry<Double, ItemStack> set : getItemStackFromString(config.getStringList("config.gifts")).entrySet()) {
            gifts.add(set.getKey(), set.getValue());
        }
        giftDelay = config.getInt("config.repeatDelayInHour");
        grinchChance = config.getInt("config.grinchPercent");
        // PLUGIN
        getServer().getPluginCommand("christmas").setExecutor(this);
        getLogger().log(Level.INFO, "Loaded, merry christmas! (;");
    }

    @Override
    public void onDisable() {
        getLogger().log(Level.INFO, "Thanks for using ChristmasGift by rfsantos1996!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0) {
            if (sender instanceof Player) {
                if (sender.hasPermission("christmas.get")) {
                    Player p = (Player) sender;
                    String name = p.getName().toLowerCase();
                    if (remaining.containsKey(name)) {
                        giveRemaining(p);
                        return true;
                    } else {
                        sender.sendMessage(getLang("thereAreNoItemsRemaining"));
                        return true;
                    }
                } else {
                    sender.sendMessage(getLang("noPermission"));
                    return true;
                }
            } else {
                sender.sendMessage("Not avaliable on console");
                return true;
            }
        } else {
            if (sender instanceof Player) {
                if (sender.hasPermission("christmas.get")) {
                    Player p = (Player) sender;
                    String name = p.getName().toLowerCase();
                    long timeUsed = config.getLong("cooldown." + name);
                    if (timeUsed > 0) { // have cooldown time
                        if (outOfCooldown(timeUsed)) {
                            giveGift(p, false);
                            return true;
                        } else {
                            sender.sendMessage(getLang("onCooldown").replaceAll("%cooldown", Integer.toString(giftDelay)));
                            return true;
                        }
                    } else { // first time
                        giveGift(p, true);
                        return true;
                    }
                } else {
                    sender.sendMessage(getLang("noPermission"));
                    return true;
                }
            } else {
                sender.sendMessage("Not avaliable on console");
                return true;
            }
        }
    }

    private String getLang(String path) {
        return config.getString("lang." + path).replaceAll("&", "§");
    }

    private Map<Double, ItemStack> getItemStackFromString(List<String> s) {
        Map<Double, ItemStack> map = new HashMap();
        for (String st : s) {
            String[] st2 = st.split(";");
            for (Material m : Material.values()) {
                if (m.toString().equalsIgnoreCase(st2[1])) {
                    map.put(Double.parseDouble(st2[0]), new ItemStack(m, Integer.parseInt(st2[2])));
                    break;
                }
            }
        }
        return map;
    }

    private boolean takePresent(int chance) {
        Random r = new Random();
        return (r.nextInt(100) < chance);
    }

    private void giveGift(Player p, boolean firstTime) {
        if (!takePresent(grinchChance)) {
            Map<Integer, ItemStack> it = p.getInventory().addItem(gifts.next());
            if (it.size() > 0) {
                remaining.put(p.getName().toLowerCase(), it);
                p.sendMessage(getLang("useCommandToGetItems"));
            } else {
                p.sendMessage(getLang("giftMessage"));
            }
        } else {
            p.sendMessage(getLang("grinchMessage"));
        }
        if (firstTime) {
            config.addDefault("cooldown." + p.getName().toLowerCase(), System.currentTimeMillis());
        } else {
            config.set("cooldown." + p.getName().toLowerCase(), System.currentTimeMillis());
        }
        configYML.saveCustomConfig();
    }

    private void giveRemaining(Player p) {
        Map<Integer, ItemStack> it = p.getInventory().addItem(gifts.next());
        if (it.size() > 0) {
            remaining.put(p.getName().toLowerCase(), it);
            p.sendMessage(getLang("useCommandToGetItems"));
        } else {
            remaining.remove(p.getName().toLowerCase());
            p.sendMessage(getLang("giftMessage"));
        }
    }

    private boolean outOfCooldown(long timeUsed) {
        return System.currentTimeMillis() - timeUsed > (giftDelay * 3600 * 1000);
    }
}
