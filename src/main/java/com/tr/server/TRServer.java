package com.tr.server;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class TRServer extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        // 注册BungeeCord通信通道
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("TRServer v1.0 已启用！");
        getLogger().info("支持版本: 1.20 - 1.21");
    }

    @Override
    public void onDisable() {
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(this);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Sign)) return;
        
        Sign sign = (Sign) block.getState();
        String[] lines = sign.getLines();
        
        // 验证是否为有效的服务器跳转告示牌
        if (!ChatColor.stripColor(lines[0]).equalsIgnoreCase("[Server]")) return;
        
        Player player = event.getPlayer();
        String address = ChatColor.stripColor(lines[1]).trim();
        
        if (address.isEmpty()) {
            player.sendMessage(ChatColor.RED + "服务器地址未填写！");
            return;
        }

        // 解析地址和端口
        String[] addressParts = address.split(":");
        String host = addressParts[0];
        int port = 25565; // 默认Minecraft端口
        
        if (addressParts.length > 1) {
            try {
                port = Integer.parseInt(addressParts[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "端口格式无效！使用默认端口 25565");
            }
        }
        
        // 显示描述信息（如果有）
        String description = ChatColor.stripColor(lines[2]).trim();
        if (!description.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "服务器描述: " + description);
        }
        
        // 发送连接请求
        if (sendConnectRequest(player, host, port)) {
            player.sendMessage(ChatColor.GREEN + "正在连接到服务器 " + host + ":" + port + "...");
        } else {
            player.sendMessage(ChatColor.RED + "连接请求发送失败！");
        }
    }
    
    private boolean sendConnectRequest(Player player, String host, int port) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            
            out.writeUTF("Connect");
            out.writeUTF(host);
            out.writeInt(port);
            
            player.sendPluginMessage(this, "BungeeCord", b.toByteArray());
            return true;
        } catch (IOException e) {
            getLogger().warning("发送连接请求时发生错误: " + e.getMessage());
            return false;
        }
    }
}
