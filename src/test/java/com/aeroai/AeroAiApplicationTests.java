package com.aeroai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import com.aeroai.model.RegisteredUser;
import com.aeroai.repository.RegisteredUserRepository;
import com.aeroai.repository.ExperimentRunRepository;
import com.aeroai.repository.RespiratoryReadingRepository;
import com.aeroai.repository.SymptomAssessmentRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:aeroai_tests")
@AutoConfigureMockMvc
class AeroAiApplicationTests {
	@Autowired MockMvc mockMvc;
	@Autowired RegisteredUserRepository users;
	@Autowired ExperimentRunRepository experimentRuns;
	@Autowired RespiratoryReadingRepository respiratoryReadings;
	@Autowired SymptomAssessmentRepository assessments;

	@Test
	void contextLoads() { }

	@Test
	void loginPageRenders() throws Exception {
		mockMvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Welcome Back")));
	}

	@Test
	void dashboardRequiresAnAuthenticatedSession() throws Exception {
		mockMvc.perform(get("/dashboard"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));
	}

	@Test
	void allLabPagesRenderForAnAuthenticatedSession() throws Exception {
		for (String module : new String[]{"anatomy", "gas-exchange", "circulation", "analytics", "device", "integration", "experiments"}) {
			mockMvc.perform(get("/lab/" + module).sessionAttr("aeroUser", 1L))
					.andExpect(status().isOk())
					.andExpect(content().string(org.hamcrest.Matchers.containsString("Interactive learning lab")))
					.andExpect(content().string(org.hamcrest.Matchers.containsString("is-active")))
					.andExpect(content().string(org.hamcrest.Matchers.containsString("Digital Twin &amp; Experiment Lab")));
		}
		mockMvc.perform(get("/dashboard").sessionAttr("aeroUser", 1L).sessionAttr("aeroName", "Aero"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Welcome back, Aero")));
	}

	@Test
	void registeredCredentialsCreateASession() throws Exception {
		users.save(new RegisteredUser("Aero", "Learner", "aero-test@example.org",
				new BCryptPasswordEncoder().encode("safe-demo-password"), "Student", null));
		var result = mockMvc.perform(post("/login").param("email", "aero-test@example.org")
				.param("password", "safe-demo-password"))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/dashboard"))
				.andReturn();
		mockMvc.perform(get("/dashboard").session((org.springframework.mock.web.MockHttpSession) result.getRequest().getSession(false)))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Welcome back, Aero")));
	}

	@Test
	void educationalToolsReturnExplainableResultsAndRejectInvalidInputs() throws Exception {
		var session = new org.springframework.mock.web.MockHttpSession();
		session.setAttribute("aeroUser", 81L);
		mockMvc.perform(post("/api/reliability/check").session(session)
				.contentType("application/json").content("{\"issues\":[\"MOTION\",\"COLD_HAND\"]}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.recheckSuggested").value(true))
				.andExpect(jsonPath("$.scenarios[0].possibleEffect").isNotEmpty())
				.andExpect(jsonPath("$.limitation").value(org.hamcrest.Matchers.containsString("no sensor signal")));
		mockMvc.perform(post("/api/reliability/check").session(session)
				.contentType("application/json").content("{\"issues\":[\"FAKE_SIGNAL\"]}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/reliability/check").session(session)
				.contentType("application/json").content("{\"issues\":[\"SMOKING_CO\"]}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.recheckSuggested").value(false))
				.andExpect(jsonPath("$.summary").value(org.hamcrest.Matchers.containsString("cannot rule out carbon monoxide")));
		mockMvc.perform(post("/api/air-quality/advice").session(session)
				.contentType("application/json").content("{\"aqi\":137,\"scale\":\"US_EPA\"}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.category").value("Unhealthy for Sensitive Groups"))
				.andExpect(jsonPath("$.sourceMode").value("MANUAL_INPUT"))
				.andExpect(jsonPath("$.sourceNote").value(org.hamcrest.Matchers.containsString("did not fetch")));
		mockMvc.perform(post("/api/air-quality/advice").session(session)
				.contentType("application/json").content("{\"aqi\":137,\"scale\":\"INDIA_CPCB\"}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.category").value("Moderate"));
		mockMvc.perform(post("/api/air-quality/advice").session(session)
				.contentType("application/json").content("{\"aqi\":501}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/alerts/evaluate").session(session)
				.contentType("application/json").content("{\"spo2\":91,\"breathingRate\":24,\"pulse\":130,\"spo2Below\":93,\"rateAbove\":20,\"pulseBelow\":55,\"pulseAbove\":125}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.matchedRules").value(3))
				.andExpect(jsonPath("$.rules[0].calculation").value("91 < 93 = true"))
				.andExpect(jsonPath("$.rules[0].reason").isNotEmpty());
		mockMvc.perform(post("/api/alerts/evaluate").session(session)
				.contentType("application/json").content("{\"spo2\":98,\"breathingRate\":16,\"pulse\":80,\"pulseBelow\":120,\"pulseAbove\":100}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void savedReadingsAreScopedAndComparedByTheBackend() throws Exception {
		var session = new org.springframework.mock.web.MockHttpSession();
		session.setAttribute("aeroUser", 82L);
		var first = mockMvc.perform(post("/api/readings").session(session).contentType("application/json")
				.content("{\"spo2\":98,\"breathingRate\":16,\"pulse\":72,\"dataOrigin\":\"USER_MEASURED\"}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.dataOrigin").value("USER_MEASURED")).andReturn();
		var second = mockMvc.perform(post("/api/readings").session(session).contentType("application/json")
				.content("{\"spo2\":94,\"breathingRate\":20,\"pulse\":80}"))
				.andExpect(status().isOk()).andReturn();
		long firstId = Long.parseLong(java.util.regex.Pattern.compile("\\\"id\\\":(\\d+)")
				.matcher(first.getResponse().getContentAsString()).results().findFirst().orElseThrow().group(1));
		long secondId = Long.parseLong(java.util.regex.Pattern.compile("\\\"id\\\":(\\d+)")
				.matcher(second.getResponse().getContentAsString()).results().findFirst().orElseThrow().group(1));
		var otherUser = new org.springframework.mock.web.MockHttpSession();
		otherUser.setAttribute("aeroUser", 820L);
		var attemptedUpdate = mockMvc.perform(post("/api/readings").session(otherUser).contentType("application/json")
				.content("{\"id\":" + firstId + ",\"spo2\":50,\"breathingRate\":80,\"pulse\":250,\"ownerId\":820}"))
				.andExpect(status().isOk()).andReturn();
		long insertedId = Long.parseLong(java.util.regex.Pattern.compile("\\\"id\\\":(\\d+)")
				.matcher(attemptedUpdate.getResponse().getContentAsString()).results().findFirst().orElseThrow().group(1));
		org.junit.jupiter.api.Assertions.assertNotEquals(firstId, insertedId);
		var original = respiratoryReadings.findByIdAndOwnerId(firstId, 82L).orElseThrow();
		org.junit.jupiter.api.Assertions.assertEquals(98, original.getSpo2());
		org.junit.jupiter.api.Assertions.assertEquals(16, original.getBreathingRate());
		mockMvc.perform(post("/api/readings/compare").session(session).contentType("application/json")
				.content("{\"firstId\":" + firstId + ",\"secondId\":" + secondId + "}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.changes.spo2PercentagePoints").value(-4))
				.andExpect(jsonPath("$.changes.breathingRatePerMinute").value(4))
				.andExpect(jsonPath("$.explanation").value(org.hamcrest.Matchers.containsString("sample B minus sample A")));
		mockMvc.perform(get("/api/readings").sessionAttr("aeroUser", 82L))
				.andExpect(status().isOk()).andExpect(jsonPath("$[?(@.ownerId)]").doesNotExist());
		mockMvc.perform(get("/api/readings").sessionAttr("aeroUser", 820L))
				.andExpect(status().isOk()).andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
				.andExpect(jsonPath("$[0].spo2").value(50));
		mockMvc.perform(post("/api/readings").session(session).contentType("application/json")
				.content("{\"spo2\":101,\"breathingRate\":16,\"pulse\":72}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/readings").session(session).contentType("application/json")
				.content("{\"spo2\":98,\"breathingRate\":16,\"pulse\":72,\"dataOrigin\":\"SENSOR_CONNECTED\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void whatIfComparisonCalculatesAndPersistsBothScenarios() throws Exception {
		var session = new org.springframework.mock.web.MockHttpSession();
		session.setAttribute("aeroUser", 83L);
		mockMvc.perform(post("/api/experiments/compare").session(session).contentType("application/json")
				.content("{\"scenarioA\":{\"name\":\"Baseline A\",\"respiratoryRate\":16,\"tidalVolumeMl\":500,\"compliance\":60},\"scenarioB\":{\"name\":\"Test B\",\"respiratoryRate\":20,\"tidalVolumeMl\":400,\"compliance\":70}}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.scenarioA.simulatedVentilation").value(8.0))
				.andExpect(jsonPath("$.scenarioB.simulatedVentilation").value(8.0))
				.andExpect(jsonPath("$.differenceLPerMin").value(0.0))
				.andExpect(jsonPath("$.assumptions").isArray());
		mockMvc.perform(get("/api/experiments").sessionAttr("aeroUser", 83L))
				.andExpect(status().isOk()).andExpect(jsonPath("$[?(@.name == 'Baseline A')]").isNotEmpty())
				.andExpect(jsonPath("$[?(@.name == 'Test B')]").isNotEmpty())
				.andExpect(jsonPath("$[0].ownerId").doesNotExist());
		var savedA = experimentRuns.findByOwnerIdOrderByCreatedAtDesc(83L).stream()
				.filter(run -> run.getName().equals("Baseline A")).findFirst().orElseThrow();
		var savedB = experimentRuns.findByOwnerIdOrderByCreatedAtDesc(83L).stream()
				.filter(run -> run.getName().equals("Test B")).findFirst().orElseThrow();
		mockMvc.perform(get("/api/experiments/report").sessionAttr("aeroUser", 83L)
				.param("firstId", savedA.getId().toString()).param("secondId", savedB.getId().toString()))
				.andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
					.string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Baseline A")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Difference B-A")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("0.0")));
		mockMvc.perform(get("/api/experiments/report").sessionAttr("aeroUser", 830L)
				.param("firstId", savedA.getId().toString()).param("secondId", savedB.getId().toString()))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/experiments/compare").session(session).contentType("application/json")
				.content("{\"scenarioA\":{\"name\":\"bad\",\"respiratoryRate\":2,\"tidalVolumeMl\":500,\"compliance\":60},\"scenarioB\":{\"name\":\"valid\",\"respiratoryRate\":20,\"tidalVolumeMl\":400,\"compliance\":70}}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void emergencyGuidanceIsPublicAndAssessmentsAreRuleBasedAndOwnerScoped() throws Exception {
		mockMvc.perform(get("/emergency"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("112")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Do not wait")));
		var session = new org.springframework.mock.web.MockHttpSession();
		session.setAttribute("aeroUser", 91L);
		mockMvc.perform(post("/api/assessment").session(session).contentType("application/json")
				.content("{\"symptoms\":[\"blue_grey\"]}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.urgency").value("URGENT"))
				.andExpect(jsonPath("$.guidance").value(org.hamcrest.Matchers.containsString("112")));
		mockMvc.perform(post("/api/assessment").session(session).contentType("application/json")
				.content("{\"symptoms\":[\"cough\"]}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.urgency").value("NON_EMERGENCY"))
				.andExpect(jsonPath("$.guidance").value(org.hamcrest.Matchers.containsString("cannot rule out")));
		mockMvc.perform(post("/api/assessment").session(session).contentType("application/json")
				.content("{\"symptoms\":[\"not-supported\"]}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/assessment/history").sessionAttr("aeroUser", 91L))
				.andExpect(status().isOk()).andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
				.andExpect(jsonPath("$[0].ownerId").doesNotExist());
		mockMvc.perform(get("/api/assessment/history").sessionAttr("aeroUser", 910L))
				.andExpect(status().isOk()).andExpect(content().string("[]"));
		mockMvc.perform(post("/api/assessment").session(session).contentType("application/json")
				.content("{\"symptoms\":[\"breathlessness\"],\"severity\":\"moderate\",\"onset\":\"unknown\",\"duration\":\"days\",\"progression\":\"stable\"}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.urgency").value("PROMPT_REVIEW"));
		mockMvc.perform(post("/api/assessment").session(session).contentType("application/json")
				.content("{\"symptoms\":[\"cough\"],\"severity\":\"severe\"}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.urgency").value("URGENT"))
				.andExpect(jsonPath("$.guidance").value(org.hamcrest.Matchers.containsString("Do not wait")));
		mockMvc.perform(post("/api/assistant").session(session).contentType("application/json")
				.content("{\"question\":\"What does SpO2 mean?\"}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.label").value("Rule-based assistant"));
		org.junit.jupiter.api.Assertions.assertEquals(4, assessments.findByOwnerIdOrderByCreatedAtDesc(91L).size());
	}

	@Test
	void allSupportPagesRenderForAnExistingAccount() throws Exception {
		var user=users.save(new RegisteredUser("Support", "Tester", "support-test@example.org", "unused-hash", "Student", null));
		for(String page:new String[]{"profile","assistant","assessment","medicines","air-quality","notifications","knowledge"}) {
			var result=mockMvc.perform(get("/support/"+page).sessionAttr("aeroUser",user.getId()))
					.andExpect(status().isOk()).andReturn();
			String expected=page.equals("knowledge")?"Respiratory Knowledge Centre":"Emergency warning signs";
			org.junit.jupiter.api.Assertions.assertTrue(result.getResponse().getContentAsString().contains(expected),"Expected page content for "+page);
		}
		mockMvc.perform(post("/support/profile").sessionAttr("aeroUser",user.getId())
				.param("firstName","Updated").param("lastName","Tester").param("fieldOfInterest","Respiratory Physiology"))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/support/profile"));
		var saved=users.findById(user.getId()).orElseThrow();
		org.junit.jupiter.api.Assertions.assertEquals("Updated",saved.getFirstName());
		org.junit.jupiter.api.Assertions.assertEquals("support-test@example.org",saved.getEmail());
	}
}
