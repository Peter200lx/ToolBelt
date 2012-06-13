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

public class Pliers extends Tool  {

	public Pliers(GlobalConf gc) {
		super(gc);
	}

	public static String name = "pliers";

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
		List<String> subRanks = gc.ranks.getUserRank(subject);
		if(!target.getType().equals(Material.AIR) &&
				noOverwrite(subRanks,target.getType()) ){
			subject.sendMessage(ChatColor.RED+"Sorry, you can't overwrite "+
					ChatColor.GOLD+target.getType());
			return;
		}
		if(!subject.isSneaking()&&!target.getType().equals(Material.AIR)){
			subject.sendMessage(ChatColor.RED+
					"Can't copy into a non-air block without crouching.");
			return;
		}
		if(noCopy(subRanks,clicked.getType()) ){
			subject.sendMessage(ChatColor.RED+"Sorry, you can't copy "+
				ChatColor.GOLD+clicked.getType());
			return;
		}
		if(spawnBuild(clicked,subject)&&spawnBuild(target,subject)) {
			MaterialData set = clicked.getState().getData();
			if(isUseEvent()) {
				if(safeReplace(set,target,subject,true)) {
					this.updateUser(subject, target.getLocation(), set);
				}
			}else {
				target.setTypeIdAndData(set.getItemTypeId(), set.getData(), false);
				this.updateUser(subject, target.getLocation(), set);
			}
		}
	}

	@Override
	public boolean printUse(CommandSender sender) {
		if(hasPerm(sender)) {
			sender.sendMessage("left/right click with the "+ChatColor.GOLD+getType()+
					ChatColor.WHITE+" to copy and push or pull blocks");
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

		if(!loadOnlyAllow(tSet, conf))
			return false;

		if(!loadStopCopy(tSet, conf))
			return false;

		if(!loadStopOverwrite(tSet, conf))
			return false;

		return true;
	}
}
