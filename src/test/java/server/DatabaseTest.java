package server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseTest {

    private Database database;

    @BeforeEach
    void setUp() {
        database = new Database();
    }

    @Test
    void testSetAndGet() {
        String result = database.set("name", "Sreeja");
        assertEquals("OK", result);

        String value = database.get("name");
        assertEquals("Sreeja", value);
    }

    @Test
    void testGetNonExistingKey() {
        String result = database.get("unknown");
        assertEquals("ERROR", result);
    }

    @Test
    void testDeleteExistingKey() {
        database.set("city", "Hyderabad");
        String result = database.delete("city");

        assertEquals("OK", result);
        assertEquals("ERROR", database.get("city"));
    }

    @Test
    void testDeleteNonExistingKey() {
        String result = database.delete("missing");
        assertEquals("ERROR", result);
    }

    @Test
    void testSize() {
        assertEquals(0, database.size());

        database.set("a", "1");
        database.set("b", "2");

        assertEquals(2, database.size());
    }
}
