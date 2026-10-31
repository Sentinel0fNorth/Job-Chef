package com.jobchef.shared.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FixtureJobProviderTest {
    private final FixtureJobProvider provider = new FixtureJobProvider();

    @Test
    void fixturesAreClearlyLabeledAndBounded() {
        var jobs = provider.search("in", "engineer", "", "", 2);

        assertEquals(2, jobs.size());
        assertTrue(provider.attribution().contains("Synthetic"));
        assertTrue(jobs.stream().allMatch(job -> job.providerId().startsWith("demo-")));
    }

    @Test
    void fixturesAreNotUsedAsImplicitLiveProviderFallback() {
        var jobs = provider.search("in", "not-a-demo-title", "", "impossible phrase", 20);

        assertTrue(jobs.isEmpty());
    }

    @Test
    void emptyKeywordsDoNotMatchEverySyntheticListing() {
        var jobs = provider.search("in", "role-that-does-not-exist", "", "", 20);

        assertTrue(jobs.isEmpty());
    }
}
