# Hub Automation Framework: Enterprise Unified UI Testing Solution

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
| **`hub-spring-boot-starter`** | Autoconfiguration, bean management, and JUnit 5 extensions. |

### Integration Standards

#### Maven Configuration
```xml
<dependency>
    <groupId>com.dod.hub</groupId>
    <artifactId>hub-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

#### Centralized Configuration
Declarative management via `application.yml`:
```yaml
hub:
  provider: selenium # Options: selenium, playwright
  browser: chrome    # Options: chrome, firefox, edge, webkit (playwright only)
  headless: true
  grid-url: ${GRID_URL:http://localhost:4444/wd/hub}
  provider-options:
    acceptInsecureCerts: true
    pageLoadStrategy: eager
```

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

### Remote Execution and Infrastructure Support
Hub supports hybrid cloud and on-premise Selenium Grid setups, as well as Playwright Connect scenarios. Custom browser capabilities can be configured both programmatically and decoratively.

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
| **`hub-spring-boot-starter`** | Otomatik konfigürasyon, bean yönetimi ve JUnit 5 uzantıları. |

### Entegrasyon Standartları

#### Maven Yapılandırması
```xml
<dependency>
    <groupId>com.dod.hub</groupId>
    <artifactId>hub-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

#### Merkezi Konfigürasyon Yönetimi
`application.yml` üzerinden deklaratif yönetim:

```yaml
hub:
  provider: selenium # Seçenekler: selenium, playwright
  browser: chrome    # Seçenekler: chrome, firefox, edge, webkit (sadece playwright)
  headless: true
  grid-url: ${GRID_URL:http://localhost:4444/wd/hub}
  provider-options:
    acceptInsecureCerts: true
    pageLoadStrategy: eager
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

### Uzaktan Yürütme ve Altyapı Desteği
Hub, hibrit bulut ve şirket içi Selenium Grid yapılarının yanı sıra Playwright Connect senaryolarını da destekler. Özelleştirilmiş tarayıcı yetenekleri (Capabilities) hem programatik hem de deklaratif olarak konfigüre edilebilir.

---
© 2026 **DOD Team** - Advanced Agentic Coding Systems. Tüm hakları saklıdır.
