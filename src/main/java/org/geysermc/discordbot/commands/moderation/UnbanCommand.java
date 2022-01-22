/*
 * Copyright (c) 2020-2022 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/GeyserDiscordBot
 */

package org.geysermc.discordbot.commands.moderation;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.geysermc.discordbot.GeyserBot;
import org.geysermc.discordbot.storage.ServerSettings;
import org.geysermc.discordbot.util.BotColors;
import org.geysermc.discordbot.util.BotHelpers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UnbanCommand extends Command {

    public UnbanCommand() {
        this.name = "unban";
        this.hidden = true;
        this.userPermissions = new Permission[] { Permission.BAN_MEMBERS };
    }

    @Override
    protected void execute(CommandEvent event) {
        List<String> args = new ArrayList<>(Arrays.asList(event.getArgs().split(" ")));

        // Fetch the user
        User user = BotHelpers.getUser(args.remove(0));

        // Check user is valid
        if (user == null) {
            event.getMessage().replyEmbeds(new EmbedBuilder()
                    .setTitle("Invalid user")
                    .setDescription("The user ID specified doesn't link with any valid user in this server.")
                    .setColor(BotColors.FAILURE.getColor())
                    .build()).queue();
            return;
        }

        // Check if the user is banned
        try {
            event.getGuild().retrieveBan(user).complete();
        } catch (ErrorResponseException ignored) {
            event.getMessage().replyEmbeds(new EmbedBuilder()
                    .setTitle("User not banned")
                    .setDescription("The user ID specified doesn't have a ban on this server.")
                    .setColor(BotColors.FAILURE.getColor())
                    .build()).queue();
            return;
        }

        boolean silent = false;

        // Handle all the option args
        // We clone the args here to prevent a CME
        for (String arg : args.toArray(new String[0])) {
            if (!arg.startsWith("-") || arg.length() < 2) {
                break;
            }

            if (arg.toCharArray()[1] == 's') {
                // Check for silent flag
                silent = true;
            } else {
                event.getMessage().replyEmbeds(new EmbedBuilder()
                        .setTitle("Invalid option")
                        .setDescription("The option `" + arg + "` is invalid")
                        .setColor(BotColors.FAILURE.getColor())
                        .build()).queue();
            }

            args.remove(0);
        }

        // Get the reason or use None
        String reasonParts = String.join(" ", args);
        String reason;
        if (reasonParts.trim().isEmpty()) {
            reason = "*None*";
        } else {
            reason = reasonParts;
        }

        // Let the user know they're unbanned if we are not being silent
        if (!silent) {
            user.openPrivateChannel().queue((channel) ->
                    channel.sendMessageEmbeds(new EmbedBuilder()
                            .setTitle("You have been unbanned from GeyserMC!")
                            .addField("Reason", reason, false)
                            .setTimestamp(Instant.now())
                            .setColor(BotColors.SUCCESS.getColor())
                            .build()).queue(message -> {}, throwable -> {}), throwable -> {});
        }

        // Unban user
        event.getGuild().unban(user).queue();

        // Log the change
        int id = GeyserBot.storageManager.addLog(event.getMember(), "unban", user, reason);

        MessageEmbed unbannedEmbed = new EmbedBuilder()
                .setTitle("Unbanned user")
                .addField("User", user.getAsMention(), false)
                .addField("Staff member", event.getAuthor().getAsMention(), false)
                .addField("Reason", reason, false)
                .setFooter("ID: " + id)
                .setTimestamp(Instant.now())
                .setColor(BotColors.SUCCESS.getColor())
                .build();

        // Send the embed as a reply and to the log
        ServerSettings.getLogChannel(event.getGuild()).sendMessageEmbeds(unbannedEmbed).queue();
        event.getMessage().replyEmbeds(unbannedEmbed).queue();
    }
}
