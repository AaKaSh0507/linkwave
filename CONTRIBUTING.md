# Contributing to LinkWave

Thank you for your interest in contributing to LinkWave! This document provides guidelines and best practices for contributing to the project.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Pre-commit Hooks](#pre-commit-hooks)
- [Coding Standards](#coding-standards)
- [Commit Guidelines](#commit-guidelines)
- [Pull Request Process](#pull-request-process)
- [Testing](#testing)
- [Documentation](#documentation)

## Code of Conduct

By participating in this project, you agree to maintain a welcoming, inclusive, and harassment-free environment for everyone.

## Getting Started

1. Fork the repository
2. Clone your fork: `git clone https://github.com/YOUR_USERNAME/linkwave.git`
3. Add upstream remote: `git remote add upstream https://github.com/linkwave/linkwave.git`
4. Create a feature branch: `git checkout -b feature/your-feature-name`

## Development Setup

### Prerequisites

- **Java 21** (OpenJDK or Temurin)
- **Node.js 20+** and npm
- **Docker** and Docker Compose
- **Python 3.8+** (for pre-commit)
- **Git 2.28+**

### Quick Start

```bash
# Clone the repository
git clone https://github.com/linkwave/linkwave.git
cd linkwave

# Set up pre-commit hooks (REQUIRED)
./scripts/setup-pre-commit.sh

# Start development environment
make dev

# Services will be available at:
# - Frontend: http://localhost:3000
# - API: http://localhost:8080
# - API Docs: http://localhost:8080/swagger-ui.html
```

## Pre-commit Hooks

We use [pre-commit](https://pre-commit.com/) to ensure code quality before commits reach the repository.

### Installation

```bash
# Automated setup (recommended)
./scripts/setup-pre-commit.sh

# Or manual setup
pip install pre-commit
pre-commit install
pre-commit install --hook-type commit-msg
```

### What Gets Checked

| Category | Checks |
| -------- | ------ |
| **Universal** | Trailing whitespace, EOF newline, YAML/JSON syntax, large files |
| **Security** | Secrets detection (Gitleaks, detect-secrets), private keys |
| **Java** | Google Java Format, Checkstyle, no System.out.println |
| **TypeScript** | Prettier, ESLint, TypeScript compilation |
| **Commits** | Conventional commits format |
| **Docker** | Hadolint, Compose validation |
| **SQL** | SQLFluff linting |

### Running Hooks Manually

```bash
# Run all hooks on all files
pre-commit run --all-files

# Run specific hook
pre-commit run prettier --all-files
pre-commit run eslint --all-files
pre-commit run gitleaks --all-files

# Run on staged files only (what happens on commit)
pre-commit run
```

### Skipping Hooks (Emergency Only!)

```bash
# Skip all hooks
git commit --no-verify -m "emergency fix"

# Skip specific check in code
// eslint-disable-next-line
// pre-commit:ignore
```

> ⚠️ **Warning**: Only skip hooks in genuine emergencies. Skipped commits will still be checked in CI.

### Updating Hooks

```bash
pre-commit autoupdate
```

## Coding Standards

### Java (Backend)

- **Style**: Google Java Style Guide
- **Formatting**: Enforced by Google Java Format
- **Logging**: Use SLF4J, never `System.out.println`
- **Exceptions**: Never use `printStackTrace()`, use proper logging
- **Testing**: JUnit 5, meaningful test names

```java
// ✅ Good
@Slf4j
public class UserService {
    public User findById(Long id) {
        log.debug("Finding user with id: {}", id);
        return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
    }
}

// ❌ Bad
public class UserService {
    public User findById(Long id) {
        System.out.println("Finding user: " + id);  // Never do this
        try {
            return userRepository.findById(id).get();
        } catch (Exception e) {
            e.printStackTrace();  // Never do this
            return null;
        }
    }
}
```

### TypeScript (Frontend)

- **Style**: Prettier + ESLint
- **Formatting**: 2 spaces, single quotes, no semicolons
- **Imports**: Organized and sorted
- **Logging**: No `console.log` in production code

```typescript
// ✅ Good
import { useState, useEffect } from 'react'
import type { User } from '@/lib/types'

export function UserProfile({ userId }: { userId: string }) {
  const [user, setUser] = useState<User | null>(null)

  useEffect(() => {
    fetchUser(userId).then(setUser)
  }, [userId])

  return user ? <div>{user.name}</div> : <Loading />
}

// ❌ Bad
import {useState,useEffect} from "react";
import { User } from "@/lib/types";

export function UserProfile({ userId }) {
  console.log("Rendering user:", userId);  // No console.log
  // ...
}
```

## Commit Guidelines

We follow [Conventional Commits](https://www.conventionalcommits.org/) specification.

### Format

```text
type(scope): subject

[optional body]

[optional footer]
```

### Types

| Type | Description |
| ---- | ----------- |
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation only |
| `style` | Code style (formatting) |
| `refactor` | Code refactoring |
| `perf` | Performance improvement |
| `test` | Adding/updating tests |
| `build` | Build system changes |
| `ci` | CI/CD changes |
| `chore` | Maintenance tasks |
| `revert` | Reverting changes |

### Examples

```bash
# Features
git commit -m "feat(auth): add OAuth2 login support"
git commit -m "feat(chat): implement real-time message delivery"

# Bug fixes
git commit -m "fix(api): handle null user in profile endpoint"
git commit -m "fix(ui): correct button alignment on mobile"

# Documentation
git commit -m "docs(readme): update installation instructions"

# Refactoring
git commit -m "refactor(services): extract user validation logic"

# Breaking changes
git commit -m "feat(api)!: change authentication endpoint response format

BREAKING CHANGE: The /auth/login endpoint now returns a different JSON structure.
See migration guide: docs/migrations/auth-v2.md"
```

### Rules

- Subject line: max 72 characters
- Use imperative mood: "add feature" not "added feature"
- Don't end subject with period
- Separate subject from body with blank line

## Pull Request Process

### Before Creating a PR

1. ✅ All pre-commit hooks pass
2. ✅ Tests pass locally: `make test`
3. ✅ Code builds successfully: `make build`
4. ✅ Documentation updated if needed
5. ✅ Changelog updated for user-facing changes

### PR Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
Describe testing performed

## Checklist
- [ ] Pre-commit hooks pass
- [ ] Tests added/updated
- [ ] Documentation updated
- [ ] No console.log/System.out.println
```

### Review Process

1. Create PR against `develop` branch
2. Ensure CI passes
3. Request review from code owners
4. Address feedback
5. Squash and merge when approved

## Testing

### Backend Testing

```bash
cd backend
./gradlew test
./gradlew integrationTest
```

### Frontend Testing

```bash
cd frontend
npm test
npm run test:coverage
```

### Running All Tests

```bash
make test
```

## Documentation

- Update README.md for user-facing changes
- Add JSDoc/Javadoc for public APIs
- Update API documentation (Swagger annotations)
- Include migration guides for breaking changes

## Branch Naming

```text
feature/LW-123-user-authentication
bugfix/LW-456-fix-login-error
hotfix/LW-789-security-patch
chore/update-dependencies
docs/api-documentation
```

## Questions?

- Check existing issues and discussions
- Join our Discord/Slack community
- Create a GitHub Discussion for questions

---

Thank you for contributing to LinkWave! 🎉
