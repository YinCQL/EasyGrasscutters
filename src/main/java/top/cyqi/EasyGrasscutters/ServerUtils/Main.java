package top.cyqi.EasyGrasscutters.ServerUtils;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.command.CommandMap;
import emu.grasscutter.data.GameData;
import emu.grasscutter.data.excels.GadgetData;
import emu.grasscutter.data.excels.ItemData;
import emu.grasscutter.data.excels.MonsterData;
import emu.grasscutter.database.DatabaseHelper;
import emu.grasscutter.game.entity.EntityItem;
import emu.grasscutter.game.entity.EntityMonster;
import emu.grasscutter.game.entity.EntityVehicle;
import emu.grasscutter.game.entity.GameEntity;
import emu.grasscutter.game.mail.Mail;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.quest.GameMainQuest;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.enums.QuestState;
import emu.grasscutter.game.world.Scene;
import emu.grasscutter.utils.Position;
import io.javalin.websocket.WsMessageContext;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import top.cyqi.EasyGrasscutters.Event.KillEntityEvent;
import top.cyqi.EasyGrasscutters.Event.PlayerExpEvent;
import top.cyqi.EasyGrasscutters.Event.PositionEvent;
import top.cyqi.EasyGrasscutters.Event.QuestEvent;
import top.cyqi.EasyGrasscutters.Event.content.KillEntity;
import top.cyqi.EasyGrasscutters.Event.content.PlayerExp_t;
import top.cyqi.EasyGrasscutters.Event.content.Quest_t;
import top.cyqi.EasyGrasscutters.Event.content.position_t;

import java.util.ArrayList;
import java.util.List;

import static emu.grasscutter.Grasscutter.getLogger;
import static emu.grasscutter.config.Configuration.GAME_OPTIONS;
import static top.cyqi.EasyGrasscutters.EasyGrasscutters.getGameServer;

public class Main {

    public static void DealMessage(@NotNull JSONObject object, WsMessageContext wsMessageContext) {

        String type = object.getString("type");
        Player player = null;
        String player_uid = "";

        try {
            if (object.has("player_uid")) {
                player_uid = object.getString("player_uid");
                player = getGameServer().getPlayerByUid(Integer.parseInt(player_uid), false);
            }
        } catch (Exception e) {
            send_error("���UID����:" + e.getMessage(), object, wsMessageContext);
        }


        switch (type) {
            case "CMD" -> {
                /*
                 * { "type":"CMD","ִ�е�����":"help","player_uid":"ִ����uid(�����������ڿ���ִ̨��)","msg_id":"�����" }
                 * */
                String rawMessage = object.getString("cmd");
                getLogger().info("[EasyGrasscutters] ִ������:" + rawMessage);

                if (object.has("player_uid")) {
                    // �ж���������Ƿ�����
                    if (!player_online(player, object, wsMessageContext)) {
                        return;
                    }
                    try {
                        QMessageHandler resultCollector = new QMessageHandler();
                        resultCollector.wsMessageContext = wsMessageContext;
                        resultCollector.player = player;
                        resultCollector.msg_id = object.getString("msg_id");
                        resultCollector.player_uid = player_uid;
                        player.setMessageHandler(resultCollector);
                        ExecuteCommand(player, rawMessage);
                    } catch (Exception e) {
                        send_error("��������ִ���������:" + e.getMessage(), object, wsMessageContext);
                    }
                } else {
                    try {
                        ExecuteCommand(null, rawMessage);
                    } catch (Exception e) {
                        send_error("����ִ̨���������:" + e.getMessage(), object, wsMessageContext);
                    }
                }

            }
            case "GetPlayerNum" -> {
                getLogger().info("[EasyGrasscutters] ��ȡ�������");
                try {
                    int number = getGameServer().getPlayers().size();
                    JSONObject temp = new JSONObject();
                    temp.put("type", "GetPlayerNum");
                    temp.put("msg_id", object.getString("msg_id"));
                    temp.put("data", number);
                    wsMessageContext.send(temp.toString());
                } catch (Exception e) {
                    send_error("��ȡ�����������:" + e.getMessage(), object, wsMessageContext);
                }
            }
            case "GetPlayerLocation" -> {
                // �ж���������Ƿ�����
                if (!player_online(player, object, wsMessageContext)) {
                    return;
                }

                try {
                    assert player != null;
                    getLogger().info("[EasyGrasscutters] ��ȡ���λ��:" + player.getUid());
                    JSONObject temp = new JSONObject();
                    temp.put("type", "GetPlayerLocation");
                    temp.put("msg_id", object.getString("msg_id"));
                    temp.put("X", player.getPlayerLocationInfo().getPos().getX());
                    temp.put("Y", player.getPlayerLocationInfo().getPos().getY());
                    temp.put("Z", player.getPlayerLocationInfo().getPos().getZ());
                    temp.put("scene", player.getSceneId());
                    temp.put("player_uid", player_uid);
                    wsMessageContext.send(temp.toString());
                } catch (Exception e) {
                    send_error("��ȡ���λ�ó���:" + e.getMessage(), object, wsMessageContext);
                }
            }

            case "GetPlayerBirthday" -> {
                try {
                    player = getGameServer().getPlayerByUid(Integer.parseInt(player_uid), true);
                    if (player == null) {
                        JSONObject temp = new JSONObject();
                        temp.put("type", "error");
                        temp.put("msg_id", object.getString("msg_id"));
                        temp.put("data", "��Ҳ�����");
                        wsMessageContext.send(temp.toString());
                        return;
                    }
                    getLogger().info("[EasyGrasscutters] ��ȡ�������:" + player.getUid());
                    JSONObject temp = new JSONObject();
                    temp.put("type", "GetPlayerBirthday");
                    temp.put("msg_id", object.getString("msg_id"));
                    temp.put("Month", player.getBirthday().getMonth());
                    temp.put("Day", player.getBirthday().getDay());
                    temp.put("player_uid", player_uid);
                    wsMessageContext.send(temp.toString());
                } catch (Exception e) {
                    send_error("��ȡ������ճ���:" + e.getMessage(), object, wsMessageContext);
                }
            }

            case "QuestAction" -> {
                // �ж���������Ƿ�����
                if (!player_online(player, object, wsMessageContext)) {
                    return;
                }


                try {
                    assert player != null;
                    int Quest_id = object.getInt("Quest_id");
                    GameQuest quest = player.getQuestManager().addQuest(Quest_id);
                    getLogger().info("[EasyGrasscutters] ��" + player.getUid() + "���������:" + Quest_id);
                    JSONObject temp = new JSONObject();
                    temp.put("type", "QuestAction");
                    temp.put("msg_id", object.getString("msg_id"));
                    temp.put("data", quest != null);
                    temp.put("player_uid", player_uid);
                    wsMessageContext.send(temp.toString());
                } catch (Exception e) {
                    send_error("����������:" + e.getMessage(), object, wsMessageContext);
                }
            }

            case "QuestFinish" -> {
                // �ж���������Ƿ�����
                if (!player_online(player, object, wsMessageContext)) {
                    return;
                }


                try {
                    assert player != null;
                    int Quest_id = object.getInt("Quest_id");
                    GameQuest quest = player.getQuestManager().addQuest(Quest_id);
                    getLogger().info("[EasyGrasscutters] ��" + player.getUid() + "���������:" + Quest_id);
                    if (quest == null) {
                        JSONObject temp = new JSONObject();
                        temp.put("type", "error");
                        temp.put("msg_id", object.getString("msg_id"));
                        temp.put("data", "��Ҳ����ڸþ���");
                        wsMessageContext.send(temp.toString());
                        return;
                    }
                    quest.finish();
                    JSONObject temp = new JSONObject();
                    temp.put("type", "QuestFinish");
                    temp.put("msg_id", object.getString("msg_id"));
                    temp.put("data", true);
                    temp.put("player_uid", player_uid);
                    wsMessageContext.send(temp.toString());
                } catch (Exception e) {
                    send_error("����������:" + e.getMessage(), object, wsMessageContext);
                }
            }

            case "OnPosition" -> {
                try {
                    String msg_id = object.getString("msg_id");
                    try {
                        if (object.has("del")) {
                            PositionEvent.delete(msg_id);
                            return;
                        }
                    } catch (Exception e) {
                        send_error("ɾ��λ�ü���������:" + e.getMessage(), object, wsMessageContext);
                        return;
                    }

                    float X = object.getFloat("X");
                    float Y = object.getFloat("Y");
                    float Z = object.getFloat("Z");
                    int scene = object.getInt("scene");
                    float R = object.getFloat("R");
                    getLogger().info("[EasyGrasscutters] ���λ�ü�������X=" + X + " Y=" + Y + " Z=" + Z + " scene=" + scene + " R=" + R);
                    Position position = new Position(X, Y, Z);

                    position_t position_a = new position_t(msg_id, position, R, scene, wsMessageContext);
                    // �����������ݰ����������player����player����player����ȥ
                    if (player != null)
                        position_a.player_uid = player.getUid();
                    PositionEvent.add_position(position_a);
                } catch (Exception e) {
                    send_error("���λ�ü���������:" + e.getMessage(), object, wsMessageContext);
                }


            }
            case "ChangePosition" -> {
                // �ж���������Ƿ�����
                if (!player_online(player, object, wsMessageContext)) {
                    return;
                }

                try {
                    assert player != null;
                    float X = object.getFloat("X");
                    float Y = object.getFloat("Y");
                    float Z = object.getFloat("Z");
                    int scene = object.getInt("scene");
                    Position position = new Position(X, Y, Z);
                    getLogger().info("[EasyGrasscutters] �������" + player.getUid() + "�� X=" + X + " Y=" + Y + " Z=" + Z + " scene=" + scene);
                    boolean result = player.getWorld().transferPlayerToScene(player, scene, position);
                    JSONObject temp = new JSONObject();
                    temp.put("type", "QuestFinish");
                    temp.put("msg_id", object.getString("msg_id"));
                    temp.put("data", result);
                    temp.put("player_uid", player_uid);
                    wsMessageContext.send(temp.toString());
                } catch (Exception e) {
                    send_error("������ҳ���:" + e.getMessage(), object, wsMessageContext);
                }
            }
            case "OnKillEntity" -> {
                try {
                    String msg_id = object.getString("msg_id");
                    try {
                        if (object.has("del")) {
                            KillEntityEvent.delete(msg_id);
                            return;
                        }
                    } catch (Exception e) {
                        send_error("ɾ��ʵ����������������:" + e.getMessage(), object, wsMessageContext);
                    }
                    int Entity_id = object.getInt("Entity");
                    getLogger().info("[EasyGrasscutters] ���ʵ������������������ʵ��:" + Entity_id);

                    //�������ʵ�����
                    KillEntity killEntity = new KillEntity(msg_id, Entity_id, wsMessageContext);
                    //���ö�����ӵ�����
                    KillEntityEvent.add_killEntity(killEntity);

                } catch (Exception e) {
                    send_error("���ʵ����������������:" + e.getMessage(), object, wsMessageContext);
                }
            }
            case "CreateEntity" -> {
                // �ж���������Ƿ�����
                if (!player_online(player, object, wsMessageContext)) {
                    return;
                }
                try {
                    assert player != null;
                    int id = object.getInt("id");
                    int amount = object.getInt("amount");
                    int level = object.getInt("level");
                    MonsterData monsterData = GameData.getMonsterDataMap().get(id);
                    GadgetData gadgetData = GameData.getGadgetDataMap().get(id);
                    ItemData itemData = GameData.getItemDataMap().get(id);
                    if (monsterData == null && gadgetData == null && itemData == null) {
                        send_error("��Чʵ��ID", object, wsMessageContext);
                        return;
                    }
                    Scene scene = player.getScene();
                    if (scene.getEntities().size() + amount > GAME_OPTIONS.sceneEntityLimit) {
                        amount = Math.max(Math.min(GAME_OPTIONS.sceneEntityLimit - scene.getEntities().size(), amount), 0);
                        send_error("��ǰ����ʵ�������������ֵ:" + amount, object, wsMessageContext);
                        if (amount <= 0) {
                            return;
                        }
                    }
                    Position center = (player.getPosition());
                    double maxRadius = Math.sqrt(amount * 0.2 / Math.PI);
                    for (int i = 0; i < amount; i++) {
                        Position pos = GetRandomPositionInCircle(center, maxRadius).addY(3);
                        GameEntity entity = null;
                        if (itemData != null) {
                            entity = new EntityItem(scene, null, itemData, pos, 1, true);
                        }
                        if (gadgetData != null) {
                            pos.addY(-3);
                            entity = new EntityVehicle(scene, player, id, 0, pos, player.getRotation());
                        }
                        if (monsterData != null) {
                            entity = new EntityMonster(scene, monsterData, pos, level);
                        }
                        getLogger().info("[EasyGrasscutters] ��" + player.getUid() + "���ʵ��:" + id);
                        scene.addEntity(entity);
                        JSONObject temp = new JSONObject();
                        temp.put("type", "CreateEntity");
                        temp.put("msg_id", object.getString("msg_id"));
                        temp.put("data", entity.getId());
                        temp.put("player_uid", player_uid);
                        wsMessageContext.send(temp.toString());
                    }
                } catch (Exception e) {
                    send_error("���ʵ�����:" + e.getMessage(), object, wsMessageContext);

                }
            }

            case "OnQuestChange" -> {
                try {
                    String msg_id = object.getString("msg_id");
                    try {
                        if (object.has("del")) {
                            QuestEvent.delete(msg_id);
                            return;
                        }
                    } catch (Exception e) {
                        send_error("ɾ�������������������:" + e.getMessage(), object, wsMessageContext);
                        return;
                    }

                    int id = object.getInt("id");
                    QuestState state;
                    String state_str = object.getString("state");
                    switch (state_str) {
                        case "UNSTARTED" -> state = QuestState.QUEST_STATE_UNSTARTED;
                        case "UNFINISHED" -> state = QuestState.QUEST_STATE_UNFINISHED;
                        case "FINISHED" -> state = QuestState.QUEST_STATE_FINISHED;
                        case "FAILED" -> state = QuestState.QUEST_STATE_FAILED;
                        default -> state = QuestState.QUEST_STATE_NONE;
                    }

                    getLogger().info("[EasyGrasscutters] ��Ӿ����������������������:" + id + "����" + state_str);

                    Quest_t Quest_a = new Quest_t(msg_id, id, state, wsMessageContext);
                    // �����������ݰ����������player����player����player����ȥ
                    if (player != null)
                        Quest_a.player_uid = player.getUid();
                    QuestEvent.add_QuestEvent(Quest_a);

                } catch (Exception e) {
                    send_error("��Ӿ����������������:" + e.getMessage(), object, wsMessageContext);
                }
            }
            case "SendMessage" -> {

                // �ж���������Ƿ�����
                if (!player_online(player, object, wsMessageContext)) {
                    return;
                }
                try {
                    assert player != null;
                    String msg = object.getString("message");
                    getGameServer().getChatSystem().sendPrivateMessageFromServer(player.getUid(), msg);
                    getLogger().info("[EasyGrasscutters] ����ҷ�����Ϣ:" + player.getUid());
                    JSONObject temp = new JSONObject();
                    temp.put("type", "SendMessage");
                    temp.put("msg_id", object.getString("msg_id"));
                    temp.put("data", true);
                    temp.put("player_uid", player_uid);
                    wsMessageContext.send(temp.toString());
                } catch (Exception e) {
                    send_error("������Ϣ����:" + e.getMessage(), object, wsMessageContext);
                }
            }
            case "SendMail" -> {
                // �ж���������Ƿ�����
                if (!player_online(player, object, wsMessageContext)) {
                    return;
                }
                try {
                    assert player != null;

                    getLogger().info("[EasyGrasscutters] �����ʼ�:1");

                    //��ȡjson�е�MailContent��ʵ�ֻ�������
                    JSONObject mailContent_obj = object.getJSONObject("MailContent");
                    Mail.MailContent mailContent = new Mail.MailContent();
                    mailContent.sender = mailContent_obj.getString("sender");
                    mailContent.content = mailContent_obj.getString("content");
                    mailContent.title = mailContent_obj.getString("title");

                    getLogger().info("[EasyGrasscutters] �����ʼ�:" + mailContent.title + "����" + player.getUid());

                    //��ȡ��Ʒ��������Ʒ�Ž��ʼ�����
                    List<Mail.MailItem> itemList = new ArrayList<>();
                    JSONArray itemList_Array = object.getJSONArray("itemList");

                    for (int i = 0; i < itemList_Array.length(); i++) {
                        JSONObject itemList_obj = itemList_Array.getJSONObject(i);
                        //{ "itemCount": 10 , "itemId": 10 , "itemLevel" :10 }
                        Mail.MailItem mailItem = new Mail.MailItem();
                        mailItem.itemCount = itemList_obj.getInt("itemCount");
                        mailItem.itemId = itemList_obj.getInt("itemId");
                        mailItem.itemLevel = itemList_obj.getInt("itemLevel");
                        itemList.add(mailItem);
                    }

                    long expireTime = object.getLong("expireTime");
                    Mail mail = new Mail(mailContent, itemList, expireTime);
                    player.sendMail(mail);

                    JSONObject temp = new JSONObject();
                    temp.put("type", "SendMail");
                    temp.put("msg_id", object.getString("msg_id"));
                    temp.put("data", true);
                    temp.put("player_uid", player_uid);
                    wsMessageContext.send(temp.toString());
                } catch (Exception e) {
                    send_error("�����ʼ�����:" + e.getMessage(), object, wsMessageContext);
                }
            }
            case "DelQuest" -> {
                // �ж���������Ƿ�����
                if (!player_online(player, object, wsMessageContext)) {
                    return;
                }
                try {
                    assert player != null;
                    String Quest_id = object.getString("Quest_id");
                    List<GameMainQuest> gameMainQuests = DatabaseHelper.getAllQuests(player);
                    boolean flag = false;
                    for (GameMainQuest gameMainQuest : gameMainQuests) {
                        //�������id��ͬ,����ȫ��ɾ��
                        if (Quest_id.equals("ALL") || Quest_id.contains(gameMainQuest.getParentQuestId() + "")) {
                            flag = true;
                            //ɾ������
                            DatabaseHelper.deleteQuest(gameMainQuest);
                            if (!Quest_id.equals("ALL")) {
                                return;
                            }
                        }
                    }
                    getLogger().info("[EasyGrasscutters] ɾ������:���" + player.getUid());
                    JSONObject temp = new JSONObject();
                    temp.put("type", "DelQuest");
                    temp.put("msg_id", object.getString("msg_id"));
                    temp.put("data", flag);
                    temp.put("player_uid", player_uid);
                    wsMessageContext.send(temp.toString());
                } catch (Exception e) {
                    send_error("ɾ���������:" + e.getMessage(), object, wsMessageContext);
                }
            }
            case "OnPlayerExp" -> {

                try {
                    String msg_id = object.getString("msg_id");
                    try {
                        if (object.has("del")) {
                            PlayerExpEvent.delete(msg_id);
                            return;
                        }
                    } catch (Exception e) {
                        send_error("ɾ����Ҿ������������:" + e.getMessage(), object, wsMessageContext);
                    }

                    getLogger().info("[EasyGrasscutters] �����Ҿ��������");

                    Integer experience = object.getInt("Exp");
                    //�������ʵ�����
                    PlayerExp_t PlayerExp_a = new PlayerExp_t(msg_id, experience, wsMessageContext);
                    // �����������ݰ����������player����player����player����ȥ
                    if (player != null)
                        PlayerExp_a.player_uid = player.getUid();
                    //���ö�����ӵ�����
                    PlayerExpEvent.add_PlayerExpEvent(PlayerExp_a);

                } catch (Exception e) {
                    send_error("������Ҿ������������:" + e.getMessage(), object, wsMessageContext);
                }
            }


        }
    }

    private static Position GetRandomPositionInCircle(Position origin, double radius) {
        Position target = origin.clone();
        double angle = Math.random() * 360;
        double r = Math.sqrt(Math.random() * radius * radius);
        target.addX((float) (r * Math.cos(angle))).addZ((float) (r * Math.sin(angle)));
        return target;
    }

    public static void ExecuteCommand(Player player, String data) {
        try {
            CommandMap commandMap = Grasscutter.getCommandMap();
            commandMap.invoke(player, player, data);
        } catch (Exception e) {
            getLogger().info("[EasyGrasscutters] ִ������:" + data + "��������:" + e.getMessage());
        }
    }

    public static boolean player_online(Player player, JSONObject object, WsMessageContext wsMessageContext) {
        //�ж�����Ƿ����ߣ������߷��ز����߾���
        if (player == null) {
            send_error("��Ҳ�����", object, wsMessageContext);
            return false;
        } else {
            return true;
        }
    }

    public static void send_error(String msg, JSONObject object, WsMessageContext wsMessageContext) {
        JSONObject temp = new JSONObject();
        temp.put("type", "error");
        temp.put("data", msg);
        if (object.has("msg_id"))
            temp.put("msg_id", object.getString("msg_id"));
        wsMessageContext.send(temp.toString());
        getLogger().error("[EasyGrasscutters] ��������:" + msg);
    }


}
