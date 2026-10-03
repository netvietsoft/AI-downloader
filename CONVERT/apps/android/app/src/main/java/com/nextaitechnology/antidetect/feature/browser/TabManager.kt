package com.nextaitechnology.antidetect.feature.browser

import com.nextaitechnology.antidetect.core.model.TabModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Trình Quản Lý Đa Thẻ Trình Duyệt (Multi-Tab Manager)
 * Manages browser tabs state, creation, closure, and active switching
 *
 * @author NextAI Technology Core Team
 */
class TabManager {

    private val tabsList = mutableListOf<TabModel>()
    private var activeTabId: String = ""

    private val _tabsFlow = MutableStateFlow<List<TabModel>>(emptyList())
    val tabsFlow: StateFlow<List<TabModel>> = _tabsFlow.asStateFlow()

    private val _activeTabFlow = MutableStateFlow<TabModel?>(null)
    val activeTabFlow: StateFlow<TabModel?> = _activeTabFlow.asStateFlow()

    init {
        // Khởi tạo tab mặc định đầu tiên
        createNewTab("Home", "about:blank")
    }

    val tabsCount: Int get() = tabsList.size
    val activeTab: TabModel? get() = tabsList.find { it.id == activeTabId }
    fun getAllTabs(): List<TabModel> = tabsList.toList()
    fun getActiveTabId(): String = activeTabId

    fun createNewTab(title: String = "Home", url: String = "about:blank"): TabModel {
        val tabId = "TAB_${System.currentTimeMillis()}_${(100..999).random()}"
        val newTab = TabModel(
            id = tabId,
            title = title,
            currentUrl = url
        )
        tabsList.add(newTab)
        activeTabId = tabId
        emitUpdate()
        return newTab
    }

    fun closeTab(tabId: String): TabModel? {
        if (tabsList.size <= 1) {
            // Giữ lại tối thiểu 1 tab, chỉ reset url
            tabsList[0] = tabsList[0].copy(title = "Home", currentUrl = "about:blank")
            activeTabId = tabsList[0].id
            emitUpdate()
            return tabsList[0]
        }

        val idx = tabsList.indexOfFirst { it.id == tabId }
        if (idx != -1) {
            val wasActive = (activeTabId == tabId)
            tabsList.removeAt(idx)
            if (wasActive) {
                activeTabId = tabsList.getOrNull(idx)?.id ?: tabsList.last().id
            }
            emitUpdate()
        }
        return activeTab
    }

    fun closeAllTabs(): TabModel {
        tabsList.clear()
        return createNewTab("Home", "about:blank")
    }

    fun selectTab(tabId: String): TabModel? {
        val found = tabsList.find { it.id == tabId }
        if (found != null) {
            activeTabId = tabId
            emitUpdate()
        }
        return found
    }

    fun updateCurrentTabUrl(url: String, title: String) {
        val idx = tabsList.indexOfFirst { it.id == activeTabId }
        if (idx != -1) {
            val cleanTitle = if (title.isNotEmpty() && !title.startsWith("http")) title else tabsList[idx].title
            tabsList[idx] = tabsList[idx].copy(currentUrl = url, title = cleanTitle)
            emitUpdate()
        }
    }

    private fun emitUpdate() {
        _tabsFlow.value = tabsList.toList()
        _activeTabFlow.value = tabsList.find { it.id == activeTabId }
    }
}
