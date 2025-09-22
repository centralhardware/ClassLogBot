package me.centralhardware.znatoki.telegram.statistic.entity

@JvmInline
value class Amount(val amount: Int) {
    init {
        require(validate(amount))
    }

    companion object {
        fun validate(value: Int?) = value != null && value > 0
    }

}