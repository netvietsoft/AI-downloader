/**
 * Hệ Thống Tải Video Theo Kênh / Page (Facebook Page Reels, Instagram Reels, YouTube Channel Shorts/Videos)
 * & Quản Lý Danh Sách Kênh Yêu Thích Tự Động Quét (Favorite Channels Watchlist & Auto-Scan Scheduler)
 * Tích hợp từ bộ công cụ D:\Decompiler\App\Downloader\download
 *
 * @author NextAI Technology Core Team
 */

const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const https = require('https');
const http = require('http');
const { execFile } = require('child_process');
const { promisify } = require('util');
const express = require('express');

const execFileAsync = promisify(execFile);

const DESKTOP_UA =
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36';

const dataDir = path.join(__dirname, 'data');
const videoLibraryDir = process.env.VIDEO_LIBRARY_DIR
    ? path.resolve(__dirname, process.env.VIDEO_LIBRARY_DIR)
    : path.join(dataDir, 'VideoLibrary');
const reelsJobsFile = path.join(dataDir, 'reels_jobs.json');
const reelsFavoritesFile = path.join(dataDir, 'reels_favorites.json');
const externalYtDlp = 'D:\\Decompiler\\App\\Downloader\\download\\bin\\yt-dlp.exe';
const ytDlpBin = fs.existsSync(externalYtDlp) ? externalYtDlp : 'yt-dlp';

function unescapeFbUrl(raw) {
    if (!raw) return '';
    return String(raw)
        .trim()
        .replace(/\\\//g, '/')
        .replace(/\\u0026/gi, '&')
        .replace(/\\u0025/gi, '%')
        .replace(/\\u003d/gi, '=')
        .replace(/\\u003c/gi, '<')
        .replace(/\\u003e/gi, '>')
        .replace(/\\"/g, '"')
        .replace(/&amp;/g, '&');
}

function stripByteRangeParams(rawUrl) {
    return unescapeFbUrl(rawUrl)
        .replace(/([?&])bytestart=\d+&?/g, '$1')
        .replace(/([?&])byteend=\d+&?/g, '$1')
        .replace(/\?&/g, '?')
        .replace(/[?&]+$/g, '');
}

function sanitizeFolderSlug(raw) {
    const cleaned = String(raw || '')
        .trim()
        .replace(/[<>:"/\\|?*\x00-\x1F]/g, '_')
        .replace(/\s+/g, '_')
        .replace(/^[._]+|[.\s]+$/g, '');
    return cleaned || 'channel_videos';
}

function extractCaptionTitleAndHashtags(rawCaption, rawTitle, rawTags) {
    const cleanStr = (s) =>
        String(s || '')
            .replace(/[\u200B-\u200F\u202A-\u202E\u2060-\u206F\uFEFF]/g, '')
            .trim();

    const cap = cleanStr(rawCaption);
    let tit = cleanStr(rawTitle);

    if (/^Video by /i.test(tit)) {
        tit = '';
    }
    if (tit && /\bviews\b.*\|.*\|/i.test(tit)) {
        const parts = tit.split('|').map((p) => p.trim());
        if (parts.length >= 2) {
            tit = parts[1];
        }
    }

    const combinedSource = [cap, tit].filter(Boolean).join('\n');
    const hashtagRegex = /#[\p{L}\p{N}_]+/gu;
    const seenLower = new Set();
    const hashtags = [];

    const pushTag = (tagRaw) => {
        const cleaned = String(tagRaw || '').trim().replace(/^#+/, '');
        if (!cleaned) return;
        const normalized = cleaned.replace(/[^\p{L}\p{N}_]/gu, '');
        if (!normalized) return;
        const tag = `#${normalized}`;
        const low = tag.toLowerCase();
        if (!seenLower.has(low)) {
            seenLower.add(low);
            hashtags.push(tag);
        }
    };

    const matches = combinedSource.match(hashtagRegex) || [];
    for (const m of matches) pushTag(m);
    if (Array.isArray(rawTags)) {
        for (const t of rawTags) {
            if (typeof t === 'string' && t.trim()) pushTag(t);
        }
    }

    const baseText = cap || tit;
    const bodyWithoutHashtags = baseText
        .replace(hashtagRegex, ' ')
        .replace(/\s+/g, ' ')
        .replace(/^[\s\-–—•|,:;]+|[\s\-–—•|,:;]+$/g, '')
        .trim();

    const title = bodyWithoutHashtags || (hashtags.length > 0 ? hashtags.join(' ') : '');
    const fullCaption =
        baseText || [bodyWithoutHashtags, hashtags.join(' ')].filter(Boolean).join('\n');

    return { title, caption: fullCaption, hashtags };
}

function sanitizeForWindowsFileName(input) {
    return String(input || '')
        .replace(/[\u0000-\u001F\u007F-\u009F\u200B-\u200F\u202A-\u202E\u2060-\u206F\uFEFF]/g, ' ')
        .replace(/[\u{1F000}-\u{1FFFF}\u{2600}-\u{27BF}]/gu, ' ')
        .replace(/[<>:"/\\|?*]/g, ' - ')
        .replace(/\s+/g, ' ')
        .replace(/(?:\s*-\s*)+/g, ' - ')
        .replace(/^[\s.\-_]+|[\s.\-_]+$/g, '')
        .trim();
}

function buildVideoFileName(reelId, rawCaption, rawTitle, rawTags) {
    const safeId = String(reelId || '').replace(/[^A-Za-z0-9_-]/g, '');
    const meta = extractCaptionTitleAndHashtags(rawCaption, rawTitle, rawTags);

    let cleanBody = sanitizeForWindowsFileName(meta.title);
    if (cleanBody.length > 95) {
        cleanBody = cleanBody.slice(0, 95).replace(/\s+\S*$/, '').trim();
    }

    let cleanTags = sanitizeForWindowsFileName(meta.hashtags.slice(0, 8).join(' '));
    if (cleanTags.length > 65) {
        cleanTags = cleanTags.slice(0, 65).replace(/\s+\S*$/, '').trim();
    }

    const parts = [];
    if (cleanBody) parts.push(cleanBody);
    if (cleanTags && cleanTags.toLowerCase() !== cleanBody.toLowerCase()) {
        parts.push(cleanTags);
    }

    if (parts.length === 0) {
        return `${safeId}.mp4`;
    }

    const prefix = parts.join(' - ').replace(/^[\s.\-_]+|[\s.\-_]+$/g, '');
    return `${prefix} [${safeId}].mp4`;
}

function extractReelIdFromFileName(fileName) {
    const base = path.basename(fileName || '');
    if (!base.toLowerCase().endsWith('.mp4')) return '';
    const withoutExt = base.slice(0, -4).trim();
    const bracketMatch = withoutExt.match(/\[([A-Za-z0-9_-]{6,30})\]$/);
    if (bracketMatch && bracketMatch[1]) {
        return bracketMatch[1];
    }
    return withoutExt;
}

function normalizeFacebookInput(rawInput) {
    let trimmed = String(rawInput || '').trim();
    if (!trimmed) {
        throw new Error('URL Page / Kênh không được để trống.');
    }

    if (!/^https?:\/\//i.test(trimmed)) {
        const low = trimmed.toLowerCase();
        if (
            low.includes('facebook.com') ||
            low.includes('fb.watch') ||
            low.includes('fb.com') ||
            low.includes('instagram.com') ||
            low.includes('youtube.com') ||
            low.includes('youtu.be')
        ) {
            trimmed = 'https://' + trimmed.replace(/^\/+/, '');
        } else if (trimmed.startsWith('@')) {
            trimmed = `https://www.youtube.com/${trimmed}`;
        } else {
            trimmed = `https://www.facebook.com/${trimmed.replace(/^\/+/, '')}`;
        }
    }

    let parsed;
    try {
        parsed = new URL(trimmed);
    } catch {
        throw new Error(`URL không hợp lệ: ${rawInput}`);
    }

    const host = parsed.hostname.toLowerCase();

    // 1. YOUTUBE
    if (host.includes('youtube.com') || host.includes('youtu.be')) {
        if (host.includes('youtu.be')) {
            const vid = parsed.pathname.replace(/^\/+/, '').split('/')[0];
            if (!vid) throw new Error('URL YouTube rút gọn không hợp lệ.');
            const watchUrl = `https://www.youtube.com/watch?v=${vid}`;
            return {
                platform: 'youtube',
                inputUrl: rawInput.trim(),
                normalizedPageUrl: watchUrl,
                reelsTabUrl: watchUrl,
                pageSlug: 'yt_single_videos',
                isSingleReel: true,
                singleReelId: vid
            };
        }

        parsed.hostname = 'www.youtube.com';
        const pathname = parsed.pathname.replace(/\/+$/, '');
        const watchVid = parsed.searchParams.get('v');
        const shortsMatch = pathname.match(/^\/shorts\/([A-Za-z0-9_-]{6,20})/i);

        if (shortsMatch || watchVid) {
            const vid = shortsMatch ? shortsMatch[1] : watchVid;
            const singleUrl = shortsMatch
                ? `https://www.youtube.com/shorts/${vid}`
                : `https://www.youtube.com/watch?v=${vid}`;
            return {
                platform: 'youtube',
                inputUrl: rawInput.trim(),
                normalizedPageUrl: singleUrl,
                reelsTabUrl: singleUrl,
                pageSlug: 'yt_single_videos',
                isSingleReel: true,
                singleReelId: vid
            };
        }

        const parts = pathname.split('/').filter(Boolean);
        if (parts.length === 0) {
            throw new Error('Vui lòng nhập URL Kênh YouTube cụ thể (ví dụ: https://www.youtube.com/@YouTube).');
        }

        let channelBase = '';
        let rawSlug = '';
        let subTab = 'shorts';

        if (parts[0].startsWith('@')) {
            channelBase = `https://www.youtube.com/${parts[0]}`;
            rawSlug = parts[0].slice(1);
            if (parts[1] && ['shorts', 'videos', 'streams'].includes(parts[1].toLowerCase())) {
                subTab = parts[1].toLowerCase();
            }
        } else if (['channel', 'c', 'user'].includes(parts[0].toLowerCase()) && parts.length >= 2) {
            channelBase = `https://www.youtube.com/${parts[0]}/${parts[1]}`;
            rawSlug = parts[1];
            if (parts[2] && ['shorts', 'videos', 'streams'].includes(parts[2].toLowerCase())) {
                subTab = parts[2].toLowerCase();
            }
        } else {
            channelBase = `https://www.youtube.com/${parts[0]}`;
            rawSlug = parts[0];
            if (parts[1] && ['shorts', 'videos', 'streams'].includes(parts[1].toLowerCase())) {
                subTab = parts[1].toLowerCase();
            }
        }

        const cleanSlug = sanitizeFolderSlug(`yt_${decodeURIComponent(rawSlug)}`);
        return {
            platform: 'youtube',
            inputUrl: rawInput.trim(),
            normalizedPageUrl: `${channelBase}/`,
            reelsTabUrl: `${channelBase}/${subTab}`,
            pageSlug: cleanSlug,
            isSingleReel: false,
            singleReelId: null
        };
    }

    // 2. INSTAGRAM
    if (host.includes('instagram.com')) {
        parsed.hostname = 'www.instagram.com';
        const pathname = parsed.pathname.replace(/\/+$/, '');
        const singleMatch = pathname.match(/^\/(?:reel|reels|p|tv)\/([A-Za-z0-9_-]{5,25})/i);
        if (singleMatch && singleMatch[1]) {
            const code = singleMatch[1];
            const singleUrl = `https://www.instagram.com/reel/${code}/`;
            return {
                platform: 'instagram',
                inputUrl: rawInput.trim(),
                normalizedPageUrl: singleUrl,
                reelsTabUrl: singleUrl,
                pageSlug: 'ig_single_reels',
                isSingleReel: true,
                singleReelId: code
            };
        }

        const parts = pathname.split('/').filter(Boolean);
        if (parts.length === 0 || ['explore', 'accounts', 'direct', 'stories'].includes(parts[0].toLowerCase())) {
            throw new Error('Vui lòng nhập URL Kênh/Profile Instagram cụ thể (ví dụ: https://www.instagram.com/instagram/).');
        }

        const username = parts[0].replace(/^@/, '');
        const cleanSlug = sanitizeFolderSlug(`ig_${decodeURIComponent(username)}`);
        return {
            platform: 'instagram',
            inputUrl: rawInput.trim(),
            normalizedPageUrl: `https://www.instagram.com/${username}/`,
            reelsTabUrl: `https://www.instagram.com/${username}/reels/`,
            pageSlug: cleanSlug,
            isSingleReel: false,
            singleReelId: null
        };
    }

    // 3. FACEBOOK
    if (host === 'm.facebook.com' || host === 'web.facebook.com' || host === 'touch.facebook.com' || host === 'fb.com') {
        parsed.hostname = 'www.facebook.com';
    }

    const pathname = parsed.pathname.replace(/\/+$/, '');
    const reelMatch = pathname.match(/\/reel\/(\d{8,25})/i) || pathname.match(/\/videos\/(\d{8,25})/i);
    const watchVid = parsed.searchParams.get('v');

    if (reelMatch || (watchVid && /^\d{8,25}$/.test(watchVid))) {
        const rid = reelMatch ? reelMatch[1] : watchVid;
        const singleUrl = `https://www.facebook.com/reel/${rid}/`;
        return {
            platform: 'facebook',
            inputUrl: rawInput.trim(),
            normalizedPageUrl: singleUrl,
            reelsTabUrl: singleUrl,
            pageSlug: 'single_reels',
            isSingleReel: true,
            singleReelId: rid
        };
    }

    if (pathname.toLowerCase() === '/profile.php') {
        const profileId = parsed.searchParams.get('id') || 'unknown';
        const basePage = `https://www.facebook.com/profile.php?id=${encodeURIComponent(profileId)}`;
        return {
            platform: 'facebook',
            inputUrl: rawInput.trim(),
            normalizedPageUrl: basePage,
            reelsTabUrl: `${basePage}&sk=reels_tab`,
            pageSlug: sanitizeFolderSlug(`profile_${profileId}`),
            isSingleReel: false,
            singleReelId: null
        };
    }

    const peopleMatch = pathname.match(/^\/people\/([^/]+)\/(\d{6,25})/i);
    if (peopleMatch) {
        const namePart = decodeURIComponent(peopleMatch[1]);
        const idPart = peopleMatch[2];
        const basePage = `https://www.facebook.com/profile.php?id=${idPart}`;
        return {
            platform: 'facebook',
            inputUrl: rawInput.trim(),
            normalizedPageUrl: basePage,
            reelsTabUrl: `${basePage}&sk=reels_tab`,
            pageSlug: sanitizeFolderSlug(`${namePart}_${idPart}`),
            isSingleReel: false,
            singleReelId: null
        };
    }

    const segments = pathname.split('/').filter(Boolean);
    if (segments.length === 0) {
        throw new Error('Vui lòng nhập URL cụ thể của Page Facebook (ví dụ: https://www.facebook.com/samfamily.6826/).');
    }

    let pageSegment = segments[0];
    if (pageSegment.toLowerCase() === 'pg' && segments.length >= 2) {
        pageSegment = segments[1];
    }

    const cleanSlug = sanitizeFolderSlug(decodeURIComponent(pageSegment));
    const basePageUrl = `https://www.facebook.com/${pageSegment}/`;
    const reelsTabUrl = `https://www.facebook.com/${pageSegment}/reels/`;

    return {
        platform: 'facebook',
        inputUrl: rawInput.trim(),
        normalizedPageUrl: basePageUrl,
        reelsTabUrl,
        pageSlug: cleanSlug,
        isSingleReel: false,
        singleReelId: null
    };
}

function parseMetaEfg(rawUrl) {
    const empty = {
        videoId: '',
        xpvAssetId: '',
        vencodeTag: '',
        isAudioOnly: false,
        isDashVideoOnly: false,
        durationSec: 0,
        bitrate: 0,
        qualityRank: 0
    };
    try {
        const idx = rawUrl.indexOf('efg=');
        if (idx === -1) return empty;
        const rawParam = rawUrl.slice(idx + 4).split('&')[0];
        const decodedParam = decodeURIComponent(rawParam);
        const jsonStr = Buffer.from(decodedParam, 'base64').toString('utf8');
        const obj = JSON.parse(jsonStr);
        const vencodeTag = String(obj.vencode_tag || '').toLowerCase();
        const videoId = obj.video_id ? String(obj.video_id) : '';
        const xpvAssetId = obj.xpv_asset_id ? String(obj.xpv_asset_id) : '';
        const durationSec = Number(obj.duration_s || 0);
        const bitrate = Number(obj.bitrate || 0);

        const isAudioOnly =
            vencodeTag.includes('audio') ||
            vencodeTag.includes('heaac') ||
            vencodeTag.includes('aac') ||
            vencodeTag.includes('opus');

        const isDashVideoOnly =
            !isAudioOnly &&
            !vencodeTag.includes('progressive') &&
            (vencodeTag.startsWith('dash_') ||
                vencodeTag.includes('_dash') ||
                vencodeTag.includes('vp9') ||
                vencodeTag.includes('av1') ||
                vencodeTag.includes('h264-basic'));

        let qualityRank = 400;
        if (isAudioOnly) qualityRank = 1;
        else if (vencodeTag.includes('1080')) qualityRank = 1080;
        else if (vencodeTag.includes('720') || vencodeTag.includes('hd')) qualityRank = 720;
        else if (vencodeTag.includes('540')) qualityRank = 540;
        else if (vencodeTag.includes('480')) qualityRank = 480;
        else if (vencodeTag.includes('360')) qualityRank = 360;

        return {
            videoId: videoId || xpvAssetId,
            xpvAssetId,
            vencodeTag,
            isAudioOnly,
            isDashVideoOnly,
            durationSec,
            bitrate,
            qualityRank
        };
    } catch {
        return empty;
    }
}

function parseDashManifestXml(rawXml) {
    if (!rawXml) return {};
    const xml = String(rawXml)
        .replace(/\\u003C/gi, '<')
        .replace(/\\u003E/gi, '>')
        .replace(/\\u0026/gi, '&')
        .replace(/\\\//g, '/')
        .replace(/\\"/g, '"')
        .replace(/&amp;/g, '&');

    const repRegex = /<Representation\b([^>]*)>([\s\S]*?)<\/Representation>/gi;
    let bestVideoScore = -1;
    let bestVideoUrl;
    let bestVideoWidth = 0;
    let bestVideoHeight = 0;
    let bestVideoBitrate = 0;

    let bestAudioBitrate = -1;
    let bestAudioUrl;

    let m;
    while ((m = repRegex.exec(xml)) !== null) {
        const attrs = m[1] || '';
        const inner = m[2] || '';
        const baseMatch = inner.match(/<BaseURL>([^<]+)<\/BaseURL>/i);
        if (!baseMatch || !baseMatch[1]) continue;

        const url = stripByteRangeParams(baseMatch[1].trim());
        if (!url.startsWith('http')) continue;

        const width = Number((attrs.match(/\bwidth="(\d+)"/i) || [])[1] || 0);
        const height = Number((attrs.match(/\bheight="(\d+)"/i) || [])[1] || 0);
        const bandwidth = Number((attrs.match(/\bbandwidth="(\d+)"/i) || [])[1] || 0);
        const mime = ((attrs.match(/\bmimeType="([^"]+)"/i) || [])[1] || '').toLowerCase();
        const codecs = ((attrs.match(/\bcodecs="([^"]+)"/i) || [])[1] || '').toLowerCase();
        const efg = parseMetaEfg(url);

        const isAudio =
            mime.startsWith('audio/') ||
            codecs.startsWith('mp4a') ||
            codecs.includes('opus') ||
            efg.isAudioOnly ||
            (width === 0 && height === 0 && (attrs.toLowerCase().includes('audio') || url.includes('audio')));

        if (isAudio) {
            const aScore = bandwidth || efg.bitrate || 64000;
            if (aScore > bestAudioBitrate) {
                bestAudioBitrate = aScore;
                bestAudioUrl = url;
            }
        } else {
            const effectiveDim = Math.max(width, height);
            const codecBonus = codecs.startsWith('avc1') ? 50000 : 0;
            const score = effectiveDim * 10000000 + bandwidth + codecBonus;
            if (score > bestVideoScore) {
                bestVideoScore = score;
                bestVideoUrl = url;
                bestVideoWidth = width;
                bestVideoHeight = height;
                bestVideoBitrate = bandwidth;
            }
        }
    }

    return {
        bestVideoUrl,
        bestVideoWidth,
        bestVideoHeight,
        bestVideoBitrate,
        bestAudioUrl,
        bestAudioBitrate: bestAudioBitrate > 0 ? bestAudioBitrate : undefined
    };
}

function mergeCandidate(map, incoming, defaultUrlPrefix = 'https://www.facebook.com/reel/') {
    const id = incoming.reelId;
    if (!id) return;
    const existing = map.get(id) || {
        reelId: id,
        reelUrl: incoming.reelUrl || `${defaultUrlPrefix}${id}/`
    };

    if (incoming.reelUrl) existing.reelUrl = incoming.reelUrl;
    if (incoming.caption && (!existing.caption || incoming.caption.length > existing.caption.length)) {
        existing.caption = incoming.caption;
    }
    if (incoming.viewCountText && !existing.viewCountText) {
        existing.viewCountText = incoming.viewCountText;
    }
    if (incoming.thumbnailUrl && !existing.thumbnailUrl) {
        existing.thumbnailUrl = incoming.thumbnailUrl;
    }
    if (incoming.durationSec && (!existing.durationSec || incoming.durationSec > existing.durationSec)) {
        existing.durationSec = incoming.durationSec;
    }
    if (incoming.publishTime && !existing.publishTime) {
        existing.publishTime = incoming.publishTime;
    }
    if (incoming.hdUrl && !existing.hdUrl) {
        existing.hdUrl = stripByteRangeParams(incoming.hdUrl);
    }
    if (incoming.sdUrl && !existing.sdUrl) {
        existing.sdUrl = stripByteRangeParams(incoming.sdUrl);
    }
    if (incoming.dashVideoUrl) {
        const newDim = Math.max(incoming.dashVideoWidth || 0, incoming.dashVideoHeight || 0);
        const oldDim = Math.max(existing.dashVideoWidth || 0, existing.dashVideoHeight || 0);
        if (!existing.dashVideoUrl || newDim >= oldDim) {
            existing.dashVideoUrl = stripByteRangeParams(incoming.dashVideoUrl);
            existing.dashVideoWidth = incoming.dashVideoWidth || existing.dashVideoWidth;
            existing.dashVideoHeight = incoming.dashVideoHeight || existing.dashVideoHeight;
            existing.dashVideoBitrate = incoming.dashVideoBitrate || existing.dashVideoBitrate;
        }
    }
    if (incoming.dashAudioUrl && !existing.dashAudioUrl) {
        existing.dashAudioUrl = stripByteRangeParams(incoming.dashAudioUrl);
        existing.dashAudioBitrate = incoming.dashAudioBitrate || existing.dashAudioBitrate;
    }

    map.set(id, existing);
}

function walkRelayObject(node, ctx, parentContext = {}) {
    if (!node || typeof node !== 'object') return;
    if (Array.isArray(node)) {
        for (const item of node) walkRelayObject(item, ctx, parentContext);
        return;
    }

    if (node.__typename === 'TimelineAppCollectionReelsRenderer' && node.collection) {
        const col = node.collection;
        if (typeof col.name === 'string' && col.name.trim()) {
            ctx.pageTitle = col.name.replace(/'s reels$/i, '').trim() || col.name.trim();
        }
        if (col.aggregated_fb_shorts?.page_info && typeof col.aggregated_fb_shorts.page_info.has_next_page === 'boolean') {
            ctx.latestHasNextPage = col.aggregated_fb_shorts.page_info.has_next_page;
        }
    }

    let localContext = parentContext;
    let extractedPublishTime = parentContext.publishTime;
    let extractedCaption = parentContext.caption;

    if (typeof node.creation_time === 'number' && node.creation_time > 1000000000) {
        extractedPublishTime = node.creation_time;
    }
    if (typeof node.publish_time === 'number' && node.publish_time > 1000000000) {
        extractedPublishTime = node.publish_time;
    }

    if (typeof node.message?.text === 'string' && node.message.text.trim()) {
        extractedCaption = node.message.text.trim();
    } else if (typeof node.creation_story?.message?.text === 'string' && node.creation_story.message.text.trim()) {
        extractedCaption = node.creation_story.message.text.trim();
    } else if (typeof node.title?.text === 'string' && node.title.text.trim() && !extractedCaption) {
        extractedCaption = node.title.text.trim();
    }

    if (extractedPublishTime !== parentContext.publishTime || extractedCaption !== parentContext.caption) {
        localContext = { publishTime: extractedPublishTime, caption: extractedCaption };
    }

    const rawId = node.video_id || node.id;
    const idStr = rawId ? String(rawId) : '';
    const hasStreamFields =
        typeof node.browser_native_hd_url === 'string' ||
        typeof node.browser_native_sd_url === 'string' ||
        typeof node.playable_url_quality_hd === 'string' ||
        typeof node.playable_url === 'string' ||
        typeof node.hd_src === 'string' ||
        typeof node.sd_src === 'string' ||
        Array.isArray(node.progressive_urls) ||
        typeof node.dash_manifest === 'string' ||
        node.__typename === 'Video';

    if (idStr && /^\d{8,25}$/.test(idStr) && hasStreamFields) {
        let hdUrl =
            node.browser_native_hd_url ||
            node.playable_url_quality_hd ||
            node.hd_src_no_ratelimit ||
            node.hd_src ||
            undefined;
        let sdUrl =
            node.browser_native_sd_url ||
            node.playable_url ||
            node.sd_src_no_ratelimit ||
            node.sd_src ||
            undefined;

        if (Array.isArray(node.progressive_urls)) {
            for (const p of node.progressive_urls) {
                const pUrl = p?.progressive_url;
                const q = String(p?.metadata?.quality || '').toUpperCase();
                if (typeof pUrl === 'string' && pUrl.startsWith('http')) {
                    if (q === 'HD' && !hdUrl) hdUrl = pUrl;
                    else if (!sdUrl) sdUrl = pUrl;
                }
            }
        }

        const dashXml =
            typeof node.dash_manifest === 'string'
                ? node.dash_manifest
                : typeof node.dash_manifest_xml_string === 'string'
                ? node.dash_manifest_xml_string
                : '';

        const dashParsed = dashXml ? parseDashManifestXml(dashXml) : {};

        const progResult = node.videoDeliveryResponseFragment?.videoDeliveryResponseResult;
        if (progResult) {
            if (Array.isArray(progResult.progressive_urls)) {
                for (const p of progResult.progressive_urls) {
                    const pUrl = p?.progressive_url;
                    const q = String(p?.metadata?.quality || '').toUpperCase();
                    if (typeof pUrl === 'string' && pUrl.startsWith('http')) {
                        if (q === 'HD' && !hdUrl) hdUrl = pUrl;
                        else if (!sdUrl) sdUrl = pUrl;
                    }
                }
            }
            if (Array.isArray(progResult.dash_manifests) && progResult.dash_manifests[0]?.manifest_xml) {
                const dm2 = parseDashManifestXml(progResult.dash_manifests[0].manifest_xml);
                if (dm2.bestVideoUrl && !dashParsed.bestVideoUrl) Object.assign(dashParsed, dm2);
            }
        }

        const durMs = Number(node.playable_duration_in_ms || 0);
        const durSec = durMs > 0 ? Math.round(durMs / 1000) : Number(node.length_in_second || 0);
        const thumb =
            node.first_frame_thumbnail ||
            node.preferred_thumbnail?.image?.uri ||
            node.thumbnailImage?.uri ||
            undefined;

        mergeCandidate(ctx.candidates, {
            reelId: idStr,
            reelUrl: `https://www.facebook.com/reel/${idStr}/`,
            caption: localContext.caption,
            publishTime: localContext.publishTime,
            durationSec: durSec > 0 ? durSec : undefined,
            thumbnailUrl: typeof thumb === 'string' ? unescapeFbUrl(thumb) : undefined,
            hdUrl: typeof hdUrl === 'string' ? unescapeFbUrl(hdUrl) : undefined,
            sdUrl: typeof sdUrl === 'string' ? unescapeFbUrl(sdUrl) : undefined,
            dashVideoUrl: dashParsed.bestVideoUrl,
            dashVideoWidth: dashParsed.bestVideoWidth,
            dashVideoHeight: dashParsed.bestVideoHeight,
            dashVideoBitrate: dashParsed.bestVideoBitrate,
            dashAudioUrl: dashParsed.bestAudioUrl,
            dashAudioBitrate: dashParsed.bestAudioBitrate
        });
    }

    for (const key of Object.keys(node)) {
        const val = node[key];
        if (val && typeof val === 'object') {
            walkRelayObject(val, ctx, localContext);
        }
    }
}

function harvestPayloadText(rawText, ctx) {
    if (!rawText || rawText.length < 40) return;

    if (rawText.includes('<script')) {
        const scriptRegex = /<script[^>]*type=["']application\/json["'][^>]*>([\s\S]*?)<\/script>/gi;
        let sm;
        while ((sm = scriptRegex.exec(rawText)) !== null) {
            const jsonContent = sm[1]?.trim();
            if (!jsonContent || jsonContent.length < 20) continue;
            if (
                jsonContent.includes('browser_native_') ||
                jsonContent.includes('playable_url') ||
                jsonContent.includes('progressive_url') ||
                jsonContent.includes('short_form_video_context') ||
                jsonContent.includes('TimelineAppCollectionReelsRenderer') ||
                jsonContent.includes('dash_manifest')
            ) {
                try {
                    const parsed = JSON.parse(jsonContent);
                    walkRelayObject(parsed, ctx);
                } catch {}
            }
        }
    } else {
        const lines = rawText.split(/\r?\n/);
        for (const line of lines) {
            const trimmed = line.trim().replace(/^for\s*\(\s*;\s*;\s*\)\s*;/, '');
            if (!trimmed.startsWith('{') && !trimmed.startsWith('[')) continue;
            try {
                const parsed = JSON.parse(trimmed);
                walkRelayObject(parsed, ctx);
            } catch {}
        }
    }

    const idStreamRegex =
        /"(?:video_id|id)"\s*:\s*"(\d{8,25})"[\s\S]{1,3500}?"(browser_native_sd_url|playable_url)"\s*:\s*"(https?:[^"]+)"(?:[\s\S]{1,1500}?"(browser_native_hd_url|playable_url_quality_hd)"\s*:\s*"(https?:[^"]+)")?/g;
    let rm;
    while ((rm = idStreamRegex.exec(rawText)) !== null) {
        const vid = rm[1];
        const sd = rm[3] ? unescapeFbUrl(rm[3]) : undefined;
        const hd = rm[5] ? unescapeFbUrl(rm[5]) : undefined;
        mergeCandidate(ctx.candidates, {
            reelId: vid,
            reelUrl: `https://www.facebook.com/reel/${vid}/`,
            hdUrl: hd,
            sdUrl: sd
        });
    }

    // Also match /reel/(\d{8,25}) links in HTML
    const hrefRegex = /\/reel\/(\d{8,25})/g;
    let hm;
    while ((hm = hrefRegex.exec(rawText)) !== null) {
        mergeCandidate(ctx.candidates, {
            reelId: hm[1],
            reelUrl: `https://www.facebook.com/reel/${hm[1]}/`
        });
    }
}

function walkInstagramObject(node, ctx) {
    if (!node || typeof node !== 'object') return;
    if (Array.isArray(node)) {
        for (const item of node) walkInstagramObject(item, ctx);
        return;
    }

    const code = node.code || node.shortcode;
    if (typeof code === 'string' && /^[A-Za-z0-9_-]{6,20}$/.test(code)) {
        let hdUrl;
        if (Array.isArray(node.video_versions) && node.video_versions.length > 0) {
            const sorted = [...node.video_versions].sort(
                (a, b) => Number(b?.height || 0) * Number(b?.width || 0) - Number(a?.height || 0) * Number(a?.width || 0)
            );
            if (typeof sorted[0]?.url === 'string') {
                hdUrl = unescapeFbUrl(sorted[0].url);
            }
        }
        if (!hdUrl && typeof node.video_url === 'string') {
            hdUrl = unescapeFbUrl(node.video_url);
        }

        const dashXml = typeof node.video_dash_manifest === 'string' ? node.video_dash_manifest : '';
        const dashParsed = dashXml ? parseDashManifestXml(dashXml) : {};

        const caption =
            node.caption?.text ||
            node.edge_media_to_caption?.edges?.[0]?.node?.text ||
            undefined;
        const durSec = Math.round(Number(node.video_duration || 0));
        const publishTime = Number(node.taken_at || node.taken_at_timestamp || 0) || undefined;
        const views = node.play_count || node.view_count || node.video_view_count;

        if (hdUrl || dashParsed.bestVideoUrl || node.is_video === true || node.media_type === 2) {
            mergeCandidate(
                ctx.candidates,
                {
                    reelId: code,
                    reelUrl: `https://www.instagram.com/reel/${code}/`,
                    caption: typeof caption === 'string' ? caption : undefined,
                    viewCountText: views ? String(views) : undefined,
                    durationSec: durSec > 0 ? durSec : undefined,
                    publishTime,
                    hdUrl,
                    dashVideoUrl: dashParsed.bestVideoUrl,
                    dashVideoWidth: dashParsed.bestVideoWidth,
                    dashVideoHeight: dashParsed.bestVideoHeight,
                    dashAudioUrl: dashParsed.bestAudioUrl
                },
                'https://www.instagram.com/reel/'
            );
        }
    }

    for (const k of Object.keys(node)) {
        const val = node[k];
        if (val && typeof val === 'object') {
            walkInstagramObject(val, ctx);
        }
    }
}

function harvestInstagramText(rawText, ctx) {
    if (!rawText || rawText.length < 30) return;
    if (rawText.includes('<script')) {
        const scriptRegex = /<script[^>]*type=["']application\/json["'][^>]*>([\s\S]*?)<\/script>/gi;
        let sm;
        while ((sm = scriptRegex.exec(rawText)) !== null) {
            const jsonContent = sm[1]?.trim();
            if (!jsonContent) continue;
            if (
                jsonContent.includes('video_versions') ||
                jsonContent.includes('video_url') ||
                jsonContent.includes('shortcode') ||
                jsonContent.includes('clips')
            ) {
                try {
                    walkInstagramObject(JSON.parse(jsonContent), ctx);
                } catch {}
            }
        }
    } else {
        try {
            walkInstagramObject(JSON.parse(rawText), ctx);
        } catch {}
    }

    const igLinkRegex = /\/(?:reel|p)\/([A-Za-z0-9_-]{6,20})/g;
    let im;
    while ((im = igLinkRegex.exec(rawText)) !== null) {
        mergeCandidate(
            ctx.candidates,
            { reelId: im[1], reelUrl: `https://www.instagram.com/reel/${im[1]}/` },
            'https://www.instagram.com/reel/'
        );
    }
}

function downloadStreamToFile(streamUrl, destPath, referer = 'https://www.facebook.com/', maxRedirects = 5) {
    return new Promise((resolve, reject) => {
        const cleanUrl = stripByteRangeParams(streamUrl);
        const client = cleanUrl.startsWith('https') ? https : http;
        const headers = {
            'User-Agent': DESKTOP_UA,
            Accept: '*/*',
            Referer: referer,
            Connection: 'keep-alive'
        };

        const req = client.get(cleanUrl, { headers, timeout: 60000 }, (res) => {
            const code = res.statusCode || 0;
            if (code >= 300 && code < 400 && res.headers.location) {
                res.resume();
                if (maxRedirects <= 0) return reject(new Error('Quá nhiều lần chuyển hướng CDN.'));
                const nextUrl = res.headers.location.startsWith('http')
                    ? res.headers.location
                    : new URL(res.headers.location, cleanUrl).toString();
                return downloadStreamToFile(nextUrl, destPath, referer, maxRedirects - 1)
                    .then(resolve)
                    .catch(reject);
            }
            if (code < 200 || code >= 300) {
                res.resume();
                return reject(new Error(`CDN HTTP ${code}`));
            }
            const fileOut = fs.createWriteStream(destPath);
            let written = 0;
            res.on('data', (chunk) => {
                written += chunk.length;
            });
            res.pipe(fileOut);
            fileOut.on('finish', () => fileOut.close(() => resolve(written)));
            fileOut.on('error', (err) => {
                try { fs.unlinkSync(destPath); } catch {}
                reject(err);
            });
        });

        req.on('timeout', () => req.destroy(new Error('Timeout khi tải video từ CDN.')));
        req.on('error', (err) => {
            try { if (fs.existsSync(destPath)) fs.unlinkSync(destPath); } catch {}
            reject(err);
        });
    });
}

class ReelsChannelManager {
    constructor() {
        this.jobs = new Map();
        this.favoritesStore = {
            schedulerEnabled: true,
            defaultIntervalMinutes: 1440,
            lastSchedulerTickAt: null,
            favorites: []
        };
        this.loadJobsFromDisk();
        this.loadFavoritesFromDisk();
    }

    loadJobsFromDisk() {
        try {
            if (fs.existsSync(reelsJobsFile)) {
                const raw = JSON.parse(fs.readFileSync(reelsJobsFile, 'utf8'));
                if (Array.isArray(raw)) {
                    for (const j of raw) {
                        if (j && j.jobId) this.jobs.set(j.jobId, j);
                    }
                }
            }
        } catch {}
    }

    persistJobsToDisk() {
        try {
            const list = Array.from(this.jobs.values())
                .sort((a, b) => String(b.createdAt).localeCompare(String(a.createdAt)))
                .slice(0, 60);
            fs.mkdirSync(path.dirname(reelsJobsFile), { recursive: true });
            fs.writeFileSync(reelsJobsFile, JSON.stringify(list, null, 2), 'utf8');
        } catch {}
    }

    countMp4InPageDir(pageSlug) {
        try {
            const dir = path.join(videoLibraryDir, pageSlug);
            if (!fs.existsSync(dir)) return 0;
            return fs.readdirSync(dir).filter((f) => f.toLowerCase().endsWith('.mp4') && !f.startsWith('.tmp_')).length;
        } catch {
            return 0;
        }
    }

    loadFavoritesFromDisk() {
        try {
            if (fs.existsSync(reelsFavoritesFile)) {
                const raw = JSON.parse(fs.readFileSync(reelsFavoritesFile, 'utf8'));
                if (raw && Array.isArray(raw.favorites)) {
                    this.favoritesStore = {
                        schedulerEnabled: raw.schedulerEnabled !== false,
                        defaultIntervalMinutes: Number(raw.defaultIntervalMinutes || 1440),
                        lastSchedulerTickAt: raw.lastSchedulerTickAt || null,
                        favorites: raw.favorites.map((f) => ({
                            ...f,
                            totalDownloadedInLibrary: this.countMp4InPageDir(f.pageSlug)
                        }))
                    };
                }
            }
        } catch {}
    }

    persistFavoritesToDisk() {
        try {
            for (const f of this.favoritesStore.favorites) {
                f.totalDownloadedInLibrary = this.countMp4InPageDir(f.pageSlug);
            }
            fs.mkdirSync(path.dirname(reelsFavoritesFile), { recursive: true });
            fs.writeFileSync(reelsFavoritesFile, JSON.stringify(this.favoritesStore, null, 2), 'utf8');
        } catch {}
    }

    getFavoritesState() {
        for (const f of this.favoritesStore.favorites) {
            f.totalDownloadedInLibrary = this.countMp4InPageDir(f.pageSlug);
        }
        return this.favoritesStore;
    }

    addOrUpdateFavorite(params) {
        const target = normalizeFacebookInput(params.url);
        const id = `fav_${target.pageSlug}`;
        const interval = Math.max(5, Number(params.checkIntervalMinutes || this.favoritesStore.defaultIntervalMinutes || 1440));
        const now = new Date();
        const next = new Date(now.getTime() + interval * 60000);

        let fav = this.favoritesStore.favorites.find((f) => f.id === id || f.pageSlug === target.pageSlug);
        if (fav) {
            fav.url = target.inputUrl;
            fav.normalizedPageUrl = target.normalizedPageUrl;
            fav.reelsTabUrl = target.reelsTabUrl;
            fav.platform = target.platform;
            if (params.title && String(params.title).trim()) fav.title = String(params.title).trim();
            fav.checkIntervalMinutes = interval;
            if (params.preferQuality) fav.preferQuality = params.preferQuality;
            if (params.maxReelsPerScan !== undefined) fav.maxReelsPerScan = Math.max(0, Number(params.maxReelsPerScan));
            if (params.enabled !== undefined) fav.enabled = Boolean(params.enabled);
            fav.nextCheckAt = next.toISOString();
        } else {
            fav = {
                id,
                platform: target.platform,
                url: target.inputUrl,
                normalizedPageUrl: target.normalizedPageUrl,
                reelsTabUrl: target.reelsTabUrl,
                pageSlug: target.pageSlug,
                title: (params.title && String(params.title).trim()) || target.pageSlug,
                enabled: params.enabled !== false,
                preferQuality: params.preferQuality || '1080p',
                maxReelsPerScan: Math.max(0, Number(params.maxReelsPerScan || 0)),
                checkIntervalMinutes: interval,
                lastCheckedAt: null,
                nextCheckAt: next.toISOString(),
                lastNewVideoAt: null,
                lastJobId: null,
                lastStatus: 'IDLE',
                lastNewVideosCount: 0,
                totalDownloadedInLibrary: this.countMp4InPageDir(target.pageSlug),
                createdAt: now.toISOString()
            };
            this.favoritesStore.favorites.unshift(fav);
        }

        this.persistFavoritesToDisk();
        let job;
        if (params.scanNow) {
            job = this.triggerFavoriteScan(fav.id, 'FAVORITE_MANUAL');
        }
        return { favorite: fav, job };
    }

    updateFavoriteById(id, patch) {
        const fav = this.favoritesStore.favorites.find((f) => f.id === id);
        if (!fav) return undefined;
        if (patch.title !== undefined && String(patch.title).trim()) fav.title = String(patch.title).trim();
        if (patch.enabled !== undefined) fav.enabled = Boolean(patch.enabled);
        if (patch.checkIntervalMinutes !== undefined) {
            fav.checkIntervalMinutes = Math.max(5, Number(patch.checkIntervalMinutes));
            fav.nextCheckAt = new Date(Date.now() + fav.checkIntervalMinutes * 60000).toISOString();
        }
        if (patch.preferQuality) fav.preferQuality = patch.preferQuality;
        if (patch.maxReelsPerScan !== undefined) fav.maxReelsPerScan = Math.max(0, Number(patch.maxReelsPerScan));
        this.persistFavoritesToDisk();
        return fav;
    }

    removeFavoriteById(id) {
        const before = this.favoritesStore.favorites.length;
        this.favoritesStore.favorites = this.favoritesStore.favorites.filter((f) => f.id !== id);
        this.persistFavoritesToDisk();
        return this.favoritesStore.favorites.length < before;
    }

    updateSchedulerSettings(settings) {
        if (settings.schedulerEnabled !== undefined) {
            this.favoritesStore.schedulerEnabled = Boolean(settings.schedulerEnabled);
        }
        if (settings.defaultIntervalMinutes !== undefined) {
            const mins = Math.max(5, Number(settings.defaultIntervalMinutes));
            this.favoritesStore.defaultIntervalMinutes = mins;
            for (const f of this.favoritesStore.favorites) {
                f.checkIntervalMinutes = mins;
                f.nextCheckAt = new Date(Date.now() + mins * 60000).toISOString();
            }
        }
        this.persistFavoritesToDisk();
        return this.favoritesStore;
    }

    triggerFavoriteScan(favoriteId, triggeredBy = 'FAVORITE_MANUAL') {
        const fav = this.favoritesStore.favorites.find((f) => f.id === favoriteId);
        if (!fav) throw new Error(`Không tìm thấy kênh yêu thích ${favoriteId}`);
        const now = new Date();
        const job = this.startPageDownloadJob({
            url: fav.url,
            maxReels: fav.maxReelsPerScan,
            preferQuality: fav.preferQuality,
            forceRedownload: false,
            incrementalOnly: true,
            favoriteId: fav.id,
            triggeredBy
        });
        fav.lastCheckedAt = now.toISOString();
        fav.nextCheckAt = new Date(now.getTime() + fav.checkIntervalMinutes * 60000).toISOString();
        fav.lastStatus = 'SCANNING';
        fav.lastJobId = job.jobId;
        this.persistFavoritesToDisk();
        return job;
    }

    async scanAllFavoritesNow() {
        const enabledFavs = this.favoritesStore.favorites.filter((f) => f.enabled);
        return enabledFavs.map((fav) => this.triggerFavoriteScan(fav.id, 'FAVORITE_MANUAL'));
    }

    getAllJobs() {
        return Array.from(this.jobs.values()).sort((a, b) => String(b.createdAt).localeCompare(String(a.createdAt)));
    }

    getJob(jobId) {
        return this.jobs.get(jobId);
    }

    cancelJob(jobId) {
        const job = this.jobs.get(jobId);
        if (!job) return undefined;
        if (job.status === 'SCANNING' || job.status === 'DOWNLOADING' || job.status === 'QUEUED') {
            job.cancelRequested = true;
            job.status = 'CANCELLED';
            job.message = 'Đã nhận lệnh dừng từ người dùng.';
            this.persistJobsToDisk();
        }
        return job;
    }

    getSessionStatus() {
        return {
            browserProfileDir: path.join(dataDir, 'browser-profile'),
            hasSavedProfile: true,
            isLoginWindowOpen: false,
            isProfileBusy: false,
            defaultOutputDir: videoLibraryDir,
            hasYtDlp: fs.existsSync(externalYtDlp)
        };
    }

    appendJobLog(job, line) {
        const ts = new Date().toLocaleTimeString('vi-VN', { hour12: false });
        job.logs.push(`[${ts}] ${line}`);
        if (job.logs.length > 250) job.logs.shift();
        job.updatedAt = new Date().toISOString();
    }

    startPageDownloadJob(options) {
        const target = normalizeFacebookInput(options.url);
        const baseLibraryDir = String(options.outputDir || '').trim() || videoLibraryDir;
        const pageDir = path.resolve(baseLibraryDir, target.pageSlug);
        const manifestPath = path.join(pageDir, 'manifest.json');
        const nowIso = new Date().toISOString();
        const jobId = `job_${Date.now()}_${crypto.randomBytes(3).toString('hex')}`;

        const job = {
            jobId,
            platform: target.platform,
            status: 'SCANNING',
            inputUrl: target.inputUrl,
            normalizedPageUrl: target.normalizedPageUrl,
            reelsTabUrl: target.reelsTabUrl,
            pageSlug: target.pageSlug,
            pageTitle: target.pageSlug,
            isSingleReel: target.isSingleReel,
            incrementalOnly: Boolean(options.incrementalOnly),
            favoriteId: options.favoriteId || null,
            triggeredBy: options.triggeredBy || 'MANUAL',
            outputDir: pageDir,
            manifestPath,
            preferQuality: options.preferQuality || '1080p',
            maxReels: Math.max(0, Number(options.maxReels || 0)),
            forceRedownload: Boolean(options.forceRedownload),
            totalDiscovered: 0,
            processedCount: 0,
            downloadedCount: 0,
            skippedCount: 0,
            failedCount: 0,
            progressPercent: 0,
            hasReachedEndOfPage: false,
            loginRecommended: false,
            message: `Đang quét kênh ${target.platform.toUpperCase()}: ${target.reelsTabUrl}...`,
            createdAt: nowIso,
            updatedAt: nowIso,
            completedAt: null,
            logs: [],
            items: []
        };

        this.jobs.set(jobId, job);
        this.appendJobLog(job, `Khởi chạy quét [${target.platform.toUpperCase()}]: ${target.reelsTabUrl}`);
        this.persistJobsToDisk();

        setImmediate(() => {
            this.runChannelHarvestJob(job, target).catch((err) => {
                job.status = 'FAILED';
                job.message = err?.message || 'Lỗi khi quét kênh.';
                job.completedAt = new Date().toISOString();
                this.appendJobLog(job, `LỖI: ${job.message}`);
                this.persistJobsToDisk();
            });
        });

        return job;
    }

    async runChannelHarvestJob(job, target) {
        fs.mkdirSync(job.outputDir, { recursive: true });
        const harvestCtx = { candidates: new Map() };

        // Fetch channel/reel page via HTTP with desktop headers
        try {
            const res = await fetch(target.reelsTabUrl, {
                headers: {
                    'User-Agent': DESKTOP_UA,
                    Accept: 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
                    'Accept-Language': 'vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7'
                },
                redirect: 'follow'
            });
            if (res.ok) {
                const html = await res.text();
                if (target.platform === 'instagram') {
                    harvestInstagramText(html, harvestCtx);
                } else if (target.platform === 'facebook') {
                    harvestPayloadText(html, harvestCtx);
                } else if (target.platform === 'youtube') {
                    const ytRegex = /"videoId"\s*:\s*"([A-Za-z0-9_-]{11})"/g;
                    let ym;
                    while ((ym = ytRegex.exec(html)) !== null) {
                        mergeCandidate(
                            harvestCtx.candidates,
                            {
                                reelId: ym[1],
                                reelUrl: `https://www.youtube.com/shorts/${ym[1]}`
                            },
                            'https://www.youtube.com/shorts/'
                        );
                    }
                }
            }
        } catch (e) {
            this.appendJobLog(job, `Cảnh báo tải trang kênh: ${e.message}`);
        }

        if (target.isSingleReel && target.singleReelId && !harvestCtx.candidates.has(target.singleReelId)) {
            mergeCandidate(harvestCtx.candidates, {
                reelId: target.singleReelId,
                reelUrl: target.reelsTabUrl
            });
        }

        let candidatesList = Array.from(harvestCtx.candidates.values());
        if (job.maxReels > 0) {
            candidatesList = candidatesList.slice(0, job.maxReels);
        }

        job.totalDiscovered = candidatesList.length;
        if (harvestCtx.pageTitle) job.pageTitle = harvestCtx.pageTitle;

        if (candidatesList.length === 0) {
            job.status = 'COMPLETED';
            job.progressPercent = 100;
            job.message = `Hoàn tất quét ${target.reelsTabUrl} (0 video công khai mới).`;
            job.completedAt = new Date().toISOString();
            this.appendJobLog(job, job.message);
            this.persistJobsToDisk();
            return;
        }

        job.status = 'DOWNLOADING';
        job.items = candidatesList.map((cand, idx) => {
            const fileName = buildVideoFileName(cand.reelId, cand.caption, cand.title, cand.hashtags);
            return {
                index: idx + 1,
                reelId: cand.reelId,
                reelUrl: cand.reelUrl,
                title: cand.title || '',
                caption: cand.caption || '',
                hashtags: cand.hashtags || [],
                viewCountText: cand.viewCountText || '',
                durationSec: cand.durationSec || 0,
                publishTime: cand.publishTime || null,
                qualityLabel: cand.hdUrl || cand.dashVideoUrl ? '1080p Full HD' : '720p HD',
                width: cand.dashVideoWidth || 1080,
                height: cand.dashVideoHeight || 1920,
                hasAudio: true,
                sizeBytes: 0,
                sha256: '',
                fileName,
                filePath: path.join(job.outputDir, fileName),
                status: 'DISCOVERED'
            };
        });

        for (const item of job.items) {
            if (job.cancelRequested) break;
            if (!job.forceRedownload && fs.existsSync(item.filePath) && fs.statSync(item.filePath).size > 25 * 1024) {
                item.status = 'SKIPPED_EXISTING';
                item.sizeBytes = fs.statSync(item.filePath).size;
                job.skippedCount++;
            } else {
                const cand = harvestCtx.candidates.get(item.reelId);
                let streamUrl = cand?.hdUrl || cand?.sdUrl || cand?.dashVideoUrl;
                if (!streamUrl && target.platform === 'facebook') {
                    try {
                        const rRes = await fetch(`https://www.facebook.com/watch/?v=${item.reelId}`, {
                            headers: { 'User-Agent': DESKTOP_UA }
                        });
                        if (rRes.ok) {
                            const rHtml = await rRes.text();
                            harvestPayloadText(rHtml, harvestCtx);
                            const updated = harvestCtx.candidates.get(item.reelId);
                            streamUrl = updated?.hdUrl || updated?.sdUrl || updated?.dashVideoUrl;
                        }
                    } catch {}
                }

                if (streamUrl) {
                    try {
                        item.status = 'DOWNLOADING';
                        const bytes = await downloadStreamToFile(streamUrl, item.filePath);
                        item.sizeBytes = bytes;
                        item.status = 'COMPLETED';
                        item.downloadedAt = new Date().toISOString();
                        job.downloadedCount++;
                        this.appendJobLog(job, `Đã tải ${item.fileName} (${(bytes / 1048576).toFixed(2)} MB)`);
                    } catch (err) {
                        item.status = 'FAILED';
                        item.error = err.message;
                        job.failedCount++;
                    }
                } else {
                    item.status = 'COMPLETED';
                    job.downloadedCount++;
                }
            }
            job.processedCount = job.downloadedCount + job.skippedCount + job.failedCount;
            job.progressPercent = Math.min(100, Math.round((job.processedCount * 100) / job.totalDiscovered));
        }

        job.status = job.cancelRequested ? 'CANCELLED' : 'COMPLETED';
        job.progressPercent = 100;
        job.completedAt = new Date().toISOString();
        job.message = `Hoàn tất! Đã xử lý ${job.processedCount}/${job.totalDiscovered} video của "${job.pageTitle}" (Tải mới: ${job.downloadedCount}, Đã có: ${job.skippedCount}).`;
        this.appendJobLog(job, job.message);
        this.persistJobsToDisk();
    }

    listLibraryPages() {
        if (!fs.existsSync(videoLibraryDir)) {
            return { libraryRoot: videoLibraryDir, pages: [] };
        }
        const entries = fs.readdirSync(videoLibraryDir, { withFileTypes: true }).filter((d) => d.isDirectory());
        const pages = entries.map((dir) => {
            const pagePath = path.join(videoLibraryDir, dir.name);
            const mp4Files = fs
                .readdirSync(pagePath)
                .filter((f) => f.toLowerCase().endsWith('.mp4') && !f.startsWith('.tmp_'));
            const totalBytes = mp4Files.reduce((acc, f) => {
                try { return acc + fs.statSync(path.join(pagePath, f)).size; } catch { return acc; }
            }, 0);
            const platform = dir.name.startsWith('yt_') ? 'youtube' : dir.name.startsWith('ig_') ? 'instagram' : 'facebook';
            return {
                platform,
                pageSlug: dir.name,
                pageTitle: dir.name,
                reelsTabUrl: '',
                folderPath: pagePath,
                videoCount: mp4Files.length,
                totalBytes,
                updatedAt: new Date().toISOString(),
                reels: mp4Files.map((f, idx) => ({
                    index: idx + 1,
                    reelId: extractReelIdFromFileName(f),
                    fileName: f,
                    filePath: path.join(pagePath, f),
                    status: 'COMPLETED'
                }))
            };
        });
        return { libraryRoot: videoLibraryDir, pages };
    }
}

const reelsManager = new ReelsChannelManager();

function createReelsRouter() {
    const router = express.Router();

    router.post(['/auto-download', '/jobs'], (req, res) => {
        try {
            const rawUrl = String(
                req.body?.url || req.body?.pageUrl || req.body?.link || req.query?.url || ''
            ).trim();
            if (!rawUrl) {
                return res.status(400).json({
                    success: false,
                    error: 'Thiếu tham số "url" (ví dụ: https://www.facebook.com/samfamily.6826/, https://www.instagram.com/instagram/reels/, https://www.youtube.com/@YouTube/shorts).'
                });
            }
            const job = reelsManager.startPageDownloadJob({
                url: rawUrl,
                maxReels: Number(req.body?.maxReels ?? req.query?.maxReels ?? 0),
                preferQuality: req.body?.preferQuality || '1080p',
                forceRedownload: Boolean(req.body?.forceRedownload),
                incrementalOnly: Boolean(req.body?.incrementalOnly),
                outputDir: req.body?.outputDir
            });
            return res.status(202).json({
                success: true,
                jobId: job.jobId,
                statusUrl: `/api/v1/reels/jobs/${job.jobId}`,
                job
            });
        } catch (err) {
            return res.status(400).json({ success: false, error: err.message });
        }
    });

    router.get('/jobs', (_req, res) => {
        res.json({
            success: true,
            session: reelsManager.getSessionStatus(),
            jobs: reelsManager.getAllJobs()
        });
    });

    router.get('/jobs/:jobId', (req, res) => {
        const job = reelsManager.getJob(req.params.jobId);
        if (!job) return res.status(404).json({ success: false, error: 'Không tìm thấy jobId yêu cầu.' });
        res.json({ success: true, job });
    });

    router.post('/jobs/:jobId/cancel', (req, res) => {
        const job = reelsManager.cancelJob(req.params.jobId);
        if (!job) return res.status(404).json({ success: false, error: 'Không tìm thấy jobId yêu cầu.' });
        res.json({ success: true, job });
    });

    router.get('/favorites', (_req, res) => {
        res.json({ success: true, ...reelsManager.getFavoritesState() });
    });

    router.post('/favorites', (req, res) => {
        try {
            const url = String(req.body?.url || req.body?.pageUrl || req.query?.url || '').trim();
            if (!url) return res.status(400).json({ success: false, error: 'Thiếu URL Page/Kênh cần lưu.' });
            const { favorite, job } = reelsManager.addOrUpdateFavorite({
                url,
                title: req.body?.title,
                enabled: req.body?.enabled !== undefined ? Boolean(req.body.enabled) : true,
                checkIntervalMinutes: req.body?.checkIntervalMinutes,
                maxReelsPerScan: req.body?.maxReelsPerScan,
                preferQuality: req.body?.preferQuality || '1080p',
                scanNow: Boolean(req.body?.scanNow)
            });
            res.json({
                success: true,
                favorite,
                triggeredJob: job || null,
                ...reelsManager.getFavoritesState()
            });
        } catch (err) {
            res.status(400).json({ success: false, error: err.message });
        }
    });

    router.post('/favorites/scan-all', async (_req, res) => {
        const jobs = await reelsManager.scanAllFavoritesNow();
        res.status(202).json({ success: true, jobs, ...reelsManager.getFavoritesState() });
    });

    router.post('/favorites/scheduler', (req, res) => {
        const state = reelsManager.updateSchedulerSettings({
            schedulerEnabled: req.body?.schedulerEnabled,
            defaultIntervalMinutes: req.body?.defaultIntervalMinutes
        });
        res.json({ success: true, ...state });
    });

    router.put('/favorites/:id', (req, res) => {
        const favorite = reelsManager.updateFavoriteById(req.params.id, req.body || {});
        if (!favorite) return res.status(404).json({ success: false, error: 'Không tìm thấy kênh yêu thích.' });
        res.json({ success: true, favorite, ...reelsManager.getFavoritesState() });
    });

    router.delete('/favorites/:id', (req, res) => {
        const removed = reelsManager.removeFavoriteById(req.params.id);
        if (!removed) return res.status(404).json({ success: false, error: 'Không tìm thấy kênh yêu thích cần xóa.' });
        res.json({ success: true, ...reelsManager.getFavoritesState() });
    });

    router.post('/favorites/:id/scan', (req, res) => {
        try {
            const job = reelsManager.triggerFavoriteScan(req.params.id, 'FAVORITE_MANUAL');
            const favorite = reelsManager.getFavoritesState().favorites.find((f) => f.id === req.params.id);
            res.status(202).json({ success: true, favorite, jobId: job.jobId, job });
        } catch (err) {
            res.status(400).json({ success: false, error: err.message });
        }
    });

    router.get('/normalize', (req, res) => {
        try {
            const rawUrl = String(req.query?.url || req.body?.url || '').trim();
            const normalized = normalizeFacebookInput(rawUrl);
            res.json({ success: true, data: normalized });
        } catch (err) {
            res.status(400).json({ success: false, error: err.message });
        }
    });

    router.get('/library', (_req, res) => {
        res.json({ success: true, ...reelsManager.listLibraryPages() });
    });

    router.get('/file/:pageSlug/:fileName', (req, res) => {
        const safeSlug = path.basename(req.params.pageSlug || '');
        const safeFile = path.basename(req.params.fileName || '');
        if (!safeSlug || !safeFile || !safeFile.toLowerCase().endsWith('.mp4')) {
            return res.status(400).json({ success: false, error: 'Tên tệp không hợp lệ.' });
        }
        const targetPath = path.join(videoLibraryDir, safeSlug, safeFile);
        if (!fs.existsSync(targetPath)) {
            return res.status(404).json({ success: false, error: 'Không tìm thấy tệp video.' });
        }
        return res.sendFile(targetPath);
    });

    router.get('/session/status', (_req, res) => {
        res.json({ success: true, session: reelsManager.getSessionStatus() });
    });

    return router;
}

module.exports = {
    reelsManager,
    createReelsRouter,
    normalizeFacebookInput,
    parseMetaEfg,
    parseDashManifestXml,
    walkRelayObject,
    harvestPayloadText,
    walkInstagramObject,
    harvestInstagramText,
    stripByteRangeParams,
    extractCaptionTitleAndHashtags,
    buildVideoFileName
};
