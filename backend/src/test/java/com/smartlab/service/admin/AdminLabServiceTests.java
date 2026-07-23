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

import com.smartlab.dto.response.admin.AdminLabResponse;
import com.smartlab.entity.File;
import com.smartlab.entity.Lab;
import com.smartlab.entity.Role;
import com.smartlab.entity.User;
import com.smartlab.entity.UserRole;
import com.smartlab.enums.UserAccountStatus;
import com.smartlab.enums.UserRoleStatus;
import com.smartlab.exception.InvalidAdminServiceInputException;
import com.smartlab.mapper.AdminLabApiMapper;
import com.smartlab.repository.FileRepository;
import com.smartlab.repository.LabRepository;
import com.smartlab.repository.RoleRepository;
import com.smartlab.repository.UserRepository;
import com.smartlab.repository.UserRoleRepository;

class AdminLabServiceTests {

	private final LabRepository labRepository = mock(LabRepository.class);
	private final FileRepository fileRepository = mock(FileRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final RoleRepository roleRepository = mock(RoleRepository.class);
	private final UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);

	private final AdminRolePolicy rolePolicy = new AdminRolePolicy(userRepository, roleRepository, userRoleRepository);
	private final AdminLabService service = new AdminLabService(
			labRepository,
			fileRepository,
			rolePolicy,
			new AdminLabApiMapper());

	private final UUID actorUserId = UUID.randomUUID();
	private final Lab lab = new Lab();

	@BeforeEach
	void setUp() {
		lab.setId(UUID.randomUUID());
		lab.setName("SmartLab");
		lab.setCode("SMARTLAB");

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
	void getLabInfo_Success() {
		AdminLabResponse response = service.getLabInfo(actorUserId);
		assertNotNull(response);
		assertEquals("SmartLab", response.name());
		assertEquals("SMARTLAB", response.code());
	}

	@Test
	void updateLabInfo_Success() {
		when(labRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		AdminLabResponse response = service.updateLabInfo(
				new AdminLabService.UpdateLabInfoCommand(actorUserId, "Updated Lab", "Desc", "Mission", "Vision", "lab@test.com", "http://test.com"));

		assertNotNull(response);
		assertEquals("Updated Lab", response.name());
		assertEquals("lab@test.com", response.contactEmail());
	}

	@Test
	void updateLogo_Success() {
		UUID fileId = UUID.randomUUID();
		File file = new File();
		file.setId(fileId);
		file.setLab(lab);

		when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
		when(labRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		AdminLabResponse response = service.updateLogo(new AdminLabService.UpdateLabImageCommand(actorUserId, fileId));

		assertNotNull(response);
		assertEquals(fileId, response.logoFileId());
	}

	@Test
	void updateCover_FileFromOtherLab_ThrowsException() {
		UUID fileId = UUID.randomUUID();
		Lab otherLab = new Lab();
		otherLab.setId(UUID.randomUUID());

		File file = new File();
		file.setId(fileId);
		file.setLab(otherLab);

		when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));

		assertThrows(InvalidAdminServiceInputException.class,
				() -> service.updateCover(new AdminLabService.UpdateLabImageCommand(actorUserId, fileId)));
	}
}
