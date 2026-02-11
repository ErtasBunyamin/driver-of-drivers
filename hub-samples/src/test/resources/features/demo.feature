Feature: Hub Framework Demo

  @Playwright
  Scenario: Login Playwright Test
    Given I navigate to "https://example.com"
    Then the page title should contain "Example"
    And I should see the header "Example Domain"

  @Selenium
  Scenario: Login Selenium Test
    Given I navigate to "https://example.com"
    Then the page title should contain "Example"
    And I should see the header "Example Domain"

  @Hybrid
  Scenario: Login Hybrid Test
    Given I navigate to "https://example.com"
    Then the page title should contain "Example"
    And I should see the header "Example Domain"
