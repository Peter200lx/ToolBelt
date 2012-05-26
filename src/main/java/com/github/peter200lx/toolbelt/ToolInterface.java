package com.github.peter200lx.toolbelt;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.plugin.java.JavaPlugin;

public interface ToolInterface {

	//This is the string used for the config.yml and plugin.yml files
	// Every tool should have a unique lowercase name here.
	public static String name = null;

	//Return the Material that this tool overloads in-game
	public Material getType();

	//Sets the Material that this tool overloads in-game
	// This should only ever be set in ToolBelt.loadConf()
	public void setType(Material type);

	//Returns the tool specific name
	public String getToolName();

	//This catches left/right click events
	public void handleInteract(PlayerInteractEvent event);

	//This catches then event when a player changes the item in their hand
	public void handleItemChange(PlayerItemHeldEvent event);

	//This catches when an entity receives damage
	public void handleDamage(EntityDamageEvent event);

	//Return true if "sender" has permission to use this tool
	public boolean hasPerm(CommandSender sender);

	//This is for printing use instructions for a player
	public boolean printUse(CommandSender sender);

	//If the tool has its own area in config.yml, then here is
	// where those settings are loaded
	public boolean loadConf(String tSet, FileConfiguration conf);

	//If the tool has its own area in config.yml, then this is
	// what will set up the help file for configuration.
	public void saveHelp(JavaPlugin host);
}
