@comprehensive @Selenium @Window
Feature: Comprehensive HubComponent Verification

  Scenario: Verify List of Nested HubComponents
    Given I navigate to the complex components page
    Then I should find 2 parent components
    
    When I inspect parent 1
    Then it should have title "Parent 1"
    And it should have a single child with grandchild content "Grandchild Content P1-S"
    And it should have a child list of size 2
    
    When I inspect parent 2
    Then it should have title "Parent 2"
    And it should have a single child with grandchild content "Grandchild Content P2-S"
    And it should have a child list of size 3
