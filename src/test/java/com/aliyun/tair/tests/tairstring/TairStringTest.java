package com.aliyun.tair.tests.tairstring;

import com.aliyun.tair.tairstring.params.ExincrbyFloatParams;
import com.aliyun.tair.tairstring.params.ExincrbyParams;
import com.aliyun.tair.tairstring.params.ExsetParams;
import com.aliyun.tair.tairstring.results.ExcasResult;
import com.aliyun.tair.tairstring.results.ExgetResult;
import org.junit.Test;
import io.valkey.util.SafeEncoder;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.Assert.*;

public class TairStringTest extends TairStringTestBase {
    private String key;
    private String value;
    private byte[] bkey;
    private byte[] bvalue;
    private String randomkey_;
    private byte[] randomKeyBinary_;

    public TairStringTest() {
        randomkey_ = "randomkey_" + Thread.currentThread().getName() + UUID.randomUUID().toString();
        randomKeyBinary_ = ("randomkey_" + Thread.currentThread().getName() + UUID.randomUUID().toString()).getBytes();
        key = "key" + Thread.currentThread().getName() + UUID.randomUUID().toString();
        value = "value" + Thread.currentThread().getName() + UUID.randomUUID().toString();
        bkey = ("bkey" + Thread.currentThread().getName() + UUID.randomUUID().toString()).getBytes();
        bvalue = ("bvalue" + Thread.currentThread().getName() + UUID.randomUUID().toString()).getBytes();
    }

    @Test
    public void exsetTest() {
        String ret = "";

        // String
        ret = tairString.exset(key, value);
        assertEquals("OK", ret);
        ExgetResult<String> result = tairString.exget(key);
        assertNotNull(result);
        assertEquals(true, this.value.equals(result.getValue()));
        assertEquals((long)1, result.getVersion());

        //binary
        ret = tairString.exset(bkey, bvalue);
        assertEquals("OK", ret);
        ExgetResult<byte[]> bresult = tairString.exget(bkey);
        assertNotNull(bresult);
        assertEquals(true, Arrays.equals(bvalue, bresult.getValue()));
        assertEquals((long)1, bresult.getVersion());
    }

    @Test
    public void exsetKeepTTLTest() throws Exception{
        ExsetParams params = new ExsetParams();
        params.keepttl();
        ExsetParams params_ex = new ExsetParams();
        params_ex.ex(2);

        // String
        String ret = tairString.exset(key, value, params_ex);
        assertEquals("OK", ret);
        ret = tairString.exset(key, value);
        assertEquals("OK", ret);
        Thread.sleep(3000);
        ExgetResult<String> getRet = tairString.exget(key);
        assertEquals(value, getRet.getValue());
        assertEquals(2, getRet.getVersion());

        ret = tairString.exset(key, value, params_ex);
        assertEquals("OK", ret);
        ret = tairString.exset(key, value, params);
        assertEquals("OK", ret);
        getRet = tairString.exget(key);
        assertEquals(value, getRet.getValue());
        assertEquals(4, getRet.getVersion());
        Thread.sleep(3000);
        getRet = tairString.exget(key);
        assertNull(getRet);
    }

    @Test
    public void exsetParamsTest() {
        ExsetParams params_nx = new ExsetParams();
        params_nx.nx();
        ExsetParams params_xx = new ExsetParams();
        params_xx.xx();
        String ret_xx = "";
        String ret_nx = "";

        // String
        ret_xx = tairString.exset(key, value, params_xx);
        assertEquals(null, ret_xx);
        ret_nx = tairString.exset(key, value, params_nx);
        assertEquals("OK", ret_nx);
        ret_xx = tairString.exset(key, value, params_xx);
        assertEquals("OK", ret_xx);

        //binary
        ret_xx = tairString.exset(bkey, bvalue, params_xx);
        assertEquals(null, ret_xx);
        ret_nx = tairString.exset(bkey, bvalue, params_nx);
        assertEquals("OK", ret_nx);
        ret_xx = tairString.exset(bkey, bvalue, params_xx);
        assertEquals("OK", ret_xx);
    }

    @Test
    public void exsetverTest() {
        String ret = "";
        long ret_var = 0;

        // String
        ret = tairString.exset(key, value);
        assertEquals("OK", ret);
        ExgetResult<String> result = tairString.exget(key);
        assertNotNull(result);
        assertEquals(true, this.value.equals(result.getValue()));
        assertEquals((long)1, result.getVersion());

        ret_var = tairString.exsetver(key, 10);
        assertEquals(1, ret_var);
        result = tairString.exget(key);
        assertNotNull(result);
        assertEquals(true, this.value.equals(result.getValue()));
        assertEquals((long)10, result.getVersion());

        //binary
        ret = tairString.exset(bkey, bvalue);
        assertEquals("OK", ret);
        ExgetResult<byte[]> bresult = tairString.exget(bkey);
        assertNotNull(bresult);
        assertEquals(true, Arrays.equals(bvalue, bresult.getValue()));
        assertEquals((long)1, bresult.getVersion());

        ret_var = tairString.exsetver(bkey, 10);
        assertEquals(1, ret_var);
        bresult = tairString.exget(bkey);
        assertNotNull(bresult);
        assertEquals(true, Arrays.equals(bvalue, bresult.getValue()));
        assertEquals((long)10, bresult.getVersion());
    }

    @Test
    public void exincrbyTest() {
        String ret = "";
        String num_string_value = "100";
        byte[] num_byte_value = SafeEncoder.encode("100");
        long incr_value = 100;
        String new_string_value = "200";
        byte[] new_byte_value = SafeEncoder.encode("200");
        long new_long_value = 200;
        long ret_var = 0;

        // String
        ret = tairString.exset(key, num_string_value);
        assertEquals("OK", ret);
        ExgetResult<String> result = tairString.exget(key);
        assertNotNull(result);
        assertEquals(true, num_string_value.equals(result.getValue()));
        assertEquals((long)1, result.getVersion());

        ret_var = tairString.exincrBy(key, incr_value);
        assertEquals(new_long_value, ret_var);
        result = tairString.exget(key);
        assertNotNull(result);
        assertEquals(true, new_string_value.equals(result.getValue()));
        assertEquals((long)2, result.getVersion());

        //binary
        ret = tairString.exset(bkey, num_byte_value);
        assertEquals("OK", ret);
        ExgetResult<byte[]> bresult = tairString.exget(bkey);
        assertNotNull(bresult);
        assertEquals(true, Arrays.equals(num_byte_value, bresult.getValue()));
        assertEquals((long)1, bresult.getVersion());

        ret_var = tairString.exincrBy(bkey, incr_value);
        assertEquals(new_long_value, ret_var);
        bresult = tairString.exget(bkey);
        assertNotNull(bresult);
        assertEquals(true, Arrays.equals(new_byte_value, bresult.getValue()));
        assertEquals((long)2, bresult.getVersion());
    }

    @Test
    public void exincrbyParamsTest() throws InterruptedException {
        String ret = "";
        String num_string_value = "100";
        byte[] num_byte_value = SafeEncoder.encode("100");
        long incr_value = 100;
        String new_string_value = "200";
        byte[] new_byte_value = SafeEncoder.encode("200");
        long new_long_value = 200;
        long ret_var = 0;
        ExgetResult<String> result = null;
        ExgetResult<byte[]> bresult = null;

        ExincrbyParams params_nx_px = new ExincrbyParams();
        params_nx_px.nx();
        params_nx_px.px(1000);
        ExincrbyParams params_xx_ex = new ExincrbyParams();
        params_xx_ex.xx();
        params_xx_ex.ex(1);
        ExincrbyParams params_xx_pxat = new ExincrbyParams();
        params_xx_pxat.xx();
        params_xx_pxat.pxat(System.currentTimeMillis() + 1000);

        ret_var = tairString.exincrBy(key, incr_value, params_nx_px);
        assertEquals(incr_value, ret_var);
        result = tairString.exget(key);
        assertNotNull(result);
        assertEquals(true, num_string_value.equals(result.getValue()));
        assertEquals((long)1, result.getVersion());
        Thread.sleep(3000);
        result = tairString.exget(key);
        assertEquals(null, result);

        ret = tairString.exset(key, num_string_value);
        assertEquals("OK", ret);
        ret_var = tairString.exincrBy(key, incr_value, params_xx_ex);
        assertEquals(new_long_value, ret_var);
        result = tairString.exget(key);
        assertNotNull(result);
        assertEquals(true, new_string_value.equals(result.getValue()));
        assertEquals((long)2, result.getVersion());
        Thread.sleep(3000);
        result = tairString.exget(key);
        assertEquals(null, result);

        ret = tairString.exset(key, num_string_value);
        assertEquals("OK", ret);
        params_xx_pxat.pxat(System.currentTimeMillis() + 1000);
        ret_var = tairString.exincrBy(key, incr_value, params_xx_pxat);
        assertEquals(new_long_value, ret_var);
        result = tairString.exget(key);
        assertNotNull(result);
        assertEquals(true, new_string_value.equals(result.getValue()));
        assertEquals((long)2, result.getVersion());
        Thread.sleep(3000);
        result = tairString.exget(key);
        assertEquals(null, result);

        //binary
        ret_var = tairString.exincrBy(bkey, incr_value, params_nx_px);
        assertEquals(incr_value, ret_var);
        bresult = tairString.exget(bkey);
        assertNotNull(bresult);
        assertEquals(true, Arrays.equals(num_byte_value, bresult.getValue()));
        assertEquals((long)1, bresult.getVersion());
        Thread.sleep(3000);
        bresult = tairString.exget(bkey);
        assertEquals(null, bresult);

        ret = tairString.exset(bkey, num_byte_value);
        assertEquals("OK", ret);
        ret_var = tairString.exincrBy(bkey, incr_value, params_xx_ex);
        assertEquals(new_long_value, ret_var);
        bresult = tairString.exget(bkey);
        assertNotNull(bresult);
        assertEquals(true, Arrays.equals(new_byte_value, bresult.getValue()));
        assertEquals((long)2, bresult.getVersion());
        Thread.sleep(3000);
        bresult = tairString.exget(bkey);
        assertEquals(null, bresult);

        ret = tairString.exset(bkey, num_byte_value);
        assertEquals("OK", ret);
        params_xx_pxat.pxat(System.currentTimeMillis() + 1000);
        ret_var = tairString.exincrBy(bkey, incr_value, params_xx_pxat);
        assertEquals(new_long_value, ret_var);
        bresult = tairString.exget(bkey);
        assertNotNull(bresult);
        assertEquals(true, Arrays.equals(new_byte_value, bresult.getValue()));
        assertEquals((long)2, bresult.getVersion());
        Thread.sleep(2000);
        bresult = tairString.exget(bkey);
        assertEquals(null, bresult);
    }

    @Test
    public void exincrbyfloatTest() {
        String ret = "";
        String num_string_value = "100";
        byte[] num_byte_value = SafeEncoder.encode("100");
        Double incr_value = Double.valueOf(100);
        String new_string_value = "200";
        byte[] new_byte_value = SafeEncoder.encode("200");
        Double new_float_value = Double.valueOf(200);
        Double ret_var = Double.valueOf(0);

        // String
        ret = tairString.exset(key, num_string_value);
        assertEquals("OK", ret);
        ExgetResult<String> result = tairString.exget(key);
        assertNotNull(result);
        assertEquals(true, num_string_value.equals(result.getValue()));
        assertEquals((long)1, result.getVersion());

        ret_var = tairString.exincrByFloat(key, incr_value);
        assertEquals(new_float_value, ret_var);
        result = tairString.exget(key);
        assertNotNull(result);
        assertEquals(true, new_string_value.equals(result.getValue()));
        assertEquals((long)2, result.getVersion());

        //binary
        ret = tairString.exset(bkey, num_byte_value);
        assertEquals("OK", ret);
        ExgetResult<byte[]> bresult = tairString.exget(bkey);
        assertNotNull(bresult);
        assertEquals(true, Arrays.equals(num_byte_value, bresult.getValue()));
        assertEquals((long)1, bresult.getVersion());

        ret_var = tairString.exincrByFloat(bkey, incr_value);
        assertEquals(new_float_value, ret_var);
        bresult = tairString.exget(bkey);
        assertNotNull(bresult);
        assertEquals(true, Arrays.equals(new_byte_value, bresult.getValue()));
        assertEquals((long)2, bresult.getVersion());
    }

    @Test
    public void exincrbyfloatParamsTest() throws InterruptedException {
        String ret = "";
        String num_string_value = "100";
        byte[] num_byte_value = SafeEncoder.encode("100");
        Double incr_value = Double.valueOf(100);
        String new_string_value = "200";
        byte[] new_byte_value = SafeEncoder.encode("200");
        Double new_float_value = Double.valueOf(200);
        Double ret_var = Double.valueOf(0);
        ExgetResult<String> result = null;
        ExgetResult<byte[]> bresult = null;

        ExincrbyFloatParams params_nx_px = new ExincrbyFloatParams();
        params_nx_px.nx();
        params_nx_px.px(1000);
        ExincrbyFloatParams params_xx_ex = new ExincrbyFloatParams();
        params_xx_ex.xx();
        params_xx_ex.ex(1);
        ExincrbyFloatParams params_xx_pxat = new ExincrbyFloatParams();
        params_xx_pxat.xx();
        params_xx_pxat.pxat(System.currentTimeMillis() + 1000);

        ret_var = tairString.exincrByFloat(key, incr_value, params_nx_px);
        assertEquals(incr_value, ret_var);
        result = tairString.exget(key);
        assertNotNull(result);
        assertEquals(true, num_string_value.equals(result.getValue()));
        assertEquals((long)1, result.getVersion());
        Thread.sleep(3000);
        result = tairString.exget(key);
        assertEquals(null, result);

        ret = tairString.exset(key, num_string_value);
        assertEquals("OK", ret);
        ret_var = tairString.exincrByFloat(key, incr_value, params_xx_ex);
        assertEquals(new_float_value, ret_var);
        result = tairString.exget(key);
        assertNotNull(result);
        assertEquals(true, new_string_value.equals(result.getValue()));
        assertEquals((long)2, result.getVersion());
        Thread.sleep(3000);
        result = tairString.exget(key);
        assertEquals(null, result);

        ret = tairString.exset(key, num_string_value);
        assertEquals("OK", ret);
        params_xx_pxat.pxat(System.currentTimeMillis() + 1000);
        ret_var = tairString.exincrByFloat(key, incr_value, params_xx_pxat);
        assertEquals(new_float_value, ret_var);
        result = tairString.exget(key);
        assertNotNull(result);
        assertEquals(true, new_string_value.equals(result.getValue()));
        assertEquals((long)2, result.getVersion());
        Thread.sleep(3000);
        result = tairString.exget(key);
        assertEquals(null, result);

        //binary
        ret_var = tairString.exincrByFloat(bkey, incr_value, params_nx_px);
        assertEquals(incr_value, ret_var);
        bresult = tairString.exget(bkey);
        assertNotNull(bresult);
        assertEquals(true, Arrays.equals(num_byte_value, bresult.getValue()));
        assertEquals((long)1, bresult.getVersion());
        Thread.sleep(3000);
        bresult = tairString.exget(bkey);
        assertEquals(null, bresult);

        ret = tairString.exset(bkey, num_byte_value);
        assertEquals("OK", ret);
        ret_var = tairString.exincrByFloat(bkey, incr_value, params_xx_ex);
        assertEquals(new_float_value, ret_var);
        bresult = tairString.exget(bkey);
        assertNotNull(bresult);
        assertEquals(true, Arrays.equals(new_byte_value, bresult.getValue()));
        assertEquals((long)2, bresult.getVersion());
        Thread.sleep(3000);
        bresult = tairString.exget(bkey);
        assertEquals(null, bresult);

        ret = tairString.exset(bkey, num_byte_value);
        assertEquals("OK", ret);
        params_xx_pxat.pxat(System.currentTimeMillis() + 1000);
        ret_var = tairString.exincrByFloat(bkey, incr_value, params_xx_pxat);
        assertEquals(new_float_value, ret_var);
        bresult = tairString.exget(bkey);
        assertNotNull(bresult);
        assertEquals(true, Arrays.equals(new_byte_value, bresult.getValue()));
        assertEquals((long)2, bresult.getVersion());
        Thread.sleep(3000);
        bresult = tairString.exget(bkey);
        assertEquals(null, bresult);
    }

    @Test
    public void excasTest() {
        String ret = "";
        ExcasResult<String> ret2 = null;
        ExcasResult<byte[]> ret3 = null;

        // String
        ret = tairString.exset(key, value);
        assertEquals("OK", ret);
        ret2 = tairString.excas(key, "new" + value, 2);
        assertEquals(value, ret2.getValue());
        assertEquals((long)1, ret2.getVersion());
        ret2 = tairString.excas(key, "new" + value, 1);
        assertEquals("OK", ret2.getMsg());
        assertEquals("", ret2.getValue());
        assertEquals((long)2, ret2.getVersion());

        //binary
        ret = tairString.exset(bkey, bvalue);
        assertEquals("OK", ret);
        ret3 = tairString.excas(bkey, SafeEncoder.encode("new" + bvalue), 2);
        assertEquals(true, Arrays.equals(bvalue, ret3.getValue()));
        assertEquals((long)1, ret3.getVersion());
        ret3 = tairString.excas(bkey, SafeEncoder.encode("new" + bvalue), 1);
        assertEquals(true, Arrays.equals(SafeEncoder.encode("OK"), ret3.getMsg()));
        assertEquals(true, Arrays.equals(SafeEncoder.encode(""), ret3.getValue()));
        assertEquals((long)2, ret3.getVersion());
    }

    @Test
    public void excadTest() throws InterruptedException {
        String ret = "";
        long ret2 = 0;

        // String
        ret = tairString.exset(key, value);
        assertEquals("OK", ret);
        ret2 = tairString.excad(key, 2);
        assertEquals((long)0, ret2);
        ret2 = tairString.excad(key, 1);
        assertEquals((long)1, ret2);

        //binary
        ret = tairString.exset(bkey, bvalue);
        assertEquals("OK", ret);
        ret2 = tairString.excad(bkey, 2);
        assertEquals((long)0, ret2);
        ret2 = tairString.excad(bkey, 1);
        assertEquals((long)1, ret2);
    }

    //@Test
    //public void exsetException() {
    //    tairString.exset(randomkey_, "");
    //
    //    try {
    //        jedis.set(randomkey_, "bar");
    //        tairString.exset(randomkey_, "");
    //    } catch (Exception e) {
    //        assertTrue(e.getMessage().contains("WRONGTYPE"));
    //    }
    //}
    //
    //@Test
    //public void exgetException() {
    //    tairString.exget(randomkey_);
    //
    //    try {
    //        jedis.set(randomkey_, "bar");
    //        tairString.exget(randomkey_);
    //    } catch (Exception e) {
    //        assertTrue(e.getMessage().contains("WRONGTYPE"));
    //    }
    //}
    //
    //@Test
    //public void exsetverException() {
    //    tairString.exsetver(randomkey_, 10);
    //
    //    try {
    //        jedis.set(randomkey_, "bar");
    //        tairString.exsetver(randomkey_, 10);
    //    } catch (Exception e) {
    //        assertTrue(e.getMessage().contains("WRONGTYPE"));
    //    }
    //}
    //
    //@Test
    //public void exincrbyException() {
    //    tairString.exincrBy(randomkey_, 10);
    //
    //    try {
    //        jedis.set(randomkey_, "bar");
    //        tairString.exincrBy(randomkey_, 10);
    //    } catch (Exception e) {
    //        assertTrue(e.getMessage().contains("WRONGTYPE"));
    //    }
    //}
    //
    //@Test
    //public void exincrbyfloatException() {
    //    tairString.exincrByFloat(randomkey_, 10.0);
    //
    //    try {
    //        jedis.set(randomkey_, "bar");
    //        tairString.exincrByFloat(randomkey_, 10.0);
    //    } catch (Exception e) {
    //        assertTrue(e.getMessage().contains("WRONGTYPE"));
    //    }
    //}
    //
    //@Test
    //public void excasException() {
    //    tairString.excas(randomkey_, "",10);
    //
    //    try {
    //        jedis.set(randomkey_, "bar");
    //        tairString.excas(randomkey_, "", 10);
    //    } catch (Exception e) {
    //        assertTrue(e.getMessage().contains("WRONGTYPE"));
    //    }
    //}
    //
    //@Test
    //public void excadException() {
    //    tairString.excad(randomkey_, 1);
    //
    //    try {
    //        jedis.set(randomkey_, "bar");
    //        tairString.excad(randomkey_,1);
    //    } catch (Exception e) {
    //        assertTrue(e.getMessage().contains("WRONGTYPE"));
    //    }
    //}

}
