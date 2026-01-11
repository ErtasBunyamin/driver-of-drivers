Feature: Hub Framework Demo

  Scenario: Login Test
    Given I navigate to "https://example.com"
    Then the page title should contain "Example"
    And I should see the header "Example Domain"
