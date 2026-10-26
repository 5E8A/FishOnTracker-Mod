package fishontracker.client;

import fishontracker.client.util.TextUtil;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmuletStarTexturesTest {

    @Test
    void amuletNamesAreNormalizedLikeItemIds() {
        // The mapping keys are normalized the same way blacklists are, so apostrophes
        // collapse into a space and lookups stay stable if the server renames them.
        assertEquals("wade s amulet", TextUtil.normalize("Wade's Amulet"));
        assertEquals("bigfoot s necklace", TextUtil.normalize("Bigfoot's Necklace"));

        // toId is the id form - apostrophes are removed there.
        assertEquals("wades_amulet", TextUtil.toId("Wade's Amulet"));
        assertEquals("bigfoots_necklace", TextUtil.toId("Bigfoot's Necklace"));
    }

    @Test
    void builtInMappingCoversEveryShippedTexture() {
        Map<String, String> mapping = AmuletStarTextures.currentMapping();

        assertEquals(11, mapping.size());
        assertEquals("prospecting_icon.png", mapping.get("prospecting amulet"));
        assertEquals("wades_icon.png", mapping.get("wade s amulet"));
        assertEquals("midas_icon.png", mapping.get("midas amulet"));
        assertEquals("onyx_icon.png", mapping.get("onyx amulet"));
        assertEquals("ivory_icon.png", mapping.get("ivory amulet"));
        assertEquals("fabled_icon.png", mapping.get("fabled amulet"));
        assertEquals("bigfoot_necklace_icon.png", mapping.get("bigfoot s necklace"));
        assertEquals("bigfoot_amulet_icon.png", mapping.get("bigfoot s amulet"));
        assertEquals("moon_icon.png", mapping.get("moon amulet"));
        assertEquals("cryptid_icon.png", mapping.get("cryptid amulet"));
        assertEquals("titans_icon.png", mapping.get("titan s graceful necklace"));
    }

    @Test
    void unknownAmuletsHaveNoTextureSoFishOnMCExtrasKeepsItsStar() {
        assertTrue(AmuletStarTextures.currentMapping().keySet().stream()
            .noneMatch(key -> key.contains("unknown")));
    }
}
