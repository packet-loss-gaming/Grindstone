package gg.packetloss.grindstone.util.chat;

import com.sk89q.minecraft.util.commands.CommandException;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.bukkittext.TextAction;
import gg.packetloss.bukkittext.TextBuilder;
import net.md_5.bungee.api.chat.BaseComponent;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Optional;

public abstract class TextComponentChatPaginator<T> {
    private final ChatColor headerColor;
    private final String headerText;

    protected static final int PER_PAGE = 9;

    public TextComponentChatPaginator(ChatColor headerColor, String headerText) {
        this.headerColor = headerColor;
        this.headerText = headerText;
    }

    public abstract Optional<String> getPagerCommand(int page);

    public void display(CommandSender sender, List<T> collection, int page) throws CommandException {
        display(sender, new CollectionPaginationProvider<>(collection), page);
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

    public void display(CommandSender sender, PaginationProvider<T> results, int page) throws CommandException {
        if (results.getNumberOfPages() == 0) {
            throw new CommandException("No results match!");
        } else {
            int lastPage = results.getNumberOfPages();
            page = Math.max(1, Math.min(page, lastPage));

            sender.sendMessage(Text.of(headerColor, StringUtils.center(headerText, ChatConstants.CHAT_WIDTH)).build());

            results.forEachItemInPage(page, (item) -> sender.sendMessage(format(item)));

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

    public abstract BaseComponent[] format(T item);
}
