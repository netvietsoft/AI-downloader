package com.nextaitechnology.antidetect

import com.nextaitechnology.antidetect.core.channel.FavoriteChannelRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelDownloaderTest {

    @Test
    fun testNormalizeChannelInput_FacebookPageAndReels() {
        val pageTarget = FavoriteChannelRepository.normalizeChannelInput("https://www.facebook.com/samfamily.6826/")
        assertEquals("facebook", pageTarget.platform)
        assertEquals("https://www.facebook.com/samfamily.6826/reels/", pageTarget.reelsTabUrl)
        assertEquals("samfamily.6826", pageTarget.pageSlug)
        assertFalse(pageTarget.isSingleReel)

        val singleReel = FavoriteChannelRepository.normalizeChannelInput("https://www.facebook.com/reel/123456789012345/")
        assertEquals("facebook", singleReel.platform)
        assertTrue(singleReel.isSingleReel)
        assertEquals("123456789012345", singleReel.singleReelId)
    }

    @Test
    fun testNormalizeChannelInput_InstagramProfileAndReel() {
        val igProfile = FavoriteChannelRepository.normalizeChannelInput("https://www.instagram.com/instagram/")
        assertEquals("instagram", igProfile.platform)
        assertEquals("https://www.instagram.com/instagram/reels/", igProfile.reelsTabUrl)
        assertEquals("ig_instagram", igProfile.pageSlug)
        assertFalse(igProfile.isSingleReel)

        val igSingle = FavoriteChannelRepository.normalizeChannelInput("https://www.instagram.com/reel/C8abcDEF_12/")
        assertEquals("instagram", igSingle.platform)
        assertTrue(igSingle.isSingleReel)
        assertEquals("C8abcDEF_12", igSingle.singleReelId)
    }

    @Test
    fun testNormalizeChannelInput_YouTubeChannelAndShorts() {
        val ytHandle = FavoriteChannelRepository.normalizeChannelInput("@YouTube")
        assertEquals("youtube", ytHandle.platform)
        assertEquals("https://www.youtube.com/@YouTube/shorts", ytHandle.reelsTabUrl)
        assertEquals("yt_YouTube", ytHandle.pageSlug)
        assertFalse(ytHandle.isSingleReel)

        val ytShort = FavoriteChannelRepository.normalizeChannelInput("https://www.youtube.com/shorts/dQw4w9WgXcQ")
        assertEquals("youtube", ytShort.platform)
        assertTrue(ytShort.isSingleReel)
        assertEquals("dQw4w9WgXcQ", ytShort.singleReelId)
    }

    @Test
    fun testExtractCaptionTitleAndHashtags_AndBuildVideoFileName() {
        val meta = FavoriteChannelRepository.extractCaptionTitleAndHashtags(
            rawCaption = "Funny family moment 😂 #samfamily #reels #trending #SamFamily",
            rawTitle = "1.2M views | Funny family moment | Sam Family"
        )
        assertEquals("Funny family moment", meta.title)
        assertEquals(listOf("#samfamily", "#reels", "#trending"), meta.hashtags)

        val fileName = FavoriteChannelRepository.buildVideoFileName(
            reelId = "987654321012345",
            rawCaption = "Funny family moment #samfamily #reels"
        )
        assertEquals("Funny family moment - #samfamily #reels [987654321012345].mp4", fileName)
        assertEquals("987654321012345", FavoriteChannelRepository.extractReelIdFromFileName(fileName))
    }

    @Test
    fun testParseDashManifestXml_SelectsHighest1080pVideoAndAudio() {
        val xml = """
            <MPD>
              <Period>
                <AdaptationSet>
                  <Representation width="720" height="1280" bandwidth="1500000" codecs="avc1.64001f" mimeType="video/mp4">
                    <BaseURL>https://video.fbcdn.net/v/720p.mp4?bytestart=0&amp;byteend=1000</BaseURL>
                  </Representation>
                  <Representation width="1080" height="1920" bandwidth="3800000" codecs="avc1.64002a" mimeType="video/mp4">
                    <BaseURL>https://video.fbcdn.net/v/1080p.mp4?efg=abc&amp;bytestart=0&amp;byteend=5000</BaseURL>
                  </Representation>
                  <Representation bandwidth="128000" codecs="mp4a.40.5" mimeType="audio/mp4">
                    <BaseURL>https://video.fbcdn.net/v/audio.m4a?bytestart=0</BaseURL>
                  </Representation>
                </AdaptationSet>
              </Period>
            </MPD>
        """.trimIndent()

        val parsed = FavoriteChannelRepository.parseDashManifestXml(xml)
        assertEquals("https://video.fbcdn.net/v/1080p.mp4?efg=abc", parsed.bestVideoUrl)
        assertEquals(1080, parsed.bestVideoWidth)
        assertEquals(1920, parsed.bestVideoHeight)
        assertEquals("https://video.fbcdn.net/v/audio.m4a", parsed.bestAudioUrl)
    }

    @Test
    fun testHarvestChannelReelsFromHtml_FacebookInstagramYouTube() {
        val fbHtml = """
            <a href="/reel/111222333444555/">Reel 1</a>
            <script type="application/json">{"video_id":"666777888999000","playable_url":"https:\/\/video.fbcdn.net\/sd.mp4","browser_native_hd_url":"https:\/\/video.fbcdn.net\/hd.mp4"}</script>
        """.trimIndent()
        val fbReels = FavoriteChannelRepository.harvestChannelReelsFromHtml("https://www.facebook.com/samfamily.6826/reels/", fbHtml)
        assertEquals(2, fbReels.size)
        val second = fbReels.first { it.reelId == "666777888999000" }
        assertEquals("https://video.fbcdn.net/hd.mp4", second.hdUrl)

        val ytHtml = """
            <a href="/shorts/abcDEF12345">Short 1</a>
            {"videoId":"xyz98765432"}
        """.trimIndent()
        val ytReels = FavoriteChannelRepository.harvestChannelReelsFromHtml("https://www.youtube.com/@YouTube/shorts", ytHtml)
        assertEquals(2, ytReels.size)

        val igHtml = """
            <a href="/instagram/reel/C8abcDEF_12/">Reel 1</a>
            <a href="/reels/D1xyzUVW_99/?hl=en">Reel 2</a>
            <script type="application/json">{"shortcode":"E5mnoPQR_77","video_url":"https:\/\/scontent.cdninstagram.com\/v\/t50.2886-16\/reel3.mp4"}</script>
            <script type="application/json">{"code":"en_US","node":{"__typename":"XIGPolarisVideoMedia","product_type":"clips","play_count":57081696,"code":"Df7khkFoAZq"}}</script>
        """.trimIndent()
        val igReels = FavoriteChannelRepository.harvestChannelReelsFromHtml("https://www.instagram.com/instagram/reels/", igHtml)
        assertEquals(4, igReels.size)
        assertTrue(igReels.any { it.reelId == "Df7khkFoAZq" })
        assertFalse(igReels.any { it.reelId == "en_US" })

        val longDashXml = "x".repeat(8500)
        val fbLargeRelayHtml = """
            {"video_id":"29380452708263804","dash_manifest":"$longDashXml","dash_manifest_url":"https:\/\/www.facebook.com\/dash_mpd_debug.mpd?v=29380452708263804&dummy=.mpd","browser_native_sd_url":"https:\/\/video.fbcdn.net\/o1\/v\/t2\/f2\/sd_stream.mp4?bytestart=0&byteend=999","browser_native_hd_url":"https:\/\/video.fbcdn.net\/o1\/v\/t2\/f2\/hd_stream.mp4"}
        """.trimIndent()
        val fbLargeReels = FavoriteChannelRepository.harvestChannelReelsFromHtml("https://www.facebook.com/samfamily.6826/reels/", fbLargeRelayHtml)
        assertEquals(1, fbLargeReels.size)
        assertEquals("29380452708263804", fbLargeReels[0].reelId)
        assertEquals("https://video.fbcdn.net/o1/v/t2/f2/hd_stream.mp4", fbLargeReels[0].hdUrl)
        assertEquals("https://video.fbcdn.net/o1/v/t2/f2/sd_stream.mp4", fbLargeReels[0].sdUrl)
    }
}
