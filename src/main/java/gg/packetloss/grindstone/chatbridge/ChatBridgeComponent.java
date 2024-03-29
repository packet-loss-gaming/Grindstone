/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.chatbridge;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.AbstractComponent;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.ComponentManager;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.util.ChatUtil;
import org.bukkit.Bukkit;

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
    private List<Consumer<String>> modMessageConsumers = new ArrayList<>();

    @Override
    public void enable() {
        Bukkit.getScheduler().runTaskLater(CommandBook.inst(), this::integrateWithTelegram, 1);
    }

    private void registerDebugIntegration() {
        // We failed to integrate with Telegram, add a sendDebug fallback to handle mod messages
        // that may contain important information (e.g. from the problem reporting component)
        // that would otherwise go nowhere.
        modMessageConsumers.add(ChatUtil::sendDebug);
    }

    private void handleIntegrationNotFound(String integrationName) {
        CommandBook.logger().info(integrationName + " was not found, not integrated.");
        registerDebugIntegration();
    }

    private void handleIncompatibleIntegration(String integrationName, Throwable t) {
        CommandBook.logger().warning(integrationName + " did not integrate properly.");
        t.printStackTrace();

        registerDebugIntegration();
    }

    private AbstractComponent getTelegramBotComponent() {
        ComponentManager<BukkitComponent> componentManager = CommandBook.inst().getComponentManager();
        return componentManager.getComponent("telegram-bot");
    }

    private void integrateWithTelegram() {
        String integrationName = "Telegram chat bridge";

        AbstractComponent componentInst = getTelegramBotComponent();
        if (componentInst == null) {
            handleIntegrationNotFound(integrationName);
            return;
        }

        try {
            // Get the bot instance
            Class<?> telegramBotComponent = componentInst.getClass();
            Object telegramBotInst = telegramBotComponent.getMethod("getBot").invoke(null);

            // Find methods
            Method sendToSyncChannels = telegramBotInst.getClass().getMethod("sendMessageToSyncChannels", String.class);
            Method sentToSyncChannelsPlayer = telegramBotInst.getClass().getMethod("sendMessageToSyncChannels", String.class, String.class);
            Method sendToModChannels = telegramBotInst.getClass().getMethod("sendMessageToModChannels", String.class);

            // Register consumers
            messageConsumers.add((message) -> {
                try {
                    sendToSyncChannels.invoke(telegramBotInst, message);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            });
            playerMessageConsumers.add((fromUser, message) -> {
                try {
                    sentToSyncChannelsPlayer.invoke(telegramBotInst, fromUser, message);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            });
            modMessageConsumers.add((message) -> {
                try {
                    sendToModChannels.invoke(telegramBotInst, message);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            });
        } catch (Throwable t) {
            handleIncompatibleIntegration(integrationName, t);
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

    public void modBroadcast(String message) {
        for (Consumer<String> modMessageConsumer : modMessageConsumers) {
            modMessageConsumer.accept(message);
        }
    }
}
