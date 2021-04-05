package cag.packets.client;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;

import cag.Secret;

public class HelloC {
    public static byte[] makePacket(int id) {
        try {
            MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
            packer.packMapHeader(3);
            packer.packString("id");
            packer.packInt(id);
            packer.packString("command");
            packer.packString("authorize");
            packer.packString("data");
            packer.packMapHeader(4);
            packer.packString("token");
            packer.packString(Secret.userToken);
            packer.packString("handling");
            packer.packMapHeader(4);
            packer.packString("arr");
            packer.packString("1");
            packer.packString("das");
            packer.packString("1");
            packer.packString("sdf");
            packer.packString("41");
            packer.packString("safelock");
            packer.packBoolean(true);
            packer.packString("signature");
            packer.packMapHeader(6);
            packer.packString("mode");
            packer.packString("production");
            packer.packString("version");
            packer.packString("6.0.4");
            packer.packString("countdown");
            packer.packBoolean(false);
            packer.packString("commit");
            packer.packMapHeader(2);
            packer.packString("id");
            packer.packString("2d05c95");
            packer.packString("time");
            packer.packLong(1617227309000L);
            packer.packString("serverCycle");
            packer.packString("fMEhXk-V7");
            packer.packString("build");
            packer.packMapHeader(2);
            packer.packString("id");
            packer.packString("gdlLS4_Vd");
            packer.packString("time");
            packer.packLong(1617314405656L);
            packer.packString("i");
            packer.packString("unknown");
            
            byte[] packed = packer.toByteArray();
            return packed;
        } catch (Exception e) {
            return null;
        }
    }
}
