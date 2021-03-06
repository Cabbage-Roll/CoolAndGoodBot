package cag;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

import java.io.IOException;
import java.util.Map;

public abstract class Packet {

    private Map<Value, Value> values;

    public static byte[] mapToPacket(Map<Value, Value> values) {
        MessageBufferPacker mp = MessagePack.newDefaultBufferPacker();
        try {
            mp.packMapHeader(values.entrySet().size());
            for (Map.Entry<Value, Value> entry : values.entrySet()) {
                mp.packValue(entry.getKey());
                mp.packValue(entry.getValue());
            }
            mp.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mp.toByteArray();
    }

    public static Map<Value, Value> packetToMap(byte[] packet) {
        try {
            return MessagePack.newDefaultUnpacker(packet).unpackValue().asMapValue().map();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Map<Value, Value> getValues() {
        return values;
    }

    public void setValues(Map<Value, Value> values) {
        this.values = values;
    }

    public Value find(String key) {
        return find(key, getValues());
    }

    public static Value find(String key, Map<Value, Value> valueMap) {
        Value value = null;
        String[] keySplitted = key.split("\\.");
        StringBuilder temp = new StringBuilder();
        for (String s : keySplitted) {
            temp.append(s);
            value = valueMap.get(ValueFactory.newString(temp.toString()));
            if (value != null) {
                temp = new StringBuilder();
                if (value.isMapValue()) {
                    valueMap = value.asMapValue().map();
                }
            }
        }
        return value;
    }
}
