package com.tr.server;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class TRServer extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("TRServer 1.0 已启用！");
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
    }

    // 处理告示牌编辑事件
    @EventHandler
    public void onSignEdit(SignChangeEvent event) {
        Player player = event.getPlayer();
        String firstLine = ChatColor.stripColor(event.getLine(0));
        
        // 验证是否是服务器告示牌
        if (firstLine.equalsIgnoreCase("[Server]")) {
            String address = ChatColor.stripColor(event.getLine(1)).trim();
            
            if (address.isEmpty()) {
                // 实时反馈错误信息
                event.setLine(0, ChatColor.RED + "[Server]");
                player.sendMessage(ChatColor.RED + "警告: 服务器地址未填写！");
            } else {
                // 验证地址格式
                if (isValidAddress(address)) {
                    event.setLine(0, ChatColor.DARK_BLUE + "[Server]");
                    player.sendMessage(ChatColor.GREEN + "服务器告示牌设置成功！");
                } else {
                    event.setLine(0, ChatColor.RED + "[Server]");
                    player.sendMessage(ChatColor.RED + "警告: 服务器地址格式无效！");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Sign)) return;
        
        Sign sign = (Sign) block.getState();
        String[] lines = sign.getLines();
        
        // 验证是否为有效的服务器跳转告示牌
        String firstLine = ChatColor.stripColor(lines[0]);
        if (!firstLine.equalsIgnoreCase("[Server]")) return;
        
        Player player = event.getPlayer();
        String address = ChatColor.stripColor(lines[1]).trim();
        
        if (address.isEmpty()) {
            player.sendMessage(ChatColor.RED + "错误: 服务器地址未填写！");
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
                player.sendMessage(ChatColor.RED + "警告: 端口格式无效！使用默认端口 25565");
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
            player.sendMessage(ChatColor.RED + "错误: 无法发送连接请求！");
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
            getLogger().warning("连接错误: " + e.getMessage());
            return false;
        }
    }
    
    private boolean isValidAddress(String address) {
        // 简单的地址格式验证
        return !address.contains(" ") && address.contains(".") && address.length() > 5;
    }
}
