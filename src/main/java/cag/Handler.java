package cag;

import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

class Handler implements Thread.UncaughtExceptionHandler {

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        Map<Value, Value> object = new HashMap<>();
        Map<Value, Value> data = new HashMap<>();

        object.put(ValueFactory.newString("command"), ValueFactory.newString("social.dm"));

        data.put(ValueFactory.newString("recipient"), ValueFactory.newString("5e9ebe1653f8813fadb7e862"));

        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String exceptionAsString = sw.toString();
        data.put(ValueFactory.newString("msg"), ValueFactory.newString(exceptionAsString));

        object.put(ValueFactory.newString("data"), ValueFactory.newMap(data));

        Main.chatInstance.sendPacket(Packet.mapToPacket(object));
    }
}