Feature: Execute Script Compatibility

  @Selenium @Window
  Scenario: Execute JavaScript with Arguments and Return Value
    Given I open the local stability page
    When I execute script "return arguments[0] + arguments[1];" with arguments 10 and 20
    Then the script result should be 30

  @Playwright @Window
  Scenario: Execute JavaScript with Arguments and Return Value (Playwright)
    Given I open the local stability page
    When I execute script "return arguments[0] + arguments[1];" with arguments 10 and 20
    Then the script result should be 30

  @Hybrid @Window
  Scenario: Execute JavaScript with Arguments and Return Value (Hybrid)
    Given I open the local stability page
    When I execute script "return arguments[0] + arguments[1];" with arguments 10 and 20
    Then the script result should be 30

  @Selenium @Window
  Scenario: Execute JavaScript with HubWebElement Argument
    Given I open the local stability page
    When I find the element by ID "item-input"
    And I execute script "arguments[0].value = 'Hello Hub';" on the found element
    Then the element value should be "Hello Hub"

  @Playwright @Window
  Scenario: Execute JavaScript with HubWebElement Argument (Playwright)
    Given I open the local stability page
    When I find the element by ID "item-input"
    And I execute script "arguments[0].value = 'Hello Hub';" on the found element
    Then the element value should be "Hello Hub"

  @Hybrid @Window
  Scenario: Execute JavaScript with HubWebElement Argument (Hybrid)
    Given I open the local stability page
    When I find the element by ID "item-input"
    And I execute script "arguments[0].value = 'Hello Hub';" on the found element
    Then the element value should be "Hello Hub"
