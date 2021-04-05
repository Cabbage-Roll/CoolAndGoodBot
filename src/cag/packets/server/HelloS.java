package cag.packets.server;

import org.msgpack.core.MessageUnpacker;

import cag.Main;
import cag.packets.client.HelloC;

public class HelloS {
    
    public HelloS(MessageUnpacker unpacker) {
        //we do a response and completely ditch this packet
        Main.instance.sendPacket(HelloC.makePacket(0));
    }
}
