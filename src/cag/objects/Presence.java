package cag.objects;

import java.io.IOException;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessageUnpacker;

public class Presence {
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

    public void pack(MessageBufferPacker packer) throws IOException {
        packer.packMapHeader(4);

        packer.packString("status");
        if (status != null) {
            packer.packString(status);
        } else {
            packer.packNil();
        }

        packer.packString("detail");
        if (detail != null) {
            packer.packString(detail);
        } else {
            packer.packNil();
        }

        packer.packString("user");
        if (user != null) {
            packer.packString(user);
        } else {
            packer.packNil();
        }

        packer.packString("invitable");
        if (invitable != null) {
            packer.packBoolean(invitable);
        } else {
            packer.packNil();
        }
    }
}
