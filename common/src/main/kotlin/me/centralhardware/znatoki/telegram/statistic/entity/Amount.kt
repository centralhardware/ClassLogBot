package me.centralhardware.znatoki.telegram.statistic.entity

@JvmInline
value class Amount(val amount: Int) {
    init {
        require(validate(amount)) {
            "Amount must be greater than 0"
        }
    }

    companion object {
        fun validate(value: Int?) = value != null && value > 0
    }

}

fun Int.toAmount() = Amount(this)