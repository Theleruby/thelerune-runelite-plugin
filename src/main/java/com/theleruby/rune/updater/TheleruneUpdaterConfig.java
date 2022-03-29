package com.theleruby.rune.updater;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("theleruneupdater")
public interface TheleruneUpdaterConfig extends Config
{
	@Range(min = 0)
	@ConfigItem(
			keyName = "minXP",
			name = "Min XP",
			description = "Minimum XP to send update"
	)
	default int minXP()
	{
		return 10000;
	}

	@ConfigItem(
		keyName = "apiKey",
		name = "API Key",
		description = "Required to submit update"
	)
	default String apiKey()
	{
		return "";
	}
}
