# Product Overview

## Problem

Curious users frequently save articles, Wikipedia pages, essays, and PDFs but fail to consume them later. Conventional bookmark tools preserve a URL but do not preserve a reliable reading experience.

## Product promise

Save something interesting now. Read it later in a clean, private library, even when the original page is inconvenient or unavailable.

## Target user

The primary user is a self-hosting enthusiast who:

- Reads broadly across technology, science, history, culture, and general knowledge.
- Saves more content than they can immediately read.
- Values ownership and privacy.
- Uses a desktop browser and may access the library from multiple devices.
- Wants a calm reading experience rather than a productivity dashboard.

## Core concepts

1. **Content item** — The user-facing library entry.
2. **Artifact** — The immutable retained representation of content.
3. **Source** — The original URL or local document origin.
4. **Adapter** — The source-specific capture strategy.
5. **Reader representation** — The normalized data rendered by the frontend.
6. **Reading state** — User-specific status and progress.

## Product principles

- Capture should be low friction.
- The saved artifact is more important than the live source.
- The reader must not depend on the original site being available.
- The backend must not blindly trust uploaded HTML.
- Source-specific behavior must be isolated behind adapters.
- The system should fail transparently rather than claim a low-quality capture succeeded.
- User data must remain private by default.
- The library must remain portable and backup-friendly.
