package cag;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
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
            case "hello":
                authorize();
                break;
            case "err":
                processError();
                break;
            default:
                System.out.println("Unhandled command " + command);
                break;
        }
    }

    private void processError() {

    }

    private void reactToMessage() throws IOException {


        String content = find("data.data.content").asStringValue().asString();

        byte[] packet;
        String[] words = content.split(" ");
        Map<Value, Value> object = new HashMap<>();
        Map<Value, Value> data = new HashMap<>();
        if(content.startsWith("get real")) {
            data.put(ValueFactory.newString("status"), ValueFactory.newString("online"));
            data.put(ValueFactory.newString("detail"), ValueFactory.newString(""));
            object.put(ValueFactory.newString("command"), ValueFactory.newString("social.presence"));
            object.put(ValueFactory.newString("data"), ValueFactory.newMap(data));
            packet = Packet.mapToPacket(object);
            Main.instance.sendPacket(packet);
        } else if(content.startsWith("get fake")) {
            data.put(ValueFactory.newString("status"), ValueFactory.newString("offline"));
            data.put(ValueFactory.newString("detail"), ValueFactory.newString(""));
            object.put(ValueFactory.newString("command"), ValueFactory.newString("social.presence"));
            object.put(ValueFactory.newString("data"), ValueFactory.newMap(data));
            packet = Packet.mapToPacket(object);
            Main.instance.sendPacket(packet);
        } else if(content.startsWith("help")) {
            data.put(ValueFactory.newString("recipient"), find("data.data.user"));
            data.put(ValueFactory.newString("msg"), ValueFactory.newString("get real - go online\n" + "get fake - go offline\n"
                    + "toString() - call toString method for sent message\n" + "help - this"));
            object.put(ValueFactory.newString("command"), ValueFactory.newString("social.dm"));
            object.put(ValueFactory.newString("data"), ValueFactory.newMap(data));
            packet = Packet.mapToPacket(object);
            Main.instance.sendPacket(packet);
        } else if(content.startsWith("to string")) {
            data.put(ValueFactory.newString("recipient"), find("data.data.user"));
            data.put(ValueFactory.newString("msg"), ValueFactory.newString(getValues().toString()));
            object.put(ValueFactory.newString("command"), ValueFactory.newString("social.dm"));
            object.put(ValueFactory.newString("data"), ValueFactory.newMap(data));
            packet = Packet.mapToPacket(object);
            Main.instance.sendPacket(packet);
        } else if(content.startsWith("get")) {
            data.put(ValueFactory.newString("recipient"), find("data.data.user"));
            String word = words[1];
            String jsonString = Main.getFromApi(word);
            Map map = new Gson().fromJson(jsonString, Map.class);
            data.put(ValueFactory.newString("msg"), ValueFactory.newString((String) map.get("_id")));
            object.put(ValueFactory.newString("command"), ValueFactory.newString("social.dm"));
            object.put(ValueFactory.newString("data"), ValueFactory.newMap(data));
            packet = Packet.mapToPacket(object);
            Main.instance.sendPacket(packet);
        }
    }

    private void authorize() throws IOException {
        byte[] packet;
        Map<Value, Value> object = new HashMap<>();
        Map<Value, Value> data = new HashMap<>();
        Map<Value, Value> handling = new HashMap<>();
        Map<Value, Value> signature = new HashMap<>();
        Map<Value, Value> commit = new HashMap<>();
        Map<Value, Value> build = new HashMap<>();


        object.put(ValueFactory.newString("id"), ValueFactory.newInteger(0));
        object.put(ValueFactory.newString("command"), ValueFactory.newString("authorize"));
        data.put(ValueFactory.newString("token"), ValueFactory.newString(Secret.userToken));
        handling.put(ValueFactory.newString("arr"), ValueFactory.newString("1"));
        handling.put(ValueFactory.newString("das"), ValueFactory.newString("1"));
        handling.put(ValueFactory.newString("sdf"), ValueFactory.newString("41"));
        handling.put(ValueFactory.newString("safelock"), ValueFactory.newBoolean(true));
        data.put(ValueFactory.newString("handling"), ValueFactory.newMap(handling));
        signature.put(ValueFactory.newString("mode"), ValueFactory.newString("production"));
        signature.put(ValueFactory.newString("version"), ValueFactory.newString("6.0.4"));
        signature.put(ValueFactory.newString("countdown"), ValueFactory.newBoolean(false));
        commit.put(ValueFactory.newString("id"), ValueFactory.newString("2d05c95"));
        commit.put(ValueFactory.newString("time"), ValueFactory.newInteger(1617227309000L));
        signature.put(ValueFactory.newString("commit"), ValueFactory.newMap(commit));
        build.put(ValueFactory.newString("id"), ValueFactory.newString("gdlLS4_Vd"));
        build.put(ValueFactory.newString("time"), ValueFactory.newInteger(1617314405656L));
        signature.put(ValueFactory.newString("build"), ValueFactory.newMap(build));
        data.put(ValueFactory.newString("signature"), ValueFactory.newMap(signature));
        object.put(ValueFactory.newString("data"), ValueFactory.newMap(data));
        packet = Packet.mapToPacket(object);
        Main.instance.sendPacket(packet);
    }
}
