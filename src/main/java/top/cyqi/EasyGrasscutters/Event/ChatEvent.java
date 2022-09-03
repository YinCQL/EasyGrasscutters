package top.cyqi.EasyGrasscutters.Event;

import emu.grasscutter.net.packet.PacketOpcodes;
import emu.grasscutter.net.proto.ChatInfoOuterClass;
import emu.grasscutter.net.proto.PrivateChatNotifyOuterClass;
import emu.grasscutter.server.event.game.SendPacketEvent;
import emu.grasscutter.utils.EventConsumer;
import org.json.JSONObject;
import top.cyqi.EasyGrasscutters.Event.content.ChatEvent_t;
import top.cyqi.EasyGrasscutters.websocket.WebSocketServer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static emu.grasscutter.Grasscutter.getLogger;

public class ChatEvent implements EventConsumer<SendPacketEvent> {

    public static Map<String, ChatEvent_t> All_Chat = new HashMap<>();

    @Override
    public void consume(SendPacketEvent sendPacketEvent) {
        // TODO ȷ�����ݰ�����
        int Opcode = sendPacketEvent.getPacket().getOpcode();
        if (Opcode == PacketOpcodes.PrivateChatNotify) {
            // ȷ����˽�����ݰ�
            //getLogger().info("��ҷ���˽�����ݰ�");
            try {
                PrivateChatNotifyOuterClass.PrivateChatNotify privateChatNotify = PrivateChatNotifyOuterClass.PrivateChatNotify.parseFrom(sendPacketEvent.getPacket().getData());//�������ݰ�
                if (!privateChatNotify.hasChatInfo()) {
                    //��Ч���ݰ�
                    return;
                }

                ChatInfoOuterClass.ChatInfo chatInfo = privateChatNotify.getChatInfo();//��ȡ��������
                ChatInfoOuterClass.ChatInfo.ContentCase content = chatInfo.getContentCase();//��ȡ�����������������ж������ֻ��Ǳ���
                JSONObject json = new JSONObject();
                json.put("type", "OnChat");
                json.put("from", chatInfo.getUid());
                json.put("to", chatInfo.getToUid());
                if (content == ChatInfoOuterClass.ChatInfo.ContentCase.TEXT) {
                    json.put("msg_type", "TEXT");
                    json.put("data", chatInfo.getText());

                    for (String key : All_Chat.keySet()) {
                        ChatEvent_t ChatEvent_a = All_Chat.get(key);
                        if (ChatEvent_a.check(String.valueOf(chatInfo.getUid()), String.valueOf(chatInfo.getToUid()), chatInfo.getText())) {
                            All_Chat.remove(key);
                        }
                    }

                } else if (content == ChatInfoOuterClass.ChatInfo.ContentCase.ICON) {
                    json.put("msg_type", "ICON");
                    json.put("data", chatInfo.getIcon());
                }

                WebSocketServer.ClientContextMap.keySet().stream().filter(ctx -> ctx.session.isOpen()).forEach(session -> session.send(json.toString()));


            } catch (Exception e) {
                getLogger().error("����������������:" + e.getMessage());
            }

        }
    }

    public static void delete(String msg_id) {
        for (String key : All_Chat.keySet()) {
            ChatEvent_t ChatEvent_a = All_Chat.get(key);
            if (ChatEvent_a.msg_id.equals(msg_id)) {
                All_Chat.remove(key);
            }

        }
    }

    public static void add_ChatEvent(ChatEvent_t ChatEvent_a) {
        String uuid = UUID.randomUUID().toString();
        All_Chat.put(uuid, ChatEvent_a);
    }

}
