#!/usr/bin/env python3
"""Lightweight cross file audit for the AnimeKi sources.

There is no JDK available in some development sandboxes, so this script performs the checks a
compiler would catch most often inside this code base:

* every ``import com.animeki...`` resolves to a real file (and nested member)
* every ``AnimeKiServerConfig.X.Y`` / ``AnimeKiClientConfig.X.Y`` reference exists
* every enum / registry constant reference (``VfxEvent.HEAVY_HIT``) exists
* every static helper call (``KiService.drain(...)``, ``MathUtil.clamp(...)``) exists

Usage::

    python3 tools/java_audit.py [source-root]
"""
from __future__ import annotations

import glob
import os
import re
import sys

IDENT = r"[A-Za-z_]\w*"
TYPE = r"[\w<>\[\],\.\?]+"

IMPORT_RE = re.compile(r"^import\s+(com\.animeki\.[\w\.]+);", re.M)
FIELD_RE = re.compile(
    r"\b(?:public|protected|private)\s+(?:static\s+)?(?:final\s+)?[^=;()]*?\b(%s)\s*(?:=|;)" % IDENT
)
CONSTANT_RE = re.compile(r"\b(?:public|protected|private)\s+(?:static\s+)?(?:final\s+)?[^=;()]*?\b([A-Z][A-Z0-9_]*)\s*=")
METHOD_RE = re.compile(r"\b(?:public|protected|private)\s+(?:static\s+)?(?:final\s+)?[\w<>\[\],\.\? ]+\s+(%s)\s*\(" % IDENT)
ENUM_RE = re.compile(r"^\s{4}([A-Z][A-Z0-9_]*)\s*[(,;]", re.M)
NESTED_RE = re.compile(r"\b(?:public|protected|private)\s+(?:static\s+)?(?:final\s+)?(?:abstract\s+)?(?:class|interface|enum|record)\s+(\w+)")
COMMENT_RE = re.compile(r"//[^\n]*|/\*[\s\S]*?\*/")

CONST_CLASSES = {
    "VfxEvent": "vfx/VfxEvent.java",
    "CameraEffectType": "vfx/CameraEffectType.java",
    "ModSounds": "registry/ModSounds.java",
    "ModDamageTypes": "registry/ModDamageTypes.java",
    "ModAttachments": "registry/ModAttachments.java",
    "ModItems": "registry/ModItems.java",
    "ModEntities": "registry/ModEntities.java",
    "ModTags": "registry/ModTags.java",
    "AbilityAction": "ability/AbilityAction.java",
    "AbilityStopReason": "ability/AbilityStopReason.java",
    "FeedbackReason": "ability/FeedbackReason.java",
    "AttackKind": "combat/AttackKind.java",
    "BossPhase": "boss/BossPhase.java",
    "BossAbility": "boss/BossAbility.java",
    "BeamTypes": "beam/BeamTypes.java",
    "TransformationPhase": "transformation/TransformationPhase.java",
}

HELPER_CLASSES = {
    "MathUtil": "util/MathUtil.java",
    "Targeting": "util/Targeting.java",
    "KiMath": "ki/KiMath.java",
    "KiService": "ki/KiService.java",
    "CombatService": "combat/CombatService.java",
    "FlightService": "flight/FlightService.java",
    "DashService": "flight/DashService.java",
    "TransformationService": "transformation/TransformationService.java",
    "Transformations": "transformation/Transformations.java",
    "AbilityService": "ability/AbilityService.java",
    "AbilityRegistry": "ability/AbilityRegistry.java",
    "BeamService": "beam/BeamService.java",
    "DestructionService": "destruction/DestructionService.java",
    "DestructionProfile": "destruction/DestructionProfile.java",
    "VfxDispatcher": "vfx/VfxDispatcher.java",
    "ModAttachments": "registry/ModAttachments.java",
    "AnimeKiNetwork": "network/AnimeKiNetwork.java",
    "ModDamageTypes": "registry/ModDamageTypes.java",
    "ModSounds": "registry/ModSounds.java",
    # Client side helpers: same check, so a renamed HUD/render helper is caught without compiling.
    "ClientState": "client/ClientState.java",
    "CameraManager": "client/camera/CameraManager.java",
    "ClientVfx": "client/vfx/ClientVfx.java",
    "ClientInput": "client/input/ClientInput.java",
    "KeyBindings": "client/KeyBindings.java",
    "ClientPacketHandler": "client/net/ClientPacketHandler.java",
    "KiHud": "client/hud/KiHud.java",
    "RenderGeometry": "client/render/RenderGeometry.java",
    "GlowRenderType": "client/render/GlowRenderType.java",
    "AuraRenderer": "client/render/AuraRenderer.java",
    "BeamRenderer": "client/render/BeamRenderer.java",
    "ImpactRenderer": "client/render/ImpactRenderer.java",
    "WorldVfxRenderer": "client/render/WorldVfxRenderer.java",
    "AnimeKiClient": "client/AnimeKiClient.java",
}


def load(root: str) -> dict[str, str]:
    files: dict[str, str] = {}
    for path in glob.glob(os.path.join(root, "**", "*.java"), recursive=True):
        files[os.path.relpath(path, root)] = open(path, encoding="utf-8").read()
    return files


def resolve(files: dict[str, str], fq: str) -> tuple[str | None, list[str]]:
    if not fq.startswith("com.animeki."):
        return None, []
    parts = fq[len("com.animeki."):].split(".")
    for i in range(len(parts), 0, -1):
        rel = "/".join(parts[:i]) + ".java"
        if rel in files:
            return rel, parts[i:]
    return None, []


def main() -> int:
    root = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), "..", "src", "main", "java", "com", "animeki")
    root = os.path.abspath(root)
    files = load(root)

    members: dict[str, dict[str, set[str]]] = {}
    for rel, src in files.items():
        clean = COMMENT_RE.sub("", src)
        members[rel] = {
            "all": set(FIELD_RE.findall(clean)) | set(METHOD_RE.findall(clean)) | set(ENUM_RE.findall(clean))
                   | set(CONSTANT_RE.findall(clean)),
            "nested": set(NESTED_RE.findall(clean)),
        }

    problems: list[str] = []

    # 1. imports
    for rel, src in files.items():
        for imp in IMPORT_RE.findall(src):
            target, rest = resolve(files, imp)
            if target is None:
                problems.append(f"IMPORT {rel}: cannot resolve {imp}")
            elif rest and not all(part in members[target]["nested"] for part in rest):
                problems.append(f"IMPORT {rel}: nested member missing for {imp}")

    # 2. config usage
    for cfg, simple in (("config/AnimeKiServerConfig.java", "AnimeKiServerConfig"),
                        ("config/AnimeKiClientConfig.java", "AnimeKiClientConfig")):
        if cfg not in files:
            problems.append(f"MISSING {cfg}")
            continue
        names = members[cfg]["all"]
        pattern = re.compile(re.escape(simple) + r"((?:\.[A-Za-z_]\w*)+)")
        for rel, src in files.items():
            body = IMPORT_RE.sub("", src)
            for match in pattern.finditer(body):
                text = match.group(0)
                for access in re.finditer(r"\.([A-Za-z_]\w*)", text):
                    end = match.start() + access.end()
                    if body[end:end + 1] == "(":  # method call, not a config property
                        continue
                    name = access.group(1)
                    if name[0].islower() and name not in names:
                        problems.append(f"CONFIG {rel}: {text} -> {name} not declared")

    # 3. constants
    for cls, rel in CONST_CLASSES.items():
        if rel not in files:
            problems.append(f"MISSING {rel}")
            continue
        declared = members[rel]["all"] | members[rel]["nested"]
        pattern = re.compile(r"\b" + cls + r"\.([A-Z][A-Z0-9_]*)\b")
        for other, src in files.items():
            body = IMPORT_RE.sub("", src)
            for match in pattern.finditer(body):
                if match.group(1) not in declared:
                    problems.append(f"CONST {other}: {cls}.{match.group(1)} not declared in {rel}")

    # 4. static helper calls
    for cls, rel in HELPER_CLASSES.items():
        if rel not in files:
            problems.append(f"MISSING {rel}")
            continue
        declared = members[rel]["all"] | members[rel]["nested"]
        pattern = re.compile(r"\b" + cls + r"\.([a-z]\w*)\s*\(")
        for other, src in files.items():
            if other == rel:
                continue
            body = IMPORT_RE.sub("", src)
            for match in pattern.finditer(body):
                if match.group(1) not in declared:
                    problems.append(f"CALL {other}: {cls}.{match.group(1)}() missing in {rel}")

    # 5. instance method calls on our own types (approx: local/param/field type tracking)
    methods_by_class, parents, class_file = {}, {}, {}
    for rel, src in files.items():
        clean = COMMENT_RE.sub("", src)
        simple = os.path.basename(rel)[:-5]
        declared = set(METHOD_RE.findall(clean))
        for record in re.finditer(r"\brecord\s+\w+\s*\(([^)]*)\)", clean):
            for component in record.group(1).split(","):
                parts = component.strip().split()
                if len(parts) >= 2:
                    declared.add(parts[-1])
        if "enum " in clean:
            declared |= {"values", "valueOf", "name", "ordinal", "getSerializedName"}
        declared |= members.get(rel, {}).get("nested", set())
        methods_by_class[simple] = declared
        class_file[simple] = rel
        # package private / modifier-less members are legal too
        for member in re.finditer(r"(?:^|[;{\s])([\w<>\[\],\.\?]+)\s+([a-z]\w*)\s*\(", clean):
            declared.add(member.group(2))
        match = re.search(r"\b(?:class|interface|record|enum)\s+%s\b[^{]*?\bextends\s+([A-Z]\w*)" % simple, clean)
        parents[simple] = match.group(1) if match else None

    def has_method(cls, name, seen=None):
        seen = seen or set()
        if cls in seen:
            return False
        seen.add(cls)
        if cls in methods_by_class and name in methods_by_class[cls]:
            return True
        parent = parents.get(cls)
        if not parent:
            return False
        if parent not in methods_by_class:
            # Inherits from vanilla (or an unknown type): assume the method exists there.
            return True
        return has_method(parent, name, seen)

    INSTANCE_SKIP = {"get", "set", "equals", "hashCode", "toString", "ordinal", "name", "clone"}
    for rel, src in files.items():
        clean = COMMENT_RE.sub("", src)
        variables = {}
        for pattern, group in (
                (r"\b(?:final\s+)?([A-Z]\w*)(?:<[^;=(){}]*>)?\s+([a-z]\w*)\s*[=;,)]", 2),
                (r"\binstanceof\s+([A-Z]\w*)\s+([a-z]\w*)", 2),
                (r"\bfor\s*\(\s*(?:final\s+)?([A-Z]\w*)(?:<[^>]*>)?\s+([a-z]\w*)\s*:", 2),
                (r"\bcatch\s*\(\s*([A-Z]\w*)\s+([a-z]\w*)\s*\)", 2),
        ):
            for match in re.finditer(pattern, clean):
                variables.setdefault(match.group(group), match.group(1))
        for match in re.finditer(r"\bvar\s+([a-z]\w*)\s*=\s*new\s+([A-Z]\w*)", clean):
            variables.setdefault(match.group(1), match.group(2))
        # ``var x = SomeClass.factory(...)`` -> the declared return type of the factory.
        for match in re.finditer(r"\bvar\s+([a-z]\w*)\s*=\s*([A-Z]\w*)\.([a-z]\w*)\s*\(", clean):
            holder = methods_by_class.get(match.group(2))
            if holder is None or match.group(2) not in methods_by_class:
                continue
            source = files.get(class_file.get(match.group(2), rel), "")
            declared = re.search(
                r"([\w<>\[\],\.\?]+)\s+" + re.escape(match.group(3)) + r"\s*\(", source)
            if declared:
                candidate = re.sub(r"<.*", "", declared.group(1)).strip().split(".")[-1].strip()
                if candidate in methods_by_class:
                    variables[match.group(1)] = candidate
        for match in re.finditer(r"\.([a-z]\w*)\s*\(", clean):
            pass
        for match in re.finditer(r"\b([a-z]\w*)\.([a-z]\w*)\s*\(", clean):
            variable, method = match.group(1), match.group(2)
            if method in INSTANCE_SKIP:
                continue
            cls = variables.get(variable)
            if not cls or cls not in methods_by_class:
                continue
            if not has_method(cls, method):
                problems.append(f"METHOD {rel}: {variable}.{method}() not found on {cls}")

    # 6. argument counts for our own constructors and methods
    def split_args(text):
        text = re.sub(r"<[^<>()]*>", "", text)
        args, depth, current = [], 0, []
        for char in text:
            if char in "([{":
                depth += 1
            elif char in ")]}":
                depth -= 1
            if char == "," and depth == 0:
                args.append("".join(current).strip())
                current = []
            else:
                current.append(char)
        tail = "".join(current).strip()
        if tail:
            args.append(tail)
        return args

    def arity(params):
        params = params.strip()
        if not params:
            return 0
        return len(split_args(params))

    constructor_arities, method_arities = {}, {}
    for rel, src in files.items():
        clean = COMMENT_RE.sub("", src)
        simple = os.path.basename(rel)[:-5]
        arities = set()
        for match in re.finditer(r"\b" + re.escape(simple) + r"\s*\(([^()]*(?:\([^()]*\)[^()]*)*)\)\s*\{", clean):
            arities.add(arity(match.group(1)))
        constructor_arities[simple] = arities
        methods = {}
        for match in re.finditer(r"\b([A-Za-z_]\w*)\s*\(([^()]*(?:\([^()]*\)[^()]*)*)\)\s*(?:\{|throws|;)", clean):
            name = match.group(1)
            params = match.group(2)
            if name in ("if", "for", "while", "switch", "catch", "return", "new", "synchronized"):
                continue
            methods.setdefault(name, set()).add(("varargs" if "..." in params else arity(params)))
        for record in re.finditer(r"\brecord\s+\w+\s*\(([^)]*)\)", clean):
            for component in split_args(record.group(1)):
                parts = component.strip().split()
                if len(parts) >= 2:
                    methods.setdefault(parts[-1].split("[")[0], set()).add(0)
        method_arities[simple] = methods

    def arity_ok(kind, candidates, name, count):
        for (rel_key, simple) in candidates:
            if kind == "ctor":
                declared = constructor_arities.get(simple, set())
            else:
                declared = method_arities.get(simple, {}).get(name, set())
            if "varargs" in declared:
                return True
            if count in {value for value in declared if value != "varargs"}:
                return True
        return False

    def count_call_args(text, open_index):
        depth, index = 1, open_index + 1
        while index < len(text) and depth > 0:
            if text[index] == "(":
                depth += 1
            elif text[index] == ")":
                depth -= 1
            index += 1
        return arity(text[open_index + 1:index - 1])

    def candidates_for(name):
        return [(rel, os.path.basename(rel)[:-5]) for rel in files
                if os.path.basename(rel)[:-5] == name]

    # 6a. constructors of our own classes
    for rel, src in files.items():
        clean = COMMENT_RE.sub("", src)
        for match in re.finditer(r"\bnew\s+([A-Z]\w*)\s*\(", clean):
            simple = match.group(1)
            candidates = candidates_for(simple)
            if not candidates:
                continue
            if re.search(r"\b(?:class|record|enum)\s+" + simple + r"\b", clean):
                continue  # a nested class in the same file handles this constructor
            count = count_call_args(clean, match.end() - 1)
            if not arity_ok("ctor", candidates, simple, count):
                declared = sorted(
                    {value for (_, key) in candidates
                     for value in constructor_arities.get(key, set()) if value != "varargs"})
                problems.append(f"ARGS {rel}: new {simple}(...) called with {count} args, declared {declared}")

    # 6b. static calls on our own classes
    static_names = {os.path.basename(rel)[:-5] for rel in files}
    for rel, src in files.items():
        clean = COMMENT_RE.sub("", src)
        for match in re.finditer(r"\b([A-Z]\w*)\.([a-z]\w*)\s*\(", clean):
            simple, name = match.group(1), match.group(2)
            if simple not in static_names:
                continue
            candidates = candidates_for(simple)
            declared_all = method_arities.get(simple, {}).get(name)
            if not declared_all:
                continue
            count = count_call_args(clean, match.end() - 1)
            if not arity_ok("method", candidates, name, count):
                problems.append(f"ARGS {rel}: {simple}.{name}(...) called with {count} args, declared "
                                f"{sorted(value for value in declared_all if value != 'varargs')}")

    # 6c. instance calls on tracked variables
    for rel, src in files.items():
        clean = COMMENT_RE.sub("", src)
        variables = {}
        for pattern in (
                r"\b(?:final\s+)?([A-Z]\w*)(?:<[^;=(){}]*>)?\s+([a-z]\w*)\s*[=;,)]",
                r"\binstanceof\s+([A-Z]\w*)\s+([a-z]\w*)",
                r"\bfor\s*\(\s*(?:final\s+)?([A-Z]\w*)(?:<[^>]*>)?\s+([a-z]\w*)\s*:",
                r"\bvar\s+([a-z]\w*)\s*=\s*new\s+([A-Z]\w*)",
        ):
            for match in re.finditer(pattern, clean):
                if pattern.startswith("\\bvar"):
                    variables.setdefault(match.group(1), match.group(2))
                else:
                    variables.setdefault(match.group(2), match.group(1))
        for match in re.finditer(r"\bvar\s+([a-z]\w*)\s*=\s*([A-Z]\w*)\.([a-z]\w*)\s*\(", clean):
            source_rel = None
            for rel_key, simple in candidates_for(match.group(2)):
                source_rel = rel_key
                break
            if source_rel is None:
                continue
            declared = re.search(r"([\w<>\[\],\.\?]+)\s+" + re.escape(match.group(3)) + r"\s*\(",
                                 files[source_rel])
            if declared:
                candidate = re.sub(r"<.*", "", declared.group(1)).strip().split(".")[-1].strip()
                if candidate in static_names:
                    variables[match.group(1)] = candidate
        for match in re.finditer(r"\b([a-z]\w*)\.([a-z]\w*)\s*\(", clean):
            variable, name = match.group(1), match.group(2)
            cls = variables.get(variable)
            if not cls or cls not in method_arities:
                continue
            declared_all = method_arities[cls].get(name)
            if not declared_all:
                continue
            count = count_call_args(clean, match.end() - 1)
            if not arity_ok("method", [(rel, cls)], name, count):
                problems.append(f"ARGS {rel}: {variable}.{name}(...) called with {count} args, declared "
                                f"{sorted(value for value in declared_all if value != 'varargs')}")

    print(f"audited {len(files)} java files in {root}")
    if problems:
        unique = sorted(set(problems))
        print(f"== {len(unique)} unique problems ==")
        for problem in unique:
            print(" -", problem)
        return 1
    print("no cross reference problems found")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
