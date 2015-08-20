package com.github.peter200lx.toolbelt.tool;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.CropState;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.GrassSpecies;
import org.bukkit.Material;
import org.bukkit.TreeSpecies;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.DetectorRail;
import org.bukkit.material.Door;
import org.bukkit.material.Lever;
import org.bukkit.material.MaterialData;
import org.bukkit.material.PistonBaseMaterial;
import org.bukkit.material.PoweredRail;
import org.bukkit.material.Step;
import org.bukkit.material.TrapDoor;
import org.bukkit.material.TripwireHook;
import org.bukkit.material.WoodenStep;

import com.github.peter200lx.toolbelt.GlobalConf;
import com.github.peter200lx.toolbelt.PrintEnum;
import com.github.peter200lx.toolbelt.AbstractTool;

@SuppressWarnings("deprecation") // org.bukkit.material.Door (no new api yet)
public class Scroll extends AbstractTool {

	public Scroll(GlobalConf gc) {
		super(gc);
	}

	public static final String NAME = "scroll";

	private Map<Material, Integer> dataMap;

	private boolean scrollAllBlocks = false;
	private boolean scrollAllValues = false;

	@Override
	public String getToolName() {
		return NAME;
	}

	@Override
	public void handleInteract(PlayerInteractEvent event) {
		final Player subject = event.getPlayer();
		if (!delayElapsed(subject.getName())) {
			return;
		}

		final Action act = event.getAction();
		if (act.equals(Action.LEFT_CLICK_BLOCK)
				|| (act.equals(Action.RIGHT_CLICK_BLOCK))) {
			final Block clicked = event.getClickedBlock();
			final Material type = clicked.getType();
			if (!dataMap.containsKey(type) && !scrollAllBlocks) {
				uPrint(PrintEnum.DEBUG, subject, "" + ChatColor.GOLD + type
						+ ChatColor.DARK_PURPLE
						+ " is not supported for scrolling");
			} else if (noOverwrite(subject, type)) {
				uPrint(PrintEnum.DEBUG, subject, ChatColor.DARK_PURPLE
						+ "You can't overwrite " + ChatColor.GOLD + type);
			} else {
				if (subject.getGameMode().equals(GameMode.CREATIVE)
						&& act.equals(Action.LEFT_CLICK_BLOCK)
						&& (type.equals(Material.SIGN_POST)
								|| type.equals(Material.WALL_SIGN))) {
					uPrint(PrintEnum.WARN, subject, "The sign is not erased on"
							+ " the server, it is just client side");
				}

				int max;
				if (scrollAllValues) {
					max = 16;
				} else {
					max = dataMap.get(type);
				}
				byte data = clicked.getData();

				if (max != 0) {
					data = simpScroll(act, data, max);
				} else {
					try {
						data = specialCase(subject, act, clicked.getState());
					} catch (UnsupportedOperationException error) {
						if (error.getMessage() != null) {
							uPrint(PrintEnum.DEBUG, subject,
									error.getMessage());
						}
						return;
					}
				}

				MaterialData newInfo = clicked.getState().getData();
				newInfo.setData(data);
				if (spawnBuild(clicked, subject)) {
					if (isUseEvent()) {
						if (safeReplace(newInfo, clicked, subject, true)) {
							this.updateUser(subject, clicked.getLocation(),
									type, data);
							uPrint(PrintEnum.INFO, subject, ChatColor.GREEN
									+ "Block is now " + ChatColor.GOLD + type
									+ ChatColor.WHITE + ":" + ChatColor.BLUE
									+ data2Str(clicked.getState().getData()));
						}
					} else {
						clicked.setData(data, false);
						this.updateUser(subject, clicked.getLocation(), type,
								data);
						uPrint(PrintEnum.INFO, subject, ChatColor.GREEN
								+ "Block is now " + ChatColor.GOLD + type
								+ ChatColor.WHITE + ":" + ChatColor.BLUE
								+ data2Str(clicked.getState().getData()));
					}
				}
			}
		}
	}

	private byte specialCase(Player subject, Action act, BlockState src) {
		final MaterialData b = src.getData();
		byte data = b.getData();
		switch (b.getItemType()) {
		case LOG_2:
			//Map down removing bit 0x2 which is unused currently (MC1.7)
			data = (byte) (((data & 0xC) >>> 1) | (data & 0x1));
			data = (byte) (simpScroll(act, data, 8));
			//Expand up leaving bit 0x2 as 0
			data = (byte) (((data & 0x6) << 1) | (data & 0x1));
			break;
		case LEAVES:
			data = (byte) (simpScroll(act, (byte) (data & 0x3), 4)
					| (data & 0xC));
			break;
		case LEAVES_2:
			data = (byte) (simpScroll(act, (byte) (data & 0x3), 2)
					| (data & 0xC));
			break;
		case JUKEBOX:
			throw new UnsupportedOperationException(ChatColor.DARK_PURPLE
					+ "Data value indicates contained record, can't scroll");
		case SOIL:
			throw new UnsupportedOperationException(ChatColor.DARK_PURPLE
					+ "Data value indicates dampness level, can't scroll");
		case TORCH:
		case REDSTONE_TORCH_OFF:
		case REDSTONE_TORCH_ON:
			data = simpScroll(act, data, 1, 6);
			break;
		case POWERED_RAIL:
			data = simpScroll(act, (byte) (data & 0x07), 6);
			if (((PoweredRail) b).isPowered()) {
				data |= 0x08;
			}
			break;
		case DETECTOR_RAIL:
			data = simpScroll(act, (byte) (data & 0x07), 6);
			if (((DetectorRail) b).isPressed()) {
				data |= 0x08;
			}
			break;
		case ACTIVATOR_RAIL:
			data = simpScroll(act, (byte) (data & 0x07), 6);
			if ((b.getData() & 0x8) == 0x8) {
				data |= 0x08;
			}
			break;
		case LEVER:
			data = simpScroll(act, (byte) (data & 0x07), 0, 8);
			if (((Lever) b).isPowered()) {
				data |= 0x08;
			}
			break;
		case WOODEN_DOOR:
		case IRON_DOOR_BLOCK:
			if (((Door) b).isTopHalf()) {
				uPrint(PrintEnum.INFO, subject, "Clicking the top half of a"
						+ " door can't scroll the rotation corner.");
				throw new UnsupportedOperationException();
			}
			data = simpScroll(act, (byte) (data & 0x07), 4);
			if (((Door) b).isOpen()) {
				data |= 0x04;
			}
			uPrint(PrintEnum.HINT, subject, "Top door half now "
					+ " looks funny, open/close door to fix");
			break;
		case SPRUCE_DOOR:
		case BIRCH_DOOR:
		case JUNGLE_DOOR:
		case ACACIA_DOOR:
		case DARK_OAK_DOOR:
			// Bug for Spigot/Bukkit, these don't have Door.class
			if ((data & 0x8) == 0x8) {
				uPrint(PrintEnum.INFO, subject, "Clicking the top half of a"
						+ " door can't scroll the rotation corner.");
				throw new UnsupportedOperationException();
			}
			boolean is_open = ((data & 0x4) == 0x4);
			data = simpScroll(act, (byte) (data & 0x07), 4);
			if (is_open) {
				data |= 0x04;
			}
			uPrint(PrintEnum.HINT, subject, "Top door half now "
					+ " looks funny, open/close door to fix");
			break;
		case STONE_BUTTON:
		case WOOD_BUTTON:
			data = simpScroll(act, (byte) (data & 0x07), 1, 5);
			break;
		case LADDER:
		case WALL_SIGN:
		case FURNACE:
		case BURNING_FURNACE:
			data = simpScroll(act, (byte) (data & 0x07), 2, 6);
			break;
		case DISPENSER:
		case DROPPER:
		case HOPPER:
			data = simpScroll(act, (byte) (data & 0x07), 0, 6);
			break;
		case CHEST:
		case ENDER_CHEST:
		case TRAPPED_CHEST:
			// CHEST can not be scrolled because of double chests.
			throw new UnsupportedOperationException(""
					+ ChatColor.GOLD + b.getItemType()
					+ ChatColor.DARK_PURPLE + " is not scrollable");
		case STONE_PLATE:
		case WOOD_PLATE:
		case GOLD_PLATE:
		case IRON_PLATE:
			throw new UnsupportedOperationException(ChatColor.DARK_PURPLE
					+ "There is no useful data to scroll");
		case STEP:
			data = handleStep(act, b, 8, 0x7);
			break;
		case WOOD_STEP:
			data = handleStep(act, b, TreeSpecies.values().length, 0x7);
			break;
		case STONE_SLAB2:
			data = handleStep(act, b, 1, 0x7);
			break;
		case DOUBLE_STONE_SLAB2:
			if ((data & 0x8) == 0x8) {
				data &= 0x7;
			} else {
				data |= 0x8;
			}
			break;
		case BED_BLOCK:
			// TODO More research into modifying foot and head of  bed at once
			throw new UnsupportedOperationException(""
					+ ChatColor.GOLD + b.getItemType()
					+ ChatColor.DARK_PURPLE + " is not yet scrollable");
		case DIODE_BLOCK_OFF:
		case DIODE_BLOCK_ON:
			data = simpScroll(act, (byte) (data & 0x03), 4);
			data |= (byte) (b.getData() & 0x0C);
			break;
		case REDSTONE_COMPARATOR_OFF:
		case REDSTONE_COMPARATOR_ON:
			data = simpScroll(act, (byte) (data & 0x03), 4);
			data |= (byte) (b.getData() & 0x0C);
			break;
		case REDSTONE_WIRE:
			throw new UnsupportedOperationException(ChatColor.DARK_PURPLE
					+ "There is no useful data to scroll");
		case TRAP_DOOR:
			Boolean inverted = ((data & 0x08) == 0x08);
			if (act.equals(Action.LEFT_CLICK_BLOCK) && ((data & 0x03) == 0x0)
					|| (act.equals(Action.RIGHT_CLICK_BLOCK)
							&& ((data & 0x03) == 0x03))) {
				inverted = !inverted;
			}
			data = simpScroll(act, (byte) (data & 0x03), 4);
			if (((TrapDoor) b).isOpen()) {
				data |= 0x04;
			}
			if (inverted) {
				data |= 0x08;
			}
			break;
		case PISTON_BASE:
		case PISTON_STICKY_BASE:
			if (((PistonBaseMaterial) b).isPowered()) {
				uPrint(PrintEnum.INFO, subject, "The piston will not be"
						+ " scrolled while extended");
				throw new UnsupportedOperationException();
			}
			data = simpScroll(act, (byte) (data & 0x07), 6);
			break;
		case PISTON_EXTENSION:
			uPrint(PrintEnum.HINT, subject, "The piston extension should "
							+ "not be scrolled");
			throw new UnsupportedOperationException();
		case FENCE_GATE:
		case SPRUCE_FENCE_GATE:
		case BIRCH_FENCE_GATE:
		case JUNGLE_FENCE_GATE:
		case DARK_OAK_FENCE_GATE:
		case ACACIA_FENCE_GATE:
			data = simpScroll(act, (byte) (data & 0x03), 4);
			if ((b.getData() & 0x04) == 0x04) {
				data |= 0x04;
			}
			break;
		case BREWING_STAND:
			throw new UnsupportedOperationException(ChatColor.DARK_PURPLE
					+ "Stand data just is for visual "
					+ "indication of placed glass bottles");
		case TRIPWIRE_HOOK:
			data = simpScroll(act, (byte) (data & 0x03), 4);
			if (((TripwireHook) b).isConnected()) {
				data |= 0x04;
			}
			if (((TripwireHook) b).isActivated()) {
				data |= 0x08;
			}
			break;
		case TRIPWIRE:
			throw new UnsupportedOperationException(ChatColor.DARK_PURPLE
					+ "There is no useful data to scroll");
		case SKULL:
			throw new UnsupportedOperationException(ChatColor.DARK_PURPLE
					+ "Direction partly controlled by Tile Entity, "
					+ "unsupported");
		case ANVIL:
			data = (byte) (simpScroll(act, (byte) (data & 0x01), 2)
					| (data & ~0x01));
			break;
		case HAY_BLOCK:
			data = (byte) (simpScroll(act, (byte) (data >> 2), 4) << 2);
			data = (byte) ((data & 0xC) | (b.getData() & 0x3));
			break;
		case DOUBLE_PLANT:
			if ((data & 0x8) == 0x8) {
				uPrint(PrintEnum.INFO, subject, "Clicking the top half of a"
						+ " tall flower can't change the type.");
				throw new UnsupportedOperationException();
			}
			data = simpScroll(act, data, 6);
			break;
		case WALL_BANNER:
			data = simpScroll(act, (byte) (data & 0x07), 2, 6);
			break;
		default:
			throw new UnsupportedOperationException(""
					+ ChatColor.GOLD + b.getItemType()
					+ ChatColor.DARK_PURPLE + " is not yet scrollable");
		}
		return data;
	}

	private byte handleStep(Action act, MaterialData b, int stepMax,
			int mask) {
		byte data = b.getData();
		boolean inverted;
		if (b instanceof Step) {
			inverted = ((Step) b).isInverted();
		} else if (b instanceof WoodenStep) {
			inverted = ((WoodenStep) b).isInverted();
		} else {
			// The new slabs don't have a class
			inverted = ((data & 0x8) == 0x8);
		}

		data = (byte) (data & mask);
		if (act.equals(Action.LEFT_CLICK_BLOCK)) {
			if (!inverted) {
				if ((data - 1) < 0) {
					data = (byte) (stepMax - 1);
				} else {
					data = (byte) ((data - 1) % stepMax);
				}
			}
			inverted = !inverted;
		} else if (act.equals(Action.RIGHT_CLICK_BLOCK)) {
			if (inverted) {
				data = (byte) ((data + 1) % stepMax);
			}
			inverted = !inverted;
		}
		if (inverted) {
			data |= 0x8;
		} else {
			data &= 0x7;
		}
		return data;
	}

	// Note that min is inclusive and max is exclusive.
	// So to scroll through 1,2,3,4 set min to 1 and max to 5
	private byte simpScroll(Action act, byte data, int min, int max) {
		return (byte) (simpScroll(act, (byte) (data - min), max - min) + min);
	}

	// Note that max is exclusive, to scroll through 0,1,2 set max to 3
	private byte simpScroll(Action act, byte oldData, int max) {
		byte data = oldData;
		if (act.equals(Action.LEFT_CLICK_BLOCK)) {
			if ((data - 1) < 0) {
				data = (byte) (max - 1);
			} else {
				data = (byte) ((data - 1) % max);
			}
		} else if (act.equals(Action.RIGHT_CLICK_BLOCK)) {
			data = (byte) ((data + 1) % max);
		}
		return data;
	}

	@Override
	public boolean printUse(CommandSender sender) {
		if (hasPerm(sender)) {
			uPrint(PrintEnum.CMD, sender, useFormat("Left/Right click to"
					+ " change a block's  data value"));
			return true;
		}
		return false;
	}

	@Override
	public boolean loadConf(String tSet, ConfigurationSection conf) {

		// Load the repeat delay
		if (!loadRepeatDelay(tSet, conf, -1)) {
			return false;
		}

		String scrollAll = conf.getString(tSet + "." + NAME + ".all", "no");
		if (scrollAll.contentEquals("no")) {
			scrollAllValues = false;
			scrollAllBlocks = false;
		} else if (scrollAll.contentEquals("values")) {
			scrollAllValues = true;
			scrollAllBlocks = false;
		} else if (scrollAll.contentEquals("blocks")) {
			scrollAllValues = true;
			scrollAllBlocks = true;
		} else if (scrollAll.contentEquals("false")) {
			log.warning("[" + gc.modName + "][loadConf] " + NAME + ".all is "
					+ "not a recognized value of (no|values|blocks)");
			log.warning("[" + gc.modName + "][loadConf] it was not recognized "
					+ "as a string, did you include the quotes?");
			return false;
		} else {
			log.warning("[" + gc.modName + "][loadConf] " + NAME + ".all is "
					+ "not a recognized value of (no|values|blocks)");
			return false;
		}
		if (isDebug()) {
			log.info("[" + gc.modName + "][loadConf] Scrolling override"
					+ " is set to " + scrollAll);
		}

		// Initialize the dataMap list
		dataMap = defDataMap();

		if (!loadOnlyAllow(tSet, conf)) {
			return false;
		}

		if (!loadStopOverwrite(tSet, conf)) {
			return false;
		}

		return true;
	}

	private Map<Material, Integer> defDataMap() {
		final Map<Material, Integer> dm = new HashMap<Material, Integer>();
		// If the integer is 0, that means that a simple numerical shift won't
		// work
		dm.put(Material.STONE, 7);
		dm.put(Material.DIRT, 3);
		dm.put(Material.SAND, 2);
		dm.put(Material.LOG, 16);
		dm.put(Material.LOG_2, 0);
		dm.put(Material.WOOD, TreeSpecies.values().length);
		dm.put(Material.LEAVES, 0);
		dm.put(Material.LEAVES_2, 0);
		dm.put(Material.JUKEBOX, 0);
		dm.put(Material.SAPLING, TreeSpecies.values().length);
		dm.put(Material.CACTUS, 16);
		dm.put(Material.SUGAR_CANE_BLOCK, 16);
		// TODO Add Water and Lava? Likely not, ... What if tool id is a bucket
		dm.put(Material.SOIL, 0);
		dm.put(Material.CROPS, CropState.values().length);
		dm.put(Material.NETHER_WARTS, 4);
		dm.put(Material.PUMPKIN_STEM, 8);
		dm.put(Material.MELON_STEM, 8);
		dm.put(Material.WOOL, DyeColor.values().length);
		// Add Dyes? No block to click
		dm.put(Material.TORCH, 0);
		dm.put(Material.REDSTONE_TORCH_OFF, 0);
		dm.put(Material.REDSTONE_TORCH_ON, 0);
		dm.put(Material.RAILS, 10);
		dm.put(Material.POWERED_RAIL, 0);
		dm.put(Material.DETECTOR_RAIL, 0);
		dm.put(Material.ACTIVATOR_RAIL, 0);
		dm.put(Material.WOOD_STAIRS, 8);
		dm.put(Material.COBBLESTONE_STAIRS, 8);
		dm.put(Material.BRICK_STAIRS, 8);
		dm.put(Material.SMOOTH_STAIRS, 8);
		dm.put(Material.NETHER_BRICK_STAIRS, 8);
		dm.put(Material.SPRUCE_WOOD_STAIRS, 8);
		dm.put(Material.BIRCH_WOOD_STAIRS, 8);
		dm.put(Material.JUNGLE_WOOD_STAIRS, 8);
		dm.put(Material.SANDSTONE_STAIRS, 8);
		dm.put(Material.QUARTZ_STAIRS, 8);
		dm.put(Material.ACACIA_STAIRS, 8);
		dm.put(Material.DARK_OAK_STAIRS, 8);
		dm.put(Material.RED_SANDSTONE_STAIRS, 8);
		dm.put(Material.LEVER, 0);
		dm.put(Material.WOODEN_DOOR, 0);
		dm.put(Material.IRON_DOOR_BLOCK, 0);
		dm.put(Material.SPRUCE_DOOR, 0);
		dm.put(Material.BIRCH_DOOR, 0);
		dm.put(Material.JUNGLE_DOOR, 0);
		dm.put(Material.ACACIA_DOOR, 0);
		dm.put(Material.DARK_OAK_DOOR, 0);
		dm.put(Material.STONE_BUTTON, 0);
		dm.put(Material.SIGN_POST, 16);
		dm.put(Material.LADDER, 0);
		dm.put(Material.WALL_SIGN, 0);
		dm.put(Material.FURNACE, 0);
		dm.put(Material.BURNING_FURNACE, 0);
		dm.put(Material.DISPENSER, 0);
		dm.put(Material.DROPPER, 0);
		dm.put(Material.HOPPER, 0);
		dm.put(Material.CHEST, 0);
		dm.put(Material.ENDER_CHEST, 0);
		dm.put(Material.TRAPPED_CHEST, 0);
		dm.put(Material.PUMPKIN, 4);
		dm.put(Material.JACK_O_LANTERN, 4);
		dm.put(Material.STONE_PLATE, 0);
		dm.put(Material.WOOD_PLATE, 0);
		dm.put(Material.GOLD_PLATE, 0);
		dm.put(Material.IRON_PLATE, 0);
		// Add Coal? No block to click
		// Add Tools & Armor? No block to click
		dm.put(Material.STEP, 0);
		dm.put(Material.DOUBLE_STEP, 10);
		dm.put(Material.WOOD_STEP, 0);
		dm.put(Material.WOOD_DOUBLE_STEP, TreeSpecies.values().length);
		dm.put(Material.SNOW, 8);
		dm.put(Material.CAKE_BLOCK, 6);
		dm.put(Material.BED_BLOCK, 0);
		dm.put(Material.DIODE_BLOCK_OFF, 0);
		dm.put(Material.DIODE_BLOCK_ON, 0);
		dm.put(Material.REDSTONE_COMPARATOR_OFF, 0);
		dm.put(Material.REDSTONE_COMPARATOR_ON, 0);
		dm.put(Material.REDSTONE_WIRE, 0);
		dm.put(Material.LONG_GRASS, GrassSpecies.values().length);
		dm.put(Material.TRAP_DOOR, 0);
		dm.put(Material.PISTON_BASE, 0);
		dm.put(Material.PISTON_STICKY_BASE, 0);
		dm.put(Material.PISTON_EXTENSION, 0);
		dm.put(Material.SANDSTONE, 3);
		dm.put(Material.SMOOTH_BRICK, 4);
		dm.put(Material.HUGE_MUSHROOM_1, 11);
		dm.put(Material.HUGE_MUSHROOM_2, 11);
		dm.put(Material.VINE, 16);
		dm.put(Material.FENCE_GATE, 0);
		dm.put(Material.SPRUCE_FENCE_GATE, 0);
		dm.put(Material.BIRCH_FENCE_GATE, 0);
		dm.put(Material.JUNGLE_FENCE_GATE, 0);
		dm.put(Material.DARK_OAK_FENCE_GATE, 0);
		dm.put(Material.ACACIA_FENCE_GATE, 0);
		dm.put(Material.COCOA, 12);
		// Add Potions? No block to click
		dm.put(Material.MONSTER_EGGS, 6);
		dm.put(Material.BREWING_STAND, 0);
		dm.put(Material.CAULDRON, 4);
		dm.put(Material.ENDER_PORTAL_FRAME, 4);
		// Add EGG? No block to click
		dm.put(Material.TRIPWIRE_HOOK, 0);
		dm.put(Material.TRIPWIRE, 0);
		dm.put(Material.COBBLE_WALL, 2);
		dm.put(Material.CARROT, 8);
		dm.put(Material.POTATO, 8);
		dm.put(Material.WOOD_BUTTON, 0);
		dm.put(Material.SKULL, 0);
		dm.put(Material.ANVIL, 0);
		dm.put(Material.QUARTZ_BLOCK, 5);
		dm.put(Material.STAINED_CLAY, DyeColor.values().length);
		dm.put(Material.HAY_BLOCK, 0);
		dm.put(Material.CARPET, DyeColor.values().length);
		dm.put(Material.STAINED_GLASS, DyeColor.values().length);
		dm.put(Material.STAINED_GLASS_PANE, DyeColor.values().length);
		dm.put(Material.RED_ROSE, 9);
		dm.put(Material.DOUBLE_PLANT, 0);
		dm.put(Material.SPONGE, 2);
		dm.put(Material.PRISMARINE, 3);
		dm.put(Material.HARD_CLAY, DyeColor.values().length);
		dm.put(Material.STANDING_BANNER, 16);
		dm.put(Material.WALL_BANNER, 0);
		dm.put(Material.RED_SANDSTONE, 3);
		dm.put(Material.DOUBLE_STONE_SLAB2, 0);
		dm.put(Material.STONE_SLAB2, 0);
		return dm;
	}

}
