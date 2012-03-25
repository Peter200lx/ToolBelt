package com.github.peter200lx.toolbelt;

import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
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
import org.bukkit.material.Stairs;
import org.bukkit.material.Step;
import org.bukkit.material.Torch;
import org.bukkit.material.TrapDoor;
import org.bukkit.material.Tree;
import org.bukkit.material.Wool;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class Tool implements ToolInterface {

	protected Tool(String modName, boolean debug, boolean permissions) {
		this.modName = modName;
		this.debug = debug;
		this.permissions = permissions;
		setPrintData();
	}

	protected final Logger log = Logger.getLogger("Minecraft");

	protected String modName;

	public static String name;

	private Material type;

	private boolean debug;

	private boolean permissions;

	protected HashSet<Material> onlyAllow;

	protected HashSet<Material> stopCopy;

	protected HashSet<Material> stopOverwrite;

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
		return debug;
	}

	protected boolean isPermissions() {
		return permissions;
	}

	protected String getPermStr() {
		return modName.toLowerCase()+".tool."+getToolName();
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
		if(isPermissions())
			return sender.hasPermission(getPermStr());
		else
			return true;
	}

	//This is for printing use instructions for a player
	public abstract boolean printUse(CommandSender sender);

	public boolean loadConf(String tSet, FileConfiguration conf) {
		//All tools should override this IF they have
		//  configuration options they need to grab.
		return false;
	}

	public void saveHelp(JavaPlugin host) {
		if(isDebug()) log.info("["+modName+"] Help saved for: "+getToolName());
		host.saveResource("help/"+getToolName()+".txt", true);
	}

	protected boolean loadGlobalRestrictions(String tSet, FileConfiguration conf) {
		String globalName = "global";

		List<Integer> intL = conf.getIntegerList(tSet+"."+globalName+".onlyAllow");

		onlyAllow = loadMatList(intL,new HashSet<Material>(),tSet+"."+
				globalName+".onlyAllow");
		if(onlyAllow == null)
			return false;

		if(isDebug()) {
			logMatSet(onlyAllow,"loadGlobalRestrictions",globalName+".onlyAllow:");
			if(onlyAllow.isEmpty())
				log.info( "["+modName+"][loadGlobalRestrictions] As onlyAllow"+
						" is empty, all non-restricted materials are allowed");
			else
				log.info( "["+modName+"][loadGlobalRestrictions] As onlyAllow "+
						"has items, only those materials can be painted");
		}

		intL = conf.getIntegerList(tSet+"."+globalName+".stopCopy");

		stopCopy = loadMatList(intL,defStop(),tSet+"."+globalName+".stopCopy");
		if(stopCopy == null)
			return false;

		if(isDebug()) logMatSet(stopCopy,"loadGlobalRestrictions",globalName+
				".stopCopy:");

		intL = conf.getIntegerList(tSet+"."+globalName+".stopOverwrite");

		stopOverwrite = loadMatList(intL,defStop(),tSet+"."+globalName+
				".stopOverwrite");
		if(stopOverwrite == null)
			return false;

		if(isDebug()) logMatSet(stopOverwrite,"loadGlobalRestrictions",
				globalName+".stopOverwrite:");

		return true;
	}

	protected HashSet<Material> loadMatList(List<Integer> input,
			HashSet<Material> def, String warnMessage) {
		if(input == null) {
			log.warning("["+modName+"] "+warnMessage+" is returning null");
			return null;
		}else if(def == null) {
			log.warning("["+modName+"]*** Warn tool developer that their call"+
					" to loadMatList() is bad "+warnMessage);
			return null;
		}
		for(Integer entry : input) {
			if(entry > 0) {
				Material type = Material.getMaterial(entry);
				if(type != null) {
					def.add(type);
					continue;
				}
			}
			log.warning("["+modName+"] "+warnMessage + ": '" + entry +
					"' is not a Material type" );
			return null;
		}
		return def;
	}

	protected void logMatSet(HashSet<Material> set, String function, String summary) {
		for(Material mat: set) {
			log.info("["+modName+"]["+function+"] "+summary+" "+mat.toString());
		}
	}

	protected HashSet<Material> defStop() {
		HashSet<Material> stop = new HashSet<Material>();
		stop.add(Material.AIR);
		stop.add(Material.BED_BLOCK);
		stop.add(Material.PISTON_EXTENSION);
		stop.add(Material.PISTON_MOVING_PIECE);
		stop.add(Material.FIRE);
		stop.add(Material.WOODEN_DOOR);
		stop.add(Material.IRON_DOOR_BLOCK);
		return stop;
	}

	protected String data2Str(MaterialData b) {
		byte data = b.getData();
		//if(isDebug()) log.info("["+modName+"][data2str] Block "+b.toString());
		switch(b.getItemType()) {
		case LOG:
			if(((Tree)b).getSpecies() != null)
				return ((Tree)b).getSpecies().toString();
			else
				return ""+data;
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
			return ((Stairs)b).getFacing().toString();
		case NETHER_BRICK_STAIRS:
		case BRICK_STAIRS:
		case SMOOTH_STAIRS:
			if((data&0x3) == 0x0) {
				return "NORTH";
			} else if((data&0x3) == 0x1) {
				return "SOUTH";
			} else if((data&0x3) == 0x2) {
				return "EAST";
			} else if((data&0x3) == 0x3) {
				return "WEST";
			}
			return "" + data;
		case LEVER:
			return ((Lever)b).getAttachedFace().toString();
		case WOODEN_DOOR:
		case IRON_DOOR_BLOCK:
			return ((Door)b).getHingeCorner().toString() + " is " +
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
		case SMOOTH_BRICK:
			if(data == 0x0)			return "NORMAL";
			else if(data == 0x1)	return "MOSSY";
			else if(data == 0x2)	return "CRACKED";
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
			String append = " is Closed";
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
		case MONSTER_EGGS:
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

