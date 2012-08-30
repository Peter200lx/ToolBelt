package com.github.peter200lx.toolbelt;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Button;
import org.bukkit.material.Cake;
import org.bukkit.material.Coal;
import org.bukkit.material.CocoaPlant;
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
import org.bukkit.material.TripwireHook;
import org.bukkit.material.WoodenStep;
import org.bukkit.material.Wool;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author peter200lx
 *
 * Base class which holds a lot of shared code between tools. Tools should
 *     extend this and add the particular functionally they want on top.
 */
public abstract class AbstractTool implements ToolInterface {

	/**
	 * Initialize a tool by loading in the shared settings.
	 *
	 * @param gc global (shared) settings between all tools.
	 */
	public AbstractTool(GlobalConf gc) {
		this.gc = gc;
		onlyAllow = gc.onlyAllow.copy();
		stopCopy = gc.stopCopy.copy();
		stopOverwrite = gc.stopOverwrite.copy();
		setPrintData();
	}

	/**
	 * Reference to Minecraft logger for printing to the console.
	 */
	protected final Logger log = Logger.getLogger("Minecraft");

	/**
	 * Storage of global (shared) settings between all tools.
	 */
	protected GlobalConf gc;

	// String NAME was defined here, but it must be in the implementation.
	// Defining it here makes no sense.

	/**
	 * Material that this tool is bound to. This material will be prevented from
	 *     interaction with the world around it, and as such it should be chosen
	 *     carefully to prevent blocking basic functionality.
	 */
	private Material type;

	/**
	 * Delay between how often a given user can use this tool. This is in milli-
	 *     seconds, and is used to prevent multiple actions on a single click.
	 *     If this is set to zero, then all repeat protection is removed.
	 */
	private int repeatDelay;

	/**
	 * Storage for how recently a given user has used this tool, used to
	 *     limit performing multiple actions for a single click. If the
	 *     repeatDelay value non-zero.
	 */
	private final Map<String, Long> pCooldown = new HashMap<String, Long>();

	/**
	 * Whitelist of what material types are allowed to be changed/placed.
	 */
	protected SetMat onlyAllow;

	/**
	 * Blacklist of what material types can't be placed.
	 */
	protected SetMat stopCopy;

	/**
	 * Blacklist of what material types can't be removed/changed.
	 */
	protected SetMat stopOverwrite;

	/**
	 * Collection of what material types have data values to print to user.
	 */
	protected static Set<Material> printData = new HashSet<Material>();

	/**
	 * Tells caller the Material this tool maps to.
	 *
	 * @return Material this tool overloads
	 */
	public Material getType() {
		return type;
	}

	/**
	 * Sets the Material that this tool overloads in-game.
	 *     This should only ever be set in ToolBelt.loadConf()
	 *
	 * @param newType Material this tool should overload
	 */
	public void setType(Material newType) {
		this.type = newType;
	}

	/**
	 * Tells the caller the tool name. This should be used instead of calling
	 *      "name" directly. Each tool must implement this, so that the tool
	 *      specific name is in-scope.
	 *
	 * @return Name of to tool.
	 */
	public abstract String getToolName();

	/**
	 * Check for whether console debugging is enabled.
	 *
	 * @return true if debug mode is enable, false otherwise.
	 */
	protected boolean isDebug() {
		return gc.debug;
	}

	/**
	 * Check for whether safeBreak/safeReplace should be used, or if blocks
	 *     should be modified without protection.
	 *
	 * @return true if safe* functions should be used, false otherwise
	 */
	protected boolean isUseEvent() {
		return gc.useEvent;
	}

	/**
	 * Combine the plugin name, ".tool." and the tool name to give the
	 *     permission string for the tool.
	 *
	 * @return permission string representing use of this tool
	 */
	protected String getPermStr() {
		return gc.modName.toLowerCase() + ".tool." + getToolName();
	}

	/**
	 * This catches left/right click events. This function is where most of the
	 *     magic happens with tools.
	 *
	 * @param event Bukkit PlayerInteractEvent to handle
	 */
	public abstract void handleInteract(PlayerInteractEvent event);

	/**
	 * This catches the event when a player changes the item in their hand. Only
	 *     some tools will override this, most will just leave this as a no-op.
	 *
	 * @param event Bukkit PlayerItemHeldEvent to handle
	 */
	public void handleItemChange(PlayerItemHeldEvent event) {
	}

	/**
	 * This catches when a player receives damage. Only some tools will
	 *     override this, most will just leave this as a no-op.
	 * As we have to know the player is holding the correct tool, this
	 *     should only be called after verifying that the Entity in question
	 *     is a Player.
	 *
	 * @param event Bukkit EntityDamageEvent to handle
	 */
	public void handleDamage(EntityDamageEvent event) {
	}

	/**
	 * Return true if "sender" has permission to use this tool.
	 *
	 * @param sender Person to check if they have permission
	 * @return true if they can use tool, false otherwise
	 */
	public boolean hasPerm(CommandSender sender) {
		if (gc.perm) {
			return sender.hasPermission(getPermStr());
		} else {
			return true;
		}
	}

	/**
	 * This is for printing use instructions for a player.
	 *
	 * @param sender Person to display instructions for
	 * @return true if anything is printed to the user, false otherwise.
	 */
	public abstract boolean printUse(CommandSender sender);

	/**
	 * Load the tool specific configuration settings. All tools must implement
	 *     this, even if they don't have any settings to load. If that is the
	 *     case, just return true;
	 *
	 * @param tSet name of the first level of the config.yml "tools"
	 * @param conf configuration object representing config.yml
	 * @return true if no errors processing conf, false if errors occur
	 */
	public abstract boolean loadConf(String tSet, ConfigurationSection conf);

	/**
	 * If the tool has its own area in config.yml, then this is
	 *     what will set up the help file for configuration.
	 *
	 * @param host reference to plugin object, used to access saveResource()
	 */
	public void saveHelp(JavaPlugin host) {
		if (isDebug()) {
			log.info("[" + gc.modName + "] Help saved for: " + getToolName());
		}
		host.saveResource("help/" + getToolName() + ".txt", true);
	}

	/**
	 * Personal version of .sendMessage() supporting multiple levels of print
	 *     messages. This allows an admin to configure a default level of print
	 *     spam to users, and if permissions are being used, a per-user level of
	 *     print spam.
	 *
	 * @param pri verbosity level this message is aimed at
	 * @param subject tool user to send message to (if the priority is correct)
	 * @param message string to stuff into .sendMessage(message)
	 */
	protected void uPrint(PrintEnum pri, CommandSender subject,
			String message) {
		if (gc.perm) {
			PrintEnum usrLvl = null;
			int count = 0;
			for (PrintEnum level : PrintEnum.values()) {
				if (subject.hasPermission(gc.modName.toLowerCase() + ".print."
						+ level.getPermName())) {
					count++;
					usrLvl = level;
				}
			}
			if (count > 1) {
				log.warning("[" + gc.modName + "] " + subject.getName()
						+ " has multiple print perms, using toolbelt.print."
						+ usrLvl.getPermName());
			}
			if (usrLvl != null) {
				if (usrLvl.shouldPrint(pri)) {
					subject.sendMessage(message);
				}
			} else if (gc.pl.shouldPrint(pri)) {
				subject.sendMessage(message);
			}
		} else if (gc.pl.shouldPrint(pri)) {
			subject.sendMessage(message);
		}
	}

	/**
	 * Check to make sure block is outside of spawn protection, or the tool
	 *     user is an op.
	 *
	 * @param target block that is being checked (to get location)
	 * @param subject tool user attempting to change block
	 * @return true if user can change block, false otherwise
	 */
	protected boolean spawnBuild(Block target, Player subject) {
		final int spawnSize = gc.server.getSpawnRadius();
		if (subject.isOp()) {
			return true;
		} else if (spawnSize <= 0) {
			return true;
		} else {
			final Location spawn = target.getWorld().getSpawnLocation();
			final int distanceFromSpawn = (int) Math.max(
					Math.abs(target.getX() - spawn.getX()),
					Math.abs(target.getZ() - spawn.getZ()));
			if (distanceFromSpawn > spawnSize) {
				return true;
			} else {
				uPrint(PrintEnum.DEBUG, subject, ChatColor.DARK_PURPLE
						+ "You can't build inside spawn protection");
				return false;
			}
		}
	}

	/**
	 * Delete a block in the world while respecting all build rights and region
	 *     protection settings. This function calls both BlockDamageEvent and
	 *     BlockBreakEvent in order to discover if the block in question can be
	 *     removed. If the event is canceled by any other plugin, the block
	 *     remains untouched.
	 * The BlockDamageEvent is thrown first as the VoxelGuest region protection
	 *     plugin does not catch the BlockBreakEvent that this function throws.
	 *     Thus to provide compatibility with as many plugins as possible, both
	 *     are thrown.
	 * This function is used instead of safeReplace() if breaking a block and
	 *     not replacing it with a new block.
	 *
	 * @param target block to remove (break)
	 * @param subject tool user attempting to remove the block
	 * @param applyPhysics set false to cancel physics check on block removal
	 * @return true if block has been removed, false otherwise
	 */
	protected boolean safeBreak(Block target, Player subject,
			boolean applyPhysics) {
		final ItemStack hand = subject.getItemInHand();
		final BlockDamageEvent canDamage = new BlockDamageEvent(subject, target,
				hand, true);
		gc.server.getPluginManager().callEvent(canDamage);
		if (canDamage.isCancelled()) {
			uPrint(PrintEnum.DEBUG, subject, ChatColor.DARK_PURPLE
					+ "You can't damage blocks here");
			return false;
		}
		BlockBreakEvent canBreak = new BlockBreakEvent(target, subject);
		gc.server.getPluginManager().callEvent(canBreak);
		if (!canBreak.isCancelled()) {
			target.setTypeId(0, applyPhysics);
		}
		if (!canBreak.isCancelled()) {
			return true;
		} else {
			uPrint(PrintEnum.DEBUG, subject, ChatColor.DARK_PURPLE
					+ "You can't break blocks here");
			return false;
		}
	}

	/**
	 * Change a block in the world while respecting all build rights and region
	 *     protection settings. This function throws a BlockPlaceEvent which all
	 *     protection plugins should be listening for. If any other plugin
	 *     cancels the event, then we know that the user shouldn't be able to
	 *     change the block in question. In that case we revert so nothing has
	 *     changed.
	 * There is an issue with blocks that interact with the minecraft map meta-
	 *     data storage. Because the block is changed for BlockPlaceEvent, the
	 *     metadata can be lost when reverting the block if the event is
	 *     canceled. Because of this, blocks with metadata are tested for and
	 *     the tools are prevented from changing the blocks in question.
	 *     Hopefully at some point a way around this issue will be discovered
	 *     and the tool restrictions will be removed.
	 *
	 * @param newInfo type and data for new block to place
	 * @param old object for block we wish to replace
	 * @param subject tool user attempting to replace the block
	 * @param canBuild passed into BlockPlaceEvent, always true?
	 * @return true of block has been replaced, false otherwise
	 */
	protected boolean safeReplace(MaterialData newInfo, Block old,
			Player subject, boolean canBuild) {
		BlockState oldInfo = old.getState();
		if (oldInfo.getType().equals(Material.SIGN_POST)
				|| oldInfo.getType().equals(Material.WALL_SIGN)
				|| newInfo.getItemType().equals(Material.SIGN_POST)
				|| newInfo.getItemType().equals(Material.WALL_SIGN)) {
			// LogBlock doesn't catch BlockPlaceEvent's having to do with signs
			uPrint(PrintEnum.DEBUG, subject, ChatColor.DARK_PURPLE
					+ "Plugin doesn't support writing or overwriting "
					+ ChatColor.GOLD + "Signs");
			return false;
		} else if (oldInfo.getType().equals(newInfo.getItemType())) {
			old.setData(newInfo.getData(), false);
		} else if (oldInfo instanceof InventoryHolder) {
			uPrint(PrintEnum.DEBUG, subject, ChatColor.DARK_PURPLE
					+ "Plugin doesn't support overwriting "
					+ ChatColor.GOLD + "Container Blocks");
			return false;
		} else if (oldInfo.getType().equals(Material.SIGN_POST)
				|| oldInfo.getType().equals(Material.WALL_SIGN)) {
			uPrint(PrintEnum.DEBUG, subject, ChatColor.DARK_PURPLE
					+ "Plugin doesn't support overwriting "
					+ ChatColor.GOLD + "Signs");
			return false;
		} else if (oldInfo.getType().equals(Material.NOTE_BLOCK)) {
			uPrint(PrintEnum.DEBUG, subject, ChatColor.DARK_PURPLE
					+ "Plugin doesn't support overwriting "
					+ ChatColor.GOLD + "NoteBlocks");
			return false;
		} else if (oldInfo.getType().equals(Material.JUKEBOX)) {
			uPrint(PrintEnum.DEBUG, subject, ChatColor.DARK_PURPLE
					+ "Plugin doesn't support overwriting "
					+ ChatColor.GOLD + "Jukeboxs");
			return false;
		} else if (oldInfo.getType().equals(Material.MOB_SPAWNER)) {
			uPrint(PrintEnum.DEBUG, subject, ChatColor.DARK_PURPLE
					+ "Plugin doesn't support overwriting "
					+ ChatColor.GOLD + "CreatureSpawners");
			return false;
		} else {
			old.setTypeIdAndData(newInfo.getItemTypeId(), newInfo.getData(),
					false);
		}
		ItemStack newType = newInfo.toItemStack();
		BlockPlaceEvent canPlace = new BlockPlaceEvent(old, oldInfo, old,
				newType, subject, canBuild);
		gc.server.getPluginManager().callEvent(canPlace);
		if (canPlace.isCancelled()) {
			if (oldInfo.getType().equals(newInfo.getItemType())) {
				old.setData(oldInfo.getRawData(), false);
			} else {
				old.setTypeIdAndData(oldInfo.getTypeId(), oldInfo.getRawData(),
						false);
			}
		}
		if (!canPlace.isCancelled()) {
			return true;
		} else {
			uPrint(PrintEnum.DEBUG, subject, ChatColor.DARK_PURPLE
					+ "You can't place/replace blocks here");
			return false;
		}
	}

	/**
	 * Test to see if a block type can be placed into the world by a Player.
	 *     The test is done when a ToolBelt tool is loading a material type
	 *     for placing with a tool. Before it tries, it checks to see if the
	 *     admin has prevented that block type from being changed.
	 *
	 * @param subject tool user to test permissions for (if Ranks are used)
	 * @param toTest block type to test for loading into the tool for placing
	 * @return true if block type is prevented from being loaded/placed
	 */
	protected boolean noCopy(Player subject, Material toTest) {
		List<String> ranks = gc.ranks.getUserRank(subject);
		if (ranks != null) {
			uPrint(PrintEnum.DEBUG, subject, ChatColor.DARK_PURPLE
					+ "Your ranks are: " + ChatColor.GOLD + ranks);
		}
		return noCopy(ranks, toTest);
	}

	/**
	 * Test to see if a block type can be placed into the world.
	 *     The test is done when a ToolBelt tool is loading a material type
	 *     for placing with a tool. Before it tries, it checks to see if the
	 *     admin has prevented that block type from being changed.
	 * The list of rank names is for if the admin has configured multiple ranks
	 *     on the server that have differing restrictions as to which material
	 *     types they can alter.
	 *
	 * @param subRanks a list of what rank names to check (or null if no ranks)
	 * @param toTest block type to test for loading into the tool for placing
	 * @return true if block type is prevented from being loaded/placed
	 */
	protected boolean noCopy(List<String> subRanks, Material toTest) {
		return stopCopy.contains(subRanks, toTest)
				|| !(onlyAllow.isEmpty(subRanks)
				|| onlyAllow.contains(subRanks, toTest));
	}

	/**
	 * Test to see if a block in the world can be overwritten by the Player.
	 *     The test is done when a ToolBelt tool would be placing a block.
	 *     Before it tries, it checks to see if the admin has prevented that
	 *     block from being changed.
	 *
	 * @param subject tool user to test permissions for (if Ranks are used)
	 * @param toTest block type currently in the world that would be overwritten
	 * @return true if block type is prevented from being overwritten
	 */
	protected boolean noOverwrite(Player subject, Material toTest) {
		List<String> ranks = gc.ranks.getUserRank(subject);
		if (ranks != null) {
			uPrint(PrintEnum.DEBUG, subject, ChatColor.DARK_PURPLE
					+ "Your ranks are: " + ChatColor.GOLD + ranks);
		}
		return noOverwrite(ranks, toTest);
	}

	/**
	 * Test to see if a block in the world can be overwritten.
	 *     The test is done when a ToolBelt tool would be placing a block.
	 *     Before it tries, it checks to see if the admin has prevented that
	 *     block from being changed.
	 * The list of rank names is for if the admin has configured multiple ranks
	 *     on the server that have differing restrictions as to which material
	 *     types they can alter.
	 *
	 * @param subRanks a list of what rank names to check (or null if no ranks)
	 * @param toTest block type currently in the world that would be overwritten
	 * @return true if block type is prevented from being overwritten
	 */
	protected boolean noOverwrite(List<String> subRanks, Material toTest) {
		return stopOverwrite.contains(subRanks, toTest)
				|| !(onlyAllow.isEmpty(subRanks)
				|| onlyAllow.contains(subRanks, toTest));
	}

	/**
	 * Check to determine if the user has waited longer then repeatDelay since
	 *     the last use. This is used to prevent a tool from effecting several
	 *     blocks at once, such as when clicking on grass and such.
	 *
	 * @param userName user to check time difference for
	 * @return true if last check > then repeatDelay, false otherwise
	 */
	protected boolean delayElapsed(String userName) {
		if (repeatDelay == 0) {
			return true; // Don't fill pCooldown with data when not used
		}
		if (pCooldown.containsKey(userName) && (System.currentTimeMillis()
						< (pCooldown.get(userName) + repeatDelay))) {
			return false;
		}
		pCooldown.put(userName, System.currentTimeMillis());
		return true;
	}

	/**
	 * Update the tool user (and those in the immediate area) with the block
	 *     that just changed.
	 *
	 * @param subject tool user
	 * @param loc location of changed block
	 * @param info new information for the changed block
	 */
	protected void updateUser(Player subject, Location loc,
			MaterialData info) {
		updateUser(subject, loc, info.getItemTypeId(), info.getData());
	}

	/**
	 * Update the tool user (and those in the immediate area) with the block
	 *     that just changed.
	 *
	 * @param subject tool user
	 * @param loc location of changed block
	 * @param newType new material type for the changed block
	 * @param data new data value for the changed block
	 */
	protected void updateUser(Player subject, Location loc, Material newType,
			byte data) {
		updateUser(subject, loc, newType.getId(), data);
	}

	/**
	 * Update the tool user (and those in the immediate area) with the block
	 *     that just changed.
	 *
	 * @param subject tool user
	 * @param loc location of changed block
	 * @param newType new int ID of material type for the changed block
	 * @param data new data value for the changed block
	 */
	protected void updateUser(Player subject, Location loc, int newType,
			byte data) {
		int horizon = (gc.server.getViewDistance() + 1) * 16;
		int horSqr = horizon * horizon;
		Player[] online = gc.server.getOnlinePlayers();
		for (Player other : online) {
			if (loc.getWorld().equals(other.getWorld())
					&& loc.distanceSquared(other.getLocation()) < horSqr) {
				other.sendBlockChange(loc, newType, data);
			}
		}
		subject.sendBlockChange(loc, newType, data);
	}

	/**
	 * Load tools repeatDelay from config.yml . If tool's value is -1,
	 *     then revert to using the global value.
	 *
	 * @param tSet name of base section in config.yml (tools)
	 * @param conf representation of config.yml
	 * @param def value to use if repeatDelay not specified in config.yml
	 * @return true if no errors occurred, false otherwise
	 */
	protected boolean loadRepeatDelay(String tSet, ConfigurationSection conf,
			int def) {

		int localDelay = conf.getInt(tSet + "." + getToolName()
				+ ".repeatDelay", def);

		if (localDelay == -1) {
			// If the local value is -1, we want to grab the global setting
			repeatDelay = gc.repeatDelay;
			if (isDebug()) {
				log.info("[" + gc.modName + "][loadConf] Using global tool"
						+ " reuse delay for " + getToolName());
			}
		} else if (localDelay < 0) {
			// If we are any negative number that isn't -1
			log.warning("[" + gc.modName + "] " + tSet + "." + getToolName()
					+ ".repeatDelay has an invalid value of " + repeatDelay);
			log.warning("[" + gc.modName + "] (The tool specific delay must be"
					+ " -1,0, or a positive number)");
			return false;
		} else {
			// We want to go with what the local value is
			repeatDelay = localDelay;
		}
		if (isDebug()) {
			log.info("[" + gc.modName + "][loadConf] " + getToolName()
					+ " tool use repeat delay is " + repeatDelay);
		}
		return true;
	}

	/**
	 * Load tools onlyAllow list from config.yml .
	 *
	 * @param tSet name of base section in config.yml (tools)
	 * @param conf representation of config.yml
	 * @return true if no errors occurred, false otherwise
	 */
	protected boolean loadOnlyAllow(String tSet, ConfigurationSection conf) {
		List<Integer> intL = conf.getIntegerList(tSet + "." + getToolName()
				+ ".onlyAllow");

		if (!intL.isEmpty()) {
			if (isDebug()) {
				log.info("[" + gc.modName + "][loadConf] As " + getToolName()
						+ ".onlyAllow has items, it overwrites the global");
			}

			if (!onlyAllow.loadMatList(intL, false,
					tSet + "." + getToolName())) {
				return false;
			}

			if (isDebug()) {
				onlyAllow.logMatSet("loadConf", getToolName());
				log.info("[" + gc.modName + "][loadConf] As " + getToolName()
						+ ".onlyAllow  has items, only those materials are"
						+ " usable");
			}
		} else if (isDebug() && !onlyAllow.isEmpty()) {
			log.info("[" + gc.modName + "][loadConf] As global.onlyAllow has"
					+ " items, only those materials are usable");
		}

		String rankName = "ranks";
		ConfigurationSection rankConf = conf.getConfigurationSection(
				tSet + "." + getToolName() + "." + rankName);

		if (!onlyAllow.loadRankedMatLists(rankConf, gc.ranks,
				getToolName() + "." + rankName)) {
			return false;
		}
		if (gc.debug) {
			onlyAllow.logRankedMatSet("loadConf",
					getToolName() + "." + rankName);
		}

		return true;
	}

	/**
	 * Load tools stopCopy list from config.yml .
	 *
	 * @param tSet name of base section in config.yml (tools)
	 * @param conf representation of config.yml
	 * @return true if no errors occurred, false otherwise
	 */
	protected boolean loadStopCopy(String tSet, ConfigurationSection conf) {
		List<Integer> intL = conf.getIntegerList(tSet + "." + getToolName()
				+ ".stopCopy");

		if (!intL.isEmpty()) {
			if (isDebug()) {
				log.info("[" + gc.modName + "][loadConf] As " + getToolName()
						+ ".stopCopy has items, it overwrites the global");
			}

			if (!stopCopy.loadMatList(intL, true, tSet + "." + getToolName())) {
				return false;
			}

			if (isDebug()) {
				stopCopy.logMatSet("loadConf", getToolName());
			}
		}

		String rankName = "ranks";
		ConfigurationSection rankConf = conf.getConfigurationSection(
				tSet + "." + getToolName() + "." + rankName);

		if (!stopCopy.loadRankedMatLists(rankConf, gc.ranks,
				getToolName() + "." + rankName)) {
			return false;
		}
		if (gc.debug) {
			stopCopy.logRankedMatSet("loadConf",
					getToolName() + "." + rankName);
		}

		return true;
	}

	/**
	 * Load tools stopOverwrite list from config.yml .
	 *
	 * @param tSet name of base section in config.yml (tools)
	 * @param conf representation of config.yml
	 * @return true if no errors occurred, false otherwise
	 */
	protected boolean loadStopOverwrite(String tSet,
			ConfigurationSection conf) {
		List<Integer> intL = conf.getIntegerList(tSet + "." + getToolName()
				+ ".stopOverwrite");

		if (!intL.isEmpty()) {
			if (isDebug()) {
				log.info("[" + gc.modName + "][loadConf] As " + getToolName()
						+ ".stopOverwrite has items, it overwrites the global");
			}

			if (!stopOverwrite.loadMatList(intL, true, tSet + "."
					+ getToolName())) {
				return false;
			}

			if (isDebug()) {
				stopOverwrite.logMatSet("loadConf", getToolName());
			}
		}

		String rankName = "ranks";
		ConfigurationSection rankConf = conf.getConfigurationSection(
				tSet + "." + getToolName() + "." + rankName);

		if (!stopOverwrite.loadRankedMatLists(rankConf, gc.ranks,
				getToolName() + "." + rankName)) {
			return false;
		}
		if (gc.debug) {
			stopOverwrite.logRankedMatSet("loadConf",
					getToolName() + "." + rankName);
		}

		return true;
	}

	/**
	 * Convert Material's data value to user-friendly String.
	 *
	 * @param b object containing a block's Material type and data value
	 * @return user-friendly String representing blocks data value
	 */
	protected String data2Str(MaterialData b) {
		byte data = b.getData();
		switch (b.getItemType()) {
		case LOG:
			String species = "";
			if (((Tree) b).getSpecies() != null) {
				species = ((Tree) b).getSpecies().toString();
			}
			if ((data & 0x0C) == 0x0) {
				return species + " is Vertical";
			} else if ((data & 0x0C) == 0x4) {
				return species + " is East-West";
			} else if ((data & 0x0C) == 0x8) {
				return species + " is North-South";
			} else {
				return species + " is Directionless";
			}
		case WOOD:
		case LEAVES:
		case SAPLING:
			if (((Tree) b).getSpecies() != null) {
				return ((Tree) b).getSpecies().toString();
			} else {
				return "" + data;
			}
		case JUKEBOX:
			if (data == 0x0) {
				return "Empty";
			} else if (data == 0x1) {
				return "Record 13";
			} else if (data == 0x2) {
				return "Record cat";
			} else if (data == 0x3) {
				return "Record blocks";
			} else if (data == 0x4) {
				return "Record chrip";
			} else if (data == 0x5) {
				return "Record far";
			} else if (data == 0x6) {
				return "Record mall";
			} else if (data == 0x7) {
				return "Record melloci";
			} else if (data == 0x8) {
				return "Record stal";
			} else if (data == 0x9) {
				return "Record strad";
			} else if (data == 0x10) {
				return "Record ward";
			} else {
				return "Record " + data;
			}
		case CROPS:
			return ((Crops) b).getState().toString();
		case WOOL:
			return ((Wool) b).getColor().toString();
		case INK_SACK:
			return ((Dye) b).toString();
		case TORCH:
			return ((Torch) b).getFacing().toString();
		case REDSTONE_TORCH_OFF:
		case REDSTONE_TORCH_ON:
			return ((RedstoneTorch) b).getFacing().toString();
		case RAILS:
			return ((Rails) b).getDirection()
					+ (((Rails) b).isCurve() ? " on a curve" : (((Rails) b)
							.isOnSlope() ? " on a slope" : ""));
		case POWERED_RAIL:
			return ((PoweredRail) b).getDirection()
					+ (((PoweredRail) b).isOnSlope() ? " on a slope" : "");
		case DETECTOR_RAIL:
			return ((DetectorRail) b).getDirection()
					+ (((DetectorRail) b).isOnSlope() ? " on a slope" : "");
		case WOOD_STAIRS:
		case COBBLESTONE_STAIRS:
		case NETHER_BRICK_STAIRS:
		case BRICK_STAIRS:
		case SMOOTH_STAIRS:
		case SPRUCE_WOOD_STAIRS:
		case BIRCH_WOOD_STAIRS:
		case JUNGLE_WOOD_STAIRS:
		case SANDSTONE_STAIRS:
			String append = "";
			if ((data & 0x4) == 0x4) {
				append = " and UPSIDE-DOWN";
			}
			if ((data & 0x3) == 0x0) {
				return "NORTH" + append;
			} else if ((data & 0x3) == 0x1) {
				return "SOUTH" + append;
			} else if ((data & 0x3) == 0x2) {
				return "EAST" + append;
			} else if ((data & 0x3) == 0x3) {
				return "WEST" + append;
			}
			return "" + data;
		case LEVER:
			if (((Lever) b).getAttachedFace().equals(BlockFace.DOWN)) {
				if ((data & 0x07) == 0x5) {
					return "FLOOR EAST-WEST";
				} else {
					return "FLOOR NORTH-SOUTH";
				}
			} else if (((Lever) b).getAttachedFace().equals(BlockFace.UP)) {
				if ((data & 0x07) == 0x7) {
					return "CEILING EAST-WEST";
				} else {
					return "CEILING NORTH-SOUTH";
				}
			} else {
				return ((Lever) b).getAttachedFace().toString();
			}
		case WOODEN_DOOR:
		case IRON_DOOR_BLOCK:
			if (((Door) b).isTopHalf()) {
				return "TOP half," + " hinge "
						+ (((data & 0x1) == 0x1) ? "LEFT" : "RIGHT");
			} else {
				return "BOTTOM half, " + ((Door) b).getHingeCorner().toString()
						+ " is " + (((Door) b).isOpen() ? "OPEN" : "CLOSED");
			}
		case STONE_BUTTON:
			return ((Button) b).getAttachedFace().toString();
		case SIGN_POST:
			return ((Sign) b).getFacing().toString();
		case LADDER:
			return ((Ladder) b).getAttachedFace().toString();
		case WALL_SIGN:
			return ((Sign) b).getAttachedFace().toString();
		case FURNACE:
			return ((Directional) b).getFacing().toString();
		case DISPENSER:
			return ((Directional) b).getFacing().toString();
		case PUMPKIN:
		case JACK_O_LANTERN:
			return ((Pumpkin) b).getFacing().toString();
		case STONE_PLATE:
		case WOOD_PLATE:
			return ((PressurePlate) b).isPressed() ? " is PRESSED"
					: " is not PRESSED";
		case COAL:
			return ((Coal) b).getType().toString();
		case STEP:
			append = " BOTTOM-HALF";
			if ((data & 0x8) == 0x8) {
				append = " TOP-HALF";
			}
			if ((data & 0x7) != 0x6) {
				return ((Step) b).getMaterial().toString() + append;
			} else {
				return "" + data;
			}
		case DOUBLE_STEP:
			return ((Step) b).getMaterial().toString();
		case WOOD_STEP:
			append = " BOTTOM-HALF";
			if ((data & 0x8) == 0x8) {
				append = " TOP-HALF";
			}
			return ((WoodenStep) b).getSpecies().toString() + append;
		case WOOD_DOUBLE_STEP:
			return ((WoodenStep) b).getSpecies().toString();
		case SNOW:
			if (data == 0x0) {
				return "1/8 HEIGHT";
			} else if (data == 0x1) {
				return "2/8 HEIGHT";
			} else if (data == 0x2) {
				return "3/8 HEIGHT (STEP)";
			} else if (data == 0x3) {
				return "4/8 HEIGHT (STEP)";
			} else if (data == 0x4) {
				return "5/8 HEIGHT (STEP)";
			} else if (data == 0x5) {
				return "6/8 HEIGHT (STEP)";
			} else if (data == 0x6) {
				return "7/8 HEIGHT (STEP)";
			} else if (data == 0x7) {
				return "FULL HEIGHT (STEP)";
			} else {
				return "" + data;
			}
		case CAKE_BLOCK:
			return "" + ((Cake) b).getSlicesRemaining() + "/6 REMAINING";
		case DIODE_BLOCK_OFF:
		case DIODE_BLOCK_ON:
			return ((Diode) b).getFacing().toString() + " with DELAY of "
					+ ((Diode) b).getDelay();
		case LONG_GRASS:
			return ((LongGrass) b).getSpecies().toString();
		case TRAP_DOOR:
			return ((TrapDoor) b).getAttachedFace().toString() + " is "
					+ (((TrapDoor) b).isOpen() ? "OPEN" : "CLOSED");
		case PISTON_BASE:
		case PISTON_STICKY_BASE:
			return ((PistonBaseMaterial) b).getFacing().toString();
		case SANDSTONE:
			if (data == 0x0) {
				return "CRACKED";
			} else if (data == 0x1) {
				return "GLYPHED";
			} else if (data == 0x2) {
				return "SMOOTH";
			} else {
				return "" + data;
			}
		case SMOOTH_BRICK:
			if (data == 0x0) {
				return "NORMAL";
			} else if (data == 0x1) {
				return "MOSSY";
			} else if (data == 0x2) {
				return "CRACKED";
			} else if (data == 0x3) {
				return "CIRCLE";
			} else {
				return "" + data;
			}
		case HUGE_MUSHROOM_1:
		case HUGE_MUSHROOM_2:
			if (data == 0x0) {
				return "FLESHY PIECE";
			} else if (data == 0x1) {
				return "CAP ON TOP & W & N";
			} else if (data == 0x2) {
				return "CAP ON TOP & N";
			} else if (data == 0x3) {
				return "CAP ON TOP & N & E";
			} else if (data == 0x4) {
				return "CAP ON TOP & W";
			} else if (data == 0x5) {
				return "CAP ON TOP";
			} else if (data == 0x6) {
				return "CAP ON TOP & E";
			} else if (data == 0x7) {
				return "CAP ON TOP & S & W";
			} else if (data == 0x8) {
				return "CAP ON TOP & S";
			} else if (data == 0x9) {
				return "CAP ON TOP & E & S";
			} else if (data == 0x10) {
				return "STEM";
			} else {
				return "" + data;
			}
		case VINE:
			String ret = "";
			if ((data & 0x1) == 0x1) {
				if (ret.length() == 0) {
					ret += "SOUTH";
				} else {
					ret += " & SOUTH";
				}
			}
			if ((data & 0x2) == 0x2) {
				if (ret.length() == 0) {
					ret += "WEST";
				} else {
					ret += " & WEST";
				}
			}
			if ((data & 0x4) == 0x4) {
				if (ret.length() == 0) {
					ret += "NORTH";
				} else {
					ret += " & NORTH";
				}
			}
			if ((data & 0x8) == 0x8) {
				if (ret.length() == 0) {
					ret += "EAST";
				} else {
					ret += " & EAST";
				}
			}
			if (ret.length() == 0) {
				ret += "TOP";
			}
			return ret;
		case FENCE_GATE:
			append = " is Closed";
			if ((data & 0x4) == 0x4) {
				append = " is OPEN";
			}
			if ((data & 0x3) == 0x0) {
				return "SOUTH" + append;
			} else if ((data & 0x3) == 0x1) {
				return "WEST" + append;
			} else if ((data & 0x3) == 0x2) {
				return "NORTH" + append;
			} else if ((data & 0x3) == 0x3) {
				return "EAST" + append;
			}
			return "" + data;
		case MONSTER_EGGS: // Hidden Silverfish
			if (data == 0x0) {
				return Material.STONE.toString();
			} else if (data == 0x1) {
				return Material.COBBLESTONE.toString();
			} else if (data == 0x2) {
				return Material.SMOOTH_BRICK.toString();
			} else {
				return "" + data;
			}
		case BREWING_STAND:
			ret = "Bottle in ";
			if ((data & 0x1) == 0x1) {
				if (ret.length() == 10) {
					ret += "EAST Slot";
				} else {
					ret += " & EAST Slot";
				}
			}
			if ((data & 0x2) == 0x2) {
				if (ret.length() == 10) {
					ret += "SOUTH_WEST Slot";
				} else {
					ret += " & SOUTH_WEST Slot";
				}
			}
			if ((data & 0x4) == 0x4) {
				if (ret.length() == 10) {
					ret += "NORTH_WEST Slot";
				} else {
					ret += " & NORTH_WEST Slot";
				}
			}
			if (ret.length() == 10) {
				ret = "Empty";
			}
			return ret;
		case CAULDRON:
			if (data == 0x0) {
				return "EMPTY";
			} else if (data == 0x1) {
				return "1/3 FILLED";
			} else if (data == 0x2) {
				return "2/3 FILLED";
			} else if (data == 0x3) {
				return "FULL";
			} else {
				return "" + data;
			}
		case ENDER_PORTAL_FRAME:
			// TODO Add intelligence here
			return "" + data;
		case EGG:
			// TODO Is there anywhere we can get a mapping of entity id to name?
			return "" + data;
		case COCOA:
			return ((CocoaPlant) b).getFacing() + " "
					+ ((CocoaPlant) b).getSize();
		case TRIPWIRE_HOOK:
			return ((TripwireHook) b).getFacing()
					+ (((TripwireHook) b).isActivated() ? " Activated" : "")
					+ (((TripwireHook) b).isConnected() ? " Connected" : "");
		default:
			return "" + data;
		}
	}

	/**
	 * Initialize printData structure with all supported Material types for
	 *     printing data values.
	 */
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
		printData.add(Material.SPRUCE_WOOD_STAIRS);
		printData.add(Material.BIRCH_WOOD_STAIRS);
		printData.add(Material.JUNGLE_WOOD_STAIRS);
		printData.add(Material.SANDSTONE_STAIRS);
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
		printData.add(Material.WOOD_STEP);
		printData.add(Material.WOOD_DOUBLE_STEP);
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
		printData.add(Material.COCOA);
		printData.add(Material.TRIPWIRE_HOOK);
	}

}
