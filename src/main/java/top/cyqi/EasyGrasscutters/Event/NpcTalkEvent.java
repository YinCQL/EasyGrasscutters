package top.cyqi.EasyGrasscutters.Event;

import emu.grasscutter.net.packet.PacketOpcodes;
import emu.grasscutter.net.proto.NpcTalkReqOuterClass;
import emu.grasscutter.server.event.game.SendPacketEvent;
import emu.grasscutter.utils.EventConsumer;
import org.json.JSONObject;
import top.cyqi.EasyGrasscutters.websocket.WebSocketServer;

import static emu.grasscutter.Grasscutter.getLogger;

public class NpcTalkEvent implements EventConsumer<SendPacketEvent> {


    @Override
    public void consume(SendPacketEvent sendPacketEvent) {
        // TODO ȷ�����ݰ�����
        int Opcode = sendPacketEvent.getPacket().getOpcode();
        if (Opcode == PacketOpcodes.NpcTalkRsp) {
            // ȷ����NPC�Ի����ݰ�
            try {
                NpcTalkReqOuterClass.NpcTalkReq talkInfo = NpcTalkReqOuterClass.NpcTalkReq.parseFrom(sendPacketEvent.getPacket().getData());
                JSONObject json = new JSONObject();
                json.put("type", "OnNpcTalk");
                json.put("npc_id", talkInfo.getNpcEntityId());
                json.put("TalkId", talkInfo.getTalkId());
                json.put("data", true);
                WebSocketServer.ClientContextMap.keySet().stream().filter(ctx -> ctx.session.isOpen()).forEach(session -> session.send(json.toString()));
            } catch (Exception e) {
                getLogger().error("NPC�Ի��������������:" + e.getMessage());
            }
        }
    }

}
