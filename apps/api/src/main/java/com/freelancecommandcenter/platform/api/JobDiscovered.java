package com.freelancecommandcenter.platform.api;

/**
 * Published for every listing a platform sync returns, new or changed. The same listing can be published again, so
 * handlers must be idempotent.
 */
public record JobDiscovered(JobListing listing) {}
