import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.appgurjant.stickynotes.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetPinBottomSheet(navController: NavController, updateUserPin:(String)->Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    val isPinValid = pin.length == 6
    val isConfirmPinValid = confirmPin.length == 6
    val isSubmitEnabled = isPinValid && isConfirmPinValid && pin == confirmPin

    val bottomSheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = { navController.popBackStack() },
        sheetState = bottomSheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "App PIN",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Enter PIN field
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) pin = it },
                label = { Text("Enter 6-digit PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                isError = pin.isNotEmpty() && !isPinValid,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm PIN field
            OutlinedTextField(
                value = confirmPin,
                onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) confirmPin = it },
                label = { Text("Confirm 6-digit PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                isError = confirmPin.isNotEmpty() && (!isConfirmPinValid || pin != confirmPin),
                modifier = Modifier.fillMaxWidth()
            )

            if (confirmPin.isNotEmpty() && pin != confirmPin) {
                Text(
                    text = "PINs do not match",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))


            // Submit button
            Button(
                onClick = {
                    updateUserPin(pin)
                },
                enabled = isSubmitEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.primary),
                    contentColor = Color.White
                )
            ) {
                Text("Set PIN")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}