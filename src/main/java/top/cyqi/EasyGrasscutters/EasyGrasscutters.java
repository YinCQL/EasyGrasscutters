package top.cyqi.EasyGrasscutters;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.auth.DefaultAuthentication;
import emu.grasscutter.config.ConfigContainer;
import emu.grasscutter.plugin.Plugin;
import emu.grasscutter.plugin.api.ServerHook;
import emu.grasscutter.server.event.EventHandler;
import emu.grasscutter.server.event.HandlerPriority;
import emu.grasscutter.server.event.entity.EntityDeathEvent;
import emu.grasscutter.server.event.game.SendPacketEvent;
import emu.grasscutter.server.event.player.PlayerJoinEvent;
import emu.grasscutter.server.event.player.PlayerMoveEvent;
import emu.grasscutter.server.event.player.PlayerQuitEvent;
import emu.grasscutter.server.event.types.PlayerEvent;
import emu.grasscutter.server.game.GameServer;
import emu.grasscutter.server.http.HttpServer;
import top.cyqi.EasyGrasscutters.Event.*;
import top.cyqi.EasyGrasscutters.ServerUtils.QConsoleListAppender;
import top.cyqi.EasyGrasscutters.utils.Utils;
import top.cyqi.EasyGrasscutters.websocket.WebSocketServer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;


public class EasyGrasscutters extends Plugin {

    public static Config config;
    public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private File configFile;
    private WebSocketServer webSocketServer;

    //ע�����λ���¼�������
    EventHandler<PlayerMoveEvent> serverPositionEvent;
    //ע��ʵ������������
    EventHandler<EntityDeathEvent> serverKillEntityEvent;
    //ע�����ı������
    EventHandler<PlayerEvent> serverQuestEvent;
    //ע����ҽ��������
    EventHandler<PlayerJoinEvent> serverJoinEvent;
    //ע������˳�������
    EventHandler<PlayerQuitEvent> serverQuitEvent;
    //��Ҿ��������
    EventHandler<PlayerEvent> serverPlayerExpEvent;
    //������������
    EventHandler<SendPacketEvent> serverChatEvent;
    //ע��NPC�Ի�������
    EventHandler<SendPacketEvent> serverNpcTalkEvent;

    public static EasyGrasscutters getInstance() {
        return (EasyGrasscutters) Grasscutter.getPluginManager().getPlugin("EasyGrasscutters");
    }

    @Override
    public void onEnable() {

        String pic_str = """
                �q�������r          �q�������r                �q�r �q�r
                ���q�����s          ���q���r��               �q�s�t�רs�t�r
                ���t�����ש����ש����רr �q�r�����u�t�贈�ש����ש����ש����ש����רr�Ǩr�q�ߨr�q�贈���ש��ש����r
                ���q�����Ϩq�r�������ϩ� ���������q���Ϩq�Ϩq�r�������ϩ����Ϩq���ϩ��������u�����������Ϩq�ϩ�����
                ���t�����Ϩq�r�ǩ������t���s�����t�ߩ��������q�r�ǩ����ǩ������t���Ϩt�s���t�r���t�ϩ����ϩ��ǩ�����
                �t�������ߨs�t�ߩ����ߩ��r�q�s�t�������ߨs�t�s�t�ߩ����ߩ����ߩ����ߩ����ߩ��s�t���ߩ����ߨs�t�����s
                          �q���s��
                          �t�����s""";
        System.out.println(pic_str);

        webSocketServer = new WebSocketServer();
        configFile = new File(getDataFolder().toPath() + "/config.json");
        if (!configFile.exists()) {
            try {
                Files.createDirectories(configFile.toPath().getParent());
            } catch (IOException e) {
                getLogger().error("Failed to create config.json");
            }
        }

        loadConfig();
        if (config.token == null || config.token.equals("")) {
            getLogger().info("[EasyGrasscutters] δ��ȡ�������ļ������������ļ�");
            config.token = Utils.generateRandomString(8);
        }
        saveConfig();
        getLogger().info("[EasyGrasscutters] �����ļ��������");

        //ע�����λ���¼�������
        Registered_monitor();
        getLogger().info("[EasyGrasscutters] �¼�������ע�����");

        //ע��websocket����
        webSocketServer.start();
        getLogger().info("[EasyGrasscutters] �����ɹ���");
        getLogger().info("[EasyGrasscutters] ǰ�˰�װ����: https://flows.nodered.org/node/node-red-easy-grasscutters");
        getLogger().info("[EasyGrasscutters] ��������ַ: " + Utils.GetDispatchAddress() + "/easy/" + config.token);
        System.out.println("---------------------------------------");
    }

    public void Registered_monitor() {
        try {
            ListAppender<ILoggingEvent> listAppender = new QConsoleListAppender<>();
            listAppender.start();
            listAppender.setName("EasyGrasscuttersConsole");
            listAppender.start();
            Grasscutter.getLogger().addAppender(listAppender);
        } catch (Exception e) {
            getLogger().error("Զ����־ע��ʧ�ܣ����ܻ��޷���ȡ��������־��" + e.getMessage());
        }

        try {
            serverPositionEvent = new EventHandler<>(PlayerMoveEvent.class);
            serverPositionEvent.listener(new PositionEvent());
            serverPositionEvent.priority(HandlerPriority.NORMAL);
            serverPositionEvent.register(this);
        } catch (Exception e) {
            getLogger().error("ע�����λ���¼����������ִ��󣬿��ܻᵼ�����λ�ô��������ã�" + e.getMessage());
        }

        try {
            serverKillEntityEvent = new EventHandler<>(EntityDeathEvent.class);
            serverKillEntityEvent.listener(new KillEntityEvent());
            serverKillEntityEvent.priority(HandlerPriority.NORMAL);
            serverKillEntityEvent.register(this);
        } catch (Exception e) {
            getLogger().error("ע��ʵ���������������ִ��󣬿��ܻᵼ��ɱ�ִ��������ã�" + e.getMessage());
        }

        try {
            serverQuestEvent = new EventHandler<>(PlayerEvent.class);
            serverQuestEvent.listener(new QuestEvent());
            serverQuestEvent.priority(HandlerPriority.NORMAL);
            serverQuestEvent.register(this);
        } catch (Exception e) {
            getLogger().error("ע�����ı���������ִ��󣬿��ܻᵼ����ɾ��鴥�������ã�" + e.getMessage());
        }

        try {
            serverJoinEvent = new EventHandler<>(PlayerJoinEvent.class);
            serverJoinEvent.listener(new JoinEvent());
            serverJoinEvent.priority(HandlerPriority.NORMAL);
            serverJoinEvent.register(this);
        } catch (Exception e) {
            getLogger().error("ע����ҽ�����������ִ��󣬿��ܻᵼ����ҽ��봥�������ã�" + e.getMessage());
        }

        try {
            serverQuitEvent = new EventHandler<>(PlayerQuitEvent.class);
            serverQuitEvent.listener(new QuitEvent());
            serverQuitEvent.priority(HandlerPriority.NORMAL);
            serverQuitEvent.register(this);
        } catch (Exception e) {
            getLogger().error("ע������˳����������ִ��󣬿��ܻᵼ������˳����������ã�" + e.getMessage());
        }

        try {
            serverPlayerExpEvent = new EventHandler<>(PlayerEvent.class);
            serverPlayerExpEvent.listener(new PlayerExpEvent());
            serverPlayerExpEvent.priority(HandlerPriority.NORMAL);
            serverPlayerExpEvent.register(this);
        } catch (Exception e) {
            getLogger().error("ע����Ҿ�����������ִ��󣬿��ܻᵼ����Ҿ��鴥�������ã�" + e.getMessage());
        }

        //ע��������������
        try {
            serverChatEvent = new EventHandler<>(SendPacketEvent.class);
            serverChatEvent.listener(new ChatEvent());
            serverChatEvent.priority(HandlerPriority.NORMAL);
            serverChatEvent.register(this);
        } catch (Exception e) {
            getLogger().error("ע�����������������ִ��󣬿��ܻᵼ��������촥�������ã�" + e.getMessage());
        }

        //ע��NPC�Ի�������
        try {
            serverNpcTalkEvent = new EventHandler<>(SendPacketEvent.class);
            serverNpcTalkEvent.listener(new NpcTalkEvent());
            serverNpcTalkEvent.priority(HandlerPriority.NORMAL);
            serverNpcTalkEvent.register(this);
        } catch (Exception e) {
            getLogger().error("ע��NPC�Ի����������ִ��󣬿��ܻᵼ��������촥�������ã�" + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        Grasscutter.setAuthenticationSystem(new DefaultAuthentication());
        webSocketServer.stop();
    }

    public void loadConfig() {
        try (FileReader file = new FileReader(configFile)) {
            config = gson.fromJson(file, Config.class);
            saveConfig();
        } catch (Exception e) {
            config = new Config();
            saveConfig();
        }
    }

    public void saveConfig() {
        try (FileWriter file = new FileWriter(configFile)) {
            file.write(gson.toJson(config));
        } catch (Exception e) {
            getLogger().error("�޷����������ļ�!" + e.getMessage());
        }
    }

    public static GameServer getGameServer() {
        return EasyGrasscutters.getInstance().getServer();
    }

    public static ConfigContainer getServerConfig() {
        return Grasscutter.getConfig();
    }

    public static HttpServer getDispatchServer() {
        return ServerHook.getInstance().getHttpServer();
    }
}
