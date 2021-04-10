package cag;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


public class Ribbon {

    private static final Map<String, String> RIBBON_CLOSE_CODES = new HashMap<>();

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

    private final URI endpoint;
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
        for (byte b : packet) {
            System.out.print((char) b);
        }
        System.out.println();
    }

    private void receiveAndDecodePacket(byte[] packet) {
        try {
            System.out.print("Receive: ");
            for (byte b : packet) {
                System.out.print((char) b);
            }
            System.out.println();
            byte header = packet[0];
            byte[] withoutHeader;

            switch (header) {
                case RIBBON_EXTRACTED_ID_TAG:
                    withoutHeader = new byte[packet.length - 5];
                    System.arraycopy(packet, 5, withoutHeader, 0, withoutHeader.length);
                    // extract id from here

                    new ReceivedPacket(withoutHeader);
                    break;
                case RIBBON_STANDARD_ID_TAG:
                    withoutHeader = new byte[packet.length - 1];
                    System.arraycopy(packet, 1, withoutHeader, 0, withoutHeader.length);

                    new ReceivedPacket(withoutHeader);
                case RIBBON_BATCH_TAG:
                    System.out.println("Batch tags are not processed");
                    break;
                case RIBBON_EXTENSION_TAG:
                    if (packet[1] == RIBBON_PONG_TAG) {
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                sendPacket(new byte[]{RIBBON_EXTENSION_TAG, RIBBON_PING_TAG});
                            }
                        }, 5000);
                    } else {
                        System.out.println("Unknown extension " + packet[1]);
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
                arg0.getMessage();
            }

            @Override
            public void onMessage(ByteBuffer bytes) {
                receiveAndDecodePacket(bytes.array());
            }

            @Override
            public void onOpen(ServerHandshake arg0) {
                Map<Value, Value> base = new HashMap<>();
                base.put(ValueFactory.newString("command"), ValueFactory.newString("new"));
                byte[] packet = Packet.mapToPacket(base);
                sendPacket(packet);

                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        sendPacket(new byte[]{(byte) 0xB0, (byte) 0x0B});
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
