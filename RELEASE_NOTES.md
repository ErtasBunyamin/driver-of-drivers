# Release Notes

## v0.2.0 (2026-01-12)

### ✨ New Features

#### Telemetry Emitter
- New `TelemetryListener` interface for custom integrations
- `JsonFileTelemetryListener` generates `hub-telemetry.json` automatically
- Events: `TEST_PASSED`, `TEST_FAILED`, `TEST_STARTED`, `TEST_SKIPPED`
- Configurable via `hub.telemetry.enabled`

#### Artifact Management
- `ArtifactManager` interface for extensible storage strategies
- `LocalFileSystemArtifactManager` default implementation
- Configurable policies: `ALWAYS`, `ON_FAILURE`, `NEVER`
- Screenshot capture integrated with JUnit 5 lifecycle

#### Performance Optimizations
- **Blocking Driver Pool**: Prevents resource exhaustion in CI/CD
- **Lazy Proxy**: Deferred browser initialization until first command
- Thread-safe `HubContext` with `ThreadLocal` storage

### 🔧 Improvements
- Professional README with Maven/Gradle installation snippets
- Maven Central badges and documentation
- HubProperties expanded with telemetry and artifact configurations
- SLF4J logging replaced System.out in HubExtension

### 📦 Dependencies
- Jackson JSR310 module for JSON date/time serialization

---

## v0.1.0 (2025-12-01)

### 🎉 Initial Release

#### Core Framework
- `HubWebDriver` unified facade API
- `HubWebElement` and `HubBy` abstractions
- Multi-provider architecture

#### Providers
- **Selenium Provider**: Full WebDriver 4.x support
- **Playwright Provider**: Microsoft Playwright integration
- **Hybrid Provider**: Dual-engine via CDP (Chrome DevTools Protocol)

#### Spring Boot Integration
- `@HubTest` annotation for JUnit 5
- `@HubDriver` field injection
- Auto-configuration via `HubAutoConfiguration`
- `application.yml` declarative configuration

#### Features
- HybridProvider with Playwright capabilities (network mocking, tracing, screenshots)
- Thread-safe parallel execution support
- Grid/Cloud execution support

### 📦 Modules
- `hub-core`
- `hub-webdriver-facade`
- `hub-provider-selenium`
- `hub-provider-playwright`
- `hub-provider-hybrid`
- `hub-spring-boot-starter`
- `hub-samples`
