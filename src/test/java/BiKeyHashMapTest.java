import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import sharedconfig.utils.collections.BiKeyHashMap;
import sharedconfig.utils.tuples.NameVersionTuple;
import sharedconfig.utils.tuples.StrictNameVersionTuple;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class BiKeyHashMapTest {
    @Test
    void test1() {
        val map = new BiKeyHashMap<String, String, Integer>();
        map.put("1", "2", 1);
        assertThrows(IllegalStateException.class, () -> map.put("1", "3", 2));
        map.put("2", "3", 3);

        Assertions.assertEquals(2, map.size());

        Assertions.assertTrue(map.containsKey1("1"));
        Assertions.assertTrue(map.containsKey2("2"));
        Assertions.assertEquals(1, map.getByKey1("1"));
        Assertions.assertEquals(1, map.getByKey2("2"));

        Assertions.assertTrue(map.containsKey1("2"));
        Assertions.assertTrue(map.containsKey2("3"));
        Assertions.assertEquals(3, map.getByKey1("2"));
        Assertions.assertEquals(3, map.getByKey2("3"));
    }

    @Test
    void test2() {
        val map = new BiKeyHashMap<String, NameVersionTuple, Integer>();
        map.put("a", new StrictNameVersionTuple("a", "1.0.0"), 1);
        map.put("b", new StrictNameVersionTuple("b", "1.0.1"), 2);
        map.put("c", new StrictNameVersionTuple("c", "1.0.2"), 3);

        assertThrows(IllegalStateException.class, () -> map.put("a", new StrictNameVersionTuple("b", "1.0.0"), 1));
        assertThrows(IllegalStateException.class, () -> map.put("b", new StrictNameVersionTuple("a", "1.0.0"), 1));

        Assertions.assertEquals(3, map.size());

        Assertions.assertEquals(1, map.getByKey1("a"));
        Assertions.assertEquals(1, map.getByKey2(new NameVersionTuple("a", "1.0.0")));

        Assertions.assertEquals(2, map.getByKey1("b"));
        Assertions.assertEquals(2, map.getByKey2(new NameVersionTuple("b", "1.0.1")));
        Assertions.assertNull(map.getByKey2(new NameVersionTuple("b", "1.0.0")));

        Assertions.assertEquals(3, map.getByKey1("c"));
        Assertions.assertEquals(3, map.getByKey2(new NameVersionTuple("c", "1.0.2")));
    }
}
