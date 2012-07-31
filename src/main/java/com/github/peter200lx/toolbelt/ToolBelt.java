package com.github.peter200lx.toolbelt;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.peter200lx.toolbelt.tool.Chainsaw;
import com.github.peter200lx.toolbelt.tool.Duplicator;
import com.github.peter200lx.toolbelt.tool.Leap;
import com.github.peter200lx.toolbelt.tool.Paint;
import com.github.peter200lx.toolbelt.tool.Pickhax;
import com.github.peter200lx.toolbelt.tool.Pliers;
import com.github.peter200lx.toolbelt.tool.Ruler;
import com.github.peter200lx.toolbelt.tool.Scroll;
import com.github.peter200lx.toolbelt.tool.Shovel;
import com.github.peter200lx.toolbelt.tool.Sledge;
import com.github.peter200lx.toolbelt.tool.Watch;

public class ToolBelt extends JavaPlugin {
	private Logger log = Logger.getLogger("Minecraft");

	private String cName = "ToolBelt";

	private GlobalConf gc;

	public List<ToolInterface> tools;

	public HashSet<String> tbDisabled;

	@Override
	public void onDisable() {
		// Nothing to do
	}

	@Override
	public void onEnable() {
		if (loadConf()) {
			// Register our events
			getServer().getPluginManager().registerEvents(
					new ToolListener(this), this);

			// Print ToolBelt loaded message
			if (gc.debug) {
				PluginDescriptionFile pdfFile = this.getDescription();
				log.info("[" + cName + "] version " + pdfFile.getVersion()
						+ " is now loaded with debug enabled");
			}
		} else {
			log.warning("[" + cName + "] had an error loading config.yml and"
					+ " is now disabled");
			this.setEnabled(false);
		}
	}

	public boolean onCommand(CommandSender sender, Command cmd,
			String commandLabel, String[] args) {
		// Safety check for determining console status
		boolean console = true;
		if (sender instanceof Player) {
			console = false;
		}

		if ((cmd.getName().equalsIgnoreCase("ToolBelt") || cmd.getName()
				.equalsIgnoreCase("tb")) && (args.length == 1)) {
			if (args[0].contentEquals("reload")) {
				if (hasAdminPerm(sender, cName.toLowerCase() + ".reload")) {
					if (!console) {
						log.info("[" + cName + "] " + sender.getName()
								+ " Just ran /toolbelt reload");
					}
					this.reloadConfig();
					if (loadConf()) {
						sender.sendMessage("Configuration file config.yml has"
								+ " been reloaded");
					} else {
						sender.sendMessage("[WARNING] Configuration file load"
								+ " error, check console logs");
						tools = new ArrayList<ToolInterface>();
						sender.sendMessage("[WARNING] Tools have been disabled"
								+ " until a valid config file is loaded");
						if (!console) {
							log.warning(" Tools have been disabled until "
									+ "a valid config file is loaded");
						}
					}
				} else {
					sender.sendMessage("You don't have permission to reload"
							+ " the config file");
				}
				return true;
			} else if (args[0].contentEquals("tools")) {
				if (console) {
					sender.sendMessage("This command only usable by players");
				} else {
					if (tbDisabled.contains(sender.getName())) {
						sender.sendMessage("ToolBelt is disabled for you,"
								+ " type '/tb on' to enable it");
						return true;
					}
					Boolean any = false;
					for (ToolInterface tool : tools) {
						if (tool.printUse(sender)) {
							any = true;
						}
					}
					if (!any) {
						sender.sendMessage("There are currently no tools "
								+ "that you can access");
					}
				}
				return true;
			} else if (args[0].contentEquals("off")) {
				if (console) {
					sender.sendMessage("This command only usable by players");
				} else {
					if (tbDisabled.contains(sender.getName())) {
						sender.sendMessage("You have already disabled ToolBelt"
								+ " for yourself");
					} else {
						sender.sendMessage(ChatColor.RED + "ToolBelt tools are"
								+ " now disabled for you, '/tb on' to restore");
					}
					tbDisabled.add(sender.getName());
				}
				return true;
			} else if (args[0].contentEquals("on")) {
				if (console) {
					sender.sendMessage("This command only usable by players");
				} else {
					if (tbDisabled.contains(sender.getName())) {
						sender.sendMessage(ChatColor.GREEN
								+ "You have re-enabled ToolBelt for yourself,"
								+ " type '/tb tools' for more information");
					} else {
						sender.sendMessage("ToolBelt is already enabled.");
					}
					tbDisabled.remove(sender.getName());
				}
				return true;
			}
		}
		return false;
	}

	private Boolean hasAdminPerm(CommandSender p, String what) {
		if (gc.perm) {
			return p.hasPermission(what);
		} else {
			return p.isOp();
		}
	}

	private boolean loadConf() {
		String tSet = "tools";
		// Load and/or initialize configuration file
		if (!this.getConfig().isSet(tSet)) {
			this.saveDefaultConfig();
			log.info("[" + cName + "][loadConf] config.yml copied from .jar"
					+ " (likely first run)");
			this.reloadConfig();
		}

		// Set up a list so that they can disable ToolBelt for themselves
		tbDisabled = new HashSet<String>();

		// Reload and hold config for this function
		FileConfiguration conf = this.getConfig();

		// Load all global configuration settings
		gc = loadGlobalConf(conf, tSet);
		if (gc == null) {
			return false;
		}

		// Initialize all available tools
		HashMap<String, AbstractTool> available =
				new HashMap<String, AbstractTool>();
		available.put(Duplicator.NAME, new Duplicator(gc));
		available.put(Scroll.NAME, new Scroll(gc));
		available.put(Paint.NAME, new Paint(gc));
		available.put(Leap.NAME, new Leap(gc));
		available.put(Pickhax.NAME, new Pickhax(gc));
		available.put(Ruler.NAME, new Ruler(gc));
		available.put(Watch.NAME, new Watch(gc));
		available.put(Sledge.NAME, new Sledge(gc));
		available.put(Pliers.NAME, new Pliers(gc));
		available.put(Shovel.NAME, new Shovel(gc));
		available.put(Chainsaw.NAME, new Chainsaw(gc));

		// Pull in config.yml .bind section for desired tools and bindings.
		ConfigurationSection sect = conf.getConfigurationSection(
				tSet + ".bind");

		if (sect == null) {
			log.warning("[" + cName + "] " + tSet + ".bind is returning null");
			return false;
		}

		// Read what tools the admin wants, and what items to bind them to.
		List<ToolInterface> holdTool = new ArrayList<ToolInterface>();
		for (Entry<String, Object> entry : sect.getValues(false).entrySet()) {
			if (entry.getValue() instanceof Number) {
				int id = ((Number) entry.getValue()).intValue();
				if (id > 0) {
					Material type = Material.getMaterial(id);
					if ((type != null)
							&& (available.containsKey(entry.getKey()))) {
						// At this point we know we have a valid bind item, and
						// that we know what tool they are talking about
						if (!holdTool.contains(available.get(entry.getKey()))) {
							available.get(entry.getKey()).setType(type);
							holdTool.add(available.get(entry.getKey()));
							if (gc.debug) {
								log.info("[" + cName + "][loadConf] tools: "
										+ entry.getKey() + " is now " + type);
							}
							continue;
						} else {
							log.warning("[" + cName + "] " + tSet + ".bind."
									+ entry.getKey() + ": '" + entry.getValue()
									+ "' has a duplicate id of another tool");
							return false;
						}
					}
					if (!available.containsKey(entry.getKey())) {
						log.warning("[" + cName + "] " + tSet + ".bind."
								+ entry.getKey() + ": Is not a known tool");
					}
				}
			}
			log.warning("[" + cName + "] " + tSet + ".bind." + entry.getKey()
					+ ": '" + entry.getValue() + "' is not a Material type");
			// No return false; here so that an admin can disable tools
			// by setting them to zero and such
		}
		tools = holdTool;

		// Load tool specific configuration
		for (ToolInterface tool : tools) {
			if (!tool.loadConf(tSet, conf)) {
				return false;
			}
			tool.saveHelp(this);
		}

		// Create help/Permissions.txt listing all permissions from plugin.yml
		printPerm("help/Permissions.txt", gc.ranks);

		// Create help/Commands.txt listing all commands from plugin.yml
		printCommands("help/Commands.txt");

		this.saveResource("help/Ranks.txt", true);

		// The return value for the above commands is not checked as we don't
		// need to disable the plugin if they don't succeed.

		return true;
	}

	private GlobalConf loadGlobalConf(ConfigurationSection conf, String tSet) {
		// Check and set the debug printout flag
		boolean debug = conf.getBoolean("debug", false);
		if (debug) {
			log.info("[" + cName + "][loadGlobalConf] Debugging is enabled");
		} else if (((gc == null) ? false : gc.debug)) {
			log.info("[" + cName + "][loadGlobalConf] Debugging is now"
					+ " disabled");
		}

		// Check and set the permissions flag
		boolean permissions = conf.getBoolean("permissions", true);
		if (debug) {
			log.info("[" + cName + "][loadGlobalConf] permmissions are "
					+ permissions);
		}

		// Check and set the useEvent flag
		boolean useEvent = conf.getBoolean("useEvent", true);
		if (debug) {
			log.info("[" + cName + "][loadGlobalConf] The plugin will use"
					+ " Block Events: " + useEvent);
		}

		// Check and load the Ranks object
		Ranks ranks = loadRanks(conf, debug, permissions);
		if (ranks == null) {
			return null;
		}

		// Check and load the user print level from config
		PrintEnum printLevel = loadUserPrint(conf, debug);
		if (printLevel == null) {
			return null;
		}

		String globalName = "global";

		// Load Global repeat delay
		int repeatDelay;
		repeatDelay = conf.getInt(tSet + "." + globalName + ".repeatDelay",
				125);
		if (repeatDelay < 0) {
			log.warning("[" + cName + "] " + tSet + "." + globalName
					+ ".repeatDelay has an invalid value of " + repeatDelay);
			log.warning("[" + cName + "] (The global delay must be greater"
					+ " than or equal to zero)");
			return null;
		}
		if (debug) {
			log.info("[" + cName + "][loadGlobalConf] Global tool use repeat"
					+ " delay is " + repeatDelay);
		}

		// Initialize global protection lists
		SetMat onlyAllow = new SetMat(log, cName, "onlyAllow");
		SetMat stopCopy = new SetMat(log, cName, "stopCopy");
		SetMat stopOverwrite = new SetMat(log, cName, "stopOverwrite");

		// Pull in global protection lists ranked section
		String rankName = "ranks";
		ConfigurationSection rankConf = conf.getConfigurationSection(tSet + "."
				+ globalName + "." + rankName);

		// Load onlyAllow global protection list
		List<Integer> intL = conf.getIntegerList(tSet + "." + globalName
				+ ".onlyAllow");

		if (!onlyAllow.loadMatList(intL, false, tSet + "." + globalName)) {
			return null;
		}
		if (debug) {
			onlyAllow.logMatSet("loadGlobalConf", globalName);
			if (onlyAllow.isEmpty()) {
				log.info("[" + cName + "][loadGlobalConf] As onlyAllow is"
						+ " empty, all non-restricted materials are allowed");
			} else {
				log.info("[" + cName + "][loadGlobalConf] As onlyAllow "
						+ "has items, only those materials can be painted");
			}
		}
		if (!onlyAllow.loadRankedMatLists(rankConf, ranks, globalName + "."
				+ rankName)) {
			return null;
		}
		if (debug) {
			onlyAllow.logRankedMatSet("loadGlobalConf",
					globalName + "." + rankName);
		}

		// Load stopCopy global protection list
		intL = conf.getIntegerList(tSet + "." + globalName + ".stopCopy");

		if (!stopCopy.loadMatList(intL, true, tSet + "." + globalName)) {
			return null;
		}
		if (debug) {
			stopCopy.logMatSet("loadGlobalConf", globalName);
		}
		if (!stopCopy.loadRankedMatLists(rankConf, ranks, globalName + "."
				+ rankName)) {
			return null;
		}
		if (debug) {
			stopCopy.logRankedMatSet("loadGlobalConf",
					globalName + "." + rankName);
		}

		// Load stopOverwrite global protection list
		intL = conf.getIntegerList(tSet + "." + globalName + ".stopOverwrite");

		if (!stopOverwrite.loadMatList(intL, true, tSet + "." + globalName)) {
			return null;
		}
		if (debug) {
			stopOverwrite.logMatSet("loadGlobalConf", globalName);
		}
		if (!stopOverwrite.loadRankedMatLists(rankConf, ranks, globalName + "."
				+ rankName)) {
			return null;
		}
		if (debug) {
			stopOverwrite.logRankedMatSet("loadGlobalConf",
					globalName + "." + rankName);
		}

		// Store settings into global config for use outside of loadConf()
		return new GlobalConf(cName, this.getServer(), debug, permissions,
				useEvent, repeatDelay, onlyAllow, stopCopy, stopOverwrite,
				ranks, printLevel);
	}

	private Ranks loadRanks(ConfigurationSection conf, boolean debug,
			boolean permissions) {
		Ranks ranks = null;
		try {
			ranks = new Ranks(permissions
					? conf.getConfigurationSection("ranksDef") : null, cName);
		} catch (RuntimeException e) {
			log.warning(e.getMessage());
			return null;
		}
		if (debug) {
			log.info("[" + cName
					+ "][loadRanks] Below is a listing of the defined ranks");
			ranks.printRanks(log);
		}
		// Register permissions for newly loaded ranks (toolbelt.rank.rankname)
		for (String rank : ranks.getRanks()) {
			if (getServer().getPluginManager().getPermission(
					ranks.getPrefix() + rank) == null) {
				getServer().getPluginManager().addPermission(new Permission(
						ranks.getPrefix() + rank, "auto-gen rank perm: " + rank,
								PermissionDefault.FALSE));
			}
		}
		return ranks;
	}

	private PrintEnum loadUserPrint(ConfigurationSection conf, boolean debug) {
		int printLevelInt = conf.getInt("userPrint", PrintEnum.DEBUG.getPri());
		PrintEnum printLevel = null;
		for (PrintEnum level : PrintEnum.values()) {
			if (level.getPri() == printLevelInt) {
				printLevel = level;
			}
		}
		if (printLevel == null) {
			log.warning("[" + cName + "][loadUserPrint] " + printLevelInt
					+ " is not a valid userPrint level.");
			return null;
		}
		if (debug) {
			log.info("[" + cName + "][loadUserPrint] The current user print"
					+ " level is " + printLevel);
		}
		return printLevel;
	}

	private boolean printPerm(String outName, Ranks ranks) {
		List<Permission> perms = this.getDescription().getPermissions();
		File outFile = new File(this.getDataFolder(), outName);
		try {
			if (!outFile.getParentFile().exists()) {
				outFile.getParentFile().mkdirs();
			}

			PrintWriter out = new PrintWriter(outFile);

			for (Permission perm : perms) {
				out.write(perm.getName() + "\n\t" + perm.getDescription()
						+ "\n");
				switch (perm.getDefault()) {
				case TRUE:
					out.write("\t\tBy default this is given to everyone\n");
					break;
				case NOT_OP:
					out.write("\t\tBy default this is given to all who are not"
							+ " server OPs\n");
					break;
				default:
				}
				Map<String, Boolean> children = perm.getChildren();
				if (!children.isEmpty()) {
					out.write("\t\tHas Children:\n");
					for (Entry<String, Boolean> child : children.entrySet()) {
						out.write("\t\t\t" + child.getKey() + "\t: "
								+ child.getValue() + "\n");
					}
				}
			}
			for (String rank : ranks.getRanks()) {
				Permission perm = this.getServer().getPluginManager()
						.getPermission(ranks.getPrefix() + rank);
				out.write(perm.getName() + "\n\t" + perm.getDescription()
						+ "\n");
			}
			out.close();
		} catch (IOException e) {
			log.warning("[" + cName + "] Was not able to save"
					+ " help/Permissions.txt");
			return false;
		}
		return true;
	}

	private boolean printCommands(String outName) {
		Map<String, Map<String, Object>> commands =
				this.getDescription().getCommands();
		File outFile = new File(this.getDataFolder(), outName);
		try {
			if (!outFile.getParentFile().exists()) {
				outFile.getParentFile().mkdirs();
			}

			PrintWriter out = new PrintWriter(outFile);

			for (Entry<String, Map<String, Object>> entry : commands
					.entrySet()) {
				out.write("\nCommand: " + entry.getKey() + "\n");
				if (entry.getValue().containsKey("description")) {
					out.write("\ndescription:\n"
							+ entry.getValue().get("description") + "\n");
				}
				if (entry.getValue().containsKey("usage")) {
					String use = (String) entry.getValue().get("usage");
					out.write("\nusage:\n");
					if (use.length() > 0) {
						for (String line : use.replace("<command>",
								entry.getKey()).split("\n")) {
							out.write(line + "\n");
						}
					}
				}
				if (entry.getValue().containsKey("aliases")) {
					out.write("\ncommand alias(es):\n"
							+ entry.getValue().get("aliases") + "\n");
				}
				out.write("\n####################################\n");
			}
			out.close();
		} catch (IOException e) {
			log.warning("[" + cName + "] Was not able to save"
					+ " help/Commands.txt");
			return false;
		}
		return true;
	}

}
