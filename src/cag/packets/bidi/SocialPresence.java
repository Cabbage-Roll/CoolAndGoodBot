package cag.packets.bidi;

import java.io.IOException;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

import cag.packets.Packet;

public class SocialPresence implements Packet {

    public enum Premade {
        ONLINE, OFFLINE
    }

    private class Presence {
        private String status;
        private String detail;
        private String user;
        private Boolean invitable;

        public Presence(MessageUnpacker unpacker) {
            // unpack it
        }

        public Presence(String status, String detail) {
            this.status = status;
            this.detail = detail;
        }

        public void packData(MessageBufferPacker packer) throws IOException {
            int n = 0;

            if (status != null) {
                n++;
            }

            if (detail != null) {
                n++;
            }
            
            packer.packMapHeader(n);

            if (status != null) {
                packer.packString("status");
                packer.packString(status);
            }

            if (detail != null) {
                packer.packString("detail");
                packer.packString(detail);
            }
        }
    }

    private static String command = "social.presence";

    public static byte[] premakePacket(Premade p) throws IOException {
        switch (p) {
        case ONLINE:
            return new SocialPresence("online", "").toPacket();
        case OFFLINE:
            return new SocialPresence("offline", null).toPacket();
        default:
            return null;
        }
    }

    private Presence data;

    private SocialPresence(String status, String detail) {
        data = new Presence(status, detail);
    }

    @Override
    public void packData(MessageBufferPacker packer) throws IOException {
        data.packData(packer);
    }

    @Override
    public byte[] toPacket() throws IOException {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        packer.packMapHeader(2);
        packer.packString("command");
        packer.packString(command);
        packer.packString("data");
        packData(packer);
        packer.close();
        return packer.toByteArray();
    }

}
