@Stability
Feature: Hub Provider Stability

  @Playwright
  Scenario Outline: Playwright stability run <run>
    Given I navigate to "https://example.com"
    Then the page title should contain "Example"
    And I should see the header "Example Domain"

    Examples:
      | run |
      | 1   |
      | 2   |
      | 3   |
      | 4   |
      | 5   |

  @Selenium
  Scenario Outline: Selenium stability run <run>
    Given I navigate to "https://example.com"
    Then the page title should contain "Example"
    And I should see the header "Example Domain"

    Examples:
      | run |
      | 1   |
      | 2   |
      | 3   |
      | 4   |
      | 5   |

  @Hybrid
  Scenario Outline: Hybrid stability run <run>
    Given I navigate to "https://example.com"
    Then the page title should contain "Example"
    And I should see the header "Example Domain"

    Examples:
      | run |
      | 1   |
      | 2   |
      | 3   |
      | 4   |
      | 5   |
