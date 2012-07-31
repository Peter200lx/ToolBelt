package com.github.peter200lx.toolbelt;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author peter200lx
 *
 * This defines the interactions between the ToolBelt core and any given tool.
 */
interface ToolInterface {

	/**
	 * Tool identifier string. Should be a single unique lowercase word.
	 * This is the string used for the config.yml and plugin.yml files
	 */
	String NAME = null;

	/**
	 * Tells caller the Material this tool maps to.
	 *
	 * @return Material this tool overloads
	 */
	Material getType();

	/**
	 * Sets the Material that this tool overloads in-game.
	 * This should only ever be set in ToolBelt.loadConf()
	 *
	 * @param type Material this tool should overload
	 */
	void setType(Material type);

	/**
	 * Tells the caller the tool name.
	 * This should be used instead of calling "name" directly.
	 *
	 * @return Name of to tool.
	 */
	String getToolName();

	/**
	 * This catches left/right click events.
	 *
	 * @param event Bukkit PlayerInteractEvent to handle
	 */
	void handleInteract(PlayerInteractEvent event);

	/**
	 * This catches the event when a player changes the item in their hand.
	 *
	 * @param event Bukkit PlayerItemHeldEvent to handle
	 */
	void handleItemChange(PlayerItemHeldEvent event);

	/**
	 * This catches when a player receives damage.
	 * As we have to know the player is holding the correct tool, this
	 * should only be called after verifying that the Entity in question
	 * is a Player.
	 *
	 * @param event Bukkit EntityDamageEvent to handle
	 */
	void handleDamage(EntityDamageEvent event);

	/**
	 * Return true if "sender" has permission to use this tool.
	 *
	 * @param sender Person to check if they have permission
	 * @return true if they can use tool, false otherwise
	 */
	boolean hasPerm(CommandSender sender);

	/**
	 * This is for printing use instructions for a player.
	 *
	 * @param sender Person to display instructions for
	 * @return true if anything is printed to the user, false otherwise.
	 */
	boolean printUse(CommandSender sender);

	/**
	 * If the tool has its own area in config.yml, then here is
	 * where those settings are loaded.
	 *
	 * @param tSet name of the first level of the config.yml "tools"
	 * @param conf configuration object representing config.yml
	 * @return true if no errors processing conf, false if errors occur
	 */
	boolean loadConf(String tSet, ConfigurationSection conf);

	/**
	 * If the tool has its own area in config.yml, then this is
	 * what will set up the help file for configuration.
	 *
	 * @param host reference to plugin object, used to access saveResource()
	 */
	void saveHelp(JavaPlugin host);
}
