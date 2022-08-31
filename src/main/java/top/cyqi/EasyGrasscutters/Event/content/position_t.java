package top.cyqi.EasyGrasscutters.Event.content;

import emu.grasscutter.game.player.Player;
import emu.grasscutter.utils.Position;
import io.javalin.websocket.WsMessageContext;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;


public class position_t {

    //����
    public Position position;
    //�뾶
    public Float radius;
    //����
    public Integer sceneId;
    //�Ƿ������Ҽ��
    public Integer player_uid = -1;

    public String msg_id;
    public WsMessageContext wsMessageContext;

    public position_t(String Msg_id, Position position, Float radius, Integer sceneId, WsMessageContext wsMessageContext) {
        if (Msg_id == null || position == null || radius == null || sceneId == null || wsMessageContext == null)
            throw new RuntimeException("position_t��������Ϊ��");
        this.msg_id = Msg_id;
        this.position = position;
        this.radius = radius;
        this.sceneId = sceneId;
        this.wsMessageContext = wsMessageContext;
    }

    public boolean check(Position input_position, Player player) {
        if (player_uid != -1) {
            if (player.getUid() != player_uid)
                return false;
        }
        if (player.getSceneId() == this.sceneId) {
            if (getDistance(input_position, this.position) <= radius) {
                JSONObject temp = new JSONObject();
                temp.put("type", "OnPosition");
                temp.put("msg_id", this.msg_id);
                temp.put("data", player.getUid());
                wsMessageContext.send(temp.toString());
                return true;
            }
        }
        return false;
    }

    // �������빫ʽ
    public static float getDistance(@NotNull Position p1, @NotNull Position p2) {
        return (float) Math.sqrt(Math.abs((p1.getX() - p2.getX()) * (p1.getX() - p2.getX()) + (p1.getY() - p2.getY()) * (p1.getY() - p2.getY())));
    }
}
