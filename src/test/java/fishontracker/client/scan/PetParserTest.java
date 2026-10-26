package fishontracker.client.scan;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PetParserTest {

    @Test
    void trailIdGetsPetTrailSuffix() {
        assertEquals("storm_trail_pet_trail", PetParser.equippedTrailId("equipped trail: Storm Trail"));
    }

    @Test
    void trailIdDoesNotGetDoubleSuffix() {
        assertEquals("storm_trail_pet_trail", PetParser.equippedTrailId("equipped trail: Storm Trail Pet Trail"));
    }

    @Test
    void defaultOrEmptyTrailsAreIgnored() {
        assertNull(PetParser.equippedTrailId("equipped trail: Default"));
        assertNull(PetParser.equippedTrailId("equipped trail: None"));
        assertNull(PetParser.equippedTrailId("equipped trail: "));
        assertNull(PetParser.equippedTrailId("equipped trail: Empty"));
    }

    @Test
    void onlyTrailLinesProduceAnId() {
        assertNull(PetParser.equippedTrailId("equipped skin: Golden Skin"));
        assertNull(PetParser.equippedTrailId("equipped item: Golden Hat"));
        assertNull(PetParser.equippedTrailId("some unrelated lore line"));
        assertNull(PetParser.equippedTrailId(null));
    }

    @Test
    void trailNamesAreNormalizedIntoIds() {
        assertEquals("auroras_pet_trail", PetParser.equippedTrailId("equipped trail: Aurora's"));
        assertEquals("storm_trail_pet_trail", PetParser.equippedTrailId("  equipped trail:   Storm-Trail!  "));
    }

    @Test
    void standaloneTrailIdsKeepTheFullName() {
        assertEquals("cherry_blossom_pet_trail", PetParser.petTrailId("Cherry Blossom Pet Trail"));
        assertEquals("storm_trail_pet_trail", PetParser.petTrailId("Storm Trail"));
        assertEquals("storm_trail_pet_trail", PetParser.petTrailId("Storm Trail Pet Trail"));
    }

    @Test
    void standaloneSkinIdsDropTheSkinSuffix() {
        assertEquals("leatherback_pet", PetParser.petSkinId("Leatherback Pet Skin"));
        assertEquals("plushified_shark_pet", PetParser.petSkinId("Plushified Shark Pet Skin"));
        assertEquals("royal_capybara_pet", PetParser.petSkinId("Royal Capybara Pet Skin"));
        assertEquals("fancy_pet", PetParser.petSkinId("Fancy Skin"));
        // Equipped skins carry no "skin" word - both paths must produce the same id.
        assertEquals("leatherback_pet", PetParser.petSkinId("Leatherback Pet"));
        assertEquals("leatherback_pet", PetParser.petSkinId("Leatherback"));
    }

    @Test
    void cosmeticNamesIdentifyTheirKind() {
        assertEquals(PetParser.CosmeticKind.TRAIL,
            PetParser.classifyPetCosmetic("cherry blossom pet trail", List.of()));
        assertEquals(PetParser.CosmeticKind.SKIN,
            PetParser.classifyPetCosmetic("leatherback pet skin", List.of()));
    }

    @Test
    void cosmeticLoreIdentifiesTheKindWhenTheNameDoesNot() {
        assertEquals(PetParser.CosmeticKind.TRAIL, PetParser.classifyPetCosmetic(
            "cherry blossom",
            List.of("pet cosmetic item", "pet trails add a particle effect to a pet")));
        assertEquals(PetParser.CosmeticKind.SKIN, PetParser.classifyPetCosmetic(
            "leatherback",
            List.of("pet cosmetic item", "pet skins change the look of a pet")));
    }

    @Test
    void unknownCosmeticsFallBackToSkin() {
        assertEquals(PetParser.CosmeticKind.SKIN,
            PetParser.classifyPetCosmetic("mystery cosmetic", List.of("pet cosmetic item")));
    }
}
