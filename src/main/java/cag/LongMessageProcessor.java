package cag;

import org.msgpack.value.Value;

import java.util.HashMap;
import java.util.Map;

public class LongMessageProcessor extends Packet {

    public static Map<String, String> potentialLongMessage = new HashMap<>();
    public static Map<String, String> sendingLongMessage = new HashMap<>();

    LongMessageProcessor(Map<Value, Value> map) {
        setValues(map);
    }

    public void longMessageProcess() {
        String msg = LongMessageProcessor.sendingLongMessage.get(find("data.data.user").asStringValue().asString());
        new MessageUtil(getValues()).sendDM(msg.substring(0, 511));
        if (msg.length() > 512) {
            LongMessageProcessor.sendingLongMessage.put(find("data.data.user").asStringValue().asString(), msg.substring(512));
        } else {
            LongMessageProcessor.sendingLongMessage.remove(find("data.data.user").asStringValue().asString());
        }
    }

    public void reactToPendingLongMessage() {
        String content = find("data.data.content").asStringValue().asString();
        if (content.startsWith("yes")) {
            LongMessageProcessor.sendingLongMessage.put(find("data.data.user").asStringValue().asString(), LongMessageProcessor.potentialLongMessage.get(find("data.data.user").asStringValue().asString()));
        } else if (content.startsWith("no")) {
            new MessageUtil(getValues()).sendDM("ok");
        } else {
            new MessageUtil(getValues()).sendDM("Invalid response, the message won't be sent.");
        }

        LongMessageProcessor.potentialLongMessage.remove(find("data.data.user").asStringValue().asString());
    }
}
