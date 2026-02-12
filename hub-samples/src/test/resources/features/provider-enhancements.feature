Feature: Provider Enhancements

  @Window @Playwright
  Scenario: Playwright window controls and screenshot
    Given I open the local stability page
    When I open a new tab
    Then I should have at least 2 windows
    When I switch to the newest window
    And I set window position to 20 and 30
    Then I can read window position
    When I close the current window
    Then I should have at least 1 windows
    And I capture a screenshot file
    And I should be able to read browser logs

  @Window @Selenium
  Scenario: Selenium window controls and screenshot
    Given I open the local stability page
    When I open a new tab
    Then I should have at least 2 windows
    When I switch to the newest window
    And I set window position to 20 and 30
    Then I can read window position
    When I close the current window
    Then I should have at least 1 windows
    And I capture a screenshot file
    And I should be able to read browser logs

  @Window @Hybrid
  Scenario: Hybrid window controls and screenshot
    Given I open the local stability page
    When I open a new tab
    Then I should have at least 2 windows
    When I switch to the newest window
    And I set window position to 20 and 30
    Then I can read window position
    When I close the current window
    Then I should have at least 1 windows
    And I capture a screenshot file
    And I should be able to read browser logs

  @Playwright
  Scenario: Playwright async script timeout handling
    Given I open the local stability page
    When I set async script timeout to 50 ms
    Then an async script with 200 ms delay should time out
    When I set async script timeout to 1000 ms
    Then an async script with 50 ms delay should succeed

  @Selenium
  Scenario: Selenium async script timeout handling
    Given I open the local stability page
    When I set async script timeout to 50 ms
    Then an async script with 200 ms delay should time out
    When I set async script timeout to 1000 ms
    Then an async script with 50 ms delay should succeed

  @Hybrid
  Scenario: Hybrid async script timeout handling
    Given I open the local stability page
    When I set async script timeout to 50 ms
    Then an async script with 200 ms delay should time out
    When I set async script timeout to 1000 ms
    Then an async script with 50 ms delay should succeed
