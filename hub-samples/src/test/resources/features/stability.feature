@Stability
Feature: Hub Provider Stability

  @Playwright
  Scenario Outline: Playwright stability run <run>
    Given I open the local stability page
    Then the delayed badge should appear
    And the page title should contain "DOD Stability Lab"
    And I should see the header "Stability Lab"
    When I fill the input with "alpha"
    And I click the add button
    Then the list should contain item "alpha"
    When I add 3 items quickly
    Then the list should have 4 items
    When I trigger the async status update
    Then the status should be "READY"
    When I toggle the details panel
    Then the details panel should be "hidden"
    When I toggle the details panel
    Then the details panel should be "visible"
    And I capture a screenshot

    Examples:
      | run |
      | 1   |
      | 2   |
      | 3   |
      | 4   |
      | 5   |

  @Selenium
  Scenario Outline: Selenium stability run <run>
    Given I open the local stability page
    Then the delayed badge should appear
    And the page title should contain "DOD Stability Lab"
    And I should see the header "Stability Lab"
    When I fill the input with "alpha"
    And I click the add button
    Then the list should contain item "alpha"
    When I add 3 items quickly
    Then the list should have 4 items
    When I trigger the async status update
    Then the status should be "READY"
    When I toggle the details panel
    Then the details panel should be "hidden"
    When I toggle the details panel
    Then the details panel should be "visible"
    And I capture a screenshot

    Examples:
      | run |
      | 1   |
      | 2   |
      | 3   |
      | 4   |
      | 5   |

  @Hybrid
  Scenario Outline: Hybrid stability run <run>
    Given I open the local stability page
    Then the delayed badge should appear
    And the page title should contain "DOD Stability Lab"
    And I should see the header "Stability Lab"
    When I fill the input with "alpha"
    And I click the add button
    Then the list should contain item "alpha"
    When I add 3 items quickly
    Then the list should have 4 items
    When I trigger the async status update
    Then the status should be "READY"
    When I toggle the details panel
    Then the details panel should be "hidden"
    When I toggle the details panel
    Then the details panel should be "visible"
    And I capture a screenshot

    Examples:
      | run |
      | 1   |
      | 2   |
      | 3   |
      | 4   |
      | 5   |
