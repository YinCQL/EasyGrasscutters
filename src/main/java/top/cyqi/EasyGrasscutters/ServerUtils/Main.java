package top.cyqi.EasyGrasscutters.ServerUtils;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.command.CommandMap;
import emu.grasscutter.game.player.Player;
import io.javalin.websocket.WsMessageContext;
import org.json.JSONObject;

import static top.cyqi.EasyGrasscutters.EasyGrasscutters.getGameServer;

public class Main {

    public static void DealMessage(JSONObject object, WsMessageContext wsMessageContext) {

        String type = object.getString("type");

        switch (type) {
            case "CMD" -> {
                /*
                 * { "type":"CMD","ִ�е�����":"help","player_uid":"ִ����uid(�����������ڿ���ִ̨��)","msg_id":"�����" }
                 * */
                String rawMessage = object.getString("cmd");
                if (object.has("player_uid")) {
                    String player_uid = object.getString("player_uid");

                    Player tmp = getGameServer().getPlayers().get(Integer.valueOf(player_uid));
                    if (tmp == null) {
                        JSONObject temp = new JSONObject();
                        temp.put("type", "error");
                        temp.put("msg_id", object.getString("msg_id"));
                        temp.put("data", "�û�������");
                        wsMessageContext.send(temp.toString());
                        return;
                    }
                    QMessageHandler resultCollector = new QMessageHandler();
                    resultCollector.wsMessageContext = wsMessageContext;
                    resultCollector.player = tmp;
                    resultCollector.msg_id = object.getString("msg_id");
                    tmp.setMessageHandler(resultCollector);
                    ExecuteCommand(tmp, rawMessage);
                } else {
                    ExecuteCommand(null, rawMessage);
                }
            }
            case "GetPlayerNum" -> {
                int number = getGameServer().getPlayers().size();
                JSONObject temp = new JSONObject();
                temp.put("type", "GetPlayerNum");
                temp.put("msg_id", object.getString("msg_id"));
                temp.put("data", number);
                wsMessageContext.send(temp.toString());
                return;
            }
        }
    }

    public static void ExecuteCommand(Player player, String data) {
        try {
            CommandMap commandMap = Grasscutter.getCommandMap();
            commandMap.invoke(player, player, data);
        } catch (Exception e) {
            Grasscutter.getLogger().info("[EasyGrasscutters] ִ������:" + data + "��������:" + e.getMessage());
        }
    }
}
