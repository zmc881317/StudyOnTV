#!/usr/bin/env python3
"""校验 data/banks/*.json：格式与设备端导入规则一致、id 全局唯一、题目（题干+选项）不重复。

用法：python3 scripts/validate_banks.py [--stats]
"""
import collections
import glob
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TYPES = {"single", "multiple", "boolean", "text", "subjective"}
JUNIOR = ("初中", "初一", "初二", "初三", "七年级", "八年级", "九年级", "中考")
SENIOR = ("高中", "高一", "高二", "高三", "高考")


def stage_of(level):
    level = (level or "").strip()
    if level in ("", "通用", "全部"):
        return "共用"
    if any(w in level for w in JUNIOR):
        return "初中"
    if any(w in level for w in SENIOR):
        return "高中"
    return "小学"


def main():
    errors = []
    ids = {}
    stems = {}
    stats = collections.Counter()
    for path in sorted(glob.glob(os.path.join(ROOT, "data", "banks", "*.json"))):
        name = os.path.basename(path)
        try:
            bank = json.load(open(path, encoding="utf-8"))
        except Exception as exc:  # noqa: BLE001
            errors.append(f"{name}: JSON 解析失败 {exc}")
            continue
        items = bank["items"] if isinstance(bank, dict) else bank
        for n, item in enumerate(items):
            where = f"{name}#{n}"
            t = item.get("type", "single")
            q = str(item.get("q", "")).strip()
            if t not in TYPES:
                errors.append(f"{where}: 未知题型 {t}")
            if not q:
                errors.append(f"{where}: 题干为空")
            diff = item.get("difficulty", 2)
            if not isinstance(diff, int) or not 1 <= diff <= 5:
                errors.append(f"{where}: difficulty 需为 1–5")
            if t in ("single", "multiple"):
                a = item.get("a") or []
                if not 2 <= len(a) <= 8 or any(not str(x).strip() for x in a):
                    errors.append(f"{where}: 选项需 2–8 个且不能为空")
                if len(set(map(str, a))) != len(a):
                    errors.append(f"{where}: 选项重复")
                if t == "single" and not (isinstance(item.get("k"), int) and 0 <= item["k"] < len(a)):
                    errors.append(f"{where}: k 超出范围")
                if t == "multiple":
                    ci = item.get("correctIndices") or []
                    if not ci or any(not (0 <= i < len(a)) for i in ci):
                        errors.append(f"{where}: correctIndices 无效")
            if t == "boolean" and item.get("k") not in (0, 1):
                errors.append(f"{where}: 判断题 k 只能是 0（正确）或 1（错误）")
            if t == "text" and not [x for x in item.get("answers", []) if str(x).strip()]:
                errors.append(f"{where}: 填空题缺少 answers")
            qid = item.get("id")
            if qid:
                if qid in ids:
                    errors.append(f"{where}: id {qid} 与 {ids[qid]} 重复")
                ids[qid] = where
            key = q + "|" + "|".join(sorted(map(str, item.get("a") or item.get("answers") or [])))
            if item.get("level") and key in stems:
                errors.append(f"{where}: 与 {stems[key]} 重复：{q[:30]}")
            if item.get("level"):
                stems[key] = where
            stats[(stage_of(item.get("level")), item.get("subject", "-"), name)] += 1
    if "--stats" in sys.argv or not errors:
        for (stage, subject, name), count in sorted(stats.items()):
            print(f"{stage}\t{subject}\t{name}\t{count}")
    if errors:
        print("\n".join(errors), file=sys.stderr)
        print(f"共 {len(errors)} 处错误", file=sys.stderr)
        sys.exit(1)
    print(f"OK：{sum(stats.values())} 题，{len(ids)} 个 id")


if __name__ == "__main__":
    main()
