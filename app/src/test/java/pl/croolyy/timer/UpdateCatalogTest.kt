package pl.croolyy.timer

import org.junit.Assert.*
import org.junit.Test
import pl.croolyy.timer.update.*
import pl.croolyy.timer.network.GitHubHttp
import pl.croolyy.timer.data.sha256File
import java.nio.file.Files

class UpdateCatalogTest {
    private val signer = "a".repeat(64)
    private fun release(code: Long = 3, min: Int = 26) = AppRelease(code, "1.2.0", "v1.2.0", "CrooLyyTimer-1.2.0-debug.apk", "b".repeat(64), 4, min, setOf(signer))
    @Test fun channelsVersionCodesAndAndroidCompatibilityDetermineAvailability() {
        val catalog = UpdateCatalog(stable = release(3), preview = release(5, 35))
        assertEquals(3L, catalog.newest(2, 26, true)!!.versionCode)
        assertEquals(5L, catalog.newest(2, 36, true)!!.versionCode)
        assertEquals(3L, catalog.newest(2, 36, false)!!.versionCode)
        assertNull(catalog.newest(5, 36, true))
        assertNull(UpdateCatalog().newest(2, 36, true))
    }
    @Test fun catalogRejectsRedirectInjectionOversizeAndUnsupportedSchemas() {
        assertThrows(IllegalArgumentException::class.java) { release().copy(apkName = "../../other.apk").validate() }
        assertThrows(IllegalArgumentException::class.java) { release().copy(tag = "v9/../../other").validate() }
        assertThrows(IllegalArgumentException::class.java) { release().copy(size = MAX_APK_BYTES + 1).validate() }
        assertThrows(IllegalArgumentException::class.java) { release().copy(signerSha256 = emptySet()).validate() }
        assertThrows(IllegalArgumentException::class.java) { UpdateCatalog.parse("""{"schemaVersion":9}""") }
        assertFalse(GitHubHttp.allowed("http://github.com/file"))
        assertFalse(GitHubHttp.allowed("https://github.com.attacker.test/file"))
        assertFalse(GitHubHttp.allowed("https://token@github.com/file"))
        assertTrue(GitHubHttp.allowed("https://release-assets.githubusercontent.com/file?signature=public-download"))
    }
    @Test fun verifierChecksActualBytesIdentityVersionAndSigningCertificate() {
        val file = Files.createTempFile("update", ".apk").toFile()
        try {
            file.writeBytes(byteArrayOf(1, 2, 3, 4))
            val candidate = release().copy(sha256 = sha256File(file))
            val old = ApkIdentity("pl.croolyy.timer", 2, 26, setOf(signer))
            val next = old.copy(versionCode = 3)
            fun verify(identity: ApkIdentity = next, entry: AppRelease = candidate) = ArtifactVerifier({ old }, { identity }, 36).verify(file, entry)
            verify()
            assertThrows(IllegalArgumentException::class.java) { verify(next.copy(packageName = "other.app")) }
            assertThrows(IllegalArgumentException::class.java) { verify(next.copy(versionCode = 2)) }
            assertThrows(IllegalArgumentException::class.java) { verify(next.copy(signers = setOf("c".repeat(64)))) }
            assertThrows(IllegalArgumentException::class.java) { verify(next.copy(minSdk = 37)) }
            assertThrows(IllegalArgumentException::class.java) { verify(entry = candidate.copy(signerSha256 = setOf("c".repeat(64)))) }
            file.appendBytes(byteArrayOf(5))
            assertThrows(IllegalArgumentException::class.java) { verify() }
        } finally { file.delete() }
    }
}
