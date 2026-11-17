package com.jaguar.gearbox.data

import android.content.Context
import com.jaguar.gearbox.R
import java.time.LocalDate

/** Context-resolved presentation. Canonical English catalog values remain stable fallbacks for
 * pure logic/tests and search aliases; never use them as the locale-dependent display contract. */
fun Tool.localizedName(context: Context): String = when (route) {
    Tools.ROUTE_AVERAGE -> R.string.catalog_tool_average_name
    Tools.ROUTE_LOAN -> R.string.catalog_tool_loan_name
    Tools.ROUTE_AGE -> R.string.catalog_tool_age_name
    Tools.ROUTE_GEOMETRY -> R.string.catalog_tool_geometry_name
    Tools.ROUTE_PERCENTAGE -> R.string.catalog_tool_percentage_name
    Tools.ROUTE_RATIOS -> R.string.catalog_tool_ratios_name
    Tools.ROUTE_BMI -> R.string.catalog_tool_bmi_name
    Tools.ROUTE_TIP -> R.string.catalog_tool_tip_name
    Tools.ROUTE_DATE_CALCULATOR -> R.string.catalog_tool_date_calculator_name
    Tools.ROUTE_DISCOUNT -> R.string.catalog_tool_discount_name
    Tools.ROUTE_GST -> R.string.catalog_tool_gst_name
    Tools.ROUTE_DAYS_UNTIL -> R.string.catalog_tool_days_until_name
    Tools.ROUTE_FUEL_COST -> R.string.catalog_tool_fuel_cost_name
    Tools.ROUTE_BASE_CONVERTER -> R.string.catalog_tool_base_converter_name
    Tools.ROUTE_UNIT_CONVERTER -> R.string.catalog_tool_unit_converter_name
    Tools.ROUTE_NUMBER_TO_WORDS -> R.string.catalog_tool_number_to_words_name
    Tools.ROUTE_NUMBER_TO_ROMAN -> R.string.catalog_tool_number_to_roman_name
    Tools.ROUTE_DECIMAL_TO_FRACTION -> R.string.catalog_tool_decimal_to_fraction_name
    Tools.ROUTE_MORSE_BINARY -> R.string.catalog_tool_morse_binary_name
    Tools.ROUTE_RANDOM -> R.string.catalog_tool_random_name
    Tools.ROUTE_TAMBOLA -> R.string.catalog_tool_tambola_name
    Tools.ROUTE_FLIP_COIN -> R.string.catalog_tool_flip_coin_name
    Tools.ROUTE_SPIN_BOTTLE -> R.string.catalog_tool_spin_bottle_name
    Tools.ROUTE_RPS -> R.string.catalog_tool_rps_name
    Tools.ROUTE_DICE -> R.string.catalog_tool_dice_name
    Tools.ROUTE_ONE_OF_TWO -> R.string.catalog_tool_one_of_two_name
    Tools.ROUTE_TEAM_PICKER -> R.string.catalog_tool_team_picker_name
    Tools.ROUTE_TIC_TAC_TOE -> R.string.catalog_tool_tic_tac_toe_name
    Tools.ROUTE_COUNTER -> R.string.catalog_tool_counter_name
    Tools.ROUTE_SCOREBOARD -> R.string.catalog_tool_scoreboard_name
    Tools.ROUTE_CHESS_SCOREBOARD -> R.string.catalog_tool_chess_scoreboard_name
    Tools.ROUTE_COLOR_PICKER -> R.string.catalog_tool_color_picker_name
    Tools.ROUTE_TEXT_TOOLS -> R.string.catalog_tool_text_tools_name
    Tools.ROUTE_FOCUS_TIMER -> R.string.focus_catalog_name
    Tools.ROUTE_TIMER_STOPWATCH -> R.string.catalog_tool_timer_stopwatch_name
    Tools.ROUTE_PASSWORD_GENERATOR -> R.string.catalog_tool_password_generator_name
    Tools.ROUTE_HASH_GENERATOR -> R.string.catalog_tool_hash_generator_name
    Tools.ROUTE_QR_GENERATOR -> R.string.catalog_tool_qr_generator_name
    Tools.ROUTE_BUBBLE_LEVEL -> R.string.catalog_tool_bubble_level_name
    Tools.ROUTE_COMPASS -> R.string.catalog_tool_compass_name
    Tools.ROUTE_FLASHLIGHT -> R.string.catalog_tool_flashlight_name
    Tools.ROUTE_SCREEN_RULER -> R.string.catalog_tool_screen_ruler_name
    else -> null
}?.let(context::getString) ?: name

fun Tool.localizedDescription(context: Context): String = when (route) {
    Tools.ROUTE_AVERAGE -> R.string.catalog_tool_average_description
    Tools.ROUTE_LOAN -> R.string.catalog_tool_loan_description
    Tools.ROUTE_AGE -> R.string.catalog_tool_age_description
    Tools.ROUTE_GEOMETRY -> R.string.catalog_tool_geometry_description
    Tools.ROUTE_PERCENTAGE -> R.string.catalog_tool_percentage_description
    Tools.ROUTE_RATIOS -> R.string.catalog_tool_ratios_description
    Tools.ROUTE_BMI -> R.string.catalog_tool_bmi_description
    Tools.ROUTE_TIP -> R.string.catalog_tool_tip_description
    Tools.ROUTE_DATE_CALCULATOR -> R.string.catalog_tool_date_calculator_description
    Tools.ROUTE_DISCOUNT -> R.string.catalog_tool_discount_description
    Tools.ROUTE_GST -> R.string.catalog_tool_gst_description
    Tools.ROUTE_DAYS_UNTIL -> R.string.catalog_tool_days_until_description
    Tools.ROUTE_FUEL_COST -> R.string.catalog_tool_fuel_cost_description
    Tools.ROUTE_BASE_CONVERTER -> R.string.catalog_tool_base_converter_description
    Tools.ROUTE_UNIT_CONVERTER -> R.string.catalog_tool_unit_converter_description
    Tools.ROUTE_NUMBER_TO_WORDS -> R.string.catalog_tool_number_to_words_description
    Tools.ROUTE_NUMBER_TO_ROMAN -> R.string.catalog_tool_number_to_roman_description
    Tools.ROUTE_DECIMAL_TO_FRACTION -> R.string.catalog_tool_decimal_to_fraction_description
    Tools.ROUTE_MORSE_BINARY -> R.string.catalog_tool_morse_binary_description
    Tools.ROUTE_RANDOM -> R.string.catalog_tool_random_description
    Tools.ROUTE_TAMBOLA -> R.string.catalog_tool_tambola_description
    Tools.ROUTE_FLIP_COIN -> R.string.catalog_tool_flip_coin_description
    Tools.ROUTE_SPIN_BOTTLE -> R.string.catalog_tool_spin_bottle_description
    Tools.ROUTE_RPS -> R.string.catalog_tool_rps_description
    Tools.ROUTE_DICE -> R.string.catalog_tool_dice_description
    Tools.ROUTE_ONE_OF_TWO -> R.string.catalog_tool_one_of_two_description
    Tools.ROUTE_TEAM_PICKER -> R.string.catalog_tool_team_picker_description
    Tools.ROUTE_TIC_TAC_TOE -> R.string.catalog_tool_tic_tac_toe_description
    Tools.ROUTE_COUNTER -> R.string.catalog_tool_counter_description
    Tools.ROUTE_SCOREBOARD -> R.string.catalog_tool_scoreboard_description
    Tools.ROUTE_CHESS_SCOREBOARD -> R.string.catalog_tool_chess_scoreboard_description
    Tools.ROUTE_COLOR_PICKER -> R.string.catalog_tool_color_picker_description
    Tools.ROUTE_TEXT_TOOLS -> R.string.catalog_tool_text_tools_description
    Tools.ROUTE_FOCUS_TIMER -> R.string.focus_catalog_description
    Tools.ROUTE_TIMER_STOPWATCH -> R.string.catalog_tool_timer_stopwatch_description
    Tools.ROUTE_PASSWORD_GENERATOR -> R.string.catalog_tool_password_generator_description
    Tools.ROUTE_HASH_GENERATOR -> R.string.catalog_tool_hash_generator_description
    Tools.ROUTE_QR_GENERATOR -> R.string.catalog_tool_qr_generator_description
    Tools.ROUTE_BUBBLE_LEVEL -> R.string.catalog_tool_bubble_level_description
    Tools.ROUTE_COMPASS -> R.string.catalog_tool_compass_description
    Tools.ROUTE_FLASHLIGHT -> R.string.catalog_tool_flashlight_description
    Tools.ROUTE_SCREEN_RULER -> R.string.catalog_tool_screen_ruler_description
    else -> null
}?.let(context::getString) ?: description

fun ToolCategory.localizedLabel(context: Context): String = context.getString(when (this) {
    ToolCategory.CALCULATORS -> R.string.catalog_category_calculators
    ToolCategory.MONEY -> R.string.catalog_category_money
    ToolCategory.TIME -> R.string.catalog_category_time
    ToolCategory.UTILITIES -> R.string.catalog_category_utilities
    ToolCategory.GAMES_AND_RANDOM -> R.string.catalog_category_games_and_random
    ToolCategory.RANDOM -> R.string.catalog_category_random
    ToolCategory.DEVICE -> R.string.catalog_category_device
})

fun Tool.localized(context: Context): Tool = copy(name = localizedName(context), description = localizedDescription(context), keywords = localizedKeywords(context))

internal fun localizedToolHelp(context: Context, route: String?): ToolHelp? = when (route) {
    Tools.ROUTE_AVERAGE -> ToolHelp(context.getString(R.string.catalog_help_average_instructions), context.getString(R.string.catalog_help_average_example), context.getString(R.string.catalog_help_average_limitation))
    Tools.ROUTE_LOAN -> ToolHelp(context.getString(R.string.catalog_help_loan_instructions), context.getString(R.string.catalog_help_loan_example), context.getString(R.string.catalog_help_loan_limitation))
    Tools.ROUTE_COUNTER -> ToolHelp(context.getString(R.string.catalog_help_counter_instructions), context.getString(R.string.catalog_help_counter_example), context.getString(R.string.catalog_help_counter_limitation))
    Tools.ROUTE_AGE -> ToolHelp(context.getString(R.string.catalog_help_age_instructions), context.getString(R.string.catalog_help_age_example), context.getString(R.string.catalog_help_age_limitation))
    Tools.ROUTE_SCOREBOARD -> ToolHelp(context.getString(R.string.catalog_help_scoreboard_instructions), context.getString(R.string.catalog_help_scoreboard_example), context.getString(R.string.catalog_help_scoreboard_limitation))
    Tools.ROUTE_RANDOM -> ToolHelp(context.getString(R.string.catalog_help_random_instructions), context.getString(R.string.catalog_help_random_example), context.getString(R.string.catalog_help_random_limitation))
    Tools.ROUTE_BASE_CONVERTER -> ToolHelp(context.getString(R.string.catalog_help_base_converter_instructions), context.getString(R.string.catalog_help_base_converter_example), context.getString(R.string.catalog_help_base_converter_limitation))
    Tools.ROUTE_COLOR_PICKER -> ToolHelp(context.getString(R.string.catalog_help_color_picker_instructions), context.getString(R.string.catalog_help_color_picker_example), context.getString(R.string.catalog_help_color_picker_limitation))
    Tools.ROUTE_UNIT_CONVERTER -> ToolHelp(context.getString(R.string.catalog_help_unit_converter_instructions), context.getString(R.string.catalog_help_unit_converter_example), context.getString(R.string.catalog_help_unit_converter_limitation))
    Tools.ROUTE_NUMBER_TO_WORDS -> ToolHelp(context.getString(R.string.catalog_help_number_to_words_instructions), context.getString(R.string.catalog_help_number_to_words_example), context.getString(R.string.catalog_help_number_to_words_limitation))
    Tools.ROUTE_NUMBER_TO_ROMAN -> ToolHelp(context.getString(R.string.catalog_help_number_to_roman_instructions), context.getString(R.string.catalog_help_number_to_roman_example), context.getString(R.string.catalog_help_number_to_roman_limitation))
    Tools.ROUTE_GEOMETRY -> ToolHelp(context.getString(R.string.catalog_help_geometry_instructions), context.getString(R.string.catalog_help_geometry_example), context.getString(R.string.catalog_help_geometry_limitation))
    Tools.ROUTE_PERCENTAGE -> ToolHelp(context.getString(R.string.catalog_help_percentage_instructions), context.getString(R.string.catalog_help_percentage_example), context.getString(R.string.catalog_help_percentage_limitation))
    Tools.ROUTE_TAMBOLA -> ToolHelp(context.getString(R.string.catalog_help_tambola_instructions), context.getString(R.string.catalog_help_tambola_example), context.getString(R.string.catalog_help_tambola_limitation))
    Tools.ROUTE_FLIP_COIN -> ToolHelp(context.getString(R.string.catalog_help_flip_coin_instructions), context.getString(R.string.catalog_help_flip_coin_example), context.getString(R.string.catalog_help_flip_coin_limitation))
    Tools.ROUTE_SPIN_BOTTLE -> ToolHelp(context.getString(R.string.catalog_help_spin_bottle_instructions), context.getString(R.string.catalog_help_spin_bottle_example), context.getString(R.string.catalog_help_spin_bottle_limitation))
    Tools.ROUTE_RPS -> ToolHelp(context.getString(R.string.catalog_help_rps_instructions), context.getString(R.string.catalog_help_rps_example), context.getString(R.string.catalog_help_rps_limitation))
    Tools.ROUTE_DICE -> ToolHelp(context.getString(R.string.catalog_help_dice_instructions), context.getString(R.string.catalog_help_dice_example), context.getString(R.string.catalog_help_dice_limitation))
    Tools.ROUTE_CHESS_SCOREBOARD -> ToolHelp(context.getString(R.string.catalog_help_chess_scoreboard_instructions), context.getString(R.string.catalog_help_chess_scoreboard_example), context.getString(R.string.catalog_help_chess_scoreboard_limitation))
    Tools.ROUTE_ONE_OF_TWO -> ToolHelp(context.getString(R.string.catalog_help_one_of_two_instructions), context.getString(R.string.catalog_help_one_of_two_example), context.getString(R.string.catalog_help_one_of_two_limitation))
    Tools.ROUTE_RATIOS -> ToolHelp(context.getString(R.string.catalog_help_ratios_instructions), context.getString(R.string.catalog_help_ratios_example), context.getString(R.string.catalog_help_ratios_limitation))
    Tools.ROUTE_DECIMAL_TO_FRACTION -> ToolHelp(context.getString(R.string.catalog_help_decimal_to_fraction_instructions), context.getString(R.string.catalog_help_decimal_to_fraction_example), context.getString(R.string.catalog_help_decimal_to_fraction_limitation))
    Tools.ROUTE_TEXT_TOOLS -> ToolHelp(context.getString(R.string.catalog_help_text_tools_instructions), context.getString(R.string.catalog_help_text_tools_example), context.getString(R.string.catalog_help_text_tools_limitation))
    Tools.ROUTE_FOCUS_TIMER -> ToolHelp(context.getString(R.string.focus_catalog_instructions), context.getString(R.string.focus_catalog_example), context.getString(R.string.focus_catalog_limitation))
    Tools.ROUTE_TIMER_STOPWATCH -> ToolHelp(context.getString(R.string.catalog_help_timer_stopwatch_instructions), context.getString(R.string.catalog_help_timer_stopwatch_example), context.getString(R.string.catalog_help_timer_stopwatch_limitation))
    Tools.ROUTE_BMI -> ToolHelp(context.getString(R.string.catalog_help_bmi_instructions), context.getString(R.string.catalog_help_bmi_example), context.getString(R.string.catalog_help_bmi_limitation))
    Tools.ROUTE_TIP -> ToolHelp(context.getString(R.string.catalog_help_tip_instructions), context.getString(R.string.catalog_help_tip_example), context.getString(R.string.catalog_help_tip_limitation))
    Tools.ROUTE_PASSWORD_GENERATOR -> ToolHelp(context.getString(R.string.catalog_help_password_generator_instructions), context.getString(R.string.catalog_help_password_generator_example), context.getString(R.string.catalog_help_password_generator_limitation))
    Tools.ROUTE_DATE_CALCULATOR -> ToolHelp(context.getString(R.string.catalog_help_date_calculator_instructions), context.getString(R.string.catalog_help_date_calculator_example), context.getString(R.string.catalog_help_date_calculator_limitation))
    Tools.ROUTE_DISCOUNT -> ToolHelp(context.getString(R.string.catalog_help_discount_instructions), context.getString(R.string.catalog_help_discount_example), context.getString(R.string.catalog_help_discount_limitation))
    Tools.ROUTE_GST -> ToolHelp(context.getString(R.string.catalog_help_gst_instructions), context.getString(R.string.catalog_help_gst_example), context.getString(R.string.catalog_help_gst_limitation))
    Tools.ROUTE_DAYS_UNTIL -> ToolHelp(context.getString(R.string.catalog_help_days_until_instructions), context.getString(R.string.catalog_help_days_until_example), context.getString(R.string.catalog_help_days_until_limitation))
    Tools.ROUTE_TEAM_PICKER -> ToolHelp(context.getString(R.string.catalog_help_team_picker_instructions), context.getString(R.string.catalog_help_team_picker_example), context.getString(R.string.catalog_help_team_picker_limitation))
    Tools.ROUTE_FUEL_COST -> ToolHelp(context.getString(R.string.catalog_help_fuel_cost_instructions), context.getString(R.string.catalog_help_fuel_cost_example), context.getString(R.string.catalog_help_fuel_cost_limitation))
    Tools.ROUTE_MORSE_BINARY -> ToolHelp(context.getString(R.string.catalog_help_morse_binary_instructions), context.getString(R.string.catalog_help_morse_binary_example), context.getString(R.string.catalog_help_morse_binary_limitation))
    Tools.ROUTE_HASH_GENERATOR -> ToolHelp(context.getString(R.string.catalog_help_hash_generator_instructions), context.getString(R.string.catalog_help_hash_generator_example), context.getString(R.string.catalog_help_hash_generator_limitation))
    Tools.ROUTE_TIC_TAC_TOE -> ToolHelp(context.getString(R.string.catalog_help_tic_tac_toe_instructions), context.getString(R.string.catalog_help_tic_tac_toe_example), context.getString(R.string.catalog_help_tic_tac_toe_limitation))
    Tools.ROUTE_QR_GENERATOR -> ToolHelp(context.getString(R.string.catalog_help_qr_generator_instructions), context.getString(R.string.catalog_help_qr_generator_example), context.getString(R.string.catalog_help_qr_generator_limitation))
    Tools.ROUTE_BUBBLE_LEVEL -> ToolHelp(context.getString(R.string.catalog_help_bubble_level_instructions), context.getString(R.string.catalog_help_bubble_level_example), context.getString(R.string.catalog_help_bubble_level_limitation))
    Tools.ROUTE_COMPASS -> ToolHelp(context.getString(R.string.catalog_help_compass_instructions), context.getString(R.string.catalog_help_compass_example), context.getString(R.string.catalog_help_compass_limitation))
    Tools.ROUTE_FLASHLIGHT -> ToolHelp(context.getString(R.string.catalog_help_flashlight_instructions), context.getString(R.string.catalog_help_flashlight_example), context.getString(R.string.catalog_help_flashlight_limitation))
    Tools.ROUTE_SCREEN_RULER -> ToolHelp(context.getString(R.string.catalog_help_screen_ruler_instructions), context.getString(R.string.catalog_help_screen_ruler_example), context.getString(R.string.catalog_help_screen_ruler_limitation))
    else -> null
}

internal fun localizedToolExample(context: Context, route: String?, today: LocalDate): ToolExample? {
    val value = ToolExampleCatalog.forRoute(route, today) ?: return null
    val description = when (route) {
    Tools.ROUTE_AVERAGE -> context.getString(R.string.catalog_example_average)
    Tools.ROUTE_AGE -> context.getString(R.string.catalog_example_age)
    Tools.ROUTE_BASE_CONVERTER -> context.getString(R.string.catalog_example_base_converter)
    Tools.ROUTE_BMI -> context.getString(R.string.catalog_example_bmi)
    Tools.ROUTE_COLOR_PICKER -> context.getString(R.string.catalog_example_color_picker)
    Tools.ROUTE_DATE_CALCULATOR -> context.getString(R.string.catalog_example_date_calculator)
    Tools.ROUTE_DECIMAL_TO_FRACTION -> context.getString(R.string.catalog_example_decimal_to_fraction)
    Tools.ROUTE_DISCOUNT -> context.getString(R.string.catalog_example_discount)
    Tools.ROUTE_FUEL_COST -> context.getString(R.string.catalog_example_fuel_cost)
    Tools.ROUTE_GST -> context.getString(R.string.catalog_example_gst)
    Tools.ROUTE_GEOMETRY -> context.getString(R.string.catalog_example_geometry)
    Tools.ROUTE_LOAN -> context.getString(R.string.catalog_example_loan)
    Tools.ROUTE_MORSE_BINARY -> context.getString(R.string.catalog_example_morse_binary)
    Tools.ROUTE_NUMBER_TO_ROMAN -> context.getString(R.string.catalog_example_number_to_roman)
    Tools.ROUTE_NUMBER_TO_WORDS -> context.getString(R.string.catalog_example_number_to_words)
    Tools.ROUTE_PERCENTAGE -> context.getString(R.string.catalog_example_percentage)
    Tools.ROUTE_RATIOS -> context.getString(R.string.catalog_example_ratios)
    Tools.ROUTE_TEXT_TOOLS -> context.getString(R.string.catalog_example_text_tools)
    Tools.ROUTE_TIP -> context.getString(R.string.catalog_example_tip)
    Tools.ROUTE_UNIT_CONVERTER -> context.getString(R.string.catalog_example_unit_converter)
    Tools.ROUTE_HASH_GENERATOR -> context.getString(R.string.catalog_example_hash_generator)
    Tools.ROUTE_QR_GENERATOR -> context.getString(R.string.catalog_example_qr_generator)
    Tools.ROUTE_DAYS_UNTIL -> context.getString(R.string.catalog_example_days_until, today.plusDays(7).toString())
    else -> value.description
    }
    return value.copy(description = description, inputs = if (route == Tools.ROUTE_DAYS_UNTIL)
        value.inputs + ("label" to context.getString(R.string.catalog_example_event)) else value.inputs)
}

/** Localized discovery terms plus stable English aliases keep existing search habits working. */
fun Tool.localizedKeywords(context: Context): List<String> {
    val resource = when (route) {
    Tools.ROUTE_FOCUS_TIMER -> R.array.focus_catalog_keywords
    Tools.ROUTE_LOAN -> R.array.catalog_keywords_loan
    Tools.ROUTE_BMI -> R.array.catalog_keywords_bmi
    Tools.ROUTE_TIP -> R.array.catalog_keywords_tip
    Tools.ROUTE_DISCOUNT -> R.array.catalog_keywords_discount
    Tools.ROUTE_GST -> R.array.catalog_keywords_gst
    Tools.ROUTE_DAYS_UNTIL -> R.array.catalog_keywords_days_until
    Tools.ROUTE_FUEL_COST -> R.array.catalog_keywords_fuel_cost
    Tools.ROUTE_BASE_CONVERTER -> R.array.catalog_keywords_base_converter
    Tools.ROUTE_UNIT_CONVERTER -> R.array.catalog_keywords_unit_converter
    Tools.ROUTE_MORSE_BINARY -> R.array.catalog_keywords_morse_binary
    Tools.ROUTE_TAMBOLA -> R.array.catalog_keywords_tambola
    Tools.ROUTE_DICE -> R.array.catalog_keywords_dice
    Tools.ROUTE_TEAM_PICKER -> R.array.catalog_keywords_team_picker
    Tools.ROUTE_TIC_TAC_TOE -> R.array.catalog_keywords_tic_tac_toe
    Tools.ROUTE_PASSWORD_GENERATOR -> R.array.catalog_keywords_password_generator
    Tools.ROUTE_HASH_GENERATOR -> R.array.catalog_keywords_hash_generator
    Tools.ROUTE_QR_GENERATOR -> R.array.catalog_keywords_qr_generator
    Tools.ROUTE_BUBBLE_LEVEL -> R.array.catalog_keywords_bubble_level
    Tools.ROUTE_COMPASS -> R.array.catalog_keywords_compass
    Tools.ROUTE_FLASHLIGHT -> R.array.catalog_keywords_flashlight
    Tools.ROUTE_SCREEN_RULER -> R.array.catalog_keywords_screen_ruler
        else -> null
    }
    return ((resource?.let { context.resources.getStringArray(it).toList() } ?: emptyList()) + keywords).distinct()
}
