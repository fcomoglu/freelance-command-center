package com.freelancecommandcenter.platform.domain;

/** Where one of the user's proposals (a bid on Freelancer.com) stands on its platform. */
public enum ProposalStatus {
    /** Submitted, no decision yet. */
    PENDING,
    SHORTLISTED,
    /** The client chose the proposal; the work itself stays on the platform. */
    AWARDED,
    REJECTED,
    /** An award the client took back. */
    REVOKED,
    /** Taken back by the user. */
    WITHDRAWN,
    /** The job ended without a decision on the proposal, for example because it was cancelled. */
    CLOSED
}
