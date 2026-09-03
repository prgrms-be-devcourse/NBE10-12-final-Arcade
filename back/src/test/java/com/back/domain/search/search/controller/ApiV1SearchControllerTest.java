package com.back.domain.search.search.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiV1SearchControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void missingQReturns400_1() throws Exception {
        mvc.perform(get("/api/v1/parties/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    void blankQReturns400_1() throws Exception {
        mvc.perform(get("/api/v1/parties/search").param("q", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    void tooShortQReturns400_4() throws Exception {
        mvc.perform(get("/api/v1/parties/search").param("q", "!!!"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-4"));
    }

    @Test
    void tooLongQReturns400_1() throws Exception {
        mvc.perform(get("/api/v1/parties/search").param("q", "가".repeat(26)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }
}
