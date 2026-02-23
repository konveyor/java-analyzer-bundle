# Plan: Fix annotation FQN resolution for on-demand imports when annotation elements are queried

## 1. Problem summary

When an annotation is used via an **on-demand import** (e.g. `import com.example.*;`) and the rule includes **annotation elements** (e.g. `annotated.elements`), the match fails.

**Root cause:** In `WithAnnotationQuery.getFQN(IAnnotation annotation)`, FQN resolution for a simple-name annotation only considers imports whose `getElementName()` **ends with** the annotation’s simple name. That works for single-type imports (`import com.example.MyAnnotation;` → `"com.example.MyAnnotation".endsWith("MyAnnotation")` ✓) but not for on-demand imports (`import com.example.*;` → `"com.example.*".endsWith("MyAnnotation")` ✗). So `getFQN` returns `""` for on-demand-imported annotations. Then `matchesAnnotation(fqn)` is false, we never call `doElementsMatch`, and the symbol is rejected even when the annotation and its elements would match.

**Scope:** The fix is confined to **resolving the annotation’s FQN** when it comes from an on-demand import. No change to element-matching logic (`doElementsMatch`) or to how the main search finds the annotatable.

---

## 2. Desired behavior after the fix

- **Single-type import** (unchanged): `import com.example.MyAnnotation;` → `getFQN` returns `"com.example.MyAnnotation"`.
- **On-demand import** (new): `import com.example.*;` and annotation simple name `"MyAnnotation"` → `getFQN` returns `"com.example.MyAnnotation"`.
- **FQN in source** (unchanged): If the annotation’s `getElementName()` is already a FQN (current logic treats “ends with dot” as FQN; we leave that as-is unless we find it problematic).
- **No matching import**: If neither a single-type nor an on-demand import resolves the name, keep returning `""` (current behavior).

Once FQN is correct, the existing flow applies: `matchesAnnotation(fqn)` can succeed and `doElementsMatch` runs for annotation-element rules.

---

## 3. Where to change

- **File:** `java-analyzer-bundle.core/src/main/java/io/konveyor/tackle/core/internal/symbol/WithAnnotationQuery.java`
- **Method:** `getFQN(IAnnotation annotation)` (private).

No changes to `AnnotationSymbolProvider`, `doElementsMatch`, or `matchesAnnotationQuery` signature/contract.

---
## 4. Approach: implement on-demand resolution in `getFQN`

There is **no existing helper** in the codebase to resolve a simple type/annotation name from a compilation unit’s imports (single-type or on-demand). So the fix will **implement that logic** where it’s needed: inside `WithAnnotationQuery.getFQN`, using the imports we already obtain via `tryToGetImports(annotation)`.

**Logic to add (on-demand fallback):**

- We already have the list of imports for the annotation’s compilation unit (from `tryToGetImports(annotation)`).
- Current code only considers an import a match when `import.getElementName().endsWith(name)` (single-type import).
- **Add:** When that yields no FQN, iterate the same imports again and handle on-demand imports: if `import.getElementName().endsWith(".*")`, then take the package prefix (e.g. `name.substring(0, name.length() - 2)` for `"com.example.*"`), and form the candidate FQN as `prefix + "." + simpleName` (e.g. `"com.example.MyAnnotation"`). Return the first such candidate (or we could define a deterministic order; “first” is acceptable for matching).
- **Order:** Prefer keeping the current single-type check first (so we don’t change behavior for single-type imports), then add the on-demand pass when single-type found nothing.

**Where to put the logic:** Either inline in `getFQN` (second loop over the same imports), or a small private helper in the same class, e.g. `resolveSimpleNameFromImports(List<IImportDeclaration> imports, String simpleName)` that returns the resolved FQN or null/empty. No new class or cross-package dependency.

---

## 5. Implementation plan (no code yet)

### 5.1 Change `getFQN` logic

1. **Keep** the current “already FQN” check: if `Pattern.matches(".*\\.", name)` then return `name` (no change).
2. **Keep** the current single-type resolution: from `tryToGetImports(annotation)`, find an import whose `getElementName()` ends with `name` and return that import’s name. (No change to this branch.)
3. **Add** on-demand fallback when the above yields no FQN:
   - Use the same imports list from `tryToGetImports(annotation)` (no need to get the CU again).
   - Iterate the imports; for each import whose `getElementName()` ends with `".*"`, take the package prefix (e.g. `elementName.substring(0, elementName.length() - 2)`), ensure it’s non-empty, then form candidate FQN `prefix + "." + name`. Return the first such candidate.
   - If no on-demand import produced a candidate, return `""` (current behavior).

Implementation can be a second stream over the same imports, or a small private helper in the same class that takes `(List<IImportDeclaration> imports, String simpleName)` and returns the resolved FQN or empty string.

### 5.2 Edge cases and behavior

- **Multiple on-demand imports:** Two packages could both have a type with the same simple name (e.g. `pkg1.*` and `pkg2.*`). We return the first on-demand match. For annotation matching we only need one valid FQN that matches the rule pattern; the first one is fine. If the rule is strict (e.g. exact FQN), only one would match anyway.
- **Null / exceptions:** If `tryToGetImports(annotation)` returns an empty list or throws, keep existing behavior: return `""`. No change to existing catch/logging in `tryToGetImports` unless we want to improve it.
- **Empty name:** If `annotation.getElementName()` is null or empty, short-circuit and return `""` before doing any import iteration.

### 5.3 Tests

- **New or extended test:** Add a scenario where:
  - A compilation unit has an on-demand import (e.g. `import javax.annotation.sql.*;`).
  - A type/method/field is annotated with an annotation from that package (e.g. `@DataSourceDefinition(...)` with at least one element set).
  - The rule uses ANNOTATION location, pattern matching that annotation’s FQN, and `annotated.elements` with name/value that match the source.
  - **Expected:** The symbol is returned (match succeeds). Without the fix, `getFQN` returns `""`, so the match fails.
- **Regression:** Existing tests that use single-type imports or FQN annotations should still pass; no change to `doElementsMatch` or other providers.

### 5.4 Documentation

- In `WithAnnotationQuery`, add a short Javadoc note on `getFQN` that FQN is resolved from the compilation unit’s imports, including on-demand imports (so that annotations from e.g. `import pkg.*;` are resolved to `pkg.AnnotationName`).

---

## 6. Summary

| Item | Decision |
|------|----------|
| **Change location** | `WithAnnotationQuery.getFQN` only. |
| **Strategy** | Implement on-demand resolution inside `getFQN`: when single-type resolution yields no FQN, iterate the same imports and for any import ending with `".*"` build `prefix + "." + simpleName` and return the first such FQN (inline or via a small private helper in the same class). |
| **Behavior** | After the fix, on-demand-imported annotations get a correct FQN, so `matchesAnnotation(fqn)` and `doElementsMatch` run and element-based rules work. |
| **Tests** | Add or extend a test: on-demand import + annotation with elements in the rule → match succeeds. |
| **Risk** | Low: localized change, no new dependencies, no API or contract change. |

---

## 7. Reviewer notes (for your annotations)

- [ ] Is placing the plan in `docs/PLAN.md` correct, or do you prefer another path?
- [ ] Prefer implementing the on-demand loop inline in `getFQN` or in a small private helper (e.g. `resolveSimpleNameFromImports(List<IImportDeclaration>, String)`) in the same class?
- [ ] Any existing test class you want this covered in (e.g. `AnnotationQueryTest`, `AnnotationSymbolProvider`-related tests)?
