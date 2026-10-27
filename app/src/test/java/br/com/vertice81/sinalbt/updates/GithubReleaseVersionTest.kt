package br.com.vertice81.sinalbt.updates

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GithubReleaseVersionTest {
    @Test fun betaTagsAreRecognized() {
        assertEquals(0, GithubUpdateManager.compareVersions("1.5.10-beta", "1.5.10"))
        assertEquals(1, GithubUpdateManager.compareVersions("v1.5.10-beta", "1.5.9"))
        assertEquals(-1, GithubUpdateManager.compareVersions("1.5.8-beta", "1.5.9"))
    }

    @Test fun newVersionOrderingUsesNumericPatch() {
        assertEquals(1, GithubUpdateManager.compareVersions("1.5.10", "1.5.9"))
        assertEquals(-1, GithubUpdateManager.compareVersions("1.5.9", "1.5.10"))
        assertEquals(0, GithubUpdateManager.compareVersions("1.5.10", "1.5.10"))
    }

    @Test fun rejectUnexpectedReleaseTags() {
        assertNull(GithubUpdateManager.compareVersions("beta-latest", "1.5.9"))
        assertNull(GithubUpdateManager.compareVersions("1.5.10-unknown", "1.5.9"))
    }
}
