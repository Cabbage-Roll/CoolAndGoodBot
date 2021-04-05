package cag.packets.server;

import java.io.IOException;

import org.msgpack.core.MessageUnpacker;

public class SocialDm {
    String stream;
    String content;
    String content_safe;
    String user;
    String role;
    boolean supporter;
    int supporter_tier;
    boolean verified;
    boolean system;
    String ts;
    String _id;

    public SocialDm(MessageUnpacker unpacker) throws IOException {
        unpacker.unpackMapHeader();
        unpacker.unpackString();
        stream = unpacker.unpackString();
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
    }

    @Override
    public String toString() {
        return "SocialDm [stream=" + stream + ", content=" + content + ", content_safe=" + content_safe + ", user="
                + user + ", role=" + role + ", supporter=" + supporter + ", supporter_tier=" + supporter_tier
                + ", verified=" + verified + ", system=" + system + ", ts=" + ts + ", _id=" + _id + "]";
    }

}
