package de.arbeitszeiterfassung.backend

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootTest
@AutoConfigureMockMvc
@Import(BackendApplicationTests.TestEndpoint::class)
class BackendApplicationTests {
    @RestController
    class TestEndpoint {
        @GetMapping("/api/test")
        fun get() = "OK"
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun contextLoads() {
    }

    @Test
    fun `GET ohne Zugriffstoken liefert 401`() {
        mockMvc.get("/api/test").andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `GET mit authentifizierter Testidentitaet erreicht den Testendpunkt`() {
        mockMvc.get("/api/test") { with(jwt()) }.andExpect {
            status { isOk() }
            content { string("OK") }
        }
    }

    @Test
    fun `Healthcheck ist ohne Zugriffstoken erreichbar`() {
        mockMvc.get("/actuator/health").andExpect {
            status { isOk() }
        }
    }
}
