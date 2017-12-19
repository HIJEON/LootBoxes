package me.yhl;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.economy.Economy;
import net.minecraft.server.v1_12_R1.BlockPosition;
import net.minecraft.server.v1_12_R1.TileEntityChest;
import net.minecraft.server.v1_12_R1.World;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.block.Chest;
import org.bukkit.Location;

public class LootBoxes extends JavaPlugin implements Listener{

	 HashMap<Block, Location> chests = new HashMap<Block, Location>();
	 HashMap<Block, Location> after = new HashMap<Block, Location>();
	 HashMap<Block, Boolean> looted = new HashMap<Block, Boolean>();
	 
	    public static Economy econ = null;
	    
	    private boolean setupEconomy() {
	    if (getServer().getPluginManager().getPlugin("Vault") == null) {
	        return false;
	    }
	    RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
	    if (rsp == null) {
	        return false;
	    }
	    econ = rsp.getProvider();
	    return econ != null;
	}
	    
	    public void onEnable() {
	    if (!setupEconomy() ) {
	        getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
	        getServer().getPluginManager().disablePlugin(this);
	        return;
	    }
		saveDefaultConfig();
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
		
	}
    public ItemStack getRandomItemstackFromConfig() {
        ArrayList<ItemStack> aLotOfItemsFromConfig = new ArrayList<ItemStack>();
      
        for (String s : getConfig().getConfigurationSection("items").getKeys(false)) {
            if (Material.matchMaterial(getConfig().getString("items." + s + ".item")) == null) {
                getLogger().log(Level.WARNING, "Item " + s + " isn't a valid item, skipping!");
                continue;
            }
          
            if (Material.matchMaterial(s).equals(Material.AIR)) continue;
          
            ItemStack item = new ItemStack(Material.matchMaterial(getConfig().getString("items." + s + ".item")), getConfig().getInt("items." + s + ".amount"));
            ItemMeta meta = item.getItemMeta();
          
            if (getConfig().contains("items." + s + ".enchantments")) {
                enchantments:
                    for (String e : getConfig().getStringList("items." + s + ".enchantments")) {
                        String[] en = e.split(";");
                        if (Enchantment.getByName(en[0]) == null) {
                            getLogger().log(Level.WARNING, "Enchantment " + en[0] + " for item " + s + " isn't a valid enchantment, skipping!");
                            continue enchantments;
                        } try {
                            meta.addEnchant(Enchantment.getByName(en[0]), Integer.parseInt(en[1]), true);
                        } catch (NumberFormatException ex) {
                            getLogger().log(Level.WARNING, "Enchantment level " + en[1] + " for item " + s + " isn't a valid number, skipping!");
                            continue enchantments;
                        }
                    }
            }

            if (getConfig().contains("items." + s + ".name")) meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', getConfig().getString("items." + s + ".name")));
          
            if (getConfig().contains("items." + s + ".lore")) {
                ArrayList<String> lore = new ArrayList<String>();
                for (String l : getConfig().getStringList("items." + s + ".lore")) lore.add(ChatColor.translateAlternateColorCodes('&', l));
                meta.setLore(lore);
                item.setItemMeta(meta);
                aLotOfItemsFromConfig.add(item);
            }
        }
      
        HashMap<String, Integer> chances = new HashMap<String, Integer>();
      
        for (String key : getConfig().getConfigurationSection("items").getKeys(false)) {
            chances.put(key, getConfig().getInt("items." + key + ".chance"));
        }
      
        int chosen = -1;
        do {
            Random random = new Random();
            int total = 0;
            for (String item : chances.keySet()) {
                total += chances.get(item);
            }
            int chancePicked = random.nextInt(total);
            for (String itemKey : chances.keySet()) {
                if (chancePicked < chances.get(itemKey)) {
                    chosen = Integer.parseInt(itemKey);
                }
            }
        } while (chosen == -1);
      
        return aLotOfItemsFromConfig.get(chosen - 1);
    }
	
	public static void drawFloor(Location cursor, int width, int height, Material mat) {
		Location loc = cursor.clone();
		for (int x = 0; x < width; x++) {
			for (int z = 0; z < height; z++) {
				setType(loc, mat);
				loc.add(0, 0, 1);
			}
			loc.add(1, 0, height * -1);
		}
	}
	
    public static void drawWall(Location cursor, boolean drawEast, int length, int height, Material mat) {
        for (int i = 0; i < length; i++) {
            for (int y = 0; y < height; y++) {
                setType(cursor, mat);
                cursor.add(0, 1, 0);
            }
            
            if (drawEast) {
                cursor.add(1, height * -1, 0);
            } else {
                cursor.add(0, height * -1, 1);
            }
        }
    }

	public void drawCuboid(Location loc, int east, int south, int height, Material mat, Boolean chest) {
		Location cursor = loc.clone();
		for (int y = 0; y < height; y++) {
			drawFloor(cursor, east, south, Material.AIR);
			cursor.add(0,1,0);
		}
		

		
		cursor = loc.clone();
		

		

		cursor.add(0, -1, 0);
		drawFloor(cursor, east, south, mat);
		cursor.add(0, height + 1, 0);
		drawFloor(cursor, east, south, mat);
		cursor = loc.clone();
		drawWall(cursor, true, east, height, mat);
		cursor.add(-1, 0, 0);
		drawWall(cursor, false, south, height, mat);
		cursor = loc.clone();
		drawWall(cursor, false, south, height, mat);
		cursor.add(0, 0, -1);
		drawWall(cursor, true, east, height, mat);
		
		if(chest) {
			int mX = east /2;
			int mZ = south/2;
			
			cursor.add(-mX + -1, 0, -mZ);
			setType(cursor, Material.CHEST);
			chests.put(cursor.getBlock(), cursor);
	        Chest ch = (Chest)cursor.getBlock().getState();
	        Inventory inv = ch.getInventory();
        	int count = getConfig().getInt("numofitems");
	        for(int i = 0; i < count; i++) {
	        inv.addItem(getRandomItemstackFromConfig());
	        }
		}

	}
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.AQUA + "Can't run thru console.");
			return true;
		}

		Player p = (Player) sender;
		Random r = new Random();
		if (cmd.getName().equalsIgnoreCase("LootBox")) {
			
			if(args.length == 0 || args[0].equalsIgnoreCase("help")) {
				if(!p.hasPermission("lootbox.help")) {
				p.sendMessage("§4§lLootBoxes v1");
				}
				else {
				p.sendMessage("§4§lLootBoxes v1");
				p.sendMessage("§4Commands:");
				p.sendMessage("§c/lootbox help");
				p.sendMessage("§c/lootbox <number of lootboxes>");
				}
			}
			
			if(p.hasPermission("LootBox.create")) {
	        if(args.length == 1) {
	        	try {
	        	int arg = Integer.valueOf(args[0]);

	        	int width = getConfig().getInt("width");
	        	int height = getConfig().getInt("height");
	        	int length = getConfig().getInt("length");
	        	int X = getConfig().getInt("X");
	        	int Z = getConfig().getInt("Z");
	        	boolean chest = getConfig().getBoolean("chest");
	        	ItemStack item = new ItemStack(Material.matchMaterial(getConfig().getString("block")));
	        	
	        	p.sendMessage(ChatColor.GOLD + "Generating " + args[0] + " Loot Boxes...");
	        	for(int i = 0; i < arg; i++) {
		        	Location lo = new Location(p.getWorld(), r.nextInt(X), 80, r.nextInt(Z));
	        		drawCuboid(lo, length, width, height, item.getType(), chest);
	                Location loc = p.getLocation();
	                for(int i2 = 0; i2 <360; i2+=5){
	                    Location flameloc = loc;
	                    flameloc.setZ(flameloc.getZ() + Math.cos(i2)*5);
	                    flameloc.setX(flameloc.getX() + Math.sin(i2)*5);
	                    p.playSound(p.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 10, 10);
	                    loc.getWorld().playEffect(flameloc, Effect.EXPLOSION, 51);
	                }
	        		p.sendMessage("§4" + (i + 1) + "");
	        		p.sendMessage("§cX:" + lo.getX());
	        		p.sendMessage("§cY:" + lo.getY());
	        		p.sendMessage("§cZ:" + lo.getZ());
	
	        	}
	        	
	        	} catch(Exception e) {
	        		if(!args[0].equalsIgnoreCase("help"))
	        		p.sendMessage("§c/lootbox help");
	        	}
	        }
	        }



		}
		return true;
	}
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
    	Player p = e.getPlayer();
    if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock().getType() != Material.CHEST) {
    return;
    }
    if(chests.containsKey(e.getClickedBlock())) {
    	p.playSound(e.getClickedBlock().getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 10, 10);
        Location loc = p.getLocation();
        for(int i2 = 0; i2 <360; i2+=5){
            Location flameloc = loc;
            flameloc.setZ(flameloc.getZ() + Math.cos(i2)*5);
            flameloc.setX(flameloc.getX() + Math.sin(i2)*5);
            p.playSound(p.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 20, 20);
            loc.getWorld().playEffect(flameloc, Effect.EXPLOSION, 51);
        }
    	chests.remove(e.getClickedBlock());
    	Location lo = e.getClickedBlock().getLocation();
    	Chest chest = (Chest) e.getClickedBlock().getState();
    	Bukkit.broadcastMessage("§8§l[§c§l!§8§l]§c " + p.getDisplayName() + " §cfound a loot crate!");
    	setType(lo, Material.AIR);
    	setType(lo, Material.CHEST);
    	after.put(e.getClickedBlock(), lo);
    	e.setCancelled(true);
    	looted.put(chest.getBlock(), true);
    } if(after.containsKey(e.getClickedBlock())) {
    	Chest chest = (Chest) e.getClickedBlock().getState();
  //  	playChestAction(chest, true);
    }
    

    
    
    
    
    
    }
    
	private static void setType(Location cursor, Material m) {
		cursor.getBlock().setType(m);
	}
	
	@SuppressWarnings("deprecation")
	private static void setData(Location cursor, byte data) {
		cursor.getBlock().setData(data);
		//world.setBlockData(BukkitUtil.toVector(cursor), data);
	}
	
//    public void playChestAction(Chest chest, boolean open) {
//        Location location = chest.getLocation();
//        World world = ((CraftWorld) location.getWorld()).getHandle();
//        BlockPosition position = new BlockPosition(location.getX(), location.getY(), location.getZ());
//        TileEntityChest tileChest = (TileEntityChest) world.getTileEntity(position);
//        world.playBlockAction(position, tileChest.w(), 1, open ? 1 : 0);
//    }
    
    @SuppressWarnings("deprecation")
	@EventHandler
    public void Interact (PlayerInteractEvent e) {
    	Player p = e.getPlayer();
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock().getType() == Material.CHEST) {
            return;
            }
        if(e.getClickedBlock().getType() != Material.CHEST && p.getItemInHand().getType() != Material.PAPER) {
        	return;
        }
    	List<String> lore = p.getItemInHand().getItemMeta().getLore();
    	String str = ChatColor.stripColor(lore.get(0));
    	String par = str.replace("Amount: $", "");
        if(p.getItemInHand().getType() == Material.PAPER && e.getAction() == Action.RIGHT_CLICK_BLOCK) {
        	if(p.getItemInHand().getItemMeta().hasEnchant(Enchantment.DURABILITY) && e.getItem().getItemMeta().getEnchantLevel(Enchantment.DURABILITY) == 100) {
        	econ.bankDeposit(p.getName(), Double.valueOf(par));
        	int am = p.getInventory().getItemInHand().getAmount() - 1;
        	p.getInventory().getItemInHand().setAmount(am);
        	if(am == 0) {
        		p.getInventory().remove(Material.PAPER);
        	}
        	p.updateInventory();
        	p.sendMessage("§aAdded §2" + Double.valueOf(par) + "§a to your account!");
        	econ.depositPlayer(p.getName(), Double.valueOf(par));
        	} else {
        		return;
        	}
        
    } else {
    	return;
    }
    }
    
    
    
}
