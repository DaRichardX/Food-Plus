package me.RichardX.FoodPlus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class Main extends JavaPlugin implements Listener {
	List<ItemStack> Foodlist = new ArrayList<>();

	List<String> Foodname = new ArrayList<>();

	List<String> Foodlore = new ArrayList<>();

	List<Integer> Foodhunger = new ArrayList<>();

	Map<String, List<Location>> FoodLocations = new HashMap<>();

	Plugin plugin;

	FileConfiguration config = null;

	ItemStack Strawberry;

	void print(Object s) {
		System.out.println(s);
	}

	public String cc(String string) {
		return ChatColor.translateAlternateColorCodes('&', string);
	}

	public ItemStack makeHead(String name, String lore, String head) {
		ItemStack citem = SkullCreator.itemFromBase64(head);
		SkullMeta skullMeta = (SkullMeta) citem.getItemMeta();
		skullMeta.setDisplayName(ChatColor.RESET + name);
		ArrayList<String> slore = new ArrayList<>();
		slore.add(ChatColor.GOLD + lore);
		skullMeta.setLore(slore);
		citem.setItemMeta((ItemMeta) skullMeta);
		return citem;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Player player = (Player) sender;
		if (cmd.getName().equalsIgnoreCase("food"))
			if (args.length > 0) {
				if (player.hasPermission("placeablebread.food")) {
					int i = 0;
					for (String food : this.Foodname) {
						if (args[0].equalsIgnoreCase(food.replace(" ", "_"))) {
							player.sendMessage(ChatColor.GOLD + "You have recieved " + food);
							player.getInventory().addItem(new ItemStack[] { this.Foodlist.get(i) });
							return true;
						}
						i++;
					}
					player.sendMessage(ChatColor.DARK_RED + "That is not a food");
				}
			} else {
				player.sendMessage(ChatColor.DARK_RED + "Usage: /food (foodname)");
			}
		if (cmd.getName().equalsIgnoreCase("foodreload")) {
			reloadConfig();
			this.config = getConfig();
			loadConfig();
			player.sendMessage(ChatColor.GOLD + "Config reloaded");
		}
		return false;
	}

	public void onDisable() {
		this.config.set("placedfoods", this.FoodLocations);
		saveConfig();
	}

	@SuppressWarnings("unchecked")
	public void onEnable() {
		saveDefaultConfig();
		this.plugin = (Plugin) this;
		this.config = getConfig();
		loadConfig();
		Set<String> placedfoods = this.config.getConfigurationSection("placedfoods").getKeys(false);
		for (String s : placedfoods) {
			String food = "placedfoods." + s;
			List<Location> foodlocation = (List<Location>) this.config.getList(food);
			this.FoodLocations.put(s, foodlocation);
		}
		getServer().getPluginManager().registerEvents(this, (Plugin) this);
	}

	@SuppressWarnings("deprecation")
	public void loadConfig() {
		int count = 0;
		ItemStack sstrawberry = makeHead("Strawberry", "Juicy!", this.config.getString("Strawberry-playername"));
		this.Strawberry = sstrawberry;
		this.Foodlist.add(sstrawberry);
		this.Foodlore.add("Juicy!");
		this.Foodname.add("Strawberry");
		this.Foodhunger.add(Integer.valueOf(this.config.getInt("Strawberry-hunger")));
		Set<String> foods = this.config.getConfigurationSection("foods").getKeys(false);
		for (String s : foods) {
			String food = "foods." + s;
			ItemStack cfood = makeHead(this.config.getString(String.valueOf(food) + ".foodname"),
					this.config.getString(String.valueOf(food) + ".foodlore"),
					this.config.getString(String.valueOf(food) + ".playername"));
			System.out.println(count + "FoodPlus");
			ShapedRecipe craftfood = new ShapedRecipe(new NamespacedKey(plugin, count + "FoodPlus"), cfood);
			count++;
			List<String> crafting = this.config.getStringList(String.valueOf(food) + ".crafting");
			for (String ingredient : crafting)
				ingredient.replaceAll("%", " ");
			craftfood.shape(new String[] { crafting.get(0), crafting.get(1), crafting.get(2) });
			Set<String> ingredients = this.config.getConfigurationSection(String.valueOf(food) + ".ingredients")
					.getKeys(false);
			for (String ingredient : ingredients) {
				char c = ingredient.charAt(0);
				String[] parts = this.config.getString(String.valueOf(food) + ".ingredients." + ingredient).split(":");
				String material = parts[0];
				int data = 0;
				if (parts.length > 1)
					data = Integer.parseInt(parts[1]);
				craftfood.setIngredient(c, Material.getMaterial(material), data);
			}
			Bukkit.getServer().addRecipe((Recipe) craftfood);
			this.Foodlore.add(this.config.getString(String.valueOf(food) + ".foodlore"));
			this.Foodname.add(this.config.getString(String.valueOf(food) + ".foodname"));
			this.Foodhunger.add(Integer.valueOf(this.config.getInt(String.valueOf(food) + ".foodhunger")));
			this.Foodlist.add(cfood);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		Player player = event.getPlayer();
		if (player.getGameMode().equals(GameMode.SURVIVAL) && !event.isCancelled()) {
			Random rand = new Random();
			int randomnumber = rand.nextInt(getConfig().getInt("Strawberry-chance"));
			if (block.getType() == Material.TALL_GRASS && randomnumber == 0 && getConfig().getBoolean("Strawberries"))
				block.getLocation().getWorld().dropItemNaturally(block.getLocation(), this.Strawberry);
			for (Map.Entry<String, List<Location>> entrys : this.FoodLocations.entrySet()) {
				for (Location loc : entrys.getValue()) {
					if (block.getLocation().equals(loc)) {
						String s = entrys.getKey();
						int i = this.Foodname.indexOf(s);
						block.getWorld().dropItemNaturally(block.getLocation(), this.Foodlist.get(i));
						removeFood(block, this.Foodname.get(i));
					}
				}
			}
		}
	}

	private void removeFood(final Block block, final String s) {
		block.setType(Material.AIR);
		(new BukkitRunnable() {
			public void run() {
				List<Location> List = Main.this.FoodLocations.get(s);
				List.remove(block.getLocation());
				Main.this.FoodLocations.put(s, List);
			}
		}).runTaskLater(this.plugin, 1L);
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		Block block = event.getClickedBlock();
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK && player.getFoodLevel() < 20)
			for (Map.Entry<String, List<Location>> entrys : this.FoodLocations.entrySet()) {
				for (Location loc : entrys.getValue()) {
					if (block.getLocation().equals(loc)) {
						String s = entrys.getKey();
						int i = this.Foodname.indexOf(s);
						removeFood(block, s);
						player.playSound(block.getLocation(), Sound.ENTITY_GENERIC_EAT, 3.0F, 0.0F);
						player.setFoodLevel(player.getFoodLevel() + this.Foodhunger.get(i));
						block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, 179); 
						event.setCancelled(true);
						if (player.getFoodLevel() > 20)
							player.setFoodLevel(20);
					}
				}
			}
	}

	public void onExplode(EntityExplodeEvent e) {
		for (Block block : e.blockList()) {
			for (Map.Entry<String, List<Location>> entrys : this.FoodLocations.entrySet()) {
				for (Location loc : entrys.getValue()) {
					if (block.getLocation().equals(loc)) {
						String s = entrys.getKey();
						int i = this.Foodname.indexOf(s);
						removeFood(block, this.Foodname.get(i));
						block.getLocation().getWorld().dropItemNaturally(block.getLocation(), this.Foodlist.get(i));
					}
				}
			}
		}
	}

	@SuppressWarnings("unused")
	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		Block block = event.getBlockPlaced();
		ItemStack item = event.getItemInHand();
		if (block.getType() == Material.PLAYER_HEAD && item.getItemMeta().getLore() != null) {
			int i = 0;
			for (ItemStack food : this.Foodlist) {
				if (item.getItemMeta().getLore().contains(ChatColor.GOLD + (String) this.Foodlore.get(i))) {
					List<Location> list = this.FoodLocations.get(this.Foodname.get(i));
					if (list == null)
						list = new ArrayList<>();
					list.add(block.getLocation());
					this.FoodLocations.put(this.Foodname.get(i), list);
				}
				i++;
			}
		}
	}
}
