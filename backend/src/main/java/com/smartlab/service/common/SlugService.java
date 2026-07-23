package com.smartlab.service.common;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

import org.springframework.stereotype.Service;

@Service
public class SlugService {

	public String generateSlug(String input) {
		if (input == null) {
			throw new IllegalArgumentException("Slug source must not be null.");
		}

		String trimmedInput = input.trim();
		if (trimmedInput.isBlank()) {
			throw new IllegalArgumentException("Slug source must not be blank.");
		}

		String withoutVietnameseD = trimmedInput
				.replace('đ', 'd')
				.replace('Đ', 'D');
		String withoutDiacritics = Normalizer
				.normalize(withoutVietnameseD, Normalizer.Form.NFD)
				.replaceAll("\\p{M}+", "");
		String slug = withoutDiacritics
				.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9]+", "-")
				.replaceAll("^-+|-+$", "");

		if (slug.isEmpty()) {
			throw new IllegalArgumentException("Slug source must contain at least one slug character.");
		}

		return slug;
	}

	public String generateUniqueSlug(String input, Predicate<String> slugExists) {
		Objects.requireNonNull(slugExists, "Slug existence predicate must not be null.");

		String baseSlug = generateSlug(input);
		if (!slugExists.test(baseSlug)) {
			return baseSlug;
		}

		for (long suffix = 2; suffix <= Integer.MAX_VALUE; suffix++) {
			String candidate = baseSlug + "-" + suffix;
			if (!slugExists.test(candidate)) {
				return candidate;
			}
		}

		throw new IllegalStateException("Unable to generate a unique slug.");
	}
}
