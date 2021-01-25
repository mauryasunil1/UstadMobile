package com.ustadmobile.core.controller

import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.util.MessageIdOption
import com.ustadmobile.core.util.ext.putEntityAsJson
import com.ustadmobile.core.util.safeParse
import com.ustadmobile.core.view.DateRangeView
import com.ustadmobile.core.view.UstadEditView.Companion.ARG_ENTITY_JSON
import com.ustadmobile.door.DoorLifecycleOwner
import com.ustadmobile.door.doorMainDispatcher
import com.ustadmobile.lib.db.entities.DateRangeMoment
import com.ustadmobile.lib.db.entities.Moment
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kodein.di.DI


class DateRangePresenter(context: Any,
        arguments: Map<String, String>, view: DateRangeView,
        lifecycleOwner: DoorLifecycleOwner,
        di: DI)
    : UstadEditPresenter<DateRangeView, DateRangeMoment>(context, arguments, view, di, lifecycleOwner) {

    override val persistenceMode: PersistenceMode
        get() = PersistenceMode.JSON


    enum class RelUnitOption(val optionVal: Int, val messageId: Int) {
        DAY(Moment.DAYS_REL_UNIT,
                MessageID.day),
        WEEK(Moment.WEEKS_REL_UNIT,
                MessageID.xapi_week),
        MONTH(Moment.MONTHS_REL_UNIT,
                MessageID.xapi_month),
        YEAR(Moment.YEARS_REL_UNIT,
                MessageID.year)
    }

    class RelUnitMessageIdOption(day: RelUnitOption, context: Any)
        : MessageIdOption(day.messageId, context, day.optionVal)


    enum class RelToOption(val optionVal: Int, val messageId: Int) {
        DAY(Moment.TODAY_REL_TO,
                MessageID.today),
    }

    class RelToMessageIdOption(day: RelToOption, context: Any)
        : MessageIdOption(day.messageId, context, day.optionVal)

    override fun onCreate(savedState: Map<String, String>?) {
        super.onCreate(savedState)

        view.relToOptions =  RelToOption.values().map { RelToMessageIdOption(it, context) }
        view.relUnitOptions = RelUnitOption.values().map { RelUnitMessageIdOption(it, context) }
    }

    override fun onLoadFromJson(bundle: Map<String, String>): DateRangeMoment? {
        super.onLoadFromJson(bundle)

        val entityJsonStr = bundle[ARG_ENTITY_JSON]

        return if(entityJsonStr != null) {
            safeParse(di, DateRangeMoment.serializer(), entityJsonStr)
        }else {
            DateRangeMoment(Moment(), Moment())
        }
    }

    override fun onSaveInstanceState(savedState: MutableMap<String, String>) {
        super.onSaveInstanceState(savedState)
        val entityVal = entity ?: return
        savedState.putEntityAsJson(ARG_ENTITY_JSON, null, entityVal)
    }

    override fun handleClickSave(entity: DateRangeMoment) {
        when(entity.fromMoment.typeFlag){
            Moment.TYPE_FLAG_FIXED -> {
                view.fromFixedDateMissing = if(entity.fromMoment.fixedTime == 0L)
                    systemImpl.getString(MessageID.field_required_prompt, context) else null
            }
        }
        when(entity.toMoment.typeFlag){
            Moment.TYPE_FLAG_FIXED -> {
                view.toFixedDateMissing = if(entity.toMoment.fixedTime == 0L)
                    systemImpl.getString(MessageID.field_required_prompt, context) else null
            }
        }
        GlobalScope.launch(doorMainDispatcher()) {
            withContext(doorMainDispatcher()) {
                view.finishWithResult(listOf(entity))
            }
        }
    }

    companion object {

        //TODO: Add constants for keys that would be used for any One To Many Join helpers

    }

}