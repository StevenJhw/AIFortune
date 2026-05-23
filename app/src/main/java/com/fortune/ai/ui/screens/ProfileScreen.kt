package com.fortune.ai.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.ai.data.model.UserProfile
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
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            if (isEdit) "修改基本信息" else "🔮 AI 算命",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        if (!isEdit) {
            Text(
                "填写信息，诸位大师为你批命",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Name
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                nameError = null
            },
            label = { Text("姓名") },
            isError = nameError != null,
            supportingText = nameError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Gender
        Text("性别", style = MaterialTheme.typography.bodyMedium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("男", "女").forEach { g ->
                FilterChip(
                    selected = gender == g,
                    onClick = { gender = g },
                    label = { Text(g) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Birth date
        Button(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors()
        ) {
            Text(
                if (birthDate.isBlank()) "选择出生日期" else "出生日期：$birthDate",
                color = if (birthDate.isBlank()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.onSurface
            )
        }
        if (dateError != null) {
            Text(dateError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Birth time
        Button(
            onClick = { showTimePicker = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors()
        ) {
            Text(
                if (birthTime.isBlank()) "选择出生时间" else "出生时间：$birthTime",
                color = if (birthTime.isBlank()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.onSurface
            )
        }
        if (timeError != null) {
            Text(timeError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Birth city
        OutlinedTextField(
            value = birthCity,
            onValueChange = {
                birthCity = it
                cityError = null
            },
            label = { Text("出生城市（中文或英文）") },
            placeholder = { Text("如：北京 / Beijing") },
            isError = cityError != null,
            supportingText = cityError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Blood type
        Text("血型", style = MaterialTheme.typography.bodyMedium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("A", "B", "AB", "O", "不知道").forEach { bt ->
                FilterChip(
                    selected = bloodType == bt,
                    onClick = { bloodType = bt },
                    label = { Text(bt) }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

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
                .height(52.dp)
        ) {
            Text(if (isEdit) "保存修改" else "🔮 开始全算", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
