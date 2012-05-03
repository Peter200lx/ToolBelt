package com.github.peter200lx.toolbelt;

import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Button;
import org.bukkit.material.Cake;
import org.bukkit.material.Coal;
import org.bukkit.material.Crops;
import org.bukkit.material.DetectorRail;
import org.bukkit.material.Diode;
import org.bukkit.material.Directional;
import org.bukkit.material.Door;
import org.bukkit.material.Dye;
import org.bukkit.material.Ladder;
import org.bukkit.material.Lever;
import org.bukkit.material.LongGrass;
import org.bukkit.material.MaterialData;
import org.bukkit.material.PistonBaseMaterial;
import org.bukkit.material.PoweredRail;
import org.bukkit.material.PressurePlate;
import org.bukkit.material.Pumpkin;
import org.bukkit.material.Rails;
import org.bukkit.material.RedstoneTorch;
import org.bukkit.material.Sign;
import org.bukkit.material.Step;
import org.bukkit.material.Torch;
import org.bukkit.material.TrapDoor;
import org.bukkit.material.Tree;
import org.bukkit.material.Wool;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class Tool implements ToolInterface {

	public Tool(GlobalConf gc) {
		this.gc = gc;
		onlyAllow = gc.onlyAllow;
		stopCopy = gc.stopCopy;
		stopOverwrite = gc.stopOverwrite;
		setPrintData();
	}

	protected final Logger log = Logger.getLogger("Minecraft");

	protected GlobalConf gc;

	public static String name;

	private Material type;

	private int repeatDelay;

	private HashMap<String, Long> pCooldown = new HashMap<String, Long>();

	protected SetMat onlyAllow;

	protected SetMat stopCopy;

	protected SetMat stopOverwrite;

	protected static HashSet<Material> printData = new HashSet<Material>();

	public Material getType() {
		return type;
	}

	public void setType(Material type) {
		this.type = type;
	}

	//Each tool must implement this so that the tool specific name is returned
	public abstract String getToolName();

	protected boolean isDebug() {
		return gc.debug;
	}

	protected boolean isUseEvent() {
		return gc.useEvent;
	}

	protected String getPermStr() {
		return gc.modName.toLowerCase()+".tool."+getToolName();
	}

	//This catches left/right click events
	public abstract void handleInteract(PlayerInteractEvent event);

	public void handleItemChange(PlayerItemHeldEvent event) {
		//Only some tools will override this, it is the
		//  change selected item bar event catch
	}

	public void handleDamage(EntityDamageEvent event) {
		//Only some tools will override this, it is used
		//  if a tool wants to protect a user from damage.
	}

	public boolean hasPerm(CommandSender sender) {
		if(gc.perm)
			return sender.hasPermission(getPermStr());
		else
			return true;
	}

	//This is for printing use instructions for a player
	public abstract boolean printUse(CommandSender sender);

	//All tools must override this, however they can just return true; if
	//  they have no data to load.
	public abstract boolean loadConf(String tSet, FileConfiguration conf);

	public void saveHelp(JavaPlugin host) {
		if(isDebug()) log.info("["+gc.modName+"] Help saved for: "+getToolName());
		host.saveResource("help/"+getToolName()+".txt", true);
	}

	protected boolean spawnBuild(Block target, Player subject) {
		int spawnSize = gc.server.getSpawnRadius();
		if (subject.isOp())
			return true;
		else if(spawnSize <= 0)
			return true;
		else {
			Location spawn = target.getWorld().getSpawnLocation();
			int distanceFromSpawn = (int) Math.max(
					Math.abs(target.getX() - spawn.getX()),
					Math.abs(target.getZ() - spawn.getZ()));
			return (distanceFromSpawn > spawnSize);
		}
	}

	//This is only needed if breaking a block and not replacing it with a new block
	protected boolean safeBreak(Block target, Player subject, boolean applyPhysics) {
		ItemStack hand = subject.getItemInHand();
		BlockDamageEvent canDamage = new BlockDamageEvent(subject, target, hand, true);
		gc.server.getPluginManager().callEvent(canDamage);
		if(canDamage.isCancelled())
			return false;
		BlockBreakEvent canBreak = new BlockBreakEvent(target,subject);
		gc.server.getPluginManager().callEvent(canBreak);
		if(!canBreak.isCancelled())
			target.setTypeId(0, applyPhysics);
		return !canBreak.isCancelled();
	}

	@SuppressWarnings("deprecation") //Replace ContainerBlock with InventoryHolder once not
	// supporting CB-1.1-R4
	protected boolean safeReplace(MaterialData newInfo, Block old, Player subject, boolean canBuild) {
		BlockState oldInfo = old.getState();
		if(oldInfo.getType().equals(Material.SIGN_POST)         ||
				oldInfo.getType().equals(Material.WALL_SIGN)    ||
				newInfo.getItemType().equals(Material.SIGN_POST)||
				newInfo.getItemType().equals(Material.WALL_SIGN)){
			//LogBlock doesn't catch BlockPlaceEvent's having to do with signs
			subject.sendMessage(ChatColor.RED + "Plugin doesn't support writing or overwriting "+
					ChatColor.GOLD+"Signs");
			return false;
		}else if(oldInfo.getType().equals(newInfo.getItemType()))
			old.setData(newInfo.getData(), false);
		else if(oldInfo instanceof org.bukkit.block.ContainerBlock) {
			subject.sendMessage(ChatColor.RED + "Plugin doesn't support overwriting "+
					ChatColor.GOLD+"Container Blocks");
			return false;
		}else if(oldInfo.getType().equals(Material.SIGN_POST)||
				oldInfo.getType().equals(Material.WALL_SIGN) ){
			subject.sendMessage(ChatColor.RED + "Plugin doesn't support overwriting "+
					ChatColor.GOLD+"Signs");
			return false;
		}else if(oldInfo.getType().equals(Material.NOTE_BLOCK)) {
			subject.sendMessage(ChatColor.RED + "Plugin doesn't support overwriting "+
					ChatColor.GOLD+"NoteBlocks");
			return false;
		}else if(oldInfo.getType().equals(Material.JUKEBOX)) {
			subject.sendMessage(ChatColor.RED + "Plugin doesn't support overwriting "+
					ChatColor.GOLD+"Jukeboxs");
			return false;
		}else if(oldInfo.getType().equals(Material.MOB_SPAWNER)) {
			subject.sendMessage(ChatColor.RED + "Plugin doesn't support overwriting "+
					ChatColor.GOLD+"CreatureSpawners");
			return false;
		}else
			old.setTypeIdAndData(newInfo.getItemTypeId(), newInfo.getData(), false);
		ItemStack type = newInfo.toItemStack();
		BlockPlaceEvent canPlace = new BlockPlaceEvent(old,oldInfo,old,type,subject,canBuild);
		gc.server.getPluginManager().callEvent(canPlace);
		if(canPlace.isCancelled()) {
			if(oldInfo.getType().equals(newInfo.getItemType()))
				old.setData(oldInfo.getRawData(), false);
			else
				old.setTypeIdAndData(oldInfo.getTypeId(), oldInfo.getRawData(), false);
		}
		return !canPlace.isCancelled();
	}

	protected boolean delayElapsed(String name) {
		if(repeatDelay == 0)
			return true; //Don't bother filling pCooldown with data when not used
		if(pCooldown.containsKey(name) &&
				(System.currentTimeMillis() < (pCooldown.get(name)+repeatDelay)))
			return false;
		pCooldown.put(name, System.currentTimeMillis());
		return true;
	}

	protected boolean loadRepeatDelay(String tSet, FileConfiguration conf, int def) {

		int localDelay = conf.getInt(tSet+"."+getToolName()+".repeatDelay", def);

		if(localDelay == -1) {
			//If the local value is -1, we want to grab the global setting
			repeatDelay = gc.repeatDelay;
			if(isDebug()) {
				log.info("["+gc.modName+"][loadConf] Using global tool reuse delay for "+
						getToolName());
			}
		}else if(localDelay < 0) {
			//If we are any negative number that isn't -1
			log.warning("["+gc.modName+"] "+tSet+"."+getToolName()+".repeatDelay has an "+
					"invalid value of "+repeatDelay);
			log.warning("["+gc.modName+"] (The tool specific delay must be -1,0,"+
					" or a positive number)");
			return false;
		}else {
			//We want to go with what the local value is
			repeatDelay = localDelay;
		}
		if(isDebug()) {
			log.info("["+gc.modName+"][loadConf] "+getToolName()+" tool use repeat delay is "+
					repeatDelay);
		}
		return true;
	}

	protected String data2Str(MaterialData b) {
		byte data = b.getData();
		//if(gc.debug) log.info("["+gc.modName+"][data2str] Block "+b.toString());
		switch(b.getItemType()) {
		case LOG:
		case WOOD:
		case LEAVES:
		case SAPLING:
			if(((Tree)b).getSpecies() != null)
				return ((Tree)b).getSpecies().toString();
			else
				return ""+data;
		case JUKEBOX:
			if(data == 0x0)			return "Empty";
			else if(data == 0x1)	return "Record 13";
			else if(data == 0x2)	return "Record cat";
			else if(data == 0x3)	return "Record blocks";
			else if(data == 0x4)	return "Record chrip";
			else if(data == 0x5)	return "Record far";
			else if(data == 0x6)	return "Record mall";
			else if(data == 0x7)	return "Record melloci";
			else if(data == 0x8)	return "Record stal";
			else if(data == 0x9)	return "Record strad";
			else if(data == 0x10)	return "Record ward";
			else					return "Record " + data;
		case CROPS:
			return ((Crops)b).getState().toString();
		case WOOL:
				return ((Wool)b).getColor().toString();
		case INK_SACK:
				return ((Dye)b).toString();
		case TORCH:
			return ((Torch)b).getFacing().toString();
		case REDSTONE_TORCH_OFF:
		case REDSTONE_TORCH_ON:
			return ((RedstoneTorch)b).getFacing().toString();
		case RAILS:
			return ((Rails)b).getDirection() +
				(	((Rails)b).isCurve() ? " on a curve" : (
					((Rails)b).isOnSlope() ? " on a slope" : ""	)	);
		case POWERED_RAIL:
			return ((PoweredRail)b).getDirection() +
					(((PoweredRail)b).isOnSlope() ? " on a slope" : "");
		case DETECTOR_RAIL:
			return ((DetectorRail)b).getDirection() +
					(((DetectorRail)b).isOnSlope() ? " on a slope" : "");
		case WOOD_STAIRS:
		case COBBLESTONE_STAIRS:
		case NETHER_BRICK_STAIRS:
		case BRICK_STAIRS:
		case SMOOTH_STAIRS:
			String append = "";
			if((data&0x4) == 0x4)
				append = " and UPSIDE-DOWN";
			if((data&0x3) == 0x0) {
				return "NORTH"+append;
			} else if((data&0x3) == 0x1) {
				return "SOUTH"+append;
			} else if((data&0x3) == 0x2) {
				return "EAST"+append;
			} else if((data&0x3) == 0x3) {
				return "WEST"+append;
			}
			return "" + data;
		case LEVER:
			return ((Lever)b).getAttachedFace().toString();
		case WOODEN_DOOR:
		case IRON_DOOR_BLOCK:
			if(((Door)b).isTopHalf())
				return "TOP half,"+" hinge "+(((data&0x1)==0x1)?"LEFT":"RIGHT");
			else
				return "BOTTOM half, "+((Door)b).getHingeCorner().toString() + " is " +
					(((Door)b).isOpen()?"OPEN":"CLOSED");
		case STONE_BUTTON:
			return ((Button)b).getAttachedFace().toString();
		case SIGN_POST:
			return ((Sign)b).getFacing().toString();
		case LADDER:
			return ((Ladder)b).getAttachedFace().toString();
		case WALL_SIGN:
			return ((Sign)b).getAttachedFace().toString();
		case FURNACE:
			return ((Directional)b).getFacing().toString();
		case DISPENSER:
			return ((Directional)b).getFacing().toString();
		case PUMPKIN:
		case JACK_O_LANTERN:
			return ((Pumpkin)b).getFacing().toString();
		case STONE_PLATE:
		case WOOD_PLATE:
			return ((PressurePlate)b).isPressed()?" is PRESSED":" is not PRESSED";
		case COAL:
			return ((Coal)b).getType().toString();
		case STEP:
			append = " BOTTOM-HALF";
			if((data&0x8) == 0x8)
				append = " TOP-HALF";
			if((data&0x7) == 0x0) {
				return Material.STONE.toString()+append;
			} else if((data&0x7) == 0x1) {
				return Material.SANDSTONE.toString()+append;
			} else if((data&0x7) == 0x2) {
				return Material.WOOD.toString()+append;
			} else if((data&0x7) == 0x3) {
				return Material.COBBLESTONE.toString()+append;
			} else if((data&0x7) == 0x4) {
				return Material.BRICK.toString()+append;
			} else if((data&0x7) == 0x5) {
				return Material.SMOOTH_BRICK.toString()+append;
			}
			return "" + data;
		case DOUBLE_STEP:
			return ((Step)b).getMaterial().toString();
		case SNOW:
			if(data == 0x0)			return "1/8 HEIGHT";
			else if(data == 0x1)	return "2/8 HEIGHT";
			else if(data == 0x2)	return "3/8 HEIGHT (STEP)";
			else if(data == 0x3)	return "4/8 HEIGHT (STEP)";
			else if(data == 0x4)	return "5/8 HEIGHT (STEP)";
			else if(data == 0x5)	return "6/8 HEIGHT (STEP)";
			else if(data == 0x6)	return "7/8 HEIGHT (STEP)";
			else if(data == 0x7)	return "FULL HEIGHT (STEP)";
			else					return ""+data;
		case CAKE_BLOCK:
			return ""+((Cake)b).getSlicesRemaining()+"/6 REMAINING";
		case DIODE_BLOCK_OFF:
		case DIODE_BLOCK_ON:
			return ((Diode)b).getFacing().toString()+" with DELAY of "+
				((Diode)b).getDelay();
		case LONG_GRASS:
			return ((LongGrass)b).getSpecies().toString();
		case TRAP_DOOR:
			return ((TrapDoor)b).getAttachedFace().toString() + " is " +
									(((TrapDoor)b).isOpen()?"OPEN":"CLOSED");
		case PISTON_BASE:
		case PISTON_STICKY_BASE:
			return ((PistonBaseMaterial)b).getFacing().toString();
		case SANDSTONE:
			if(data == 0x0)			return "CRACKED";
			else if(data == 0x1)	return "GLYPHED";
			else if(data == 0x2)	return "SMOOTH";
			else					return ""+data;
		case SMOOTH_BRICK:
			if(data == 0x0)			return "NORMAL";
			else if(data == 0x1)	return "MOSSY";
			else if(data == 0x2)	return "CRACKED";
			else if(data == 0x3)	return "CIRCLE";
			else					return ""+data;
		case HUGE_MUSHROOM_1:
		case HUGE_MUSHROOM_2:
			if(data == 0x0)			return "FLESHY PIECE";
			else if(data == 0x1)	return "CAP ON TOP & W & N";
			else if(data == 0x2)	return "CAP ON TOP & N";
			else if(data == 0x3)	return "CAP ON TOP & N & E";
			else if(data == 0x4)	return "CAP ON TOP & W";
			else if(data == 0x5)	return "CAP ON TOP";
			else if(data == 0x6)	return "CAP ON TOP & E";
			else if(data == 0x7)	return "CAP ON TOP & S & W";
			else if(data == 0x8)	return "CAP ON TOP & S";
			else if(data == 0x9)	return "CAP ON TOP & E & S";
			else if(data == 0x10)	return "STEM";
			else					return ""+data;
		case VINE:
			String ret = "";
			if((data&0x1) == 0x1) {
				if(ret.length() == 0)	ret += "SOUTH";
				else					ret += " & SOUTH";	}
			if((data&0x2) == 0x2) {
				if(ret.length() == 0)	ret += "WEST";
				else					ret += " & WEST";	}
			if((data&0x4) == 0x4) {
				if(ret.length() == 0)	ret += "NORTH";
				else					ret += " & NORTH";	}
			if((data&0x8) == 0x8) {
				if(ret.length() == 0)	ret += "EAST";
				else					ret += " & EAST";	}
			if(ret.length() == 0)
				ret += "TOP";
			return ret;
		case FENCE_GATE:
			append = " is Closed";
			if((data&0x4) == 0x4)
				append = " is OPEN";
			if((data&0x3) == 0x0) {
				return "SOUTH"+append;
			} else if((data&0x3) == 0x1) {
				return "WEST"+append;
			} else if((data&0x3) == 0x2) {
				return "NORTH"+append;
			} else if((data&0x3) == 0x3) {
				return "EAST"+append;
			}
			return ""+data;
		case MONSTER_EGGS:	//Hidden Silverfish
			if(data == 0x0)			return Material.STONE.toString();
			else if(data == 0x1)	return Material.COBBLESTONE.toString();
			else if(data == 0x2)	return Material.SMOOTH_BRICK.toString();
			else					return ""+data;
		case BREWING_STAND:
			ret = "Bottle in ";
			if((data&0x1) == 0x1) {
			if(ret.length() == 10)	ret += "EAST Slot";
			else					ret += " & EAST Slot";	}
			if((data&0x2) == 0x2) {
			if(ret.length() == 10)	ret += "SOUTH_WEST Slot";
			else					ret += " & SOUTH_WEST Slot";	}
			if((data&0x4) == 0x4) {
				if(ret.length() == 10)	ret += "NORTH_WEST Slot";
				else					ret += " & NORTH_WEST Slot";	}
			if(ret.length() == 10)
				ret = "Empty";
			return ret;
		case CAULDRON:
			if(data == 0x0)			return "EMPTY";
			else if(data == 0x1)	return "1/3 FILLED";
			else if(data == 0x2)	return "2/3 FILLED";
			else if(data == 0x3)	return "FULL";
			else					return ""+data;
		case ENDER_PORTAL_FRAME:
			//TODO Add intelligence here
			return "" + data;
		case EGG:
			//TODO Is there anywhere we can get a mapping of entity id to name?
			return "" + data;
		default:
			return "" + data;
		}
	}

	private static void setPrintData() {
		printData.add(Material.LOG);
		printData.add(Material.WOOD);
		printData.add(Material.LEAVES);
		printData.add(Material.SAPLING);
		printData.add(Material.JUKEBOX);
		printData.add(Material.CROPS);
		printData.add(Material.WOOL);
		printData.add(Material.INK_SACK);
		printData.add(Material.TORCH);
		printData.add(Material.REDSTONE_TORCH_OFF);
		printData.add(Material.REDSTONE_TORCH_ON);
		printData.add(Material.RAILS);
		printData.add(Material.POWERED_RAIL);
		printData.add(Material.DETECTOR_RAIL);
		printData.add(Material.WOOD_STAIRS);
		printData.add(Material.COBBLESTONE_STAIRS);
		printData.add(Material.NETHER_BRICK_STAIRS);
		printData.add(Material.BRICK_STAIRS);
		printData.add(Material.SMOOTH_STAIRS);
		printData.add(Material.LEVER);
		printData.add(Material.WOODEN_DOOR);
		printData.add(Material.IRON_DOOR_BLOCK);
		printData.add(Material.STONE_BUTTON);
		printData.add(Material.SIGN_POST);
		printData.add(Material.LADDER);
		printData.add(Material.WALL_SIGN);
		printData.add(Material.FURNACE);
		printData.add(Material.DISPENSER);
		printData.add(Material.PUMPKIN);
		printData.add(Material.JACK_O_LANTERN);
		printData.add(Material.STONE_PLATE);
		printData.add(Material.WOOD_PLATE);
		printData.add(Material.COAL);
		printData.add(Material.STEP);
		printData.add(Material.DOUBLE_STEP);
		printData.add(Material.SNOW);
		printData.add(Material.CAKE_BLOCK);
		printData.add(Material.DIODE_BLOCK_OFF);
		printData.add(Material.DIODE_BLOCK_ON);
		printData.add(Material.LONG_GRASS);
		printData.add(Material.TRAP_DOOR);
		printData.add(Material.PISTON_BASE);
		printData.add(Material.PISTON_STICKY_BASE);
		printData.add(Material.SANDSTONE);
		printData.add(Material.SMOOTH_BRICK);
		printData.add(Material.HUGE_MUSHROOM_1);
		printData.add(Material.HUGE_MUSHROOM_2);
		printData.add(Material.VINE);
		printData.add(Material.FENCE_GATE);
		printData.add(Material.MONSTER_EGGS);
		printData.add(Material.BREWING_STAND);
		printData.add(Material.CAULDRON);
		printData.add(Material.ENDER_PORTAL_FRAME);
		printData.add(Material.EGG);
	}

}

