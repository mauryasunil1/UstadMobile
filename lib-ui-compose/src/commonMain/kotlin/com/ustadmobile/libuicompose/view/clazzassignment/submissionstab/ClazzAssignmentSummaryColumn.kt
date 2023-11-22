package com.ustadmobile.libuicompose.view.clazzassignment.submissionstab

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun RowScope.ClazzAssignmentSummaryColumn(
    number: Int = 0,
    label: String = "",
    addDividerToEnd: Boolean = false,
){
    Column(
        modifier = Modifier.padding(20.dp)
    ){
        Text(number.toString(), style = MaterialTheme.typography.h1)
        Text(label)
    }

    if(addDividerToEnd) {
        Divider(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .padding(vertical = 20.dp)
        )
    }
}
