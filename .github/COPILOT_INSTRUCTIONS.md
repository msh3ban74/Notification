# Rafeeq Project Development Rules

# PROJECT NAME

Rafeeq

This is a real production Android application.

The goal is to build a beautiful AI-powered personal assistant.

------------------------------------------------------------

# RULE #1

Project stability is more important than adding new features.

Never introduce breaking changes.

If you are not 100% certain about a change, DO NOT change it.

------------------------------------------------------------

# NEVER MODIFY

Do NOT modify any of the following unless explicitly requested:

- package name
- applicationId
- namespace
- signing configs
- Gradle Wrapper
- gradlew
- gradle-wrapper.jar
- gradle.properties
- settings.gradle.kts
- GitHub Actions
- workflow files
- Firebase configuration
- google-services.json
- local.properties mechanism
- Secrets Gradle Plugin
- .env loading
- BuildConfig API loading
- API key loading
- Credential Manager configuration
- Google Sign In architecture
- Authentication architecture
- Existing Release signing

Never upgrade:

- Kotlin
- AGP
- Gradle
- Compose BOM
- dependencies

unless explicitly requested.

------------------------------------------------------------

# API KEYS

Never hardcode API keys.

Always preserve the existing mechanism.

Current project already loads:

- GEMINI_API_KEY from .env
- GOOGLE_WEB_CLIENT_ID from local.properties

Never replace this.

------------------------------------------------------------

# FIREBASE

Never remove:

- Firebase Auth
- Firestore
- Google Sign In
- Credential Manager
- Google Services plugin

If login fails:

Fix ONLY the bug.

Do not rewrite authentication.

------------------------------------------------------------

# BUILD

Every change must preserve successful Gradle build.

Never leave compilation errors.

Never remove imports only to silence errors.

Never delete files just because they appear unused.

------------------------------------------------------------

# UI PHILOSOPHY

The application must feel like a premium modern app.

Style inspiration:

- Notion
- TickTick
- Google Tasks
- Material 3
- Apple Human Interface

Use:

- large spacing
- rounded cards
- minimal design
- clean typography
- high contrast
- excellent accessibility

Avoid:

- crowded screens
- tiny buttons
- duplicated controls
- unnecessary tabs
- excessive colors

------------------------------------------------------------

# APP STRUCTURE

The application has only four main sections.

1. AI Assistant

2. Tasks

3. Schedule

4. Notifications

Everything else should be opened from these sections.

------------------------------------------------------------

# DESIGN PRINCIPLES

Each screen should have ONE primary purpose.

Every card should feel useful.

No empty decoration.

Every action should require as few taps as possible.

------------------------------------------------------------

# SMART CARDS

Cards should adapt depending on their type.

Examples:

Debt

Fields:

- lender
- amount
- due date
- repayment date
- notes

Savings Group (Gam3iya)

Fields:

- start date
- end date
- installment
- receive date
- members
- manager/member mode

Medicine

Fields:

- medicine name
- dosage
- reminder
- repeat
- notes

Bills

Fields:

- company
- amount
- due date
- reminder

Meeting

Fields:

- title
- location
- attendees
- reminder

Daily Habit

Fields:

- repeat
- goal
- progress

Each card type should have its own intelligent UI.

Do NOT reuse the same form for everything.

------------------------------------------------------------

# AI

AI is the core feature.

The AI Assistant should become the main entry point.

It should help users:

- create reminders
- create meetings
- create debts
- create savings groups
- answer questions
- organize life

------------------------------------------------------------

# CODE STYLE

Prefer:

small files

single responsibility

clean architecture

readable code

descriptive names

Avoid:

huge composables

duplicated code

magic numbers

hardcoded strings

------------------------------------------------------------

# BEFORE FINISHING

Always verify:

Project still compiles.

No existing feature is broken.

No authentication flow is broken.

No Gradle configuration changed.

No API key mechanism changed.

No Firebase configuration changed.

------------------------------------------------------------

# IMPORTANT

Never optimize by deleting features.

Never simplify by removing functionality.

Always improve while preserving compatibility.

The project owner manually pushes to GitHub.

The AI cannot verify GitHub Actions.

Therefore NEVER modify:

.github/workflows

unless explicitly instructed.

------------------------------------------------------------

# FINAL GOAL

Build the best AI personal organizer on Android.

Beautiful.

Fast.

Reliable.

Premium.

Production ready.
