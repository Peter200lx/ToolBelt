package com.github.peter200lx.toolbelt.tool;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import com.github.peter200lx.toolbelt.GlobalConf;
import com.github.peter200lx.toolbelt.PrintEnum;
import com.github.peter200lx.toolbelt.AbstractTool;

public class Ruler extends AbstractTool {

	public Ruler(GlobalConf gc) {
		super(gc);
	}

	private final Map<String, Location[]> pCube = new HashMap<String,
			Location[]>();

	public static final String NAME = "ruler";

	@Override
	public String getToolName() {
		return NAME;
	}

	@Override
	public void handleInteract(PlayerInteractEvent event) {
		final Player subject = event.getPlayer();
		final String playerName = subject.getName();
		if (!delayElapsed(subject.getName())) {
			return;
		}
		// Prep HashMap
		if (!pCube.containsKey(playerName)) {
			pCube.put(playerName, new Location[2]);
		}
		Location[] ptArray = pCube.get(playerName);

		// Load in new click
		switch (event.getAction()) {
		case LEFT_CLICK_BLOCK:
			ptArray[0] = event.getClickedBlock().getLocation();
			uPrint(PrintEnum.IMPORT, subject, ChatColor.GREEN + "Point 0"
					+ " is now: " + ChatColor.GOLD + loc2Str(ptArray[0]));
			break;
		case RIGHT_CLICK_BLOCK:
			ptArray[1] = event.getClickedBlock().getLocation();
			uPrint(PrintEnum.IMPORT, subject, ChatColor.GREEN + "Point 1"
					+ " is now: " + ChatColor.GOLD + loc2Str(ptArray[1]));
			break;
		case LEFT_CLICK_AIR:
			if (subject.isSneaking()) {
				ptArray[0] = subject.getTargetBlock(null, 0).getLocation();
				uPrint(PrintEnum.IMPORT, subject, ChatColor.GREEN
						+ "Point 0 is now: "
						+ ChatColor.GOLD + loc2Str(ptArray[0]));
			} else {
				uPrint(PrintEnum.HINT, subject, ChatColor.RED + "Didn't catch"
						+ " that, try crouching for distance measurement");
				return;
			}
			break;
		case RIGHT_CLICK_AIR:
			if (subject.isSneaking()) {
				ptArray[1] = subject.getTargetBlock(null, 0).getLocation();
				uPrint(PrintEnum.IMPORT, subject, ChatColor.GREEN
						+ "Point 1 is now: "
						+ ChatColor.GOLD + loc2Str(ptArray[1]));
			} else {
				uPrint(PrintEnum.HINT, subject, ChatColor.RED + "Didn't catch"
						+ " that, try crouching for distance measurement");
				return;
			}
			break;
		default:
			return;
		}
		pCube.put(playerName, ptArray);

		// Display data or help
		final Location pt1 = ptArray[0];
		final Location pt2 = ptArray[1];
		if ((pt1 != null) && (pt2 != null)
				&& (pt1.getWorld() == pt2.getWorld())) {
			final int widthX = 1 + Math.abs(pt1.getBlockX() - pt2.getBlockX());
			final int widthY = 1 + Math.abs(pt1.getBlockY() - pt2.getBlockY());
			final int widthZ = 1 + Math.abs(pt1.getBlockZ() - pt2.getBlockZ());
			final int vol = widthX * widthY * widthZ;
			final double dist = pt1.distance(pt2);
			final DecimalFormat df = new DecimalFormat("#.##");
			uPrint(PrintEnum.IMPORT, subject, ChatColor.GREEN
					+ "The width in (x,y,z) is: " + ChatColor.GOLD + "("
					+ widthX + "," + widthY + "," + widthZ + ")"
					+ ChatColor.GREEN + " and the total distance: "
					+ ChatColor.GOLD + df.format(dist));
			uPrint(PrintEnum.IMPORT, subject, ChatColor.GREEN + "The volume of"
					+ " the enclosed space is: " + ChatColor.GOLD + vol);
		} else if ((pt1 != null) && (pt2 != null)) {
			// tell user worlds are different
			if (subject.getWorld().equals(pt1.getWorld())) {
				uPrint(PrintEnum.WARN, subject, ChatColor.RED
						+ "Your previous click was out "
						+ "of this world, right-click to refresh it");
			} else if (subject.getWorld().equals(pt2.getWorld())) {
				uPrint(PrintEnum.WARN, subject, ChatColor.RED
						+ "Your previous click was out "
						+ "of this world, left-click to refresh it");
			}
		} else if ((pt1 != null) && (pt2 == null)) {
			// Tell user to right-click
			uPrint(PrintEnum.HINT, subject,
					"Now right-click to grab the other point");
		} else if ((pt1 == null) && (pt2 != null)) {
			// Tell user to left-click
			uPrint(PrintEnum.HINT, subject,
					"Now left-click to grab the other point");
		}
	}

	private String loc2Str(Location loc) {
		return loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
	}

	@Override
	public boolean printUse(CommandSender sender) {
		if (hasPerm(sender)) {
			uPrint(PrintEnum.CMD, sender, useFormat("Left/Right click"
					+ " for measurements (crouch for range)"));
			return true;
		}
		return false;
	}

	public boolean loadConf(String tSet, ConfigurationSection conf) {

		// Load the repeat delay
		if (!loadRepeatDelay(tSet, conf, 0)) {
			return false;
		}

		return true;
	}
}
