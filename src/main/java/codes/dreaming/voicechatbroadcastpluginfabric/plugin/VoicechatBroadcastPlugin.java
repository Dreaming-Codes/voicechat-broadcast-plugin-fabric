package codes.dreaming.voicechatbroadcastpluginfabric.plugin;

import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.packets.StaticSoundPacket;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.network.ServerPlayerEntity;

public class VoicechatBroadcastPlugin implements VoicechatPlugin {
    static String PLUGIN_ID = "voicechat_broadcast";

    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophone);
    }

    /**
     * This method is called whenever a player sends audio to the server via the voice chat.
     *
     * @param event the microphone packet event
     */
    private void onMicrophone(MicrophonePacketEvent event) {
        // The connection might be null if the event is caused by other means
        if (event.getSenderConnection() == null) {
            return;
        }
        // Cast the generic player object of the voice chat API to an actual fabric player
        // This object should always be a fabric player object on fabric-based servers
        if (!(event.getSenderConnection().getPlayer().getPlayer() instanceof ServerPlayerEntity player)) {
            return;
        }

        // Check if the player has the broadcast permission
        if (Permissions.check(player, "voicechat.broadcast", 2)) {
            return;
        }

        Group group = event.getSenderConnection().getGroup();

        // Check if the player sending the audio is actually in a group
        if (group == null) {
            return;
        }

        // Only broadcast the voice when the group name is "broadcast"
        if (!group.getName().strip().equalsIgnoreCase("broadcast")) {
            return;
        }

        // Cancel the actual microphone packet event that people in that group or close by don't hear the broadcaster twice
        event.cancel();

        VoicechatServerApi api = event.getVoicechat();

        IntegratedServer server = MinecraftClient.getInstance().getServer();

        if (server == null) {
            return;
        }

        PlayerManager playerManager = server.getPlayerManager();

        StaticSoundPacket packet = event.getPacket().staticSoundPacketBuilder().build();

        // Iterating over every player on the server
        for (ServerPlayerEntity onlinePlayer : playerManager.getPlayerList()) {
            // Don't send the audio to the player that is broadcasting
            if (onlinePlayer.equals(player)) {
                continue;
            }
            VoicechatConnection connection = api.getConnectionOf(onlinePlayer.getUuid());
            // Check if the player is actually connected to the voice chat
            if (connection == null) {
                continue;
            }

            // Send a static audio packet of the microphone data to the connection of each player
            api.sendStaticSoundPacketTo(connection, packet);
        }
    }
}
