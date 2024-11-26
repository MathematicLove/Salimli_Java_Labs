package org.example;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AppTest {
    @Test
    public void testAdd() {
        BestSSet<Integer> set = new BestSSet<>();
        set.add(1531);
        set.add(21111);
        set.add(5013);
        assertEquals(3, set.size());
        assertTrue(set.contains(1531));
        assertTrue(set.contains(21111));
        assertTrue(set.contains(5013));
        assertFalse(set.contains(30));
    }
    @Test
    public void testRemove() {
        BestSSet<Integer> set = new BestSSet<>();
        set.add(1531);
        set.add(21111);
        set.add(5013);
        set.remove(5013);
        assertEquals(2, set.size());
        assertFalse(set.contains(5013));
        assertTrue(set.contains(1531));
        assertTrue(set.contains(21111));
    }
}
