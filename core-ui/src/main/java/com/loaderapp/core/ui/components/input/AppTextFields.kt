package com.loaderapp.core.ui.components.input

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
) {
    val style = AppTextFieldDefaults.style()

    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.defaultMinSize(minHeight = style.minHeight),
        enabled = enabled,
        singleLine = singleLine,
        textStyle = AppTextFieldDefaults.textStyle,
        label = label?.let { { Text(text = it, style = AppTextFieldDefaults.textStyle) } },
        placeholder =
            placeholder?.let {
                {
                    Text(
                        text = it,
                        style = AppTextFieldDefaults.placeholderTextStyle,
                    )
                }
            },
        shape = style.shape,
        colors = AppTextFieldDefaults.textFieldColors(),
        contentPadding =
            TextFieldDefaults.contentPadding(
                start = style.horizontalPadding,
                top = style.verticalPadding,
                end = style.horizontalPadding,
                bottom = style.verticalPadding,
            ),
    )
}

@Composable
fun AppOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
) {
    val style = AppTextFieldDefaults.style()

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.defaultMinSize(minHeight = style.minHeight),
        enabled = enabled,
        singleLine = singleLine,
        textStyle = AppTextFieldDefaults.textStyle,
        label = label?.let { { Text(text = it, style = AppTextFieldDefaults.textStyle) } },
        placeholder =
            placeholder?.let {
                {
                    Text(
                        text = it,
                        style = AppTextFieldDefaults.placeholderTextStyle,
                    )
                }
            },
        shape = style.shape,
        colors = AppTextFieldDefaults.outlinedTextFieldColors(),
    )
}

@Composable
fun AppSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String,
) {
    val style = AppTextFieldDefaults.style()

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().defaultMinSize(minHeight = style.minHeight),
        singleLine = true,
        textStyle = AppTextFieldDefaults.textStyle,
        placeholder = {
            Text(
                text = placeholder,
                style = AppTextFieldDefaults.placeholderTextStyle,
            )
        },
        shape = style.shape,
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
            )
        },
        colors = AppTextFieldDefaults.outlinedTextFieldColors(),
    )
}
