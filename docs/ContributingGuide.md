# Contributing Guide

**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  
**Version:** 1.0  
**Date:** 2026-06-10  

[⬅ Back to README](../README.md) | [Next: Changelog](Changelog.md)

---

## 1. How to Contribute

We welcome contributions ranging from bug fixes and documentation improvements to new feature proposals.

### 1.1 Branching Strategy

We follow **GitFlow**.

- `main`: Stable production releases.
- `develop`: Integration branch for the next release.
- `feature/*`: For new features (e.g., `feature/voice-commands`).
- `bugfix/*`: For patches (e.g., `bugfix/ar-crash-fix`).

### 1.2 Pull Request (PR) Requirements

1. **Pass CI**: All PRs must pass the GitHub Actions CI pipeline (Lint and Unit Tests).
2. **Review**: At least one core maintainer must approve the PR.
3. **Documentation**: If your feature changes architecture, update the corresponding markdown file in `/docs`.

## 2. Local Environment Setup

See the [Deployment Guide](DeploymentGuide.md) for full setup instructions, including Firebase placeholder configuration.

## 3. Reporting Bugs

Use GitHub Issues. Please include:

1. Android OS Version.
2. Device Model.
3. Steps to reproduce.
4. Stacktrace (if applicable).
