package com.github.peter200lx.toolbelt.tool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.BlockChangeDelegate;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.MaterialData;

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

	/**
	 * List of TreeType that admin has configured to allow.
	 */
	private List<TreeType> availableTypes = new ArrayList<TreeType>();

	@Override
	public final String getToolName() {
		return NAME;
	}

	@Override
	public final void handleInteract(PlayerInteractEvent event) {
		Player subject = event.getPlayer();
		String name = subject.getName();
		TreeType type = availableTypes.get(0);
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
				TreeBlockChangeDelegate delegate = new TreeBlockChangeDelegate(
						gc, block.getWorld(), subject);
				if (!block.getWorld().generateTree(block.getLocation(), type,
						delegate)) {
					uPrint(PrintEnum.WARN, subject, ChatColor.RED + "Failed"
							+ " to place the tree at this location");
				}
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
				int typeIntVal = availableTypes.indexOf(type);
				typeIntVal++;
				typeIntVal %= availableTypes.size();
				type = availableTypes.get(typeIntVal);
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
		List<String> availStr = conf.getStringList(tSet + "." + NAME
				+ ".possible");
		List<String> defStr = new ArrayList<String>();
		for (TreeType defType: TreeType.values()) {
			defStr.add(defType.toString());
		}
		if (availStr.isEmpty()) {
			availStr = defStr;
		}
		for (String type: availStr) {
			if (defStr.contains(type)) {
				if (availableTypes.contains(TreeType.valueOf(type))) {
					log.warning("[" + gc.modName + "] " + tSet + "." + NAME
							+ ".possible: '" + type + "': is duplicated");
					return false;
				}
				availableTypes.add(TreeType.valueOf(type));
				if (isDebug()) {
					log.info("[" + gc.modName + "][loadConf] added to " + NAME
							+ ".possible: " + type);
				}
			} else {
				log.warning("[" + gc.modName + "] " + tSet + "." + NAME
						+ ".possible: '" + type + "': is not a TreeType");
				return false;
			}
		}

		return true;
	}

	/**
	 * Class for verifying block placement is respecting player permissions.
	 */
	private class TreeBlockChangeDelegate implements BlockChangeDelegate {

		/**
		 * Initialize block placement verification class for the Tree tool.
		 *
		 * @param gc plugin global variables
		 * @param world what world to verify build rights in
		 * @param subj player to test for block placement permissions
		 */
		TreeBlockChangeDelegate(GlobalConf gc, World world, Player subj) {
			this.gc = gc;
			this.world = world;
			this.subject = subj;
		}

		/**
		 * Reference to plugin global variables.
		 */
		private GlobalConf gc;

		/**
		 * World that block placement is to be verified in.
		 */
		private World world;

		/**
		 * Person to verify block placement permissions.
		 */
		private Player subject;

		@Override
		public int getHeight() {
			return world.getMaxHeight() + 1;
		}

		@Override
		public int getTypeId(int x, int y, int z) {
			return world.getBlockTypeIdAt(x, y, z);
		}

		@Override
		public boolean isEmpty(int x, int y, int z) {
			return world.getBlockAt(x, y, z).getType().equals(Material.AIR);
		}

		@Override
		public boolean setRawTypeId(int x, int y, int z, int typeId) {
			return setRawTypeIdAndData(x, y, z, typeId, 0);
		}

		@Override
		public boolean setRawTypeIdAndData(int x, int y, int z,
				int typeId, int data) {
			Block toChange = world.getBlockAt(x, y, z);
			if (spawnBuild(toChange, subject)) {
				final MaterialData set = new MaterialData(typeId, (byte) data);
				if (gc.useEvent) {
					if (safeReplace(set, toChange, subject, true)) {
						updateUser(subject, toChange.getLocation(), set);
						return true;
					}
				} else {
					toChange.setTypeIdAndData(set.getItemTypeId(),
							set.getData(), false);
					updateUser(subject, toChange.getLocation(), set);
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean setTypeId(int x, int y, int z, int typeId) {
			return setRawTypeIdAndData(x, y, z, typeId, 0);
		}

		@Override
		public boolean setTypeIdAndData(int x, int y, int z, int typeId,
				int data) {
			return setRawTypeIdAndData(x, y, z, typeId, data);
		}

	}
}
