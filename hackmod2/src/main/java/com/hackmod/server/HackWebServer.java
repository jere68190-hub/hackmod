package com.hackmod.server;

import com.hackmod.common.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.concurrent.*;

public class HackWebServer {

    public static final int PORT = 7331;
    private static final Logger LOG = LoggerFactory.getLogger("hackmod-server");

    private HttpThread httpThread;
    private HackWsServer wsServer;
    private ScheduledExecutorService scheduler;

    public void start() {
        try {
            httpThread = new HttpThread(PORT);
            httpThread.setDaemon(true);
            httpThread.start();

            wsServer = new HackWsServer(PORT + 1);
            wsServer.setReuseAddr(true);
            wsServer.start();

            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "hackmod-push");
                t.setDaemon(true);
                return t;
            });
            scheduler.scheduleAtFixedRate(this::push, 300, 300, TimeUnit.MILLISECONDS);
            LOG.info("[HackMod] HTTP :{}  WS :{}", PORT, PORT + 1);
        } catch (Exception e) {
            LOG.error("[HackMod] Server start failed", e);
        }
    }

    public void stop() {
        try {
            if (scheduler != null) scheduler.shutdownNow();
            if (wsServer != null) wsServer.stop(1000);
            if (httpThread != null) httpThread.interrupt();
        } catch (Exception ignored) {}
    }

    private void push() {
        if (wsServer == null) return;
        Collection<WebSocket> conns = wsServer.getConnections();
        if (conns.isEmpty()) return;
        String json = buildState();
        for (WebSocket ws : conns) if (ws.isOpen()) ws.send(json);
    }

    private String buildState() {
        MinecraftClient mc = MinecraftClient.getInstance();
        double x=0,y=0,z=0; int fps=0,ping=0;
        String facing="N", dim="overworld", biome="—", server="Solo";

        if (mc != null && mc.player != null) {
            x=mc.player.getX(); y=mc.player.getY(); z=mc.player.getZ();
            fps=mc.getCurrentFps();
            facing=mc.player.getHorizontalFacing().getName().toUpperCase();
            if (mc.world!=null) {
                dim=mc.world.getRegistryKey().getValue().getPath();
                var bk=mc.world.getBiome(mc.player.getBlockPos()).getKey();
                if (bk.isPresent()) biome=bk.get().getValue().getPath().replace("_"," ");
            }
            if (mc.getCurrentServerEntry()!=null) server=mc.getCurrentServerEntry().address;
            if (mc.getNetworkHandler()!=null) {
                var entry=mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (entry!=null) ping=entry.getLatency();
            }
        }

        return String.format(
            "{\"type\":\"state\"," +
            "\"chestEsp\":%b,\"playerEsp\":%b,\"fullbright\":%b," +
            "\"noFall\":%b,\"autoSprint\":%b," +
            "\"coordsHud\":%b,\"armorHud\":%b,\"biomeHud\":%b,\"clockHud\":%b,\"armorAlert\":%b," +
            "\"zoomEnabled\":%b,\"customFovEnabled\":%b," +
            "\"chestEspRadius\":%d,\"customFov\":%d," +
            "\"x\":%.2f,\"y\":%.2f,\"z\":%.2f," +
            "\"facing\":\"%s\",\"dim\":\"%s\",\"biome\":\"%s\",\"fps\":%d,\"ping\":%d,\"server\":\"%s\"}",
            ModConfig.chestEsp.get(), ModConfig.playerEsp.get(), ModConfig.fullbright.get(),
            ModConfig.noFall.get(), ModConfig.autoSprint.get(),
            ModConfig.coordsHud.get(), ModConfig.armorHud.get(), ModConfig.biomeHud.get(),
            ModConfig.clockHud.get(), ModConfig.armorAlert.get(),
            ModConfig.zoomEnabled.get(), ModConfig.customFovEnabled,
            ModConfig.chestEspRadius, ModConfig.customFov,
            x,y,z, facing,dim,biome,fps,ping,server
        );
    }

    // ── WebSocket ─────────────────────────────────────────────────────────────

    private static class HackWsServer extends WebSocketServer {
        HackWsServer(int port) throws UnknownHostException {
            super(new InetSocketAddress("localhost", port));
        }

        @Override public void onOpen(WebSocket c, ClientHandshake h) { LOG.info("[WS] connected"); }
        @Override public void onClose(WebSocket c, int code, String r, boolean remote) {}
        @Override public void onError(WebSocket c, Exception e) {}
        @Override public void onStart() {}

        @Override
        public void onMessage(WebSocket conn, String msg) {
            String type = str(msg, "type");
            if ("toggle".equals(type)) {
                String f = str(msg, "feature");
                boolean v = toggleFeature(f);
                conn.send("{\"type\":\"ack\",\"feature\":\""+f+"\",\"value\":"+v+"}");
            } else if ("set".equals(type)) {
                String f = str(msg, "feature");
                int v = num(msg, "value");
                setFeature(f, v);
                conn.send("{\"type\":\"ack\",\"feature\":\""+f+"\",\"value\":"+v+"}");
            }
        }

        private boolean toggleFeature(String f) {
            return switch (f) {
                case "chestEsp"    -> ModConfig.chestEsp.updateAndGet(v->!v);
                case "playerEsp"   -> ModConfig.playerEsp.updateAndGet(v->!v);
                case "fullbright"  -> ModConfig.fullbright.updateAndGet(v->!v);
                case "noFall"      -> ModConfig.noFall.updateAndGet(v->!v);
                case "autoSprint"  -> ModConfig.autoSprint.updateAndGet(v->!v);
                case "coordsHud"   -> ModConfig.coordsHud.updateAndGet(v->!v);
                case "armorHud"    -> ModConfig.armorHud.updateAndGet(v->!v);
                case "biomeHud"    -> ModConfig.biomeHud.updateAndGet(v->!v);
                case "clockHud"    -> ModConfig.clockHud.updateAndGet(v->!v);
                case "armorAlert"  -> ModConfig.armorAlert.updateAndGet(v->!v);
                case "zoomEnabled" -> ModConfig.zoomEnabled.updateAndGet(v->!v);
                case "customFovEnabled" -> { ModConfig.customFovEnabled=!ModConfig.customFovEnabled; yield ModConfig.customFovEnabled; }
                default -> false;
            };
        }

        private void setFeature(String f, int v) {
            switch (f) {
                case "chestEspRadius" -> ModConfig.chestEspRadius = Math.max(1, Math.min(32, v));
                case "customFov"      -> ModConfig.customFov = Math.max(30, Math.min(120, v));
            }
        }

        private String str(String j, String k) {
            String s="\""+k+"\":\""; int i=j.indexOf(s); if(i<0) return "";
            i+=s.length(); int e=j.indexOf('"',i); return e<0?"":j.substring(i,e);
        }
        private int num(String j, String k) {
            String s="\""+k+"\":"; int i=j.indexOf(s); if(i<0) return 0;
            i+=s.length(); int e=i; while(e<j.length()&&(Character.isDigit(j.charAt(e))||j.charAt(e)=='-'))e++;
            try{return Integer.parseInt(j.substring(i,e));}catch(Exception ex){return 0;}
        }
    }

    // ── HTTP ──────────────────────────────────────────────────────────────────

    private static class HttpThread extends Thread {
        private final ServerSocket ss;
        HttpThread(int port) throws IOException { super("hackmod-http"); ss=new ServerSocket(port); }

        @Override
        public void run() {
            while (!isInterrupted()) {
                try {
                    Socket c=ss.accept();
                    Thread t=new Thread(()->handle(c),"hackmod-req");
                    t.setDaemon(true); t.start();
                } catch (IOException e) { if(!isInterrupted()) LOG.warn("[HTTP] {}",e.getMessage()); }
            }
        }

        private void handle(Socket c) {
            try (c; BufferedReader in=new BufferedReader(new InputStreamReader(c.getInputStream())); OutputStream out=c.getOutputStream()) {
                String req=in.readLine(); if(req==null) return;
                String[] parts=req.split(" "); String path=parts.length>=2?parts[1].split("\\?")[0]:"/";
                String res=switch(path){case "/","index.html"->"/assets/hackmod/web/index.html"; default->null;};
                if(res==null){out.write("HTTP/1.1 404 Not Found\r\nContent-Length:0\r\n\r\n".getBytes());return;}
                try(InputStream s=HackWebServer.class.getResourceAsStream(res)){
                    if(s==null){out.write("HTTP/1.1 404 Not Found\r\nContent-Length:0\r\n\r\n".getBytes());return;}
                    byte[] body=s.readAllBytes();
                    String hdr="HTTP/1.1 200 OK\r\nContent-Type:text/html;charset=utf-8\r\nContent-Length:"+body.length+"\r\nConnection:close\r\n\r\n";
                    out.write(hdr.getBytes(StandardCharsets.UTF_8)); out.write(body);
                }
            } catch (IOException ignored) {}
        }
    }
}
