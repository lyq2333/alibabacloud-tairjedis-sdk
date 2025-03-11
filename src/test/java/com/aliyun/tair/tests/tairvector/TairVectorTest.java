package com.aliyun.tair.tests.tairvector;

import com.aliyun.tair.tairvector.factory.VectorBuilderFactory;
import com.aliyun.tair.tairvector.factory.VectorBuilderFactory.KnnItem;
import com.aliyun.tair.tairvector.params.DistanceMethod;
import com.aliyun.tair.tairvector.params.HscanParams;
import com.aliyun.tair.tairvector.params.IndexAlgorithm;
import org.junit.Ignore;
import org.junit.Test;
import com.aliyun.tair.jedis3.ScanResult;
import io.valkey.util.SafeEncoder;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TairVectorTest extends TairVectorTestBase {
    final String index = "default_index";
    final int dims = 8;
    final IndexAlgorithm algorithm = IndexAlgorithm.HNSW;
    final DistanceMethod method = DistanceMethod.IP;
    final long dbid = 2;
    final List<String> index_params = Arrays.asList("ef_construct", "100", "M", "16");
    final List<String> index_params_with_dataType = Arrays.asList("ef_construct", "100", "M", "16", "data_type",
        "BINARY");
    final List<String> ef_params = Arrays.asList("ef_search", "100");

    final List<String[]> test_data = Arrays.asList(
        new String[] { "VECTOR", "[7,3]", "name", "Aaron", "age", "12" }, // dist 58
        new String[] { "VECTOR", "[9,2]", "name", "Bob", "age", "33" }, // dist 85
        new String[] { "VECTOR", "[6,6]", "name", "Charlie", "age", "29" }, // dist 72
        new String[] { "VECTOR", "[3,5]", "name", "Daniel", "age", "23" }, // dist 34
        new String[] { "VECTOR", "[3,7]", "name", "Eason", "age", "22" }, // dist 58
        new String[] { "VECTOR", "[3,6]", "name", "Fabian", "age", "35" }, // dist 45
        new String[] { "VECTOR", "[5,2]", "name", "George", "age", "12" }, // dist 29
        new String[] { "VECTOR", "[8,9]", "name", "Henry", "age", "30" }, // dist 145
        new String[] { "VECTOR", "[5,5]", "name", "Ivan", "age", "16" }, // dist 50
        new String[] { "VECTOR", "[2,7]", "name", "James", "age", "12" }); // dist 53

    /**
     * 127.0.0.1:6379> tvs.createindex default_index 8 HNSW IP
     */
    private void tvs_create_index(int dims, IndexAlgorithm algorithm, DistanceMethod method, final String... attr) {
        tairVector.tvsdelindex(index);
        assertEquals("OK", tairVector.tvscreateindex(index, dims, algorithm, method, attr));
    }

    private void check_index(int dims, IndexAlgorithm algorithm, DistanceMethod method, final String... attr) {
        check_and_create_index(this.index, dims, algorithm, method, attr);
    }

    private void check_and_create_index(String index, int dims, IndexAlgorithm algorithm, DistanceMethod method,
        final String... attr) {
        Map<String, String> objs = tairVector.tvsgetindex(index);
        if (!objs.isEmpty()) {
            long result = tairVector.tvsdelindex(index);
            assertEquals(result, 1);
        }
        assertEquals("OK", tairVector.tvscreateindex(index, dims, algorithm, method, attr));
    }

    private void tvs_hset(final String entityid, final String vector, final String param_k, final String param_v) {
        long result = tairVector.tvshset(index, entityid, vector, param_k, param_v);
        assertEquals(result, 2);
    }

    private void tvs_hset(byte[] entityid, byte[] vector, byte[] param_k, byte[] param_v) {
        long result = tairVector.tvshset(SafeEncoder.encode(index), entityid, vector, param_k, param_v);
        assertTrue(result <= 2);
    }

    private long tvs_del_entity(String... entity) {
        return tairVector.tvsdel(index, entity);
    }

    private long tvs_del_entity(byte[]... entity) {
        return tairVector.tvsdel(SafeEncoder.encode(index), entity);
    }

    private long tvs_del_entity(String entity) {
        return tairVector.tvsdel(index, entity);
    }

    private long tvs_del_entity(byte[] entity) {
        return tairVector.tvsdel(SafeEncoder.encode(index), entity);
    }

    @Test
    public void tvs_create_index() {
        tvs_del_index();
        assertEquals("OK",
            tairVector.tvscreateindex(index, dims, algorithm, method, index_params.toArray(new String[0])));
        try {
            tairVector.tvscreateindex(SafeEncoder.encode(index), dims, algorithm, method);
        } catch (Exception e) {
            assertEquals(e.getMessage(), "ERR duplicated index key");
        }
    }

    @Test
    public void tvs_create_index_with_datatype() {
        tvs_del_index();
        try {
            tairVector.tvscreateindex(index, dims, algorithm, method,
                index_params_with_dataType.toArray(new String[0]));
        } catch (Exception e) {
            assertEquals(e.getMessage(), "ERR index parameters invalid");
        }
        assertEquals("OK", tairVector.tvscreateindex(index, dims, algorithm, DistanceMethod.JACCARD,
            index_params_with_dataType.toArray(new String[0])));
        try {
            tairVector.tvscreateindex(SafeEncoder.encode(index), dims, algorithm, method);
        } catch (Exception e) {
            assertEquals(e.getMessage(), "ERR duplicated index key");
        }
    }

    @Test
    public void tvs_create_index_withoption_args() {
        tvs_del_index();
        assertEquals("OK", tairVector.tvscreateindex(index, dims, algorithm, method,
            "ef_construct", "50", "M", "20"));
        Map<String, String> schema = tairVector.tvsgetindex(index);
        assertEquals(String.valueOf(50), schema.get("ef_construct"));
        assertEquals(String.valueOf(20), schema.get("M"));
    }

    /**
     * 127.0.0.1:6379> tvs.getindex default_index
     */
    @Test
    public void tvs_get_index() {
        tvs_create_index(dims, algorithm, method, index_params.toArray(new String[0]));

        Map<String, String> schema = tairVector.tvsgetindex(index);
        assertEquals(algorithm.name(), schema.get("algorithm"));
        assertEquals(method.name(), schema.get("distance_method"));
        assertEquals(String.valueOf(0), schema.get("data_count"));

        Map<byte[], byte[]> schema_bytecode = tairVector.tvsgetindex(SafeEncoder.encode(index));
        Iterator<Map.Entry<byte[], byte[]>> entries = schema_bytecode.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<byte[], byte[]> entry = entries.next();
            assertEquals(schema.get(SafeEncoder.encode(entry.getKey())), SafeEncoder.encode(entry.getValue()));
        }
    }

    @Test
    public void tvs_del_index() {
        check_index(dims, algorithm, method, index_params.toArray(new String[0]));

        Map<String, String> schema = tairVector.tvsgetindex(index);
        assertEquals(algorithm.name(), schema.get("algorithm"));
        assertEquals(method.name(), schema.get("distance_method"));
        assertEquals(String.valueOf(0), schema.get("data_count"));

        long result = tairVector.tvsdelindex(index);
        assertEquals(result, 1);
        long result_byte = tairVector.tvsdelindex(SafeEncoder.encode(index));
        assertEquals(result_byte, 0);
    }

    @Test
    @Ignore
    public void tvs_scan_index() {
        check_index(dims, algorithm, method, index_params.toArray(new String[0]));

        HscanParams hscanParams = new HscanParams();
        hscanParams.count(1);
        hscanParams.match("default_index");
        ScanResult<String> result = tairVector.tvsscanindex(0L, hscanParams);

        System.out.println(result.getCursor());
        result.getResult().forEach(t -> System.out.println(t));

        assertTrue(Long.valueOf(result.getCursor()) >= 0);
        assertEquals(1, result.getResult().size());
        assertEquals(index, result.getResult().get(0));
    }

    @Test
    public void tvs_hset_data_bin() {
        check_index(dims, algorithm, DistanceMethod.JACCARD, index_params_with_dataType.toArray(new String[0]));
        tvs_del_entity("fourth_entity_knn");
        tvs_hset("fourth_entity_knn", "[1,1,0,0,1,0,1,0]", "name", "sammy");
        tvs_del_entity("ten_entity_knn");
        tvs_hset(SafeEncoder.encode("ten_entity_knn"), SafeEncoder.encode("[1,1,0,0,1,0,1,0]"),
            SafeEncoder.encode("name"), SafeEncoder.encode("tiddy"));
    }

    @Test
    public void tvs_hgetall_data_bin() {
        tvs_del_entity("first_entity_knn");
        tvs_del_entity("second_entity_knn");
        tvs_hset("first_entity_knn", "[1,1,1,1,0,0,0,0]", "name", "sammy");
        tvs_hset(SafeEncoder.encode("second_entity_knn"), SafeEncoder.encode("[1,1,1,1,0,0,0,0]"),
            SafeEncoder.encode("name"), SafeEncoder.encode("tiddy"));

        Map<String, String> entity_string = tairVector.tvshgetall(index, "first_entity_knn");
        assertEquals("[1,1,1,1,0,0,0,0]", entity_string.get(VectorBuilderFactory.VECTOR_TAG));
        assertEquals("sammy", entity_string.get("name"));

        Map<byte[], byte[]> entity_byte = tairVector.tvshgetall(SafeEncoder.encode(index),
            SafeEncoder.encode("first_entity_knn"));
        assertEquals("[1,1,1,1,0,0,0,0]",
            SafeEncoder.encode(entity_byte.get(SafeEncoder.encode(VectorBuilderFactory.VECTOR_TAG))));
        assertEquals("sammy", SafeEncoder.encode(entity_byte.get(SafeEncoder.encode("name"))));
    }

    @Test
    public void tvs_hset() {
        check_index(dims, algorithm, method, index_params.toArray(new String[0]));
        tvs_del_entity("fourth_entity_knn");
        tvs_hset("fourth_entity_knn", "[0.12, 0.23, 0.56, 0.67, 0.78, 0.89, 0.01, 0.89]", "name", "sammy");
        tvs_del_entity("ten_entity_knn");
        tvs_hset(SafeEncoder.encode("ten_entity_knn"),
            SafeEncoder.encode("[0.22, 0.33, 0.66, 0.77, 0.88, 0.89, 0.11, 0.89]"),
            SafeEncoder.encode("name"), SafeEncoder.encode("tiddy"));
    }

    @Test
    public void tvs_hgetall() {
        tvs_del_entity("first_entity_knn");
        tvs_del_entity("second_entity_knn");
        tvs_hset("first_entity_knn", "[0.12, 0.23, 0.56, 0.67, 0.78, 0.89, 0.01, 0.89]", "name", "sammy");
        tvs_hset(SafeEncoder.encode("second_entity_knn"),
            SafeEncoder.encode("[0.22, 0.33, 0.66, 0.77, 0.88, 0.89, 0.11, 0.89]"),
            SafeEncoder.encode("name"), SafeEncoder.encode("tiddy"));

        Map<String, String> entity_string = tairVector.tvshgetall(index, "first_entity_knn");
        assertEquals("[0.12,0.23,0.56,0.67,0.78,0.89,0.01,0.89]", entity_string.get(VectorBuilderFactory.VECTOR_TAG));
        assertEquals("sammy", entity_string.get("name"));

        Map<byte[], byte[]> entity_byte = tairVector.tvshgetall(SafeEncoder.encode(index),
            SafeEncoder.encode("first_entity_knn"));
        assertEquals("[0.12,0.23,0.56,0.67,0.78,0.89,0.01,0.89]",
            SafeEncoder.encode(entity_byte.get(SafeEncoder.encode(VectorBuilderFactory.VECTOR_TAG))));
        assertEquals("sammy", SafeEncoder.encode(entity_byte.get(SafeEncoder.encode("name"))));
    }

    @Test
    public void tvs_hmgetall() {
        check_index(dims, algorithm, method, index_params.toArray(new String[0]));
        tvs_del_entity("first_entity_knn");
        tvs_del_entity("second_entity_knn");
        tvs_hset("first_entity_knn", "[0.12, 0.23, 0.56, 0.67, 0.78, 0.89, 0.01, 0.89]", "name", "sammy");
        tvs_hset(SafeEncoder.encode("second_entity_knn"),
            SafeEncoder.encode("[0.22, 0.33, 0.66, 0.77, 0.88, 0.89, 0.11, 0.89]"),
            SafeEncoder.encode("name"), SafeEncoder.encode("tiddy"));

        List<String> entity_string = tairVector.tvshmget(index, "first_entity_knn", VectorBuilderFactory.VECTOR_TAG,
            "name");
        assertEquals("[0.12,0.23,0.56,0.67,0.78,0.89,0.01,0.89]", entity_string.get(0));
        assertEquals("sammy", entity_string.get(1));

        List<byte[]> entity_byte = tairVector.tvshmget(SafeEncoder.encode(index),
            SafeEncoder.encode("first_entity_knn"),
            SafeEncoder.encode(VectorBuilderFactory.VECTOR_TAG), SafeEncoder.encode("name"));
        assertEquals("[0.12,0.23,0.56,0.67,0.78,0.89,0.01,0.89]", SafeEncoder.encode(entity_byte.get(0)));
        assertEquals("sammy", SafeEncoder.encode(entity_byte.get(1)));
    }

    @Test
    public void tvs_del() {
        check_index(dims, algorithm, method, index_params.toArray(new String[0]));
        tvs_del_entity("first_entity_knn");
        tvs_del_entity("second_entity_knn");

        tvs_hset("first_entity_knn", "[0.12, 0.23, 0.56, 0.67, 0.78, 0.89, 0.01, 0.89]", "name", "sammy");
        tvs_hset(SafeEncoder.encode("second_entity_knn"),
            SafeEncoder.encode("[0.22, 0.33, 0.66, 0.77, 0.88, 0.89, 0.11, 0.89]"),
            SafeEncoder.encode("name"), SafeEncoder.encode("tiddy"));

        long count_string = tvs_del_entity("first_entity_knn", "second_entity_knn");
        assertEquals(2, count_string);

        tvs_hset("first_entity_knn", "[0.12, 0.23, 0.56, 0.67, 0.78, 0.89, 0.01, 0.89]", "name", "sammy");
        tvs_hset(SafeEncoder.encode("second_entity_knn"),
            SafeEncoder.encode("[0.22, 0.33, 0.66, 0.77, 0.88, 0.89, 0.11, 0.89]"),
            SafeEncoder.encode("name"), SafeEncoder.encode("tiddy"));
        long  count_byte = tvs_del_entity(SafeEncoder.encode("first_entity_knn"), SafeEncoder.encode("second_entity_knn"));
        assertEquals(2, count_byte);
    }

    @Test
    public void tvs_hdel() {
        check_index(dims, algorithm, method, index_params.toArray(new String[0]));
        tvs_del_entity("first_entity_knn");
        tvs_del_entity("second_entity_knn");

        tvs_hset("first_entity_knn", "[0.12, 0.23, 0.56, 0.67, 0.78, 0.89, 0.01, 0.89]", "name", "sammy");
        tvs_hset(SafeEncoder.encode("second_entity_knn"),
            SafeEncoder.encode("[0.22, 0.33, 0.66, 0.77, 0.88, 0.89, 0.11, 0.89]"),
            SafeEncoder.encode("name"), SafeEncoder.encode("tiddy"));

        long count_string = tairVector.tvshdel(index, "first_entity_knn", "name");
        assertEquals(1, count_string);
        Map<String, String> entity_string = tairVector.tvshgetall(index, "first_entity_knn");
        assertTrue(entity_string.size() == 1 && (!entity_string.containsKey("name")));

        long count_byte = tairVector.tvshdel(SafeEncoder.encode(index), SafeEncoder.encode("second_entity_knn"),
            SafeEncoder.encode(VectorBuilderFactory.VECTOR_TAG));
        // assertEquals(1, count_byte);
        Map<String, String> entity_byte = tairVector.tvshgetall(index, "second_entity_knn");
        assertTrue(entity_byte.size() == 1 && (!entity_byte.containsKey(VectorBuilderFactory.VECTOR_TAG)));
    }

    @Test
    public void tvs_scan() {
        check_index(dims, algorithm, method, index_params.toArray(new String[0]));
        tvs_del_entity("first_entity_knn");
        tvs_hset("first_entity_knn", "[0.12, 0.23, 0.56, 0.67, 0.78, 0.89, 0.01, 0.89]", "name", "sammy");

        tvs_del_entity("five_entity_knn");
        tvs_hset(SafeEncoder.encode("five_entity_knn"),
            SafeEncoder.encode("[0.22, 0.33, 0.66, 0.77, 0.88, 0.89, 0.11, 0.89]"),
            SafeEncoder.encode("name"), SafeEncoder.encode("tiddy"));

        long cursor = 0;
        HscanParams hscanParams = new HscanParams();
        hscanParams.count(1);
        hscanParams.match("*entit*");
        ScanResult<String> result_string = tairVector.tvsscan(index, cursor, hscanParams);
        assert (result_string.getResult().size() >= 1);

        ScanResult<byte[]> entity_byte = tairVector.tvsscan(SafeEncoder.encode(index), cursor, hscanParams);
        assert (entity_byte.getResult().size() >= 1);
    }

    @Test
    public void tvs_knnsearch() {
        check_index(dims, algorithm, method, index_params.toArray(new String[0]));
        tvs_del_entity("first_entity_knn");
        tvs_del_entity(SafeEncoder.encode("second_entity_knn"));

        tvs_hset("first_entity_knn", "[0.12, 0.23, 0.56, 0.67, 0.78, 0.89, 0.01, 0.89]", "name", "sammy");
        tvs_hset(SafeEncoder.encode("second_entity_knn"),
            SafeEncoder.encode("[0.22, 0.33, 0.66, 0.77, 0.88, 0.89, 0.11, 0.89]"),
            SafeEncoder.encode("name"), SafeEncoder.encode("tiddy"));

        long topn = 2L;
        String query = "[0.12, 0.23, 0.56, 0.67, 0.78, 0.89, 0.01, 0.89]";
        VectorBuilderFactory.Knn<String> result_string = tairVector.tvsknnsearch(index, topn,query);
        assertEquals(2, result_string.getKnnResults().size());

        VectorBuilderFactory.Knn<byte[]> entity_byte = tairVector.tvsknnsearch(SafeEncoder.encode(index), topn,
            SafeEncoder.encode(query));
        assertEquals(2, entity_byte.getKnnResults().size());

        Collection<String> fields = Collections.singletonList("name");
        VectorBuilderFactory.KnnField<String> entity_string_field = tairVector.tvsknnsearchfield(index, topn, query, fields);
        assertEquals(2, entity_string_field.getKnnResults().size());
        entity_string_field.getKnnResults().forEach(knn -> {
            assertTrue(knn.getIndex().isEmpty());
            assertEquals(1, knn.getFields().size());
            assertEquals(knn.getFields().get("name"), knn.getId().equals("first_entity_knn") ? "sammy" : "tiddy");
        });

        Collection<byte[]> byte_fields = Collections.singletonList(SafeEncoder.encode("name"));
        VectorBuilderFactory.KnnField<byte[]> entity_byte_field = tairVector.tvsknnsearchfield(SafeEncoder.encode(index),
            topn, SafeEncoder.encode(query), byte_fields);
        assertEquals(2, entity_byte_field.getKnnResults().size());
        entity_byte_field.getKnnResults().forEach(knn -> {
            assertTrue(SafeEncoder.encode(knn.getIndex()).isEmpty());
            assertEquals(1, knn.getFields().size());
            assertArrayEquals(knn.getFields().get(SafeEncoder.encode("name")),
                Arrays.equals(knn.getId(), SafeEncoder.encode("first_entity_knn")) ?
                    SafeEncoder.encode("sammy") : SafeEncoder.encode("tiddy"));
        });
    }

    @Test
    public void tvs_knnsearch_with_databin() {
        check_index(dims, algorithm, DistanceMethod.JACCARD, index_params_with_dataType.toArray(new String[0]));
        tvs_del_entity("first_entity_knn");
        tvs_del_entity(SafeEncoder.encode("second_entity_knn"));

        tvs_hset("first_entity_knn", "[1,1,1,1,0,0,0,0]", "name", "sammy");
        tvs_hset(SafeEncoder.encode("second_entity_knn"), SafeEncoder.encode("[1,1,1,1,0,0,0,0]"),
            SafeEncoder.encode("name"), SafeEncoder.encode("tiddy"));

        long topn = 2L;
        String query = "[1,1,1,1,0,0,0,0]";
        VectorBuilderFactory.Knn<String> result_string = tairVector.tvsknnsearch(index, topn, query);
        assertEquals(2, result_string.getKnnResults().size());

        VectorBuilderFactory.Knn<byte[]> entity_byte = tairVector.tvsknnsearch(SafeEncoder.encode(index), topn,
            SafeEncoder.encode(query));
        assertEquals(2, entity_byte.getKnnResults().size());

        Collection<String> fields = Collections.singletonList("name");
        VectorBuilderFactory.KnnField<String> entity_string_field = tairVector.tvsknnsearchfield(index, topn, query, fields);
        assertEquals(2, entity_string_field.getKnnResults().size());
        entity_string_field.getKnnResults().forEach(knn -> {
            assertTrue(knn.getIndex().isEmpty());
            assertEquals(1, knn.getFields().size());
            assertEquals(knn.getFields().get("name"), knn.getId().equals("first_entity_knn") ? "sammy" : "tiddy");
        });

        Collection<byte[]> byte_fields = Collections.singletonList(SafeEncoder.encode("name"));
        VectorBuilderFactory.KnnField<byte[]> entity_byte_field = tairVector.tvsknnsearchfield(SafeEncoder.encode(index),
            topn, SafeEncoder.encode(query), byte_fields);
        assertEquals(2, entity_byte_field.getKnnResults().size());
        entity_byte_field.getKnnResults().forEach(knn -> {
            assertTrue(SafeEncoder.encode(knn.getIndex()).isEmpty());
            assertEquals(1, knn.getFields().size());
            assertArrayEquals(knn.getFields().get(SafeEncoder.encode("name")),
                Arrays.equals(knn.getId(), SafeEncoder.encode("first_entity_knn")) ?
                    SafeEncoder.encode("sammy") : SafeEncoder.encode("tiddy"));
        });
    }

    @Test
    public void tvs_knnsearch_with_filter() {
        tairVector.tvsdelindex(SafeEncoder.encode(index));

        check_index(dims, algorithm, method, index_params.toArray(new String[0]));
        tvs_del_entity("first_entity_knn");
        tvs_del_entity(SafeEncoder.encode("second_entity_knn"));

        tvs_hset("first_entity_knn", "[0.12, 0.23, 0.56, 0.67, 0.78, 0.89, 0.01, 0.89]", "name", "sammy");
        tvs_hset(SafeEncoder.encode("second_entity_knn"),
            SafeEncoder.encode("[0.22, 0.33, 0.66, 0.77, 0.88, 0.89, 0.11, 0.89]"),
            SafeEncoder.encode("name"), SafeEncoder.encode("tiddy"));

        long topn = 10L;
        String query = "[0.12, 0.23, 0.56, 0.67, 0.78, 0.89, 0.01, 0.89]";
        String filter = "name != \"sammy\"";
        VectorBuilderFactory.Knn<String> result_string = tairVector.tvsknnsearchfilter(index, topn, query, filter);
        assertEquals(1, result_string.getKnnResults().size());

        VectorBuilderFactory.Knn<byte[]> entity_byte = tairVector.tvsknnsearchfilter(SafeEncoder.encode(index), topn,
            SafeEncoder.encode(query), SafeEncoder.encode(filter));
        assertEquals(1, entity_byte.getKnnResults().size());

        Collection<String> fields = Collections.singletonList("name");
        VectorBuilderFactory.KnnField<String> entity_string_field = tairVector.tvsknnsearchfilterfield(index, topn, query, fields, filter);
        assertEquals(1, entity_string_field.getKnnResults().size());
        entity_string_field.getKnnResults().forEach(knn -> {
            assertTrue(knn.getIndex().isEmpty());
            assertEquals(1, knn.getFields().size());
            assertEquals(knn.getFields().get("name"), knn.getId().equals("first_entity_knn") ? "sammy" : "tiddy");
        });

        Collection<byte[]> byte_fields = Collections.singletonList(SafeEncoder.encode("name"));
        VectorBuilderFactory.KnnField<byte[]> entity_byte_field = tairVector.tvsknnsearchfilterfield(SafeEncoder.encode(index),
            topn, SafeEncoder.encode(query), byte_fields, SafeEncoder.encode(filter));
        assertEquals(1, entity_byte_field.getKnnResults().size());
        entity_byte_field.getKnnResults().forEach(knn -> {
            assertTrue(SafeEncoder.encode(knn.getIndex()).isEmpty());
            assertEquals(1, knn.getFields().size());
            assertArrayEquals(knn.getFields().get(SafeEncoder.encode("name")),
                Arrays.equals(knn.getId(), SafeEncoder.encode("first_entity_knn")) ?
                    SafeEncoder.encode("sammy") : SafeEncoder.encode("tiddy"));
        });
    }

    @Test
    public void tvs_knnsearch_with_params() {
        check_index(dims, algorithm, method, index_params.toArray(new String[0]));
        tvs_del_entity("first_entity_knn");
        tvs_del_entity(SafeEncoder.encode("second_entity_knn"));

        tvs_hset("first_entity_knn", "[0.12, 0.23, 0.56, 0.67, 0.78, 0.89, 0.01, 0.89]", "name", "sammy");
        tvs_hset(SafeEncoder.encode("second_entity_knn"),
            SafeEncoder.encode("[0.22, 0.33, 0.66, 0.77, 0.88, 0.89, 0.11, 0.89]"),
            SafeEncoder.encode("name"), SafeEncoder.encode("tiddy"));

        long topn = 2L;
        String query = "[0.12, 0.23, 0.56, 0.67, 0.78, 0.89, 0.01, 0.89]";
        VectorBuilderFactory.Knn<String> result_string = tairVector.tvsknnsearch(index, topn, query,
            ef_params.toArray(new String[0]));
        assertEquals(2, result_string.getKnnResults().size());

        VectorBuilderFactory.Knn<byte[]> entity_byte = tairVector.tvsknnsearch(SafeEncoder.encode(index), topn,
            SafeEncoder.encode(query), SafeEncoder.encodeMany(ef_params.toArray(new String[0])));
        assertEquals(2, entity_byte.getKnnResults().size());

        Collection<String> fields = Collections.singletonList("name");
        VectorBuilderFactory.KnnField<String> entity_string_field = tairVector.tvsknnsearchfield(index, topn, query,
            fields, ef_params.toArray(new String[0]));
        assertEquals(2, entity_string_field.getKnnResults().size());
        entity_string_field.getKnnResults().forEach(knn -> {
            assertTrue(knn.getIndex().isEmpty());
            assertEquals(1, knn.getFields().size());
            assertEquals(knn.getFields().get("name"), knn.getId().equals("first_entity_knn") ? "sammy" : "tiddy");
        });

        Collection<byte[]> byte_fields = Collections.singletonList(SafeEncoder.encode("name"));
        VectorBuilderFactory.KnnField<byte[]> entity_byte_field = tairVector.tvsknnsearchfield(SafeEncoder.encode(index),
            topn, SafeEncoder.encode(query), byte_fields, SafeEncoder.encodeMany(ef_params.toArray(new String[0])));
        assertEquals(2, entity_byte_field.getKnnResults().size());
        entity_byte_field.getKnnResults().forEach(knn -> {
            assertTrue(SafeEncoder.encode(knn.getIndex()).isEmpty());
            assertEquals(1, knn.getFields().size());
            assertArrayEquals(knn.getFields().get(SafeEncoder.encode("name")),
                Arrays.equals(knn.getId(), SafeEncoder.encode("first_entity_knn")) ?
                    SafeEncoder.encode("sammy") : SafeEncoder.encode("tiddy"));
        });
    }

    @Test
    public void tvs_mknnsearch() {
        check_index(dims, algorithm, method, index_params.toArray(new String[0]));
        tvs_del_entity("first_entity_knn");
        tvs_del_entity("second_entity_knn");

        tvs_hset("first_entity_knn", "[0.12, 0.23, 0.56, 0.67, 0.78, 0.89, 0.01, 0.89]", "name", "sammy");
        tvs_hset(SafeEncoder.encode("second_entity_knn"),
            SafeEncoder.encode("[0.22, 0.33, 0.66, 0.77, 0.88, 0.89, 0.11, 0.89]"),
            SafeEncoder.encode("name"), SafeEncoder.encode("tiddy"));

        long topn = 2L;
        List<String> vectors = Arrays.asList("[0.12, 0.23, 0.56, 0.67, 0.78, 0.89, 0.01, 0.89]",
            "[0.22, 0.33, 0.66, 0.77, 0.88, 0.89, 0.11, 0.89]");
        Collection<VectorBuilderFactory.Knn<String>> result_string = tairVector.tvsmknnsearch(index, topn, vectors);
        result_string.forEach(result -> {
            assertEquals(2, result.getKnnResults().size());
        });
        result_string.forEach(one -> System.out.printf("string: %s\n", one.toString()));

        Collection<VectorBuilderFactory.Knn<byte[]>> result_byte = tairVector.tvsmknnsearch(SafeEncoder.encode(index),
            topn,
            vectors.stream().map(item -> SafeEncoder.encode(item)).collect(Collectors.toList()));
        result_byte.forEach(result -> {
            assertEquals(2, result.getKnnResults().size());
        });
        result_string.forEach(one -> System.out.printf("byte: %s\n", one.toString()));
    }

    @Test
    public void tvs_mknnsearch_filter() {
        check_index(dims, algorithm, method, index_params.toArray(new String[0]));
        tvs_del_entity("first_entity_knn");
        tvs_del_entity("second_entity_knn");

        tvs_hset("first_entity_knn", "[0.12, 0.23, 0.56, 0.67, 0.78, 0.89, 0.01, 0.89]", "name", "sammy");
        tvs_hset(SafeEncoder.encode("second_entity_knn"),
            SafeEncoder.encode("[0.22, 0.33, 0.66, 0.77, 0.88, 0.89, 0.11, 0.89]"),
            SafeEncoder.encode("name"), SafeEncoder.encode("tiddy"));

        long topn = 1L;
        List<String> vectors = Arrays.asList("[0.12, 0.23, 0.56, 0.67, 0.78, 0.89, 0.01, 0.89]",
            "[0.22, 0.33, 0.66, 0.77, 0.88, 0.89, 0.11, 0.89]");
        String pattern = "name == \"no-sammy\"";
        Collection<VectorBuilderFactory.Knn<String>> result_string = tairVector.tvsmknnsearchfilter(index, topn,
            vectors, pattern);
        result_string.forEach(result -> {
            assertEquals(0, result.getKnnResults().size());
        });
        result_string.forEach(one -> System.out.printf("string: %s\n", one.toString()));

        Collection<VectorBuilderFactory.Knn<byte[]>> result_byte = tairVector.tvsmknnsearchfilter(
            SafeEncoder.encode(index),
            topn, vectors.stream().map(item -> SafeEncoder.encode(item)).collect(Collectors.toList()),
            SafeEncoder.encode(pattern));
        result_byte.forEach(result -> {
            assertEquals(0, result.getKnnResults().size());
        });
        result_string.forEach(one -> System.out.printf("byte: %s\n", one.toString()));
    }

    @Test
    public void tvs_mknnsearch_with_params() {
        check_index(dims, algorithm, method, index_params.toArray(new String[0]));
        tvs_del_entity("first_entity_knn");
        tvs_del_entity("second_entity_knn");

        tvs_hset("first_entity_knn", "[0.12, 0.23, 0.56, 0.67, 0.78, 0.89, 0.01, 0.89]", "name", "sammy");
        tvs_hset(SafeEncoder.encode("second_entity_knn"),
            SafeEncoder.encode("[0.22, 0.33, 0.66, 0.77, 0.88, 0.89, 0.11, 0.89]"),
            SafeEncoder.encode("name"), SafeEncoder.encode("tiddy"));

        long topn = 2L;
        List<String> vectors = Arrays.asList("[0.12, 0.23, 0.56, 0.67, 0.78, 0.89, 0.01, 0.89]",
            "[0.22, 0.33, 0.66, 0.77, 0.88, 0.89, 0.11, 0.89]");
        Collection<VectorBuilderFactory.Knn<String>> result_string = tairVector.tvsmknnsearch(index, topn, vectors,
            ef_params.toArray(new String[0]));
        result_string.forEach(result -> {
            assertEquals(2, result.getKnnResults().size());
        });
        result_string.forEach(one -> System.out.printf("string: %s\n", one.toString()));

        Collection<VectorBuilderFactory.Knn<byte[]>> result_byte = tairVector.tvsmknnsearch(SafeEncoder.encode(index),
            topn,
            vectors.stream().map(item -> SafeEncoder.encode(item)).collect(Collectors.toList()),
            SafeEncoder.encodeMany(ef_params.toArray(new String[0])));
        result_byte.forEach(result -> {
            assertEquals(2, result.getKnnResults().size());
        });
        result_string.forEach(one -> System.out.printf("byte: %s\n", one.toString()));
    }

    @Test
    public void tvs_mindexknnsearch_with_params() {
        check_and_create_index("index1", dims, algorithm, DistanceMethod.L2, index_params.toArray(new String[0]));
        check_and_create_index("index2", dims, algorithm, DistanceMethod.L2, index_params.toArray(new String[0]));

        long result = tairVector.tvshset("index1", "first_entity_knn", "[1, 1, 1, 1, 1, 1, 1, 1]", "name", "sammy");
        assertEquals(result, 2);
        result = tairVector.tvshset("index1", "second_entity_knn", "[3, 1, 1, 1, 1, 1, 1, 1]", "name", "sammy");
        assertEquals(result, 2);
        result = tairVector.tvshset("index2", "third_entity_knn", "[2, 1, 1, 1, 1, 1, 1, 1]", "name", "sammy");
        assertEquals(result, 2);
        result = tairVector.tvshset("index2", "fourth_entity_knn", "[4, 1, 1, 1, 1, 1, 1, 1]", "name", "sammy");
        assertEquals(result, 2);

        long topn = 2L;
        List<String> indexs = Arrays.asList("index1", "index2");
        String vector = "[0, 0, 0, 0, 0, 0, 0, 0]";
        VectorBuilderFactory.Knn<String> result_string = tairVector.tvsmindexknnsearch(indexs, topn, vector,
            ef_params.toArray(new String[0]));
        assertEquals(2, result_string.getKnnResults().size());
        VectorBuilderFactory.Knn<byte[]> result_byte = tairVector.tvsmindexknnsearch(
            indexs.stream().map(SafeEncoder::encode).collect(Collectors.toList()), topn,
            SafeEncoder.encode(vector), SafeEncoder.encodeMany(ef_params.toArray(new String[0])));
        assertEquals(2, result_byte.getKnnResults().size());

        Collection<String> fields = Collections.singletonList("name");
        VectorBuilderFactory.KnnField<String> entity_string_field = tairVector.tvsmindexknnsearchField(indexs, topn,
            vector, fields, ef_params.toArray(new String[0]));
        assertEquals(2, entity_string_field.getKnnResults().size());
        entity_string_field.getKnnResults().forEach(knn -> {
            assertEquals(knn.getIndex(), knn.getId().equals("first_entity_knn") ? "index1" : "index2");
            assertEquals(1, knn.getFields().size());
            assertEquals(knn.getFields().get("name"), "sammy");
        });

        Collection<byte[]> byte_fields = Collections.singletonList(SafeEncoder.encode("name"));
        VectorBuilderFactory.KnnField<byte[]> entity_byte_field = tairVector.tvsmindexknnsearchField(
            indexs.stream().map(SafeEncoder::encode).collect(Collectors.toList()), topn,
            SafeEncoder.encode(vector), byte_fields, SafeEncoder.encodeMany(ef_params.toArray(new String[0])));
        assertEquals(2, entity_byte_field.getKnnResults().size());
        entity_byte_field.getKnnResults().forEach(knn -> {
            assertArrayEquals(knn.getIndex(), Arrays.equals(knn.getId(), SafeEncoder.encode("first_entity_knn")) ?
                SafeEncoder.encode("index1") : SafeEncoder.encode("index2"));
            assertEquals(1, knn.getFields().size());
            assertArrayEquals(knn.getFields().get(SafeEncoder.encode("name")), SafeEncoder.encode("sammy"));
        });

        tairVector.tvsdelindex("index1");
        tairVector.tvsdelindex("index2");
    }

    @Test
    public void tvs_mindexmknnsearch_with_params() {
        check_and_create_index("index1", dims, algorithm, DistanceMethod.L2, index_params.toArray(new String[0]));
        check_and_create_index("index2", dims, algorithm, DistanceMethod.L2, index_params.toArray(new String[0]));

        long result = tairVector.tvshset("index1", "first_entity_knn", "[1, 1, 1, 1, 1, 1, 1, 1]", "name", "sammy");
        assertEquals(result, 2);
        result = tairVector.tvshset("index1", "second_entity_knn", "[3, 1, 1, 1, 1, 1, 1, 1]", "name", "sammy");
        assertEquals(result, 2);
        result = tairVector.tvshset("index2", "third_entity_knn", "[2, 1, 1, 1, 1, 1, 1, 1]", "name", "sammy");
        assertEquals(result, 2);
        result = tairVector.tvshset("index2", "fourth_entity_knn", "[4, 1, 1, 1, 1, 1, 1, 1]", "name", "sammy");
        assertEquals(result, 2);

        long topn = 2L;
        List<String> indexs = Arrays.asList("index1", "index2");
        List<String> vectors = Arrays.asList("[0, 0, 0, 0, 0, 0, 0, 0]", "[1, 1, 1, 1, 1, 1, 1, 1]");

        Collection<VectorBuilderFactory.Knn<String>> result_string = tairVector.tvsmindexmknnsearch(indexs, topn,
            vectors, ef_params.toArray(new String[0]));
        result_string.forEach(res -> {
            assertEquals(2, res.getKnnResults().size());
        });
        Collection<VectorBuilderFactory.Knn<byte[]>> result_byte = tairVector.tvsmindexmknnsearch(
            indexs.stream().map(item -> SafeEncoder.encode(item)).collect(Collectors.toList()), topn,
            vectors.stream().map(item -> SafeEncoder.encode(item)).collect(Collectors.toList()),
            SafeEncoder.encodeMany(ef_params.toArray(new String[0])));
        result_byte.forEach(res -> {
            assertEquals(2, res.getKnnResults().size());
        });

        tairVector.tvsdelindex("index1");
        tairVector.tvsdelindex("index2");
    }

    @Test
    public void tvs_scan_with_filter_and_max_distance() {
        final String index_name = "scan_test";
        check_and_create_index(index_name, 2, algorithm, DistanceMethod.L2);

        for (int i = 0; i < test_data.size(); ++i) {
            String[] args = test_data.get(i);
            Long ret = tairVector.tvshset(index_name, String.format("key-%d", i), args[1],
                Arrays.copyOfRange(args, 2, 6));
            assertEquals(3, ret.longValue());
        }

        HscanParams params = new HscanParams();
        // scan with filter
        params.count(10).filter("name>\"H\"");
        ScanResult<String> result = tairVector.tvsscan(index_name, 0L, params);
        assertEquals(3, result.getResult().size());
        assertTrue(result.getResult().indexOf("key-7") >= 0);
        assertTrue(result.getResult().indexOf("key-8") >= 0);
        assertTrue(result.getResult().indexOf("key-9") >= 0);

        // scan with max distance
        params = new HscanParams();
        params.count(10).vector("[0, 0]").max_dist(50);
        result = tairVector.tvsscan(index_name, 0L, params);
        assertEquals(3, result.getResult().size());
        assertTrue(result.getResult().indexOf("key-3") >= 0);
        assertTrue(result.getResult().indexOf("key-5") >= 0);
        assertTrue(result.getResult().indexOf("key-6") >= 0);

        // scan with both filter and max distance
        params = new HscanParams();
        params.count(10).vector("[0, 0]").max_dist(50).filter("age<30");
        result = tairVector.tvsscan(index_name, 0L, params);
        assertEquals(2, result.getResult().size());
        assertTrue(result.getResult().indexOf("key-3") >= 0);
        assertTrue(result.getResult().indexOf("key-6") >= 0);

        tairVector.tvsdelindex(index_name);
    }

    @Test
    public void tvs_knnsearch_with_max_distance() {
        final String index_name = "knnsearch_test";
        check_and_create_index(index_name, 2, algorithm, DistanceMethod.L2);

        for (int i = 0; i < test_data.size(); ++i) {
            String[] args = test_data.get(i);
            Long ret = tairVector.tvshset(index_name, String.format("key-%d", i), args[1],
                Arrays.copyOfRange(args, 2, 6));
            assertEquals(3, ret.longValue());
        }

        VectorBuilderFactory.Knn<String> result = tairVector.tvsknnsearch(index_name, 5L, "[0,0]", "MAX_DIST", "50");
        assertEquals(4, result.getKnnResults().size());
        VectorBuilderFactory.KnnItem<String>[] items = (VectorBuilderFactory.KnnItem<String>[]) result.getKnnResults()
            .toArray(new VectorBuilderFactory.KnnItem<?>[0]);
        assertEquals(items[0].getId(), "key-6");
        assertEquals(items[1].getId(), "key-3");
        assertEquals(items[2].getId(), "key-5");
        assertEquals(items[3].getId(), "key-8");

        result = tairVector.tvsknnsearchfilter(index_name, 5L, "[0,0]", "age<20", "MAX_DIST", "50");
        System.out.println(result.toString());
        assertEquals(2, result.getKnnResults().size());
        items = (VectorBuilderFactory.KnnItem<String>[]) result.getKnnResults()
            .toArray(new VectorBuilderFactory.KnnItem<?>[0]);
        assertEquals(items[0].getId(), "key-6");
        assertEquals(items[1].getId(), "key-8");

        tairVector.tvsdelindex(index_name);
    }

    @Test
    public void tvs_hincrby_tvs_hincrbyfloat() {
        final String index_name = "tvs_hincrby_test";
        check_and_create_index(index_name, 2, algorithm, DistanceMethod.L2);
        long tvshincrby = tairVector.tvshincrby(index_name, "entityid1", "field1", 2);
        assertEquals(2, tvshincrby);
        tvshincrby = tairVector.tvshincrby(SafeEncoder.encode(index_name), SafeEncoder.encode("entityid1"), SafeEncoder.encode("field1"), 2);
        assertEquals(4, tvshincrby);

        double tvshincrbyfloat = tairVector.tvshincrbyfloat(index_name, "entityid2", "field1", 1.5d);
        assertEquals(Double.compare(1.5d, tvshincrbyfloat), 0);
        tvshincrbyfloat = tairVector.tvshincrbyfloat(SafeEncoder.encode(index_name), SafeEncoder.encode("entityid2"), SafeEncoder.encode("field1"), 1.5d);
        assertEquals(Double.compare(3.0d, tvshincrbyfloat), 0);
        tairVector.tvsdelindex(index_name);
    }


    public void tvs_getdistance() {
        final String index_name = "getdistance_test";
        check_and_create_index(index_name, 2, algorithm, DistanceMethod.L2);

        for (int i = 0; i < test_data.size(); ++i) {
            String[] args = test_data.get(i);
            Long ret = tairVector.tvshset(index_name, String.format("key-%d", i), args[1],
                Arrays.copyOfRange(args, 2, 6));
            assertEquals(3, ret.longValue());
        }

        List<String> keys = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            String key = "key-" + i;
            keys.add(key);
        }

        // getdistance
        {
            VectorBuilderFactory.Knn<String> result = tairVector.tvsgetdistance(index_name, "[0,0]", keys, null, null, null);
            assertEquals(10, result.getKnnResults().size());
        }
        {
            VectorBuilderFactory.Knn<byte[]> result = tairVector.tvsgetdistance(SafeEncoder.encode(index_name), SafeEncoder.encode("[0,0]"),
                keys.stream().map(key -> SafeEncoder.encode(key)).collect(Collectors.toList()), null, null, null);
            assertEquals(10, result.getKnnResults().size());
        }

        // getdistance with TOPN
        {
            VectorBuilderFactory.Knn<String> result = tairVector.tvsgetdistance(index_name, "[0,0]", keys, Long.valueOf(5), null, null);
            assertEquals(5, result.getKnnResults().size());
            KnnItem<String> items[] = result.getKnnResults().toArray(new KnnItem[0]);
            for (int i = 0; i < items.length - 1; ++i) {
                assertTrue(items[i].getScore() <= items[i + 1].getScore());
            }
        }
        {
            VectorBuilderFactory.Knn<byte[]> result = tairVector.tvsgetdistance(SafeEncoder.encode(index_name), SafeEncoder.encode("[0,0]"),
                keys.stream().map(key -> SafeEncoder.encode(key)).collect(Collectors.toList()), Long.valueOf(5), null, null);
            assertEquals(5, result.getKnnResults().size());
            KnnItem<byte[]> items[] = result.getKnnResults().toArray(new KnnItem[0]);
            for (int i = 0; i < items.length - 1; ++i) {
                assertTrue(items[i].getScore() <= items[i + 1].getScore());
            }
        }

        // getdistance with MAX_DIST
        {
            VectorBuilderFactory.Knn<String> result = tairVector.tvsgetdistance(index_name, "[0,0]", keys, null, Float.valueOf(50), null);
            assertEquals(3, result.getKnnResults().size());
            KnnItem<String> items[] = result.getKnnResults().toArray(new KnnItem[0]);
            for (int i = 0; i < items.length; ++i) {
                assertTrue(items[i].getScore() < 50);
            }
        }
        {
            VectorBuilderFactory.Knn<byte[]> result = tairVector.tvsgetdistance(SafeEncoder.encode(index_name), SafeEncoder.encode("[0,0]"),
                keys.stream().map(key -> SafeEncoder.encode(key)).collect(Collectors.toList()), null, Float.valueOf(50), null);
            assertEquals(3, result.getKnnResults().size());
            KnnItem<byte[]> items[] = result.getKnnResults().toArray(new KnnItem[0]);
            for (int i = 0; i < items.length; ++i) {
                assertTrue(items[i].getScore() < 50);
            }
        }

        // getdistance with FILTER
        {
            VectorBuilderFactory.Knn<String> result = tairVector.tvsgetdistance(index_name, "[0,0]", keys, null, null, "name>\"H\"");
            assertEquals(3, result.getKnnResults().size());
            KnnItem<String> items[] = result.getKnnResults().toArray(new KnnItem[0]);
            for (int i = 0; i < items.length; ++i) {
                assertTrue(items[i].getId().equals("key-7") || items[i].getId().equals("key-8") || items[i].getId().equals("key-9"));
            }
        }
        {
            VectorBuilderFactory.Knn<byte[]> result = tairVector.tvsgetdistance(SafeEncoder.encode(index_name), SafeEncoder.encode("[0,0]"),
                keys.stream().map(key -> SafeEncoder.encode(key)).collect(Collectors.toList()), null, null, SafeEncoder.encode("name>\"H\""));
            assertEquals(3, result.getKnnResults().size());
            KnnItem<byte[]> items[] = result.getKnnResults().toArray(new KnnItem[0]);
            for (int i = 0; i < items.length; ++i) {
                assertTrue(SafeEncoder.encode((byte[]) items[i].getId()).equals("key-7") ||
                    SafeEncoder.encode((byte[]) items[i].getId()).equals("key-8") ||
                    SafeEncoder.encode((byte[]) items[i].getId()).equals("key-9"));
            }
        }

        tairVector.tvsdelindex(index_name);
    }

    @Test
    public void tvs_hexpire() {
        check_and_create_index(index, dims, algorithm, method);
        String key = UUID.randomUUID().toString();
        String vector = generateVector(dims);
        assertEquals(3, tairVector.tvshset(index, key, vector,
            "name", "tom", "age", String.valueOf(random.nextInt(100))).intValue());
        assertTrue(tairVector.tvshexpire(index, key, 100));
        Long ttl = tairVector.tvshttl(index, key);
        assertTrue(0 < ttl && ttl <= 100);
    }

    @Test
    public void tvs_hpexpire() {
        check_and_create_index(index, dims, algorithm, method);
        String key = UUID.randomUUID().toString();
        String vector = generateVector(dims);
        assertEquals(3, tairVector.tvshset(index, key, vector,
            "name", "tom", "age", String.valueOf(random.nextInt(100))).intValue());
        assertTrue(tairVector.tvshpexpire(index, key, 100));
        Long ttl = tairVector.tvshpttl(index, key);
        assertTrue(0 < ttl && ttl <= 100);
    }

    @Test
    public void tvs_hexpireat() {
        check_and_create_index(index, dims, algorithm, method);
        String key = UUID.randomUUID().toString();
        String vector = generateVector(dims);
        assertEquals(3, tairVector.tvshset(index, key, vector,
            "name", "tom", "age", String.valueOf(random.nextInt(100))).intValue());
        Long unixTime = System.currentTimeMillis() / 1000 + 100;
        assertTrue(tairVector.tvshexpireAt(index, key, unixTime));
        Long ttl = tairVector.tvshttl(index, key);
        assertTrue(0 < ttl && ttl <= 100);
        assertEquals(tairVector.tvshexpiretime(index, key), unixTime);
    }

    @Test
    public void tvs_hpexpireat() {
        check_and_create_index(index, dims, algorithm, method);
        String key = UUID.randomUUID().toString();
        String vector = generateVector(dims);
        assertEquals(3, tairVector.tvshset(index, key, vector,
            "name", "tom", "age", String.valueOf(random.nextInt(100))).intValue());
        Long unixTime = System.currentTimeMillis() + 100;
        assertTrue(tairVector.tvshpexpireAt(index, key, unixTime));
        Long ttl = tairVector.tvshpttl(index, key);
        assertTrue(0 < ttl && ttl <= 100);
        assertEquals(tairVector.tvshpexpiretime(index, key), unixTime);
    }
}
