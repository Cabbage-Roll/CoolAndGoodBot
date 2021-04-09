package cag;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import cag.packets.Packet;
import cag.packets.bidi.SocialPresence;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack.UnpackerConfig;

import cag.packets.bidi.SocialDm;
import cag.packets.server.SocialOnline;

import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

public class PacketReceivingProcess extends Packet {

    public PacketReceivingProcess(byte[] packet) throws IOException {
        setValues(packetToMap(packet));
        react();
    }

    private void react() throws IOException {
        String command = find("command").asStringValue().asString();
        switch (command) {
        case "social.dm":
            reactToMessage();
            break;
        default:
            System.out.println("Unhandled command " + command);
        }
    }

    private void reactToMessage() throws IOException {
        String content = find("data.data.content").asStringValue().asString();
        String sender = find("data.stream").asStringValue().asString().split(":")[1];

        byte[] packet;
        Map<Value, Value> base = new HashMap<Value, Value>();
        Map<Value, Value> data = new HashMap<Value, Value>();

        switch (content) {
            case "get real":
                data.put(ValueFactory.newString("status"), ValueFactory.newString("online"));
                data.put(ValueFactory.newString("detail"), ValueFactory.newString(""));
                base.put(ValueFactory.newString("command"), ValueFactory.newString("social.presence"));
                base.put(ValueFactory.newString("data"), ValueFactory.newMap(data));
                packet = Packet.mapToPacket(base);
                Main.instance.sendPacket(packet);
                break;
            case "get fake":
                data.put(ValueFactory.newString("status"), ValueFactory.newString("offline"));
                data.put(ValueFactory.newString("detail"), ValueFactory.newString(""));
                base.put(ValueFactory.newString("command"), ValueFactory.newString("social.presence"));
                base.put(ValueFactory.newString("data"), ValueFactory.newMap(data));
                packet = Packet.mapToPacket(base);
                Main.instance.sendPacket(packet);
                break;
            case "help":
                data.put(ValueFactory.newString("recipient"), ValueFactory.newString(find("data.data.user").asStringValue().asString()));
                data.put(ValueFactory.newString("msg"), ValueFactory.newString(SocialDm.helpString));
                base.put(ValueFactory.newString("command"), ValueFactory.newString("social.dm"));
                base.put(ValueFactory.newString("data"), ValueFactory.newMap(data));
                packet = Packet.mapToPacket(base);
                Main.instance.sendPacket(packet);
                break;
            case "toString()":
                data.put(ValueFactory.newString("recipient"), ValueFactory.newString(find("data.data.user").asStringValue().asString()));
                data.put(ValueFactory.newString("msg"), ValueFactory.newString(getValues().toString()));
                base.put(ValueFactory.newString("command"), ValueFactory.newString("social.dm"));
                base.put(ValueFactory.newString("data"), ValueFactory.newMap(data));
                packet = Packet.mapToPacket(base);
                Main.instance.sendPacket(packet);
                break;
        }
    }
}
