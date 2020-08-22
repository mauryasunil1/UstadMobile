package com.ustadmobile.port.android.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.os.bundleOf
import com.toughra.ustadmobile.R
import com.ustadmobile.core.account.UstadAccountManager
import com.ustadmobile.core.controller.ClazzListPresenter
import com.ustadmobile.core.controller.UstadListPresenter
import com.ustadmobile.core.db.UmAppDatabase.Companion.TAG_REPO
import com.ustadmobile.core.impl.UMAndroidUtil
import com.ustadmobile.core.util.MessageIdOption
import com.ustadmobile.core.view.*
import com.ustadmobile.lib.db.entities.Clazz
import com.ustadmobile.lib.db.entities.ClazzWithListDisplayDetails
import com.ustadmobile.port.android.view.ext.navigateToEditEntity
import com.ustadmobile.port.android.view.util.NewItemRecyclerViewAdapter
import org.kodein.di.direct
import org.kodein.di.instance
import org.kodein.di.on

class ClazzListFragment(): UstadListViewFragment<Clazz, ClazzWithListDisplayDetails>(),
        ClazzList2View, MessageIdSpinner.OnMessageIdOptionSelectedListener, View.OnClickListener,
        BottomSheetOptionSelectedListener{

    private var mPresenter: ClazzListPresenter? = null

    override val listPresenter: UstadListPresenter<*, in ClazzWithListDisplayDetails>?
        get() = mPresenter

    override var newClazzListOptionVisible: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        val accountManager: UstadAccountManager by instance()
        dbRepo = on(accountManager.activeAccount).direct.instance(tag = TAG_REPO)
        mPresenter = ClazzListPresenter(requireContext(), UMAndroidUtil.bundleToMap(arguments),
                this, di, viewLifecycleOwner)
        mNewItemRecyclerViewAdapter = NewItemRecyclerViewAdapter(this,
            requireContext().getString(R.string.add_a_new,
                    requireContext().getString(R.string.clazz)))
        mDataRecyclerViewAdapter = ClazzListRecyclerAdapter(mPresenter)

        return view
    }                                                                                                                                                                                                                                                                       

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fabManager?.text = requireContext().getText(R.string.clazz)

        //override this to show our own bottom sheet
        fabManager?.onClickListener = {
            val optionList = if(newClazzListOptionVisible) {
                listOf(BottomSheetOption(R.drawable.ic_add_black_24dp,
                        requireContext().getString(R.string.add_a_new,
                                requireContext().getString(R.string.clazz).toLowerCase()), NEW_CLAZZ))
            }else {
                listOf()
            } + listOf(BottomSheetOption(R.drawable.ic_login_24px,
                requireContext().getString(R.string.join_existing,
                requireContext().getString(R.string.clazz).toLowerCase()), JOIN_CLAZZ))

            val sheet = OptionsBottomSheetFragment(optionList, this)
            sheet.show(childFragmentManager, sheet.tag)
        }
    }

    override fun onBottomSheetOptionSelected(optionSelected: BottomSheetOption) {
        when(optionSelected.optionCode) {
            NEW_CLAZZ -> mPresenter?.handleClickCreateNewFab()
            JOIN_CLAZZ -> mPresenter?.handleClickJoinClazz()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mPresenter = null
        dbRepo = null
    }


    override fun onClick(v: View?) {
        if(v?.id == R.id.item_createnew_layout) {
            var args = bundleOf()
            val filterExcludeMembersOfSchool =
                    arguments?.get(PersonListView.ARG_FILTER_EXCLUDE_MEMBERSOFSCHOOL)?.toString()?.toLong()?:0L
            if(filterExcludeMembersOfSchool != 0L){
                args = bundleOf(UstadView.ARG_SCHOOL_UID to filterExcludeMembersOfSchool.toString())
            }
            navigateToEditEntity(null, R.id.clazz_edit_dest, Clazz::class.java,
                    argBundle = args)
        }
    }

    override fun onMessageIdOptionSelected(view: AdapterView<*>?, messageIdOption: MessageIdOption) {
        mPresenter?.handleClickSortOrder(messageIdOption)
    }

    override fun onNoMessageIdOptionSelected(view: AdapterView<*>?) {
        //do nothing
    }

    override val displayTypeRepo: Any?
        get() = dbRepo?.clazzDao

    companion object {

        const val NEW_CLAZZ = 2

        const val JOIN_CLAZZ = 3

    }


}