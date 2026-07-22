package com.smartlab.exception;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class ApiExceptionHandlerTests {

	private final MockMvc mockMvc = MockMvcBuilders
			.standaloneSetup(new ExceptionProbeController())
			.setControllerAdvice(new ApiExceptionHandler())
			.build();

	@Test
	void invalidPostTransitionUsesStandardBadRequestErrorResponse() throws Exception {
		mockMvc.perform(get("/probe/post-transition"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.timestamp").exists())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").value("Bad Request"))
				.andExpect(jsonPath("$.message").value("Invalid workflow transition."))
				.andExpect(jsonPath("$.path").value("/probe/post-transition"))
				.andExpect(content().string(not(Matchers.containsString("<html"))));
	}

	@RestController
	static class ExceptionProbeController {

		@GetMapping("/probe/post-transition")
		Map<String, String> invalidPostTransition() {
			throw new InvalidPostTransitionException("Invalid workflow transition.");
		}
	}
}
