Feature: Comprehensive Provider Test (Page Object Model)

  Tüm provider'lar (Selenium, Playwright, Hybrid) için POM tabanlı kapsamlı test.
  Her senaryo, StabilityPage page object üzerinden tüm HubWebDriver özelliklerini doğrular.

  # ── Navigation, Element Ops, State ──────────────────────────────

  @Selenium @Window
  Scenario: Selenium — Full feature verification via POM
    Given I initialize the StabilityPage
    # Navigation
    Then the page title text should be "Stability Lab"
    And the current URL should contain "stability-page"
    # Element Finding & Text
    And the subtitle text should be "Local test page for provider stability."
    # Input, Click, List
    When I add an item "Selenium Item" via POM
    Then the item list should have 1 entries
    And item 0 should have text "Selenium Item"
    # Clear & Type
    When I type "overwrite test" into the item input
    Then the item input value should be "overwrite test"
    When I clear the item input
    Then the item input value should be ""
    # Element State — isEnabled
    Then the disabled input should not be enabled
    And the item input should be enabled
    # Element State — isDisplayed
    And the page title should be displayed
    # Element State — isSelected (checkbox)
    Then the agree checkbox should not be selected
    When I toggle the agree checkbox
    Then the agree checkbox should be selected
    # Select interaction
    Then the selected color should be "green"
    When I select color "blue"
    Then the selected color should be "blue"
    # getAttribute
    Then the nav link href should contain "example.com"
    # Async Update
    When I click async update via POM
    Then the status should eventually be "READY"
    # Toggle Details
    When I toggle details via POM
    Then the details panel should be hidden
    When I toggle details via POM
    Then the details panel should be visible
    # JavaScript Execution
    When I execute JS "arguments[0].scrollIntoView({ behavior: \"instant\", block: \"center\", inline: \"nearest\" });" with element "nav-link-footer" via driver
    When I execute JS "return document.title;" via driver
    Then the JS result should be "DOD Stability Lab"
    # Screenshot
    Then I should be able to take a screenshot
    # Frame Switching
    When I switch to frame "test-frame"
    Then the frame text should be "Hello from iframe"
    When I switch to default content
    Then the page title text should be "Stability Lab"
    # Window Management
    When I open a new window via POM
    Then I should have at least 2 window handles
    When I close the extra window and switch back
    Then I should have at least 1 window handles
    # Capabilities
    Then the capabilities should include "se:cdp"
    # Page Source
    Then the page source should contain "Stability Lab"

  @Playwright @Window
  Scenario: Playwright — Full feature verification via POM
    Given I initialize the StabilityPage
    Then the page title text should be "Stability Lab"
    And the current URL should contain "stability-page"
    And the subtitle text should be "Local test page for provider stability."
    When I add an item "Playwright Item" via POM
    Then the item list should have 1 entries
    And item 0 should have text "Playwright Item"
    When I type "overwrite test" into the item input
    Then the item input value should be "overwrite test"
    When I clear the item input
    Then the item input value should be ""
    Then the disabled input should not be enabled
    And the item input should be enabled
    And the page title should be displayed
    Then the agree checkbox should not be selected
    When I toggle the agree checkbox
    Then the agree checkbox should be selected
    Then the selected color should be "green"
    When I select color "blue"
    Then the selected color should be "blue"
    Then the nav link href should contain "example.com"
    When I click async update via POM
    Then the status should eventually be "READY"
    When I toggle details via POM
    Then the details panel should be hidden
    When I toggle details via POM
    Then the details panel should be visible
    When I execute JS "arguments[0].scrollIntoView({ behavior: \"instant\", block: \"center\", inline: \"nearest\" });" with element "nav-link-footer" via driver
    When I execute JS "return document.title;" via driver
    Then the JS result should be "DOD Stability Lab"
    Then I should be able to take a screenshot
    When I switch to frame "test-frame"
    Then the frame text should be "Hello from iframe"
    When I switch to default content
    Then the page title text should be "Stability Lab"
    When I open a new window via POM
    Then I should have at least 2 window handles
    When I close the extra window and switch back
    Then I should have at least 1 window handles
    Then the capabilities should include "se:cdp"
    Then the page source should contain "Stability Lab"

  @Hybrid @Window
  Scenario: Hybrid — Full feature verification via POM
    Given I initialize the StabilityPage
    Then the page title text should be "Stability Lab"
    And the current URL should contain "stability-page"
    And the subtitle text should be "Local test page for provider stability."
    When I add an item "Hybrid Item" via POM
    Then the item list should have 1 entries
    And item 0 should have text "Hybrid Item"
    When I type "overwrite test" into the item input
    Then the item input value should be "overwrite test"
    When I clear the item input
    Then the item input value should be ""
    Then the disabled input should not be enabled
    And the item input should be enabled
    And the page title should be displayed
    Then the agree checkbox should not be selected
    When I toggle the agree checkbox
    Then the agree checkbox should be selected
    Then the selected color should be "green"
    When I select color "blue"
    Then the selected color should be "blue"
    Then the nav link href should contain "example.com"
    When I click async update via POM
    Then the status should eventually be "READY"
    When I toggle details via POM
    Then the details panel should be hidden
    When I toggle details via POM
    Then the details panel should be visible
    When I execute JS "return document.title;" via driver
    Then the JS result should be "DOD Stability Lab"
    Then I should be able to take a screenshot
    When I switch to frame "test-frame"
    Then the frame text should be "Hello from iframe"
    When I switch to default content
    Then the page title text should be "Stability Lab"
    When I open a new window via POM
    Then I should have at least 2 window handles
    When I close the extra window and switch back
    Then I should have at least 1 window handles
    Then the capabilities should include "se:cdp"
    Then the page source should contain "Stability Lab"
