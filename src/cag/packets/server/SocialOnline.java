package cag.packets.server;

import java.io.IOException;

import org.msgpack.core.MessageUnpacker;

public class SocialOnline {

    int data;

    public SocialOnline(MessageUnpacker unpacker) throws IOException {
        data = unpacker.unpackInt();
    }
}
