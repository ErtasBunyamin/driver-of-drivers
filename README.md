# Hub Automation Framework: Enterprise Unified UI Testing Solution

[![Maven Central](https://img.shields.io/maven-central/v/io.github.ertasbunyamin/hub-spring-boot-starter.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:io.github.ertasbunyamin%20AND%20a:hub-spring-boot-starter)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://openjdk.java.net/)

[English](#en) | [Türkçe](#tr)

---

<h2 id="en">English Version</h2>

Hub is an enterprise-grade test automation solution designed to manage complex test scenarios by unifying **Selenium** and **Playwright** engines under a single abstraction layer. It aims to increase test sustainability and minimize vendor lock-in within the Software Development Life Cycle (SDLC).

### Architectural Overview and Strategic Advantages

Hub Framework is built on the principle of "Separation of Concerns," offering the following strategic benefits:

*   **Technology-Agnostic Facade API:** Test scripts are written using the `HubWebDriver` interface. This allows switching between Selenium and Playwright without modifying test code.
*   **Advanced Spring Boot Integration:** The `@HubTest` ecosystem brings Dependency Injection (DI) principles to the testing layer. Page Objects and services are fully compatible with Spring's lifecycle management.
*   **High Scalability:** The thread-safe `HubContext` architecture manages high-density parallel test executions (Grid, Cloud, or Local) in an isolated manner.
*   **Familiar WebDriver Patterns:** Even though Hub provides a modern abstraction, it retains the familiar WebDriver API patterns. Experienced automation engineers can adapt instantly without learning a new paradigm.
*   **Extensible Provider Model:** The framework architecture allows for easy integration of next-generation automation engines.

### Technical Module Specifications

| Module | Description |
| :--- | :--- |
| **`hub-core`** | Core logic, configuration models, and provider protocols. |
| **`hub-webdriver-facade`** | Unified cross-platform API definitions (WebDriver, WebElement, By). |
| **`hub-provider-selenium`** | Adapter implementation for the Selenium WebDriver engine. |
| **`hub-provider-playwright`** | Adapter implementation for the Microsoft Playwright engine. |
| **`hub-provider-hybrid`** | Dual-driver provider connecting Selenium + Playwright to the same session via CDP. |
| **`hub-spring-boot-starter`** | Autoconfiguration, bean management, and JUnit 5 extensions. |

### Installation

#### Maven
```xml
<!-- Core starter (required) -->
<dependency>
    <groupId>io.github.ertasbunyamin</groupId>
    <artifactId>hub-spring-boot-starter</artifactId>
    <version>0.3.1</version>
</dependency>

<!-- Choose your provider (at least one required) -->
<!-- Option 1: Selenium -->
<dependency>
    <groupId>io.github.ertasbunyamin</groupId>
    <artifactId>hub-provider-selenium</artifactId>
    <version>0.3.1</version>
</dependency>

<!-- Option 2: Playwright -->
<dependency>
    <groupId>io.github.ertasbunyamin</groupId>
    <artifactId>hub-provider-playwright</artifactId>
    <version>0.3.1</version>
</dependency>

<!-- Option 3: Hybrid (Selenium + Playwright via CDP) -->
<dependency>
    <groupId>io.github.ertasbunyamin</groupId>
    <artifactId>hub-provider-hybrid</artifactId>
    <version>0.3.1</version>
</dependency>
```

#### Gradle (Kotlin DSL)
```kotlin
implementation("io.github.ertasbunyamin:hub-spring-boot-starter:0.3.1")
implementation("io.github.ertasbunyamin:hub-provider-selenium:0.3.1") // or playwright/hybrid
```

### Quick Start

**1. Add `application.yml` to `src/test/resources`:**
```yaml
hub:
  provider: selenium
  browser: chrome
  headless: true
```

**2. Create your first test:**
```java
import com.dod.hub.facade.HubWebDriver;
import com.dod.hub.starter.junit.HubDriver;
import com.dod.hub.starter.junit.HubTest;
import org.junit.jupiter.api.Test;

@HubTest
public class MyFirstTest {

    @HubDriver
    private HubWebDriver driver;

    @Test
    void shouldOpenGoogle() {
        driver.get("https://www.google.com");
        assert driver.getTitle().contains("Google");
    }
}
```

**3. Run the test!** 🚀

### Configuration Options

| Property | Default | Description |
|:---|:---|:---|
| `hub.provider` | `selenium` | Engine: `selenium`, `playwright`, `hybrid` |
| `hub.browser` | `chrome` | Browser: `chrome`, `firefox`, `edge`, `webkit` |
| `hub.headless` | `false` | Run browser in headless mode |
| `hub.performance.lazy-init` | `false` | Defer driver creation until first use |
| `hub.performance.pooling.enabled` | `false` | Enable driver reuse across tests |
| `hub.performance.pooling.max-active` | `5` | Max concurrent pooled drivers |
| `hub.artifacts.path` | `target/hub-artifacts` | Screenshot output directory |
| `hub.artifacts.policy` | `ON_FAILURE` | Capture policy: `ALWAYS`, `ON_FAILURE`, `NEVER` |
| `hub.telemetry.enabled` | `true` | Emit test events to JSON |

### Development Patterns & Framework Support

#### 1. JUnit 5 (Spring Integrated)
```java
@HubTest
@SpringBootTest
public class MyJUnitTest {
    @HubDriver
    private HubWebDriver driver;

    @Test
    void testSearch() {
        driver.get("https://google.com");
    }
}
```

#### 2. TestNG (Manual Lifecycle)
```java
public class MyTestNGTest {
    private HubWebDriver driver;

    @BeforeMethod
    public void setup() {
        driver = HubFactory.create();
    }

    @Test
    public void testFlow() {
        driver.get("https://example.com");
    }

    @AfterMethod
    public void tearDown() {
        driver.quit();
    }
}
```

#### 3. Cucumber (BDD)
```java
@CucumberContextConfiguration
@SpringBootTest
public class StepDefinitions {
    @Autowired
    private HubWebDriver driver; // Managed proxy bean

    @Given("I am on the login page")
    public void step() {
        driver.get("https://site.com/login");
    }
}
```

#### 4. Spring-Aware Page Object Model
```java
@Component
@Scope("prototype")
public class LoginPage {
    @Autowired
    private HubWebDriver driver;

    @FindBy(id = "login-btn")
    private HubWebElement loginBtn;

    public void clickLogin() {
        loginBtn.click();
    }
}
```

### Advanced Features & Performance

#### Artifact Management
Hub provides an automated artifact collection system integrated with the JUnit 5 lifecycle.

| Policy | Description |
| :--- | :--- |
| `ALWAYS` | Captures artifacts for every test completion. |
| `ON_FAILURE` | Captures only when a test fails (Default). |
| `NEVER` | Disables artifact collection. |

**Extensible Storage Strategy:**
You can provide a custom `ArtifactManager` bean to store screenshots in S3, Azure, or custom cloud storage.

```java
@Bean
public ArtifactManager s3Manager() {
    return new S3ArtifactManager("my-bucket");
}
```

#### Telemetry Emitter
Hub emits structured test execution events for monitoring, reporting, and CI/CD integration.

```yaml
hub:
  telemetry:
    enabled: true  # Default: true
```

**Output:** A `hub-telemetry.json` file is created in the artifacts directory containing:

```json
[
  {
    "event": "TEST_PASSED",
    "timestamp": "2026-01-12T00:00:00Z",
    "testClass": "LoginTest",
    "testMethod": "shouldLoginSuccessfully",
    "durationMs": 1250
  }
]
```

**Custom Listeners:** Implement `TelemetryListener` for custom integrations (e.g., Datadog, Prometheus).

#### Performance & Scaling
Designed for high-concurrency environments like CI/CD pipelines.

*   **Blocking Driver Pool**: Prevents resource exhaustion by blocking test threads until a driver becomes available.
*   **Lazy Proxying**: Injects a proxy that only initializes the physical browser when a command (e.g., `driver.get()`) is actually called.
*   **Thread-Safe Context**: Uses `ThreadLocal` storage to ensure zero leakage between parallel threads.

### Remote Execution and Infrastructure Support
Hub supports hybrid cloud and on-premise Selenium Grid setups, as well as Playwright Connect scenarios. Custom browser capabilities can be configured both programmatically and decoratively.

---

### HybridProvider: Dual-Engine Power 🔀

Hub Framework introduces a revolutionary **HybridProvider** that connects both Selenium and Playwright to the **same browser session** via Chrome DevTools Protocol (CDP). This unique capability allows you to leverage the best features of both frameworks simultaneously.

#### Architecture
```
┌─────────────────────────────────────────────────────┐
│                  HybridProvider                     │
├─────────────────────────────────────────────────────┤
│  Browser Process (Chrome/Edge)                      │
│  └── CDP Endpoint: localhost:9222                   │
│       ├── Selenium WebDriver ──┐                    │
│       └── Playwright Page ─────┼──► HubWebDriver    │
│                                │                    │
│  PlaywrightCapabilities ───────┘                    │
│  (Auto-wait, Network Mock, Tracing, Dialogs)        │
└─────────────────────────────────────────────────────┘
```

#### Configuration
```yaml
hub:
  provider: hybrid  # Enables dual-driver mode
  browser: chrome
  headless: false
  provider-options:
    hybrid.cdp.port: 9222
    hybrid.playwright.autowait: true
```

#### Strategy Routing

| Operation | Engine | Rationale |
| :--- | :--- | :--- |
| `find` (wait phase) | **Playwright** | Auto-wait for element visibility |
| `find` (element ref) | Selenium | WebElement compatibility |
| `click`, `type` | Selenium | Mature, stable API |
| `screenshot` | **Playwright** | Full-page, high quality |
| `network mock` | **Playwright** | Native first-class support |

#### PlaywrightCapabilities API

Access advanced Playwright features through the `HybridSession`:

```java
@HubTest
@SpringBootTest
public class HybridTest {
    @HubDriver(provider = HubProviderType.HYBRID)
    private HubWebDriver driver;

    @Test
    void testWithPlaywrightPowers() {
        HybridSession session = (HybridSession) driver.getSession();
        PlaywrightCapabilities pw = session.playwright();

        // Network Interception
        pw.mockJsonRequest("**/api/users", "[{\"id\": 1}]");
        pw.blockRequests("**/analytics/**");

        // Dialog Handling
        pw.autoAcceptDialogs();

        // Console Logging
        pw.onConsoleMessage(msg -> System.out.println("Browser: " + msg));

        // Tracing (for debugging)
        pw.startTracing("my-test");

        driver.get("https://example.com");
        driver.findElement(By.id("btn")).click(); // Auto-wait enabled!

        // Full-page screenshot
        byte[] screenshot = pw.fullPageScreenshot();

        pw.stopTracing(Path.of("trace.zip"));
    }
}
```

#### Available Capabilities

| Method | Description |
| :--- | :--- |
| `waitForSelector(selector, timeout)` | Wait for element visibility |
| `waitForLoadState(state)` | Wait for `load`, `domcontentloaded`, `networkidle` |
| `waitForURL(pattern)` | Wait for URL navigation |
| `mockJsonRequest(pattern, json)` | Mock API with JSON response |
| `blockRequests(pattern)` | Block matching requests |
| `interceptRequests(pattern, handler)` | Custom request interception |
| `startTracing(name)` / `stopTracing(path)` | Record debug traces |
| `autoAcceptDialogs()` | Auto-accept alerts/confirms |
| `autoDismissDialogs()` | Auto-dismiss dialogs |
| `onConsoleMessage(handler)` | Capture browser console |
| `onPageError(handler)` | Capture page errors |
| `fullPageScreenshot()` | High-quality full-page capture |
| `elementScreenshot(selector)` | Screenshot specific element |
| `setGeolocation(lat, lng)` | Emulate location |
| `evaluate(js)` | Execute JavaScript |

> **Note:** HybridProvider only supports Chromium-based browsers (Chrome, Edge).

---

<h2 id="tr">Türkçe Versiyon</h2>

Hub, karmaşık test senaryolarını yönetmek üzere tasarlanmış, **Selenium** ve **Playwright** altyapılarını tek bir soyutlama katmanı (Abstraction Layer) altında birleştiren kurumsal düzeyde bir test otomasyonu çözümüdür. Yazılım geliştirme yaşam döngüsünde (SDLC) test sürdürülebilirliğini artırmak ve teknoloji bağımlılığını (Vendor Lock-in) minimize etmek amacıyla geliştirilmiştir.

### Mimari Bakış ve Stratejik Avantajlar

Hub Framework, "Separation of Concerns" (Sorumlulukların Ayrılması) prensibini temel alarak aşağıdaki stratejik avantajları sunar:

*   **Teknolojiden Bağımsız Facade API:** Test kodları, motor detaylarından arındırılarak `HubWebDriver` arayüzü ile yazılır. Bu sayede, test kodunda değişiklik yapmadan Selenium'dan Playwright'a geçiş imkanı sağlanır.
*   **Gelişmiş Spring Boot Entegrasyonu:** `@HubTest` ekosistemi sayesinde Dependency Injection (DI) prensipleri test katmanına taşınır. Page Object'ler ve servisler, Spring'in yaşam döngüsü yönetimi ile tam uyumlu çalışır.
*   **Yüksek Ölçeklenebilirlik:** Thread-safe `HubContext` mimarisi, yüksek yoğunluklu paralel test koşturmalarını (Grid, Cloud veya Local) izole bir şekilde yönetir.
*   **Aşina Olunan WebDriver Desenleri:** Hub modern bir soyutlama sağlasa da, standart WebDriver API desenlerini korur. Deneyimli otomasyon mühendisleri, yeni bir paradigma öğrenmek zorunda kalmadan mevcut alışkanlıklarıyla anında uyum sağlayabilir.
*   **Genişletilebilir Provider Yapısı:** Framework mimarisi, yeni nesil otomasyon motorlarının kolayca entegre edilmesine olanak tanır.

### Teknik Modül Spesifikasyonları

| Modül | Tanım |
| :--- | :--- |
| **`hub-core`** | Çekirdek mantık, konfigürasyon modelleri ve provider protokolleri. |
| **`hub-webdriver-facade`** | Platformlar arası ortak API tanımları (WebDriver, WebElement, By). |
| **`hub-provider-selenium`** | Selenium WebDriver motoru için adaptör uygulaması. |
| **`hub-provider-playwright`** | Microsoft Playwright motoru için adaptör uygulaması. |
| **`hub-provider-hybrid`** | Selenium + Playwright'ı CDP üzerinden aynı oturuma bağlayan çift motor sağlayıcı. |
| **`hub-spring-boot-starter`** | Otomatik konfigürasyon, bean yönetimi ve JUnit 5 uzantıları. |

### Kurulum

#### Maven
```xml
<!-- Ana starter (gerekli) -->
<dependency>
    <groupId>io.github.ertasbunyamin</groupId>
    <artifactId>hub-spring-boot-starter</artifactId>
    <version>0.2.0</version>
</dependency>

<!-- Provider seçin (en az biri gerekli) -->
<!-- Seçenek 1: Selenium -->
<dependency>
    <groupId>io.github.ertasbunyamin</groupId>
    <artifactId>hub-provider-selenium</artifactId>
    <version>0.2.0</version>
</dependency>
```

#### Gradle (Kotlin DSL)
```kotlin
implementation("io.github.ertasbunyamin:hub-spring-boot-starter:0.2.0")
implementation("io.github.ertasbunyamin:hub-provider-selenium:0.2.0")
```

### Hızlı Başlangıç

**1. `src/test/resources/application.yml` dosyası ekleyin:**
```yaml
hub:
  provider: selenium
  browser: chrome
  headless: true
```

**2. İlk testinizi oluşturun:**
```java
import com.dod.hub.facade.HubWebDriver;
import com.dod.hub.starter.junit.HubDriver;
import com.dod.hub.starter.junit.HubTest;
import org.junit.jupiter.api.Test;

@HubTest
public class IlkTestim {

    @HubDriver
    private HubWebDriver driver;

    @Test
    void googleAcilmali() {
        driver.get("https://www.google.com");
        assert driver.getTitle().contains("Google");
    }
}
```

**3. Testi çalıştırın!** 🚀

#### Merkezi Konfigürasyon Yönetimi
`application.yml` üzerinden deklaratif yönetim:

```yaml
hub:
  provider: selenium   # Seçenekler: selenium, playwright, hybrid
  browser: chrome      # Seçenekler: chrome, firefox, edge, webkit
  headless: true
  performance:
    lazy-init: true    # Sürücü oluşturmayı ilk kullanıma kadar erteler
    pooling:
      enabled: true    # Sürücü yeniden kullanımını etkinleştirir
      max-active: 5    # Maksimum eşzamanlı sürücü sayısı
  artifacts:
    path: target/hub-artifacts
    policy: ON_FAILURE # ALWAYS, ON_FAILURE, NEVER
```

### Geliştirme Desenleri ve Framework Desteği

#### 1. JUnit 5 (Spring Entegrasyonlu)
```java
@HubTest
@SpringBootTest
public class MyJUnitTest {
    @HubDriver
    private HubWebDriver driver;

    @Test
    void testSearch() {
        driver.get("https://google.com");
    }
}
```

#### 2. TestNG (Manuel Yaşam Döngüsü)
```java
public class MyTestNGTest {
    private HubWebDriver driver;

    @BeforeMethod
    public void setup() {
        driver = HubFactory.create();
    }

    @Test
    public void testFlow() {
        driver.get("https://example.com");
    }

    @AfterMethod
    public void tearDown() {
        driver.quit();
    }
}
```

#### 3. Cucumber (BDD - İş Odaklı Test)
```java
@CucumberContextConfiguration
@SpringBootTest
public class StepDefinitions {
    @Autowired
    private HubWebDriver driver; // Yönetilen proxy bean

    @Given("Giriş sayfasındayım")
    public void step() {
        driver.get("https://site.com/login");
    }
}
```

#### 4. Spring-Uyumlu Page Object Modeli
```java
@Component
@Scope("prototype")
public class LoginPage {
    @Autowired
    private HubWebDriver driver;

    @FindBy(id = "login-btn")
    private HubWebElement loginBtn;

    public void clickLogin() {
        loginBtn.click();
    }
}
```

### Gelişmiş Özellikler ve Performans

#### Artifact ve Ekran Görüntüsü Yönetimi
Hub, JUnit 5 yaşam döngüsüne entegre bir otomatik artifact toplama sistemi sunar.

| Politika | Açıklama |
| :--- | :--- |
| `ALWAYS` | Her test sonunda ekran görüntüsü alır. |
| `ON_FAILURE` | Sadece test başarısız olduğunda alır (Varsayılan). |
| `NEVER` | Artifact toplamayı kapatır. |

**Genişletilebilir Depolama:**
Ekran görüntülerini S3, Azure veya özel bir bulut depolama alanına kaydetmek için kendi `ArtifactManager` bean'inizi tanımlayabilirsiniz.

```java
@Bean
public ArtifactManager s3Manager() {
    return new S3ArtifactManager("bucket-adim");
}
```

#### Telemetri Yayıncısı (Telemetry Emitter) 
Hub, izleme, raporlama ve CI/CD entegrasyonu için yapılandırılmış test yürütme olayları yayınlar.

```yaml
hub:
  telemetry:
    enabled: true  # Varsayılan: true
```

**Çıktı:** Artifact dizininde aşağıdaki içeriğe sahip `hub-telemetry.json` dosyası oluşturulur:

```json
[
  {
    "event": "TEST_PASSED",
    "timestamp": "2026-01-12T00:00:00Z",
    "testClass": "LoginTest",
    "testMethod": "shouldLoginSuccessfully",
    "durationMs": 1250
  }
]
```

**Özel Dinleyiciler:** Özel entegrasyonlar için `TelemetryListener` arayüzünü uygulayın (ör. Datadog, Prometheus).

#### Performans ve Ölçeklendirme
CI/CD süreçleri gibi yüksek eşzamanlılık gerektiren ortamlar için optimize edilmiştir.

*   **Bloklayan Sürücü Havuzu (Blocking Pool)**: Kaynak tükenmesini önlemek için, boşta sürücü kalmadığında test thread'lerini güvenli bir şekilde bekletir.
*   **Tembel Proxy (Lazy Proxy)**: Fiziksel tarayıcıyı sadece bir komut (örn. `driver.get()`) çağrıldığında ayağa kaldırarak boşta kaynak kullanımını engeller.
*   **Thread-Safe Bağlam**: `ThreadLocal` yapısı sayesinde paralel koşan testler arasında veri sızıntısını sıfıra indirir.

### Uzaktan Yürütme ve Altyapı Desteği
Hub, hibrit bulut ve şirket içi Selenium Grid yapılarının yanı sıra Playwright Connect senaryolarını da destekler. Özelleştirilmiş tarayıcı yetenekleri (Capabilities) hem programatik hem de deklaratif olarak konfigüre edilebilir.

---

### HybridProvider: Çift Motor Gücü 🔀

Hub Framework, Chrome DevTools Protocol (CDP) üzerinden Selenium ve Playwright'ı **aynı tarayıcı oturumuna** bağlayan devrimci bir **HybridProvider** sunar. Bu benzersiz yetenek, her iki framework'ün en iyi özelliklerinden aynı anda yararlanmanızı sağlar.

#### Mimari
```
┌─────────────────────────────────────────────────────┐
│                  HybridProvider                      │
├─────────────────────────────────────────────────────┤
│  Tarayıcı İşlemi (Chrome/Edge)                      │
│  └── CDP Endpoint: localhost:9222                   │
│       ├── Selenium WebDriver ──┐                    │
│       └── Playwright Page ─────┼──► HubWebDriver    │
│                                │                    │
│  PlaywrightCapabilities ───────┘                    │
│  (Otomatik Bekleme, Network Mock, İzleme, Diyalog)  │
└─────────────────────────────────────────────────────┘
```

#### Konfigürasyon
```yaml
hub:
  provider: hybrid  # Çift motor modunu etkinleştirir
  browser: chrome
  headless: false
  provider-options:
    hybrid.cdp.port: 9222
    hybrid.playwright.autowait: true
```

#### Strateji Yönlendirmesi

| İşlem | Motor | Gerekçe |
| :--- | :--- | :--- |
| `find` (bekleme aşaması) | **Playwright** | Element görünürlüğü için otomatik bekleme |
| `find` (element referansı) | Selenium | WebElement uyumluluğu |
| `click`, `type` | Selenium | Olgun, stabil API |
| `screenshot` | **Playwright** | Tam sayfa, yüksek kalite |
| `network mock` | **Playwright** | Yerel birinci sınıf destek |

#### PlaywrightCapabilities API

`HybridSession` üzerinden gelişmiş Playwright özelliklerine erişim:

```java
@HubTest
@SpringBootTest
public class HybridTest {
    @HubDriver(provider = HubProviderType.HYBRID)
    private HubWebDriver driver;

    @Test
    void testWithPlaywrightPowers() {
        HybridSession session = (HybridSession) driver.getSession();
        PlaywrightCapabilities pw = session.playwright();

        // Network Yakalama
        pw.mockJsonRequest("**/api/users", "[{\"id\": 1}]");
        pw.blockRequests("**/analytics/**");

        // Diyalog Yönetimi
        pw.autoAcceptDialogs();

        // Konsol Loglama
        pw.onConsoleMessage(msg -> System.out.println("Tarayıcı: " + msg));

        // İzleme (debugging için)
        pw.startTracing("benim-testim");

        driver.get("https://example.com");
        driver.findElement(By.id("btn")).click(); // Otomatik bekleme aktif!

        // Tam sayfa ekran görüntüsü
        byte[] screenshot = pw.fullPageScreenshot();

        pw.stopTracing(Path.of("trace.zip"));
    }
}
```

#### Mevcut Yetenekler

| Metod | Açıklama |
| :--- | :--- |
| `waitForSelector(selector, timeout)` | Element görünürlüğünü bekle |
| `waitForLoadState(state)` | `load`, `domcontentloaded`, `networkidle` bekle |
| `waitForURL(pattern)` | URL navigasyonunu bekle |
| `mockJsonRequest(pattern, json)` | API'yi JSON yanıtıyla mockla |
| `blockRequests(pattern)` | Eşleşen istekleri blokla |
| `interceptRequests(pattern, handler)` | Özel istek yakalama |
| `startTracing(name)` / `stopTracing(path)` | Debug izleri kaydet |
| `autoAcceptDialogs()` | Alert/confirm'leri otomatik kabul et |
| `autoDismissDialogs()` | Diyalogları otomatik kapat |
| `onConsoleMessage(handler)` | Tarayıcı konsolunu yakala |
| `onPageError(handler)` | Sayfa hatalarını yakala |
| `fullPageScreenshot()` | Yüksek kaliteli tam sayfa görüntüsü |
| `elementScreenshot(selector)` | Belirli elementi görüntüle |
| `setGeolocation(lat, lng)` | Konum emülasyonu |
| `evaluate(js)` | JavaScript çalıştır |

> **Not:** HybridProvider yalnızca Chromium tabanlı tarayıcıları destekler (Chrome, Edge).

---
© 2026 **DOD Framework**. All rights reserved.
