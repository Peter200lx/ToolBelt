package com.github.peter200lx.toolbelt.tool;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import com.github.peter200lx.toolbelt.GlobalConf;
import com.github.peter200lx.toolbelt.PrintEnum;
import com.github.peter200lx.toolbelt.AbstractTool;

public class Chainsaw extends AbstractTool {

	public Chainsaw(GlobalConf gc) {
		super(gc);
	}

	public static final String NAME = "saw";

	private int horzLimit = 16;
	private int vertLimit = 40;
	private double radiusSphere;

	@Override
	public String getToolName() {
		return NAME;
	}

	@Override
	public void handleInteract(PlayerInteractEvent event) {
		List<Block> toChange;
		final Player subject = event.getPlayer();
		if (!delayElapsed(subject.getName())) {
			return;
		}
		Block target;

		final List<String> subRanks = gc.ranks.getUserRank(subject);
		if (subRanks != null) {
			uPrint(PrintEnum.DEBUG, subject, ChatColor.DARK_PURPLE
					+ "Your ranks are: " + ChatColor.GOLD + subRanks);
		}

		switch (event.getAction()) {
		case LEFT_CLICK_BLOCK:
			target = event.getClickedBlock();
			if (!noOverwrite(subRanks, target.getType())) {
				toChange = recursiveRemove(target, subRanks,
						new ArrayList<Block>(), target.getLocation());
			} else {
				uPrint(PrintEnum.HINT, subject, ChatColor.RED
						+ "Need to click on a block the saw can cut");
				return;
			}
			break;
		case RIGHT_CLICK_BLOCK:
			target = event.getClickedBlock();
			toChange = getSphere(target, radiusSphere, subRanks);
			break;
		default:
			return;
		}

		if (toChange == null) {
			log.warning("[" + gc.modName + "][Chainsaw] Got a null block"
					+ " selection");
			return;
		}
		for (Block cur : toChange) {
			if (spawnBuild(cur, event.getPlayer())) {
				if (isUseEvent()) {
					if (safeBreak(cur, event.getPlayer(), true)) {
						this.updateUser(subject, cur.getLocation(), 0,
								(byte) 0);
					}
				} else {
					cur.setTypeId(0, true);
					this.updateUser(subject, cur.getLocation(), 0, (byte) 0);
				}
			}
		}
	}

	private static final BlockFace[] possible = {
			BlockFace.DOWN,
			BlockFace.WEST,
			BlockFace.EAST,
			BlockFace.NORTH,
			BlockFace.SOUTH,
			BlockFace.UP,
	};

	private List<Block> recursiveRemove(Block target, List<String> subRanks,
			List<Block> soFar, Location source) {
		if ((Math.abs(target.getX() - source.getX()) >= horzLimit)
				|| (Math.abs(target.getZ() - source.getZ()) >= horzLimit)
				|| (Math.abs(target.getY() - source.getY()) >= vertLimit)) {
			return soFar;
		}
		soFar.add(target);
		for (BlockFace check: possible) {
			final Block loc = target.getRelative(check);
			if (!soFar.contains(loc) && !noOverwrite(subRanks, loc.getType())) {
				recursiveRemove(loc, subRanks, soFar, source);
			}
		}
		return soFar;
	}

	private List<Block> getSphere(Block center, double radius,
			List<String> subRanks) {
		final List<Block> toRet = new ArrayList<Block>();
		final int round = (int) Math.round(radius - 0.001);
		for (int x = -round; x <= round; ++x) {
			for (int y = -round; y <= round; ++y) {
				for (int z = -round; z <= round; ++z) {
					final Block loc = center.getRelative(x, y, z);
					if (loc.getLocation().toVector().isInSphere(
							center.getLocation().toVector(), radius)
							&& !noOverwrite(subRanks, loc.getType())) {
						toRet.add(loc);
					}
				}
			}
		}
		return toRet;
	}

	@Override
	public boolean printUse(CommandSender sender) {
		if (hasPerm(sender)) {
			uPrint(PrintEnum.CMD, sender, useFormat(
					"Cut down large chunks of trees"));
			return true;
		}
		return false;
	}

	@Override
	public boolean loadConf(String tSet, ConfigurationSection conf) {

		// Load the repeat delay
		if (!loadRepeatDelay(tSet, conf, -1)) {
			return false;
		}

		horzLimit = conf.getInt(
				tSet + "." + NAME + ".recursiveHorizontalMax", 16);
		vertLimit = conf.getInt(
				tSet + "." + NAME + ".recursiveVerticalMax", 40);
		radiusSphere = conf.getDouble(tSet + "." + NAME + ".radiusSphere", 2.5);
		if (isDebug()) {
			log.info("[" + gc.modName + "][loadConf] Chainsaw horizontal"
					+ " recursion max set to " + horzLimit);
			log.info("[" + gc.modName + "][loadConf] Chainsaw vertical"
					+ " recursion max set to " + vertLimit);
			log.info("[" + gc.modName + "][loadConf] Chainsaw Sphere radius"
					+ " set to " + radiusSphere);
		}

		if (!loadOnlyAllow(tSet, conf)) {
			return false;
		}

		if (!loadStopOverwrite(tSet, conf)) {
			return false;
		}

		return true;
	}
}
