package com.example.instant_vaccine_mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.client.OpenAI
import com.example.instant_vaccine_mobile.ui.theme.InstantvaccinemobileTheme
import com.itextpdf.forms.PdfAcroForm
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.font.PdfFontFactory
import android.content.Context
import android.text.format.DateFormat
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.platform.LocalContext
import com.aallam.openai.api.model.ModelId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Starting activity")
        enableEdgeToEdge()
        setContent {
            InstantvaccinemobileTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    VaccinationForm(modifier = Modifier.padding(innerPadding))
                }
            }
        }
        Log.d(TAG, "onCreate: Activity setup complete")
    }
}

@Composable
fun VaccinationForm(modifier: Modifier = Modifier) {
    Log.d(TAG, "VaccinationForm: Initializing form")
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var aiRecommendation by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val openAI = remember {
        OpenAI(
            token = "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",
            timeout = Timeout(socket = 60.seconds)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Get Vaccinated and a PDF",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        OutlinedTextField(
            value = firstName,
            onValueChange = { 
                firstName = it
                Log.d(TAG, "First name updated: $it")
            },
            label = { Text("First Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = lastName,
            onValueChange = { 
                lastName = it
                Log.d(TAG, "Last name updated: $it")
            },
            label = { Text("Last Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = birthDate,
            onValueChange = { 
                birthDate = it
                Log.d(TAG, "Birth date updated: $it")
            },
            label = { Text("Birthday") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("mm/dd/yyyy") }
        )

        fun getVaccineRecommendation() {
            isLoading = true
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val chatCompletionRequest = ChatCompletionRequest(
                        messages = listOf(
                            ChatMessage(
                                role = ChatRole.System,
                                content = "You are a helpful medical assistant providing vaccine recommendations."
                            ),
                            ChatMessage(
                                role = ChatRole.User,
                                content = "Please provide a brief vaccination recommendation for a person with the following details: Name: $firstName $lastName, Birth Date: $birthDate"
                            )
                        ),
                        model = ModelId("gpt-4"),
                        temperature = 0.7,
                        topP = 1.0,
                        n = 1,
                        maxTokens = 1000,
                        presencePenalty = 0.0,
                        frequencyPenalty = 0.0
                    )
                    
                    val completion: ChatCompletion = openAI.chatCompletion(chatCompletionRequest)
                    val response = completion.choices.first().message.content
                    
                    aiRecommendation = response ?: "Unable to generate recommendation"
                    isLoading = false
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting AI recommendation: ${e.message}", e)
                    aiRecommendation = "Error: Unable to get recommendation"
                    isLoading = false
                }
            }
        }

        if (isLoading) {
            Text("Getting AI recommendation...")
        }
        
        if (aiRecommendation.isNotEmpty()) {
            Text(
                text = aiRecommendation,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }

        Button(
            onClick = { getVaccineRecommendation() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            enabled = firstName.isNotEmpty() && birthDate.isNotEmpty()
        ) {
            Text("Get AI Recommendation")
        }

        Button(
            onClick = {
                Log.d(TAG, "Generate PDF button clicked")
                Log.d(TAG, "Generating PDF for: $firstName, DOB: $birthDate")
                val pdfFile = fillPdfTemplate(context, firstName, lastName,birthDate)
                Log.d(TAG, "PDF generated at: ${pdfFile.absolutePath}")
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    pdfFile
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Text("Generate PDF")
        }
    }
}

private fun fillPdfTemplate(
    context: Context,
    firstName: String,
    lastName: String,
    birthDate: String
): File {
    val externalFilesDir = context.getExternalFilesDir(null)
    val outputDir = File(externalFilesDir, "pdfs")
    outputDir.mkdirs()
    Log.d(TAG, "Output directory created at: ${outputDir.absolutePath}")
    
    val outputFile = File(outputDir, "filled_form.pdf")
    Log.d(TAG, "Output file will be created at: ${outputFile.absolutePath}")

    try {
        context.assets.open("VaccineHistory.pdf").use { inputStream ->
            val reader = PdfReader(inputStream)
            val writer = PdfWriter(FileOutputStream(outputFile))
            val pdfDoc = PdfDocument(reader, writer)

            val form = PdfAcroForm.getAcroForm(pdfDoc, true)
            val fields = form.formFields
            Log.d(TAG, "Available form fields: ${fields.keys.joinToString()}")

            val page = pdfDoc.getFirstPage()
            val canvas = PdfCanvas(page)
            
            val random = Random
            val now = System.currentTimeMillis()
            val oneYearAgo = now - (365L * 24 * 60 * 60 * 1000)
            
            // Generate first dose date between 1 year ago and 6 months ago
            val firstDoseTime = random.nextLong(oneYearAgo, now - (180L * 24 * 60 * 60 * 1000))
            val firstDoseDate = DateFormat.format("MMMM d, yyyy", firstDoseTime)
            
            // Generate second dose date between first dose and now
            val secondDoseTime = random.nextLong(firstDoseTime + (21L * 24 * 60 * 60 * 1000), now)
            val secondDoseDate = DateFormat.format("MMMM d, yyyy", secondDoseTime)
            
            canvas.beginText()
                .setFontAndSize(PdfFontFactory.createFont(), 8f)
                .moveText(80.0, 529.0)
                .showText("$firstName $lastName")
                .moveText(0.0, -10.0)
                .showText(birthDate)
                .moveText(-52.0, -50.0)
                .showText("Dose given on $firstDoseDate")
                .moveText(0.0, -12.0)
                .showText("Dose given on $secondDoseDate")
                .endText()

            pdfDoc.close()
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error generating PDF: ${e.message}", e)
    }
    return outputFile
}

@Preview(showBackground = true)
@Composable
fun VaccinationFormPreview() {
    InstantvaccinemobileTheme {
        VaccinationForm()
    }
}