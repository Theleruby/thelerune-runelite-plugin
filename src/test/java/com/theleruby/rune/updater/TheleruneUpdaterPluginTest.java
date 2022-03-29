package com.theleruby.rune.updater;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class TheleruneUpdaterPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(TheleruneUpdaterPlugin.class);
		RuneLite.main(args);
	}
}