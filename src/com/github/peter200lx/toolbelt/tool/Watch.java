package com.github.peter200lx.toolbelt.tool;

import java.text.DecimalFormat;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import com.github.peter200lx.toolbelt.Tool;

public class Watch extends Tool  {

	public Watch(String modName, Server server, boolean debug,
			boolean permissions, boolean useEvent) {
		super(modName, server, debug, permissions, useEvent);
	}

	private long cooldown;

	private int timeDay;

	private int timeNight;

	private HashMap<String, Long> pCooldown = new HashMap<String, Long>();

	public static String name = "watch";

	@Override
	public String getToolName() {
		return name;
	}

	@Override
	public void handleInteract(PlayerInteractEvent event){
		Player subject = event.getPlayer();
		String name = subject.getName();
		DecimalFormat df = new DecimalFormat("#.#");

		switch(event.getAction()) {
		case LEFT_CLICK_BLOCK:
		case LEFT_CLICK_AIR:
			//Set time to day
			if(hasNoWaitPerm(subject) || !pCooldown.containsKey(name) ||
					(System.currentTimeMillis() >= (pCooldown.get(name)+cooldown))) {
				//Set time to day
				subject.getWorld().setTime(timeDay);
				subject.sendMessage(ChatColor.GREEN+"Time has been set to "+
						ChatColor.GOLD+timeDay);
				pCooldown.put(name, System.currentTimeMillis());
			}else {
				double left = 1.0 +
						(cooldown - (System.currentTimeMillis()-pCooldown.get(name)))/1000.0;
				subject.sendMessage(ChatColor.RED+"You have to wait "+df.format(left)+
						" seconds to change the time.");
			}
			break;
		case RIGHT_CLICK_BLOCK:
		case RIGHT_CLICK_AIR:
			//Set time to night
			if(hasNoWaitPerm(subject) || !pCooldown.containsKey(name) ||
					(System.currentTimeMillis() >= (pCooldown.get(name)+cooldown))) {
				//Set time to night
				subject.getWorld().setTime(timeNight);
				subject.sendMessage(ChatColor.GREEN+"Time has been set to "+
						ChatColor.GOLD+timeNight);
				pCooldown.put(name, System.currentTimeMillis());
			}else {
				double left = 1.0 +
						(cooldown - (System.currentTimeMillis()-pCooldown.get(name)))/1000.0;
				subject.sendMessage(ChatColor.RED+"You have to wait "+df.format(left)+
						" seconds to change the time.");
			}
			break;
		default:
			return;
		}
	}

	private boolean hasNoWaitPerm(CommandSender subject) {
		if(isPermissions())
			return subject.hasPermission(getPermStr()+".noWait");
		else
			return true;
	}

	@Override
	public boolean printUse(CommandSender sender) {
		if(hasPerm(sender)) {
			sender.sendMessage("Left click with the "+getType()+
					" to to set time to day, right-click for night");
			return true;
		}
		return false;
	}

	@Override
	public boolean loadConf(String tSet, FileConfiguration conf) {
		cooldown = conf.getInt(tSet+"."+name+".cooldown", 5)*1000;
		timeDay = conf.getInt(tSet+"."+name+".timeDay", 1000);
		timeNight = conf.getInt(tSet+"."+name+".timeNight", 14000);
		if(isDebug()) {
			log.info("["+modName+"][loadConf] Cooldown between watch time change is set to "+
					cooldown);
			log.info("["+modName+"][loadConf] Day time is defined as " + timeDay);
			log.info("["+modName+"][loadConf] Night time is defined as " + timeNight);
		}
		return true;
	}
}
