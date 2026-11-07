package com.example.starter.todo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 할 일 API 전체 흐름을 실제 스프링 앱과 H2 DB로 확인하는 "통합 테스트"입니다.
 *
 * 단위 테스트(TodoServiceTest 등)와의 차이:
 *   - 가짜(Mock) 없이 Controller → Service → Repository → H2 DB 까지 실제로 연결해서 봅니다.
 *   - 여러 API가 서로 이어졌을 때(생성한 값을 조회·수정·삭제할 수 있는지) 맞는지 확인합니다.
 *
 * 왜 @SpringBootTest 인가요?
 *   - 웹·서비스·DB 를 포함한 앱 전체를 띄웁니다. 일부만 띄우는 @WebMvcTest, @DataJpaTest 로는 이 흐름을 볼 수 없습니다.
 *
 * 왜 @AutoConfigureMockMvc 인가요?
 *   - 앱 전체를 띄우되, 실제 서버 포트를 열지 않고 MockMvc 로 HTTP 요청을 보낼 수 있게 해 줍니다.
 */
// @SpringBootTest: 스프링 부트 앱 전체를 띄워서 테스트합니다. (테스트용 설정 파일이 없으면 application.yml 을 그대로 씁니다)
@SpringBootTest
// @AutoConfigureMockMvc: 실제 서버 대신 가상의 HTTP 요청 도구(MockMvc)를 자동으로 등록합니다.
@AutoConfigureMockMvc
class TodoIntegrationTest {

    // 가상의 HTTP 요청을 보내는 도구입니다. 아래 테스트에서 perform(...)으로 요청을 보냅니다.
    @Autowired
    private MockMvc mockMvc;

    // 테스트 시작 전후 DB 상태를 정리하기 위해 Repository 를 직접 씁니다.
    @Autowired
    private TodoRepository todoRepository;

    /**
     * 각 테스트 전에 할 일 데이터를 모두 지웁니다.
     * 왜 필요한가요? @SpringBootTest 는 @DataJpaTest 와 달리 테스트마다 자동 롤백을 하지 않습니다.
     * 그래서 이전 테스트가 남긴 데이터가 목록 개수 검증을 망치지 않도록 미리 비워 둡니다.
     */
    @BeforeEach
    void clearTodos() {
        todoRepository.deleteAll();
    }

    /**
     * 할 일 생성 → 단건 조회 → 목록 조회 → 수정 → 삭제 → 삭제 후 조회(404) 흐름을 한 번에 확인합니다.
     * 각 단계는 앞 단계의 결과(생성된 id)를 이어서 쓰므로, 순서대로 이어진 하나의 시나리오로 묶었습니다.
     */
    @Test
    void todoCrudFlowWorksEndToEnd() throws Exception {
        // 1. 생성(POST): 201 과 함께 미완료 상태로 저장되어야 합니다.
        String createdJson = mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"우유 사기\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("우유 사기"))
                .andExpect(jsonPath("$.data.completed").value(false))
                .andReturn().getResponse().getContentAsString();

        // DB가 발급한 id 를 응답에서 꺼냅니다. 이후 단계가 이 id 로 같은 할 일을 가리킵니다.
        Long id = ((Number) JsonPath.read(createdJson, "$.data.id")).longValue();
        assertThat(id).isNotNull();

        // 2. 단건 조회(GET /{id}): 방금 만든 할 일이 DB에서 그대로 읽혀야 합니다.
        mockMvc.perform(get("/api/todos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.title").value("우유 사기"))
                .andExpect(jsonPath("$.data.completed").value(false));

        // 3. 전체 목록(GET): 지금은 1개만 있어야 합니다. (setUp 에서 비웠기 때문에 개수를 정확히 확인할 수 있습니다)
        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(id));

        // 4. 수정(PUT): 제목과 완료 여부가 새 값으로 바뀌어 돌아와야 합니다.
        mockMvc.perform(put("/api/todos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"우유 두 개 사기\", \"completed\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("우유 두 개 사기"))
                .andExpect(jsonPath("$.data.completed").value(true));

        // 수정 결과가 실제로 DB에 저장되었는지도 단건 조회로 한 번 더 확인합니다.
        mockMvc.perform(get("/api/todos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("우유 두 개 사기"))
                .andExpect(jsonPath("$.data.completed").value(true));

        // 5. 삭제(DELETE): 200 과 success=true 가 돌아와야 합니다. data 는 비어 있습니다.
        mockMvc.perform(delete("/api/todos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());

        // 6. 삭제 후 조회: 같은 id 로 다시 조회하면 404 와 TODO-001 이 나와야 합니다.
        // 삭제가 실제로 DB에서 지워졌는지 확인하는 마지막 단계입니다.
        mockMvc.perform(get("/api/todos/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TODO-001"));
    }
}
