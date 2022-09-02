package top.cyqi.EasyGrasscutters.Event;

import emu.grasscutter.net.packet.PacketOpcodes;
import emu.grasscutter.net.proto.PlayerChatReqOuterClass;
import emu.grasscutter.server.event.game.SendPacketEvent;
import emu.grasscutter.utils.EventConsumer;
import org.json.JSONObject;
import top.cyqi.EasyGrasscutters.websocket.WebSocketServer;

import static emu.grasscutter.Grasscutter.getLogger;

public class ChatEvent implements EventConsumer<SendPacketEvent> {


    @Override
    public void consume(SendPacketEvent sendPacketEvent) {
        // TODO ȷ�����ݰ�����
        int Opcode = sendPacketEvent.getPacket().getOpcode();
        if (Opcode == PacketOpcodes.PrivateChatNotify) {
            // ȷ����˽�����ݰ�
            //getLogger().info("��ҷ���˽�����ݰ�");
            try {
                PlayerChatReqOuterClass.PlayerChatReq chatInfo = PlayerChatReqOuterClass.PlayerChatReq.parseFrom(sendPacketEvent.getPacket().getData());

                // �Ҳ���⣿ ΪʲôhasChatInfo()���ص���false��
                // û�취ֻ�� toString ��
                /* > 7: {
                          7: 99
                          13: 1662120300
                          15: 666777
                          1946: "3"
                        }
                 */

                //�ָ��ı�
//                Integer ToUid = Integer.valueOf(resolveString(chatInfo.toString(),"  7: "));
//                Integer FromUid = Integer.valueOf(resolveString(chatInfo.toString(),"  15: "));
//                int star = chatInfo.toString().indexOf("  1946: \"") + 9;
//                int end = chatInfo.toString().indexOf("\"\n",star);
//                String msg =  chatInfo.toString().substring(star, end);

                // ����JSON
                JSONObject json = new JSONObject();
                json.put("type", "OnChat");
                json.put("data", chatInfo.toString());
                WebSocketServer.ClientContextMap.keySet().stream().filter(ctx -> ctx.session.isOpen()).forEach(session -> session.send(json.toString()));


            } catch (Exception e) {
                getLogger().error("����������������:" + e.getMessage());
            }

        }
    }

    // ��˺���ݰ�
    public String resolveString(String str, String key) {
        int star = str.indexOf(key) + key.length();
        int end = str.indexOf("\n", star);
        return str.substring(star, end);
    }
}
