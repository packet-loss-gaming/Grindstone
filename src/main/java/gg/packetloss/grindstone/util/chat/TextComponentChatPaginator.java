/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.chat;

import gg.packetloss.bukkittext.Text;
import gg.packetloss.bukkittext.TextAction;
import gg.packetloss.bukkittext.TextBuilder;
import gg.packetloss.grindstone.util.ChatUtil;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public abstract class TextComponentChatPaginator<T> {
    private final ChatColor headerColor;
    private final String headerText;

    public static final int PER_PAGE = 9;

    public TextComponentChatPaginator(ChatColor headerColor, String headerText) {
        this.headerColor = headerColor;
        this.headerText = headerText;
    }

    public abstract Optional<String> getPagerCommand(int page);

    public void display(CommandSender sender, List<T> collection, Function<PaginationProvider<T>, Integer> pageFunction) {
        PaginationProvider<T> paginationProvider = new CollectionPaginationProvider<>(collection);
        display(sender, paginationProvider, pageFunction.apply(paginationProvider));
    }

    public void display(CommandSender sender, List<T> collection, int page) {
        display(sender, collection, (ignored) -> page);
    }

    private Text buildPagerText(String token, int requestedPage, int currentPage) {
        Optional<String> optPagerCommand = getPagerCommand(requestedPage);

        if (requestedPage == currentPage || optPagerCommand.isEmpty()) {
            return Text.of(ChatColor.RED, token);
        }

        return  Text.of(
                ChatColor.BLUE, token,
                TextAction.Click.runCommand(optPagerCommand.get())
        );
    }

    public void display(CommandSender sender, PaginationProvider<T> results, int page) {
        if (results.getNumberOfPages() == 0) {
            ChatUtil.sendError(sender, "No results match!");
        } else {
            int lastPage = results.getNumberOfPages();
            page = Math.max(1, Math.min(page, lastPage));

            sender.sendMessage(Text.of(headerColor, StringUtils.center(headerText, ChatConstants.CHAT_WIDTH)).build());

            results.forEachItemInPage(page, (item) -> sender.sendMessage(format(item).build()));

            if (lastPage == 1) {
                return;
            }

            TextBuilder pagerBuilder = Text.builder();

            pagerBuilder.append(buildPagerText("<<<", 1, page));
            pagerBuilder.append("   ");
            pagerBuilder.append(buildPagerText("<<", Math.max(1, page - 5), page));
            pagerBuilder.append("   ");
            pagerBuilder.append(buildPagerText("<", Math.max(1, page - 1), page));
            pagerBuilder.append("   ");


            String curPageStr = String.valueOf(page);
            String maxPageStr = String.valueOf(lastPage);

            curPageStr = StringUtils.leftPad(curPageStr, maxPageStr.length());

            pagerBuilder.append(Text.of(ChatColor.YELLOW, "Page (", Text.of(ChatColor.GRAY, curPageStr), "/", Text.of(ChatColor.GRAY, maxPageStr), ")"));
            pagerBuilder.append("   ");

            pagerBuilder.append(buildPagerText(">", Math.min(lastPage, page + 1), page));
            pagerBuilder.append("   ");
            pagerBuilder.append(buildPagerText(">>", Math.min(lastPage, page + 5), page));
            pagerBuilder.append("   ");
            pagerBuilder.append(buildPagerText(">>>", lastPage, page));

            sender.sendMessage(pagerBuilder.build());
        }
    }

    public abstract Text format(T item);
}
