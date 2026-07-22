package com.smartlab.controller.admin;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartlab.dto.response.admin.AdminRoleCatalogResponse;
import com.smartlab.mapper.AdminUserApiMapper;
import com.smartlab.security.AuthenticatedActorResolver;
import com.smartlab.service.admin.AdminUserRoleService;

@RestController
@RequestMapping("/api/admin/roles")
@Profile("!nodb")
public class AdminRoleController {

	private final AdminUserRoleService adminUserRoleService;
	private final AdminUserApiMapper mapper;
	private final AuthenticatedActorResolver actorResolver;

	public AdminRoleController(
			AdminUserRoleService adminUserRoleService,
			AdminUserApiMapper mapper,
			AuthenticatedActorResolver actorResolver) {
		this.adminUserRoleService = adminUserRoleService;
		this.mapper = mapper;
		this.actorResolver = actorResolver;
	}

	@GetMapping
	public List<AdminRoleCatalogResponse> listRoles() {
		return adminUserRoleService.listRoleCatalog(actorResolver.requireActorUserId())
				.stream()
				.map(mapper::toRoleCatalogResponse)
				.toList();
	}
}
