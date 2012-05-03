package com.github.peter200lx.toolbelt.tool;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.MaterialData;

import com.github.peter200lx.toolbelt.GlobalConf;
import com.github.peter200lx.toolbelt.Tool;

//Instructions for setting up a new tool

//Add permission node(s) to plugin.yml

//Add configuration options (if any) to config.yml
public class Sledge extends Tool  {

	public Sledge(GlobalConf gc) {
		super(gc);
	}

	public static String name = "sledge";

	@Override
	public String getToolName() {
		return name;
	}

	@Override
	public void handleInteract(PlayerInteractEvent event){
		Player subject = event.getPlayer();
		Block clicked, target;
		if(!delayElapsed(subject.getName()))
			return;
		switch (event.getAction()) {
		case LEFT_CLICK_BLOCK:
			clicked = event.getClickedBlock();
			target = clicked.getRelative(event.getBlockFace().getOppositeFace());
			break;
		case RIGHT_CLICK_BLOCK:
			clicked = event.getClickedBlock();
			target = clicked.getRelative(event.getBlockFace());
			break;
		case LEFT_CLICK_AIR:
		case RIGHT_CLICK_AIR:
			subject.sendMessage(ChatColor.RED +"Sorry, didn't catch that, "+
					"you need to click on a block.");
		default:
			return;
		}
		if(!target.getType().equals(Material.AIR) && !(!stopOverwrite.contains(target.getType()) &&
				(onlyAllow.isEmpty() || onlyAllow.contains(target.getType()))) ){
			subject.sendMessage(ChatColor.RED+"Sorry, you can't overwrite "+
					ChatColor.GOLD+target.getType());
			return;
		}
		if(!subject.isSneaking()&&!target.getType().equals(Material.AIR)){
			subject.sendMessage(ChatColor.RED+
					"Can't move into a non-air block without crouching.");
			return;
		}
		if(stopCopy.contains(clicked.getType()) || !( onlyAllow.isEmpty() ||
				onlyAllow.contains(clicked.getType()) ) ){
			subject.sendMessage(ChatColor.RED+"Sorry, you can't move "+
				ChatColor.GOLD+clicked.getType());
			return;
		}
		if(spawnBuild(clicked,subject)&&spawnBuild(target,subject)) {
			MaterialData set = clicked.getState().getData();
			if(isUseEvent()) {
				if(safeReplace(set,target,subject,true)) {
					if(safeBreak(clicked,subject,false)){
						subject.sendBlockChange(clicked.getLocation(), 0, (byte)0);
						subject.sendBlockChange(target.getLocation(), set.getItemType(),
								set.getData());
					}
				}
			}else {
				clicked.setTypeId(0, false);
				target.setTypeIdAndData(set.getItemTypeId(), set.getData(), false);
				subject.sendBlockChange(clicked.getLocation(), 0, (byte)0);
				subject.sendBlockChange(target.getLocation(), set.getItemType(), set.getData());
			}
		}
	}

	@Override
	public boolean printUse(CommandSender sender) {
		if(hasPerm(sender)) {
			sender.sendMessage("left/right click with the "+ChatColor.GOLD+getType()+
					ChatColor.WHITE+" to push or pull blocks");
			//sender.sendMessage("Crouch to push or pull into more then just air");
			return true;
		}
		return false;
	}

	@Override
	public boolean loadConf(String tSet, FileConfiguration conf) {

		//Load the repeat delay
		if(!loadRepeatDelay(tSet,conf,-1))
			return false;

		List<Integer> intL = conf.getIntegerList(tSet+"."+name+".onlyAllow");

		if(!intL.isEmpty())
		{
			if(isDebug())
				log.info( "["+gc.modName+"][loadConf] As "+name+".onlyAllow has items,"+
						" it overwrites the global");

			if(!onlyAllow.loadMatList(intL,false,tSet+"."+name+".onlyAllow"))
				return false;

			if(isDebug()) {
				onlyAllow.logMatSet("loadConf",name+".onlyAllow:");
				log.info( "["+gc.modName+"][loadConf] As "+name+".onlyAllow has items,"+
						" only those materials are usable");
			}
		} else if(isDebug()&& !onlyAllow.isEmpty()) {
			log.info( "["+gc.modName+"][loadConf] As global.onlyAllow has items,"+
					" only those materials are usable");
		}

		intL = conf.getIntegerList(tSet+"."+name+".stopCopy");

		if(!intL.isEmpty())
		{
			if(isDebug())
				log.info( "["+gc.modName+"][loadConf] As "+name+".stopCopy has items,"+
						" it overwrites the global");

			if(!stopCopy.loadMatList(intL,true,tSet+"."+name+".stopCopy"))
				return false;

			if(isDebug()) stopCopy.logMatSet("loadConf",name+".stopCopy:");
		}

		intL = conf.getIntegerList(tSet+"."+name+".stopOverwrite");

		if(!intL.isEmpty())
		{
			if(isDebug())
				log.info( "["+gc.modName+"][loadConf] As "+name+".stopOverwrite has items,"+
						" it overwrites the global");

			if(!stopOverwrite.loadMatList(intL,true,tSet+"."+name+".stopOverwrite"))
				return false;

			if(isDebug()) stopOverwrite.logMatSet("loadConf",
					name+".stopOverwrite:");
		}
		return true;
	}
}
