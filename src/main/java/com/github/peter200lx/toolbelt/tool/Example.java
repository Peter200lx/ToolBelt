package com.github.peter200lx.toolbelt.tool;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.player.PlayerInteractEvent;

import com.github.peter200lx.toolbelt.GlobalConf;
import com.github.peter200lx.toolbelt.PrintEnum;
import com.github.peter200lx.toolbelt.AbstractTool;

//Instructions for setting up a new tool:

//Add permission node(s) to plugin.yml

//Add configuration options (if any) to config.yml

//Create a help file in src/help/unique_lowercase_name.txt

//You will also need to add a line in ToolBelt.java in
// the loadConf() function.
//Put the following line (without the //) after the last similar line
//available.put(Example.name, new Example(gc));
/**
 * A basic tool definition provided to assist others to build tools.
 */
public class Example extends AbstractTool  {

	/**
	 * Pass the global config object into Tool's constructor.
	 *
	 * @param gc GlobalConf structure holds static configuration from plugin
	 */
	public Example(GlobalConf gc) {
		super(gc);
		// You shouldn't need to add anything here. However if you have
		//  something you want to setup when the tool is loaded/reloaded
		//  you can put that logic here.
	}

	/**
	 * This is the string used for the config.yml and plugin.yml files.
	 */
	public static final String NAME = "unique_lowercase_name";

	@Override
	public final String getToolName() {
		return NAME;
	}

	@Override
	public final void handleInteract(PlayerInteractEvent event) {
		//Handle left and right clicks in here
		// This is where the main logic of your tool should go
	}

	@Override
	public final boolean printUse(CommandSender sender) {
		if (hasPerm(sender)) {
			uPrint(PrintEnum.CMD, sender, "(Right-,Left-,)Click with the "
					+ ChatColor.GOLD + getType() + ChatColor.WHITE
					+ " to (description of tool action)");
			//Also add any special case messages here
			return true;
		}
		return false;
	}

	@Override
	public final boolean loadConf(String tSet, ConfigurationSection conf) {
		//There only needs to be logic in here if you have tool specific
		// data you want to load. This function should always be present.
		//This function should return true if all data loaded successfully
		//  and return false if it got unknown data from config.yml
		return true;
	}
}
