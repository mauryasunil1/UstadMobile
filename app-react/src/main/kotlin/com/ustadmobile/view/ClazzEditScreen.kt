package com.ustadmobile.view

import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.hooks.useStringsXml
import com.ustadmobile.core.impl.locale.StringsXml
import com.ustadmobile.core.impl.locale.entityconstants.ScheduleConstants
import com.ustadmobile.core.impl.locale.entityconstants.EnrolmentPolicyConstants
import com.ustadmobile.core.impl.locale.entityconstants.ScheduleConstants.SCHEDULE_FREQUENCY_MESSAGE_ID_MAP
import com.ustadmobile.lib.db.entities.Schedule.Companion.SCHEDULE_FREQUENCY_WEEKLY
import com.ustadmobile.lib.db.entities.Schedule.Companion.DAY_MONDAY
import com.ustadmobile.core.util.MS_PER_HOUR
import com.ustadmobile.core.util.MS_PER_MIN
import com.ustadmobile.core.viewmodel.ClazzEditUiState
import com.ustadmobile.hooks.useFormattedTime
import com.ustadmobile.lib.db.entities.*
import com.ustadmobile.lib.db.entities.ext.shallowCopy
import com.ustadmobile.mui.components.UstadDateEditField
import com.ustadmobile.mui.components.UstadMessageIdDropDownField
import com.ustadmobile.mui.components.UstadTextEditField
import com.ustadmobile.util.ext.addOptionalSuffix
import com.ustadmobile.view.components.UstadBlankIcon
import com.ustadmobile.view.components.UstadEditHeader
import com.ustadmobile.view.components.UstadSwitchField
import com.ustadmobile.wrappers.reacteasysort.LockAxis
import com.ustadmobile.wrappers.reacteasysort.SortableItem
import com.ustadmobile.wrappers.reacteasysort.SortableList
import csstype.number
import csstype.pct
import csstype.px
import dom.html.HTMLDivElement
import kotlinx.js.jso
import mui.icons.material.*
import mui.material.*
import mui.material.List
import mui.material.Menu
import mui.system.responsive
import mui.system.sx
import react.*
import react.dom.aria.ariaLabel
import react.dom.events.MouseEvent
import react.dom.events.MouseEventHandler
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.div

private val COURSE_BLOCK_DRAG_CLASS = "dragging_course_block"

external interface ClazzEditScreenProps : Props {

    var uiState: ClazzEditUiState

    var onClazzChanged: (ClazzWithHolidayCalendarAndSchoolAndTerminology?) -> Unit

    var onCourseBlockMoved: (from: Int, to: Int) -> Unit

    var onClickSchool: () -> Unit

    var onClickTimezone: () -> Unit

    var onClickAddCourseBlock: () -> Unit

    var onClickAddSchedule: () -> Unit

    var onClickEditSchedule: (Schedule) -> Unit

    var onClickDeleteSchedule: (Schedule) -> Unit

    var onClickHolidayCalendar: () -> Unit

    var onCheckedAttendance: (Boolean) -> Unit

    var onClickTerminology: () -> Unit

    var onClickHideBlockPopupMenu: (CourseBlockWithEntity?) -> Unit

    var onClickIndentBlockPopupMenu: (CourseBlockWithEntity?) -> Unit

    var onClickUnIndentBlockPopupMenu: (CourseBlockWithEntity?) -> Unit

    var onClickDeleteBlockPopupMenu: (CourseBlockWithEntity?) -> Unit

    var onClickEditCourse: (CourseBlockWithEntity) -> Unit

}

val ClazzEditScreenComponent2 = FC<ClazzEditScreenProps> { props ->

    val strings: StringsXml = useStringsXml()

    Container {
        Stack {
            spacing = responsive(2)

            UstadEditHeader {
                + strings[MessageID.basic_details]
            }

            UstadTextEditField {
                value = props.uiState.entity?.clazzName ?: ""
                label = strings[MessageID.name]
                enabled = props.uiState.fieldsEnabled
                onChange = {
                    props.onClazzChanged(
                        props.uiState.entity?.shallowCopy {
                            clazzName = it
                        }
                    )
                }
            }

            UstadTextEditField {
                value = props.uiState.entity?.clazzDesc ?: ""
                label = strings[MessageID.description].addOptionalSuffix(strings)
                enabled = props.uiState.fieldsEnabled
                onChange = {
                    props.onClazzChanged(
                        props.uiState.entity?.shallowCopy {
                            clazzDesc = it
                        }
                    )
                }
            }

            UstadTextEditField {
                value = props.uiState.entity?.school?.schoolName ?: ""
                label = strings[MessageID.institution]
                enabled = props.uiState.fieldsEnabled
                onClick = props.onClickSchool
                onChange = {}
            }


            Stack {
                direction = responsive(StackDirection.row)
                spacing = responsive(3)

                sx {
                    width = 100.pct
                }

                UstadDateEditField {
                    timeInMillis = props.uiState.entity?.clazzStartTime ?: 0
                    label = strings[MessageID.start_date]
                    error = props.uiState.clazzStartDateError
                    enabled = props.uiState.fieldsEnabled
                    fullWidth = true
                    onChange = {
                        props.onClazzChanged(
                            props.uiState.entity?.shallowCopy {
                                clazzStartTime = it
                            }
                        )
                    }
                }

                UstadDateEditField {
                    timeInMillis = props.uiState.entity?.clazzEndTime ?: 0
                    label = strings[MessageID.end_date].addOptionalSuffix(strings)
                    error = props.uiState.clazzEndDateError
                    enabled = props.uiState.fieldsEnabled
                    fullWidth = true
                    onChange = {
                        props.onClazzChanged(
                            props.uiState.entity?.shallowCopy {
                                clazzEndTime = it
                            }
                        )
                    }
                }

            }


            UstadTextEditField {
                value = props.uiState.entity?.clazzTimeZone ?: ""
                label = strings[MessageID.timezone]
                enabled = props.uiState.fieldsEnabled
                onClick = { props.onClickTimezone() }
            }

            UstadEditHeader {
                + strings[MessageID.course_blocks]
            }

            CourseBlockList {
                +props
            }

            UstadEditHeader {
                + strings[MessageID.schedule]
            }


            ClazzSchedulesList {
                +props
            }


            UstadTextEditField {
                value = props.uiState.entity?.holidayCalendar?.umCalendarName ?: ""
                label = strings[MessageID.holiday_calendar]
                enabled = props.uiState.fieldsEnabled
                onChange = {}
                onClick = props.onClickHolidayCalendar
            }

            UstadEditHeader {
                + strings[MessageID.course_setup]
            }

            UstadSwitchField {
                label = strings[MessageID.attendance]
                checked = props.uiState.clazzEditAttendanceChecked
                onChanged = { props.onCheckedAttendance(it) }
                enabled = props.uiState.fieldsEnabled
            }

            UstadMessageIdDropDownField {
                value = props.uiState.entity?.clazzEnrolmentPolicy ?: 0
                options = EnrolmentPolicyConstants.ENROLMENT_POLICY_MESSAGE_IDS
                label = strings[MessageID.enrolment_policy]
                id = (props.uiState.entity?.clazzEnrolmentPolicy ?: 0).toString()
                onChange = {
                    props.onClazzChanged(
                        props.uiState.entity?.shallowCopy {
                            clazzEnrolmentPolicy = it?.value ?: 0
                        })
                }
            }

            UstadTextEditField {
                value = props.uiState.entity?.terminology?.ctTitle ?: ""
                label = strings[MessageID.terminology]
                enabled = props.uiState.fieldsEnabled
                onChange = {}
                onClick = props.onClickTerminology
            }
        }
    }
}

val ClazzSchedulesList = FC<ClazzEditScreenProps> { props ->

    val strings = useStringsXml()

    List{

        ListItem {
            ListItemButton {
                onClick = {
                    props.onClickAddSchedule()
                }

                ListItemIcon {
                    + Add.create()
                }

                ListItemText {
                    primary = ReactNode(strings[MessageID.add_a_schedule])
                }
            }

        }

        props.uiState.clazzSchedules.forEach { schedule ->
            val fromTimeFormatted = useFormattedTime(
                timeInMillisSinceMidnight = schedule.sceduleStartTime.toInt(),
            )

            val toTimeFormatted = useFormattedTime(
                timeInMillisSinceMidnight = schedule.scheduleEndTime.toInt(),
            )

            val text = "${strings[SCHEDULE_FREQUENCY_MESSAGE_ID_MAP[schedule.scheduleFrequency] ?: 0]} " +
                    " ${strings[ScheduleConstants.DAY_MESSAGE_ID_MAP[schedule.scheduleDay] ?: 0]  } " +
                    " $fromTimeFormatted - $toTimeFormatted "

            ListItem{
                secondaryAction = IconButton.create {
                    onClick = { props.onClickDeleteSchedule(schedule) }
                    ariaLabel = strings[MessageID.delete]
                    Delete { }
                }

                ListItemButton {
                    onClick = { props.onClickEditSchedule(schedule) }
                    ListItemIcon {
                        UstadBlankIcon { }
                    }

                    ListItemText{
                        primary = ReactNode(text)
                    }
                }
            }
        }
    }
}

private val CONTENT_ENTRY_TYPE_ICON_MAP = mapOf(
    ContentEntry.TYPE_EBOOK to Book,
    ContentEntry.TYPE_VIDEO to SmartDisplay,
    ContentEntry.TYPE_DOCUMENT to TextSnippet,
    ContentEntry.TYPE_ARTICLE to Article,
    ContentEntry.TYPE_COLLECTION to Collections,
    ContentEntry.TYPE_INTERACTIVE_EXERCISE to TouchApp,
    ContentEntry.TYPE_AUDIO to Audiotrack,
)

private val CourseBlockList = FC<ClazzEditScreenProps> { props ->
    val strings = useStringsXml()

    List {

        ListItem {
            ListItemButton {
                onClick = { props.onClickAddCourseBlock }
                ListItemIcon {
                    + Add.create()
                }
                ListItemText {
                    primary = ReactNode(strings[MessageID.add_block])
                }
            }
        }

        SortableList {
            draggedItemClassName = COURSE_BLOCK_DRAG_CLASS
            lockAxis = LockAxis.y

            onSortEnd = { oldIndex, newIndex ->
                props.onCourseBlockMoved(oldIndex, newIndex)
            }

            props.uiState.courseBlockList.forEach { courseBlock ->
                SortableItem {
                    div {
                        val divRef : MutableRefObject<HTMLDivElement> = useRef(null)

                        ListItem{
                            val courseBlockEditAlpha: Double = if (courseBlock.cbHidden) 0.5 else 1.0
                            val startPadding = (courseBlock.cbIndentLevel * 24).px

                            val image = if(courseBlock.cbType == CourseBlock.BLOCK_CONTENT_TYPE)
                                courseBlock.entry?.contentTypeFlag
                            else
                                courseBlock.cbType

                            ListItemButton {
                                sx {
                                    opacity = number(courseBlockEditAlpha)
                                }

                                onClick = {
                                    //Avoid triggering the onClick listener if the dragging is in process
                                    //This might not be needed
                                    if(divRef.current?.classList?.contains(COURSE_BLOCK_DRAG_CLASS) != true) {
                                        props.onClickEditCourse(courseBlock)
                                    }
                                }

                                ListItemIcon {
                                    sx {
                                        paddingLeft = startPadding
                                    }
                                    + (CONTENT_ENTRY_TYPE_ICON_MAP[image]?.create() ?: TextSnippet.create())
                                }

                                ListItemText {
                                    primary = ReactNode(courseBlock.cbTitle ?: "")
                                }
                            }

                            secondaryAction = PopUpMenu.create {
                                fieldsEnabled = props.uiState.fieldsEnabled
                                onClickHideBlockPopupMenu = props.onClickHideBlockPopupMenu
                                onClickIndentBlockPopupMenu = props.onClickIndentBlockPopupMenu
                                onClickUnIndentBlockPopupMenu = props.onClickUnIndentBlockPopupMenu
                                onClickDeleteBlockPopupMenu = props.onClickDeleteBlockPopupMenu
                                uiState = props.uiState.courseBlockStateFor(courseBlock)
                            }
                        }
                    }
                }
            }
        }
    }
}

external interface PopUpMenuProps : Props {

    var fieldsEnabled: Boolean

    var onClickHideBlockPopupMenu: (CourseBlockWithEntity?) -> Unit

    var onClickUnHideBlockPopupMenu: (CourseBlockWithEntity?) -> Unit

    var onClickIndentBlockPopupMenu: (CourseBlockWithEntity?) -> Unit

    var onClickUnIndentBlockPopupMenu: (CourseBlockWithEntity?) -> Unit

    var onClickDeleteBlockPopupMenu: (CourseBlockWithEntity?) -> Unit

    var uiState: ClazzEditUiState.CourseBlockUiState

}

private data class Point(
    val x: Double = 10.0,
    val y: Double = 10.0,
)

val PopUpMenu = FC<PopUpMenuProps> { props ->

    val strings = useStringsXml()

    var point by useState<Point>()

    val handleContextMenu = { event: MouseEvent<*, *> ->
        event.preventDefault()
        point = if (point == null) {
            Point(
                x = event.clientX - 2,
                y = event.clientY - 4,
            )
        } else {
            null
        }
    }

    val handleClose: MouseEventHandler<*> = {
        point = null
    }

    ReactHTML.div {

        IconButton{
            disabled = !(props.fieldsEnabled)
            onClick = handleContextMenu
            ariaLabel = strings[MessageID.more_options]

            + MoreVert.create()
        }

        Menu {
            open = point != null
            onClose = handleClose

            anchorReference = PopoverReference.anchorPosition
            anchorPosition = if (point != null) {
                jso {
                    top = point!!.y
                    left = point!!.x
                }
            } else {
                undefined
            }

            if(props.uiState.showHide) {
                MenuItem {
                    onClick = {
                        props.onClickHideBlockPopupMenu(props.uiState.courseBlock)
                        point = null
                    }
                    + strings[MessageID.hide]
                }
            }

            if(props.uiState.showUnhide) {
                MenuItem {
                    onClick = {
                        props.onClickUnHideBlockPopupMenu(props.uiState.courseBlock)
                        point = null
                    }
                    + strings[MessageID.unhide]
                }
            }

            if(props.uiState.showIndent) {
                MenuItem {
                    onClick = {
                        props.onClickIndentBlockPopupMenu(props.uiState.courseBlock)
                        point = null
                    }
                    + strings[MessageID.indent]
                }
            }

            if (props.uiState.showUnindent) {
                MenuItem {
                    onClick = {
                        props.onClickUnIndentBlockPopupMenu(props.uiState.courseBlock)
                        point = null
                    }
                    + strings[MessageID.unindent]
                }
            }

            MenuItem {
                onClick = {
                    props.onClickDeleteBlockPopupMenu(props.uiState.courseBlock)
                    point = null
                }
                + strings[MessageID.delete]
            }
        }
    }
}

val ClazzEditScreenPreview = FC<Props> {

    var uiStateVar : ClazzEditUiState by useState {
        ClazzEditUiState(
            entity = ClazzWithHolidayCalendarAndSchoolAndTerminology().apply {

            },
            clazzSchedules = listOf(
                Schedule().apply {
                    sceduleStartTime = ((13 * MS_PER_HOUR) + (30 * MS_PER_MIN)).toLong()
                    scheduleEndTime = ((15 * MS_PER_HOUR) + (30 * MS_PER_MIN)).toLong()
                    scheduleFrequency = SCHEDULE_FREQUENCY_WEEKLY
                    scheduleDay = DAY_MONDAY
                }
            ),
            courseBlockList = listOf(
                CourseBlockWithEntity().apply {
                    cbTitle = "Module"
                    cbHidden = true
                    cbType = CourseBlock.BLOCK_MODULE_TYPE
                    cbIndentLevel = 0
                },
                CourseBlockWithEntity().apply {
                    cbTitle = "Content"
                    cbHidden = false
                    cbType = CourseBlock.BLOCK_CONTENT_TYPE
                    entry = ContentEntry().apply {
                        contentTypeFlag = ContentEntry.TYPE_INTERACTIVE_EXERCISE
                    }
                    cbIndentLevel = 1
                },
                CourseBlockWithEntity().apply {
                    cbTitle = "Assignment"
                    cbType = CourseBlock.BLOCK_ASSIGNMENT_TYPE
                    cbHidden = false
                    cbIndentLevel = 1
                },
            ),
            fieldsEnabled = true
        )
    }

    ClazzEditScreenComponent2 {
        uiState = uiStateVar

        onCourseBlockMoved = {fromIndex, toIndex ->
            uiStateVar = uiStateVar.copy(
                courseBlockList = uiStateVar.courseBlockList.toMutableList().apply {
                    add(toIndex, removeAt(fromIndex))
                }.toList()
            )
        }
    }

}