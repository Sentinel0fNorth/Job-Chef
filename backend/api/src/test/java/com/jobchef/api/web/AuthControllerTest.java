package com.jobchef.api.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jobchef.shared.service.DeviceAuthenticationFilter;
import com.jobchef.shared.service.DeviceIdentityService;
import com.jobchef.shared.service.DeviceRequestContext;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthControllerTest {
    private final DeviceIdentityService identity = mock(DeviceIdentityService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(identity)).build();
    }

    @Test
    void revokeReturnsNoContentAndRevokesOnlyTheAuthenticatedDeviceToken() throws Exception {
        UUID deviceId = UUID.randomUUID();
        String tokenHash = "hashed-token";

        mvc.perform(post("/api/v1/auth/revoke")
                        .requestAttr(DeviceAuthenticationFilter.DEVICE_ATTRIBUTE,
                                new DeviceRequestContext(deviceId, tokenHash)))
                .andExpect(status().isNoContent());

        verify(identity).revoke(deviceId, tokenHash);
    }
}
