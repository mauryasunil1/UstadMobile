package com.ustadmobile.core.controller

import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.util.ext.effectiveTimeZone
import com.ustadmobile.core.view.ClazzWorkDetailOverviewView
import com.ustadmobile.core.view.ClazzWorkEditView
import com.ustadmobile.core.view.UstadView.Companion.ARG_ENTITY_UID
import com.ustadmobile.door.DoorLifecycleOwner
import com.ustadmobile.door.DoorMutableLiveData
import com.ustadmobile.door.doorMainDispatcher
import com.ustadmobile.lib.db.entities.*
import com.ustadmobile.lib.util.getSystemTimeInMillis
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.kodein.di.DI


class ClazzWorkDetailOverviewPresenter(context: Any,
           arguments: Map<String, String>, view: ClazzWorkDetailOverviewView,
           di: DI, lifecycleOwner: DoorLifecycleOwner,
            private val newCommentItemListener: DefaultNewCommentItemListener =
                                               DefaultNewCommentItemListener(di, context)
    )
    : UstadDetailPresenter<ClazzWorkDetailOverviewView, ClazzWorkWithSubmission>(context,
        arguments, view, di, lifecycleOwner)
        , NewCommentItemListener by newCommentItemListener {

    override val persistenceMode: PersistenceMode
        get() = PersistenceMode.DB

    override suspend fun onLoadEntityFromDb(db: UmAppDatabase): ClazzWorkWithSubmission? {
        val clazzWorkUid = arguments[ARG_ENTITY_UID]?.toLong() ?: 0L
        val loggedInPersonUid = accountManager.activeAccount.personUid

        val clazzWorkWithSubmission = withTimeoutOrNull(2000){
            db.clazzWorkDao.findWithSubmissionByUidAndPerson(clazzWorkUid, loggedInPersonUid)
        }?: ClazzWorkWithSubmission()


        val clazzWithSchool = withTimeoutOrNull(2000) {
            db.clazzDao.getClazzWithSchool(clazzWorkWithSubmission.clazzWorkClazzUid)
        } ?: ClazzWithSchool()

        view.timeZone = clazzWithSchool.effectiveTimeZone()

        val loggedInPerson = withTimeoutOrNull(2000){
            db.personDao.findByUidAsync(loggedInPersonUid)
        }
        val clazzMember: ClazzMember? = withTimeoutOrNull(2000){
            db.clazzMemberDao.findByPersonUidAndClazzUidAsync(loggedInPersonUid,
                    clazzWorkWithSubmission.clazzWorkClazzUid)
        }

        if(loggedInPerson?.admin == true){
            view.isStudent = false
        }else{
            if(clazzMember == null){
                view.isStudent = false
            }else {
                view.isStudent = (clazzMember.clazzMemberRole != ClazzMember.ROLE_TEACHER)
            }
        }

        //If Submission object doesn't exist, create it.
        if(clazzWorkWithSubmission.clazzWorkSubmission == null && view.isStudent){
            clazzWorkWithSubmission.clazzWorkSubmission = ClazzWorkSubmission().apply {
                clazzWorkSubmissionClazzWorkUid = clazzWorkWithSubmission.clazzWorkUid
                clazzWorkSubmissionClazzMemberUid = clazzMember?.clazzMemberUid?:0L
                clazzWorkSubmissionPersonUid = loggedInPersonUid
                clazzWorkSubmissionInactive = false
                clazzWorkSubmissionDateTimeStarted = getSystemTimeInMillis()
            }
        }

        if(clazzWorkWithSubmission.clazzWorkSubmissionType == ClazzWork.CLAZZ_WORK_SUBMISSION_TYPE_QUIZ) {
            val questionAndOptions: List<ClazzWorkQuestionAndOptionRow> =
                    withTimeoutOrNull(2000) {
                        db.clazzWorkQuestionDao.findAllActiveQuestionsWithOptionsInClazzWorkAsList(
                                clazzWorkUid)
                    } ?: listOf()

            val questionsAndOptionsWithResponseList: List<ClazzWorkQuestionAndOptionWithResponse> =
                questionAndOptions.groupBy { it.clazzWorkQuestion }.entries
                    .map {
                        val questionUid = it.key?.clazzWorkQuestionUid ?: 0L
                        val qResponse: MutableList<ClazzWorkQuestionResponse> =
                                withTimeoutOrNull(2000) {
                                    db.clazzWorkQuestionResponseDao.findByQuestionUidAndClazzMemberUidAsync(
                                            questionUid, clazzMember?.clazzMemberUid
                                            ?: 0L).toMutableList()
                                }?: mutableListOf()
                        if (qResponse.isEmpty()) {
                            qResponse.add(ClazzWorkQuestionResponse().apply {
                                clazzWorkQuestionResponseQuestionUid = questionUid
                                clazzWorkQuestionResponsePersonUid = loggedInPersonUid
                                clazzWorkQuestionResponseClazzMemberUid = clazzMember?.clazzMemberUid
                                        ?: 0L
                                clazzWorkQuestionResponseClazzWorkUid = clazzWorkUid
                                        ?: 0L
                            })
                        }
                        ClazzWorkQuestionAndOptionWithResponse(
                                entity ?: ClazzWorkWithSubmission(),
                                it.key ?: ClazzWorkQuestion(),
                                it.value.map {
                                    it.clazzWorkQuestionOption ?: ClazzWorkQuestionOption()
                                },
                                qResponse.first())
                    }

            if(view.isStudent && clazzWorkWithSubmission.clazzWorkSubmission?.clazzWorkSubmissionUid == 0L ) {
                view.quizSubmissionEdit =
                        DoorMutableLiveData(questionsAndOptionsWithResponseList)
            }else{
                view.quizSubmissionView = DoorMutableLiveData(questionsAndOptionsWithResponseList)
            }
        }

        newCommentItemListener.fromPerson = loggedInPersonUid
        newCommentItemListener.entityId = clazzWorkUid

        //Find Content and questions
        view.clazzWorkContent =
                withTimeoutOrNull(2000) {
                    repo.clazzWorkContentJoinDao.findAllContentByClazzWorkUidDF(
                            clazzWorkUid, loggedInPersonUid)
                }


        view.clazzWorkPublicComments = repo.commentsDao.findPublicByEntityTypeAndUidLive(
                ClazzWork.CLAZZ_WORK_TABLE_ID, clazzWorkWithSubmission.clazzWorkUid)

        if(view.isStudent) {
            view.clazzWorkPrivateComments =
                    repo.commentsDao.findPrivateByEntityTypeAndUidAndForPersonLive2(
                            ClazzWork.CLAZZ_WORK_TABLE_ID, clazzWorkWithSubmission.clazzWorkUid,
                            loggedInPersonUid)
        }

        return clazzWorkWithSubmission
    }

    override fun handleClickEdit() {
        systemImpl.go(ClazzWorkEditView.VIEW_NAME , arguments, context)
    }

    override suspend fun onCheckEditPermission(account: UmAccount?): Boolean {
        val clazzUid = withTimeoutOrNull(2000) {
            repo.clazzWorkDao.findByUidAsync(arguments[ARG_ENTITY_UID]?.toLong()
                    ?: 0)?.clazzWorkClazzUid
        } ?: 0L

        return db.clazzDao.personHasPermissionWithClazz(accountManager.activeAccount.personUid,
                    clazzUid, Role.PERMISSION_CLAZZWORK_UPDATE)
    }

    fun handleClickSubmit(){
        val questionsWithOptionsAndResponse =
                view.quizSubmissionEdit?.getValue()?: listOf()
        val newOptionsAndResponse = mutableListOf<ClazzWorkQuestionAndOptionWithResponse>()

        val clazzWorkWithSubmission = entity
        GlobalScope.launch {
            for (everyResult in questionsWithOptionsAndResponse) {
                val response = everyResult.clazzWorkQuestionResponse
                if(response.clazzWorkQuestionResponseUid == 0L) {
                    response.clazzWorkQuestionResponseUid =
                            repo.clazzWorkQuestionResponseDao.insertAsync(response)
                }else{
                    repo.clazzWorkQuestionResponseDao.updateAsync(response)
                }
                everyResult.clazzWorkQuestionResponse = response
                newOptionsAndResponse.add(everyResult)
            }

            val loggedInPersonUid = accountManager.activeAccount.personUid
            val clazzMember: ClazzMember? = withTimeoutOrNull(2000){
                repo.clazzMemberDao.findByPersonUidAndClazzUidAsync(loggedInPersonUid,
                        entity?.clazzWorkClazzUid?:0L)
            }

            val submission = entity?.clazzWorkSubmission ?: ClazzWorkSubmission().apply {
                clazzWorkSubmissionClazzWorkUid = clazzWorkWithSubmission?.clazzWorkUid ?: 0L
                clazzWorkSubmissionClazzMemberUid = clazzMember?.clazzMemberUid ?: 0L
                clazzWorkSubmissionDateTimeFinished = getSystemTimeInMillis()
                clazzWorkSubmissionInactive = false
                clazzWorkSubmissionPersonUid = loggedInPersonUid
            }

            submission.clazzWorkSubmissionDateTimeFinished = getSystemTimeInMillis()

            if(submission.clazzWorkSubmissionUid == 0L) {
                submission.clazzWorkSubmissionUid = repo.clazzWorkSubmissionDao.insertAsync(submission)
            }else{
                repo.clazzWorkSubmissionDao.updateAsync(submission)
            }
            clazzWorkWithSubmission?.clazzWorkSubmission = submission
            view.runOnUiThread(Runnable {
                view.entity = clazzWorkWithSubmission
                view.quizSubmissionView =
                        DoorMutableLiveData(newOptionsAndResponse)
            })

        }
    }


}