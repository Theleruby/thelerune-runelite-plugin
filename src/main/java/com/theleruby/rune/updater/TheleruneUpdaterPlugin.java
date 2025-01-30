package com.theleruby.rune.updater;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import okhttp3.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;

@Slf4j
@PluginDescriptor(name = "TheleRune Updater", description = "Updates TheleRune on logout", enabledByDefault = false)
public class TheleruneUpdaterPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private TheleruneUpdaterConfig config;

	@Inject
	private OkHttpClient okHttpClient;

	private long lastAccountHash;
	private boolean shouldProcessLogin;
	private long lastExperienceValue;
	HashSet<WorldType> unsupportedWorldTypes;

	@Override
	protected void startUp() throws Exception
	{
		shouldProcessLogin = true;
		lastAccountHash = -1L;
		unsupportedWorldTypes = new HashSet<>();
		unsupportedWorldTypes.add(WorldType.SEASONAL);
		unsupportedWorldTypes.add(WorldType.DEADMAN);
		unsupportedWorldTypes.add(WorldType.NOSAVE_MODE);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		GameState state = gameStateChanged.getGameState();
		if (state == GameState.LOGGED_IN && lastAccountHash != client.getAccountHash())
		{
			lastAccountHash = client.getAccountHash();
			shouldProcessLogin = true;
		}
		else if (state == GameState.LOGIN_SCREEN)
		{
			Player local = client.getLocalPlayer();
			if (local == null)
			{
				return;
			}
			for (WorldType worldType : client.getWorldType())
			{
				if (unsupportedWorldTypes.contains(worldType))
				{
					log.debug("Unsupported world type {}", worldType.name());
					return;
				}
			}
			long totalExperienceValue = client.getOverallExperience();
			long experienceDiff = totalExperienceValue - lastExperienceValue;
			if (experienceDiff >= 0 && experienceDiff >= config.minXP())
			{
				log.info("Submitting update for {}", local.getName());
				HttpUrl url = new HttpUrl.Builder().scheme("https").host("rune.theleruby.com").addPathSegment("api")
                        .addPathSegment("update").addPathSegment(local.getName().replace(" ", "_"))
                        .addPathSegment("").addQueryParameter("api_key", config.apiKey()).build();
				FormBody.Builder postDataBuilder = new FormBody.Builder();
				for(Skill skill : Skill.values()) {
					postDataBuilder.add(skill.name().toLowerCase(Locale.ROOT), String.valueOf(client.getSkillExperience(skill)));
				}
				int collectionsLoggedValue = client.getVarpValue(2943);
				if (collectionsLoggedValue > 0) {
					postDataBuilder.add("collections_logged", String.valueOf(collectionsLoggedValue));
				}
				RequestBody post = postDataBuilder.build();
				Request request = new Request.Builder().header("User-Agent", "RuneLite").url(url).post(post).build();
				okHttpClient.newCall(request).enqueue(new Callback()
				{
					@Override
					public void onFailure(Call call, IOException e)
					{
						log.warn("Updating TheleRune failed ({})", e.getMessage());
					}

					@Override
					public void onResponse(Call call, Response response)
					{
						log.debug("Update said {}", response.message());
						response.close();
					}
				});
				lastExperienceValue = totalExperienceValue;
			}
			else
			{
				log.debug("Not enough XP earned to submit update ({} of {} earned)", experienceDiff, config.minXP());
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		if (shouldProcessLogin)
		{
			lastExperienceValue = client.getOverallExperience();
			shouldProcessLogin = false;
		}
	}

	@Provides
	TheleruneUpdaterConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TheleruneUpdaterConfig.class);
	}
}
