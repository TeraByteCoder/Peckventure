package at.peckventure.rpc;

import net.arikia.dev.drpc.DiscordRPC;
import net.arikia.dev.drpc.DiscordEventHandlers;
import net.arikia.dev.drpc.DiscordRichPresence;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;

public class DiscordPresence {

    public static void start() {
        DiscordEventHandlers handlers = new DiscordEventHandlers.Builder().build();

        DiscordRPC.discordInitialize("1358089026581565570", handlers, true);

        DiscordRichPresence presence = new DiscordRichPresence.Builder("Im Hauptmenü")
            .setDetails("Bereit für ein neues Abenteuer!")
            .setBigImage("mainmenu", "Peckventure")
            .setStartTimestamps(System.currentTimeMillis())
            .build();

        DiscordRPC.discordUpdatePresence(presence);
    }

    public static void update(String state, String details) {
        DiscordRichPresence presence = new DiscordRichPresence.Builder(details)
            .setDetails(state)
            .setBigImage("playing", "Peckventure")
            .setStartTimestamps(System.currentTimeMillis())
            .build();

        DiscordRPC.discordUpdatePresence(presence);
    }

    public static void stop() {
        DiscordRPC.discordShutdown();
    }

    public static void showDiscordButton() {
        JFrame frame = new JFrame("Peckventure Discord");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(300, 120);

        JButton discordButton = new JButton("Unser Discord Server");
        discordButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI("https://discord.gg/deinserver")); // <-- hier dein Invite-Link
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        frame.getContentPane().add(discordButton, BorderLayout.CENTER);
        frame.setVisible(true);
    }
}
