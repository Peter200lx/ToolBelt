package com.github.peter200lx.toolbelt.tool;

import java.util.HashMap;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.CropState;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.GrassSpecies;
import org.bukkit.Material;
import org.bukkit.TreeSpecies;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.DetectorRail;
import org.bukkit.material.Door;
import org.bukkit.material.Lever;
import org.bukkit.material.MaterialData;
import org.bukkit.material.PistonBaseMaterial;
import org.bukkit.material.PoweredRail;
import org.bukkit.material.TrapDoor;

import com.github.peter200lx.toolbelt.Tool;

public class Scroll extends Tool {

	public Scroll(String modName, boolean debug, boolean permissions) {
		super(modName, debug, permissions);
	}

	public static String name = "scroll";

	public static HashMap<Material, Integer> dataMap;

	@Override
	public void handleInteract(PlayerInteractEvent event){
		Action act = event.getAction();
		if(act.equals(Action.LEFT_CLICK_BLOCK)||(act.equals(Action.RIGHT_CLICK_BLOCK))) {
			if(dataMap.containsKey(event.getClickedBlock().getType())) {
				Block clicked = event.getClickedBlock();
				Player p = event.getPlayer();
				if(isDebug()) log.info("["+modName+"][scrollTool] "+p.getName()+
						" clicked "+clicked.getState().getData());
				if(p.getGameMode().equals(GameMode.CREATIVE)		&&
						act.equals(Action.LEFT_CLICK_BLOCK)			&&
						clicked.getType().equals(Material.SIGN_POST)||
						clicked.getType().equals(Material.WALL_SIGN)){
						p.sendMessage("The sign is not erased on the server, "+
								"it is just client side");
				}

				int max = dataMap.get(clicked.getType());
				byte data = clicked.getData();

				if(max != 0) {
					data = simpScroll(event, data, max);
				} else {
					MaterialData b = clicked.getState().getData();
					switch (clicked.getType()) {
					case JUKEBOX:
						p.sendMessage("Data value indicates contained record, can't scroll");
						return;
					case SOIL:
						p.sendMessage("Data value indicates dampness level, can't scroll");
						return;
					case TORCH:
					case REDSTONE_TORCH_OFF:
					case REDSTONE_TORCH_ON:
						data = simpScroll(event, data, 1, 6);
						break;
					case POWERED_RAIL:
						data = simpScroll(event, (byte)(data&0x07), 6);
						if(((PoweredRail)b).isPowered())
							data |= 0x08;
						break;
					case DETECTOR_RAIL:
						data = simpScroll(event, (byte)(data&0x07), 6);
						if(((DetectorRail)b).isPressed())
							data |= 0x08;
						break;
					case LEVER:
						data = simpScroll(event,(byte)(data&0x07), 1,7);
						if(((Lever)b).isPowered())
							data |= 0x08;
						break;
					case WOODEN_DOOR:
					case IRON_DOOR_BLOCK:
						if(((Door)b).isTopHalf()) {
							p.sendMessage("Clicking the top half of a door "+
									"can't scroll the rotation corner.");
							return;
						}
						data = simpScroll(event,(byte)(data&0x07),4);
						if(((Door)b).isOpen())
							data |= 0x04;
						p.sendMessage("Top door half now looks funny, open/close door to fix");
						break;
					case STONE_BUTTON:
						data = simpScroll(event, (byte)(data&0x07), 1, 5);
						break;
					case LADDER:
					case WALL_SIGN:
					case FURNACE:
					case DISPENSER:
						data = simpScroll(event, (byte)(data&0x07), 2, 6);
						break;
					case CHEST:
						//It doesn't look like CHEST can be safely scrolled because of double chests.
						p.sendMessage(clicked.getType()+" is not scrollable");
						return;
					case STONE_PLATE:
					case WOOD_PLATE:
						p.sendMessage("There is no useful data to scroll");
						return;
					case BED_BLOCK:
						//TODO More research into modifying foot and head of bed at once
						p.sendMessage(clicked.getType()+" is not yet scrollable");
						return;
					case DIODE_BLOCK_OFF:
					case DIODE_BLOCK_ON:
						byte tick = (byte)(data & (0x08 | 0x04));
						data = simpScroll(event,(byte)(data&0x03),4);
						data |= tick;
						break;
					case REDSTONE_WIRE:
						p.sendMessage("There is no useful data to scroll");
						return;
					case TRAP_DOOR:
						data = simpScroll(event, (byte)(data&0x03), 4);
						if(((TrapDoor)b).isOpen())
							data |= 0x04;
						break;
					case PISTON_BASE:
					case PISTON_STICKY_BASE:
						if(((PistonBaseMaterial)b).isPowered()) {
							p.sendMessage("The piston will not be scrolled while extended");
							return;
						}
						data = simpScroll(event, (byte)(data&0x07), 6);
						break;
					case PISTON_EXTENSION:
						p.sendMessage("The piston extension should not be scrolled");
						return;
					case FENCE_GATE:
						data = simpScroll(event, (byte)(data&0x03), 4);
						if((b.getData()&0x04)==0x04)	//Is the gate open?
							data |= 0x04;
						break;
					case BREWING_STAND:
						p.sendMessage("Stand data just is for visual indication of placed glass bottles");
						return;
					default:
						p.sendMessage(clicked.getType()+" is not yet scrollable");
						return;
					}
				}

				clicked.setData(data, false);

				event.getPlayer().sendMessage(ChatColor.GREEN + "Block is now " + ChatColor.GOLD +
						clicked.getType() + ChatColor.WHITE + ":" +
						ChatColor.BLUE + data2Str(clicked.getState().getData()));
			}
		}
	}

	//Note that min is inclusive and max is exclusive.
	// So to scroll through 1,2,3,4 set min to 1 and max to 5
	private byte simpScroll(PlayerInteractEvent event, byte data, int min, int max) {
		return (byte) (simpScroll(event,(byte) (data-min),max-min) + min);
	}

	//Note that max is exclusive, to scroll through 0,1,2 set max to 3
	private byte simpScroll(PlayerInteractEvent event, byte data, int max) {
		if(event.getAction().equals(Action.LEFT_CLICK_BLOCK)){
			if ((data - 1) < 0)
				data = (byte) (max - 1);
			else
				data = (byte) ((data - 1) % max);
			if(event.getPlayer().getGameMode().equals(GameMode.SURVIVAL))
				event.getPlayer().sendMessage("Data value scrolled, you might "+
					"not see the change");
		} else if(event.getAction().equals(Action.RIGHT_CLICK_BLOCK)){
			data = (byte) ((data + 1) % max);
		}
		return data;
	}

	@Override
	public boolean printUse(CommandSender sender) {
		if(hasPerm(sender)) {
			sender.sendMessage("Click with the "+getType()+
					" to change a block's data value");
			return true;
		}
		return false;
	}

	@Override
	public boolean loadConf(String tSet, FileConfiguration conf) {
		HashMap<Material, Integer> supported = defDataMap();
		if(conf.getBoolean(tSet+"."+name+".override",false)) {
			HashMap<Material, Integer> holdDataMap = new HashMap<Material, Integer>();
			List<Integer> intL = conf.getIntegerList(tSet+"."+name+".allow");

			if(intL == null) {
				log.warning("["+modName+"] "+tSet+"."+name+".allow is returning null");
				return false;
			}

			for(Integer entry : intL) {
				if(entry > 0) {
					Material type = Material.getMaterial(entry);
					if(type != null) {
						if(supported.containsKey(type)) {
							holdDataMap.put(type, supported.get(type));
							if(isDebug()) log.info( "["+modName+"][loadConf] "+name+" allow: "+type);
						} else {
							log.warning("["+modName+"] "+tSet+"."+name+".allow: '" + entry +
										"' is not supported for scrolling" );
							return false;
						}
						continue;
					}
				}
				log.warning("["+modName+"] "+tSet+"."+name+".allow: '" + entry +
						"' is not a Material type" );
				return false;
			}
			dataMap = holdDataMap;
		} else {
			if(isDebug()) log.info( "["+modName+"][loadConf] "+name+" loadout set to all plugin supported materials");
			dataMap = supported;
		}
		return true;
	}

	private HashMap<Material, Integer> defDataMap() {
		HashMap<Material, Integer> dm = new HashMap<Material, Integer>();
		//If the integer is 0, that means that a simple numerical shift won't work
		dm.put(Material.LOG, TreeSpecies.values().length);
		dm.put(Material.LEAVES, 4);
		dm.put(Material.JUKEBOX, 0);
		dm.put(Material.SAPLING, TreeSpecies.values().length);
		dm.put(Material.CACTUS, 16);
		dm.put(Material.SUGAR_CANE_BLOCK, 16);
		//TODO Add Water and Lava? Likely not, ... What if tool id is a bucket
		dm.put(Material.SOIL, 0);
		dm.put(Material.CROPS, CropState.values().length);
		dm.put(Material.NETHER_WARTS, 4);
		dm.put(Material.PUMPKIN_STEM, 8);
		dm.put(Material.MELON_STEM, 8);
		dm.put(Material.WOOL, DyeColor.values().length);
		//Add Dyes? No block to click
		dm.put(Material.TORCH, 0);
		dm.put(Material.REDSTONE_TORCH_OFF, 0);
		dm.put(Material.REDSTONE_TORCH_ON, 0);
		dm.put(Material.RAILS, 10);
		dm.put(Material.POWERED_RAIL, 0);
		dm.put(Material.DETECTOR_RAIL, 0);
		dm.put(Material.WOOD_STAIRS, 4);
		dm.put(Material.COBBLESTONE_STAIRS, 4);
		dm.put(Material.BRICK_STAIRS, 4);
		dm.put(Material.SMOOTH_STAIRS, 4);
		dm.put(Material.NETHER_BRICK_STAIRS, 4);
		dm.put(Material.LEVER, 0);
		dm.put(Material.WOODEN_DOOR, 0);
		dm.put(Material.IRON_DOOR_BLOCK, 0);
		dm.put(Material.STONE_BUTTON, 0);
		dm.put(Material.SIGN_POST, 16);
		dm.put(Material.LADDER, 0);
		dm.put(Material.WALL_SIGN, 0);
		dm.put(Material.FURNACE, 0);
		dm.put(Material.DISPENSER, 0);
		dm.put(Material.CHEST, 0);
		dm.put(Material.PUMPKIN, 4);
		dm.put(Material.JACK_O_LANTERN, 4);
		dm.put(Material.STONE_PLATE, 0);
		dm.put(Material.WOOD_PLATE, 0);
		//Add Coal? No block to click
		//Add Tools & Armor? No block to click
		dm.put(Material.STEP, 7);
		dm.put(Material.DOUBLE_STEP, 7);
		dm.put(Material.SNOW, 8);
		dm.put(Material.CAKE_BLOCK, 6);
		dm.put(Material.BED_BLOCK, 0);
		dm.put(Material.DIODE_BLOCK_OFF, 0);
		dm.put(Material.DIODE_BLOCK_ON, 0);
		dm.put(Material.REDSTONE_WIRE, 0);
		dm.put(Material.LONG_GRASS, GrassSpecies.values().length);
		dm.put(Material.TRAP_DOOR, 0);
		dm.put(Material.PISTON_BASE, 0);
		dm.put(Material.PISTON_STICKY_BASE, 0);
		dm.put(Material.PISTON_EXTENSION, 0);
		dm.put(Material.SMOOTH_BRICK, 3);
		dm.put(Material.HUGE_MUSHROOM_1, 11);
		dm.put(Material.HUGE_MUSHROOM_2, 11);
		dm.put(Material.VINE, 16);
		dm.put(Material.FENCE_GATE, 0);
		//Add Potions? No block to click
		dm.put(Material.MONSTER_EGGS, 3);
		dm.put(Material.BREWING_STAND, 0);
		dm.put(Material.CAULDRON, 4);
		dm.put(Material.ENDER_PORTAL_FRAME, 4);
		//Add EGG? No block to click
		return dm;
	}

}
