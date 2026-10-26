package fishontracker.client.scan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PresetPagesTest {

    @Test
    void firstPageCoversPresetsOneToFive() {
        assertEquals("1-5", PresetPages.pageRange(1));
        assertEquals("1-5", PresetPages.pageRange(5));
    }

    @Test
    void secondPageCoversPresetsSixToTen() {
        assertEquals("6-10", PresetPages.pageRange(6));
        assertEquals("6-10", PresetPages.pageRange(7));
        assertEquals("6-10", PresetPages.pageRange(10));
    }

    @Test
    void thirdPageCoversPresetsElevenToFifteen() {
        assertEquals("11-15", PresetPages.pageRange(11));
        assertEquals("11-15", PresetPages.pageRange(12));
    }

    @Test
    void pageGroupIsOneBased() {
        assertEquals(1, PresetPages.pageGroup(1));
        assertEquals(2, PresetPages.pageGroup(6));
        assertEquals(3, PresetPages.pageGroup(11));
    }
}
