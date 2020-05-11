package com.ustadmobile.core.controller

import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.impl.UmAccountManager
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.util.MessageIdOption
import com.ustadmobile.core.util.ext.*
import com.ustadmobile.core.view.PersonEditView
import com.ustadmobile.door.DoorLifecycleOwner
import com.ustadmobile.door.DoorLiveData
import com.ustadmobile.door.doorMainDispatcher
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import com.ustadmobile.core.view.UstadView.Companion.ARG_ENTITY_UID
import com.ustadmobile.core.view.UstadEditView.Companion.ARG_ENTITY_JSON
import com.ustadmobile.lib.db.entities.*


class PersonEditPresenter(context: Any,
                          arguments: Map<String, String>, view: PersonEditView,
                          lifecycleOwner: DoorLifecycleOwner,
                          systemImpl: UstadMobileSystemImpl,
                          db: UmAppDatabase, repo: UmAppDatabase,
                          activeAccount: DoorLiveData<UmAccount?> = UmAccountManager.activeAccountLiveData)
    : UstadEditPresenter<PersonEditView, Person>(context, arguments, view, lifecycleOwner, systemImpl,
        db, repo, activeAccount) {

    override val persistenceMode: PersistenceMode
        get() = PersistenceMode.DB

    /*
     * TODO: Add any required one to many join helpers here - use these templates (type then hit tab)
     * onetomanyhelper: Adds a one to many relationship using OneToManyJoinEditHelper
     */
    override fun onCreate(savedState: Map<String, String>?) {
        super.onCreate(savedState)
        view.genderOptions = listOf(MessageIdOption(MessageID.female, context, Person.GENDER_FEMALE),
                MessageIdOption(MessageID.male, context, Person.GENDER_MALE),
                MessageIdOption(MessageID.other, context, Person.GENDER_OTHER))
    }

    override suspend fun onLoadEntityFromDb(db: UmAppDatabase): Person? {
        val entityUid = arguments[ARG_ENTITY_UID]?.toLong() ?: 0L

        val person = withTimeoutOrNull(2000) {
            db.takeIf { entityUid != 0L }?.personDao?.findByUid(entityUid)
        } ?: Person()

        val personPicture = withTimeoutOrNull(2000) {
            db.takeIf { entityUid != 0L }?.personPictureDao?.findByPersonUidAsync(entityUid)
        }

        if(personPicture != null){
            view.personPicturePath = repo.personPictureDao.getAttachmentPath(personPicture)
        }

        return person
    }

    override fun onLoadFromJson(bundle: Map<String, String>): Person? {
        super.onLoadFromJson(bundle)
        val entityJsonStr = bundle[ARG_ENTITY_JSON]
        var editEntity: Person? = null
        if(entityJsonStr != null) {
            editEntity = Json.parse(Person.serializer(), entityJsonStr)
        }else {
            editEntity = Person()
        }

        return editEntity
    }

    override fun onSaveInstanceState(savedState: MutableMap<String, String>) {
        super.onSaveInstanceState(savedState)
        val entityVal = entity
        savedState.putEntityAsJson(ARG_ENTITY_JSON, null,
                entityVal)
    }

    override fun handleClickSave(entity: Person) {
        GlobalScope.launch {
            if(entity.personUid == 0L) {
                entity.personUid = repo.personDao.insertAsync(entity)
            }else {
                repo.personDao.updateAsync(entity)
            }


            //TODO: Bring this back to save the picture
            var personPicture = db.personPictureDao.findByPersonUidAsync(entity.personUid)
            val viewPicturePath = view.personPicturePath
            val currentPath = if(personPicture != null) repo.personPictureDao.getAttachmentPath(personPicture) else null

            if(personPicture != null && viewPicturePath != null && currentPath != viewPicturePath) {
                repo.personPictureDao.setAttachment(personPicture, viewPicturePath)
                repo.personPictureDao.update(personPicture)
            }else if(viewPicturePath != null && currentPath != viewPicturePath) {
                personPicture = PersonPicture().apply {
                    personPicturePersonUid = entity.personUid
                }
                personPicture.personPictureUid = repo.personPictureDao.insert(personPicture)
                repo.personPictureDao.setAttachment(personPicture, viewPicturePath)
            }else if(personPicture != null && currentPath != null && viewPicturePath == null) {
                //picture has been removed
                personPicture.personPictureActive = false
                repo.personPictureDao.setAttachmentDataFromUri(personPicture, null, context)
                repo.personPictureDao.update(personPicture)
            }

            withContext(doorMainDispatcher()) {
                view.finishWithResult(listOf(entity))
            }
        }
    }

}