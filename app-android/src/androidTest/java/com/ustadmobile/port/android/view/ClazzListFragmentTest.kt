package com.ustadmobile.port.android.view

import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import com.toughra.ustadmobile.R
import com.ustadmobile.adbscreenrecorder.client.AdbScreenRecord
import com.ustadmobile.lib.db.entities.Clazz
import com.ustadmobile.lib.db.entities.Person
import com.ustadmobile.port.android.screen.ClazzListScreen
import com.ustadmobile.test.core.impl.CrudIdlingResource
import com.ustadmobile.test.core.impl.DataBindingIdlingResource
import com.ustadmobile.test.port.android.util.installNavController
import com.ustadmobile.test.rules.*
import org.hamcrest.Matchers.equalTo
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

@AdbScreenRecord("Class list screen tests")
class ClazzListFragmentTest  {

    @JvmField
    @Rule
    var dbRule = UmAppDatabaseAndroidClientRule(useDbAsRepo = true)

    @JvmField
    @Rule
    var systemImplNavRule = SystemImplTestNavHostRule()


    @AdbScreenRecord("List screen should show class in database and allow clicking on item")
    @Test
    fun givenClazzPresent_whenClickOnClazz_thenShouldNavigateToClazzDetail() {
        val testEntity = Clazz().apply {
            clazzName = "Test Name"
            isClazzActive = true
            clazzUid = dbRule.db.clazzDao.insert(this)
        }

        dbRule.insertPersonForActiveUser(Person().apply {
            admin = true
            firstNames = "Test"
            lastName = "User"
        })

        val fragmentScenario = launchFragmentInContainer(
            bundleOf(), themeResId = R.style.UmTheme_App){
            ClazzListFragment().also {
                it.installNavController(systemImplNavRule.navController)
            } }

        fragmentScenario.onFragment {
            Navigation.setViewNavController(it.requireView(), systemImplNavRule.navController)
        }

        ClazzListScreen{

            recycler{

                childWith<ClazzListScreen.MainItem> {
                    withTag(testEntity.clazzUid)
                } perform {
                    click()
                }

            }

        }

        Assert.assertEquals("After clicking on item, it navigates to detail view",
            R.id.clazz_detail_dest, systemImplNavRule.navController.currentDestination?.id)
        val currentArgs = systemImplNavRule.navController.currentDestination?.arguments
        //Note: as of 02/June/2020 arguments were missing even though they were given
    }

}