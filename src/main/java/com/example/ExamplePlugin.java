package com.example;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.Player;
import net.runelite.api.Actor;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.ImageCapture;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.awt.image.BufferedImage;

@Slf4j
@PluginDescriptor(
	name = "Example"
)
public class ExamplePlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ExampleConfig config;

    @Inject
	private ImageCapture imageCapture;

	private WorldPoint lastPosition;
    private int totalDistanceTraveled = 0;
    private long lastExportTime = 0;
    private static final long EXPORT_INTERVAL_MS = 1000; // Export every 1 second
	private boolean inCombat = false;
	private int deaths = 0;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Data server started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Data server stopped!");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "~~~~~ RECORDING SESSION DATA ~~~~~", null);
		}
	}

    @Subscribe
    public void onGameTick(GameTick gameTick)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        WorldPoint currentPosition = client.getLocalPlayer().getWorldLocation();
        
        if (lastPosition != null && !lastPosition.equals(currentPosition))
        {
            int distanceMoved = lastPosition.distanceTo2D(currentPosition);
            totalDistanceTraveled += distanceMoved;
        }
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastExportTime >= EXPORT_INTERVAL_MS) {
            exportGameState();
            lastExportTime = currentTime;
        }
        
        lastPosition = currentPosition;
    }

	@Subscribe
	public void onActorDeath(ActorDeath actorDeath)
	{
		Actor actor = actorDeath.getActor();
		if (actor instanceof Player) {
			Player player = (Player) actor;
			if (player == client.getLocalPlayer()) {
				deaths++;
			}
		}
	}

    private void exportGameState() {
        try {
            String playerName = client.getLocalPlayer() != null ? client.getLocalPlayer().getName() : "Unknown";
            int hitpoints = client.getBoostedSkillLevel(Skill.HITPOINTS);
            int maxHitpoints = client.getRealSkillLevel(Skill.HITPOINTS);
			int combatExperience = client.getSkillExperience(Skill.ATTACK) + client.getSkillExperience(Skill.STRENGTH) + client.getSkillExperience(Skill.DEFENCE) + client.getSkillExperience(Skill.HITPOINTS) + client.getSkillExperience(Skill.MAGIC) + client.getSkillExperience(Skill.RANGED) + client.getSkillExperience(Skill.PRAYER);
            imageCapture.takeScreenshot(null, "game_state", true, false, false);

            String json = String.format(
                "{\n" +
                "  \"timestamp\": %d,\n" +
                "  \"player_name\": \"%s\",\n" +
				"  \"game_state\": \"%s\",\n" +
                "  \"total_distance_traveled\": %d,\n" +
                "  \"hitpoints\": {\n" +
                "    \"current\": %d,\n" +
                "    \"max\": %d\n" +
                "  },\n" +
				"  \"combat_xp\": %d,\n" +
				"  \"in_combat\": %b,\n" +
				"  \"deaths\": %d\n" +
                "}",
                System.currentTimeMillis(),
                playerName,
				client.getGameState().toString(),
                totalDistanceTraveled,
                hitpoints,
                maxHitpoints,
				combatExperience,
				inCombat,
				deaths
            );

            String filePath = Paths.get(System.getProperty("user.home"), "runelite_game_state.json").toString();
            try (FileWriter writer = new FileWriter(filePath)) {
                writer.write(json);
                writer.flush();
            }
            
        } catch (IOException e) {
            log.error("Failed to export game state", e);
        }
    }

	@Provides
	ExampleConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ExampleConfig.class);
	}
}
