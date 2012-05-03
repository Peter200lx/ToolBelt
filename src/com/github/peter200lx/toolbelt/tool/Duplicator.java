package com.github.peter200lx.toolbelt.tool;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.github.peter200lx.toolbelt.GlobalConf;
import com.github.peter200lx.toolbelt.SetMat;
import com.github.peter200lx.toolbelt.Tool;

public class Duplicator extends Tool {

	public Duplicator(GlobalConf gc) {
		super(gc);
	}

	public static String name = "dupe";

	private HashMap<Material, Material> dupeMap;

	private SetMat keepData = new SetMat(log,gc.modName);

	@Override
	public String getToolName() {
		return name;
	}

	@Override
	@SuppressWarnings("deprecation")	//TODO Investigate replacement .updateInventory()
	public void handleInteract(PlayerInteractEvent event){
		Player subject = event.getPlayer();
		if(!delayElapsed(subject.getName()))
			return;
		if(event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			Block clicked = event.getClickedBlock();
			Material type = clicked.getType();

			if(isDebug()) log.info("["+gc.modName+"][dupeTool] "+subject.getName()+
					" clicked "+clicked.getState().getData());

			Material toUse = dupeMap.get(type);
			if(toUse == null)
				toUse = type;
			if(toUse == Material.AIR) {
				subject.sendMessage(ChatColor.RED + "Duplicating " + ChatColor.GOLD +
						type.toString()+ ChatColor.RED + " is disabled");
				return;
			}

			if((clicked.getData() != 0)&&(keepData.contains(toUse))&& (
					type.equals(toUse) ||
					type.equals(Material.WOOL)&&toUse.equals(Material.INK_SACK)   ||
					type.equals(Material.STEP)&&toUse.equals(Material.DOUBLE_STEP)||
					type.equals(Material.DOUBLE_STEP)&&toUse.equals(Material.STEP)||
					type.equals(Material.LOG)&&toUse.equals(Material.LEAVES) ||
					type.equals(Material.LOG)&&toUse.equals(Material.SAPLING)||
					type.equals(Material.LEAVES)&&toUse.equals(Material.LOG) ||
					type.equals(Material.LEAVES)&&toUse.equals(Material.SAPLING)	)	) {
				subject.getInventory().addItem(new ItemStack(toUse,
						64, (short) 0, clicked.getData()));
			} else {
				subject.getInventory().addItem(new ItemStack(toUse, 64));
			}
			subject.updateInventory();
			if(printData.contains(toUse))
			{
				subject.sendMessage(ChatColor.GREEN + "Enjoy your " + ChatColor.GOLD +
						toUse.toString() + ChatColor.WHITE + ":" +
						ChatColor.BLUE + data2Str(clicked.getState().getData()));
			} else {
				subject.sendMessage(ChatColor.GREEN + "Enjoy your " + ChatColor.GOLD +
							toUse.toString());
			}
		}
	}

	@Override
	public boolean printUse(CommandSender sender) {
		if(hasPerm(sender)) {
			sender.sendMessage("Right-click with the "+ChatColor.GOLD+getType()+
					ChatColor.WHITE+" to duplicate the item selected");
			return true;
		}
		return false;
	}

	@Override
	public boolean loadConf(String tSet, FileConfiguration conf) {

		//Load the repeat delay
		if(!loadRepeatDelay(tSet,conf,0))
			return false;

		ConfigurationSection sect = conf.getConfigurationSection(tSet+"."+name+".replace");

		if(sect == null) {
			log.warning("["+gc.modName+"] "+tSet+"."+name+".replace is returning null");
			return false;
		}

		HashMap<Material, Material> holdDupeMap = defDupeMap();
		for(Entry<String, Object> entry :sect.getValues(false).entrySet()) {
			try {
				int key = Integer.parseInt(entry.getKey());
				if(entry.getValue() instanceof Number) {
					int val = ((Number)entry.getValue()).intValue();
					if((key > 0)&&(val >= 0)) {
						Material keyType = Material.getMaterial(key);
						Material valType = Material.getMaterial(val);
						if((keyType != null)&&(valType != null)) {
							holdDupeMap.put(keyType,valType);
							if(isDebug()) log.info( "["+gc.modName+"][loadConf] added to dupeMap: " +
									keyType + " to " + valType);
							continue;
						}
					}
				}
				log.warning("["+gc.modName+"] "+tSet+"."+name+".replace: '"+entry.getKey()+
						"': '"+entry.getValue() + "' is not a Material type" );
				return false;
			} catch(NumberFormatException e) {
				log.warning("["+gc.modName+"] "+tSet+"."+name+".replace: '"+entry.getKey()+
						"': is not an integer" );
				return false;
			}
		}
		dupeMap = holdDupeMap;

		List<Integer> intL = conf.getIntegerList(tSet+"."+name+".keepData");

		if(!keepData.loadMatList(intL,false,tSet+"."+name+".keepData"))
			return false;

		if(isDebug()) keepData.logMatSet("loadConf",name+".keepData:");

		return true;
	}

	private HashMap<Material, Material> defDupeMap() {
		HashMap<Material, Material> dm = new HashMap<Material, Material>();
		//What about Material.GLOWING_REDSTONE_ORE ? It is safe to place
		//TODO Investigate (Stationary)Water/Lava
		//Material.STATIONARY_LAVA	Material.STATIONARY_WATER
		//Material.LAVA				Material.WATER
		dm.put(Material.BED_BLOCK, Material.BED);
		dm.put(Material.PISTON_EXTENSION, Material.PISTON_BASE);
		dm.put(Material.PISTON_MOVING_PIECE, Material.PISTON_BASE);
		//Material.DOUBLE_STEP This is fine for someone to have
		//Can anyone even click on Material.FIRE ? No
		//Do we want to block Material.MOB_SPAWNER ?
		dm.put(Material.REDSTONE_WIRE, Material.REDSTONE);
		//Do we want to block Material.SOIL ?
		dm.put(Material.SIGN_POST, Material.SIGN);
		dm.put(Material.WOODEN_DOOR, Material.WOOD_DOOR);
		dm.put(Material.WALL_SIGN, Material.SIGN);
		dm.put(Material.IRON_DOOR_BLOCK, Material.IRON_DOOR);
		dm.put(Material.REDSTONE_TORCH_OFF, Material.REDSTONE_TORCH_ON);
		dm.put(Material.SUGAR_CANE_BLOCK, Material.SUGAR_CANE);
		//Do we want to block Material.PORTAL ?
		dm.put(Material.CAKE_BLOCK, Material.CAKE);
		dm.put(Material.DIODE_BLOCK_OFF, Material.DIODE);
		dm.put(Material.DIODE_BLOCK_ON, Material.DIODE);
		dm.put(Material.LOCKED_CHEST, Material.CHEST);
		//Do we want to block Material.NETHER_WARTS ?
		dm.put(Material.BREWING_STAND,Material.BREWING_STAND_ITEM);
		dm.put(Material.CAULDRON,Material.CAULDRON_ITEM);
		//Can anyone even click Material.ENDER_PORTAL ?
		//Do we want to block Material.ENDER_PORTAL_FRAME ?
		return dm;
	}

}