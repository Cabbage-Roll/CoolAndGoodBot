package cag;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

import java.util.HashMap;
import java.util.Map;

public class MessageUtil extends Packet {

    public MessageUtil(Map<Value, Value> map) {
        setValues(map);
    }

    public void sendDM(String msg) {
        if (msg.length() <= 512) {
            Map<Value, Value> object = new HashMap<>();
            Map<Value, Value> data = new HashMap<>();
            data.put(ValueFactory.newString("recipient"), find("data.data.user"));
            data.put(ValueFactory.newString("msg"), ValueFactory.newString(msg));
            object.put(ValueFactory.newString("command"), ValueFactory.newString("social.dm"));
            object.put(ValueFactory.newString("data"), ValueFactory.newMap(data));
            Main.chatInstance.sendPacket(Packet.mapToPacket(object));
        } else {
            sendDM("Message was longer than 512 characters. Do you still want to get the message? (yes/no) DONT SAY YES THIS IS BROKEN");

            LongMessageProcessor.potentialLongMessage.put(find("data.data.user").asStringValue().asString(), msg);
        }
    }

    public void reactToMessage() {

        String content = find("data.data.content").asStringValue().asString();

        byte[] packet;
        Map<Value, Value> object = new HashMap<>();
        Map<Value, Value> data = new HashMap<>();
        String[] words = content.split(" ");
        if (content.startsWith("real")) {
            data.put(ValueFactory.newString("status"), ValueFactory.newString("online"));
            data.put(ValueFactory.newString("detail"), ValueFactory.newString(""));
            object.put(ValueFactory.newString("command"), ValueFactory.newString("social.presence"));
            object.put(ValueFactory.newString("data"), ValueFactory.newMap(data));
            packet = Packet.mapToPacket(object);
            Main.chatInstance.sendPacket(packet);
        } else if (content.startsWith("fake")) {
            data.put(ValueFactory.newString("status"), ValueFactory.newString("offline"));
            data.put(ValueFactory.newString("detail"), ValueFactory.newString(""));
            object.put(ValueFactory.newString("command"), ValueFactory.newString("social.presence"));
            object.put(ValueFactory.newString("data"), ValueFactory.newMap(data));
            packet = Packet.mapToPacket(object);
            Main.chatInstance.sendPacket(packet);
        } else if (content.startsWith("help")) {
            sendDM("real, fake, help, string, apiget, blank, chget");
        } else if (content.startsWith("string")) {
            sendDM(getValues().toString());
        } else if (content.startsWith("apiget")) {
            if (!find("data.data.user").asStringValue().asString().equals("5e9ebe1653f8813fadb7e862")) {
                sendDM("no permission");
                return;
            }
            String link = words[1];
            String key = null;
            if (words.length > 2) {
                key = words[2];
            }
            String jsonString = Main.getFromApi(link);
            String finalString;

            Gson gson = new Gson();
            Map<?, ?> somap = gson.fromJson(jsonString, Map.class);
            ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());
            byte[] bytes = null;
            try {
                bytes = objectMapper.writeValueAsBytes(somap);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            Map<Value, Value> map;
            map = Packet.packetToMap(bytes);

            if (key != null) {
                finalString = Packet.find(key, map).toString();
            } else {
                finalString = jsonString;
            }

            sendDM(finalString);
        } else if (content.startsWith("blank")) {
            sendDM("\n\n\n\n\n\n\n\n\n\n");
        } else if (content.startsWith("chget")) {
            String link = words[1];
            String key = null;
            if (words.length > 2) {
                key = words[2];
            }
            String jsonString = Main.getFromCh(link);
            String finalString;

            Gson gson = new Gson();
            Map<?, ?> somap = gson.fromJson(jsonString, Map.class);
            ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());
            byte[] bytes = null;
            try {
                bytes = objectMapper.writeValueAsBytes(somap);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            Map<Value, Value> map;
            map = Packet.packetToMap(bytes);

            if (key != null) {
                finalString = Packet.find(key, map).toString();
            } else {
                finalString = jsonString;
            }

            sendDM(finalString);
        }
    }

}
