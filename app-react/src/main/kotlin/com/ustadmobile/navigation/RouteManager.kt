package com.ustadmobile.navigation

import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.view.*
import com.ustadmobile.view.*

/**
 * Manages all route functionalities like defining the routes and find destinations
 */
object RouteManager {

    val destinationList = listOf(
        UstadDestination("library_books", MessageID.content, ContentEntryList2View.VIEW_NAME_HOME,
            ContentEntryListComponent::class, true),
        UstadDestination(view = ContentEntryList2View.VIEW_NAME,
             component = ContentEntryListComponent::class,  showSearch = true),
        UstadDestination("school", MessageID.schools,SchoolListView.VIEW_NAME, SchoolListComponent::class, showSearch = true),
        UstadDestination("people", MessageID.classes,ClazzList2View.VIEW_NAME, ClazzListComponent::class, showSearch = true),
        UstadDestination("person", MessageID.people, PersonListView.VIEW_NAME, PersonListComponent::class, showSearch = true),
        UstadDestination("pie_chart", MessageID.reports, ReportListView.VIEW_NAME, ReportListComponent::class, divider = true),
        UstadDestination("settings", MessageID.settings, SettingsView.VIEW_NAME, SettingsComponent::class),
        UstadDestination(view = AccountListView.VIEW_NAME, component = AccountListComponent::class),
        UstadDestination(view = Login2View.VIEW_NAME, labelId = MessageID.login, component = LoginComponent::class, showNavigation = false),
        UstadDestination(view = ContentEntryDetailView.VIEW_NAME, component = ContentEntryDetailComponent::class),
        UstadDestination(view = ContentEntryDetailOverviewView.VIEW_NAME, component = ContentEntryDetailOverviewComponent::class),
        UstadDestination(view = EpubContentView.VIEW_NAME, component = EpubContentComponent::class),
        UstadDestination(view = PersonDetailView.VIEW_NAME, component = PersonDetailComponent::class),
        UstadDestination(view = PersonAccountEditView.VIEW_NAME, component = PersonAccountEditComponent::class),
        UstadDestination(view = PersonEditView.VIEW_NAME, component = PersonEditComponent::class),
        UstadDestination(view = XapiPackageContentView.VIEW_NAME, component = XapiPackageContentComponent::class),
        UstadDestination(view = VideoContentView.VIEW_NAME, component = VideoContentComponent::class),
        UstadDestination(view = TimeZoneListView.VIEW_NAME, component = TimeZoneListComponent::class, showSearch = true),
        UstadDestination(view = HolidayCalendarListView.VIEW_NAME, component = HolidayCalendarListComponent::class, showSearch = true),
        UstadDestination(view = HolidayCalendarEditView.VIEW_NAME, component = HolidayCalendarEditComponent::class),
        UstadDestination(view = HolidayEditView.VIEW_NAME, component = HolidayEditComponent::class),
        UstadDestination(view = WebChunkView.VIEW_NAME, component = WebChunkComponent::class),
        UstadDestination(view = RedirectView.VIEW_NAME, component = RedirectComponent::class),
        UstadDestination(view = ClazzDetailView.VIEW_NAME, component = ClazzDetailComponent::class),
        UstadDestination(view = ClazzEdit2View.VIEW_NAME, component = ClazzEditComponent::class),
        UstadDestination(view = ClazzMemberListView.VIEW_NAME, component = ClazzMemberListComponent::class, showSearch = true),
        UstadDestination(view = ClazzDetailOverviewView.VIEW_NAME, component = ClazzDetailOverviewComponent::class),
        UstadDestination(view = ClazzLogListAttendanceView.VIEW_NAME, component = ClazzLogListAttendanceComponent::class),
        UstadDestination(view = ClazzLogEditView.VIEW_NAME, component = ClazzLogEditComponent::class),
        UstadDestination(view = ClazzLogEditAttendanceView.VIEW_NAME, component = ClazzLogEditAttendanceComponent::class),
        UstadDestination(view = SchoolDetailView.VIEW_NAME, component = SchoolDetailComponent::class),
        UstadDestination(view = SchoolDetailOverviewView.VIEW_NAME, component = SchoolDetailOverviewComponent::class),
        UstadDestination(view = SchoolMemberListView.VIEW_NAME, component = SchoolMemberListComponent::class, showSearch = true),
        UstadDestination(view = ClazzEnrolmentEditView.VIEW_NAME, component = ClazzEnrolmentEditComponent::class),
        UstadDestination(view = ScheduleEditView.VIEW_NAME, component = ScheduleEditComponent::class),
        UstadDestination(view = JoinWithCodeView.VIEW_NAME, component = JoinWithCodeComponent::class),
        UstadDestination(view = SchoolEditView.VIEW_NAME, component = SchoolEditComponent::class),
        UstadDestination(view = ScopedGrantEditView.VIEW_NAME, component = ScopedGrantEditComponent::class),
        UstadDestination(view = BitmaskEditView.VIEW_NAME, component = BitmaskEditComponent::class),
        UstadDestination(view = ContentEntryEdit2View.VIEW_NAME, component = ContentEntryEditComponent::class),
        UstadDestination(view = LanguageListView.VIEW_NAME, component = LanguageListComponent::class, showSearch = true),
        UstadDestination(view = LanguageEditView.VIEW_NAME, component = LanguageEditComponent::class),
        UstadDestination(view = ContentEntryImportLinkView.VIEW_NAME, component = ContentEntryImportLinkComponent::class),
        UstadDestination(view = InviteViaLinkView.VIEW_NAME, component = InviteViaLinkComponent::class),
        UstadDestination(view = ClazzEnrolmentListView.VIEW_NAME, component = ClazzEnrolmentListComponent::class),
        UstadDestination(view = LeavingReasonListView.VIEW_NAME, component = LeavingReasonListComponent::class),
        UstadDestination(view = LeavingReasonEditView.VIEW_NAME, component = LeavingReasonEditComponent::class),
        UstadDestination(view = ClazzAssignmentListView.VIEW_NAME, component = ClazzAssignmentListComponent::class),
        UstadDestination(view = ClazzAssignmentEditView.VIEW_NAME, component = ClazzAssignmentEditComponent::class),
        UstadDestination(view = ClazzAssignmentDetailView.VIEW_NAME, component = ClazzAssignmentDetailComponent::class),
        UstadDestination(view = ClazzAssignmentDetailOverviewView.VIEW_NAME, component = ClazzAssignmentOverviewComponent::class),
        UstadDestination(view = ClazzAssignmentDetailStudentProgressOverviewListView.VIEW_NAME, component = ClazzAssignmentDetailStudentProgressListOverviewComponent::class),
        UstadDestination(view = ClazzAssignmentDetailStudentProgressView.VIEW_NAME, component = ClazzAssignmentDetailStudentProgressComponent::class),
        UstadDestination(view = SessionListView.VIEW_NAME, component = SessionListComponent::class, showSearch = true),
        UstadDestination(view = StatementListView.VIEW_NAME, component = StatementListComponent::class),
        UstadDestination(view = ReportTemplateListView.VIEW_NAME, component = ReportTemplateListComponent::class),
        UstadDestination(view = ReportEditView.VIEW_NAME, component = ReportEditComponent::class),
        UstadDestination(view = ReportFilterEditView.VIEW_NAME, component = ReportFilterEditComponent::class),
        UstadDestination(view = ContentEntryList2View.FOLDER_VIEW_NAME, component = ContentEntryListComponent::class),
        UstadDestination(view = ReportDetailView.VIEW_NAME, component = ReportDetailComponent::class)
    )

    /**
     * Default destination to navigate to when destination is not specified
     */
    val defaultDestination: UstadDestination = destinationList.first {
        it.view == RedirectView.VIEW_NAME
    }.apply {
        component =  RedirectComponent::class
    }


    val firstDestination: UstadDestination = destinationList.first {
        it.view == ContentEntryList2View.VIEW_NAME_HOME
    }

    /**
     * Find destination given view name from URL
     * @param view: Current view name
     */
    fun lookupDestinationName(view: String?): UstadDestination? {
        return destinationList.firstOrNull{
            it.view == view
        }
    }
}