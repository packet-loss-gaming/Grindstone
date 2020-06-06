package gg.packetloss.grindstone.chatbridge;

import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@ComponentInformation(friendlyName = "Chat Bridge", desc = "Integrate grindstone with chat bridges softly.")
public class ChatBridgeComponent extends BukkitComponent {
    private List<Consumer<String>> messageConsumers = new ArrayList<>();
    private List<BiConsumer<String, String>> playerMessageConsumers = new ArrayList<>();

    @Override
    public void enable() {
        integrateWithTelegram();
    }

    private void integrateWithTelegram() {
        try {
            Object telegramBotInst = Class.forName("gg.packetloss.telegrambot.BotComponent").getMethod("getBot").invoke(null);

            // Find methods
            Method sendToSyncChannels = telegramBotInst.getClass().getMethod("sendMessageToSyncChannels", String.class);
            Method sentToSyncChannelsPlayer = telegramBotInst.getClass().getMethod("sendMessageToSyncChannels", String.class, String.class);

            // Register consumers
            messageConsumers.add((message) -> {
                try {
                    sendToSyncChannels.invoke(message);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            });
            playerMessageConsumers.add((fromUser, message) -> {
                try {
                    sentToSyncChannelsPlayer.invoke(fromUser, message);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            });
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {

        }
    }

    public void broadcast(String message) {
        for (Consumer<String> messageConsumer : messageConsumers) {
            messageConsumer.accept(message);
        }
    }

    public void sendMessageToSyncChannels(String fromUser, String message) {
        for (BiConsumer<String, String> playerMessageConsumer : playerMessageConsumers) {
            playerMessageConsumer.accept(fromUser, message);
        }
    }
}
