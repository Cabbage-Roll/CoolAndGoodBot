package cag.packets.client;

import java.io.IOException;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;

public class New {
    public static byte[] makePacket() throws IOException {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        packer.packMapHeader(1);
        packer.packString("command");
        packer.packString("new");
        packer.close();
        return packer.toByteArray();
    }
}
