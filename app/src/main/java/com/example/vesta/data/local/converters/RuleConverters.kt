package com.example.vesta.data.local.converters

import androidx.room.TypeConverter
import com.example.vesta.data.local.entities.RuleType
import com.example.vesta.data.local.entities.RuleFrequency

class RuleTypeConverter {
    @TypeConverter
    fun fromRuleType(type: RuleType): String = type.name

    @TypeConverter
    fun toRuleType(value: String): RuleType = RuleType.valueOf(value)
}

class RuleFrequencyConverter {
    @TypeConverter
    fun fromRuleFrequency(frequency: RuleFrequency): String = frequency.name

    @TypeConverter
    fun toRuleFrequency(value: String): RuleFrequency = RuleFrequency.valueOf(value)
}
