package com.smartlab.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.smartlab.enums.UserAccountStatus;

public final class SmartLabUserPrincipal implements UserDetails {

	private final UUID userId;
	private final UUID labId;
	private final String email;
	private final String fullName;
	private final String passwordHash;
	private final UserAccountStatus accountStatus;
	private final Set<GrantedAuthority> authorities;

	public SmartLabUserPrincipal(
			UUID userId,
			UUID labId,
			String email,
			String fullName,
			String passwordHash,
			UserAccountStatus accountStatus,
			Collection<? extends GrantedAuthority> authorities) {
		this.userId = Objects.requireNonNull(userId, "userId must not be null");
		this.labId = Objects.requireNonNull(labId, "labId must not be null");
		this.email = Objects.requireNonNull(email, "email must not be null");
		this.fullName = fullName;
		this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
		this.accountStatus = Objects.requireNonNull(accountStatus, "accountStatus must not be null");
		this.authorities = Set.copyOf(new LinkedHashSet<>(authorities));
	}

	public UUID userId() {
		return userId;
	}

	public UUID labId() {
		return labId;
	}

	public String email() {
		return email;
	}

	public String fullName() {
		return fullName;
	}

	public UserAccountStatus accountStatus() {
		return accountStatus;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isAccountNonExpired() {
		return accountStatus != UserAccountStatus.DELETED;
	}

	@Override
	public boolean isAccountNonLocked() {
		return accountStatus != UserAccountStatus.LOCKED;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return accountStatus != UserAccountStatus.DELETED;
	}

	@Override
	public boolean isEnabled() {
		return accountStatus == UserAccountStatus.ACTIVE;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof SmartLabUserPrincipal principal)) {
			return false;
		}
		return userId.equals(principal.userId);
	}

	@Override
	public int hashCode() {
		return userId.hashCode();
	}

	@Override
	public String toString() {
		return "SmartLabUserPrincipal[userId=%s, labId=%s, email=%s, accountStatus=%s, authorities=%s]"
				.formatted(userId, labId, email, accountStatus, authorities);
	}
}
