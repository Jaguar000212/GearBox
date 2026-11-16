package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.LocalToolHandoff
import com.jaguar.gearbox.ui.components.ToolScaffold
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ToolExamplesUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun openingAndCancellingDoNotImportAndConfirmImportsOnce() {
        var imports = 0
        var route: String? = null
        compose.setContent {
            var input by remember { mutableStateOf("existing input") }
            CompositionLocalProvider(LocalToolHandoff provides { target, values ->
                imports++
                route = target
                input = values["input"].orEmpty()
            }) {
                MaterialTheme {
                    ToolScaffold("Average", Tools.byRoute(Tools.ROUTE_AVERAGE)!!.icon, {}) {
                        OutlinedTextField(input, { input = it })
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("More tool actions").performClick()
        compose.onNodeWithText("Help & examples").performClick()
        compose.runOnIdle { assertEquals(0, imports) }
        compose.onNodeWithText("Use example").performClick()
        compose.onNodeWithText("Replace current inputs?").assertExists()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(0, imports) }
        compose.onNodeWithText("Use example").performClick()
        compose.onNodeWithText("Replace inputs").performClick()
        compose.onNodeWithText("2, 4, 6").assertExists()
        compose.runOnIdle {
            assertEquals(1, imports)
            assertEquals(Tools.ROUTE_AVERAGE, route)
        }
        compose.onNodeWithText("Help: Average").assertDoesNotExist()
    }

    @Test fun loanCatalogDescribesLoanRepaymentAndKeepsInterestAlias() {
        val loan = Tools.byRoute(Tools.ROUTE_LOAN)!!
        assertEquals("Loan Calculator", loan.name)
        assertTrue(loan.description.contains("loan repayments"))
        assertTrue(loan.keywords.contains("interest"))
    }
}
