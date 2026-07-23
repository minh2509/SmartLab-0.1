package com.smartlab.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.smartlab.dto.response.admin.AdminResearchFieldListResponse;
import com.smartlab.dto.response.admin.AdminResearchFieldResponse;
import com.smartlab.entity.Lab;
import com.smartlab.entity.ResearchField;
import com.smartlab.entity.Role;
import com.smartlab.entity.User;
import com.smartlab.entity.UserRole;
import com.smartlab.enums.CatalogStatus;
import com.smartlab.enums.UserAccountStatus;
import com.smartlab.enums.UserRoleStatus;
import com.smartlab.exception.InvalidAdminServiceInputException;
import com.smartlab.exception.ResourceNotFoundException;
import com.smartlab.mapper.AdminResearchFieldApiMapper;
import com.smartlab.repository.ResearchFieldRepository;
import com.smartlab.repository.RoleRepository;
import com.smartlab.repository.UserRepository;
import com.smartlab.repository.UserRoleRepository;

class AdminResearchFieldServiceTests {

	private final ResearchFieldRepository researchFieldRepository = mock(ResearchFieldRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final RoleRepository roleRepository = mock(RoleRepository.class);
	private final UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);

	private final AdminRolePolicy rolePolicy = new AdminRolePolicy(userRepository, roleRepository, userRoleRepository);
	private final AdminResearchFieldService service = new AdminResearchFieldService(
			researchFieldRepository,
			rolePolicy,
			new AdminResearchFieldApiMapper());

	private final UUID actorUserId = UUID.randomUUID();
	private final Lab lab = new Lab();

	@BeforeEach
	void setUp() {
		lab.setId(UUID.randomUUID());
		User actor = new User();
		actor.setId(actorUserId);
		actor.setLab(lab);
		actor.setAccountStatus(UserAccountStatus.ACTIVE);

		Role adminRole = new Role();
		adminRole.setCode("ADMIN");

		UserRole userRole = new UserRole();
		userRole.setUser(actor);
		userRole.setRole(adminRole);
		userRole.setStatus(UserRoleStatus.ACTIVE);

		when(userRepository.findById(actorUserId)).thenReturn(Optional.of(actor));
		when(userRoleRepository.findByUserAndStatus(actor, UserRoleStatus.ACTIVE)).thenReturn(List.of(userRole));
	}

	@Test
	void getResearchFields_ReturnsList() {
		ResearchField f1 = new ResearchField();
		f1.setId(UUID.randomUUID());
		f1.setCode("AI");
		f1.setName("Artificial Intelligence");

		when(researchFieldRepository.findAll()).thenReturn(List.of(f1));

		AdminResearchFieldListResponse response = service.getResearchFields(actorUserId);
		assertNotNull(response);
		assertEquals(1, response.fields().size());
		assertEquals("AI", response.fields().get(0).code());
	}

	@Test
	void createField_Success() {
		when(researchFieldRepository.findByCode("ROBOTICS")).thenReturn(Optional.empty());
		when(researchFieldRepository.save(any())).thenAnswer(invocation -> {
			ResearchField rf = invocation.getArgument(0);
			rf.setId(UUID.randomUUID());
			return rf;
		});

		AdminResearchFieldResponse response = service.createField(
				new AdminResearchFieldService.CreateResearchFieldCommand(actorUserId, "robotics", "Robotics", "Desc"));

		assertNotNull(response);
		assertEquals("ROBOTICS", response.code());
		assertEquals("Robotics", response.name());
	}

	@Test
	void createField_DuplicateCode_ThrowsException() {
		ResearchField existing = new ResearchField();
		existing.setCode("AI");
		when(researchFieldRepository.findByCode("AI")).thenReturn(Optional.of(existing));

		assertThrows(InvalidAdminServiceInputException.class, () -> service.createField(
				new AdminResearchFieldService.CreateResearchFieldCommand(actorUserId, "AI", "AI Name", null)));
	}

	@Test
	void updateField_Success() {
		UUID fieldId = UUID.randomUUID();
		ResearchField existing = new ResearchField();
		existing.setId(fieldId);
		existing.setCode("AI");
		existing.setName("Old Name");

		when(researchFieldRepository.findById(fieldId)).thenReturn(Optional.of(existing));
		when(researchFieldRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		AdminResearchFieldResponse response = service.updateField(
				new AdminResearchFieldService.UpdateResearchFieldCommand(actorUserId, fieldId, "New Name", "New Desc"));

		assertNotNull(response);
		assertEquals("New Name", response.name());
	}

	@Test
	void updateFieldStatus_Success() {
		UUID fieldId = UUID.randomUUID();
		ResearchField existing = new ResearchField();
		existing.setId(fieldId);
		existing.setStatus(CatalogStatus.ACTIVE);

		when(researchFieldRepository.findById(fieldId)).thenReturn(Optional.of(existing));
		when(researchFieldRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		AdminResearchFieldResponse response = service.updateFieldStatus(
				new AdminResearchFieldService.UpdateResearchFieldStatusCommand(actorUserId, fieldId, CatalogStatus.INACTIVE));

		assertNotNull(response);
		assertEquals(CatalogStatus.INACTIVE, response.status());
	}

	@Test
	void updateField_NotFound_ThrowsException() {
		UUID fieldId = UUID.randomUUID();
		when(researchFieldRepository.findById(fieldId)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> service.updateField(
				new AdminResearchFieldService.UpdateResearchFieldCommand(actorUserId, fieldId, "New Name", null)));
	}
}
