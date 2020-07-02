package com.ustadmobile.port.android.view

import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.core.util.PatternsCompat
import com.toughra.ustadmobile.R
import com.toughra.ustadmobile.databinding.FragmentWorkSpaceEnterLinkBinding
import com.ustadmobile.core.controller.WorkspaceEnterLinkPresenter
import com.ustadmobile.core.controller.WorkspaceEnterLinkPresenter.Companion.VALID_WORKSPACE_DOMAIN
import com.ustadmobile.core.impl.UMAndroidUtil
import com.ustadmobile.core.util.ext.toStringMap
import com.ustadmobile.core.view.WorkspaceEnterLinkView
import org.jetbrains.annotations.TestOnly


class WorkspaceEnterLinkFragment : UstadBaseFragment(), WorkspaceEnterLinkView{

    private var mBinding: FragmentWorkSpaceEnterLinkBinding? = null

    private var mPresenter: WorkspaceEnterLinkPresenter? = null

    private val inputCheckDelay: Long = 250

    private val inputCheckHandler: Handler = Handler()

    private var lastInputTime: Long = 0

    @VisibleForTesting
    private var isIdle = false

    private val inputCheckerCallback = Runnable {
        if (System.currentTimeMillis() > lastInputTime + inputCheckDelay) {
            mPresenter?.checkLinkValidity()
        }
    }

    override var workspaceLink: String?
        get() = mBinding?.workspaceLink
        set(value) {}

    override var validLink: Boolean = false
        set(value) {
            handleError(!value)
            loading = false
            mBinding?.showButton = value
            field = value
        }

    override var progressVisible: Boolean = false
        set(value) {
            field = value
            mBinding?.showProgress = value
        }

    private fun handleError(isError: Boolean){
        mBinding?.workspaceLinkView?.isErrorEnabled = isError
        mBinding?.workspaceLinkView?.error = if(isError) getString(R.string.invalid_url) else null
    }

    @TestOnly
    private fun handleIdling(){
        if(!isIdle){
            isIdle = true
            loading = isIdle
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView: View
        mBinding = FragmentWorkSpaceEnterLinkBinding.inflate(inflater, container, false).also {
            rootView = it.root
            it.showButton = false
            it.showProgress = false
        }
        mPresenter = WorkspaceEnterLinkPresenter(requireContext(), UMAndroidUtil.bundleToMap(arguments),this)
        mPresenter?.onCreate(savedInstanceState.toStringMap())
        mBinding?.organisationLink?.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(url: CharSequence?, start: Int, before: Int, count: Int) {
                inputCheckHandler.removeCallbacks(inputCheckerCallback)
            }
            override fun afterTextChanged(s: Editable?) {
                val typedText = s.toString()
                progressVisible = typedText.isNotEmpty()
                val canValidate = PatternsCompat.WEB_URL.matcher(typedText).find()
                        && typedText.contains(VALID_WORKSPACE_DOMAIN)
                handleError(!canValidate)
                if(!canValidate) return
                handleIdling()
                lastInputTime = System.currentTimeMillis()
                inputCheckHandler.postDelayed(inputCheckerCallback, inputCheckDelay)
            }
        })

        return rootView
    }

    override fun onDestroy() {
        super.onDestroy()
        mPresenter = null
        workspaceLink = null
        mBinding = null
    }
}