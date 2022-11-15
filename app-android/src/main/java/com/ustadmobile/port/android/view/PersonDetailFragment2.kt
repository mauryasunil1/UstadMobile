package com.ustadmobile.port.android.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.toughra.ustadmobile.R
import com.ustadmobile.core.impl.nav.SavedStateHandleAdapter
import com.ustadmobile.core.viewmodel.PersonDetailUiState
import com.ustadmobile.core.viewmodel.PersonDetailViewModel
import org.kodein.di.DI
import org.kodein.di.android.x.closestDI
import android.text.format.DateFormat
import androidx.compose.material.*
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ustadmobile.core.controller.PersonConstants
import com.ustadmobile.lib.db.entities.ClazzEnrolment
import com.ustadmobile.lib.db.entities.ClazzEnrolmentWithClazzAndAttendance
import com.ustadmobile.port.android.ui.theme.ui.theme.Typography
import com.ustadmobile.port.android.util.compose.messageIdMapResource
import java.util.*

class PersonDetailFragment2 : Fragment(){

    val di: DI by closestDI()

    private val viewModel: PersonDetailViewModel by viewModels {
        provideFactory(di, this, requireArguments())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )

            setContent {
                MaterialTheme {
                    PersonDetailScreen(viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        return
    }

    companion object {


        fun provideFactory(
            di: DI,
            owner: SavedStateRegistryOwner,
            defaultArgs: Bundle? = null,
        ): AbstractSavedStateViewModelFactory = object: AbstractSavedStateViewModelFactory(owner, defaultArgs) {
            override fun <T : ViewModel?> create(
                key: String,
                modelClass: Class<T>,
                handle: SavedStateHandle
            ): T {
                return PersonDetailViewModel(di, SavedStateHandleAdapter(handle)) as T
            }
        }
    }
}

@Composable
private fun PersonDetailScreen(
    uiState: PersonDetailUiState = PersonDetailUiState(),
    onClickDial: () -> Unit = {},
    onClickSms: () -> Unit = {},
    onClickEmail: () -> Unit = {},
    onClickCreateAccount: () -> Unit = {},
    onClickChangePassword: () -> Unit = {},
    onClickManageParentalConsent: () -> Unit = {},
    onClickChat: () -> Unit = {},
    onClickClazz: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    )  {

        Image(
            painter = painterResource(id = R.drawable.ic_person_black_24dp),
            contentDescription = null,
            modifier = Modifier
                .height(256.dp)
                .fillMaxWidth())

        QuickActionBar(
            uiState,
            onClickDial,
            onClickSms,
            onClickEmail,
            onClickCreateAccount,
            onClickChangePassword,
            onClickManageParentalConsent,
            onClickChat)

        Divider(color = Color.LightGray, thickness = 1.dp)

        Spacer(modifier = Modifier.height(10.dp))

        Text(stringResource(R.string.basic_details),
            style = Typography.h4,
            modifier = Modifier.padding(8.dp))

        DetailFeilds(uiState)

        Divider(color = Color.LightGray, thickness = 1.dp)

        Spacer(modifier = Modifier.height(10.dp))

        Text(stringResource(R.string.contact_details),
            style = Typography.h4,
            modifier = Modifier.padding(8.dp))

        ContactDetails(
            uiState,
            onClickDial,
            onClickSms,
            onClickEmail)

        Divider(color = Color.LightGray, thickness = 1.dp)

        Spacer(modifier = Modifier.height(10.dp))

        Text(stringResource(R.string.classes),
            style = Typography.h4,
            modifier = Modifier.padding(8.dp))

        Classes(uiState.clazzes, onClickClazz)
    }
}

@Composable
private fun QuickActionBar(
    uiState: PersonDetailUiState,
    onClickDial: () -> Unit = {},
    onClickSms: () -> Unit = {},
    onClickEmail: () -> Unit = {},
    onClickCreateAccount: () -> Unit = {},
    onClickChangePassword: () -> Unit = {},
    onClickManageParentalConsent: () -> Unit = {},
    onClickChat: () -> Unit = {},
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {

        if (!uiState.person?.phoneNum.isNullOrEmpty()){

            QuickActionButton(
                stringResource(R.string.call),
                R.drawable.ic_call_bcd4_24dp,
                onClickDial)

            QuickActionButton(
                stringResource(R.string.text),
                R.drawable.ic_baseline_sms_24,
                onClickSms)
        }

        if (!uiState.person?.emailAddr.isNullOrEmpty()){
            QuickActionButton(
                stringResource(R.string.email),
                R.drawable.ic_email_black_24dp,
                onClickEmail)
        }

        if(uiState.showCreateAccountVisible){
            QuickActionButton(
                stringResource(R.string.create_account),
                R.drawable.ic_person_black_24dp,
                onClickCreateAccount)
        }

        if(uiState.changePasswordVisible){
            QuickActionButton(
                stringResource(R.string.change_password),
                R.drawable.person_with_key,
                onClickChangePassword)
        }

        if (uiState.person?.parentJoin != null){
            QuickActionButton(
                stringResource(R.string.manage_parental_consent),
                R.drawable.ic_baseline_supervised_user_circle_24,
                onClickManageParentalConsent)
        }

        if (uiState.chatVisible){
            QuickActionButton(
                stringResource(R.string.chat),
                R.drawable.ic_baseline_chat_24,
                onClickChat)
        }
    }
}

@Composable
private fun DetailFeilds(uiState: PersonDetailUiState){
    val context = LocalContext.current
    Column(
        modifier = Modifier.padding(8.dp)
    ){

        val gender = messageIdMapResource(
            map = PersonConstants.GENDER_MESSAGE_ID_MAP,
            key = uiState.person?.gender ?: 1)

        val dateOfBirth = remember { DateFormat.getDateFormat(context)
            .format(Date(uiState.person?.dateOfBirth ?: 0)).toString() }

        if (uiState.person?.dateOfBirth != 0L
            && uiState.person?.dateOfBirth != null){
            DetailFeild(
                R.drawable.ic_date_range_black_24dp,
                dateOfBirth,
                stringResource(R.string.birthday))
        }

        if (uiState.person?.gender != 0
            && uiState.person?.gender != null){
            DetailFeild(0,
                gender,
                stringResource(R.string.gender_literal))
        }

        if (uiState.person?.personOrgId != null){
            DetailFeild(
                R.drawable.ic_badge_24dp,
                uiState.person?.personOrgId ?: "",
                stringResource(R.string.organization_id))
        }

        if (!uiState.person?.username.isNullOrEmpty()){
            DetailFeild(
                R.drawable.ic_account_circle_black_24dp,
                uiState.person?.username ?: "",
                stringResource(R.string.username))
        }
    }
}

@Composable
private fun ContactDetails(
    uiState: PersonDetailUiState,
    onClickDial: () -> Unit = {},
    onClickSms: () -> Unit = {},
    onClickEmail: () -> Unit = {},){
    Column(
        modifier = Modifier.padding(8.dp)
    ) {

        if (!uiState.person?.phoneNum.isNullOrEmpty()){
            CallRow(
                uiState.person?.phoneNum ?: "",
                onClickDial,
                onClickSms)
        }

        if (!uiState.person?.emailAddr.isNullOrEmpty()){
            DetailFeild(
                R.drawable.ic_email_black_24dp,
                uiState.person?.emailAddr ?: "",
                stringResource(R.string.email),
                onClickEmail)
        }

        if (!uiState.person?.personAddress.isNullOrEmpty()){
            DetailFeild(
                R.drawable.ic_location_pin_24dp,
                uiState.person?.personAddress ?: "",
                stringResource(R.string.address))
        }
    }
}

@Composable
private fun CallRow(
    phoneNum: String,
    onClickDial: () -> Unit = {},
    onClickSms: () -> Unit = {},){

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        DetailFeild(
            R.drawable.ic_phone_black_24dp,
            phoneNum,
            stringResource(R.string.phone),
            onClickDial)

        TextButton(
            onClick = onClickSms
        ){
            Image(
                painter = painterResource(id = R.drawable.ic_message_bcd4_24dp),
                contentDescription = null,
                colorFilter = ColorFilter.tint(color = Color.Gray),
                modifier = Modifier
                    .size(24.dp))
        }
    }
}

@Composable
private fun Classes(
    clazzes: List<ClazzEnrolmentWithClazzAndAttendance> = emptyList(),
    onClickClazz: () -> Unit = {}){

    clazzes.forEach { clazz ->
        Spacer(modifier = Modifier.height(15.dp))

        Button(onClick = onClickClazz) {
            ClassItem(clazz)
        }
    }
}

@Composable
private fun ClassItem(clazz: ClazzEnrolmentWithClazzAndAttendance){
    Row(modifier = Modifier.padding(8.dp)
    ){
        Image(
            painter = painterResource(id = R.drawable.ic_group_black_24dp),
            contentDescription = null,
            colorFilter = ColorFilter.tint(color = Color.Gray),
            modifier = Modifier
                .width(70.dp))

        Column {
            Text(text = "")
            Text(text = "")

            if (clazz.clazzEnrolmentRole == ClazzEnrolment.ROLE_STUDENT){
                Row (
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Image(painter = painterResource(id = R.drawable.ic_lens_black_24dp),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(color = Color.Gray),
                        modifier = Modifier
                            .size(12.dp))

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(text = "")
                }
            }
        }
    }
}

@Composable
private fun DetailFeild(
    imageId: Int = 0,
    valueText: String,
    labelText: String,
    onClick: () -> Unit = {}
){
    TextButton(
        onClick = onClick
    ){
        Row{
            if (imageId != 0){
                Image(
                    painter = painterResource(id = imageId),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(color = Color.Gray),
                    modifier = Modifier
                        .size(24.dp))
            } else {
                Box(modifier = Modifier.width(24.dp))
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(valueText,
                    style = Typography.h4,
                    color = Color.Black)

                Text(labelText,
                    style = Typography.body2,
                    color = Color.Gray)
            }
        }
    }
}

@Composable
private fun QuickActionButton(text: String, imageId: Int, onClick: () -> Unit){
    TextButton(
        modifier = Modifier.width(110.dp),
        onClick = onClick
    ){
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            Image(
                painter = painterResource(id = imageId),
                contentDescription = null,
                colorFilter = ColorFilter.tint(color = colorResource(R.color.primaryColor)),
                modifier = Modifier
                    .size(24.dp))

            Text(text.uppercase(),
                style= Typography.h4,
                color = colorResource(R.color.primaryColor))
        }
    }
}

@Composable
private fun PersonDetailScreen(viewModel: PersonDetailViewModel) {
    val uiState: PersonDetailUiState by viewModel.uiState.collectAsState(PersonDetailUiState())
    PersonDetailScreen(uiState)
}

@Composable
@Preview
fun PersonDetailScreenPreview() {
    val uiState = PersonDetailUiState()
    PersonDetailScreen(uiState)
}

