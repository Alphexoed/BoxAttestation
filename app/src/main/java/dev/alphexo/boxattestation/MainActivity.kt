package dev.alphexo.boxattestation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.alphexo.boxattestation.models.CertCA
import dev.alphexo.boxattestation.models.keyUsageModeled
import dev.alphexo.boxattestation.ui.theme.BoxAttestationTheme
import dev.alphexo.boxattestation.utils.CertFile
import dev.alphexo.boxattestation.utils.CertInfo
import java.security.cert.X509Certificate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BoxAttestationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(innerPadding)
                }
            }
        }
    }
}

@Composable
fun MainScreen(innerPadding: PaddingValues) {
    val (fileName, setFileName) = remember { mutableStateOf<String?>(null) }
    val (fileContent, setFileContent) = remember { mutableStateOf("") }
    val (fileKeyData, setFileKeyData) = remember { mutableStateOf<CertInfo?>(null) }
    val (fileKeyVerifyCA, setFileKeyVerifyCA) = remember { mutableStateOf(Pair(CertCA.UNKNOWN, CertCA.UNKNOWN)) }

    Column(Modifier.padding(innerPadding).padding(8.dp).verticalScroll(rememberScrollState())) {
        Card {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(fileName ?: stringResource(R.string.cert_no_file), modifier = Modifier.weight(1f))
                Spacer(Modifier.size(8.dp))
                FilePickerButton { selectedFileName, selectedFileContent, selectedFileKeyData ->
                    setFileName(selectedFileName)
                    setFileContent(selectedFileContent)
                    setFileKeyData(selectedFileKeyData)
                }
                if (fileKeyData != null) {
                    setFileKeyVerifyCA(fileKeyData.verifyCertificateCA())
                }
            }
        }

        Spacer(Modifier.size(8.dp))

        if (!fileName.isNullOrEmpty()) {
            CertificateList(title = stringResource(R.string.certs_ec_title), certificates = fileKeyData?.certsEC, verifyCA = fileKeyVerifyCA.first)
            Spacer(Modifier.size(8.dp))
            CertificateList(title = stringResource(R.string.certs_rsa_title), certificates = fileKeyData?.certsRSA, verifyCA = fileKeyVerifyCA.second)
        }
    }
}

@Composable
fun CertificateList(title: String, certificates: List<X509Certificate>?, verifyCA: CertCA) {
    var expanded by remember { mutableStateOf(false) }
    val onExpandClick = { expanded = !expanded }

    Card {
        Column(Modifier.padding(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(4.dp))
                CertificateCountChip(
                    count = certificates?.size ?: 0
                )
                CertificateCAChip(
                    ca = verifyCA
                )
                Spacer(modifier = Modifier.weight(1f))

                // Expand Icon
                IconButton(onClick = onExpandClick) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    certificates?.forEachIndexed { index, cer ->
                        CertificateInfoCard(cer)
                        if (index != (certificates.size.minus(1))) {
                            Spacer(modifier = Modifier.size(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilePickerButton(
    onFileSelected: (String, String, CertInfo) -> Unit
) {
    val certFile = CertFile()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val fileName = certFile.getFileName(context, uri)
                val fileContent = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().use { reader ->
                        reader.readText()
                    }
                } ?: ""

                onFileSelected(fileName, fileContent, CertInfo(fileContent))
            }
        }
    }

    Button(onClick = {
        if (!Environment.isExternalStorageManager()) {
            val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + context.packageName))
            context.startActivity(intent)
        } else {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "text/xml"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            launcher.launch(intent)
        }
    }) {
        Text(stringResource(R.string.cert_select_file))
    }
}

@Composable
fun CertificateInfoCard(certificate: X509Certificate) {
    val formatter = DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd HH:mm:ss")
        .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
        .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
        .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
        .toFormatter()
        .withZone(ZoneId.systemDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(8.dp)) {
            CertInfoItem(stringResource(R.string.cert_subject), certificate.subjectDN.toString())
            Spacer(Modifier.size(8.dp))
            CertInfoItem(stringResource(R.string.cert_issuer), certificate.issuerDN.toString())
            Spacer(Modifier.size(8.dp))
            CertInfoItem(stringResource(R.string.cert_sn), certificate.serialNumber.toString(16))
            Spacer(Modifier.size(8.dp))
            CertInfoItem(stringResource(R.string.cert_usage), keyUsageModeled(certificate.keyUsage).joinToString("\n"))
            Spacer(Modifier.size(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    CertInfoItem(stringResource(R.string.cert_valid_start), formatter.format(Instant.ofEpochMilli(certificate.notBefore.time)))
                }
                Box(modifier = Modifier.weight(1f)) {
                    CertInfoItem(stringResource(R.string.cert_valid_end), formatter.format(Instant.ofEpochMilli(certificate.notAfter.time)))
                }
            }
        }
    }
}

@Composable
fun CertInfoItem(title: String, value: String, noScroll: Boolean = true) {
    Column {
        Text(title, style = MaterialTheme.typography.titleLarge)
        val modifier = if (noScroll) Modifier else Modifier.horizontalScroll(rememberScrollState())
        Text(value, modifier = modifier, overflow = TextOverflow.Visible)
    }
}

@Composable
fun CertificateCountChip(count: Int) {
    ElevatedFilterChip(
        onClick = { /* do nothing lmao */ },
        selected = true,
        label = {
            Text(count.toString())
        }
    )
}

@Composable
fun CertificateCAChip(ca: CertCA) {
    ElevatedFilterChip(
        onClick = { /* do nothing lmao */ },
        selected = true,
        label = {
            Text(ca.toString())
        }
    )
}
