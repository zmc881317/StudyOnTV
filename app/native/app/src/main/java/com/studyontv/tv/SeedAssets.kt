package com.studyontv.tv

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * 内置题库：
 * 1. assets/questions.json（来自 data/seed.json）：仅在题库为空时导入一次。
 * 2. assets/banks/*.json（来自 data/banks 中带 id 的年级题库）：按「文件名 + version」各导入一次，
 *    新装和覆盖升级都会补进来；已存在的 id 不覆盖，保留家长修改。
 *    家长删除的内置题，在该题库文件提高 version 之前不会被重新导入。
 */
object SeedAssets {
    private const val TAG = "SeedAssets"
    private const val SEEDED_KEY = "seeded_banks"

    fun ensureQuestions(context: Context, store: LocalStore) {
        seedLegacy(context, store)
        seedBanks(context, store)
    }

    private fun seedLegacy(context: Context, store: LocalStore) {
        if (store.countQuestions() > 0) return
        try {
            val raw = context.assets.open("questions.json").use { it.readBytes().toString(Charsets.UTF_8) }
            val arr = JSONArray(raw)
            val normalized = JSONArray()
            for (i in 0 until arr.length()) {
                val src = arr.getJSONObject(i)
                val item = JSONObject(src.toString())
                    .put("subject", src.optString("subject", "常识"))
                    .put("level", src.optString("level", src.optString("grade", "三年级")))
                    .put("difficulty", src.optInt("difficulty", 2))
                    .put("q", src.optString("q", src.optString("stem")))
                    .put("tip", src.optString("tip", ""))
                if (!src.has("a") && src.has("options")) item.put("a", src.getJSONArray("options"))
                if (!src.has("k") && src.has("answerIndex")) item.put("k", src.optInt("answerIndex", 0))
                normalized.put(item)
            }
            store.importQuestions(normalized)
        } catch (e: Exception) {
            Log.w(TAG, "seed questions.json failed", e)
        }
    }

    private fun seedBanks(context: Context, store: LocalStore) {
        val files = try { context.assets.list("banks")?.filter { it.endsWith(".json") }?.sorted() ?: emptyList() } catch (e: Exception) { emptyList() }
        if (files.isEmpty()) return
        val seeded = try { JSONObject(store.get(SEEDED_KEY, "{}")) } catch (e: Exception) { JSONObject() }
        var changed = false
        for (file in files) {
            try {
                val raw = context.assets.open("banks/$file").use { it.readBytes().toString(Charsets.UTF_8) }
                val bank = JSONObject(raw)
                val version = bank.optInt("version", 1)
                if (seeded.optInt(file, 0) >= version) continue
                val items = bank.optJSONArray("items") ?: JSONArray()
                val fresh = JSONArray()
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    val id = item.optString("id")
                    if (id.isEmpty() || store.hasQuestion(id)) continue
                    fresh.put(item)
                }
                if (fresh.length() > 0) store.importQuestions(fresh)
                seeded.put(file, version)
                changed = true
            } catch (e: Exception) {
                Log.w(TAG, "seed bank $file failed", e)
            }
        }
        if (changed) store.set(SEEDED_KEY, seeded.toString())
    }
}
