package com.github.peter200lx.toolbelt;

import java.util.HashSet;

import org.bukkit.Material;
import org.bukkit.Server;

public class GlobalConf {

	public GlobalConf(String modName, Server server, boolean debug,
			boolean perm, boolean useEvent, int repeatDelay,
			HashSet<Material> defStop, HashSet<Material> onlyAllow,
			HashSet<Material> stopCopy, HashSet<Material> stopOverwrite) {
		super();
		this.modName = modName;
		this.server = server;
		this.debug = debug;
		this.perm = perm;
		this.useEvent = useEvent;
		this.repeatDelay = repeatDelay;
		this.defStop = defStop;
		this.onlyAllow = onlyAllow;
		this.stopCopy = stopCopy;
		this.stopOverwrite = stopOverwrite;
	}

	public final String modName;

	public final Server server;

	public final boolean debug;

	public final boolean perm;

	public final boolean useEvent;

	public final int repeatDelay;

	public final HashSet<Material> defStop;

	public final HashSet<Material> onlyAllow;

	public final HashSet<Material> stopCopy;

	public final HashSet<Material> stopOverwrite;

}
