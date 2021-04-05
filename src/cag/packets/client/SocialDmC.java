package cag.packets.client;

import java.io.IOException;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;

public class SocialDmC {
    private static String command = "social.dm";
    String recipient;
    String msg;

    public SocialDmC(String recipient, String msg) {
        this.recipient = recipient;
        this.msg = msg;
    }

    public void pack(MessageBufferPacker packer) throws IOException {
        packer.packMapHeader(2);

        packer.packString("recipient");
        packer.packString(recipient);
        packer.packString("msg");
        packer.packString(msg);
    }
    
    public byte[] makePacket() throws IOException {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        packer.packMapHeader(2);
        packer.packString("command");
        packer.packString(command);
        packer.packString("data");
        pack(packer);
        return packer.toByteArray();
    }
}
