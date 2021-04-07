package cag.packets.bidi;

import java.io.IOException;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

import cag.Main;
import cag.packets.Packet;

public class SocialDm implements Packet {
    private static String command = "social.dm";
    private static String helpString = "get real - go online\n" + "get fake - go offline\n"
            + "toString() - call toString method for sent message\n" + "help - this";
    private String sender;
    private String recipient;
    private String content;
    private String content_safe;
    private String user;
    private String role;
    private boolean supporter;
    private int supporter_tier;
    private boolean verified;
    private boolean system;
    private String ts;
    private String _id;

    public SocialDm(MessageUnpacker unpacker) throws IOException {
        unpacker.unpackMapHeader();
        unpacker.unpackString();
        putSenderAndReceiver(unpacker.unpackString());
        unpacker.unpackString();
        unpacker.unpackMapHeader();
        unpacker.unpackString();
        content = unpacker.unpackString();
        unpacker.unpackString();
        content_safe = unpacker.unpackString();
        unpacker.unpackString();
        user = unpacker.unpackString();
        unpacker.unpackString();
        unpacker.unpackMapHeader();
        unpacker.unpackString();
        role = unpacker.unpackString();
        unpacker.unpackString();
        supporter = unpacker.unpackBoolean();
        unpacker.unpackString();
        supporter_tier = unpacker.unpackInt();
        unpacker.unpackString();
        verified = unpacker.unpackBoolean();
        unpacker.unpackString();
        system = unpacker.unpackBoolean();
        unpacker.unpackString();
        ts = unpacker.unpackString();
        unpacker.unpackString();
        _id = unpacker.unpackString();
        
        reactToMessage();
    }

    public SocialDm(String recipient, String msg) {
        this.recipient = recipient;
        this.content = msg;
    }

    @Override
    public void packData(MessageBufferPacker packer) throws IOException {
        packer.packMapHeader(2);

        packer.packString("recipient");
        packer.packString(recipient);
        packer.packString("msg");
        packer.packString(content);
    }

    private void reactToMessage() throws IOException {
        switch (content) {
        case "get real":
            Main.instance.sendPacket(SocialPresence.premakePacket(SocialPresence.Premade.ONLINE));
            break;
        case "get fake":
            Main.instance.sendPacket(SocialPresence.premakePacket(SocialPresence.Premade.OFFLINE));
            break;
        case "help":
            Main.instance.sendPacket(new SocialDm(sender, helpString).toPacket());
            break;
        case "toString()":
            Main.instance.sendPacket(new SocialDm(sender, toString()).toPacket());
            break;
        }
    }

    public byte[] toPacket() throws IOException {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        packer.packMapHeader(2);
        packer.packString("command");
        packer.packString(command);
        packer.packString("data");
        packData(packer);
        return packer.toByteArray();
    }

    @Override
    public String toString() {
        return "SocialDm [sender=" + sender + ", recipient=" + recipient + ", content=" + content + ", content_safe="
                + content_safe + ", user=" + user + ", role=" + role + ", supporter=" + supporter + ", supporter_tier="
                + supporter_tier + ", verified=" + verified + ", system=" + system + ", ts=" + ts + ", _id=" + _id
                + "]";
    }

    private void putSenderAndReceiver(String stream) {
        sender = stream.split(":")[0];
        recipient = stream.split(":")[1];
    }
}
