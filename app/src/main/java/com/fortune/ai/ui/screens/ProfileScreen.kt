package com.fortune.ai.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.ai.data.model.UserProfile
import com.fortune.ai.ui.theme.CautionRed
import com.fortune.ai.ui.theme.MysticBlack
import com.fortune.ai.ui.theme.MysticDarkPurple
import com.fortune.ai.ui.theme.MysticGold
import com.fortune.ai.ui.theme.MysticLightGold
import com.fortune.ai.ui.theme.MysticPurple
import com.fortune.ai.ui.theme.TextPrimary
import com.fortune.ai.ui.theme.TextSecondary
import com.fortune.ai.ui.theme.TextTertiary
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(existingProfile: UserProfile?, onSubmit: (UserProfile) -> Unit) {
    var name by remember { mutableStateOf(existingProfile?.name ?: "") }
    var gender by remember { mutableStateOf(existingProfile?.gender ?: "男") }
    var birthDate by remember { mutableStateOf(existingProfile?.birthDate ?: "") }
    var birthTime by remember { mutableStateOf(existingProfile?.birthTime ?: "") }
    var birthCity by remember { mutableStateOf(existingProfile?.birthCity ?: "") }
    var bloodType by remember { mutableStateOf(existingProfile?.bloodType ?: "不知道") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var dateError by remember { mutableStateOf<String?>(null) }
    var timeError by remember { mutableStateOf<String?>(null) }
    var cityError by remember { mutableStateOf<String?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val isEdit = existingProfile != null

    if (showDatePicker) {
        LaunchedEffect(Unit) {
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    birthDate = String.format("%d-%02d-%02d", year, month + 1, day)
                    dateError = null
                    showDatePicker = false
                },
                calendar.get(Calendar.YEAR) - 30,
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).apply {
                setOnCancelListener { showDatePicker = false }
            }.show()
        }
    }

    if (showTimePicker) {
        LaunchedEffect(Unit) {
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    birthTime = String.format("%02d:%02d", hour, minute)
                    timeError = null
                    showTimePicker = false
                },
                12, 0, true
            ).apply {
                setOnCancelListener { showTimePicker = false }
            }.show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MysticBlack)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            if (isEdit) "修改基本信息" else "✦ AI 算命",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MysticGold
        )
        Spacer(modifier = Modifier.height(6.dp))
        if (!isEdit) {
            Text(
                "填写信息，诸位大师为你批命",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                nameError = null
            },
            label = { Text("姓名", color = TextTertiary) },
            isError = nameError != null,
            supportingText = nameError?.let { { Text(it, color = CautionRed) } },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MysticGold,
                unfocusedBorderColor = MysticPurple,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedLabelColor = MysticGold,
                cursorColor = MysticGold
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("性别", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("男", "女").forEach { g ->
                FilterChip(
                    selected = gender == g,
                    onClick = { gender = g },
                    label = { Text(g, color = if (gender == g) MysticBlack else TextPrimary) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MysticGold,
                        containerColor = MysticPurple
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MysticDarkPurple,
                contentColor = if (birthDate.isBlank()) TextTertiary else TextPrimary
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(if (birthDate.isBlank()) "选择出生日期" else "出生日期：$birthDate")
        }
        if (dateError != null) {
            Text(dateError!!, color = CautionRed, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { showTimePicker = true },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MysticDarkPurple,
                contentColor = if (birthTime.isBlank()) TextTertiary else TextPrimary
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(if (birthTime.isBlank()) "选择出生时间" else "出生时间：$birthTime")
        }
        if (timeError != null) {
            Text(timeError!!, color = CautionRed, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = birthCity,
            onValueChange = {
                birthCity = it
                cityError = null
            },
            label = { Text("出生城市", color = TextTertiary) },
            placeholder = { Text("如：北京 / Beijing", color = TextTertiary.copy(alpha = 0.5f)) },
            isError = cityError != null,
            supportingText = cityError?.let { { Text(it, color = CautionRed) } },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MysticGold,
                unfocusedBorderColor = MysticPurple,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedLabelColor = MysticGold,
                cursorColor = MysticGold
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("血型", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("A", "B", "AB", "O", "不知道").forEach { bt ->
                FilterChip(
                    selected = bloodType == bt,
                    onClick = { bloodType = bt },
                    label = { Text(bt, color = if (bloodType == bt) MysticBlack else TextPrimary, fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MysticGold,
                        containerColor = MysticPurple
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = {
                var hasError = false

                if (name.isBlank()) {
                    nameError = "请输入姓名"
                    hasError = true
                }
                if (birthDate.isBlank()) {
                    dateError = "请选择出生日期"
                    hasError = true
                }
                if (birthTime.isBlank()) {
                    timeError = "请选择出生时间"
                    hasError = true
                }
                if (birthCity.isBlank()) {
                    cityError = "请输入出生城市"
                    hasError = true
                }

                if (!hasError) {
                    onSubmit(
                        UserProfile(
                            name = name,
                            gender = gender,
                            birthDate = birthDate,
                            birthTime = birthTime,
                            birthCity = birthCity,
                            bloodType = bloodType
                        )
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MysticGold,
                contentColor = MysticBlack
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                if (isEdit) "保存修改" else "✦ 开始全算",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
