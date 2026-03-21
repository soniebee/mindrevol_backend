package com.mindrevol.core;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev") // Dòng này "cứu" cả dự án đây
class MindrevolcoreCoreApplicationTests {

	@Test
	void contextLoads() {
	}

}