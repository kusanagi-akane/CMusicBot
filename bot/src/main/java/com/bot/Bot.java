package com.bot;

import dev.arbjerg.lavalink.client.LavalinkClient;
import dev.arbjerg.lavalink.client.Link;
import dev.arbjerg.lavalink.client.NodeOptions;
import dev.arbjerg.lavalink.client.player.*;
import dev.arbjerg.lavalink.libraries.jda.JDAVoiceUpdateListener;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.net.URI;
import java.util.List;

public class Bot extends ListenerAdapter {

    private static LavalinkClient lavalink;
    private static JDA jda;

    public static void main(String[] args) {
        try {
            Dotenv dotenv = Dotenv.load();
            String token = dotenv.get("TOKEN");
            String lavalinkUri = dotenv.get("LAVALINK_URI");
            String lavalinkPassword = dotenv.get("LAVALINK_PASSWORD");

            if (token == null || lavalinkUri == null || lavalinkPassword == null) {
                System.err.println("❌ Missing required environment variables!");
                return;
            }


            String userIdBase64 = token.split("\\.")[0];
            long botUserId = Long.parseLong(new String(java.util.Base64.getDecoder().decode(userIdBase64)));
            

            lavalink = new LavalinkClient(botUserId);

            lavalink.addNode(
                new NodeOptions.Builder()
                    .setName("MainNode")
                    .setServerUri(URI.create(lavalinkUri))
                    .setPassword(lavalinkPassword)
                    .build()
            );

            jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_MEMBERS)
                .setVoiceDispatchInterceptor(new JDAVoiceUpdateListener(lavalink))
                .addEventListeners(new Bot())
                .build();

            jda.awaitReady();

            registerCommands();

            System.out.println("✅ Music Bot launch success,Login as " + jda.getSelfUser().getName() + " (ID: " + botUserId + ")");

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void registerCommands() {
        if (jda == null) return;
        jda.updateCommands().addCommands(
            Commands.slash("ping", "Check latency"),
            Commands.slash("play", "Play music").addOption(OptionType.STRING, "query", "URL or keywords", true),
            Commands.slash("stop", "Stop playing"),
            Commands.slash("pause", "Pause"),
            Commands.slash("resume", "Resume")
        ).queue();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "ping" -> event.reply("🏓 Pong! " + event.getJDA().getGatewayPing() + "ms").queue();
            case "play" -> handlePlay(event);
            case "stop" -> handleStop(event);
            case "pause" -> handlePause(event);
            case "resume" -> handleResume(event);
            default -> event.reply("❌ Unknown command").setEphemeral(true).queue();
        }
    }

    private void handlePlay(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        if (member == null) return;

        GuildVoiceState voiceState = member.getVoiceState();
        if (voiceState == null || !voiceState.inAudioChannel()) {
            event.reply("❌ Please join a voice channel first").setEphemeral(true).queue();
            return;
        }

        String query = event.getOption("query").getAsString();
        if (!query.startsWith("http")) {
            query = "ytsearch:" + query;
        }

        event.deferReply().queue();
        

        event.getJDA().getDirectAudioController().connect(voiceState.getChannel());


        Link link = lavalink.getOrCreateLink(event.getGuild().getIdLong());
        
        link.loadItem(query).subscribe(result -> {
            handleLoadResult(event, result, link);
        }, error -> {
            event.getHook().sendMessage("❌ Load error: " + error.getMessage()).queue();
        });
    }

    private void handleLoadResult(SlashCommandInteractionEvent event, LavalinkLoadResult result, Link link) {
        if (result instanceof TrackLoaded trackLoaded) {
            Track track = trackLoaded.getTrack();
            link.createOrUpdatePlayer()
                .setTrack(track)
                .subscribe();
            event.getHook().sendMessage("🎶 Playing: **" + track.getInfo().getTitle() + "**").queue();

        } else if (result instanceof PlaylistLoaded playlistLoaded) {
            List<Track> tracks = playlistLoaded.getTracks();
            
            if (!tracks.isEmpty()) {
                Track firstTrack = tracks.get(0);
                link.createOrUpdatePlayer()
                    .setTrack(firstTrack)
                    .subscribe();
                event.getHook().sendMessage("📋 Playlist: **" + playlistLoaded.getInfo().getName() + "** (" + tracks.size() + " tracks)").queue();
            }

        } else if (result instanceof SearchResult searchResult) {
            List<Track> tracks = searchResult.getTracks();
            if (!tracks.isEmpty()) {
                Track firstTrack = tracks.get(0);
                link.createOrUpdatePlayer()
                    .setTrack(firstTrack)
                    .subscribe();
                event.getHook().sendMessage("🔍 Playing search result: **" + firstTrack.getInfo().getTitle() + "**").queue();
            } else {
                event.getHook().sendMessage("❌ No songs found").queue();
            }

        } else if (result instanceof NoMatches) {
            event.getHook().sendMessage("❌ No matches found").queue();
        } else if (result instanceof LoadFailed loadFailed) {
            event.getHook().sendMessage("❌ Load failed: " + loadFailed.getException().getMessage()).queue();
        }
    }

    private void handleStop(SlashCommandInteractionEvent event) {
        Link link = lavalink.getLinkIfCached(event.getGuild().getIdLong());
        if (link != null) {
            link.createOrUpdatePlayer()
                .setTrack(null)
                .setPaused(false)
                .subscribe();
            link.destroy().subscribe();
            event.getJDA().getDirectAudioController().disconnect(event.getGuild());
            event.reply("⏹️ Stopped playing").queue();
        } else {
            event.reply("❌ Not playing").setEphemeral(true).queue();
        }
    }

    private void handlePause(SlashCommandInteractionEvent event) {
        Link link = lavalink.getLinkIfCached(event.getGuild().getIdLong());
        if (link != null) {
            link.createOrUpdatePlayer()
                .setPaused(true)
                .subscribe();
            event.reply("⏸️ Paused").queue();
        } else {
            event.reply("❌ Not playing").setEphemeral(true).queue();
        }
    }

    private void handleResume(SlashCommandInteractionEvent event) {
        Link link = lavalink.getLinkIfCached(event.getGuild().getIdLong());
        if (link != null) {
            link.createOrUpdatePlayer()
                .setPaused(false)
                .subscribe();
            event.reply("▶️ Resumed").queue();
        } else {
            event.reply("❌ Not playing").setEphemeral(true).queue();
        }
    }
    @Override
    public void onMessageReceived(net.dv8tion.jda.api.events.message.MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }
        String message = event.getMessage().getContentRaw();
        if (message.contains("<@" + event.getJDA().getSelfUser().getId() + ">")) {
            event.getMessage().reply("早安安，這是<@964849855396741130>用咖啡做出的大咖啡\ncnm花了整個下午").queue();
        }
    }
}