package cag.packets.client;

import java.io.IOException;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;

import cag.objects.Presence;

public class SocialPresenceC {
    private static String command = "social.presence";
    private static Presence data;
    
    public byte[] makePacket() throws IOException {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        packer.packMapHeader(2);
        packer.packString("command");
        packer.packString(command);
        packer.packString("data");
        data.pack(packer);
        packer.close();
        return packer.toByteArray();
    }
    
    public SocialPresenceC(String status, String detail) {
        data = new Presence(status, detail);
    }
}
