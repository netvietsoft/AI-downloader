(() => {
  const form = document.getElementById('reels-download-form');
  const urlInput = document.getElementById('page-url-input');
  const btnDownload = document.getElementById('btn-auto-download');
  const btnAddFavorite = document.getElementById('btn-add-favorite');
  const btnStop = document.getElementById('btn-stop-job');
  const btnRefreshLib = document.getElementById('btn-refresh-library');
  const optQuality = document.getElementById('opt-quality');
  const optMaxReels = document.getElementById('opt-max-reels');
  const optOutputDir = document.getElementById('opt-output-dir');
  const optIncremental = document.getElementById('opt-incremental');
  const optForce = document.getElementById('opt-force');
  const normalizedPreview = document.getElementById('normalized-preview');

  // Navigation Tabs
  const navTabs = document.querySelectorAll('.nav-tab[data-tab-target]');
  const tabPanels = document.querySelectorAll('.tab-panel');

  // Favorites & Scheduler elements
  const favoritesCount = document.getElementById('favorites-count');
  const libraryCount = document.getElementById('library-count');
  const favoritesTbody = document.getElementById('favorites-tbody');
  const schedulerIntervalSelect = document.getElementById('scheduler-interval-select');
  const btnToggleScheduler = document.getElementById('btn-toggle-scheduler');
  const btnScanAllFavorites = document.getElementById('btn-scan-all-favorites');
  const schedulerStatusText = document.getElementById('scheduler-status-text');

  // Job Progress elements
  const jobTitle = document.getElementById('job-title');
  const jobMessage = document.getElementById('job-message');
  const jobBadge = document.getElementById('job-status-badge');
  const progressBar = document.getElementById('job-progress-bar');
  const statDiscovered = document.getElementById('stat-discovered');
  const statDownloaded = document.getElementById('stat-downloaded');
  const statSkipped = document.getElementById('stat-skipped');
  const statFailed = document.getElementById('stat-failed');
  const pathsBox = document.getElementById('job-paths-box');
  const outDirEl = document.getElementById('job-output-dir');
  const manifestEl = document.getElementById('job-manifest-path');
  const jobLogs = document.getElementById('job-logs');
  const agentJson = document.getElementById('agent-job-json');

  const itemsSection = document.getElementById('job-items-section');
  const itemsCount = document.getElementById('items-count');
  const itemsTbody = document.getElementById('reels-items-tbody');

  const libRootLabel = document.getElementById('library-root-label');
  const libList = document.getElementById('library-pages-list');

  const previewModal = document.getElementById('preview-modal');
  const previewVideo = document.getElementById('preview-video');
  const previewTitle = document.getElementById('preview-title');
  const btnClosePreview = document.getElementById('btn-close-preview');

  let activeJobId = null;
  let pollTimer = null;
  let currentSchedulerEnabled = true;
  let showAllJobItems = false;

  function switchMenuTab(targetId) {
    navTabs.forEach((btn) => {
      const isMatch = btn.getAttribute('data-tab-target') === targetId;
      btn.classList.toggle('active', isMatch);
      btn.setAttribute('aria-selected', isMatch ? 'true' : 'false');
    });

    if (targetId === 'panel-all') {
      tabPanels.forEach((panel) => panel.classList.add('active'));
      return;
    }

    tabPanels.forEach((panel) => {
      panel.classList.toggle('active', panel.id === targetId);
    });
  }

  navTabs.forEach((btn) => {
    btn.addEventListener('click', () => {
      const targetId = btn.getAttribute('data-tab-target');
      if (targetId) switchMenuTab(targetId);
    });
  });

  function escapeHtml(str) {
    return String(str || '')
      .replace(/[\u{1F000}-\u{1FFFF}\u{2600}-\u{27BF}]/gu, '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  function formatBytes(bytes) {
    if (!bytes || bytes <= 0) return '-';
    if (bytes >= 1048576) return (bytes / 1048576).toFixed(2) + ' MB';
    return Math.round(bytes / 1024) + ' KB';
  }

  function formatDateTime(isoStr) {
    if (!isoStr) return 'Chưa quét';
    try {
      const d = new Date(isoStr);
      return d.toLocaleString('vi-VN', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        day: '2-digit',
        month: '2-digit',
      });
    } catch {
      return isoStr;
    }
  }

  function formatPlatformLabel(platform) {
    const p = String(platform || 'facebook').toLowerCase();
    if (p === 'instagram') return 'Instagram';
    if (p === 'youtube') return 'YouTube';
    return 'Facebook';
  }

  function statusClass(status) {
    if (status === 'COMPLETED') return 'status-completed';
    if (status === 'SCANNING' || status === 'DOWNLOADING' || status === 'QUEUED' || status === 'RUNNING') {
      return 'status-running';
    }
    if (status === 'PARTIAL' || status === 'NEEDS_LOGIN' || status === 'BLOCKED_CAPTCHA' || status === 'CANCELLED') {
      return 'status-warning';
    }
    if (status === 'FAILED') return 'status-failed';
    return 'status-idle';
  }

  function isTerminalStatus(status) {
    return (
      status === 'COMPLETED' ||
      status === 'PARTIAL' ||
      status === 'NEEDS_LOGIN' ||
      status === 'BLOCKED_CAPTCHA' ||
      status === 'CANCELLED' ||
      status === 'FAILED'
    );
  }

  function renderJobState(job) {
    if (!job) return;
    document.body.setAttribute('data-job-status', job.status);
    window.__REELS_JOB_STATE__ = job;

    const platformTag = job.platform ? `[${formatPlatformLabel(job.platform)}] ` : '';
    jobTitle.textContent = job.pageTitle
      ? `${platformTag}Kênh: ${job.pageTitle} (${job.pageSlug})`
      : `${platformTag}Tiến trình: ${job.jobId}`;
    jobMessage.textContent = job.message || '';

    jobBadge.textContent = job.status;
    jobBadge.className = 'status-text ' + statusClass(job.status);

    const pct = job.status === 'SCANNING' ? 15 : Number(job.progressPercent || 0);
    progressBar.style.width = `${Math.max(0, Math.min(100, pct))}%`;

    statDiscovered.textContent = String(job.totalDiscovered || 0);
    statDownloaded.textContent = String(job.downloadedCount || 0);
    statSkipped.textContent = String(job.skippedCount || 0);
    statFailed.textContent = String(job.failedCount || 0);

    if (job.outputDir) {
      pathsBox.style.display = 'flex';
      outDirEl.textContent = job.outputDir;
      manifestEl.textContent = job.manifestPath || '';
    }

    if (Array.isArray(job.logs) && job.logs.length > 0) {
      // Hiển thị 12 dòng nhật ký gần nhất để không tạo thanh cuộn dọc
      jobLogs.textContent = job.logs.slice(-12).join('\n');
    }

    const compactAgentPayload = {
      jobId: job.jobId,
      platform: job.platform || 'facebook',
      triggerSource: job.triggeredBy || job.triggerSource || 'MANUAL',
      status: job.status,
      pageSlug: job.pageSlug,
      pageTitle: job.pageTitle,
      reelsTabUrl: job.reelsTabUrl,
      outputDir: job.outputDir,
      manifestPath: job.manifestPath,
      totalDiscovered: job.totalDiscovered,
      downloadedCount: job.downloadedCount,
      skippedCount: job.skippedCount,
      failedCount: job.failedCount,
      progressPercent: job.progressPercent,
      hasReachedEndOfPage: job.hasReachedEndOfPage,
      loginRecommended: job.loginRecommended,
      message: job.message,
    };
    agentJson.textContent = JSON.stringify(compactAgentPayload, null, 2);

    const running = !isTerminalStatus(job.status);
    btnDownload.disabled = running;
    btnStop.style.display = running ? 'inline-flex' : 'none';

    if (Array.isArray(job.items) && job.items.length > 0) {
      itemsSection.style.display = 'block';
      itemsCount.textContent = String(job.items.length);
      const visibleItems = showAllJobItems ? job.items : job.items.slice(0, 12);
      const rowsHtml = visibleItems
        .map((it) => {
          const dur = it.durationSec > 0 ? `${it.durationSec} giây` : '-';
          const views = it.viewCountText ? `${it.viewCountText} - ` : '';
          const audioTag =
            it.status === 'COMPLETED' || it.status === 'SKIPPED_EXISTING'
              ? it.hasAudio
                ? ' - Có âm thanh'
                : ' - Không âm thanh'
              : '';
          const canPlay = it.status === 'COMPLETED' || it.status === 'SKIPPED_EXISTING';
          const playBtn = canPlay
            ? `<button type="button" class="btn btn-secondary btn-xs btn-preview-reel" data-slug="${encodeURIComponent(
                job.pageSlug
              )}" data-file="${encodeURIComponent(it.fileName)}" data-id="${it.reelId}">Xem video</button>`
            : `<a href="${it.reelUrl}" target="_blank" rel="noopener" class="text-link">Liên kết gốc</a>`;

          const displayTitle = escapeHtml(it.title || it.caption || `Video ${it.reelId}`);
          const hashtagsList = Array.isArray(it.hashtags) && it.hashtags.length > 0 ? escapeHtml(it.hashtags.join(' ')) : '';
          const displayFileName = escapeHtml(it.fileName || `${it.reelId}.mp4`);
          const shortErr = it.error ? escapeHtml(String(it.error).slice(0, 80)) : '';

          return `<tr>
            <td>${it.index}</td>
            <td>
              <div><strong>${displayTitle}</strong></div>
              ${hashtagsList ? `<div class="muted">Hashtag: ${hashtagsList}</div>` : ''}
              <div class="mono-text">Tên tệp: ${displayFileName}</div>
            </td>
            <td>${views}${dur}</td>
            <td>${it.qualityLabel || '-'}${audioTag}</td>
            <td>${formatBytes(it.sizeBytes)}</td>
            <td><span class="status-text ${statusClass(it.status)}">${it.status}</span>${
              shortErr ? `<div class="muted">${shortErr}</div>` : ''
            }</td>
            <td>${playBtn}</td>
          </tr>`;
        })
        .join('');

      const toggleRowHtml =
        job.items.length > 12
          ? `<tr>
              <td colspan="7">
                <button type="button" class="btn btn-secondary btn-sm" id="btn-toggle-all-items">
                  ${showAllJobItems ? 'Thu gọn danh sách (hiển thị 12 video đầu)' : `Xem toàn bộ ${job.items.length} video trong phiên quét`}
                </button>
              </td>
            </tr>`
          : '';

      itemsTbody.innerHTML = rowsHtml + toggleRowHtml;
    }
  }

  async function pollActiveJob() {
    if (!activeJobId) return;
    try {
      const res = await fetch(`/api/v1/reels/jobs/${encodeURIComponent(activeJobId)}`);
      const data = await res.json();
      if (data.success && data.job) {
        renderJobState(data.job);
        if (isTerminalStatus(data.job.status)) {
          clearInterval(pollTimer);
          pollTimer = null;
          loadLibrary();
          loadFavorites();
        }
      }
    } catch {}
  }

  function startJobPolling(job) {
    if (!job || !job.jobId) return;
    activeJobId = job.jobId;
    renderJobState(job);
    if (pollTimer) clearInterval(pollTimer);
    pollTimer = setInterval(pollActiveJob, 1200);
  }

  async function triggerDownload(customUrl) {
    const rawUrl = (customUrl ?? urlInput.value).trim();
    if (!rawUrl) {
      urlInput.focus();
      return;
    }

    btnDownload.disabled = true;
    document.body.setAttribute('data-job-status', 'STARTING');

    try {
      const payload = {
        url: rawUrl,
        preferQuality: optQuality.value,
        maxReels: Number(optMaxReels.value || 0),
        outputDir: optOutputDir.value.trim() || undefined,
        incrementalOnly: optIncremental ? optIncremental.checked : false,
        forceRedownload: optForce.checked,
      };

      const res = await fetch('/api/v1/reels/auto-download', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });
      const data = await res.json();
      if (!data.success) {
        alert(data.error || 'Không thể bắt đầu tải kênh.');
        btnDownload.disabled = false;
        document.body.setAttribute('data-job-status', 'FAILED');
        return;
      }

      startJobPolling(data.job);
    } catch (err) {
      btnDownload.disabled = false;
      document.body.setAttribute('data-job-status', 'FAILED');
      alert('Lỗi kết nối máy chủ local: ' + (err.message || err));
    }
  }

  // ============================================================================
  // FAVORITES / WATCHLIST MANAGEMENT
  // ============================================================================

  function renderFavoritesState(data) {
    if (!data) return;
    window.__REELS_FAVORITES_STATE__ = data;
    currentSchedulerEnabled = Boolean(data.schedulerEnabled);

    if (schedulerIntervalSelect && data.defaultIntervalMinutes) {
      const valStr = String(data.defaultIntervalMinutes);
      if ([...schedulerIntervalSelect.options].some((o) => o.value === valStr)) {
        schedulerIntervalSelect.value = valStr;
      }
    }

    if (btnToggleScheduler) {
      btnToggleScheduler.textContent = currentSchedulerEnabled
        ? 'Tự động quét: Đang bật'
        : 'Tự động quét: Đang tắt';
    }

    if (schedulerStatusText) {
      schedulerStatusText.textContent = currentSchedulerEnabled
        ? 'Trình quét tự động đang hoạt động. Cứ đến lịch hoặc có video mới trên Page/Kênh yêu thích là lập tức tải về ổ cứng.'
        : 'Trình quét tự động đang tạm dừng. Bạn vẫn có thể bấm "Quét và tải mới" trên từng kênh bất kỳ lúc nào.';
    }

    const list = Array.isArray(data.favorites) ? data.favorites : [];
    if (favoritesCount) {
      favoritesCount.textContent = String(list.length);
    }

    if (list.length === 0) {
      favoritesTbody.innerHTML = `<tr><td colspan="7" class="muted">Chưa có Page/Kênh yêu thích nào. Nhập liên kết ở mục Tải Kênh rồi bấm "Lưu vào kênh yêu thích".</td></tr>`;
      return;
    }

    favoritesTbody.innerHTML = list
      .map((fav) => {
        const platformLabel = formatPlatformLabel(fav.platform);
        const isEnabled = fav.enabled !== undefined ? Boolean(fav.enabled) : Boolean(fav.autoScanEnabled);
        const newTag =
          fav.lastNewVideosCount > 0
            ? `<span class="status-text status-completed"> (+${fav.lastNewVideosCount} mới)</span>`
            : `<span class="muted"> (0 mới)</span>`;
        const autoTag = isEnabled
          ? `<span class="status-text status-completed">Bật (${fav.checkIntervalMinutes} phút)</span>`
          : `<span class="muted">Tắt</span>`;

        return `<tr data-fav-id="${fav.id}">
          <td><span class="platform-text">${platformLabel}</span></td>
          <td>
            <div><strong>${fav.title || fav.pageSlug}</strong> <span class="muted">(${fav.pageSlug})</span></div>
            <div><a href="${fav.reelsTabUrl}" target="_blank" rel="noopener" class="text-link">${fav.reelsTabUrl}</a></div>
          </td>
          <td>
            <strong>${fav.totalDownloadedInLibrary || 0}</strong> video MP4${newTag}
          </td>
          <td>${formatDateTime(fav.lastCheckedAt)}</td>
          <td>
            <div>${autoTag}</div>
            <div class="muted">${isEnabled ? formatDateTime(fav.nextCheckAt) : '-'}</div>
          </td>
          <td>
            <span class="status-text ${statusClass(fav.lastStatus)}">${fav.lastStatus || 'IDLE'}</span>
          </td>
          <td>
            <div class="fav-actions">
              <button type="button" class="btn btn-primary btn-xs btn-fav-scan" data-id="${fav.id}" data-testid="btn-fav-scan-${fav.pageSlug}">
                Quét và tải mới
              </button>
              <button type="button" class="btn btn-secondary btn-xs btn-fav-toggle" data-id="${fav.id}" data-enabled="${isEnabled ? '1' : '0'}">
                ${isEnabled ? 'Tắt tự quét' : 'Bật tự quét'}
              </button>
              <button type="button" class="btn btn-secondary btn-xs btn-fav-delete" data-id="${fav.id}">
                Xóa
              </button>
            </div>
          </td>
        </tr>`;
      })
      .join('');
  }

  async function loadFavorites() {
    try {
      const res = await fetch('/api/v1/reels/favorites');
      const data = await res.json();
      if (data.success) {
        renderFavoritesState(data);
      }
    } catch {}
  }

  async function handleAddFavorite() {
    const rawUrl = urlInput.value.trim();
    if (!rawUrl) {
      urlInput.focus();
      alert('Vui lòng nhập liên kết Facebook Page, Kênh Instagram hoặc Kênh YouTube trước khi lưu Yêu Thích.');
      return;
    }
    btnAddFavorite.disabled = true;
    try {
      const intervalMinutes = Number(schedulerIntervalSelect?.value || 1440);
      const res = await fetch('/api/v1/reels/favorites', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          url: rawUrl,
          preferQuality: optQuality.value,
          checkIntervalMinutes: intervalMinutes,
          enabled: true,
        }),
      });
      const data = await res.json();
      if (!data.success) {
        alert(data.error || 'Không thể lưu kênh yêu thích.');
        return;
      }
      renderFavoritesState(data);
      switchMenuTab('panel-favorites');
    } catch (err) {
      alert('Lỗi khi lưu kênh yêu thích: ' + (err.message || err));
    } finally {
      btnAddFavorite.disabled = false;
    }
  }

  async function loadLibrary() {
    try {
      const res = await fetch('/api/v1/reels/library');
      const data = await res.json();
      if (!data.success) return;
      libRootLabel.textContent = `Thư mục gốc: ${data.libraryRoot}`;
      if (!optOutputDir.value && data.libraryRoot) {
        optOutputDir.placeholder = data.libraryRoot;
      }
      const pages = Array.isArray(data.pages) ? data.pages : [];
      if (libraryCount) {
        libraryCount.textContent = String(pages.length);
      }
      if (pages.length === 0) {
        libList.innerHTML = `<div class="muted">Chưa có kênh nào được tải trong thư viện.</div>`;
        return;
      }
      libList.innerHTML = pages
        .map((p) => {
          const sampleFiles = Array.isArray(p.reels)
            ? p.reels
                .slice(0, 3)
                .map((r) => `<div class="mono-text">- ${escapeHtml(r.fileName)}</div>`)
                .join('')
            : '';
          return `<div class="lib-card">
            <div class="lib-card-header">
              <h4>${escapeHtml(p.pageTitle)} <span class="muted">(${escapeHtml(p.pageSlug)})</span></h4>
              <span class="platform-text">${formatPlatformLabel(p.platform || 'facebook')}</span>
            </div>
            <div><strong>${p.videoCount}</strong> video MP4 - Tổng dung lượng: <strong>${formatBytes(p.totalBytes)}</strong></div>
            <div class="mono-text">Thư mục: ${escapeHtml(p.folderPath)}</div>
            ${sampleFiles ? `<div class="muted">Tên tệp MP4 mới nhất (đã gắn Tiêu đề &amp; Hashtag):</div>${sampleFiles}` : ''}
          </div>`;
        })
        .join('');
    } catch {}
  }

  async function loadInitialJobsAndSession() {
    try {
      const res = await fetch('/api/v1/reels/jobs');
      const data = await res.json();
      if (data.success && Array.isArray(data.jobs) && data.jobs.length > 0) {
        const latest = data.jobs[0];
        activeJobId = latest.jobId;
        renderJobState(latest);
        if (!isTerminalStatus(latest.status)) {
          if (pollTimer) clearInterval(pollTimer);
          pollTimer = setInterval(pollActiveJob, 1200);
        }
      }
    } catch {}
  }

  // Event listeners
  form.addEventListener('submit', (e) => {
    e.preventDefault();
    triggerDownload();
  });

  if (btnAddFavorite) {
    btnAddFavorite.addEventListener('click', handleAddFavorite);
  }

  if (btnToggleScheduler) {
    btnToggleScheduler.addEventListener('click', async () => {
      try {
        const res = await fetch('/api/v1/reels/favorites/scheduler', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            schedulerEnabled: !currentSchedulerEnabled,
          }),
        });
        const data = await res.json();
        if (data.success) renderFavoritesState(data);
      } catch {}
    });
  }

  if (schedulerIntervalSelect) {
    schedulerIntervalSelect.addEventListener('change', async () => {
      try {
        const mins = Number(schedulerIntervalSelect.value || 1440);
        const res = await fetch('/api/v1/reels/favorites/scheduler', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            defaultIntervalMinutes: mins,
          }),
        });
        const data = await res.json();
        if (data.success) renderFavoritesState(data);
      } catch {}
    });
  }

  if (btnScanAllFavorites) {
    btnScanAllFavorites.addEventListener('click', async () => {
      btnScanAllFavorites.disabled = true;
      try {
        const res = await fetch('/api/v1/reels/favorites/scan-all', { method: 'POST' });
        const data = await res.json();
        if (data.success) {
          renderFavoritesState(data);
          if (Array.isArray(data.jobs) && data.jobs.length > 0) {
            startJobPolling(data.jobs[0]);
            switchMenuTab('panel-downloader');
          }
        }
      } catch (e) {
        alert('Lỗi quét tất cả kênh yêu thích: ' + e.message);
      } finally {
        btnScanAllFavorites.disabled = false;
      }
    });
  }

  if (favoritesTbody) {
    favoritesTbody.addEventListener('click', async (e) => {
      const scanBtn = e.target.closest('.btn-fav-scan');
      if (scanBtn) {
        const id = scanBtn.getAttribute('data-id');
        scanBtn.disabled = true;
        try {
          const res = await fetch(`/api/v1/reels/favorites/${encodeURIComponent(id)}/scan`, {
            method: 'POST',
          });
          const data = await res.json();
          if (data.success && data.job) {
            startJobPolling(data.job);
            loadFavorites();
            switchMenuTab('panel-downloader');
          } else {
            alert(data.error || 'Không thể quét kênh yêu thích.');
          }
        } catch (err) {
          alert('Lỗi: ' + err.message);
        } finally {
          scanBtn.disabled = false;
        }
        return;
      }

      const toggleBtn = e.target.closest('.btn-fav-toggle');
      if (toggleBtn) {
        const id = toggleBtn.getAttribute('data-id');
        const currentlyEnabled = toggleBtn.getAttribute('data-enabled') === '1';
        try {
          const res = await fetch(`/api/v1/reels/favorites/${encodeURIComponent(id)}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ enabled: !currentlyEnabled }),
          });
          const data = await res.json();
          if (data.success) renderFavoritesState(data);
        } catch {}
        return;
      }

      const delBtn = e.target.closest('.btn-fav-delete');
      if (delBtn) {
        const id = delBtn.getAttribute('data-id');
        try {
          const res = await fetch(`/api/v1/reels/favorites/${encodeURIComponent(id)}`, {
            method: 'DELETE',
          });
          const data = await res.json();
          if (data.success) renderFavoritesState(data);
        } catch {}
      }
    });
  }

  document.querySelectorAll('[data-sample-url]').forEach((el) => {
    el.addEventListener('click', () => {
      const u = el.getAttribute('data-sample-url') || '';
      urlInput.value = u;
      previewNormalize(u);
    });
  });

  let normTimer = null;
  async function previewNormalize(val) {
    if (!val || val.trim().length < 4) {
      normalizedPreview.textContent = '';
      return;
    }
    try {
      const res = await fetch(`/api/v1/reels/normalize?url=${encodeURIComponent(val.trim())}`);
      const d = await res.json();
      if (d.success && d.data) {
        const plat = formatPlatformLabel(d.data.platform || 'facebook');
        normalizedPreview.textContent = `Đích quét (${plat}): ${d.data.reelsTabUrl}`;
      } else {
        normalizedPreview.textContent = '';
      }
    } catch {}
  }

  urlInput.addEventListener('input', () => {
    clearTimeout(normTimer);
    normTimer = setTimeout(() => previewNormalize(urlInput.value), 250);
  });

  btnStop.addEventListener('click', async () => {
    if (!activeJobId) return;
    await fetch(`/api/v1/reels/jobs/${encodeURIComponent(activeJobId)}/cancel`, { method: 'POST' });
    pollActiveJob();
  });

  document.querySelectorAll('.btn-login-platform').forEach((btn) => {
    btn.addEventListener('click', async () => {
      const platform = btn.getAttribute('data-platform') || 'facebook';
      try {
        const res = await fetch('/api/v1/reels/session/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ platform }),
        });
        const data = await res.json();
        alert(data.message || data.error || 'Đã gửi yêu cầu mở trình duyệt.');
      } catch (e) {
        alert('Không thể mở trình duyệt: ' + e.message);
      }
    });
  });

  btnRefreshLib.addEventListener('click', () => {
    loadLibrary();
    loadFavorites();
  });

  itemsTbody.addEventListener('click', (e) => {
    const toggleAllBtn = e.target.closest('#btn-toggle-all-items');
    if (toggleAllBtn) {
      showAllJobItems = !showAllJobItems;
      if (window.__REELS_JOB_STATE__) {
        renderJobState(window.__REELS_JOB_STATE__);
      }
      return;
    }
    const btn = e.target.closest('.btn-preview-reel');
    if (!btn) return;
    const slug = btn.getAttribute('data-slug');
    const file = btn.getAttribute('data-file');
    const rid = btn.getAttribute('data-id');
    previewTitle.textContent = `Video #${rid}`;
    previewVideo.src = `/api/v1/reels/file/${slug}/${file}`;
    previewModal.style.display = 'flex';
    previewVideo.play().catch(() => {});
  });

  btnClosePreview.addEventListener('click', () => {
    previewVideo.pause();
    previewVideo.removeAttribute('src');
    previewModal.style.display = 'none';
  });

  previewModal.addEventListener('click', (e) => {
    if (e.target === previewModal) {
      btnClosePreview.click();
    }
  });

  // Periodic refresh of favorites status in background
  setInterval(loadFavorites, 15000);

  // Initialize & support URL auto-trigger for AI Agents: ?url=...&auto=1
  (async () => {
    await Promise.all([loadLibrary(), loadFavorites()]);
    const params = new URLSearchParams(window.location.search);
    const queryUrl = params.get('url') || params.get('pageUrl');
    const queryMax = params.get('maxReels') || params.get('max');
    const queryQuality = params.get('quality');
    const queryTab = params.get('tab');
    const autoStart = params.get('auto') === '1' || params.get('autorun') === '1' || params.get('auto') === 'true';

    if (queryTab) {
      switchMenuTab(`panel-${queryTab}`);
    }
    if (queryMax && !Number.isNaN(Number(queryMax))) {
      optMaxReels.value = String(Number(queryMax));
    }
    if (queryQuality && ['1080p', 'hd', 'sd'].includes(queryQuality.toLowerCase())) {
      optQuality.value = queryQuality.toLowerCase();
    }
    if (queryUrl) {
      urlInput.value = queryUrl;
      await previewNormalize(queryUrl);
      if (autoStart) {
        triggerDownload(queryUrl);
        return;
      }
    }
    await loadInitialJobsAndSession();
  })();
})();
