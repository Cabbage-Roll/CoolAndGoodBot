package cag;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

import java.util.HashMap;
import java.util.Map;

public class ReceivedPacket extends Packet {

    public ReceivedPacket(byte[] packet) {
        setValues(packetToMap(packet));
        react();
    }

    private void react() {
        new Thread(() -> {
            String command = find("command").asStringValue().asString();
            switch (command) {
                case "social.dm":
                    if (!LongMessageProcessor.potentialLongMessage.containsKey(find("data.data.user").asStringValue().asString())) {
                        if (!LongMessageProcessor.sendingLongMessage.containsKey(find("data.data.user").asStringValue().asString())) {
                            new MessageUtil(getValues()).reactToMessage();
                        } else {
                            new LongMessageProcessor(getValues()).longMessageProcess();
                        }
                    } else {
                        new LongMessageProcessor(getValues()).reactToPendingLongMessage();
                    }
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
        }).start();
    }

    private void processError() {

    }

    private void authorize() {
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
        Main.chatInstance.sendPacket(packet);
    }
}
