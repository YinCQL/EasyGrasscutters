package top.cyqi.EasyGrasscutters.Event;

import emu.grasscutter.game.player.Player;
import emu.grasscutter.server.event.player.PlayerQuitEvent;
import emu.grasscutter.utils.EventConsumer;
import org.json.JSONObject;
import top.cyqi.EasyGrasscutters.websocket.WebSocketServer;

public class QuitEvent implements EventConsumer<PlayerQuitEvent> {

    @Override
    public void consume(PlayerQuitEvent playerQuitEvent) {

        Player player = playerQuitEvent.getPlayer();
        JSONObject temp = new JSONObject();
        //���û�н�ɫ�����ſ�ʼ�������������
        temp.put("type", "OnPlayerQuit");
        temp.put("data", player.getUid());
        WebSocketServer.ClientContextMap.keySet().stream().filter(ctx -> ctx.session.isOpen()).forEach(session -> session.send(temp.toString()));

    }
}