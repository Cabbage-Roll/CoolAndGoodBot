package cag;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.msgpack.core.MessagePack.UnpackerConfig;
import org.msgpack.core.MessageUnpacker;

import cag.packets.client.New;
import cag.packets.server.HelloS;

public class Ribbon {

    private static Map<String, String> RIBBON_CLOSE_CODES = new HashMap<String, String>();
    static {
        RIBBON_CLOSE_CODES.put("1000", "ribbon closed normally");
        RIBBON_CLOSE_CODES.put("1001", "client closed ribbon");
        RIBBON_CLOSE_CODES.put("1002", "protocol error");
        RIBBON_CLOSE_CODES.put("1003", "protocol violation");
        RIBBON_CLOSE_CODES.put("1006", "ribbon lost");
        RIBBON_CLOSE_CODES.put("1007", "payload data corrupted");
        RIBBON_CLOSE_CODES.put("1008", "protocol violation");
        RIBBON_CLOSE_CODES.put("1009", "too much data");
        RIBBON_CLOSE_CODES.put("1010", "negotiation error");
        RIBBON_CLOSE_CODES.put("1011", "server error");
        RIBBON_CLOSE_CODES.put("1012", "server restarting");
        RIBBON_CLOSE_CODES.put("1013", "temporary error");
        RIBBON_CLOSE_CODES.put("1014", "bad gateway");
        RIBBON_CLOSE_CODES.put("1015", "TLS error");
    }
    
    private static final byte RIBBON_EXTRACTED_ID_TAG = (byte) 174;
    private static final byte RIBBON_STANDARD_ID_TAG = 69;
    private static final byte RIBBON_BATCH_TAG = 88;
    private static final byte RIBBON_EXTENSION_TAG = (byte) 0xB0;
    private static final byte RIBBON_PING_TAG = 0x0B;
    private static final byte RIBBON_PONG_TAG = 0x0C;

    private URI endpoint;
    private WebSocketClient ws;
    private String id;
    private String closeReason = "ribbon lost";
    private boolean pingissues = false;
    private boolean wasEverConnected = false;
    String ribbonSessionID = "SESS-" + Math.floor(Math.random() * Integer.MAX_VALUE);

    public Ribbon(URI uri) {
        endpoint = uri;
        open();
    }

    public void sendPacket(byte[] packet) {
        ws.send(packet);
        System.out.print("Send: ");
        for (int i = 0; i < packet.length; i++) {
            System.out.print((char) packet[i]);
        }
        System.out.println();
    }

    private void receiveAndDecodePacket(byte[] packet) {
        try {
            System.out.print("Receive: ");
            for (int i = 0; i < packet.length; i++) {
                System.out.print((char) packet[i]);
            }
            System.out.println();
            byte header = packet[0];
            byte[] withoutHeader;

            switch (header) {
            case RIBBON_EXTRACTED_ID_TAG:
                withoutHeader = new byte[packet.length - 5];
                for (int i = 0; i < withoutHeader.length; i++) {
                    withoutHeader[i] = packet[5 + i];
                }
                // extract id from here

                new PacketReceivingProcess(withoutHeader);
                break;
            case RIBBON_STANDARD_ID_TAG:
                withoutHeader = new byte[packet.length - 1];
                for (int i = 0; i < withoutHeader.length; i++) {
                    withoutHeader[i] = packet[1 + i];
                }

                MessageUnpacker unpacker = new UnpackerConfig().newUnpacker(withoutHeader);
                unpacker.unpackMapHeader();
                String key = unpacker.unpackString();
                switch (key) {
                case "command":
                    String command = unpacker.unpackString();
                    switch (command) {
                    case "hello":
                        new HelloS(unpacker);
                        break;
                    default:
                        System.out.println("Unknown command " + command);
                    }
                    break;
                default:
                    System.out.println("Unknown key " + key);
                }
            case RIBBON_BATCH_TAG:
                System.out.println("BATCH TAG BATCH TAG BATCH TAG BATCH TAG BATCH TAG");
                break;
            case RIBBON_EXTENSION_TAG:
                if (packet[1] == RIBBON_PONG_TAG) {
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            sendPacket(new byte[] { RIBBON_EXTENSION_TAG, RIBBON_PING_TAG });
                        }
                    }, 5000);
                } else {
                    System.out.println("Unknown packet " + packet[1]);
                }
                break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    private void open() {
        if (ws != null) {
            ws.close();
        }
        ws = new WebSocketClient(endpoint) {
            @Override
            public void onClose(int arg0, String arg1, boolean arg2) {
                if (closeReason.equals("ribbon lost") && pingissues) {
                    closeReason = "ping timeout";
                }
                if (closeReason.equals("ribbon lost") && !wasEverConnected) {
                    closeReason = "failed to connect";
                }
                System.out.println("Ribbon " + id + " closed " + closeReason);
            }

            @Override
            public void onError(Exception arg0) {
                arg0.printStackTrace();
            }

            @Override
            public void onMessage(ByteBuffer bytes) {
                receiveAndDecodePacket(bytes.array());
            }

            @Override
            public void onOpen(ServerHandshake arg0) {
                try {
                    sendPacket(New.makePacket());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        sendPacket(new byte[] { (byte) 0xB0, (byte) 0x0B });
                    }
                }, 5000);
            }

            @Override
            public void onMessage(String arg0) {
            }
        };
        ws.connect();
    }

}
