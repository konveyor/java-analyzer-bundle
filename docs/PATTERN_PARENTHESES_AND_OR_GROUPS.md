# Parentheses in patterns: method parameters vs. alternation groups

This document explains how the Java analyzer treats parentheses in search patterns today, why supported alternation patterns such as `org.library.(Class1|Class2)` break under `CONSTRUCTOR_CALL` (and under `METHOD_CALL` when a similar alternation-only type pattern is used), and how to fix that without losing parameter-specific matching. It does **not** cover `pkg.(A|B).methodName`—that shape is unsupported.

## Problem statement

Rules often express “any of several types in the same package” using alternation inside parentheses, for example:

- `org.library.(Class1|Class2)` for constructors

Alternation is **only** supported in the type segment (package + one parenthesized list of simple class names). Patterns that alternate types and then name a method, e.g. `org.library.(Class1|Class2).someMethod`, are **not** supported and are out of scope for this document and for the intended rule syntax.

After recent work to support **method/constructor parameter lists** in patterns (e.g. `com.example.Foo.bar(java.lang.String, int)`), validation in `CustomASTVisitor` started **stripping every `(...)` segment** from the pattern before comparing it to the fully qualified name from the AST. That is correct for a **trailing parameter list**, but it is wrong for **`(A|B)` alternation**, which is meant to stay in the pattern as a regular-expression group.

So: **`extractParameterTypes` already distinguishes “method params” from “alternation” in some cases, but the follow-up step that builds the regex for FQN matching does not.** It assumes any parenthesis is disposable, which matches the mental model “parentheses = method arguments” but fails for “parentheses = OR of class names”.

---

## End-to-end flow (where the query string goes)

### 1. Search pattern construction (`SampleDelegateCommandHandler.mapLocationToSearchPatternLocation`)

If the query matches `.*\(.*\|.*\).*`, the handler treats it as an **OR list**:

- It takes the substring between the **first** `(` and the **first** `)`.
- It splits on `|`.
- It builds a JDT `SearchPattern` that is the OR of each expanded query (e.g. `org.library.Class1`, `org.library.Class2`).

**How many alternatives?** Yes: this works for **more than two** class names in the same group. The implementation splits the inner string on `|`, so `(Class1|Class2|Class3)` becomes three concrete patterns OR’d together. (That is about **alternatives in the group**, not about Java method parameter lists.)

So the **search engine** can still find references for each concrete type. This path is largely independent of the AST visitor.

**Nested parentheses:** Not supported. Patterns such as `java.io.((FileWriter|FileReader))*` are invalid for this feature; only a **single** pair of parentheses wrapping the `|` list is in scope (e.g. `pkg.(A|B)`). The code today uses the first `(` and first `)`, which is wrong for nested forms—documented here as unsupported rather than something to extend.

### 2. Requestor keeps the **original** query (`SymbolInformationTypeRequestor`)

`search(...)` passes the **unexpanded** user query into `SymbolInformationTypeRequestor` as `this.query`. Symbol providers (`MethodCallSymbolProvider`, `ConstructorCallSymbolProvider`, …) pass that same string into `CustomASTVisitor`.

So for `org.library.(Class1|Class2)`, the visitor still sees the string **with** `(Class1|Class2)`, even though the underlying `SearchPattern` was already expanded for the engine.

### 3. Pre-filter: qualification matching (`SymbolProvider.queryQualificationMatches`)

Used by `MethodCallSymbolProvider` before parsing the AST (when the query contains `.`).

Relevant line (conceptually):

- It removes parenthetical segments **only if they do not contain `|`**: `replaceAll("\\([^|]*\\)", "")`.

So alternation groups **`(Class1|Class2)` are preserved** here on purpose: they remain part of the string used for package/import/qualification heuristics. That is **inconsistent** with `CustomASTVisitor`, which removes **all** `(...)` groups regardless of `|`.

### 4. AST refinement (`CustomASTVisitor`)

This is where METHOD_CALL / CONSTRUCTOR_CALL results are accepted or rejected using bindings and `String.matches(...)`.

Constructor behavior (simplified):

1. **`extractParameterTypes(query)`**  
   - Finds the substring between the **first** `(` and the **last** `)`.  
   - If that inner text contains `|`, it returns `null` and documents that this is **regex alternation**, not a Java parameter list.  
   - If there is “significant” text after the closing `)` (other than `*`), it also returns `null`.  
   - Otherwise it parses a comma-separated parameter type list (with awareness of generics).

2. **Build `this.query` for matching**  
   - Today: `processedQuery = query.replaceAll("\\([^)]*\\)", "");`  
   - Then: unqualified `*` → `.*` for Java regex.

3. **Visit nodes** and compare:

   - METHOD_CALL: `declaringClass.getQualifiedName() + "." + methodName` against `this.query` via `.matches(...)`.
   - CONSTRUCTOR_CALL: type FQN against `this.query`.

**Bug:** Step 2 removes **`(Class1|Class2)`** entirely, because `\([^)]*\)` matches from `(` up to the matching `)` with no nested parens. After removal, `org.library.(Class1|Class2)` becomes `org.library.`, which cannot match `org.library.Class1` or `org.library.Class2` as a full string.

**Contrast:** `extractParameterTypes` returning `null` for alternation means “no parameter filtering,” but the code still destroys the alternation when building `this.query`.

---

## Why parameter matching was added (context)

See `docs/PARAMETER_MATCHING.md`. The intent is to support patterns like:

`java.util.Properties.setProperty(java.lang.String, java.lang.String)`

For that, the implementation must:

1. Parse the trailing `(Type1, Type2, ...)`.
2. Remove **only that** parenthetical from the string used for FQN matching (so the left side compares to `java.util.Properties.setProperty`).
3. Use the parsed types with JDT bindings in `matchesParameterTypes`.

The regression is that removal was implemented as **“delete every `(...)`”** instead of **“delete the parameter list we successfully parsed.”**

---

## Additional edge case: `first` / `last` parentheses in `extractParameterTypes`

`extractParameterTypes` uses `indexOf('(')` and `lastIndexOf(')')`. That becomes ambiguous if a pattern ever combined **type alternation** with a **trailing parameter list** in one string. That combination is **not** a supported rule shape (see problem statement: no `pkg.(A|B).method`). For supported patterns—either alternation-only types **or** a normal `Type.method(types)` parameter list—the current heuristics remain the right scope.

If the grammar were extended in the future, a bracket-aware parse would be needed so the parameter list to strip is not conflated with an alternation group.

---

## Recommended fix (high level)

### A. Minimal, targeted fix (preserves current structure)

In `CustomASTVisitor`’s constructor:

1. **Do not** apply `replaceAll("\\([^)]*\\)", "")` unconditionally.

2. When **`extractParameterTypes` returns a non-null list** (including empty list for `method()`), remove **only** the parenthetical group that was identified as the **method/constructor parameter list**, not arbitrary groups.

   Practical approaches:

   - **Refactor `extractParameterTypes`** to return either the parameter list **and** the start/end indices of the removed segment (only when the segment is confidently “the trailing Java parameter list”), or  
   - **After a successful parse**, remove from the **last** `(` that opens the parameter list: for patterns that passed the existing “method params vs alternation” heuristics, the closing paren at `lastIndexOf(')')` that pairs with that parse is already the intended slice.

3. When **`extractParameterTypes` returns `null`** (no parameter list, or alternation / ambiguous case), **leave `query` unchanged** for the purpose of parenthesis stripping (still apply the existing `*` → `.*` normalization).

That restores `org.library.(Class1|Class2)` (and similarly any supported alternation-only type pattern) as a regex-friendly pattern for `.matches(...)`, while keeping parameter-specific matching for `Foo.bar(java.lang.String)`.

**Implemented:** (A) is in `CustomASTVisitor` via `parseParameterList`, `ParameterParseResult`, and `buildFqnRegexPattern`; unit coverage is in `CustomASTVisitorTest`. (C) is not implemented.

### B. Align semantics across the codebase

- **`queryQualificationMatches`** already avoids stripping groups that contain `|`. After (A), **`CustomASTVisitor`** will no longer strip those groups either; the two stages will agree.

### C. Longer-term hardening (optional; beyond current product scope)

1. **Balanced scanning** for a **trailing** parameter list only, if new grammar ever combines features in one pattern. Alternation + method name in one string remains explicitly unsupported.

2. **Escape literal dots** in user patterns if the intent is “segment separator” rather than “any character” in regex (today `*` is normalized but `.` is left as regex `.`; that predates this bug but affects precision).

3. **OR expansion in `mapLocationToSearchPatternLocation`**: if nested parentheses were ever required, the implementation would need balanced matching. Under the stated rule—**no nested parentheses**—the first `(` / first `)` approach is sufficient for `pkg.(A|B|C)`-style queries.

---

## Files involved

| Area | File | Role |
|------|------|------|
| OR search patterns | `SampleDelegateCommandHandler.java` | Expands `(A\|B)` for JDT search |
| AST match filter | `CustomASTVisitor.java` | `extractParameterTypes`, strips parens, `.matches` on FQN |
| Pre-filter (method calls) | `SymbolProvider.java` | `queryQualificationMatches` strips `\([^|]*\)` only |
| Wiring | `MethodCallSymbolProvider.java`, `ConstructorCallSymbolProvider.java` | Pass original `query` into visitor |
| Tests | `CustomASTVisitorTest.java` | Parameter extraction tests; add cases for alternation + FQN matching |

---

## Summary

| Intent | Example | `extractParameterTypes` | Current strip `(...)` | Correct behavior |
|--------|---------|-------------------------|------------------------|------------------|
| Alternation | `org.library.(C1\|C2)` | `null` (sees `\|`) | Removes group → broken | Do not strip |
| Method + params | `pkg.Type.m(java.lang.String)` | `["java.lang.String"]` | Removes param list (and any other `(...)` today) | Strip **only** param list |
| Method, no params | `pkg.Type.m()` | `[]` | Removes `()` | Strip **only** `()` |

The fix is to strip parentheses **only when they are the parsed Java parameter list**, not whenever they appear in the pattern. That matches the user’s model: **parentheses + `\|` → OR of names; parentheses + commas/type syntax at the end → method/constructor parameters.**
