# Refractive Glass Dock, Olded Sepia Default, Dr. Sharma Profile & Clean Gated Validation Plan

This revised plan establishes seamless input validation where **the Continue or Save button remains inactive until all mandatory inputs (Display Name & Gender) are completed**, without explicitly writing "REQUIRED" in the section labels, alongside Aptabase gender logging, optical refraction on the floating dock, translucent blurred active pill indicator, **Olded (Sepia)** default theme, and **Dr. Sharma** name suggestions.

### User Review & Critical Decisions

> [!IMPORTANT]
> - **Clean Section Labels (No "REQUIRED" Tag)**:
>   - In Step 2 of `SetupWizardScreen.kt` and `UserProfileBottomSheet.kt`, field headers are styled cleanly: **"DISPLAY NAME"**, **"MBBS PROFESSIONAL YEAR"**, **"GENDER"**, and **"INSTITUTION · OPTIONAL"**. The word "REQUIRED" will **not** be displayed.
> - **Inactive Button Validation Gating**:
>   - In the Setup Wizard, the "Continue" CTA button stays inactive (`enabled = false`) until the user fills in both Display Name (`displayName.trim().isNotEmpty()`) and Gender (`gender.trim().isNotEmpty()`). It activates dynamically with the primary glass glow as soon as the inputs are satisfied.
>   - In the Profile Settings bottom sheet, the "Save Profile" button similarly remains inactive until valid entries are provided.
> - **Aptabase Telemetry for Gender**:
>   - `MulberryAnalytics.logProfileRegistered(name, college, prof, gender)` and `MulberryAnalytics.setUserContext(name, college, prof, gender)` report student gender directly into Aptabase event properties for `"user_onboarded"` and subsequent lifecycle events.
> - **Dock Optical Refraction & Translucent Pill**:
>   - Add an optical prism refraction border brush and caustic highlight sheen to `ElasticGlassBottomBar`.
>   - Transform the active indicator pill to a translucent frosted glass lens (`colors.surfaceCard.copy(alpha = 0.55f)` with specular vertical gloss gradient and `1.dp` primary glow outline), allowing underlying dock and screen content to show through.
> - **Default Theme Shift to Olded (Sepia)**:
>   - Set default theme across `SettingsViewModel`, `MulberryViewModel`, `EphemeralMulberryViewModel`, `ReaderActivity`, and `Theme.kt` from `ThemeMode.PAPER` to `ThemeMode.OLDED`.
> - **Name Suggestion Update to "Dr. Sharma"**:
>   - Update text input placeholder/suggestion and fallback strings in `SetupWizardScreen.kt`, `UserProfileBottomSheet.kt`, `UserProfile.kt`, `UserProfileRepository.kt`, `StudyDashboardScreen.kt`, and `StudyShareHelper.kt` from "Sahabul" to "Dr. Sharma".

---

### 1. Overview & Core Concept

- **What It Delivers**:
  1. **Subtle & Enforced Validation**: Editorial typography without disruptive "REQUIRED" badges; validation is communicated cleanly by keeping the Continue / Save CTA inactive until Name and Gender are entered.
  2. **Gender Attribute & Telemetry**: Full persistence in `UserProfileRepository` and automated reporting to Aptabase.
  3. **Refractive Floating Dock**: Multi-stop chromatic refraction border with curved caustic reflection, coupled with a translucent blurred lens pill that blends into the active theme.
  4. **Olded (Sepia) Default Theme**: Immediate warm, paper-toned eye comfort for all new and unconfigured users.
  5. **Medical Academic Identity**: Standardized "Dr. Sharma" suggestions across input text fields.

---

### 2. User Experience & Visual Design

- **Profile Setup Wizard (Step 2)**:
  - Header 1: **DISPLAY NAME** (placeholder: `"Dr. Sharma"`, single-line text field)
  - Header 2: **MBBS PROFESSIONAL YEAR** (modal selector card)
  - Header 3: **GENDER** (single-line text field with placeholder `"e.g., Male, Female, Non-binary"`, leading icon `FluentIcons.Person24Regular`)
  - Header 4: **INSTITUTION · OPTIONAL** (placeholder: `"College or institution name"`)
  - **Dynamic CTA State**:
    - When either Name or Gender is empty: "Continue" CTA renders in inactive state (`FluidGlassPrimaryButton(enabled = false)` with muted colors and inactive click).
    - When both are provided: "Continue" CTA animates to active primary glass glow with spring bounce and haptic confirmation.
- **Refractive Floating Dock**:
  - Prism refraction border brush with subtle edge chromatic dispersion (`colors.primary.copy(0.30f)`, white rim highlight, subtle cyan/amber edge dispersion).
  - Translucent active indicator pill (`alpha = 0.55f`) with primary border outline and specular highlight.

---

### 3. Technical Architecture & Component Hierarchy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Mulberry UI & Telemetry                         │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│   ┌───────────────────────────────┐  ┌─────────────────────────────┐   │
│   │       BottomNavBar.kt         │  │     MulberryAnalytics.kt    │   │
│   │ - Refractive Prism Brush      │  │ - setUserContext(...,gender)│   │
│   │ - Caustic Top Rim Sheen       │  │ - logProfileRegistered(...) │   │
│   │ - Translucent Lens Pill       │  │ - Aptabase "gender" prop    │   │
│   │   (alpha = 0.55f + blur glow) │  └──────────────▲──────────────┘   │
│   └───────────────────────────────┘                 │                  │
│                                                     │                  │
│   ┌─────────────────────────────────────────────────┴──────────────┐   │
│   │             UserProfile & Persistence                          │   │
│   │ - UserProfile(..., gender: String = "")                        │   │
│   │ - UserProfileRepository (KEY_GENDER = "user_gender")           │   │
│   │ - Default ThemeMode.OLDED across ViewModels & ReaderActivity   │   │
│   └────────────────────────────────────────────────────────────────┘   │
│                                                     │                  │
│                                                     ▼                  │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │             Setup Wizard Step 2 (Clean Gated CTA)              │   │
│   │ - DISPLAY NAME (placeholder: "Dr. Sharma")                     │   │
│   │ - MBBS PROFESSIONAL YEAR                                       │   │
│   │ - GENDER (placeholder: "e.g., Male, Female, Non-binary")       │   │
│   │ - Continue CTA: enabled = (name.isNotBlank() && gender.isNot)  │   │
│   └────────────────────────────────────────────────────────────────┘   │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```

#### File Implementation Plan
1. **`UserProfile.kt` & `UserProfileRepository.kt`**:
   - Add `gender: String = ""` to `UserProfile`.
   - Update `UserProfileRepository` to store and retrieve `KEY_GENDER = "user_gender"`.
   - Change default fallback name from `"Sahabul"` to `"Dr. Sharma"`.
2. **`MulberryAnalytics.kt`**:
   - Add `cachedGender: String = "Not Specified"`.
   - Update `setUserContext` and `logProfileRegistered` to track `gender` to Aptabase.
3. **`SetupWizardScreen.kt`**:
   - Add `gender` state and `onGenderChange` to `ProfileSetupScreen`.
   - Add `"GENDER"` input field (clean label without "REQUIRED").
   - Compute `val canContinue = displayName.trim().isNotBlank() && gender.trim().isNotBlank()`.
   - Wire `FluidGlassPrimaryButton("Continue", enabled = canContinue)`.
   - In `PermissionsScreen.onLaunchApp`, pass `gender` to `MulberryAnalytics.logProfileRegistered`.
   - Change name placeholder to `"Dr. Sharma"`.
4. **`UserProfileBottomSheet.kt` & `SettingsScreen.kt`**:
   - Add `"GENDER"` field in profile editor with clean label.
   - Gate Save button to require non-blank name and gender.
   - Update placeholder from `"e.g., Sahabul"` to `"e.g., Dr. Sharma"`.
5. **`BottomNavBar.kt`**:
   - Add refractive prism brush and caustic sheen to `ElasticGlassBottomBar`.
   - Render active indicator pill with translucent glass (`colors.surfaceCard.copy(alpha = 0.55f)`), specular vertical gradient, and primary glow border.
6. **`Theme.kt`, `SettingsViewModel.kt`, `MulberryViewModel.kt`, `EphemeralMulberryViewModel.kt`, `ReaderActivity.kt`**:
   - Change default theme fallback to `ThemeMode.OLDED`.
7. **Verification**:
   - Run `compile_applet` to confirm clean compilation.
