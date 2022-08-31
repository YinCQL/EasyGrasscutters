package top.cyqi.EasyGrasscutters.Event.content;

import emu.grasscutter.game.player.Player;
import io.javalin.websocket.WsMessageContext;
import org.json.JSONObject;

public class PlayerExp_t {

    //�Ƿ������Ҽ��
    public Integer player_uid = -1;
    public Integer experience; //Ŀ�꾭��
    public String msg_id;
    public WsMessageContext wsMessageContext;

    public PlayerExp_t(String Msg_id, Integer experience, WsMessageContext wsMessageContext) {
        if (Msg_id == null || experience == null || wsMessageContext == null)
            throw new RuntimeException("position_t��������Ϊ��");
        this.msg_id = Msg_id;
        this.experience = experience;
        this.wsMessageContext = wsMessageContext;
    }

    public boolean check(Player player) {
        if (player_uid != -1) {
            if (player.getUid() != player_uid)
                return false;
        }

        if (player.getExp() >= this.experience) {
            // ��Ҿ������Ŀ�꾭�鴥��
            JSONObject temp = new JSONObject();
            temp.put("msg_id", msg_id);
            temp.put("type", "OnPlayerExp");
            temp.put("Exp", player.getExp());
            temp.put("data", player.getUid());
            wsMessageContext.send(temp.toString());
            return true;
        }
        return false;
    }
}
