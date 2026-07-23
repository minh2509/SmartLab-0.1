package com.smartlab.service.admin;

import java.util.Locale;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartlab.dto.response.admin.AdminPostPageResponse;
import com.smartlab.enums.PostContentType;
import com.smartlab.enums.PostStatus;
import com.smartlab.enums.PostVisibility;
import com.smartlab.exception.InvalidAdminServiceInputException;
import com.smartlab.mapper.AdminPostApiMapper;
import com.smartlab.repository.PostRepository;

@Service
@Profile("!nodb")
public class AdminPostService {

	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	private final PostRepository postRepository;
	private final AdminRolePolicy rolePolicy;
	private final AdminPostApiMapper mapper;

	public AdminPostService(
			PostRepository postRepository,
			AdminRolePolicy rolePolicy,
			AdminPostApiMapper mapper) {
		this.postRepository = postRepository;
		this.rolePolicy = rolePolicy;
		this.mapper = mapper;
	}

	@Transactional(readOnly = true)
	public AdminPostPageResponse listPosts(ListAdminPostsQuery query) {
		if (query == null) {
			throw new InvalidAdminServiceInputException("List admin posts query must not be null.");
		}
		AdminRolePolicy.ActorContext actor = rolePolicy.requireAdminActor(query.actorUserId());
		int page = normalizedPage(query.page());
		int size = normalizedSize(query.size());

		return mapper.toPageResponse(postRepository.findAdminPosts(
				actor.lab(),
				normalizedKeywordPattern(query.keyword()),
				query.status(),
				query.contentType(),
				query.authorId(),
				query.projectId(),
				query.visibility(),
				PageRequest.of(page, size)));
	}

	private static int normalizedPage(Integer page) {
		if (page == null) {
			return DEFAULT_PAGE;
		}
		if (page < 0) {
			throw new InvalidAdminServiceInputException("Page must be greater than or equal to 0.");
		}
		return page;
	}

	private static int normalizedSize(Integer size) {
		if (size == null) {
			return DEFAULT_SIZE;
		}
		if (size < 1 || size > MAX_SIZE) {
			throw new InvalidAdminServiceInputException("Size must be between 1 and 100.");
		}
		return size;
	}

	private static String normalizedKeywordPattern(String keyword) {
		if (keyword == null) {
			return null;
		}
		String normalizedKeyword = keyword.strip();
		if (normalizedKeyword.isBlank()) {
			return null;
		}
		String escapedKeyword = normalizedKeyword
				.toLowerCase(Locale.ROOT)
				.replace("!", "!!")
				.replace("%", "!%")
				.replace("_", "!_");
		return "%" + escapedKeyword + "%";
	}

	public record ListAdminPostsQuery(
			UUID actorUserId,
			Integer page,
			Integer size,
			String keyword,
			PostStatus status,
			PostContentType contentType,
			UUID authorId,
			UUID projectId,
			PostVisibility visibility) {
	}
}
