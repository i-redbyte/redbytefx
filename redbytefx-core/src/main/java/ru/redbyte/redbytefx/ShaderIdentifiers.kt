package ru.redbyte.redbytefx

/**
 * Turns an author-supplied name into an identifier both GLSL ES 3.x and AGSL accept.
 *
 * Shader identifiers are ASCII only. Cyrillic is transliterated so `яркость` stays readable as
 * `yarkost`; other non-ASCII characters become separators. CamelCase becomes snake_case.
 * The result never starts with a digit, never uses the `gl_` / `sk_` prefixes or `__`, and is never
 * a keyword, a reserved word, a type, or a builtin function of either language.
 */
internal fun sanitizeSuggestedIdentifier(raw: String, leadingDigitPrefix: String): String =
    avoidReserved(baseIdentifier(raw, leadingDigitPrefix))

/** [sanitizeSuggestedIdentifier] that always starts with [prefix], such as `u_` for uniforms. */
internal fun sanitizeIdentifier(raw: String, prefix: String): String {
    val base = baseIdentifier(raw, leadingDigitPrefix = prefix)
    return avoidReserved(if (base.startsWith(prefix)) base else prefix + base)
}

/** ASCII snake_case form of [raw] that does not start with a digit; reserved words are not checked. */
private fun baseIdentifier(raw: String, leadingDigitPrefix: String): String {
    val ascii = transliterate(raw)
    val cleaned = buildString {
        ascii.forEachIndexed { index, ch ->
            when {
                !ch.isAsciiLetterOrDigit() -> if (isNotEmpty() && last() != '_') append('_')
                ch in 'A'..'Z' -> {
                    val previous = ascii.getOrNull(index - 1)
                    val next = ascii.getOrNull(index + 1)
                    val afterLowerOrDigit = previous != null && (previous in 'a'..'z' || previous in '0'..'9')
                    val beforeWordTail = previous != null && previous in 'A'..'Z' && next != null && next in 'a'..'z'
                    if (isNotEmpty() && last() != '_' && (afterLowerOrDigit || beforeWordTail)) append('_')
                    append(ch.lowercaseChar())
                }
                else -> append(ch)
            }
        }
    }.trim('_').ifBlank { FALLBACK_IDENTIFIER }
    return if (cleaned.first() in '0'..'9') "$leadingDigitPrefix$cleaned" else cleaned
}

/** Base name for a `let` local or a mutable slot. A safe ASCII suggestion keeps its spelling. */
internal fun localBase(suggested: String?, index: Int): String {
    if (suggested.isNullOrBlank()) return "l$index"
    return if (isVerbatimIdentifier(suggested)) suggested else sanitizeSuggestedIdentifier(suggested, "l")
}

/** True when [name] cannot be declared by user code in GLSL ES 3.x or AGSL. */
internal fun isReservedShaderWord(name: String): Boolean =
    name in RESERVED_SHADER_WORDS ||
        RESERVED_PREFIXES.any { name.startsWith(it) } ||
        "__" in name

private fun isVerbatimIdentifier(name: String): Boolean {
    val first = name.first()
    return (first in 'a'..'z' || first in 'A'..'Z' || first == '_') &&
        name.all { it == '_' || it.isAsciiLetterOrDigit() } &&
        !isReservedShaderWord(name)
}

private fun avoidReserved(name: String): String {
    var safe = name
    if (RESERVED_PREFIXES.any { safe.startsWith(it) }) safe = "rb_$safe"
    if (safe in RESERVED_SHADER_WORDS) safe = "${safe}_value"
    return safe
}

private fun Char.isAsciiLetterOrDigit(): Boolean = this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9'

private fun transliterate(raw: String): String {
    if (raw.all { it.code < ASCII_LIMIT }) return raw
    return buildString(raw.length + raw.length / 2) {
        for (ch in raw) {
            if (ch.code < ASCII_LIMIT) {
                append(ch)
                continue
            }
            val latin = CYRILLIC_TO_LATIN[ch.lowercaseChar()]
            when {
                latin == null -> append('_')
                latin.isEmpty() -> Unit
                ch.isUpperCase() -> append(latin.replaceFirstChar { it.uppercaseChar() })
                else -> append(latin)
            }
        }
    }
}

private const val ASCII_LIMIT = 128
private const val FALLBACK_IDENTIFIER = "value"

private val RESERVED_PREFIXES = listOf("gl_", "sk_")

private val CYRILLIC_TO_LATIN: Map<Char, String> = mapOf(
    'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d", 'е' to "e", 'ё' to "yo",
    'ж' to "zh", 'з' to "z", 'и' to "i", 'й' to "y", 'к' to "k", 'л' to "l", 'м' to "m",
    'н' to "n", 'о' to "o", 'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t", 'у' to "u",
    'ф' to "f", 'х' to "kh", 'ц' to "ts", 'ч' to "ch", 'ш' to "sh", 'щ' to "shch", 'ъ' to "",
    'ы' to "y", 'ь' to "", 'э' to "e", 'ю' to "yu", 'я' to "ya", 'і' to "i", 'ї' to "yi",
    'є' to "ye", 'ґ' to "g", 'ў' to "u",
)

/**
 * Keywords, reserved words, types, and builtin functions of GLSL ES 3.20 and AGSL.
 * A user name from this set would fail to compile or would overload a builtin.
 */
internal val RESERVED_SHADER_WORDS: Set<String> = buildSet {
    addAll(
        """
        attribute const uniform varying buffer shared coherent volatile restrict readonly writeonly
        atomic_uint layout centroid flat smooth noperspective patch sample precise invariant
        break continue do for while switch case default if else subroutine in out inout
        float double int void bool true false discard return lowp mediump highp precision struct
        uint long short half fixed unsigned superp input output filter sizeof cast namespace using
        common partition active resource asm class union enum typedef template this goto inline
        noinline public static extern external interface main shader colorFilter blender
        """.split(Regex("\\s+")),
    )
    for (lanes in 2..4) {
        for (prefix in listOf("vec", "ivec", "uvec", "bvec", "dvec", "fvec", "hvec")) add("$prefix$lanes")
        for (prefix in listOf("float", "half", "int", "uint", "bool", "short", "ushort")) add("$prefix$lanes")
        add("mat$lanes")
        add("dmat$lanes")
        for (columns in 2..4) {
            add("mat${lanes}x$columns")
            add("dmat${lanes}x$columns")
            add("float${lanes}x$columns")
            add("half${lanes}x$columns")
        }
    }
    for (kind in listOf("", "i", "u")) {
        for (shape in listOf("1D", "2D", "3D", "Cube", "2DRect", "1DArray", "2DArray", "CubeArray", "Buffer", "2DMS", "2DMSArray")) {
            add("${kind}sampler$shape")
            add("${kind}image$shape")
            add("${kind}texture$shape")
        }
    }
    addAll(
        listOf(
            "sampler1DShadow", "sampler2DShadow", "samplerCubeShadow", "sampler1DArrayShadow",
            "sampler2DArrayShadow", "samplerCubeArrayShadow", "sampler2DRectShadow", "sampler3DRect",
            "samplerExternalOES",
        ),
    )
    addAll(
        """
        radians degrees sin cos tan asin acos atan sinh cosh tanh asinh acosh atanh pow exp log exp2
        log2 sqrt inversesqrt abs sign floor trunc round roundEven ceil fract mod modf min max clamp
        mix step smoothstep isnan isinf floatBitsToInt floatBitsToUint intBitsToFloat uintBitsToFloat
        fma frexp ldexp packSnorm2x16 unpackSnorm2x16 packUnorm2x16 unpackUnorm2x16 packHalf2x16
        unpackHalf2x16 packUnorm4x8 packSnorm4x8 unpackUnorm4x8 unpackSnorm4x8 length distance dot
        cross normalize faceforward reflect refract matrixCompMult outerProduct transpose determinant
        inverse lessThan lessThanEqual greaterThan greaterThanEqual equal notEqual any all not
        uaddCarry usubBorrow umulExtended imulExtended bitfieldExtract bitfieldInsert bitfieldReverse
        bitCount findLSB findMSB textureSize texture textureProj textureLod textureOffset texelFetch
        texelFetchOffset textureProjOffset textureLodOffset textureProjLod textureProjLodOffset
        textureGrad textureGradOffset textureProjGrad textureProjGradOffset textureGather
        textureGatherOffset textureGatherOffsets texture2D textureCube atomicCounterIncrement
        atomicCounterDecrement atomicCounter atomicAdd atomicMin atomicMax atomicAnd atomicOr
        atomicXor atomicExchange atomicCompSwap imageSize imageSamples imageLoad imageStore
        imageAtomicAdd imageAtomicMin imageAtomicMax imageAtomicAnd imageAtomicOr imageAtomicXor
        imageAtomicExchange imageAtomicCompSwap dFdx dFdy fwidth ddx ddy interpolateAtCentroid
        interpolateAtSample interpolateAtOffset EmitVertex EndPrimitive barrier memoryBarrier
        memoryBarrierAtomicCounter memoryBarrierBuffer memoryBarrierImage memoryBarrierShared
        groupMemoryBarrier saturate unpremul toLinearSrgb fromLinearSrgb eval
        """.split(Regex("\\s+")),
    )
    remove("")
}
