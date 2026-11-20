package com.freelancecommandcenter.platform.adapter.out.freelancer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.freelancecommandcenter.platform.application.port.out.ConnectionRejectedException;
import com.freelancecommandcenter.platform.application.port.out.PlatformUnavailableException;
import com.freelancecommandcenter.platform.domain.ProposalSnapshot;
import com.freelancecommandcenter.platform.domain.ProposalStatus;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

class FreelancerProposalStatusTrackerTest {

    private static final String SITE = "https://www.freelancer.com";
    private static final Instant SINCE = Instant.ofEpochSecond(1795400000);

    private final RestClient.Builder builder = RestClient.builder().baseUrl(SITE);
    private final MockRestServiceServer server =
            MockRestServiceServer.bindTo(builder).build();

    @Test
    void readsTheAccountsBidsWithTheirStatusAndProject() {
        server.expect(requestTo(startsWith(SITE + FreelancerProposalStatusTracker.BIDS_PATH)))
                .andExpect(header(FreelancerConfiguration.TOKEN_HEADER, "user-token"))
                .andExpect(queryParam("bidders%5B%5D", "1001"))
                .andExpect(queryParam("from_time", "1795400000"))
                .andExpect(queryParam("project_details", "true"))
                .andExpect(queryParam("offset", "0"))
                .andRespond(withSuccess(fixture("freelancer/bids.json"), MediaType.APPLICATION_JSON));

        List<ProposalSnapshot> proposals = tracker(100, 3).fetchSubmittedSince("1001", "user-token", SINCE);

        server.verify();
        assertThat(proposals)
                .extracting(ProposalSnapshot::proposalId, ProposalSnapshot::jobTitle, ProposalSnapshot::status)
                .containsExactly(
                        tuple("7001", "Spring Boot REST API", ProposalStatus.PENDING),
                        tuple("7002", "Symfony bug fixes", ProposalStatus.SHORTLISTED),
                        tuple("7003", "Kotlin migration", ProposalStatus.AWARDED),
                        tuple("7004", "Spring Boot REST API", ProposalStatus.REJECTED),
                        tuple("7005", "Symfony bug fixes", ProposalStatus.WITHDRAWN),
                        tuple("7006", "Kotlin migration", ProposalStatus.REVOKED),
                        tuple("7007", "#3999", ProposalStatus.CLOSED));
        assertThat(proposals.getFirst().url()).isEqualTo(URI.create(SITE + "/projects/java/spring-boot-rest-api"));
        assertThat(proposals.get(2).url()).isEqualTo(URI.create(SITE + "/projects/3003"));
        assertThat(proposals.get(2).statusChangedAt()).contains(Instant.ofEpochSecond(1795600000));
        assertThat(proposals.get(1).statusChangedAt()).isEqualTo(Optional.empty());
    }

    @Test
    void readsFurtherPagesUpToTheLimit() {
        server.expect(queryParam("offset", "0"))
                .andRespond(withSuccess(fixture("freelancer/bids.json"), MediaType.APPLICATION_JSON));
        server.expect(queryParam("offset", "8"))
                .andRespond(withSuccess(fixture("freelancer/bids.json"), MediaType.APPLICATION_JSON));

        assertThat(tracker(8, 2).fetchSubmittedSince("1001", "user-token", SINCE))
                .hasSize(14);
        server.verify();
    }

    @Test
    void aRejectedTokenOrAMissingScopeAsksForANewConnection() {
        server.expect(requestTo(startsWith(SITE))).andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> tracker(100, 3).fetchSubmittedSince("1001", "user-token", SINCE))
                .isInstanceOf(ConnectionRejectedException.class);
    }

    @Test
    void otherFailuresMakeThePlatformUnavailable() {
        server.expect(requestTo(startsWith(SITE))).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> tracker(100, 3).fetchSubmittedSince("1001", "user-token", SINCE))
                .isInstanceOf(PlatformUnavailableException.class)
                .isNotInstanceOf(ConnectionRejectedException.class);
    }

    private FreelancerProposalStatusTracker tracker(int pageSize, int maxPages) {
        return new FreelancerProposalStatusTracker(
                builder.build(), URI.create(SITE), JsonMapper.builder().build(), pageSize, maxPages);
    }

    private static String fixture(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
