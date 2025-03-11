package com.aliyun.tair.tairts.params;

import com.aliyun.tair.jedis3.Params;
import io.valkey.util.SafeEncoder;

import java.util.ArrayList;

import static io.valkey.Protocol.toByteArray;

public class ExtsStringMaddParams extends Params {
    public byte[][] getByteParams(String pkey, ArrayList<ExtsStringDataPoint<String>> args) {
        ArrayList<byte[]> byteParams = new ArrayList<byte[]>();
        byteParams.add(SafeEncoder.encode(pkey));
        byteParams.add(SafeEncoder.encode(String.valueOf(args.size())));
        for (ExtsStringDataPoint<String> arg : args) {
            byteParams.add(SafeEncoder.encode(arg.getSkey()));
            byteParams.add(SafeEncoder.encode(arg.getTs()));
            byteParams.add(SafeEncoder.encode(arg.getValue()));
        }
        return byteParams.toArray(new byte[byteParams.size()][]);
    }

    public byte[][] getByteParams(byte[] pkey, ArrayList<ExtsStringDataPoint<byte[]>> args) {
        ArrayList<byte[]> byteParams = new ArrayList<byte[]>();
        byteParams.add(pkey);
        byteParams.add(toByteArray(args.size()));
        for (ExtsStringDataPoint<byte[]> arg : args) {
            byteParams.add(arg.getSkey());
            byteParams.add(arg.getTs());
            byteParams.add(arg.getValue());
        }
        return byteParams.toArray(new byte[byteParams.size()][]);
    }
}
