package com.smartlab.controller.admin;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartlab.dto.response.admin.AdminDashboardResponse;
import com.smartlab.security.AdminJwtClaims;
import com.smartlab.service.admin.AdminDashboardService;

@RestController
@RequestMapping("/api/admin/dashboard")
@Profile("!nodb")
public class AdminDashboardController {

	private final AdminDashboardService adminDashboardService;

	public AdminDashboardController(AdminDashboardService adminDashboardService) {
		this.adminDashboardService = adminDashboardService;
	}

	@GetMapping
	public AdminDashboardResponse getDashboard(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "10") int recentLimit) {
		AdminDashboardService.DashboardSummary summary = adminDashboardService
				.getDashboardSummary(AdminJwtClaims.labId(jwt), recentLimit);
		return new AdminDashboardResponse(
				summary.users(),
				summary.projects(),
				summary.posts(),
				summary.joinRequests(),
				summary.tasks(),
				summary.recentActivities().stream()
						.map(activity -> new AdminDashboardResponse.RecentActivityResponse(
								activity.id(),
								activity.action(),
								activity.entityType(),
								activity.entityId(),
								activity.actorId(),
								activity.actorName(),
								activity.createdAt()))
						.toList());
	}
}
