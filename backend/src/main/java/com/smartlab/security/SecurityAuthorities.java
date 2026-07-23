package com.smartlab.security;

public final class SecurityAuthorities {

	public static final String ROLE_PREFIX = "ROLE_";
	public static final String PERMISSION_PREFIX = "PERMISSION_";

	public static final String ADMIN_ACCESS = "ADMIN_ACCESS";

	private SecurityAuthorities() {
	}

	public static String role(String code) {
		return ROLE_PREFIX + code;
	}

	public static String permission(String code) {
		return PERMISSION_PREFIX + code;
	}
}
