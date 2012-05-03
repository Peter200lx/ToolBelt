package com.github.peter200lx.toolbelt.tool;

import java.util.HashSet;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import com.github.peter200lx.toolbelt.GlobalConf;
import com.github.peter200lx.toolbelt.Tool;

public class Watch extends Tool  {

	public Watch(GlobalConf gc) {
		super(gc);
	}

	private int timeDay;

	private int timeNight;

	private HashSet<String> pNotSync = new HashSet<String>();

	public static String name = "watch";

	@Override
	public String getToolName() {
		return name;
	}

	@Override
	public void handleInteract(PlayerInteractEvent event){
		Player subject = event.getPlayer();
		int time;
		if(!delayElapsed(subject.getName()))
			return;

		switch(event.getAction()) {
		case LEFT_CLICK_BLOCK:
		case LEFT_CLICK_AIR:
			time = timeDay;
			break;
		case RIGHT_CLICK_BLOCK:
		case RIGHT_CLICK_AIR:
			time = timeNight;
			break;
		default:
			return;
		}

		if(!subject.isSneaking()) {
			subject.setPlayerTime(time-subject.getWorld().getTime(), true);
			subject.sendMessage(ChatColor.GREEN+"Your time has been set to "+
					ChatColor.GOLD+time+ChatColor.GREEN+" (Crouch and click to reset)");
			pNotSync.add(subject.getName());
		}else {
			if(pNotSync.contains(subject.getName())) {
				subject.resetPlayerTime();
				subject.sendMessage(ChatColor.GREEN+
						"Your time is now synced with the server at "+
						ChatColor.GOLD+subject.getWorld().getTime());
				pNotSync.remove(subject.getName());
			}else if(hasServerPerm(subject)) {
				subject.getWorld().setTime(time);
				subject.sendMessage(ChatColor.DARK_GREEN+"Server time has been set to "+
						ChatColor.GOLD+time);
			}
		}
	}

	private boolean hasServerPerm(CommandSender subject) {
		if(gc.perm)
			return subject.hasPermission(getPermStr()+".server");
		else
			return true;
	}

	@Override
	public boolean printUse(CommandSender sender) {
		if(hasPerm(sender)) {
			sender.sendMessage("Left click with the "+ChatColor.GOLD+getType()+
					ChatColor.WHITE+" to set time to day, right for night");
			return true;
		}
		return false;
	}

	@Override
	public boolean loadConf(String tSet, FileConfiguration conf) {

		//Load the repeat delay
		if(!loadRepeatDelay(tSet,conf,-1))
			return false;

		timeDay = conf.getInt(tSet+"."+name+".timeDay", 1000);
		timeNight = conf.getInt(tSet+"."+name+".timeNight", 14000);
		if(isDebug()) {
			log.info("["+gc.modName+"][loadConf] Day time is defined as " + timeDay);
			log.info("["+gc.modName+"][loadConf] Night time is defined as " + timeNight);
		}
		return true;
	}
}
