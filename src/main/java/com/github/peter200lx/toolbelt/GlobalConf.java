package com.github.peter200lx.toolbelt;

import org.bukkit.Server;

/**
 * @author peter200lx
 *
 * This is a simple container object for global variables.
 *
 * GlobalConf was created so that I didn't have tons of variables passed
 *     separately into every tool. This provides a single parameter to
 *     pass into each tool. Also, this way additional variables can be
 *     added without modifying ToolInterface, Tool, and every individual
 *     tool defined.
 */
public class GlobalConf {

	/**
	 * Store the global variables into a single object.
	 *
	 * @param modName name of the bukkit plugin
	 * @param server reference to the bukkit server
	 * @param debug true if debug messages should be printed
	 * @param perm true if permissions are used on the server
	 * @param useEvent true if Block*Events should be called
	 * @param repeatDelay global time between tool uses
	 * @param onlyAllow global Material white-list
	 * @param stopCopy global Material copy blacklist
	 * @param stopOverwrite global Material write blacklist
	 * @param ranks what ranks have been defined in config.yml (if any)
	 * @param printLevel how verbose user messages should be by default
	 */
	public GlobalConf(String modName, Server server, boolean debug,
			boolean perm, boolean useEvent, int repeatDelay,
			SetMat onlyAllow, SetMat stopCopy, SetMat stopOverwrite,
			Ranks ranks, PrintEnum printLevel) {
		super();
		this.modName = modName;
		this.server = server;
		this.debug = debug;
		this.perm = perm;
		this.useEvent = useEvent;
		this.repeatDelay = repeatDelay;
		this.onlyAllow = onlyAllow;
		this.stopCopy = stopCopy;
		this.stopOverwrite = stopOverwrite;
		this.ranks = ranks;
		this.pl = printLevel;
	}

	/**
	 * Name of the bukkit plugin. (ToolBelt)
	 * This string can have capitalization, use .toLowerCase() for permissions
	 * or config.yml reading.
	 */
	public final String modName;

	/**
	 * Reference to bukkit server hosting this plugin.
	 */
	public final Server server;

	/**
	 * Indication of debug logging. This isn't used for user messages, just for
	 * logging to the console.
	 */
	public final boolean debug;

	/**
	 * Indication of whether permissions are used on this server.
	 */
	public final boolean perm;

	/**
	 * Indication if tool should call Block*Events before modifying world.
	 * BlockDamageEvent: Called before breaking a block. Some world protection
	 *     plugins check this instead of BlockBreakEvent.
	 * BlockBreakEvent: Called before breaking a block, after BlockDamageEvent.
	 * BlockPlaceEvent: Called before placing (or replacing) a block.
	 */
	public final boolean useEvent;

	/**
	 * This is the delay between when tools can be used. This was implemented to
	 *     prevent issues where a block broke instantaneously and thus created
	 *     several calls of the PlayerInteractEvent in a short time period. The
	 *     delay is both globally configurable, and can be controlled per tool.
	 */
	public final int repeatDelay;

	/**
	 * Global Material restriction white-list. Tools can overwrite the data in
	 *     here with their own lists, or just use the global copy. If this list
	 *     is empty, it does not create any restrictions, if there is one or
	 *     more items, only the Materials listed can be used.
	 */
	public final SetMat onlyAllow;

	/**
	 * Global Material copy restriction blacklist. As above tools can have
	 *     private lists. Any Materials listed here will block users from
	 *     loading with the tool to place.
	 *
	 * For example, a Material listed here can't be loaded into the paint tool.
	 *     Thus to prevent a user from placing TNT, add it to the stopCopy list
	 *     and they will not be able to place TNT in the world with the paint
	 *     tool.
	 */
	public final SetMat stopCopy;

	/**
	 * Global Material replacing restriction blacklist. As above tools can have
	 *     private lists. Any Materials listed here will block users from
	 *     replacing with the tool.
	 *
	 * For example, a Material listed here can't be replaced with the paint
	 *     tool. Thus to prevent a user from ruining Redstone, adding it to the
	 *     stopOverwrite list would prevent the tool(s) in question from
	 *     damaging wiring. Another example is to stop a user from bypassing
	 *     Adminium.
	 */
	public final SetMat stopOverwrite;

	/**
	 * Object describing what ranks have been defined by the admin. This
	 *     structure contains both the names of the possible ranks, and their
	 *     relation to one another.
	 */
	public final Ranks ranks;

	/**
	 * Global enumeration for user message verbosity. This is the level set in
	 *     config.yml, if permissions are enabled, users can have personal
	 *     print verbosity set with toolbelt.print.(PrintEnum name)
	 *
	 * pl stands for Print Level.
	 */
	public final PrintEnum pl;

}
