package cag.packets.bidi;

import cag.Main;
import cag.packets.Packet;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.Value;

import java.io.IOException;
import java.util.Map;

public class SocialDm {
    private static String command = "social.dm";
    public static String helpString = "get real - go online\n" + "get fake - go offline\n"
            + "toString() - call toString method for sent message\n" + "help - this";

    public SocialDm(String recipient, String msg) {
    }
}
