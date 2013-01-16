package com.github.peter200lx.toolbelt.tool;

import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.TreeType;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import com.github.peter200lx.toolbelt.GlobalConf;
import com.github.peter200lx.toolbelt.PrintEnum;
import com.github.peter200lx.toolbelt.AbstractTool;

/**
 * Allows the player to rapidly create trees of varying type.
 */
public class Tree extends AbstractTool  {
	private HashMap<Player, TreeSettings> selectedTreeType =
			new HashMap<Player, TreeSettings>();

	/**
	 * Pass the global config object into Tool's constructor.
	 *
	 * @param gc GlobalConf structure holds static configuration from plugin
	 */
	public Tree(GlobalConf gc) {
		super(gc);
	}

	/**
	 * This is the string used for the config.yml and plugin.yml files.
	 */
	public static final String NAME = "tree";

	@Override
	public final String getToolName() {
		return NAME;
	}

	@Override
	public final void handleInteract(PlayerInteractEvent event) {
		Player subject = event.getPlayer();
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			TreeSettings ts;
			if (!selectedTreeType.containsKey(event.getPlayer())) {
				ts = setupUser(subject);
			} else {
				ts = selectedTreeType.get(event.getPlayer());
			}
			Block block =  event.getClickedBlock().getRelative(
					event.getBlockFace());
			if (block.isEmpty() || block.isLiquid()) {
				block.getWorld().generateTree(block.getLocation(), ts.treeType);
			} else {
				uPrint(PrintEnum.WARN, subject, ChatColor.RED + "Can't place"
						+ " tree as the starting block is not empty");
			}

		} else if (event.getAction() == Action.LEFT_CLICK_AIR
				|| event.getAction() == Action.LEFT_CLICK_BLOCK) {
			if (selectedTreeType.containsKey(event.getPlayer())) {
				// User has already used the Tree tool:
				TreeSettings ts = selectedTreeType.get(event.getPlayer());
				TreeType[] treetypes = TreeType.values();
				if (ts.current == treetypes.length - 1) {
					ts.current = 0;
					ts.treeType = treetypes[0];
				} else {
					++ts.current;
					ts.treeType = treetypes[ts.current];
				}
				uPrint(PrintEnum.INFO, subject, ChatColor.GREEN
						+ "Currently selected TreeType: "
						+ ts.treeType.toString());
			} else {
				setupUser(subject);
			}
		}
	}

	@Override
	public final boolean printUse(CommandSender sender) {
		if (hasPerm(sender)) {
			uPrint(PrintEnum.CMD, sender, "Left-Click with the "
					+ ChatColor.GOLD + getType() + ChatColor.WHITE
					+ " to cycle through Tree Types");
			uPrint(PrintEnum.CMD, sender, "Right-Click with the "
					+ ChatColor.GOLD + getType() + ChatColor.WHITE
					+ " to place Tree");
			return true;
		}
		return false;
	}

	@Override
	public final boolean loadConf(String tSet, ConfigurationSection conf) {
		return true;
	}

	private class TreeSettings {
		// Settings initialized to standard tree
		public TreeType treeType = TreeType.TREE;
		public int current = TreeType.TREE.ordinal();
	}

	/**
	 * Initialize the Player with TREE type on first use of tool.
	 *
	 * @param subject subject to add to the Map
	 * @return type of tree the player has selected by default
	 */
	private TreeSettings setupUser(Player subject) {
		uPrint(PrintEnum.HINT, subject, "Welcome to the tree tool!"
				+ "Use left click to cycle through the available TreeTypes"
				+ "Use right click to place a tree of the selected type");
		TreeSettings ts = new TreeSettings();
		selectedTreeType.put(subject, ts);
		uPrint(PrintEnum.INFO, subject, ChatColor.GREEN
				+ "Currently selected TreeType: " + ts.treeType.toString());
		return ts;
	}

}
