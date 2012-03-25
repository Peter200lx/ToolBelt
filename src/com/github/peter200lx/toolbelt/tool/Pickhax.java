package com.github.peter200lx.toolbelt.tool;

import java.util.HashSet;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import com.github.peter200lx.toolbelt.Tool;

//You will also need to add a line in ToolBelt.java in
// the loadConf() function.
//Put the following line (without the //) after the last similar line
//available.put(Example.name, new Example(cName,debug,permissions));
public class Pickhax extends Tool  {

	public Pickhax(String modName, Server server, boolean debug,
			boolean permissions, boolean useEvent) {
		super(modName, server, debug, permissions, permissions);
		// You shouldn't need to add anything here. However if you have
		//  something you want to setup when the tool is loaded/reloaded
		//  you can put that logic here.
	}

	//This is the string used for the config.yml and plugin.yml files
	public static String name = "phax";
	
    @Override
    public String getToolName() {
        return name;
    }

    @Override
    public void handleInteract(PlayerInteractEvent event){
        Action act = event.getAction();
        if(act.equals(Action.LEFT_CLICK_BLOCK) || act.equals(Action.RIGHT_CLICK_BLOCK)) {
            Block target = event.getClickedBlock();
            if(target != null && !stopOverwrite.contains(target.getType())       &&
                    (onlyAllow.isEmpty() || onlyAllow.contains(target.getType())) ){
                if(spawnBuild(target,event.getPlayer())) {
                    Boolean physics;
                    if(act.equals(Action.LEFT_CLICK_BLOCK))
                        physics = true;
                    else
                        physics = false;
                    if(isUseEvent())
                        safeBreak(target,event.getPlayer(),physics);
                    else
                        target.setTypeId(0,physics);
                }
            }
        }
    }
	
	@Override
	public boolean printUse(CommandSender sender) {
		if(hasPerm(sender)) {
			sender.sendMessage("(Right-,Left-,)Click with the "+getType()+
					" to (description of tool action)");
			//Also add any special case messages here
			return true;
		}
		return false;
	}

	@Override
	public boolean loadConf(String tSet, FileConfiguration conf) {
		//Load the default restriction configuration
		if(!loadGlobalRestrictions(tSet,conf)) {
	        List<Integer> intL = conf.getIntegerList(tSet+"."+name+".onlyAllow");

	        if(!intL.isEmpty())
	        {
	            if(isDebug())
	                log.info( "["+modName+"][loadConf] As "+name+".onlyAllow has items,"+
	                        " it overwrites the global");

	            onlyAllow = loadMatList(intL,new HashSet<Material>(),tSet+"."+name+".onlyAllow");
	            if(onlyAllow == null)
	                return false;

	            if(isDebug()) {
	                logMatSet(onlyAllow,"loadGlobalRestrictions",name+".onlyAllow:");
	                log.info( "["+modName+"][loadConf] As "+name+".onlyAllow has items,"+
	                        " only those materials are usable");
	            }
	        } else if(isDebug()&& !onlyAllow.isEmpty()) {
	            log.info( "["+modName+"][loadConf] As global.onlyAllow has items,"+
	                    " only those materials are usable");
	        }

	        intL = conf.getIntegerList(tSet+"."+name+".stopOverwrite");

	        if(!intL.isEmpty())
	        {
	            if(isDebug())
	                log.info( "["+modName+"][loadConf] As "+name+".stopOverwrite has items,"+
	                        " it overwrites the global");

	            stopOverwrite = loadMatList(intL,defStop(),tSet+"."+name+".stopOverwrite");
	            if(stopOverwrite == null)
	                return false;

	            if(isDebug()) logMatSet(stopOverwrite,"loadGlobalRestrictions",
	                    name+".stopOverwrite:");
	        }
		}
		return true;
	}
}
