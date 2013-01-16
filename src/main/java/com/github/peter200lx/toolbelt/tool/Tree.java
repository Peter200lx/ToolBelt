package com.github.peter200lx.toolbelt.tool;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.TreeType;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import com.github.peter200lx.toolbelt.GlobalConf;
import com.github.peter200lx.toolbelt.PrintEnum;
import com.github.peter200lx.toolbelt.AbstractTool;

/**
 * Allows the player to rapidly create trees of varying type.
 */
public class Tree extends AbstractTool  {

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

	/**
	 * Map of Player names to TreeType selections.
	 */
	private Map<String, TreeType> pTreeType = new HashMap<String, TreeType>();

	@Override
	public final String getToolName() {
		return NAME;
	}

	@Override
	public final void handleInteract(PlayerInteractEvent event) {
		Player subject = event.getPlayer();
		String name = subject.getName();
		TreeType type = TreeType.TREE;
		switch (event.getAction()) {
		case RIGHT_CLICK_BLOCK:
			if (pTreeType.containsKey(name)) {
				type = pTreeType.get(name);
			} else {
				pTreeType.put(name, type);
				uPrint(PrintEnum.INFO, subject, ChatColor.GREEN
						+ "Default TreeType is: " + ChatColor.GOLD
						+ type.toString());
			}
			Block block =  event.getClickedBlock().getRelative(
					event.getBlockFace());
			if (block.isEmpty() || block.isLiquid()) {
				block.getWorld().generateTree(block.getLocation(), type);
			} else {
				uPrint(PrintEnum.WARN, subject, ChatColor.RED + "Can't place"
						+ " tree as the starting block is not empty");
			}
			break;
		case LEFT_CLICK_AIR:
		case LEFT_CLICK_BLOCK:
			if (pTreeType.containsKey(name)) {
				// User has already used the Tree tool:
				type = pTreeType.get(name);
				int typeIntVal = type.ordinal();
				typeIntVal++;
				typeIntVal %= TreeType.values().length;
				type = TreeType.values()[typeIntVal];
			}
			pTreeType.put(name, type);
			uPrint(PrintEnum.INFO, subject, ChatColor.GREEN
					+ "Currently selected TreeType: "
					+ ChatColor.GOLD + type.toString());
		default:
			break;
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
}
