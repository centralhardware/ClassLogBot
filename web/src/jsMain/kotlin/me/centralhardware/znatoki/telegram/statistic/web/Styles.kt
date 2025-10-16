package me.centralhardware.znatoki.telegram.statistic.web

import org.jetbrains.compose.web.css.*

object AppStyles : StyleSheet() {
    init {
        "*" style {
            margin(0.px)
            padding(0.px)
            property("box-sizing", "border-box")
            property("-webkit-overflow-scrolling", "touch")
            property("scroll-behavior", "smooth")
        }

        "body" style {
            fontFamily(
                "-apple-system", "BlinkMacSystemFont", "Segoe UI",
                "Roboto", "Helvetica", "Arial", "sans-serif"
            )
            background("linear-gradient(135deg, #667eea 0%, #764ba2 100%)")
            minHeight(100.vh)
            property("-webkit-tap-highlight-color", "transparent")
            property("-webkit-touch-callout", "none")
            property("-webkit-user-select", "none")
            property("user-select", "none")
            property("overscroll-behavior", "none")
        }
    }

    val container by style {
        maxWidth(1400.px)
        margin(0.px)
        property("margin-left", "auto")
        property("margin-right", "auto")
        padding(16.px)
        paddingTop(80.px)
        paddingBottom(80.px)
    }

    val mainNav by style {
        position(Position.Fixed)
        top(0.px)
        left(0.px)
        right(0.px)
        padding(12.px, 16.px)
        property("z-index", "100")
    }

    val pageSelector by style {
        maxWidth(600.px)
        margin(0.px)
        property("margin-left", "auto")
        property("margin-right", "auto")
        position(Position.Relative)
    }

    val pageSelectorButton by style {
        width(100.percent)
        padding(14.px, 20.px)
        fontSize(16.px)
        fontWeight(600)
        border(0.px)
        borderRadius(12.px)
        background("linear-gradient(135deg, #667eea 0%, #764ba2 100%)")
        color(Color.white)
        cursor("pointer")
        property("transition", "all 0.3s ease")
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        justifyContent(JustifyContent.Center)
        property("gap", "12px")

        self + active style {
            transform { scale(0.98) }
        }
    }

    val pageDropdown by style {
        position(Position.Absolute)
        property("top", "calc(100% + 4px)")
        left(0.px)
        right(0.px)
        backgroundColor(Color.white)
        borderRadius(12.px)
        property("box-shadow", "0 8px 24px rgba(0,0,0,0.15)")
        overflow("hidden")
        maxHeight(400.px)
        property("overflow-y", "auto")
        opacity(0)
        transform { translateY((-10).px); scale(0.95) }
        property("transition", "all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1)")
    }

    val pageOption by style {
        padding(14.px, 20.px)
        cursor("pointer")
        property("transition", "all 0.2s cubic-bezier(0.4, 0, 0.2, 1)")
        fontSize(15.px)
        fontWeight(500)
        color(Color("#2d3748"))
        property("border-bottom", "1px solid #e2e8f0")

        self + hover style {
            backgroundColor(Color("#f7fafc"))
            transform { translateX(4.px) }
        }

        self + active style {
            backgroundColor(Color("#edf2f7"))
            transform { scale(0.98) }
        }
    }

    val card by style {
        background("rgba(255, 255, 255, 0.95)")
        property("backdrop-filter", "blur(10px)")
        borderRadius(16.px)
        padding(20.px)
        marginBottom(20.px)
        property("box-shadow", "0 8px 32px rgba(0,0,0,0.1)")
        property("transition", "transform 0.3s ease, box-shadow 0.3s ease")
        property("animation", "cardSlideIn 0.4s ease-out")

        self + hover style {
            transform { translateY((-2).px) }
            property("box-shadow", "0 12px 40px rgba(0,0,0,0.15)")
        }
    }

    val button by style {
        padding(10.px, 20.px)
        background("linear-gradient(135deg, #667eea 0%, #764ba2 100%)")
        color(Color.white)
        border(0.px)
        borderRadius(8.px)
        fontSize(14.px)
        fontWeight(600)
        cursor("pointer")
        property("transition", "all 0.3s cubic-bezier(0.4, 0, 0.2, 1)")
        position(Position.Relative)
        overflow("hidden")

        self + hover style {
            transform { translateY((-2).px); scale(1.02) }
            property("box-shadow", "0 6px 20px rgba(102, 126, 234, 0.4)")
        }

        self + active style {
            transform { translateY(0.px); scale(0.98) }
        }
    }

    val secondaryButton by style {
        backgroundColor(Color("#e2e8f0"))
        color(Color("#4a5568"))

        self + hover style {
            backgroundColor(Color("#cbd5e0"))
            property("box-shadow", "0 4px 12px rgba(0, 0, 0, 0.1)")
        }
    }

    val tabButtons by style {
        display(DisplayStyle.Flex)
        property("gap", "8px")
        marginBottom(16.px)
        property("flex-wrap", "wrap")
    }

    val tabButton by style {
        flex("1")
        property("min-width", "120px")
        padding(12.px, 20.px)
        background("linear-gradient(135deg, #667eea 0%, #764ba2 100%)")
        border(2.px, LineStyle.Solid, Color.transparent)
        borderRadius(12.px)
        fontSize(15.px)
        fontWeight(600)
        color(Color("rgba(255, 255, 255, 0.8)"))
        cursor("pointer")
        property("transition", "all 0.3s ease")
        position(Position.Relative)

        self + hover style {
            color(Color.white)
            transform { translateY((-1).px) }
        }
    }

    val tabButtonActive by style {
        backgroundColor(Color.white)
        color(Color("#667eea"))
        property("border-color", "rgba(102, 126, 234, 0.2)")
        property("box-shadow", "0 4px 12px rgba(0, 0, 0, 0.1)")
        transform { translateY((-2).px) }
    }

    val loading by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        alignItems(AlignItems.Center)
        justifyContent(JustifyContent.FlexStart)
        minHeight(80.vh)
        padding(60.px, 20.px, 40.px)
    }

    val loadingHeader by style {
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        property("gap", "16px")
        marginBottom(32.px)
    }

    val loadingSpinner by style {
        position(Position.Relative)
        width(48.px)
        height(48.px)
        property("flex-shrink", "0")
    }

    val loadingTitle by style {
        fontSize(22.px)
        fontWeight(700)
        color(Color.white)
        property("text-shadow", "0 2px 4px rgba(0, 0, 0, 0.2)")
    }

    val loadingLog by style {
        width(100.percent)
        maxWidth(600.px)
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.ColumnReverse)
        property("gap", "6px")
        padding(0.px)
        minHeight(200.px)
        maxHeight(400.px)
        overflow("hidden")
        position(Position.Relative)
        property("mask-image", "linear-gradient(to bottom, transparent 0%, black 15%, black 100%)")
        property("-webkit-mask-image", "linear-gradient(to bottom, transparent 0%, black 15%, black 100%)")
    }

    val loadingLogItem by style {
        padding(8.px, 0.px)
        backgroundColor(Color.transparent)
        fontSize(15.px)
        color(Color.white)
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        property("gap", "12px")
        property("animation", "slideInFade 0.4s ease-out")
        property("text-shadow", "0 2px 4px rgba(0, 0, 0, 0.4)")
        fontWeight(500)

        before {
            property("content", "'▸'")
            color(Color("rgba(255, 255, 255, 0.6)"))
            fontWeight("bold")
            fontSize(18.px)
            property("flex-shrink", "0")
        }
    }

    val loadingLogItemSuccess by style {
        color(Color("#a3f7bf"))

        before {
            property("content", "'✓'")
            color(Color("#48bb78"))
            fontSize(20.px)
        }
    }

    val hidden by style {
        display(DisplayStyle.None)
    }

    val modal by style {
        display(DisplayStyle.None)
        position(Position.Fixed)
        top(0.px)
        left(0.px)
        width(100.percent)
        height(100.percent)
        background("rgba(0, 0, 0, 0)")
        property("z-index", "1000")
        justifyContent(JustifyContent.Center)
        alignItems(AlignItems.Center)
        padding(16.px)
        property("transition", "background 0.3s ease")
    }

    val modalActive by style {
        display(DisplayStyle.Flex)
        property("animation", "modalFadeIn 0.3s ease-out")
        background("rgba(0, 0, 0, 0.5)")
    }

    val modalContent by style {
        backgroundColor(Color.white)
        borderRadius(16.px)
        padding(24.px)
        maxWidth(600.px)
        width(100.percent)
        maxHeight(90.vh)
        property("overflow-y", "auto")
        transform { scale(0.9); translateY(20.px) }
        opacity(0)
        property("transition", "all 0.3s cubic-bezier(0.4, 0, 0.2, 1)")
    }

    val summaryItem by style {
        background("linear-gradient(135deg, #667eea 0%, #764ba2 100%)")
        color(Color.white)
        padding(16.px)
        borderRadius(12.px)
        property("text-align", "center")
        property("transition", "all 0.3s cubic-bezier(0.4, 0, 0.2, 1)")
        property("animation", "summaryPop 0.5s ease-out backwards")
        cursor("pointer")

        self + hover style {
            transform { translateY((-4).px); scale(1.05) }
            property("box-shadow", "0 12px 24px rgba(0, 0, 0, 0.2)")
        }

        self + active style {
            transform { scale(0.95) }
        }
    }

    val badge by style {
        display(DisplayStyle.InlineBlock)
        padding(4.px, 8.px)
        borderRadius(6.px)
        fontSize(11.px)
        fontWeight(600)
        marginLeft(4.px)
        property("transition", "all 0.2s cubic-bezier(0.4, 0, 0.2, 1)")
        property("animation", "badgePop 0.4s cubic-bezier(0.34, 1.56, 0.64, 1)")

        self + hover style {
            transform { scale(1.1) }
        }
    }

    val badgeGroup by style {
        backgroundColor(Color("#bee3f8"))
        color(Color("#2c5282"))
    }

    val badgeExtra by style {
        backgroundColor(Color("#fbd38d"))
        color(Color("#7c2d12"))
    }
}

// Animations should be defined separately as keyframes
val animations = """
    @keyframes cardSlideIn {
        from {
            opacity: 0;
            transform: translateY(20px);
        }
        to {
            opacity: 1;
            transform: translateY(0);
        }
    }

    @keyframes modalFadeIn {
        from {
            background: rgba(0, 0, 0, 0);
        }
        to {
            background: rgba(0, 0, 0, 0.5);
        }
    }

    @keyframes summaryPop {
        from {
            opacity: 0;
            transform: scale(0.8);
        }
        to {
            opacity: 1;
            transform: scale(1);
        }
    }

    @keyframes badgePop {
        from {
            opacity: 0;
            transform: scale(0);
        }
        to {
            opacity: 1;
            transform: scale(1);
        }
    }

    @keyframes slideInFade {
        from {
            opacity: 0;
            transform: translateY(-15px) scale(0.95);
        }
        to {
            opacity: 1;
            transform: translateY(0) scale(1);
        }
    }

    @keyframes spin {
        0% {
            transform: rotate(0deg);
        }
        100% {
            transform: rotate(360deg);
        }
    }

    @keyframes pageFadeIn {
        from {
            opacity: 0;
            transform: translateY(10px);
        }
        to {
            opacity: 1;
            transform: translateY(0);
        }
    }

    @keyframes cardFadeIn {
        from {
            opacity: 0;
            transform: translateY(15px) scale(0.98);
        }
        to {
            opacity: 1;
            transform: translateY(0) scale(1);
        }
    }

    @keyframes counterUp {
        from {
            opacity: 0;
            transform: scale(0.8);
        }
        to {
            opacity: 1;
            transform: scale(1);
        }
    }

    @keyframes modalSlideIn {
        from {
            opacity: 0;
            transform: scale(0.9) translateY(20px);
        }
        to {
            opacity: 1;
            transform: scale(1) translateY(0);
        }
    }

    @keyframes formGroupSlideIn {
        from {
            opacity: 0;
            transform: translateX(-10px);
        }
        to {
            opacity: 1;
            transform: translateX(0);
        }
    }
"""
