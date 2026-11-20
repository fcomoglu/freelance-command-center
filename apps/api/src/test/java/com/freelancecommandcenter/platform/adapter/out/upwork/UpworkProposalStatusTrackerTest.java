package com.freelancecommandcenter.platform.adapter.out.upwork;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.freelancecommandcenter.platform.application.port.out.ConnectionRejectedException;
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

class UpworkProposalStatusTrackerTest {

    private static final String API = "https://api.upwork.com/graphql";
    private static final Instant SINCE = Instant.parse("2026-10-22T00:00:00Z");

    private final RestClient.Builder builder = RestClient.builder().baseUrl(API);
    private final MockRestServiceServer server =
            MockRestServiceServer.bindTo(builder).build();

    @Test
    void readsTheClientsDecisionsOnProposalsSubmittedWithinTheWindow() {
        server.expect(requestTo(API))
                .andExpect(header("Authorization", "Bearer user-token"))
                .andExpect(jsonPath("$.query").value(containsString("status_eq: Declined")))
                .andExpect(jsonPath("$.variables.pagination.first").value(50))
                .andExpect(jsonPath("$.variables.sort.field").value("MODIFIEDDATETIME"))
                .andExpect(jsonPath("$.variables.sort.sortOrder").value("DESC"))
                .andRespond(withSuccess(fixture("upwork/vendor-proposals.json"), MediaType.APPLICATION_JSON));

        List<ProposalSnapshot> proposals = tracker().fetchSubmittedSince("1001", "user-token", SINCE);

        server.verify();
        assertThat(proposals)
                .containsExactly(
                        new ProposalSnapshot(
                                "1690000000000000001",
                                "Spring Boot REST API",
                                URI.create("https://www.upwork.com/nx/proposals/1690000000000000001"),
                                ProposalStatus.AWARDED,
                                Optional.of(Instant.parse("2026-11-20T10:00:00Z"))),
                        new ProposalSnapshot(
                                "1690000000000000002",
                                "#2002",
                                URI.create("https://www.upwork.com/nx/proposals/1690000000000000002"),
                                ProposalStatus.AWARDED,
                                Optional.empty()),
                        new ProposalSnapshot(
                                "1690000000000000004",
                                "Symfony bug fixes",
                                URI.create("https://www.upwork.com/nx/proposals/1690000000000000004"),
                                ProposalStatus.REJECTED,
                                Optional.of(Instant.parse("2026-11-21T08:00:00Z"))));
    }

    @Test
    void mapsUpworksStatusNames() {
        assertThat(UpworkProposalStatusTracker.status("Accepted")).contains(ProposalStatus.PENDING);
        assertThat(UpworkProposalStatusTracker.status("Activated")).contains(ProposalStatus.PENDING);
        assertThat(UpworkProposalStatusTracker.status("Offered")).contains(ProposalStatus.AWARDED);
        assertThat(UpworkProposalStatusTracker.status("Hired")).contains(ProposalStatus.AWARDED);
        assertThat(UpworkProposalStatusTracker.status("Declined")).contains(ProposalStatus.REJECTED);
        assertThat(UpworkProposalStatusTracker.status("Withdrawn")).contains(ProposalStatus.WITHDRAWN);
        assertThat(UpworkProposalStatusTracker.status("Archived")).contains(ProposalStatus.CLOSED);
        assertThat(UpworkProposalStatusTracker.status("Replaced")).isEmpty();
    }

    @Test
    void aRejectedTokenAsksForANewConnection() {
        server.expect(requestTo(API)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> tracker().fetchSubmittedSince("1001", "expired", SINCE))
                .isInstanceOf(ConnectionRejectedException.class);
    }

    private UpworkProposalStatusTracker tracker() {
        return new UpworkProposalStatusTracker(
                new UpworkAccountApi(builder.build(), JsonMapper.builder().build()),
                URI.create("https://www.upwork.com"),
                UpworkConfiguration.query(UpworkConfiguration.VENDOR_PROPOSALS_QUERY),
                50);
    }

    private static String fixture(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
