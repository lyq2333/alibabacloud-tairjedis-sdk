package com.aliyun.tair.taircpc.params;

import com.aliyun.tair.taircpc.CommonResult;
import com.aliyun.tair.jedis3.Params;
import io.valkey.util.SafeEncoder;

import java.util.ArrayList;

public class CpcUpdateParams extends Params {

    private static final String PX = "px";
    private static final String EX = "ex";
    private static final String EXAT = "exat";
    private static final String PXAT = "pxat";
    private static final String SIZE = "size";
    private static final String WIN = "win";

    public CpcUpdateParams() {}

    public static CpcUpdateParams CpcUpdateParams() {
        return new CpcUpdateParams();
    }

    /**
     * Set the specified expire time, in seconds.
     *
     * @param secondsToExpire
     * @return SetParams
     */
    public CpcUpdateParams ex(long secondsToExpire) {
        addParam(EX, secondsToExpire);
        return this;
    }

    /**
     * Set the specified expire time, in milliseconds.
     *
     * @param millisecondsToExpire
     * @return SetParams
     */
    public CpcUpdateParams px(long millisecondsToExpire) {
        addParam(PX, millisecondsToExpire);
        return this;
    }

    /**
     * Set the specified absolute expire time, in seconds.
     *
     * @param secondsToExpire
     * @return SetParams
     */
    public CpcUpdateParams exat(long secondsToExpire) {
        addParam(EXAT, secondsToExpire);
        return this;
    }

    /**
     * Set the specified absolute expire time, in milliseconds.
     *
     * @param millisecondsToExpire
     * @return SetParams
     */
    public CpcUpdateParams pxat(long millisecondsToExpire) {
        addParam(PXAT, millisecondsToExpire);
        return this;
    }

    public CpcUpdateParams size(long size) {
        addParam(SIZE, size);
        return this;
    }

    public CpcUpdateParams win(long winsize) {
        addParam(WIN, winsize);
        return this;
    }

    private int addParamWithValue(ArrayList<byte[]> byteParams, String option) {
        if (contains(option)) {
            byteParams.add(SafeEncoder.encode(option));
            byteParams.add(SafeEncoder.encode(String.valueOf((Object)getParam(option))));
            return 1;
        }
        return 0;
    }

    public byte[][] getByteParams(byte[]... args) {
        ArrayList<byte[]> byteParams = new ArrayList<byte[]>();
        for (byte[] arg : args) {
            byteParams.add(arg);
        }

        int ex = addParamWithValue(byteParams, EX);
        int px = addParamWithValue(byteParams, PX);
        int exat = addParamWithValue(byteParams, EXAT);
        int pxat = addParamWithValue(byteParams, PXAT);
        if (ex + px + exat + pxat > 1) {
            throw new IllegalArgumentException(CommonResult.multiExpireParam);
        }

        addParamWithValue(byteParams, SIZE);
        addParamWithValue(byteParams, WIN);

        return byteParams.toArray(new byte[byteParams.size()][]);
    }
}
