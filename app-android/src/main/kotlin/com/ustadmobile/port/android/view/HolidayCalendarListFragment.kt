package com.ustadmobile.port.android.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.toughra.ustadmobile.R
import com.toughra.ustadmobile.databinding.ItemHolidayCalendarBinding
import com.ustadmobile.core.controller.HolidayCalendarListPresenter
import com.ustadmobile.core.controller.UstadListPresenter
import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.impl.UMAndroidUtil
import com.ustadmobile.core.impl.UmAccountManager
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.util.MessageIdOption
import com.ustadmobile.core.util.ext.toStringMap
import com.ustadmobile.core.view.*
import com.ustadmobile.lib.db.entities.HolidayCalendar
import com.ustadmobile.lib.db.entities.HolidayCalendarWithNumEntries
import com.ustadmobile.port.android.view.ext.navigateToEditEntity
import com.ustadmobile.port.android.view.ext.setSelectedIfInList
import com.ustadmobile.port.android.view.util.PagedListAdapterWithNewItem
import com.ustadmobile.port.android.view.util.getDataItemViewHolder


class HolidayCalendarListFragment(): UstadListViewFragment<HolidayCalendar, HolidayCalendarWithNumEntries>(),
        HolidayCalendarListView, MessageIdSpinner.OnMessageIdOptionSelectedListener, View.OnClickListener{

    private var mPresenter: HolidayCalendarListPresenter? = null

    private var dbRepo: UmAppDatabase? = null

    override val listPresenter: UstadListPresenter<*, in HolidayCalendarWithNumEntries>?
        get() = mPresenter

    class HolidayCalendarListRecyclerAdapter(var presenter: HolidayCalendarListPresenter?,
                                             newItemVisible: Boolean,
                                             onClickNewItem: View.OnClickListener,
                                             createNewText: String)
        : PagedListAdapterWithNewItem<HolidayCalendarWithNumEntries>(DIFF_CALLBACK,
            newItemVisible = newItemVisible,
            onClickNewItem = onClickNewItem,
            createNewText = createNewText) {

        class HolidayCalendarListViewHolder(val itemBinding: ItemHolidayCalendarBinding): RecyclerView.ViewHolder(itemBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            if(viewType == ITEMVIEWTYPE_NEW) {
                return super.onCreateViewHolder(parent, viewType)
            }else {
                val itemBinding = ItemHolidayCalendarBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                itemBinding.pagedListAdapter = this
                itemBinding.presenter = presenter
                return HolidayCalendarListViewHolder(itemBinding)
            }

        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val itemHolder = holder.getDataItemViewHolder()
            if(itemHolder is HolidayCalendarListViewHolder) {
                val item = getItem(position)
                itemHolder.itemBinding.holidayCalendar = item
                itemHolder.itemView.setSelectedIfInList(item, selectedItems, DIFF_CALLBACK)
            }
        }

        override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
            super.onDetachedFromRecyclerView(recyclerView)
            presenter = null
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        dbRepo = UmAccountManager.getRepositoryForActiveAccount(requireContext())
        mPresenter = HolidayCalendarListPresenter(requireContext(), UMAndroidUtil.bundleToMap(arguments),
                this, this, UstadMobileSystemImpl.instance,
                UmAccountManager.getActiveDatabase(requireContext()),
                UmAccountManager.getRepositoryForActiveAccount(requireContext()),
                UmAccountManager.activeAccountLiveData)
        mDataBinding?.presenter = mPresenter
        mDataBinding?.onSortSelected = this
        mRecyclerViewAdapter = HolidayCalendarListRecyclerAdapter(mPresenter, false, this,
            requireContext().getString(R.string.create_new, requireContext().getString(R.string.holiday_calendar)))
        mRecyclerViewAdapter?.selectedItemsLiveData?.observe(this, selectionObserver)
        mPresenter?.onCreate(savedInstanceState.toStringMap())
        return view
    }

    override fun onResume() {
        super.onResume()
        mActivityWithFab?.activityFloatingActionButton?.text = requireContext().getString(R.string.holiday_calendar)
    }

    override fun onClick(view: View?) {
        navigateToEditEntity(null, R.id.holidaycalendar_edit_dest, HolidayCalendar::class.java)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mPresenter = null
        dbRepo = null
    }


    override fun onMessageIdOptionSelected(view: AdapterView<*>?, messageIdOption: MessageIdOption) {
        mPresenter?.handleClickSortOrder(messageIdOption)
    }

    override fun onNoMessageIdOptionSelected(view: AdapterView<*>?) {
        //do nothing
    }

    override val displayTypeRepo: Any?
        get() = dbRepo?.holidayCalendarDao

    companion object {
        val DIFF_CALLBACK: DiffUtil.ItemCallback<HolidayCalendarWithNumEntries> = object
            : DiffUtil.ItemCallback<HolidayCalendarWithNumEntries>() {
            override fun areItemsTheSame(oldItem: HolidayCalendarWithNumEntries,
                                         newItem: HolidayCalendarWithNumEntries): Boolean {
                return oldItem.umCalendarUid == newItem.umCalendarUid
            }

            override fun areContentsTheSame(oldItem: HolidayCalendarWithNumEntries,
                                            newItem: HolidayCalendarWithNumEntries): Boolean {
                return oldItem == newItem
            }
        }

        fun newInstance(bundle: Bundle?): HolidayCalendarListFragment {
            return HolidayCalendarListFragment().apply {
                arguments = bundle
            }
        }
    }
}