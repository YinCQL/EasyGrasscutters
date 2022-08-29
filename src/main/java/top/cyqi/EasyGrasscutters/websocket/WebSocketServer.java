package top.cyqi.EasyGrasscutters.websocket;

import emu.grasscutter.Grasscutter;
import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import org.json.JSONObject;
import top.cyqi.EasyGrasscutters.EasyGrasscutters;
import top.cyqi.EasyGrasscutters.ServerUtils.Main;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketServer {

    //���ڱ������е����ӵĵ�Map
    public static Map<WsContext, String> ClientContextMap = new ConcurrentHashMap<>();
    Javalin app = EasyGrasscutters.getDispatchServer().getHandle();

    public void start() {

        app.ws("/easy/" + EasyGrasscutters.config.token, ws -> {
            ws.onConnect(ctx -> {
                String ws_id = UUID.randomUUID().toString();
                ClientContextMap.put(ctx, ws_id);
                Grasscutter.getLogger().info("[EasyGrasscutters] ���ӵ���������ID:" + ws_id);
            });

            ws.onMessage(wsMessageContext -> {
                //��ȡ��Ϣ
                String Ws_Msg = wsMessageContext.message();

                JSONObject object;

                try {
                    object = new JSONObject(Ws_Msg);
                } catch (Exception e) {
                    JSONObject temp = new JSONObject();
                    temp.put("type", "error");
                    temp.put("data", "json error");
                    wsMessageContext.send(temp);
                    Grasscutter.getLogger().error("[EasyGrasscutters] �쳣����:" + Ws_Msg + ",����:" + e.getMessage());
                    return;
                }

                //���ú��Ĵ�����
                Main.DealMessage(object, wsMessageContext);

            });

            ws.onClose(ctx -> {
                String ws_id = ClientContextMap.get(ctx);
                ClientContextMap.remove(ctx);
                Grasscutter.getLogger().info("[EasyGrasscutters] ���ӶϿ���ID��" + ws_id);
            });
        });
    }


    public void stop() {
        //�����ҳ����̨���û��б��ر�����
        ClientContextMap.clear();
    }

    public void broadcast(JSONObject data) {
        ClientContextMap.keySet().stream().filter(ctx -> ctx.session.isOpen()).forEach(session -> session.send(data));
    }
}
