package com.github.peter200lx.toolbelt.tool;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.player.PlayerInteractEvent;

import com.github.peter200lx.toolbelt.Tool;

//You will also need to add a line in ToolBelt.java in
// the loadConf() function.
//Put the following line (without the //) after the last similar line
//available.put(Example.name, new Example(cName,debug,permissions));
public class Example extends Tool  {

	protected Example(String modName, boolean debug, boolean permissions) {
		super(modName, debug, permissions);
		// You shouldn't need to add anything here. However if you have
		//  something you want to setup when the tool is loaded/reloaded
		//  you can put that logic here.
	}

	//This is the string used for the config.yml and plugin.yml files
	public static String name = "unique_lowercase_name";

	@Override
	public void handleInteract(PlayerInteractEvent event){
		//Handle left and right clicks in here
		// This is where the main logic of your program should go
	}

	@Override
	public boolean printUse(CommandSender sender) {
		if(hasPerm(sender)) {
			sender.sendMessage("(Right-,Left-,)Click with the "+getType()+
					" to (description of tool action)");
			//Also add any special case messages here
			return true;
		}
		return false;
	}

	@Override
	public boolean loadConf(String tSet, FileConfiguration conf) {
		//This is only needed if you have some tool specific settings
		//  you want to get from the config.yml file
		//This file should return true if all data loaded successfully
		//  and return false if it got unknown data from config.yml
		return false;
	}
}
