package cag.packets;

import java.io.IOException;

import org.msgpack.core.MessageBufferPacker;

public interface Packet {
    
    public byte[] toPacket() throws IOException;

    public void packData(MessageBufferPacker packer) throws IOException;
    
}
