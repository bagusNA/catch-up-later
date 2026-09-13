package com.bagusna.catchuplater.features.capture.packaging

import org.owasp.html.HtmlPolicyBuilder
import org.owasp.html.PolicyFactory
import org.springframework.stereotype.Component
import java.util.regex.Pattern

data class SanitizedArticle(
    val html: String,
    /** Asset keys referenced by the HTML but absent from the validated package. */
    val missingAssetKeys: Set<String>,
)

/**
 * Backend-side re-sanitization of captured article HTML (DEC-013).
 *
 * The extension sanitizes before upload, but the backend treats every package
 * as untrusted and runs its own pinned policy. The policy:
 *
 * - keeps structural prose elements and meaningful tables/lists,
 * - strips scripts, event handlers, styles, iframes, forms and objects,
 * - only allows `http`/`https` links and images served from the reserved
 *   package-asset host,
 * - removes image references to assets that are not part of the package.
 */
@Component
class ArticleHtmlSanitizer {

    private val policy: PolicyFactory = HtmlPolicyBuilder()
        .allowElements(*ALLOWED_ELEMENTS)
        .allowAttributes("class", "title", "dir", "lang").globally()
        .allowAttributes("href", "title")
        .matching(HREF_PATTERN)
        .onElements("a")
        .allowAttributes("src", "alt", "title", "width", "height")
        .matching(ASSET_SRC_PATTERN)
        .onElements("img")
        .allowAttributes("colspan", "rowspan", "scope", "headers").onElements("td", "th")
        .allowAttributes("span").onElements("col", "colgroup")
        .allowAttributes("datetime").onElements("time")
        .allowAttributes("start", "type", "reversed").onElements("ol")
        .allowAttributes("cite").onElements("blockquote", "q")
        .allowUrlProtocols("http", "https")
        .requireRelNofollowOnLinks()
        .toFactory()

    fun sanitize(html: String, availableAssetKeys: Set<String>): SanitizedArticle {
        val sanitized = policy.sanitize(html)
        val missing = linkedSetOf<String>()
        val result = IMG_TAG_PATTERN.matcher(sanitized).let { matcher ->
            val builder = StringBuilder()
            while (matcher.find()) {
                val key = matcher.group(1)
                if (key in availableAssetKeys) {
                    matcher.appendReplacement(builder, java.util.regex.Matcher.quoteReplacement(matcher.group(0)))
                } else {
                    missing.add(key)
                    matcher.appendReplacement(builder, "")
                }
            }
            matcher.appendTail(builder)
            builder.toString()
        }
        return SanitizedArticle(html = result, missingAssetKeys = missing)
    }

    companion object {
        private val ALLOWED_ELEMENTS = arrayOf(
            "a", "abbr", "article", "aside", "b", "blockquote", "br", "caption", "cite", "code",
            "col", "colgroup", "dd", "del", "details", "div", "dl", "dt", "em", "figcaption",
            "figure", "h1", "h2", "h3", "h4", "h5", "h6", "hr", "i", "img", "ins", "kbd", "li",
            "mark", "ol", "p", "pre", "q", "s", "samp", "section", "small", "span", "strong",
            "sub", "summary", "sup", "table", "tbody", "td", "tfoot", "th", "thead", "time",
            "tr", "u", "ul", "var", "wbr",
        )

        private val HREF_PATTERN: Pattern = Pattern.compile("^https?://\\S+$")

        private val ASSET_SRC_PATTERN: Pattern =
            Pattern.compile("^https://${Pattern.quote(CapturePackagePaths.RESERVED_ASSET_HOST)}/[A-Za-z0-9._-]+$")

        private val IMG_TAG_PATTERN: Pattern = Pattern.compile(
            "<img\\b[^>]*src=\"https://${Pattern.quote(CapturePackagePaths.RESERVED_ASSET_HOST)}/([A-Za-z0-9._-]+)\"[^>]*>",
            Pattern.CASE_INSENSITIVE,
        )
    }
}
