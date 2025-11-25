package com.example.handspeak.ui.screen.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.handspeak.navigation.Screen
import com.example.handspeak.ui.theme.*

/**
 * شاشة تسجيل الدخول
 * تصميم نظيف وبسيط مع أيقونة دائرية وإشارات يد
 */
@Composable
fun LoginScreen(navController: NavController, viewModel: AuthViewModel = viewModel()) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    val uiState by viewModel.uiState.collectAsState()
    
    // Handle navigation when login is successful
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            viewModel.resetUiState()
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon with hands forming a rectangular frame
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(IconGrey),
                contentAlignment = Alignment.Center
            ) {
                // Hands forming rectangular frame - simplified representation
                // Using a rectangular frame to represent hands creating a frame
                Box(
                    modifier = Modifier
                        .size(60.dp, 50.dp)
                ) {
                    // Top hand representation - fingers (vertical lines pointing down)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        repeat(4) {
                            androidx.compose.foundation.Canvas(
                                modifier = Modifier.size(3.dp, 8.dp)
                            ) {
                                drawLine(
                                    color = Color.White,
                                    start = androidx.compose.ui.geometry.Offset(size.width / 2, 0f),
                                    end = androidx.compose.ui.geometry.Offset(size.width / 2, size.height),
                                    strokeWidth = 2f
                                )
                            }
                        }
                    }
                    
                    // Rectangular frame (hands forming the frame)
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        drawRoundRect(
                            color = Color.White,
                            size = androidx.compose.ui.geometry.Size(size.width, size.height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                        )
                    }
                    
                    // Bottom hand representation - fingers (vertical lines pointing up)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        repeat(4) {
                            androidx.compose.foundation.Canvas(
                                modifier = Modifier.size(3.dp, 8.dp)
                            ) {
                                drawLine(
                                    color = Color.White,
                                    start = androidx.compose.ui.geometry.Offset(size.width / 2, size.height),
                                    end = androidx.compose.ui.geometry.Offset(size.width / 2, 0f),
                                    strokeWidth = 2f
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App Name - "HandSpeak" with script-like style
            Text(
                text = "HandSpeak",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlack,
                style = MaterialTheme.typography.displaySmall
            )

            Spacer(modifier = Modifier.height(48.dp))

            // EMAIL Input Field
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "EMAIL",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = InputGrey,
                        unfocusedContainerColor = InputGrey,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = TextBlack,
                        focusedTextColor = TextBlack,
                        unfocusedTextColor = TextBlack
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // PASSWORD Input Field
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "PASSWORD",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = InputGrey,
                        unfocusedContainerColor = InputGrey,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = TextBlack,
                        focusedTextColor = TextBlack,
                        unfocusedTextColor = TextBlack
                    ),
                    shape = RoundedCornerShape(8.dp),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = TextBlack
                            )
                        }
                    },
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Show error message if any
            if (uiState is AuthUiState.Error) {
                Text(
                    text = (uiState as AuthUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Log in Button (Gold/Yellow with shadow)
            val isLoading = uiState is AuthUiState.Loading
            Button(
                onClick = {
                    viewModel.signIn(email.trim(), password)
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonGold
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 6.dp
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = TextBlack,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Log in",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sign up Link
            TextButton(
                onClick = {
                    navController.navigate("signup")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Sign up",
                    fontSize = 16.sp,
                    color = TextBlack
                )
            }
        }
    }
}

