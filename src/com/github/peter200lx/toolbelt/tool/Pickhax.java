package com.github.peter200lx.toolbelt.tool;

import java.util.HashSet;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import com.github.peter200lx.toolbelt.Tool;

public class Pickhax extends Tool  {

	public Pickhax(String modName, Server server, boolean debug,
			boolean permissions, boolean useEvent) {
		super(modName, server, debug, permissions, permissions);
	}

	public static String name = "phax";

	@Override
	public String getToolName() {
		return name;
	}

	@Override
	public void handleInteract(PlayerInteractEvent event){
		Action act = event.getAction();
		if(act.equals(Action.LEFT_CLICK_BLOCK) || act.equals(Action.RIGHT_CLICK_BLOCK)) {
			Block target = event.getClickedBlock();
			if(target != null && !stopOverwrite.contains(target.getType())       &&
					(onlyAllow.isEmpty() || onlyAllow.contains(target.getType())) ){
				if(spawnBuild(target,event.getPlayer())) {
					Boolean physics;
					if(act.equals(Action.LEFT_CLICK_BLOCK))
						physics = true;
					else
						physics = false;
					if(isUseEvent())
						safeBreak(target,event.getPlayer(),physics);
					else
						target.setTypeId(0,physics);
				}
			}else if(target != null) {
				event.getPlayer().sendMessage(ChatColor.RED + "You can't insta-delete "+
						ChatColor.GOLD+target.getType());
			}
		}
	}

	@Override
	public boolean printUse(CommandSender sender) {
		if(hasPerm(sender)) {
			sender.sendMessage("Click with the "+getType()+
					" to delete a block (Right-click for no-physics)");
			return true;
		}
		return false;
	}

	@Override
	public boolean loadConf(String tSet, FileConfiguration conf) {

		//Load the default restriction configuration
		if(!loadGlobalRestrictions(tSet,conf))
			return false;

		List<Integer> intL = conf.getIntegerList(tSet+"."+name+".onlyAllow");

		if(!intL.isEmpty())
		{
			if(isDebug())
				log.info( "["+modName+"][loadConf] As "+name+".onlyAllow has items,"+
						" it overwrites the global");

			onlyAllow = loadMatList(intL,new HashSet<Material>(),tSet+"."+name+".onlyAllow");
			if(onlyAllow == null)
				return false;

			if(isDebug()) {
				logMatSet(onlyAllow,"loadGlobalRestrictions",name+".onlyAllow:");
				log.info( "["+modName+"][loadConf] As "+name+".onlyAllow has items,"+
						" only those materials are usable");
			}
		} else if(isDebug()&& !onlyAllow.isEmpty()) {
			log.info( "["+modName+"][loadConf] As global.onlyAllow has items,"+
					" only those materials are usable");
		}

		intL = conf.getIntegerList(tSet+"."+name+".stopOverwrite");

		if(!intL.isEmpty())
		{
			if(isDebug())
				log.info( "["+modName+"][loadConf] As "+name+".stopOverwrite has items,"+
						" it overwrites the global");

			stopOverwrite = loadMatList(intL,defStop(),tSet+"."+name+".stopOverwrite");
			if(stopOverwrite == null)
				return false;

			if(isDebug()) logMatSet(stopOverwrite,"loadGlobalRestrictions",
					name+".stopOverwrite:");
		}
		return true;
	}
}
