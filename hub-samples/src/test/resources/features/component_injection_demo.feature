@component_injection @Selenium @Window
Feature: Component Dependency Injection

  Scenario: Verify dependency injection in HubComponent
    Given I navigate to the stability page for component test
    When I access the greeting component on the demo page
    Then the component should display the greeting "Hello from GreetingService!"
    And the component should have an initialized root element
