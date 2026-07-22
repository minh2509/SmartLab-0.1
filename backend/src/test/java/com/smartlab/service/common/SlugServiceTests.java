package com.smartlab.service.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;

import org.junit.jupiter.api.Test;

class SlugServiceTests {

	private final SlugService slugService = new SlugService();

	@Test
	void generateSlugNormalizesVietnameseText() {
		assertEquals(
				"tri-tue-nhan-tao-robotics-2026",
				slugService.generateSlug("Trí tuệ Nhân tạo & Robotics 2026!"));
	}

	@Test
	void generateSlugConvertsLowercaseAndUppercaseVietnameseD() {
		assertEquals("de-tai-dang-thuc-hien", slugService.generateSlug("Đề tài đang thực hiện"));
	}

	@Test
	void generateSlugLowercasesUppercaseText() {
		assertEquals("smartlab-ai", slugService.generateSlug("SMARTLAB AI"));
	}

	@Test
	void generateSlugTrimsLeadingAndTrailingWhitespace() {
		assertEquals("robotics-lab", slugService.generateSlug("  Robotics Lab  "));
	}

	@Test
	void generateSlugReplacesConsecutiveSpacesWithOneHyphen() {
		assertEquals("robotics-lab-2026", slugService.generateSlug("Robotics    Lab   2026"));
	}

	@Test
	void generateSlugReplacesMultipleSpecialCharactersWithOneHyphen() {
		assertEquals("ai-robotics-vision", slugService.generateSlug("AI &&& Robotics @@ Vision"));
	}

	@Test
	void generateSlugRemovesLeadingAndTrailingSpecialCharacters() {
		assertEquals("smart-lab", slugService.generateSlug("!!!Smart Lab???"));
	}

	@Test
	void generateSlugRejectsNullInput() {
		assertThrows(IllegalArgumentException.class, () -> slugService.generateSlug(null));
	}

	@Test
	void generateSlugRejectsBlankInput() {
		assertThrows(IllegalArgumentException.class, () -> slugService.generateSlug("   "));
	}

	@Test
	void generateSlugRejectsInputThatNormalizesToEmpty() {
		assertThrows(IllegalArgumentException.class, () -> slugService.generateSlug("!!!"));
	}

	@Test
	void generateUniqueSlugReturnsBaseSlugWhenUnused() {
		assertEquals("ai-lab", slugService.generateUniqueSlug("AI Lab", candidate -> false));
	}

	@Test
	void generateUniqueSlugUsesDashTwoForFirstDuplicate() {
		Set<String> usedSlugs = Set.of("ai-lab");

		assertEquals("ai-lab-2", slugService.generateUniqueSlug("AI Lab", usedSlugs::contains));
	}

	@Test
	void generateUniqueSlugUsesNextAvailableNumericSuffix() {
		Set<String> usedSlugs = Set.of("ai-lab", "ai-lab-2", "ai-lab-3");

		assertEquals("ai-lab-4", slugService.generateUniqueSlug("AI Lab", usedSlugs::contains));
	}
}
