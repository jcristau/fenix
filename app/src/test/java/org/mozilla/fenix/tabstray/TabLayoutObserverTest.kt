/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray

import com.google.android.material.tabs.TabLayout
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import mozilla.components.support.test.libstate.ext.waitUntilIdle
import mozilla.components.support.test.middleware.CaptureActionsMiddleware
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.metrics.MetricController

class TabLayoutObserverTest {
    private val interactor = mockk<TabsTrayInteractor>(relaxed = true)
    private lateinit var store: TabsTrayStore
    private lateinit var metrics: MetricController
    private val middleware = CaptureActionsMiddleware<TabsTrayState, TabsTrayAction>()

    @Before
    fun setup() {
        store = TabsTrayStore(middlewares = listOf(middleware))
        metrics = mockk(relaxed = true)
    }

    @Test
    fun `WHEN tab is selected THEN notify the interactor`() {
        val observer = TabLayoutObserver(interactor, store, metrics)
        val tab = mockk<TabLayout.Tab>()
        every { tab.position } returns 1

        observer.onTabSelected(tab)

        store.waitUntilIdle()

        verify { interactor.setCurrentTrayPosition(1, false) }
        verify { metrics.track(Event.TabsTrayPrivateModeTapped) }

        middleware.assertLastAction(TabsTrayAction.PageSelected::class) {
            assertTrue(it.page == Page.PrivateTabs)
        }

        every { tab.position } returns 0

        observer.onTabSelected(tab)

        store.waitUntilIdle()

        verify { interactor.setCurrentTrayPosition(0, true) }
        verify { metrics.track(Event.TabsTrayNormalModeTapped) }

        middleware.assertLastAction(TabsTrayAction.PageSelected::class) {
            assertTrue(it.page == Page.NormalTabs)
        }

        every { tab.position } returns 2

        observer.onTabSelected(tab)

        store.waitUntilIdle()

        verify { interactor.setCurrentTrayPosition(2, true) }
        verify { metrics.track(Event.TabsTraySyncedModeTapped) }

        middleware.assertLastAction(TabsTrayAction.PageSelected::class) {
            assertTrue(it.page == Page.SyncedTabs)
        }
    }

    @Test
    fun `WHEN observer is first started THEN do not smooth scroll`() {
        val store = TabsTrayStore()
        val observer = TabLayoutObserver(interactor, store, metrics)
        val tab = mockk<TabLayout.Tab>()
        every { tab.position } returns 1

        observer.onTabSelected(tab)

        verify { interactor.setCurrentTrayPosition(1, false) }
        verify { metrics.track(Event.TabsTrayPrivateModeTapped) }

        observer.onTabSelected(tab)

        verify { interactor.setCurrentTrayPosition(1, true) }
        verify { metrics.track(Event.TabsTrayPrivateModeTapped) }
    }
}
