package com.github.peter200lx.toolbelt;

import org.bukkit.Server;

public class GlobalConf {

	public GlobalConf(String modName, Server server, boolean debug,
			boolean perm, boolean useEvent, int repeatDelay,
			SetMat onlyAllow, SetMat stopCopy, SetMat stopOverwrite) {
		super();
		this.modName = modName;
		this.server = server;
		this.debug = debug;
		this.perm = perm;
		this.useEvent = useEvent;
		this.repeatDelay = repeatDelay;
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

	public final SetMat onlyAllow;

	public final SetMat stopCopy;

	public final SetMat stopOverwrite;

}
