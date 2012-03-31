package com.github.peter200lx.toolbelt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.peter200lx.toolbelt.tool.Duplicator;
import com.github.peter200lx.toolbelt.tool.Leap;
import com.github.peter200lx.toolbelt.tool.Paint;
import com.github.peter200lx.toolbelt.tool.Pickhax;
import com.github.peter200lx.toolbelt.tool.Pliers;
import com.github.peter200lx.toolbelt.tool.Ruler;
import com.github.peter200lx.toolbelt.tool.Scroll;
import com.github.peter200lx.toolbelt.tool.Sledge;
import com.github.peter200lx.toolbelt.tool.Watch;

public class ToolBelt extends JavaPlugin {
	private Logger log = Logger.getLogger("Minecraft");

	private String cName = "ToolBelt";

	private String lowName = cName.toLowerCase();

	private boolean debug = false;

	private boolean permissions;

	private boolean useEvent;

	public List<ToolInterface> tools;

	public HashSet<String> tbDisabled;

	@Override
	public void onDisable() {
		//Nothing to do
	}

	@Override
	public void onEnable() {
		if(loadConf()) {
			// Register our events
			getServer().getPluginManager().registerEvents(
					new ToolListener(this), this);

			//Print yadp loaded message
			if(debug) {
				PluginDescriptionFile pdfFile = this.getDescription();
				log.info( "["+cName + "] version " + pdfFile.getVersion() +
						" is now loaded with debug enabled" );
			}
		} else {
			log.warning( "["+cName+"] had an error loading config.yml and is now disabled");
			this.setEnabled(false);
		}
	}

	public boolean onCommand(CommandSender sender, Command cmd,
			String commandLabel, String[] args){
		//Safety check for determining console status
		boolean console = true;
		if (sender instanceof Player) {
			console = false;
		}

		if((cmd.getName().equalsIgnoreCase("ToolBelt") ||
				cmd.getName().equalsIgnoreCase("tb")   ) &&(args.length == 1)){
			if(args[0].contentEquals("reload")) {
				if(hasAdminPerm(sender,lowName+".reload")) {
					if(!console) log.info("["+cName+"] "+sender.getName()+
							" Just ran /toolbelt reload");
					this.reloadConfig();
					if(loadConf())
						sender.sendMessage("Configuration file config.yml has been reloaded");
					else {
						sender.sendMessage("[WARNING] Configuration file load error, "+
								"check console logs");
						tools = new ArrayList<ToolInterface>();
						sender.sendMessage("[WARNING] Tools have been disabled until "+
								"a valid config file is loaded");
						if(!console) log.warning(" Tools have been disabled until "+
								"a valid config file is loaded");
					}
				} else {
					sender.sendMessage( "You don't have permission to reload the config file");
				}
				return true;
			} else if (args[0].contentEquals("tools")) {
				if (console) {
					sender.sendMessage("This command can only be run by a player");
				} else {
					if(tbDisabled.contains(sender.getName())){
						sender.sendMessage("ToolBelt is disabled for you,"+
								" type '/tb on' to enable it");
						return true;
					}
					Boolean any = false;
					for(ToolInterface tool: tools) {
						if(tool.printUse(sender))
							any = true;
					}
					if(any == false) {
						sender.sendMessage("There are currently no tools " +
								"that you can access");
					}
				}
				return true;
			} else if (args[0].contentEquals("off")) {
				if (console) {
					sender.sendMessage("This command can only be run by a player");
				} else {
					if(tbDisabled.contains(sender.getName()))
						sender.sendMessage("You have already disabled ToolBelt for yourself");
					else
						sender.sendMessage(ChatColor.RED+
								"ToolBelt tools are now disabled for you, type '/tb on' to fix");
					tbDisabled.add(sender.getName());
				}
				return true;
			} else if (args[0].contentEquals("on")) {
				if (console) {
					sender.sendMessage("This command can only be run by a player");
				} else {
					if(tbDisabled.contains(sender.getName()))
						sender.sendMessage(ChatColor.GREEN+
								"You have re-enabled ToolBelt for yourself,"+
								" type '/tb tools' for more information");
					else
						sender.sendMessage("ToolBelt is already enabled.");
					tbDisabled.remove(sender.getName());
				}
				return true;
			}
		}
		return false;
	}

	private Boolean hasAdminPerm(CommandSender p, String what) {
		if(permissions)
			return p.hasPermission(what);
		else if(p.isOp())
			return true;
		else
			return false;
	}

	private boolean loadConf() {
		String tSet = "tools";
		// Load and/or initialize configuration file
		if(!this.getConfig().isSet(tSet)) {
			this.saveDefaultConfig();
			log.info( "["+cName+"][loadConf] config.yml copied from .jar (likely first run)" );
			this.reloadConfig();
		}

		//Set up a list for any users who wish to disable toolbelt for themselves
		tbDisabled = new HashSet<String>();

		//Reload and hold config for this function
		FileConfiguration conf = this.getConfig();

		//Check and set the debug printout flag
		Boolean old = debug;
		debug = conf.getBoolean("debug", false);
		if(debug) log.info( "["+cName+"][loadConf] Debugging is enabled");
		if(old && (!debug))
			log.info("["+cName+"][loadConf] Debugging has been disabled");
		// TODO Auto-generated method stub

		//Check and set the permissions flag
		permissions = conf.getBoolean("permissions", true);
		if(debug) log.info( "["+cName+"][loadConf] permmissions are "+permissions);

		//Check and set the useEvent flag
		useEvent = conf.getBoolean("useEvent", true);
		if(debug) log.info( "["+cName+"][loadConf] The plugin will use Block Events: "+useEvent);

		ConfigurationSection sect = conf.getConfigurationSection(tSet+".bind");

		if(sect == null) {
			log.warning("["+cName+"] "+tSet+".bind is returning null");
			return false;
		}

		HashMap<String,Tool> available = new HashMap<String,Tool>();
		available.put(Duplicator.name, new Duplicator(cName,this.getServer(),
				debug,permissions,useEvent));
		available.put(Scroll.name, new Scroll(cName,this.getServer(),debug,permissions,useEvent));
		available.put(Paint.name, new Paint(cName,this.getServer(),debug,permissions,useEvent));
		available.put(Leap.name, new Leap(cName,this.getServer(),debug,permissions,useEvent));
		available.put(Pickhax.name, new Pickhax(cName,this.getServer(),debug,permissions,useEvent));
		available.put(Ruler.name, new Ruler(cName,this.getServer(),debug,permissions,useEvent));
		available.put(Watch.name, new Watch(cName,this.getServer(),debug,permissions,useEvent));
		available.put(Sledge.name, new Sledge(cName,this.getServer(),debug,permissions,useEvent));
		available.put(Pliers.name, new Pliers(cName,this.getServer(),debug,permissions,useEvent));

		List<ToolInterface> holdTool = new ArrayList<ToolInterface>();
		for(Entry<String, Object> entry :sect.getValues(false).entrySet()) {
			if(entry.getValue() instanceof Number) {
				int id = ((Number)entry.getValue()).intValue();
				if(id > 0) {
					Material type = Material.getMaterial(id);
					if((type != null)&&(available.containsKey(entry.getKey()))) {
						if(!holdTool.contains(available.get(entry.getKey()))) {
							available.get(entry.getKey()).setType(type);
							holdTool.add(available.get(entry.getKey()));
							if(debug) log.info( "["+cName+"][loadConf] tools: " +
									entry.getKey() + " is now " + type);
							continue;
						} else {
							log.warning("["+cName+"] "+tSet+".bind."+entry.getKey()+
									": '"+entry.getValue() + "' has a duplicate " +
									"id of another tool");
							return false;
						}
					}
					if(!available.containsKey(entry.getKey()))
						log.warning("["+cName+"] "+tSet+".bind."+entry.getKey()+
									": Is not a known tool type");
				}
			}
			log.warning("["+cName+"] "+tSet+".bind."+entry.getKey()+
					": '"+entry.getValue() + "' is not a Material type" );
			//No return false; here so that an admin can disable tools
			//    by setting them to zero and such
		}
		tools = holdTool;

		//Load tool specific configuration
		for(ToolInterface tool: tools) {
			if(!tool.loadConf(tSet, conf))
				return false;
			tool.saveHelp(this);
		}

		return true;
	}

}
