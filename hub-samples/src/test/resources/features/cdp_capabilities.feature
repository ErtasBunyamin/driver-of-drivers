Feature: CDP URL & Capabilities

  @Selenium @Window
  Scenario: Selenium driver exposes capabilities with browserName
    Given I open the local stability page
    Then the driver should expose capabilities
    And the capabilities should contain "browserName"
    And the capabilities should contain "se:cdp"
    And the capabilities should not contain "hub.scriptTimeoutMs"

  @Hybrid @Window
  Scenario: Hybrid driver exposes se:cdp capability
    Given I open the local stability page
    Then the driver should expose capabilities
    And the capabilities should contain "se:cdp"
    And the capabilities should contain "browserName"
    And the capabilities should not contain "hub.scriptTimeoutMs"

  @Playwright @Window
  Scenario: Playwright driver exposes capabilities without internal keys
    Given I open the local stability page
    Then the driver should expose capabilities
    And the capabilities should contain "se:cdp"
    And the capabilities should not contain "hub.scriptTimeoutMs"
