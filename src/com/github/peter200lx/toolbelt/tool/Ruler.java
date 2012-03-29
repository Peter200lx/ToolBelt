package com.github.peter200lx.toolbelt.tool;

import java.text.DecimalFormat;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import com.github.peter200lx.toolbelt.Tool;

public class Ruler extends Tool  {

	public Ruler(String modName, Server server, boolean debug,
			boolean permissions, boolean useEvent) {
		super(modName, server, debug, permissions, useEvent);
	}

	private HashMap<String,Location[]> pCube = new HashMap<String,Location[]>();

	public static String name = "ruler";

	@Override
	public String getToolName() {
		return name;
	}

	@Override
	public void handleInteract(PlayerInteractEvent event){
		Player subject = event.getPlayer();
		String name = subject.getName();
		//Prep HashMap
		if(!pCube.containsKey(name)) {
			pCube.put(name, new Location[2]);
		}
		Location[] ptArray = pCube.get(name);
		//Load in new click
		switch(event.getAction()) {
		case LEFT_CLICK_BLOCK:
			ptArray[0] = event.getClickedBlock().getLocation();
			subject.sendMessage(ChatColor.GREEN+"Point 0 is now: "+ChatColor.GOLD+loc2Str(ptArray[0]));
			break;
		case RIGHT_CLICK_BLOCK:
			ptArray[1] = event.getClickedBlock().getLocation();
			subject.sendMessage(ChatColor.GREEN+"Point 1 is now: "+ChatColor.GOLD+loc2Str(ptArray[1]));
			break;
		case LEFT_CLICK_AIR:
			if(subject.isSneaking()) {
				ptArray[0] = subject.getTargetBlock(null, 0).getLocation();
				subject.sendMessage(ChatColor.GREEN+"Point 0 is now: "+ChatColor.GOLD+loc2Str(ptArray[0]));
			}else {
				subject.sendMessage(ChatColor.RED +"Sorry, didn't catch that, try crouching for distance measurement");
				return;
			}
			break;
		case RIGHT_CLICK_AIR:
			if(subject.isSneaking()) {
				ptArray[1] = subject.getTargetBlock(null, 0).getLocation();
				subject.sendMessage(ChatColor.GREEN+"Point 1 is now: "+ChatColor.GOLD+loc2Str(ptArray[1]));
			}else {
				subject.sendMessage(ChatColor.RED +"Sorry, didn't catch that, try crouching for distance measurement");
				return;
			}
			break;
		default:
			return;
		}
		pCube.put(name, ptArray);
		//Display data or help
		Location pt1 = ptArray[0];
		Location pt2 = ptArray[1];
		if((pt1 != null)&&(pt2 != null)&&(pt1.getWorld()==pt2.getWorld())) {
			int widthX = 1+Math.abs(pt1.getBlockX()-pt2.getBlockX());
			int widthY = 1+Math.abs(pt1.getBlockY()-pt2.getBlockY());
			int widthZ = 1+Math.abs(pt1.getBlockZ()-pt2.getBlockZ());
			int vol = widthX * widthY * widthZ;
			double dist = pt1.distance(pt2);
			DecimalFormat df = new DecimalFormat("#.##");
			subject.sendMessage("The width in (x,y,z) is: ("+widthX+","+widthY+","+widthZ+
					") and the total distance: "+df.format(dist));
			subject.sendMessage("The volume of the enclosed space is: "+vol);
		}else if((pt1 != null)&&(pt2 != null)) {
			//tell user worlds are different
			if(subject.getWorld().equals(pt1.getWorld()))
				subject.sendMessage(ChatColor.RED+"Your previous click was out of this world,"+
						" right-click to refresh it");
			else if(subject.getWorld().equals(pt2.getWorld()))
				subject.sendMessage(ChatColor.RED+"Your previous click was out of this world,"+
						" left-click to refresh it");
		}else if((pt1 != null)&&(pt2 == null)) {
			//Tell user to right-click
			subject.sendMessage("Now right-click to grab the other point");
		}else if((pt1 == null)&&(pt2 != null)) {
			//Tell user to left-click
			subject.sendMessage("Now left-click to grab the other point");
		}
	}

	private String loc2Str(Location loc) {
		return loc.getBlockX()+","+loc.getBlockY()+","+loc.getBlockZ();
	}

	@Override
	public boolean printUse(CommandSender sender) {
		if(hasPerm(sender)) {
			sender.sendMessage("Left and right click with the "+getType()+
					" to get distance information (crouch for range)");
			return true;
		}
		return false;
	}

	public boolean loadConf(String tSet, FileConfiguration conf) {
		return true;
	}
}
