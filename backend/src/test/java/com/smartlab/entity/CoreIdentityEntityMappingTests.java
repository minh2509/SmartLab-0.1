package com.smartlab.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.smartlab.enums.FileScanStatus;
import com.smartlab.enums.FileVisibility;
import com.smartlab.enums.LabStatus;
import com.smartlab.enums.UserAccountStatus;
import com.smartlab.enums.UserRoleStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

class CoreIdentityEntityMappingTests {

	@Test
	void labMapsCoreColumnsAndFileRelationships() throws NoSuchFieldException {
		assertEntityTable(Lab.class, "labs");
		assertId(Lab.class);
		assertColumn(Lab.class, "name", "name", false, 255);
		assertColumn(Lab.class, "code", "code", false, 100);
		assertColumn(Lab.class, "description", "description", true, 0);
		assertColumn(Lab.class, "mission", "mission", true, 0);
		assertColumn(Lab.class, "vision", "vision", true, 0);
		assertColumn(Lab.class, "contactEmail", "contact_email", true, 255);
		assertColumn(Lab.class, "websiteUrl", "website_url", true, 255);
		assertColumn(Lab.class, "createdAt", "created_at", false, 0);
		assertColumn(Lab.class, "updatedAt", "updated_at", false, 0);
		assertEnumField(Lab.class, "status", LabStatus.class);
		assertManyToOne(Lab.class, "logoFile", "logo_file_id", true);
		assertManyToOne(Lab.class, "coverFile", "cover_file_id", true);
	}

	@Test
	void userMapsCoreColumnsAndRelationships() throws NoSuchFieldException {
		assertEntityTable(User.class, "users");
		assertId(User.class);
		assertColumn(User.class, "username", "username", false, 100);
		assertColumn(User.class, "email", "email", false, 255);
		assertColumn(User.class, "passwordHash", "password_hash", false, 255);
		assertColumn(User.class, "fullName", "full_name", false, 255);
		assertColumn(User.class, "lastLoginAt", "last_login_at", true, 0);
		assertColumn(User.class, "createdAt", "created_at", false, 0);
		assertColumn(User.class, "updatedAt", "updated_at", false, 0);
		assertColumn(User.class, "deletedAt", "deleted_at", true, 0);
		assertEnumField(User.class, "accountStatus", UserAccountStatus.class);
		assertManyToOne(User.class, "lab", "lab_id", false);
		assertManyToOne(User.class, "deletedBy", "deleted_by", true);
		assertManyToOne(User.class, "avatarFile", "avatar_file_id", true);
	}

	@Test
	void fileMapsCoreColumnsAndRelationships() throws NoSuchFieldException {
		assertEntityTable(File.class, "files");
		assertId(File.class);
		assertColumn(File.class, "originalName", "original_name", false, 255);
		assertColumn(File.class, "storedName", "stored_name", false, 255);
		assertColumn(File.class, "storagePath", "storage_path", false, 0);
		assertColumn(File.class, "mimeType", "mime_type", true, 150);
		assertColumn(File.class, "fileSize", "file_size", false, 0);
		assertColumn(File.class, "fileExtension", "file_extension", true, 50);
		assertColumn(File.class, "createdAt", "created_at", false, 0);
		assertColumn(File.class, "deletedAt", "deleted_at", true, 0);
		assertEnumField(File.class, "visibility", FileVisibility.class);
		assertEnumField(File.class, "scanStatus", FileScanStatus.class);
		assertManyToOne(File.class, "lab", "lab_id", false);
		assertManyToOne(File.class, "uploadedBy", "uploaded_by", true);
	}

	@Test
	void roleAndPermissionMapCatalogColumns() throws NoSuchFieldException {
		assertEntityTable(Role.class, "roles");
		assertId(Role.class);
		assertColumn(Role.class, "code", "code", false, 50);
		assertColumn(Role.class, "name", "name", false, 100);
		assertColumn(Role.class, "description", "description", true, 0);
		assertColumn(Role.class, "createdAt", "created_at", false, 0);

		assertEntityTable(Permission.class, "permissions");
		assertId(Permission.class);
		assertColumn(Permission.class, "code", "code", false, 100);
		assertColumn(Permission.class, "name", "name", false, 150);
		assertColumn(Permission.class, "module", "module", false, 100);
		assertColumn(Permission.class, "description", "description", true, 0);
		assertColumn(Permission.class, "createdAt", "created_at", false, 0);
	}

	@Test
	void joinEntitiesMapRelationshipsAndStatuses() throws NoSuchFieldException {
		assertEntityTable(UserRole.class, "user_roles");
		assertId(UserRole.class);
		assertColumn(UserRole.class, "assignedAt", "assigned_at", false, 0);
		assertEnumField(UserRole.class, "status", UserRoleStatus.class);
		assertManyToOne(UserRole.class, "user", "user_id", false);
		assertManyToOne(UserRole.class, "role", "role_id", false);
		assertManyToOne(UserRole.class, "assignedBy", "assigned_by", true);

		assertEntityTable(RolePermission.class, "role_permissions");
		assertId(RolePermission.class);
		assertManyToOne(RolePermission.class, "role", "role_id", false);
		assertManyToOne(RolePermission.class, "permission", "permission_id", false);
	}

	@Test
	void equalityIsSafeForTransientEntities() {
		Lab transientLab = new Lab();
		User transientUser = new User();
		assertFalse(transientLab.equals(new Lab()));
		assertFalse(transientUser.equals(new User()));
		assertEquals(transientLab, transientLab);
		assertEquals(transientUser, transientUser);
		assertEquals(Lab.class.hashCode(), transientLab.hashCode());
		assertEquals(User.class.hashCode(), transientUser.hashCode());
	}

	@Test
	void equalityUsesGetterBasedUuidIdentifiers() {
		UUID labId = UUID.randomUUID();
		Lab firstLab = new Lab();
		firstLab.setId(labId);
		Lab secondLab = new Lab();
		secondLab.setId(labId);
		UUID userId = UUID.randomUUID();
		User firstUser = new User();
		firstUser.setId(userId);
		User secondUser = new User();
		secondUser.setId(userId);

		assertEquals(firstLab, secondLab);
		assertEquals(firstLab.hashCode(), secondLab.hashCode());
		assertEquals(firstUser, secondUser);
		assertEquals(firstUser.hashCode(), secondUser.hashCode());
	}

	@Test
	void enumValuesAndJavaDefaultsMatchDatabaseChecks() {
		assertEnumNames(LabStatus.class, "ACTIVE", "INACTIVE");
		assertEnumNames(UserAccountStatus.class, "ACTIVE", "LOCKED", "PENDING", "DELETED");
		assertEnumNames(UserRoleStatus.class, "ACTIVE", "INACTIVE");
		assertEnumNames(FileVisibility.class, "PUBLIC", "LAB_INTERNAL", "PROJECT_INTERNAL", "PRIVATE");
		assertEnumNames(FileScanStatus.class, "PENDING", "SAFE", "BLOCKED");

		assertEquals(LabStatus.ACTIVE, new Lab().getStatus());
		assertEquals(UserAccountStatus.ACTIVE, new User().getAccountStatus());
		assertEquals(UserRoleStatus.ACTIVE, new UserRole().getStatus());
		assertEquals(FileVisibility.PRIVATE, new File().getVisibility());
		assertEquals(FileScanStatus.PENDING, new File().getScanStatus());
		assertEquals(0L, new File().getFileSize());
	}

	private static void assertEntityTable(Class<?> entityType, String tableName) {
		assertNotNull(entityType.getAnnotation(Entity.class));
		assertEquals(tableName, entityType.getAnnotation(Table.class).name());
	}

	private static void assertId(Class<?> entityType) throws NoSuchFieldException {
		assertNotNull(entityType.getDeclaredField("id").getAnnotation(Id.class));
		assertEquals(UUID.class, entityType.getDeclaredField("id").getType());
	}

	private static void assertColumn(
			Class<?> entityType,
			String fieldName,
			String columnName,
			boolean nullable,
			int length) throws NoSuchFieldException {
		Column column = entityType.getDeclaredField(fieldName).getAnnotation(Column.class);
		assertNotNull(column);
		assertEquals(columnName, column.name());
		assertEquals(nullable, column.nullable());
		if (length > 0) {
			assertEquals(length, column.length());
		}
	}

	private static void assertEnumField(
			Class<?> entityType,
			String fieldName,
			Class<?> enumType) throws NoSuchFieldException {
		Field field = entityType.getDeclaredField(fieldName);
		assertEquals(enumType, field.getType());
		Enumerated enumerated = field.getAnnotation(Enumerated.class);
		assertNotNull(enumerated);
		assertEquals(EnumType.STRING, enumerated.value());
	}

	private static void assertManyToOne(
			Class<?> entityType,
			String fieldName,
			String columnName,
			boolean nullable) throws NoSuchFieldException {
		Field field = entityType.getDeclaredField(fieldName);
		ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
		assertNotNull(manyToOne);
		assertEquals(FetchType.LAZY, manyToOne.fetch());
		assertEquals(!nullable, !manyToOne.optional());
		JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
		assertNotNull(joinColumn);
		assertEquals(columnName, joinColumn.name());
		assertEquals(nullable, joinColumn.nullable());
	}

	private static <E extends Enum<E>> void assertEnumNames(Class<E> enumType, String... expectedNames) {
		assertEquals(Arrays.asList(expectedNames), Arrays.stream(enumType.getEnumConstants()).map(Enum::name).toList());
	}

}
