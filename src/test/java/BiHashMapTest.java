import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import sharedconfig.utils.collections.BiHashMap;

public class BiHashMapTest {
    @Test
    void testSamePairAdding() {
        val map = new BiHashMap<String, String>();
        map.putKV("1", "2");
        map.putKV("1", "2");

        Assertions.assertTrue(map.containsKey("1"));
        Assertions.assertTrue(map.containsValue("2"));
        Assertions.assertEquals(1, map.size());
    }

    @Test
    void testChangeExistingMappingWithFixedFirstValue() {
        val map = new BiHashMap<String, String>();
        map.putKV("1", "2");
        map.putKV("1", "3");

        Assertions.assertTrue(map.containsKey("1"));
        Assertions.assertTrue(map.containsValue("3"));
        Assertions.assertFalse(map.containsValue("2"));
        Assertions.assertEquals(1, map.size());
    }

    @Test
    void testChangeExistingMappingWithFixedSecondValue() {
        val map = new BiHashMap<String, String>();
        map.putKV("1", "2");
        map.putKV("0", "2");

        Assertions.assertTrue(map.containsKey("0"));
        Assertions.assertTrue(map.containsValue("2"));
        Assertions.assertEquals(1, map.size());
    }

    @Test
    void testNoChangesForSwappedPairs() {
        val map = new BiHashMap<String, String>();
        map.putKV("1", "2");
        map.putKV("2", "1");
        map.putKV("3", "4");
        map.putKV("4", "3");

        Assertions.assertEquals(4, map.size());

        Assertions.assertTrue(map.containsKey("1"));
        Assertions.assertTrue(map.containsValue("2"));

        Assertions.assertTrue(map.containsKey("2"));
        Assertions.assertTrue(map.containsValue("1"));

        Assertions.assertTrue(map.containsKey("3"));
        Assertions.assertTrue(map.containsValue("4"));

        Assertions.assertTrue(map.containsKey("4"));
        Assertions.assertTrue(map.containsValue("3"));
    }
}
