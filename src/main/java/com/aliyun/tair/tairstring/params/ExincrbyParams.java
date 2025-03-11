package com.aliyun.tair.tairstring.params;

import com.aliyun.tair.jedis3.Params;
import io.valkey.util.SafeEncoder;

import java.util.ArrayList;

public class ExincrbyParams extends Params {

    private static final String XX = "xx";
    private static final String NX = "nx";

    private static final String PX = "px";
    private static final String EX = "ex";
    private static final String EXAT = "exat";
    private static final String PXAT = "pxat";

    private static final String VER = "ver";
    private static final String ABS = "abs";

    private static final String MAX = "MAX";
    private static final String MIN = "MIN";

    private static final String DEF = "def";
    private static final String NONEGATIVE = "nonegative";
    private static final String KEEPTTL = "keepttl";

    public ExincrbyParams() {
    }

    public static ExincrbyParams ExincrbyParams() {
        return new ExincrbyParams();
    }

    public ExincrbyParams def(long defaultValue) {
        addParam(DEF, defaultValue);
        return this;
    }

    public ExincrbyParams nonegative() {
        addParam(NONEGATIVE);
        return this;
    }

    public ExincrbyParams max(long max) {
        addParam(MAX, max);
        return this;
    }

    public ExincrbyParams min(long min) {
        addParam(MIN, min);
        return this;
    }


    /**
     * Only set the key if it already exist.
     * @return SetParams
     */
    public ExincrbyParams xx() {
        addParam(XX);
        return this;
    }

    /**
     * Only set the key if it does not already exist.
     * @return SetParams
     */
    public ExincrbyParams nx() {
        addParam(NX);
        return this;
    }

    /**
     * Set the specified expire time, in seconds.
     * @param secondsToExpire
     * @return SetParams
     */
    public ExincrbyParams ex(int secondsToExpire) {
        addParam(EX, secondsToExpire);
        return this;
    }

    /**
     * Set the specified expire time, in milliseconds.
     * @param millisecondsToExpire
     * @return SetParams
     */
    public ExincrbyParams px(long millisecondsToExpire) {
        addParam(PX, millisecondsToExpire);
        return this;
    }

    /**
     * Set the specified absolute expire time, in seconds.
     * @param secondsToExpire
     * @return SetParams
     */
    public ExincrbyParams exat(int secondsToExpire) {
        addParam(EXAT, secondsToExpire);
        return this;
    }

    /**
     * Set the specified absolute expire time, in milliseconds.
     * @param millisecondsToExpire
     * @return SetParams
     */
    public ExincrbyParams pxat(long millisecondsToExpire) {
        addParam(PXAT, millisecondsToExpire);
        return this;
    }

    /**
     * Set if version equal or not exist
     * @param version
     * @return SetParams
     */
    public ExincrbyParams ver(long version) {
        addParam(VER, version);
        return this;
    }

    /**
     * Set version to absoluteVersion
     * @param absoluteVersion
     * @return SetParams
     */
    public ExincrbyParams abs(long absoluteVersion) {
        addParam(ABS, absoluteVersion);
        return this;
    }

    /**
     * do not update ttl
     * @return the params
     */
    public ExincrbyParams keepttl() {
        addParam(KEEPTTL);
        return this;
    }

    private void addParamWithValue(ArrayList<byte[]> byteParams, String option) {
        if (contains(option)) {
            byteParams.add(SafeEncoder.encode(option));
            byteParams.add(SafeEncoder.encode(String.valueOf((Object)getParam(option))));
        }
    }

    public byte[][] getByteParams(byte[]... args) {
        ArrayList<byte[]> byteParams = new ArrayList<byte[]>();
        for (byte[] arg : args) {
            byteParams.add(arg);
        }

        if (contains(XX)) {
            byteParams.add(SafeEncoder.encode(XX));
        }
        if (contains(NX)) {
            byteParams.add(SafeEncoder.encode(NX));
        }
        if (contains(NONEGATIVE)) {
            byteParams.add(SafeEncoder.encode(NONEGATIVE));
        }

        addParamWithValue(byteParams, EX);
        addParamWithValue(byteParams, PX);
        addParamWithValue(byteParams, EXAT);
        addParamWithValue(byteParams, PXAT);

        addParamWithValue(byteParams, VER);
        addParamWithValue(byteParams, ABS);

        addParamWithValue(byteParams, MIN);
        addParamWithValue(byteParams, MAX);

        addParamWithValue(byteParams, DEF);

        if (contains(KEEPTTL)) {
            byteParams.add(SafeEncoder.encode(KEEPTTL));
        }

        return byteParams.toArray(new byte[byteParams.size()][]);
    }
}
