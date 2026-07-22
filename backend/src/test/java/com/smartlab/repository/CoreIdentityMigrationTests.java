package com.smartlab.repository;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class CoreIdentityMigrationTests {

	@Test
	void initialAdminMigrationSeedsLabUserAndActiveAdminRole() throws IOException {
		String migration = Files.readString(Path.of(
				"src/main/resources/db/migration/V11__seed_initial_admin_account.sql"));

		assertTrue(migration.contains("INSERT INTO labs"));
		assertTrue(migration.contains("'SMARTLAB'"));
		assertTrue(migration.contains("INSERT INTO users"));
		assertTrue(migration.contains("'admin@smart.lab'"));
		assertTrue(migration.contains("$2a$10$"));
		assertTrue(migration.contains("INSERT INTO user_roles"));
		assertTrue(migration.contains("roles.code IN ('SUPER_ADMIN', 'ADMIN')"));
		assertTrue(migration.contains("ON CONFLICT (user_id, role_id) DO UPDATE"));
	}
}
