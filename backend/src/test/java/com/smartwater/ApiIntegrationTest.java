package com.smartwater;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    private JsonNode json(String s) throws Exception { return om.readTree(s); }

    @Test
    void endToEnd_usage_to_billing() throws Exception {
        // register admin
        String reg = mvc.perform(post("/api/auth/register-admin").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@t.com\",\"password\":\"Password1\",\"fullName\":\"Admin\",\"apartmentName\":\"Test Apts\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String auth = "Bearer " + json(reg).get("token").asText();
        String code = json(reg).get("user").get("apartmentCode").asText();

        // protected endpoints need a token
        mvc.perform(get("/api/admin/households")).andExpect(status().isUnauthorized());

        // create households (one metered, one not) and reject invalid input
        String h1 = mvc.perform(post("/api/admin/households").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"unitNumber\":\"A-1\",\"areaSqft\":900,\"occupants\":3,\"meterNumber\":\"M1\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long hid = json(h1).get("id").asLong();
        mvc.perform(post("/api/admin/households").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"unitNumber\":\"A-2\",\"areaSqft\":600,\"occupants\":2}")).andExpect(status().isOk());
        mvc.perform(post("/api/admin/households").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"unitNumber\":\"A-3\",\"areaSqft\":-5,\"occupants\":2}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/households").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"unitNumber\":\"A-1\",\"areaSqft\":900,\"occupants\":3}")).andExpect(status().isConflict());

        // resident registration against the apartment code, then role separation
        String rr = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"res@t.com\",\"password\":\"Password1\",\"fullName\":\"Res\",\"apartmentCode\":\"" + code + "\",\"unitNumber\":\"A-1\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String resAuth = "Bearer " + json(rr).get("token").asText();
        mvc.perform(get("/api/admin/households").header("Authorization", resAuth)).andExpect(status().isForbidden());

        // manual usage: ok, then duplicate -> 409
        LocalDate d = LocalDate.now().withDayOfMonth(1);
        String body = "{\"date\":\"" + d + "\",\"litres\":12000}";
        mvc.perform(post("/api/usage").header("Authorization", resAuth).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mvc.perform(post("/api/usage").header("Authorization", resAuth).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());

        // CSV upload: one new row, one duplicate, one unknown unit
        String csv = "unit_number,date,litres\nA-1," + d.plusDays(1) + ",3000\nA-1," + d + ",100\nZ-9," + d + ",50\n";
        // (dates in the future are rejected, so only use today-or-earlier rows when running late in the month)
        var file = new MockMultipartFile("file", "u.csv", "text/csv", csv.getBytes());
        mvc.perform(multipart("/api/admin/usage/upload").file(file).header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.duplicates").value(1))
                .andExpect(jsonPath("$.errors.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        // purchase + cycle + live preview + finalize + PDF
        mvc.perform(post("/api/admin/purchases").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\":\"" + d + "\",\"source\":\"Tanker\",\"volumeKl\":50,\"unitCost\":10}")).andExpect(status().isOk());
        String cyc = mvc.perform(post("/api/admin/cycles").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\":\"Test cycle\",\"startDate\":\"" + d + "\",\"endDate\":\"" + d.plusDays(27) + "\",\"sharedAreaKl\":5}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long cid = json(cyc).get("id").asLong();
        mvc.perform(get("/api/admin/cycles/" + cid + "/invoices").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        String inv = mvc.perform(post("/api/admin/cycles/" + cid + "/finalize").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2)).andReturn().getResponse().getContentAsString();
        mvc.perform(post("/api/admin/cycles/" + cid + "/finalize").header("Authorization", auth)).andExpect(status().isConflict());
        long invId = json(inv).get(0).get("id").asLong();
        byte[] pdf = mvc.perform(get("/api/invoices/" + invId + "/pdf").header("Authorization", auth))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertTrue(new String(pdf, 0, 4).startsWith("%PDF"));

        // resident dashboard works for the linked unit
        mvc.perform(get("/api/dashboard").header("Authorization", resAuth)).andExpect(status().isOk())
                .andExpect(jsonPath("$.unitNumber").value("A-1"));
        assertNotEquals(0L, hid);
    }
}
