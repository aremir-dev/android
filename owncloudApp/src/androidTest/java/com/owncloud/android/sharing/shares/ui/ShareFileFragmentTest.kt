/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2019 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.sharing.shares.ui

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.owncloud.android.R
import com.owncloud.android.domain.capabilities.model.CapabilityBooleanType
import com.owncloud.android.domain.capabilities.model.OCCapability
import com.owncloud.android.domain.sharing.shares.model.OCShare
import com.owncloud.android.domain.sharing.shares.model.ShareType
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.presentation.ui.sharing.fragments.ShareFileFragment
import com.owncloud.android.presentation.viewmodels.capabilities.OCCapabilityViewModel
import com.owncloud.android.presentation.viewmodels.sharing.OCShareViewModel
import com.owncloud.android.utils.AppTestUtil.DUMMY_ACCOUNT
import com.owncloud.android.utils.AppTestUtil.DUMMY_CAPABILITY
import com.owncloud.android.utils.AppTestUtil.DUMMY_FILE
import com.owncloud.android.utils.AppTestUtil.DUMMY_SHARE
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Test
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class ShareFileFragmentTest {
    private val ocCapabilityViewModel = mockk<OCCapabilityViewModel>(relaxed = true)
    private val capabilitiesLiveData = MutableLiveData<UIResult<OCCapability>>()
    private val ocShareViewModel = mockk<OCShareViewModel>(relaxed = true)
    private val sharesLiveData = MutableLiveData<UIResult<List<OCShare>>>()

    @Before
    fun setUp() {
        every { ocCapabilityViewModel.capabilities } returns capabilitiesLiveData
        every { ocShareViewModel.shares } returns sharesLiveData

        stopKoin()

        startKoin {
            androidContext(ApplicationProvider.getApplicationContext<Context>())
            modules(
                module(override = true) {
                    viewModel {
                        ocCapabilityViewModel
                    }
                    viewModel {
                        ocShareViewModel
                    }
                }
            )
        }
    }

    @Test
    fun showHeader() {
        loadShareFileFragment()
        onView(withId(R.id.shareFileName)).check(matches(withText("img.png")))
    }

    @Test
    fun fileSizeVisible() {
        loadShareFileFragment()
        onView(withId(R.id.shareFileSize)).check(matches(isDisplayed()))
    }

    @Test
    fun showPrivateLink() {
        loadShareFileFragment()
        onView(withId(R.id.getPrivateLinkButton)).check(matches(isDisplayed()))
    }

    /******************************************************************************************************
     ******************************************* PRIVATE SHARES *******************************************
     ******************************************************************************************************/

    private var userSharesList = listOf(
        DUMMY_SHARE.copy(
            sharedWithDisplayName = "Batman"
        ),
        DUMMY_SHARE.copy(
            sharedWithDisplayName = "Jocker"
        )
    )

    private var groupSharesList = listOf(
        DUMMY_SHARE.copy(
            shareType = ShareType.GROUP,
            sharedWithDisplayName = "Suicide Squad"
        ),
        DUMMY_SHARE.copy(
            shareType = ShareType.GROUP,
            sharedWithDisplayName = "Avengers"
        )
    )

    @Test
    fun showUsersAndGroupsSectionTitle() {
        loadShareFileFragment(shares = userSharesList)
        onView(withText(R.string.share_with_user_section_title)).check(matches(isDisplayed()))
    }

    @Test
    fun showNoPrivateShares() {
        loadShareFileFragment(shares = listOf())
        onView(withText(R.string.share_no_users)).check(matches(isDisplayed()))
    }

    @Test
    fun showUserShares() {
        loadShareFileFragment(shares = userSharesList)
        onView(withText("Batman")).check(matches(isDisplayed()))
        onView(withText("Batman")).check(matches(hasSibling(withId(R.id.unshareButton))))
            .check(matches(isDisplayed()))
        onView(withText("Batman")).check(matches(hasSibling(withId(R.id.editShareButton))))
            .check(matches(isDisplayed()))
        onView(withText("Jocker")).check(matches(isDisplayed()))
    }

    @Test
    fun showGroupShares() {
        loadShareFileFragment(shares = listOf(groupSharesList.first()))
        onView(withText("Suicide Squad (group)")).check(matches(isDisplayed()))
        onView(withText("Suicide Squad (group)")).check(matches(hasSibling(withId(R.id.icon))))
            .check(matches(isDisplayed()))
        onView(withTagValue(CoreMatchers.equalTo(R.drawable.ic_group))).check(matches(isDisplayed()))
    }

    /******************************************************************************************************
     ******************************************* PUBLIC SHARES ********************************************
     ******************************************************************************************************/

    private var publicShareList = listOf(
        DUMMY_SHARE.copy(
            shareType = ShareType.PUBLIC_LINK,
            path = "/Photos/image.jpg",
            isFolder = false,
            name = "Image link",
            shareLink = "http://server:port/s/1"
        ),
        DUMMY_SHARE.copy(
            shareType = ShareType.PUBLIC_LINK,
            path = "/Photos/image.jpg",
            isFolder = false,
            name = "Image link 2",
            shareLink = "http://server:port/s/2"
        ),
        DUMMY_SHARE.copy(
            shareType = ShareType.PUBLIC_LINK,
            path = "/Photos/image.jpg",
            isFolder = false,
            name = "Image link 3",
            shareLink = "http://server:port/s/3"
        )
    )

    @Test
    fun showNoPublicShares() {
        loadShareFileFragment(shares = listOf())
        onView(withText(R.string.share_no_public_links)).check(matches(isDisplayed()))
    }

    @Test
    fun showPublicShares() {
        loadShareFileFragment(shares = publicShareList)
        onView(withText("Image link")).check(matches(isDisplayed()))
        onView(withText("Image link")).check(matches(hasSibling(withId(R.id.getPublicLinkButton))))
            .check(matches(isDisplayed()))
        onView(withText("Image link")).check(matches(hasSibling(withId(R.id.deletePublicLinkButton))))
            .check(matches(isDisplayed()))
        onView(withText("Image link")).check(matches(hasSibling(withId(R.id.editPublicLinkButton))))
            .check(matches(isDisplayed()))
        onView(withText("Image link 2")).check(matches(isDisplayed()))
        onView(withText("Image link 3")).check(matches(isDisplayed()))
    }

    @Test
    fun showPublicSharesSharingEnabled() {
        loadShareFileFragment(
            capabilities = DUMMY_CAPABILITY.copy(filesSharingPublicEnabled = CapabilityBooleanType.TRUE),
            shares = publicShareList
        )

        onView(withText("Image link")).check(matches(isDisplayed()))
        onView(withText("Image link 2")).check(matches(isDisplayed()))
        onView(withText("Image link 3")).check(matches(isDisplayed()))
    }

    @Test
    fun hidePublicSharesSharingDisabled() {
        loadShareFileFragment(
            capabilities = DUMMY_CAPABILITY.copy(filesSharingPublicEnabled = CapabilityBooleanType.FALSE),
            shares = publicShareList
        )

        onView(withId(R.id.shareViaLinkSection))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
    }

    @Test
    fun createPublicShareMultipleCapability() {
        loadShareFileFragment(
            capabilities = DUMMY_CAPABILITY.copy(
                versionString = "10.1.1",
                filesSharingPublicMultiple = CapabilityBooleanType.TRUE
            ),
            shares = listOf(publicShareList.get(0))
        )

        onView(withId(R.id.addPublicLinkButton))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    }

    @Test
    fun cannotCreatePublicShareMultipleCapability() {
        loadShareFileFragment(
            capabilities = DUMMY_CAPABILITY.copy(
                versionString = "10.1.1",
                filesSharingPublicMultiple = CapabilityBooleanType.FALSE
            ),
            shares = listOf(publicShareList.get(0))
        )

        onView(withId(R.id.addPublicLinkButton))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.INVISIBLE)))
    }

    @Test
    fun cannotCreatePublicShareServerCapability() {
        loadShareFileFragment(
            capabilities = DUMMY_CAPABILITY.copy(
                versionString = "9.3.1"
            ),
            shares = listOf(publicShareList.get(0))
        )

        onView(withId(R.id.addPublicLinkButton))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.INVISIBLE)))
    }

    /******************************************************************************************************
     *********************************************** COMMON ***********************************************
     ******************************************************************************************************/

    @Test
    fun hideSharesSharingApiDisabled() {
        loadShareFileFragment(
            capabilities = DUMMY_CAPABILITY.copy(
                filesSharingApiEnabled = CapabilityBooleanType.FALSE
            )
        )
        onView(withId(R.id.shareWithUsersSection))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))

        onView(withId(R.id.shareViaLinkSection))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
    }

    @Test
    fun showError() {
        loadShareFileFragment(
            sharesUIResult = UIResult.Error(
                error = Throwable("It was not possible to retrieve the shares from server")
            )
        )
        onView(withId(com.google.android.material.R.id.snackbar_text)).
            check(matches(withText(R.string.get_shares_error)))
    }

    private fun loadShareFileFragment(
        capabilities: OCCapability = DUMMY_CAPABILITY,
        capabilitiesUIResult: UIResult<OCCapability> = UIResult.Success(capabilities),
        shares: List<OCShare> = listOf(DUMMY_SHARE),
        sharesUIResult: UIResult<List<OCShare>> = UIResult.Success(shares)
    ) {
        val ownCloudVersion = mockkClass(OwnCloudVersion::class)

        every { ownCloudVersion.isSearchUsersSupported } returns true

        val shareFileFragment = ShareFileFragment.newInstance(
            DUMMY_FILE,
            DUMMY_ACCOUNT,
            ownCloudVersion
        )

        ActivityScenario.launch(TestShareFileActivity::class.java).onActivity {
            it.startFragment(shareFileFragment)
        }

        capabilitiesLiveData.postValue(capabilitiesUIResult)
        sharesLiveData.postValue(sharesUIResult)
    }
}
